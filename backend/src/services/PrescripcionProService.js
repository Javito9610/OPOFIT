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
function objetivoDe(pilar, posicion) {
  const p = String(pilar || '').toUpperCase();
  if (p === 'RESISTENCIA' || p === 'CARDIO') return 'resistencia';
  if (p === 'MOVILIDAD') return 'resistencia';
  if (p === 'VELOCIDAD') return posicion <= 2 ? 'fuerza' : 'hipertrofia';
  if (posicion === 1) return 'fuerza';
  if (posicion >= 4) return 'resistencia';
  return 'hipertrofia';
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
  const objetivo = objetivoDe(ej.pilar || ej.categoria, ctx.posicion || 1);
  const meta = META_POR_PATRON[patron] || META_POR_PATRON.SQUAT;
  const carga = CARGA_RELATIVA[objetivo] || CARGA_RELATIVA.hipertrofia;

  // Movilidad / pliometría / sprint: no aplica % de 1RM.
  const sinCarga = ['MOB', 'PLYO', 'SPRINT', 'AGI', 'LOCO', 'ANTI_EXT'].includes(patron);

  return {
    ...prescripcion,
    patron_movimiento: patron,
    objetivo,
    tempo: meta.tempo,
    rpe_objetivo: meta.rpe[objetivo] ?? meta.rpe.hipertrofia,
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
