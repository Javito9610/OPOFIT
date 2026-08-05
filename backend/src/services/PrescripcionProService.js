/**
 * PrescripcionProService — añade metadatos profesionales a la prescripción
 * (tempo, RPE objetivo, intención de la serie, nota de carga). NO reescribe
 * series/reps que ya genera EjercicioInteligenteService — solo enriquece.
 *
 * Modelos aplicados:
 *  - Tempo notation (3-1-X-0): excéntrica/pausa/concéntrica/pausa final.
 *  - RPE de Mike Tuchscherer (1-10) por patrón y objetivo.
 *  - Carga relativa (% 1RM aproximado) cuando aplica.
 *
 * Esto convierte una prescripción "3×8 reps 90s descanso" en
 * "3×8 reps @ tempo 3-0-1-0, RPE 7, ~75% 1RM, 90s descanso" — el mismo
 * detalle que Caliber, Future o cualquier plan de coach 1-a-1.
 */

const Patron = require('./PatronMovimientoService');

const META_POR_PATRON = {
  SQUAT:   { tempo: '3-1-X-0', rpe: { fuerza: 8, hipertrofia: 7, resistencia: 6 } },
  HINGE:   { tempo: '2-1-1-0', rpe: { fuerza: 8, hipertrofia: 7, resistencia: 6 } },
  LUNGE:   { tempo: '2-1-1-0', rpe: { fuerza: 7, hipertrofia: 7, resistencia: 6 } },
  PUSH_H:  { tempo: '3-0-X-0', rpe: { fuerza: 8, hipertrofia: 7, resistencia: 6 } },
  PUSH_V:  { tempo: '2-0-X-0', rpe: { fuerza: 8, hipertrofia: 7, resistencia: 6 } },
  PULL_H:  { tempo: '2-1-1-0', rpe: { fuerza: 8, hipertrofia: 7, resistencia: 6 } },
  PULL_V:  { tempo: '2-1-1-0', rpe: { fuerza: 8, hipertrofia: 7, resistencia: 7 } },
  CARRY:   { tempo: 'continuo',  rpe: { fuerza: 8, hipertrofia: 7, resistencia: 6 } },
  ROT:     { tempo: '2-0-1-0', rpe: { fuerza: 7, hipertrofia: 7, resistencia: 6 } },
  ANTI_EXT:{ tempo: 'isométrico', rpe: { fuerza: 8, hipertrofia: 7, resistencia: 6 } },
  LOCO:    { tempo: 'rítmico',    rpe: { fuerza: 7, hipertrofia: 6, resistencia: 5 } },
  PLYO:    { tempo: 'explosivo',  rpe: { fuerza: 7, hipertrofia: 7, resistencia: 6 } },
  SPRINT:  { tempo: 'máximo',     rpe: { fuerza: 9, hipertrofia: 8, resistencia: 7 } },
  AGI:     { tempo: 'rápido',     rpe: { fuerza: 7, hipertrofia: 7, resistencia: 6 } },
  MOB:     { tempo: 'lento',      rpe: { fuerza: 3, hipertrofia: 3, resistencia: 3 } }
};

const CARGA_RELATIVA = {
  fuerza:      { rangoRm: '80-90% 1RM', nota: 'Carga alta, técnica perfecta. Deja 1-3 reps en reserva.' },
  hipertrofia: { rangoRm: '65-80% 1RM', nota: 'Carga moderada. RIR 1-2 (deja 1-2 reps en el tanque).' },
  resistencia: { rangoRm: '50-65% 1RM', nota: 'Carga ligera. Prioriza el ritmo y la respiración.' }
};

/**
 * Determina el objetivo de entrenamiento del ejercicio en base a:
 *  - Pilar (FUERZA/RESISTENCIA/VELOCIDAD/CORE/MOVILIDAD)
 *  - Posición en la sesión (1º = más pesado/exigente, último = accesorio o core)
 */
function objetivoDe(pilar, posicion, reps, unidad) {
  const p = String(pilar || '').toUpperCase();
  if (p === 'RESISTENCIA' || p === 'CARDIO' || p === 'MOVILIDAD') return 'resistencia';
  // MANDA EL Nº DE REPETICIONES (principio clásico de fuerza): 1-5 reps = fuerza
  // máxima (80-90%), 6-12 = hipertrofia (65-80%), 13+ = resistencia muscular.
  // Antes se decidía solo por la POSICIÓN → un 3×5 salía como hipertrofia. Mal.
  const u = String(unidad || 'reps').toLowerCase();
  const r = Number(reps);
  if ((u === 'reps' || u === 'rep') && Number.isFinite(r) && r > 0) {
    if (r <= 6) return 'fuerza';        // 1-6 reps = fuerza (cargas altas)
    if (r <= 12) return 'hipertrofia';   // 7-12 = hipertrofia
    return 'resistencia';                // 13+ = resistencia muscular
  }
  if (p === 'VELOCIDAD') return posicion <= 2 ? 'fuerza' : 'hipertrofia';
  if (posicion === 1) return 'fuerza';
  if (posicion >= 4) return 'resistencia';
  return 'hipertrofia';
}

function normalizarNombre(s) {
  return String(s || '').normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
}

/** RPE del cardio según la INTENSIDAD que indica el nombre (no todo el cardio
 *  es RPE 5). Intervalos/VO2máx/series/umbral = muy duro; Z2/rodaje/suave = fácil. */
