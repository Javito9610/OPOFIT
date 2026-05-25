const db = require('../config/db');
const RutinaService = require('./RutinasService');

/** Microciclo estándar OpoFit (opofit_banco_planes.md §1.2): 5 días, 2F+2R+1V */
const MICROCICLO_5_DIAS = [
  { dia_semana: 1, enfoque: 'FUERZA', titulo: 'Fuerza — sesión 1' },
  { dia_semana: 2, enfoque: 'RESISTENCIA', titulo: 'Resistencia — sesión 1' },
  { dia_semana: 3, enfoque: 'VELOCIDAD', titulo: 'Velocidad y agilidad' },
  { dia_semana: 4, enfoque: 'FUERZA', titulo: 'Fuerza — sesión 2' },
  { dia_semana: 6, enfoque: 'RESISTENCIA', titulo: 'Resistencia — sesión 2' }
];

const NOMBRES_DIA = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

/** Extractos del banco PN Escala Básica (opofit_banco_planes.md §3.1) */
const SESIONES_PN_BASICA = {
  HOMBRE_BASICO: {
    1: 'Fuerza inicial tren superior: dominadas asistidas, press banca, remo, curl, plancha.',
    2: 'Rodaje continuo Z2: 25\' carrera + progresivos al 70 %.',
    3: 'Agilidad básica: skipping, sprints 30 m, conos en T, vallas bajas.',
    4: 'Tren inferior y core: sentadilla goblet, PMD rumano, zancadas, hollow, antebrazos.',
    6: 'Series 200 m: 6×200 m al 80 % + 1 km final Z2.'
  },
  HOMBRE_INTERMEDIO: {
    1: 'Dominadas, press banca 5×5, remo barra, curl, plancha lateral.',
    2: 'HIIT 30:30 + bloque 4 km Z2.',
    3: 'Circuito agilidad oficial, vallas, sprints en cuesta, CMJ.',
    4: 'Sentadilla trasera, hip thrust, zancadas, PMD, plancha con peso.',
    6: 'Test 1000 m + 5×400 m a ritmo objetivo.'
  },
  HOMBRE_AVANZADO: {
    1: 'Dominadas lastradas, press banca 6×3, remo Pendlay, chin-up, press militar.',
    2: '4×4 min al 95 % FCmáx + 15\' Z2.',
    3: 'Circuito oficial cronometrado, sprints con resistencia, balón medicinal, drop jump.',
    4: 'Sentadilla 6×3, peso muerto, pliometría, nordic, hollow rocks.',
    6: 'Test 1000 m competición + 8×200 m al 90 %.'
  },
  MUJER_BASICO: {
    1: 'Suspensión en barra, remo invertido, press mancuerna, curl, plancha.',
    2: '25\' Z2 + progresivos. Trabajo de cadencia.',
    3: 'Skipping, sprints 30 m, cambios de dirección, vallas bajas.',
    4: 'Sentadilla goblet, hip thrust unilateral, PMD rumano, hollow, carry.',
    6: '5×200 m al 80 % + 1 km Z2.'
  },
  MUJER_INTERMEDIO: {
    1: 'Suspensión flexionada, dominadas asistidas, press banca, remo T, plancha lateral.',
    2: 'HIIT 30:30 + 3 km Z2 final.',
    3: 'Circuito agilidad oficial, sprints en cuesta, CMJ, balón medicinal.',
    4: 'Sentadilla trasera, hip thrust, zancadas, press militar, plancha.',
    6: '4×400 m al 90 % + 1000 m a ritmo objetivo.'
  },
  MUJER_AVANZADO: {
    1: 'Dominadas pronas, press banca 5×5, remo Pendlay, chin-up, press militar.',
    2: '4×4\' al 95 % FCmáx + 12\' Z2.',
    3: 'Circuito oficial 6×, sprints paracaídas, pliometría, balón medicinal.',
    4: 'Sentadilla 6×4, peso muerto, hip thrust, nordic, hollow rocks.',
    6: 'Test 1000 m objetivo + 6×200 m al 90 %.'
  }
};

