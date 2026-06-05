const {
  pilarDePrueba,
  analizarPilares,
  factorEjercicio,
  ajustarEjercicio,
  construirResumen,
  normalizarPrescripcionRealista,
  UMBRAL_DEBIL
} = require('../src/services/PlanPersonalizadorService');

describe('PlanPersonalizadorService.pilarDePrueba', () => {
  test('mapea pruebas tipicas', () => {
    expect(pilarDePrueba('Carrera 100m')).toBe('VELOCIDAD');
    expect(pilarDePrueba('Carrera 1000m')).toBe('RESISTENCIA');
    expect(pilarDePrueba('Dominadas')).toBe('FUERZA');
    expect(pilarDePrueba('Natación 100m')).toBe('RESISTENCIA');
  });
});

describe('PlanPersonalizadorService.analizarPilares', () => {
  test('detecta pilares debiles y fuertes', () => {
    const pruebas = [
      { nombre: '100m', nota: 9, pilar: 'VELOCIDAD' },
      { nombre: '1000m', nota: 4, pilar: 'RESISTENCIA' },
      { nombre: 'Dominadas', nota: 3.5, pilar: 'FUERZA' }
    ];
    const r = analizarPilares(pruebas);
    expect(r.debiles.length).toBeGreaterThanOrEqual(2);
    expect(r.fuertes.some((p) => p.pilar === 'VELOCIDAD')).toBe(true);
    expect(r.peor.pilar).toBe('FUERZA');
  });
});

describe('PlanPersonalizadorService.factorEjercicio', () => {
  const debiles = [{ pilar: 'RESISTENCIA', notaMedia: 4, pruebas: ['1000m'] }];
  const fuertes = [{ pilar: 'VELOCIDAD', notaMedia: 9, pruebas: ['100m'] }];

  test('aumenta volumen en pilar debil', () => {
    const r = factorEjercicio({
      pilarEjercicio: 'RESISTENCIA',
      enfoqueSesion: 'RESISTENCIA',
      pilaresDebiles: debiles,
      pilaresFuertes: fuertes,
      nivel: 'INTERMEDIO',
      sesionesSemana: 2,
      diasTranscurridosSemana: 3,
      diasPlanSemana: 4,
      rachaDias: 2
    });
    expect(r.factor).toBeGreaterThan(1.1);
    expect(r.motivo).toContain('prioridad');
  });

  test('reduce volumen en pilar fuerte', () => {
    const r = factorEjercicio({
      pilarEjercicio: 'VELOCIDAD',
      enfoqueSesion: 'VELOCIDAD',
      pilaresDebiles: debiles,
      pilaresFuertes: fuertes,
      nivel: 'INTERMEDIO',
      sesionesSemana: 3,
      diasTranscurridosSemana: 4,
      diasPlanSemana: 4,
      rachaDias: 1
    });
    expect(r.factor).toBeLessThan(1.0);
  });
});

describe('PlanPersonalizadorService.ajustarEjercicio', () => {
  test('modifica series y marca personalizado', () => {
    const ej = {
      nombre: 'Carrera continua',
      pilar: 'RESISTENCIA',
      series: 3,
      repeticiones: 20,
      unidad: 'min'
    };
    const adj = ajustarEjercicio(ej, {
      enfoqueSesion: 'RESISTENCIA',
      pilaresDebiles: [{ pilar: 'RESISTENCIA', notaMedia: 4, pruebas: ['1000m'] }],
      pilaresFuertes: [],
      nivel: 'INTERMEDIO',
      sesionesSemana: 2,
      diasTranscurridosSemana: 3,
      diasPlanSemana: 4,
      rachaDias: 0
    });
    expect(adj.series).toBeGreaterThanOrEqual(ej.series);
    expect(adj.series_base).toBe(3);
    expect(adj.personalizado).toBe(true);
    expect(adj.motivo_ajuste).toBeTruthy();
  });
});

describe('PlanPersonalizadorService.construirResumen', () => {
  test('genera texto legible', () => {
    const txt = construirResumen({
      analisis: {
        debiles: [{ pilar: 'RESISTENCIA', notaMedia: 4.2, pruebas: ['Carrera 1000m'] }],
        fuertes: []
      },
      nivel: 'INTERMEDIO',
      ajustes: 8,
      rachaDias: 4,
      peorPrueba: null
    });
    expect(txt).toContain('debil');
    expect(txt).toContain('8 ejercicios');
    expect(txt).toContain('racha');
  });
});

describe('PlanPersonalizadorService.normalizarPrescripcionRealista', () => {
  test('limita reps absurdas en wrist curl', () => {
    const ej = normalizarPrescripcionRealista({
      nombre: 'Reverse wrist curl',
      pilar: 'FUERZA',
      series: 3,
      repeticiones: 77,
      unidad: 'reps'
    });
    expect(ej.repeticiones).toBeLessThanOrEqual(20);
    expect(ej.series).toBeLessThanOrEqual(4);
  });
});

describe('umbrales coherentes', () => {
  test('nota bajo umbral es debil', () => {
    const r = analizarPilares([{ nombre: 'X', nota: UMBRAL_DEBIL - 0.5, pilar: 'FUERZA' }]);
    expect(r.debiles).toHaveLength(1);
  });
});
