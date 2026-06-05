/**
 * PlanPersonalizadorService
 *
 * Motor hibrido: parte del banco de planes experto (prescripciones fijas)
 * y adapta volumen/intensidad segun marcas del baremo, pilares debiles,
 * nivel del opositor, adherencia semanal y racha de entrenos.
 *
 * Funciones puras exportadas para tests; `personalizarPlan` orquesta con BD.
 */
const db = require('../config/db');
const BaremoService = require('./BaremoService');
const MarcasPerfilService = require('./MarcasPerfilService');

const FACTOR_NIVEL = { BASICO: 0.92, INTERMEDIO: 1.0, AVANZADO: 1.08, INCOMPLETO: 0.9 };
const UMBRAL_DEBIL = 5.5;
const UMBRAL_FUERTE = 8.0;

/** Mapea nombre de prueba oficial al pilar de entrenamiento. */
function pilarDePrueba(nombrePrueba) {
  const n = String(nombrePrueba || '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();
  if (/natac|1000|3000|5000|carrera continua|resistencia|subida|bici|cicl/.test(n)) return 'RESISTENCIA';
  if (/100m|50m|sprint|agilidad|velocidad|vallas|salto vertical|salto lon/.test(n)) return 'VELOCIDAD';
  if (/carrera/.test(n)) return 'RESISTENCIA';
  if (/movilidad|flexibil/.test(n)) return 'MOVILIDAD';
  if (/core|plancha|abdominal/.test(n)) return 'CORE';
  return 'FUERZA';
}

/** Agrupa notas por pilar y detecta debiles/fuertes. */
function analizarPilares(pruebasConNota) {
  const porPilar = new Map();
  for (const p of pruebasConNota) {
    const pil = p.pilar || pilarDePrueba(p.nombre);
    if (!porPilar.has(pil)) porPilar.set(pil, { pilar: pil, notas: [], pruebas: [] });
    const g = porPilar.get(pil);
    g.notas.push(p.nota);
    g.pruebas.push(p.nombre);
  }
  const pilares = [...porPilar.values()].map((g) => ({
    pilar: g.pilar,
    notaMedia: g.notas.reduce((a, b) => a + b, 0) / g.notas.length,
    pruebas: g.pruebas
  }));
  pilares.sort((a, b) => a.notaMedia - b.notaMedia);
  const debiles = pilares.filter((p) => p.notaMedia < UMBRAL_DEBIL);
  const fuertes = pilares.filter((p) => p.notaMedia >= UMBRAL_FUERTE);
  return { pilares, debiles, fuertes, peor: pilares[0] || null };
}

function clampSeries(s) {
  return Math.min(8, Math.max(1, Math.round(s)));
}

function clampReps(reps, unidad, nombre) {
  const u = String(unidad || '').toLowerCase();
  const nom = String(nombre || '').toLowerCase();
  if (u === 'min' || nom.includes('min')) return Math.min(90, Math.max(5, Math.round(reps)));
  if (u === 's' || nom.includes('seg')) return Math.min(7200, Math.max(10, Math.round(reps)));
  if (u === 'km' || nom.includes('km')) return Math.min(50, Math.max(1, Math.round(reps * 10) / 10));
  if (u === 'm' || /\d+\s*m\b/.test(nom)) return Math.min(10000, Math.max(50, Math.round(reps)));
  return Math.min(40, Math.max(1, Math.round(reps)));
}

/**
 * Factor de volumen para un ejercicio concreto.
 * @returns {{factor:number, motivo:string|null}}
 */
function factorEjercicio({
  pilarEjercicio,
  enfoqueSesion,
  pilaresDebiles,
  pilaresFuertes,
  nivel,
  sesionesSemana,
  diasTranscurridosSemana,
  diasPlanSemana,
  rachaDias
}) {
  let f = FACTOR_NIVEL[nivel] || 1.0;
  const motivos = [];
  const debilSet = new Set(pilaresDebiles.map((p) => p.pilar));
  const fuerteSet = new Set(pilaresFuertes.map((p) => p.pilar));
  const pil = pilarEjercicio || 'FUERZA';

  if (debilSet.has(pil)) {
    f *= 1.22;
    motivos.push(`prioridad ${pil.toLowerCase()}`);
  } else if (fuerteSet.has(pil)) {
    f *= 0.9;
    motivos.push('mantenimiento');
  }

  if (debilSet.has(enfoqueSesion) && pil === enfoqueSesion) {
    f *= 1.05;
    motivos.push(`sesion ${enfoqueSesion.toLowerCase()}`);
  }

  const esperadas = Math.max(1, Math.min(diasPlanSemana, diasTranscurridosSemana));
  if (sesionesSemana < esperadas - 1 && diasTranscurridosSemana >= 3) {
    f *= 0.94;
    motivos.push('recuperacion');
  } else if (rachaDias >= 5) {
    f *= 1.04;
    motivos.push('racha activa');
  }

  return {
    factor: Math.min(1.45, Math.max(0.75, f)),
    motivo: motivos.length ? motivos.join(' · ') : null
  };
}

function ajustarEjercicio(ej, ctx) {
  const baseSeries = Number(ej.series) || 1;
  const baseReps = Number(ej.repeticiones) || 1;
  const { factor, motivo } = factorEjercicio({
    pilarEjercicio: ej.pilar || ej.categoria,
    enfoqueSesion: ctx.enfoqueSesion,
    pilaresDebiles: ctx.pilaresDebiles,
    pilaresFuertes: ctx.pilaresFuertes,
    nivel: ctx.nivel,
    sesionesSemana: ctx.sesionesSemana,
    diasTranscurridosSemana: ctx.diasTranscurridosSemana,
    diasPlanSemana: ctx.diasPlanSemana,
    rachaDias: ctx.rachaDias
  });
  const series = clampSeries(baseSeries * factor);
  const repeticiones = clampReps(baseReps * factor, ej.unidad, ej.nombre);
  const personalizado = series !== baseSeries || repeticiones !== baseReps;
  return {
    ...ej,
    series,
    repeticiones,
    series_base: baseSeries,
    repeticiones_base: baseReps,
    personalizado,
    motivo_ajuste: personalizado ? motivo : null
  };
}

function construirResumen({ analisis, nivel, ajustes, rachaDias, peorPrueba }) {
  const partes = [];
  if (analisis.debiles.length > 0) {
    const d = analisis.debiles[0];
    const pruebaTxt = d.pruebas.slice(0, 2).join(', ');
    partes.push(
      `Tu punto debil es ${etiquetaPilar(d.pilar)} (nota ${d.notaMedia.toFixed(1)}/10 en ${pruebaTxt}).`
    );
  } else if (peorPrueba) {
    partes.push(`Seguimos reforzando ${peorPrueba.nombre} (nota ${peorPrueba.nota.toFixed(1)}/10).`);
  }
  partes.push(`Plan ${nivel.toLowerCase()} adaptado a tus marcas`);
  if (ajustes > 0) partes.push(`${ajustes} ejercicios ajustados esta semana`);
  if (rachaDias >= 3) partes.push(`racha de ${rachaDias} dias`);
  return partes.join('. ') + '.';
}

function etiquetaPilar(p) {
  return { FUERZA: 'Fuerza', RESISTENCIA: 'Resistencia', VELOCIDAD: 'Velocidad', MOVILIDAD: 'Movilidad', CORE: 'Core' }[p] || p;
}

function diaSemanaHoy() {
  const js = new Date().getDay();
  return js === 0 ? 7 : js;
}

function calcularRacha(fechasDistinct) {
  if (!fechasDistinct?.length) return 0;
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  const set = new Set(
    fechasDistinct.map((r) => {
      const d = new Date(r.d || r);
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

/** Aplica personalizacion sobre un plan semanal ya cargado. */
async function personalizarPlan(userId, idOposicion, plan, nivel) {
  if (!plan?.semana?.length) return plan;

  const [user] = await db.query(
    'SELECT genero, peso, altura FROM usuarios WHERE id_usuario = ?',
    [userId]
  );
  const genero = user?.[0]?.genero || 'HOMBRE';

  const marcas = await MarcasPerfilService.obtenerMarcasPorPrueba(userId, idOposicion);
  const pruebasConNota = [];
  for (const m of marcas) {
    const nota = await BaremoService.calcularNotaPrueba(
      m.id_pruebas_oficiales,
      genero,
      m.valord_record
    );
    if (nota == null) continue;
    pruebasConNota.push({
      nombre: m.nombre_prueba,
      nota: Number(nota),
      pilar: pilarDePrueba(m.nombre_prueba)
    });
  }

  const analisis = analizarPilares(pruebasConNota);
  const peorPrueba = pruebasConNota.length
    ? [...pruebasConNota].sort((a, b) => a.nota - b.nota)[0]
    : null;

  const [[{ sesionesSemana }]] = await db.query(
    `SELECT COUNT(*) AS sesionesSemana FROM historial_sesiones
     WHERE usuarios_id_usuario = ? AND tipo_rutina = 'OPO'
       AND YEARWEEK(fecha_entreno, 1) = YEARWEEK(CURDATE(), 1)`,
    [userId]
  );
  const [fechasRows] = await db.query(
    `SELECT DISTINCT DATE(fecha_entreno) AS d FROM historial_sesiones
     WHERE usuarios_id_usuario = ? ORDER BY d DESC LIMIT 90`,
    [userId]
  );
  const rachaDias = calcularRacha(fechasRows);
  const diasTranscurridosSemana = diaSemanaHoy();

  let ajustes = 0;
  const ctxBase = {
    pilaresDebiles: analisis.debiles,
    pilaresFuertes: analisis.fuertes,
    nivel: nivel || 'INTERMEDIO',
    sesionesSemana: Number(sesionesSemana || 0),
    diasTranscurridosSemana,
    diasPlanSemana: plan.dias_por_semana || plan.semana.length,
    rachaDias
  };

  const semana = plan.semana.map((dia) => {
    const ejercicios = (dia.ejercicios || []).map((ej) => {
      const adj = ajustarEjercicio(ej, { ...ctxBase, enfoqueSesion: dia.enfoque });
      if (adj.personalizado) ajustes += 1;
      return adj;
    });
    return { ...dia, ejercicios };
  });

  const personalizacion = {
    resumen: construirResumen({
      analisis,
      nivel: nivel || 'INTERMEDIO',
      ajustes,
      rachaDias,
      peorPrueba
    }),
    pilares_debiles: analisis.debiles.map((p) => ({
      pilar: p.pilar,
      etiqueta: etiquetaPilar(p.pilar),
      notaMedia: Number(p.notaMedia.toFixed(2)),
      pruebas: p.pruebas
    })),
    pilares_fuertes: analisis.fuertes.map((p) => ({
      pilar: p.pilar,
      etiqueta: etiquetaPilar(p.pilar),
      notaMedia: Number(p.notaMedia.toFixed(2)),
      pruebas: p.pruebas
    })),
    ajustes_aplicados: ajustes,
    nivel_usado: nivel || 'INTERMEDIO',
    racha_dias: rachaDias,
    sesiones_semana: Number(sesionesSemana || 0)
  };

  const sesion_hoy = semana.find((s) => s.es_hoy) || null;
  const hoy = diaSemanaHoy();
  const proxima =
    semana.find((s) => !s.completada && s.dia_semana >= hoy) ||
    semana.find((s) => !s.completada) ||
    null;

  return {
    ...plan,
    semana,
    sesion_hoy,
    proxima_sesion: proxima,
    personalizacion
  };
}

module.exports = {
  pilarDePrueba,
  analizarPilares,
  factorEjercicio,
  ajustarEjercicio,
  construirResumen,
  personalizarPlan,
  FACTOR_NIVEL,
  UMBRAL_DEBIL,
  UMBRAL_FUERTE
};
