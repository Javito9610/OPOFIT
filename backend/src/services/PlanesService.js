const db = require('../config/db');
const RutinaService = require('./RutinasService');
const BancoPlanesImportService = require('./BancoPlanesImportService');
const PlanPersonalizadorService = require('./PlanPersonalizadorService');
const EjercicioMetadataService = require('./EjercicioMetadataService');
const PlanGeneradorService = require('./PlanGeneradorService');

const NOMBRES_DIA = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

class PlanesService {
  static normalizarGenero(genero) {
    const g = String(genero || '').toUpperCase();
    return g.includes('MUJER') ? 'MUJER' : 'HOMBRE';
  }

  static async obtenerPlanId(idOposicion, nivel, genero) {
    const generoDb = PlanesService.normalizarGenero(genero);
    const [rows] = await db.query(
      `SELECT id_plan FROM planes_entrenamiento
       WHERE oposiciones_id_oposicion = ? AND nivel = ? AND genero = ?
         AND fuente = 'opofit_banco_planes'
       LIMIT 1`,
      [idOposicion, nivel, generoDb]
    );
    return rows[0]?.id_plan ?? null;
  }

  static async cargarEjerciciosSesion(idPlanDia, idRutinaOpo) {
    const [desdePlan] = await db.query(
      `SELECT pde.orden, pde.nombre_prescripcion AS nombre, pde.series, pde.repeticiones, pde.descanso,
              pde.notas AS unidad, e.id_ejercicio, e.video_url, e.animacion_url, e.instrucciones_tecnicas,
              e.tipo_ilustracion, e.categoria, e.pilar, e.grupo_muscular, e.equipamiento
       FROM plan_dia_ejercicios pde
       LEFT JOIN ejercicios e ON pde.ejercicios_id_ejercicio = e.id_ejercicio
       WHERE pde.plan_dias_id = ?
       ORDER BY pde.orden ASC`,
      [idPlanDia]
    );
    if (desdePlan.length) {
      return desdePlan.map((e) =>
        EjercicioMetadataService.enriquecerEjercicio({
          id_ejercicio: e.id_ejercicio,
          nombre: e.nombre,
          video_url: e.video_url,
          animacion_url: e.animacion_url,
          instrucciones_tecnicas: e.instrucciones_tecnicas,
          tipo_ilustracion: e.tipo_ilustracion,
          grupo_muscular: e.grupo_muscular,
          equipamiento: e.equipamiento,
          categoria: e.categoria,
          pilar: e.pilar,
          series: e.series,
          repeticiones: e.repeticiones,
          descanso: e.descanso,
          unidad: e.unidad || RutinaService.inferUnidad(e.nombre)
        })
      );
    }
    const [ejercicios] = await db.query(
      `SELECT e.id_ejercicio, e.nombre, e.video_url, e.categoria, e.pilar, d.series, d.repeticiones, d.descanso
       FROM detalle_rutina_opo d
       JOIN ejercicios e ON d.ejercicios_id_ejercicio = e.id_ejercicio
       WHERE d.rutinas_opo_id_rutina_opo = ?`,
      [idRutinaOpo]
    );
    return ejercicios.map((e) => ({
      ...e,
      unidad: RutinaService.inferUnidad(e.nombre)
    }));
  }

  static async sesionCompletadaEnFecha(userId, idRutinaOpo, fechaYmd) {
    const [rows] = await db.query(
      `SELECT 1 FROM historial_sesiones
       WHERE usuarios_id_usuario = ? AND rutinas_opo_id_rutina_opo = ?
         AND DATE(fecha_entreno) = ? AND tipo_rutina = 'OPO'
       LIMIT 1`,
      [userId, idRutinaOpo, fechaYmd]
    );
    return rows.length > 0;
  }

  static async sesionesCompletadasSemana(userId) {
    const [rows] = await db.query(
      `SELECT DISTINCT DAYOFWEEK(fecha_entreno) AS dow, h.rutinas_opo_id_rutina_opo, DATE(fecha_entreno) AS f
       FROM historial_sesiones h
       WHERE h.usuarios_id_usuario = ?
         AND h.tipo_rutina = 'OPO'
         AND YEARWEEK(fecha_entreno, 1) = YEARWEEK(CURDATE(), 1)`,
      [userId]
    );
    const map = new Map();
    for (const r of rows) {
      const diaJs = r.dow === 1 ? 7 : r.dow - 1;
      map.set(`${diaJs}_${r.rutinas_opo_id_rutina_opo}`, true);
      map.set(`fecha_${r.f}_${r.rutinas_opo_id_rutina_opo}`, true);
    }
    return map;
  }

  static diaSemanaHoy() {
    const js = new Date().getDay();
    return js === 0 ? 7 : js;
  }

