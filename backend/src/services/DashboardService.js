const db = require('../config/db');
const RutinaService = require('./RutinasService');
const PlanesService = require('./PlanesService');
const PremiumService = require('./PremiumService');
const RankingService = require('./RankingService');

class DashboardService {
  /**
   * Días consecutivos con actividad (entrenamiento o salida GPS).
   *
   * Reglas (alineadas con Strong / Hevy / Strava):
   *   - Empezamos por el día más reciente con actividad.
   *   - Si la última actividad fue HOY o AYER, la racha sigue viva.
   *   - Si pasaron 2+ días sin actividad, racha = 0.
   *
   * Antes la racha exigía que el usuario hubiera entrenado HOY mismo, así
   * que casi siempre salía 0 (la mayoría no entrenan justo el día que abren
   * la app), por eso el usuario reportaba "está siempre a 0".
   */
  static calcularRacha(fechasDistinct) {
    if (!fechasDistinct?.length) return 0;
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const ayer = new Date(hoy);
    ayer.setDate(ayer.getDate() - 1);
    const set = new Set(
      fechasDistinct.map((r) => {
        const d = new Date(r.d);
        d.setHours(0, 0, 0, 0);
        return d.getTime();
      })
    );
    // Punto de partida: si tiene actividad HOY empezamos desde hoy, si no y
    // tiene AYER empezamos desde ayer (racha sigue contando), si no → 0.
    let cursor;
    if (set.has(hoy.getTime())) cursor = new Date(hoy);
    else if (set.has(ayer.getTime())) cursor = new Date(ayer);
    else return 0;
    let racha = 0;
    while (set.has(cursor.getTime())) {
      racha += 1;
      cursor.setDate(cursor.getDate() - 1);
    }
    return racha;
  }