function rpeCardio(nombre) {
  const n = normalizarNombre(nombre);
  // Muy duro (VO2máx / intervalos / protocolos): Helgerud 4x4, series, sprints…
  if (/vo2|9[0-9]\s*%|8[5-9]\s*%|helgerud|\bz5\b|\bz4\b|\bmax\b|m[aá]xim|sprint|\bseries\b|interval|hiit|tabata|\d+\s*x\s*\d+|\br[0-9]/.test(n)) return 9;
  // Umbral / tempo / cambios de ritmo.
  if (/\bz3\b|umbral|fartlek|cambios de ritmo|\btempo\b|cuesta|progresi|moderad/.test(n)) return 7;
  return 5; // Z1-Z2 / rodaje / continua / suave / recuperacion
}

/** Valores de descanso REALISTAS y redondeados (nada de "114 s"). */
const DESCANSOS_ESTANDAR = [20, 30, 45, 60, 75, 90, 120, 150, 180, 240, 300];
function snapDescanso(v) {
  const n = Number(v);
  if (!Number.isFinite(n) || n <= 0) return null;
  return DESCANSOS_ESTANDAR.reduce((best, s) => (Math.abs(s - n) < Math.abs(best - n) ? s : best), DESCANSOS_ESTANDAR[0]);
}

/** Descanso COHERENTE: 1) respeta el que viene en el nombre ("R 2'", "rec 90s"),
 *  2) si no, redondea a un valor estándar, 3) si no hay, usa el estándar por objetivo. */
function descansoCoherente(descanso, nombre, objetivo, patron) {
  const n = normalizarNombre(nombre);
  const min = n.match(/\b(?:r|rec|recuperaci[oó]n|descanso|rest)\s*[:=]?\s*(\d+)\s*['′m]/);
  if (min) return Number(min[1]) * 60;
  const seg = n.match(/\b(?:r|rec|recuperaci[oó]n|descanso|rest)\s*[:=]?\s*(\d+)\s*["″s]/);
  if (seg) return Number(seg[1]);
  const snapped = snapDescanso(descanso);
  if (snapped) return snapped;
  if (patron === 'MOB') return 30;
  const estandar = { fuerza: 180, hipertrofia: 90, resistencia: 60 };
  return estandar[objetivo] || 90;
}

/**
 * Enriquece una prescripción base con tempo, RPE y nota de carga.
 *
 * @param {object} prescripcion {series, repeticiones, descanso, unidad}
 * @param {object} ej {nombre, pilar, grupo_muscular, ...}
 * @param {object} ctx {posicion, weekIdx}
 * @returns prescripcion + {patron, tempo, rpe_objetivo, nota_carga, rangoRm, objetivo}
 */
function enriquecer(prescripcion, ej, ctx = {}) {
  const patron = Patron.clasificar(ej);
  const objetivo = objetivoDe(
    ej.pilar || ej.categoria,
    ctx.posicion || 1,
    prescripcion.repeticiones,
    prescripcion.unidad
  );
  const meta = META_POR_PATRON[patron] || META_POR_PATRON.SQUAT;
  const carga = CARGA_RELATIVA[objetivo] || CARGA_RELATIVA.hipertrofia;

  // Detección ROBUSTA de cardio: por patrón, por pilar RESISTENCIA/VELOCIDAD o
  // por el nombre (metros, km, VO2, carrera, natación, series...). El clasificador
  // de patrón no siempre pilla "4×1000m VO2máx" → aquí sí.
  const nn = normalizarNombre(ej.nombre);
  const pil = String(ej.pilar || ej.categoria || '').toUpperCase();
  const unidad = String(prescripcion.unidad || '').toLowerCase();
  const esCardio = ['LOCO', 'SPRINT', 'AGI', 'PLYO'].includes(patron)
    || pil === 'RESISTENCIA'
    || ['min', 'km'].includes(unidad)
    || /\bvo2|\bkm\b|\d+\s*m\b|carrera|correr|trote|rodaje|fartlek|nataci|\bcrol\b|\bseries\b|interval|cuesta|bici|bicicleta|ciclismo|el[íi]ptica|remo\s*erg|assault|echo\s*bike|air\s*bike|ski\s*erg|\bcinta\b|tapiz|treadmill|step\s*mill|stairmaster|escaladora|helgerud|comba|salto/.test(nn);

  // Movilidad / pliometría / sprint / cardio: no aplica % de 1RM.
  const sinCarga = ['MOB', 'PLYO', 'SPRINT', 'AGI', 'LOCO', 'ANTI_EXT'].includes(patron) || esCardio;

  // RPE: en cardio/intervalos manda la INTENSIDAD del nombre (no todo es RPE 5).
  const rpe = esCardio ? rpeCardio(ej.nombre) : (meta.rpe[objetivo] ?? meta.rpe.hipertrofia);

  // Descanso coherente (respeta el del nombre, redondea a valor pro).
  const descanso = descansoCoherente(prescripcion.descanso, ej.nombre, objetivo, patron);

  // Tempo: NO aplica a cardio (una bici o un rodaje no tienen tempo de barra).
  const tempo = esCardio ? null : meta.tempo;

  return {
    ...prescripcion,
    descanso,
    patron_movimiento: patron,
    objetivo,
    tempo,
    rpe_objetivo: rpe,
    rango_rm: sinCarga ? null : carga.rangoRm,
    nota_carga: sinCarga ? null : carga.nota
  };
}

module.exports = {
  META_POR_PATRON,
  CARGA_RELATIVA,
  objetivoDe,
  enriquecer
};