function descripcionSesion(idOposicion, nivel, genero, diaSemana, enfoque) {
  if (idOposicion === 1) {
    const key = `${genero}_${nivel}`;
    const map = SESIONES_PN_BASICA[key];
    if (map && map[diaSemana]) return map[diaSemana];
  }
  const pilar = enfoque === 'FUERZA' ? 'fuerza' : enfoque === 'RESISTENCIA' ? 'resistencia' : 'velocidad y agilidad';
  return `Sesión de ${pilar} (${nivel.toLowerCase()}). Prescripción alineada con el banco OpoFit.`;
}

class PlanesService {
  static async seedPlanesFromRutinas() {
    const [[{ total }]] = await db.query('SELECT COUNT(*) AS total FROM planes_entrenamiento');
    if (Number(total) > 0) return;

    const [combos] = await db.query(
      `SELECT DISTINCT oposiciones_id_oposicion AS opo, nivel, genero
       FROM rutinas_opo ORDER BY opo, nivel, genero`
    );

    for (const c of combos) {
      const [ins] = await db.query(
        `INSERT INTO planes_entrenamiento
         (oposiciones_id_oposicion, nivel, genero, nombre, dias_por_semana, fuente)
         VALUES (?, ?, ?, ?, 5, 'opofit_banco_planes')`,
        [
          c.opo,
          c.nivel,
          c.genero,
          `Plan ${c.nivel} · ${c.genero === 'HOMBRE' ? 'Hombre' : 'Mujer'}`
        ]
      );
      const idPlan = ins.insertId;

      for (let i = 0; i < MICROCICLO_5_DIAS.length; i++) {
        const d = MICROCICLO_5_DIAS[i];
        const [rutinas] = await db.query(
          `SELECT id_rutina_opo FROM rutinas_opo
           WHERE oposiciones_id_oposicion = ? AND nivel = ? AND genero = ? AND enfoque_tipo = ?
           LIMIT 1`,
          [c.opo, c.nivel, c.genero, d.enfoque]
        );
        if (!rutinas.length) continue;
        const desc = descripcionSesion(c.opo, c.nivel, c.genero, d.dia_semana, d.enfoque);
        await db.query(
          `INSERT INTO plan_dias
           (planes_id_plan, dia_semana, orden, enfoque_tipo, rutinas_opo_id, titulo_sesion, descripcion_sesion)
           VALUES (?, ?, ?, ?, ?, ?, ?)`,
          [idPlan, d.dia_semana, i + 1, d.enfoque, rutinas[0].id_rutina_opo, d.titulo, desc]
        );
      }
    }
    console.log('[planes] Planes semanales generados desde rutinas_opo');
  }

  static async obtenerPlanId(idOposicion, nivel, genero) {
    const [rows] = await db.query(
      `SELECT id_plan FROM planes_entrenamiento
       WHERE oposiciones_id_oposicion = ? AND nivel = ? AND genero = ?
       LIMIT 1`,
      [idOposicion, nivel, genero]
    );
    return rows[0]?.id_plan ?? null;
  }

  static async cargarEjerciciosSesion(idRutinaOpo) {
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

  static async sesionesCompletadasSemana(userId, idOposicion) {
    const [rows] = await db.query(
      `SELECT DISTINCT DAYOFWEEK(fecha_entreno) AS dow, h.rutinas_opo_id_rutina_opo
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
    }
    return map;
  }

  static diaSemanaHoy() {
    const js = new Date().getDay();
    return js === 0 ? 7 : js;
  }

  static async obtenerPlanSemanal(userId, idOposicion, nivel, genero) {
    await PlanesService.seedPlanesFromRutinas();
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

    const completadas = await PlanesService.sesionesCompletadasSemana(userId, idOposicion);
    const hoy = PlanesService.diaSemanaHoy();
    const semana = [];

    for (const d of dias) {
      const ejercicios = await PlanesService.cargarEjerciciosSesion(d.rutinas_opo_id);
      const key = `${d.dia_semana}_${d.rutinas_opo_id}`;
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
        completada: completadas.has(key),
        ejercicios
      });
    }

    const sesionHoy = semana.find((s) => s.es_hoy) || null;
    const proxima = semana.find((s) => !s.completada && s.dia_semana >= hoy)
      || semana.find((s) => !s.completada)
      || null;

    return {
      id_plan: idPlan,
      dias_por_semana: dias.length,
      dia_hoy: hoy,
      nombre_dia_hoy: NOMBRES_DIA[hoy],
      semana,
      sesion_hoy: sesionHoy,
      proxima_sesion: proxima
    };
  }
}

module.exports = PlanesService;