  static async obtenerResumen(userId, idOposicion, opts = {}) {
    const esFitness = !!opts.esFitness;
    // duracion_oficial está en SEGUNDOS pero `minutosSemana` debe ir en minutos.
    // Antes faltaba la división → la home decía "300 min" para una sesión de
    // 5 min, falseando la métrica.
    const [[semana]] = await db.query(
      `SELECT COUNT(*) AS sesiones,
              ROUND(COALESCE(SUM(duracion_oficial), 0) / 60) AS minutos
       FROM historial_sesiones
       WHERE usuarios_id_usuario = ?
         AND fecha_entreno >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)`,
      [userId]
    );
    // Añadimos minutos de actividades GPS (carreras, bici, etc.) — antes la
    // home solo contaba entrenos clásicos.
    const [[semanaGps]] = await db.query(
      `SELECT COUNT(*) AS actividades,
              ROUND(COALESCE(SUM(duracion_seg), 0) / 60) AS minutos
         FROM gps_actividades
        WHERE usuarios_id_usuario = ?
          AND iniciada_en >= UNIX_TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY)) * 1000`,
      [userId]
    );
    const [[total]] = await db.query(
      `SELECT COUNT(*) AS sesiones FROM historial_sesiones WHERE usuarios_id_usuario = ?`,
      [userId]
    );
    // Para la racha, unimos días con entreno + días con GPS — actividad es actividad.
    const [fechasRows] = await db.query(
      `SELECT DISTINCT d FROM (
         SELECT DATE(fecha_entreno) AS d
           FROM historial_sesiones
          WHERE usuarios_id_usuario = ?
         UNION
         SELECT DATE(FROM_UNIXTIME(iniciada_en / 1000)) AS d
           FROM gps_actividades
          WHERE usuarios_id_usuario = ?
       ) u
       ORDER BY d DESC
       LIMIT 90`,
      [userId, userId]
    );
    const [[ultimaSesion]] = await db.query(
      `SELECT fecha_entreno, duracion_oficial, tipo_rutina
       FROM historial_sesiones
       WHERE usuarios_id_usuario = ?
       ORDER BY fecha_entreno DESC
       LIMIT 1`,
      [userId]
    );
    let ultimoSim = null;
    if (!esFitness) {
      const [[simRow]] = await db.query(
        `SELECT nota_media, fecha
         FROM simulacros
         WHERE usuarios_id_usuario = ? AND oposiciones_id_oposicion = ?
         ORDER BY fecha DESC
         LIMIT 1`,
        [userId, idOposicion]
      );
      ultimoSim = simRow;
    }
    const [[opo]] = await db.query(
      'SELECT nombre FROM oposiciones WHERE id_oposicion = ?',
      [idOposicion]
    );
    const nivelInfo = esFitness
      ? {
          notaMedia: null,
          nivelSugerido: 'BASICO',
          genero: opts.genero,
          totalPruebas: 0,
          pruebasCompletadas: 0,
          pruebasFaltantes: 0
        }
      : await RutinaService.calcularNotaYNivel(userId, idOposicion);
    const ranking = esFitness
      ? { posicion: null, total: null, notaMedia: null }
      : await RankingService.posicionUsuario(userId, idOposicion);
    const graficaSemanal = await DashboardService.obtenerGraficaSemanal(userId);

    let entrenoHoy = null;
    const puedePlan =
      esFitness || (nivelInfo.nivelSugerido && (nivelInfo.pruebasFaltantes ?? 0) === 0 && nivelInfo.genero);
    if (puedePlan && nivelInfo.genero) {
      const premium = await PremiumService.getEstadoPremium(userId);
      const nivelPlan = esFitness
        ? 'BASICO'
        : !premium.esPremium && nivelInfo.nivelSugerido !== 'BASICO'
          ? 'BASICO'
          : nivelInfo.nivelSugerido;
      let plan = null;
      try {
        plan = await PlanesService.obtenerPlanSemanal(
          userId,
          idOposicion,
          nivelPlan,
          nivelInfo.genero
        );
      } catch (planErr) {
        console.error('Dashboard planSemanal:', planErr.message);
      }
      const sesion = plan?.sesion_hoy || plan?.proxima_sesion;
      if (sesion) {
        entrenoHoy = {
          nombreDia: sesion.nombre_dia,
          enfoque: sesion.enfoque,
          titulo: sesion.titulo,
          descripcion: plan?.personalizacion?.resumen || sesion.descripcion,
          esHoy: !!plan?.sesion_hoy,
          completada: sesion.completada,
          id_plan_dia: sesion.id_plan_dia,
          id_rutina_opo: sesion.id_rutina_opo
        };
      }
    }

    return {
      modoUso: esFitness ? 'FITNESS' : 'OPOSITOR',
      oposicionNombre: esFitness ? 'Modo fitness' : (opo?.nombre || null),
      // Suma TODO: entrenos clásicos + salidas GPS (carrera, bici, etc.).
      sesionesSemana: Number(semana?.sesiones || 0) + Number(semanaGps?.actividades || 0),
      minutosSemana: Number(semana?.minutos || 0) + Number(semanaGps?.minutos || 0),
      sesionesTotales: Number(total?.sesiones || 0),
      rachaDias: DashboardService.calcularRacha(fechasRows),
      ultimaSesion: ultimaSesion
        ? {
            fecha: ultimaSesion.fecha_entreno,
            duracionMin: Number(ultimaSesion.duracion_oficial || 0),
            tipo: ultimaSesion.tipo_rutina
          }
        : null,
      ultimoSimulacro: ultimoSim
        ? { notaMedia: ultimoSim.nota_media, fecha: ultimoSim.fecha }
        : null,
      notaMedia: nivelInfo.notaMedia,
      nivel: nivelInfo.nivelSugerido || 'INCOMPLETO',
      pruebasCompletadas: nivelInfo.pruebasCompletadas,
      totalPruebas: nivelInfo.totalPruebas,
      rankingPosicion: ranking.posicion,
      rankingTotal: ranking.total,
      rankingNotaMedia: ranking.notaMedia,
      graficaSemanal,
      entrenoHoy
    };
  }

  static async obtenerGraficaSemanal(userId) {
    const [rows] = await db.query(
      `SELECT DATE(fecha_entreno) AS dia, COUNT(*) AS sesiones, COALESCE(SUM(duracion_oficial), 0) AS minutos
       FROM historial_sesiones
       WHERE usuarios_id_usuario = ?
         AND fecha_entreno >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
       GROUP BY DATE(fecha_entreno)
       ORDER BY dia ASC`,
      [userId]
    );
    const map = new Map((rows || []).map((r) => [String(r.dia), r]));
    const dias = ['D', 'L', 'M', 'X', 'J', 'V', 'S'];
    const out = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const key = d.toISOString().slice(0, 10);
      const row = map.get(key);
      out.push({
        dia: key,
        etiqueta: dias[d.getDay()],
        sesiones: Number(row?.sesiones || 0),
        minutos: Number(row?.minutos || 0)
      });
    }
    return out;
  }
}

module.exports = DashboardService;
