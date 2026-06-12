const db = require('../config/db');
const RutinaService = require('./RutinasService');
const BancoPlanesImportService = require('./BancoPlanesImportService');
const PlanPersonalizadorService = require('./PlanPersonalizadorService');
const EjercicioMetadataService = require('./EjercicioMetadataService');
const PlanGeneradorService = require('./PlanGeneradorService');
const EjercicioInteligenteService = require('./EjercicioInteligenteService');

const NOMBRES_DIA = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

/**
 * Reparte N días de entreno a lo largo de la semana de la forma más
 * equilibrada posible: 3 → L/X/V, 4 → L/M/J/V, 5 → L-V, etc.
 */
function repartirEnSemana(n) {
  const mapa = {
    1: [3],
    2: [1, 4],
    3: [1, 3, 5],
    4: [1, 2, 4, 5],
    5: [1, 2, 3, 4, 5],
    6: [1, 2, 3, 4, 5, 6],
    7: [1, 2, 3, 4, 5, 6, 7]
  };
  return mapa[n] || [1, 2, 3, 4, 5];
}

class PlanesService {
  static resolverUnidad(notas, nombre, pilar) {
    const validas = new Set(['reps', 'rep', 'min', 's', 'seg', 'km', 'm', 'amrap']);
    const inferida = RutinaService.inferUnidad(nombre);
    const nota = String(notas || '').toLowerCase().trim();
    const pil = String(pilar || '').toUpperCase();
    if (inferida !== 'reps') return inferida;
    if (pil === 'RESISTENCIA') return 'min';
    if (pil === 'VELOCIDAD' && /\bsprint\b/.test(String(nombre || '').toLowerCase())) return 'm';
    if (nota && validas.has(nota)) return nota === 'seg' ? 's' : nota;
    return inferida;
  }

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
      return desdePlan.map((e, idx) =>
        EjercicioInteligenteService.aplicarInteligencia(
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
            unidad: PlanesService.resolverUnidad(e.unidad, e.nombre, e.pilar),
            orden: e.orden || idx + 1
          }),
          { seed: e.orden || idx + 1 }
        )
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

  /**
   * Días/semana preferidos por el usuario. Si no está fijado, asumimos 5
   * (comportamiento legacy del banco L-V).
   */
  static async obtenerDiasEntrenoSemana(userId) {
    try {
      const [rows] = await db.query(
        'SELECT dias_entreno_semana FROM usuarios WHERE id_usuario = ?',
        [userId]
      );
      const n = Number(rows[0]?.dias_entreno_semana);
      if (!Number.isFinite(n) || n < 1 || n > 7) return 5;
      return n;
    } catch (_) {
      return 5;
    }
  }

  /**
   * Adapta los días del plan a la preferencia del usuario (1-7).
   * - Prioriza los enfoques en función de los pilares débiles del usuario.
   * - Si el usuario pide menos días que el banco trae, conserva los más
   *   prioritarios y reparte uniformemente L-D.
   * - Si pide más, añade un día extra de movilidad/aeróbico ligero.
   * Devuelve una lista de "slots" {dia_semana, sesion|null}. El día con
   *   `sesion === null` representa descanso/recuperación.
   */
  static adaptarDiasSemana(diasOrig, diasObjetivo, pilaresDebiles = []) {
    const dias = Array.isArray(diasOrig) ? [...diasOrig] : [];
    if (!dias.length || !Number.isFinite(diasObjetivo) || diasObjetivo === dias.length) {
      return { dias, diasObjetivo: dias.length };
    }
    const N = Math.min(7, Math.max(1, Math.round(diasObjetivo)));
    const PRIO_BASE = { RESISTENCIA: 0, FUERZA: 1, VELOCIDAD: 2, CORE: 3, MOVILIDAD: 4 };
    const debilSet = new Set((pilaresDebiles || []).map((p) => String(p.pilar || p).toUpperCase()));
    const score = (d) => {
      const enf = String(d.enfoque || '').toUpperCase();
      const base = PRIO_BASE[enf] ?? 5;
      const bonus = debilSet.has(enf) ? -10 : 0;
      return base + bonus;
    };

    if (N < dias.length) {
      const ordenadas = [...dias].sort((a, b) => score(a) - score(b)).slice(0, N);
      const conservadasIds = new Set(ordenadas.map((d) => d.id_plan_dia));
      const conservadas = dias.filter((d) => conservadasIds.has(d.id_plan_dia));
      const slots = repartirEnSemana(N);
      const reasignadas = conservadas.map((d, idx) => ({
        ...d,
        dia_semana: slots[idx],
        nombre_dia: NOMBRES_DIA[slots[idx]] || d.nombre_dia
      }));
      return { dias: reasignadas, diasObjetivo: N };
    }

    // Si el usuario pide MÁS días que el banco trae, no inventamos sesiones
    // vacías (rompería registro de entreno e historial). Mantenemos las
    // sesiones reales del banco hasta que la IA genere días extra con
    // ejercicios de verdad.
    if (N > dias.length) {
      return { dias, diasObjetivo: dias.length };
    }
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

    const diasObjetivo = await PlanesService.obtenerDiasEntrenoSemana(userId);
    const adaptado = PlanesService.adaptarDiasSemana(semana, diasObjetivo);
    const semanaAdaptada = adaptado.dias
      .map((s) => ({ ...s, es_hoy: s.dia_semana === hoy }))
      .sort((a, b) => a.dia_semana - b.dia_semana);

    const sesionHoy = semanaAdaptada.find((s) => s.es_hoy) || null;
    const proxima =
      semanaAdaptada.find((s) => !s.completada && s.dia_semana >= hoy && !s.es_recuperacion) ||
      semanaAdaptada.find((s) => !s.completada && !s.es_recuperacion) ||
      null;

    const planBase = {
      id_plan: idPlan,
      dias_por_semana: adaptado.diasObjetivo,
      dia_hoy: hoy,
      nombre_dia_hoy: NOMBRES_DIA[hoy],
      semana: semanaAdaptada,
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

    return PlanesService.sanitizarPlan(plan);
  }

  /** Repara prescripciones e instrucciones al servir (también planes en caché antiguos). */
  static sanitizarPlan(plan) {
    if (!plan?.semana?.length) return plan;
    const semana = plan.semana.map((dia) => {
      let motivoSesionMostrado = false;
      const ejercicios = (dia.ejercicios || []).map((ej, idx) => {
        let limpio = PlanPersonalizadorService.normalizarPrescripcionRealista(ej, {
          seed: ej.orden || idx + 1
        });
        limpio = {
          ...limpio,
          instrucciones_tecnicas: EjercicioInteligenteService.generarInstrucciones(limpio),
          grupo_muscular: EjercicioMetadataService.inferirGrupoMuscular(
            limpio.grupo_muscular,
            limpio.nombre,
            limpio.pilar || limpio.categoria
          )
        };
        if (limpio.motivo_ajuste && motivoSesionMostrado) {
          limpio = { ...limpio, motivo_ajuste: null };
        } else if (limpio.motivo_ajuste) {
          motivoSesionMostrado = true;
        }
        return limpio;
      });
      return { ...dia, ejercicios };
    });
    const hoy = PlanesService.diaSemanaHoy();
    const sesion_hoy = semana.find((s) => s.es_hoy) || null;
    const proxima =
      semana.find((s) => !s.completada && s.dia_semana >= hoy) ||
      semana.find((s) => !s.completada) ||
      null;
    return { ...plan, semana, sesion_hoy, proxima_sesion: proxima };
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
