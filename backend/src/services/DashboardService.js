const db = require('../config/db');
const RutinaService = require('./RutinasService');
const RankingService = require('./RankingService');

class DashboardService {
  static calcularRacha(fechasDistinct) {
    if (!fechasDistinct?.length) return 0;
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const set = new Set(
      fechasDistinct.map((r) => {
        const d = new Date(r.d);
        d.setHours(0, 0, 0, 0);
        return d.getTime();
      })
    );
    let racha = 0;
    const cursor = new Date(hoy);
    while (set.has(cursor.getTime())) {
      racha += 1;
      cursor.setDate(cursor.getDate() - 1);
    }
    return racha;
  }

  static async obtenerResumen(userId, idOposicion) {
    const [[semana]] = await db.query(
      `SELECT COUNT(*) AS sesiones,
              COALESCE(SUM(duracion_oficial), 0) AS minutos
       FROM historial_sesiones
       WHERE usuarios_id_usuario = ?
         AND fecha_entreno >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)`,
      [userId]
    );
    const [[total]] = await db.query(
      `SELECT COUNT(*) AS sesiones FROM historial_sesiones WHERE usuarios_id_usuario = ?`,
      [userId]
    );
    const [fechasRows] = await db.query(
      `SELECT DISTINCT DATE(fecha_entreno) AS d
       FROM historial_sesiones
       WHERE usuarios_id_usuario = ?
       ORDER BY d DESC
       LIMIT 90`,
      [userId]
    );
    const [[ultimaSesion]] = await db.query(
      `SELECT fecha_entreno, duracion_oficial, tipo_rutina
       FROM historial_sesiones
       WHERE usuarios_id_usuario = ?
       ORDER BY fecha_entreno DESC
       LIMIT 1`,
      [userId]
    );
    const [[ultimoSim]] = await db.query(
      `SELECT nota_media, fecha
       FROM simulacros
       WHERE usuarios_id_usuario = ? AND oposiciones_id_oposicion = ?
       ORDER BY fecha DESC
       LIMIT 1`,
      [userId, idOposicion]
    );
    const [[opo]] = await db.query(
      'SELECT nombre FROM oposiciones WHERE id_oposicion = ?',
      [idOposicion]
    );
    const nivelInfo = await RutinaService.calcularNotaYNivel(userId, idOposicion);
    const ranking = await RankingService.posicionUsuario(userId, idOposicion);
    const graficaSemanal = await DashboardService.obtenerGraficaSemanal(userId);

    return {
      oposicionNombre: opo?.nombre || null,
      sesionesSemana: Number(semana?.sesiones || 0),
      minutosSemana: Number(semana?.minutos || 0),
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
      graficaSemanal
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