  static formatYmd(d) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  static async obtenerPlanSemanal(userId, idOposicion, nivel, genero, opts = {}) {
    const idPlan = await PlanesService.obtenerPlanId(idOposicion, nivel, genero);
    if (!idPlan) return null;

    const [dias] = await db.query(
      `SELECT pd.id_plan_dia, pd.dia_semana, pd.orden, pd.enfoque_tipo, pd.rutinas_opo_id,
              pd.titulo_sesion, pd.descripcion_sesion
       FROM plan_dias pd
       WHERE pd.planes_id_plan = ?
       ORDER BY pd.orden ASC`,
      [idPlan]
    );

    const completadas = await PlanesService.sesionesCompletadasSemana(userId);
    const hoy = PlanesService.diaSemanaHoy();
    const hoyYmd = PlanesService.formatYmd(new Date());
    const semana = [];

    for (const d of dias) {
      const ejercicios = await PlanesService.cargarEjerciciosSesion(d.id_plan_dia, d.rutinas_opo_id);
      const key = `${d.dia_semana}_${d.rutinas_opo_id}`;
      const completadaSemana = completadas.has(key);
      const completadaHoy =
        d.dia_semana === hoy &&
        (await PlanesService.sesionCompletadaEnFecha(userId, d.rutinas_opo_id, hoyYmd));
      semana.push({
        id_plan_dia: d.id_plan_dia,
        dia_semana: d.dia_semana,
        nombre_dia: NOMBRES_DIA[d.dia_semana] || '',
        orden: d.orden,
        enfoque: d.enfoque_tipo,
        titulo: d.titulo_sesion,
        descripcion: d.descripcion_sesion,
        id_rutina_opo: d.rutinas_opo_id,
        es_hoy: d.dia_semana === hoy,
        completada: d.dia_semana === hoy ? completadaHoy : completadaSemana,
        ejercicios
      });
    }

    const sesionHoy = semana.find((s) => s.es_hoy) || null;
    const proxima =
      semana.find((s) => !s.completada && s.dia_semana >= hoy) ||
      semana.find((s) => !s.completada) ||
      null;

    const planBase = {
      id_plan: idPlan,
      dias_por_semana: dias.length,
      dia_hoy: hoy,
      nombre_dia_hoy: NOMBRES_DIA[hoy],
      semana,
      sesion_hoy: sesionHoy,
      proxima_sesion: proxima
    };

    let plan = planBase;
    try {
      plan = await PlanPersonalizadorService.personalizarPlan(userId, idOposicion, planBase, nivel);
    } catch (err) {
      console.error('Plan personalizador:', err.message);
    }

    try {
      plan = await PlanGeneradorService.aplicarGeneracionInteligente(
        userId,
        idOposicion,
        plan,
        nivel,
        opts
      );
      if (plan.generacion?.activa && plan.personalizacion) {
        plan.personalizacion = {
          ...plan.personalizacion,
          explicacion_ia: plan.generacion.explicacion_ia,
          entorno_entreno: plan.generacion.entorno_entreno,
          entorno_etiqueta: plan.generacion.entorno_etiqueta,
          entorno_emoji: plan.generacion.entorno_emoji,
          variacion_seed: plan.generacion.variacion_seed,
          sustituciones: plan.generacion.sustituciones,
          coaching_fuente: plan.generacion.coaching_fuente
        };
      }
    } catch (err) {
      console.error('Plan generador:', err.message);
    }

    return plan;
  }

  static async obtenerCalendarioMes(userId, idOposicion, nivel, genero, year, month) {
    const plan = await PlanesService.obtenerPlanSemanal(userId, idOposicion, nivel, genero);
    if (!plan) return { year, month, dias: [] };

    const y = Number(year) || new Date().getFullYear();
    const m = Number(month) || new Date().getMonth() + 1;
    const diasEnMes = new Date(y, m, 0).getDate();
    const dias = [];

    for (let d = 1; d <= diasEnMes; d++) {
      const fecha = new Date(y, m - 1, d);
      const dow = fecha.getDay() === 0 ? 7 : fecha.getDay();
      const ymd = PlanesService.formatYmd(fecha);
      const sesion = plan.semana.find((s) => s.dia_semana === dow);
      if (!sesion) {
        dias.push({ fecha: ymd, dia: d, tiene_entreno: false });
        continue;
      }
      const completada = await PlanesService.sesionCompletadaEnFecha(
        userId,
        sesion.id_rutina_opo,
        ymd
      );
      dias.push({
        fecha: ymd,
        dia: d,
        tiene_entreno: true,
        id_plan_dia: sesion.id_plan_dia,
        enfoque: sesion.enfoque,
        titulo: sesion.titulo,
        completada,
        es_hoy: ymd === PlanesService.formatYmd(new Date())
      });
    }

    return { year: y, month: m, dias, semana: plan.semana };
  }
}

module.exports = PlanesService;
