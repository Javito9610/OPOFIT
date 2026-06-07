const { clampReps } = require('../src/services/PlanPersonalizadorService');

function esperadoMin(reps) {
  return Math.min(90, Math.max(5, Math.round(reps)));
}
function esperadoSeg(reps) {
  return Math.min(7200, Math.max(10, Math.round(reps)));
}
function esperadoKm(reps) {
  return Math.min(50, Math.max(1, Math.round(reps * 10) / 10));
}
function esperadoM(reps) {
  return Math.min(10000, Math.max(50, Math.round(reps)));
}
function esperadoReps(reps) {
  // clampReps cap por defecto: 25 reps (límite "realista" en fuerza/calistenia).
  // Si reps >= 90 entra en esPrescripcionMaxima y devuelve 99 (AMRAP).
  if (reps >= 90) return 99;
  return Math.min(25, Math.max(1, Math.round(reps)));
}

const casosMin = [];
for (let i = -50; i <= 150; i += 1) {
  casosMin.push([i, 'min', 'Carrera continua', esperadoMin(i)]);
}

const casosSeg = [];
for (let i = -20; i <= 8000; i += 27) {
  casosSeg.push([i, 's', 'Plancha seg', esperadoSeg(i)]);
}

const casosKm = [];
for (let i = -5; i <= 60; i += 1) {
  casosKm.push([i, 'km', 'Rodaje km', esperadoKm(i)]);
}

const casosM = [];
for (let i = -100; i <= 12000; i += 40) {
  casosM.push([i, 'm', 'Sprint 100 m', esperadoM(i)]);
}

const casosReps = [];
for (let i = -10; i <= 80; i += 1) {
  casosReps.push([i, 'reps', 'Flexiones', esperadoReps(i)]);
}

const casosNombreMin = [];
for (let i = 0; i <= 120; i += 2) {
  casosNombreMin.push([i, null, 'Tabata 4 min', esperadoMin(i)]);
}

describe('clampReps coherencia parametrica', () => {
  test('AMRAP/máx no se escala a repeticiones absurdas', () => {
    expect(clampReps(99, 'reps', 'Dominadas asistidas con goma')).toBe(99);
    expect(clampReps(120, 'reps', 'Flexiones')).toBe(99);
  });
  test.each(casosMin)('min reps=%i -> %i', (reps, unidad, nombre, esperado) => {
    expect(clampReps(reps, unidad, nombre)).toBe(esperado);
  });

  test.each(casosSeg)('seg reps=%i -> %i', (reps, unidad, nombre, esperado) => {
    expect(clampReps(reps, unidad, nombre)).toBe(esperado);
  });

  test.each(casosKm)('km reps=%i -> %s', (reps, unidad, nombre, esperado) => {
    expect(clampReps(reps, unidad, nombre)).toBe(esperado);
  });

  test.each(casosM)('metros reps=%i -> %i', (reps, unidad, nombre, esperado) => {
    expect(clampReps(reps, unidad, nombre)).toBe(esperado);
  });

  test.each(casosReps)('reps reps=%i -> %i', (reps, unidad, nombre, esperado) => {
    expect(clampReps(reps, unidad, nombre)).toBe(esperado);
  });

  test.each(casosNombreMin)('nombre min reps=%i -> %i', (reps, unidad, nombre, esperado) => {
    expect(clampReps(reps, unidad, nombre)).toBe(esperado);
  });
});
