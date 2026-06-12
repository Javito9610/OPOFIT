/**
 * Test suite de la nueva capa pro de IA generadora.
 * Valida 4 servicios + la integración con EjercicioInteligenteService.
 */
const Periodizacion = require('../src/services/PeriodizacionService');
const Patron = require('../src/services/PatronMovimientoService');
const PrescripcionPro = require('../src/services/PrescripcionProService');
const Calentamiento = require('../src/services/CalentamientoService');
const Progresion = require('../src/services/ProgresionService');
const { aplicarInteligencia } = require('../src/services/EjercicioInteligenteService');

describe('PeriodizacionService — mesociclo 3:1', () => {
  test('semana 1 = MEV vol 1.00', () => {
    const p = Periodizacion.plantillaSemana(1);
    expect(p.fase).toBe('MEV');
    expect(p.vol).toBe(1.00);
    expect(p.deload).toBe(false);
  });

  test('semana 4 = DELOAD con vol 0.60', () => {
    const p = Periodizacion.plantillaSemana(4);
    expect(p.fase).toBe('DELOAD');
    expect(p.deload).toBe(true);
    expect(p.vol).toBeLessThan(0.8);
  });

  test('semanaDelMesociclo devuelve un valor 1..4', () => {
    for (let seed = 0; seed < 10; seed++) {
      const w = Periodizacion.semanaDelMesociclo(seed);
      expect(w).toBeGreaterThanOrEqual(1);
      expect(w).toBeLessThanOrEqual(4);
    }
  });

  test('ajustarPorSemana en semana 1 no cambia las series', () => {
    const rx = { series: 4, rpe_objetivo: 8 };
    const out = Periodizacion.ajustarPorSemana(rx, 1);
    expect(out.series).toBe(4);
    expect(out.rpe_objetivo).toBe(8);
    expect(out.deload).toBe(false);
  });

  test('ajustarPorSemana en semana 4 (deload) baja series', () => {
    const rx = { series: 5, rpe_objetivo: 8 };
    const out = Periodizacion.ajustarPorSemana(rx, 4);
    expect(out.series).toBeLessThan(5);
    expect(out.deload).toBe(true);
    expect(out.rpe_objetivo).toBeLessThan(8);
  });
});

describe('PatronMovimientoService — clasificación', () => {
  test('press banca → PUSH_H', () => {
    expect(Patron.clasificar({ nombre: 'Press banca plano con barra' })).toBe('PUSH_H');
  });
  test('press militar → PUSH_V', () => {
    expect(Patron.clasificar({ nombre: 'Press militar con barra' })).toBe('PUSH_V');
  });
  test('dominada → PULL_V', () => {
    expect(Patron.clasificar({ nombre: 'Dominadas pronas' })).toBe('PULL_V');
  });
  test('remo → PULL_H', () => {
    expect(Patron.clasificar({ nombre: 'Remo con barra' })).toBe('PULL_H');
  });
  test('sentadilla → SQUAT', () => {
    expect(Patron.clasificar({ nombre: 'Sentadilla con barra' })).toBe('SQUAT');
  });
  test('peso muerto → HINGE', () => {
    expect(Patron.clasificar({ nombre: 'Peso muerto convencional' })).toBe('HINGE');
  });
  test('hip thrust → HINGE', () => {
    expect(Patron.clasificar({ nombre: 'Hip thrust con barra' })).toBe('HINGE');
  });
  test('zancada → LUNGE', () => {
    expect(Patron.clasificar({ nombre: 'Zancada caminando con mancuernas' })).toBe('LUNGE');
  });
  test('sprint → SPRINT', () => {
    expect(Patron.clasificar({ nombre: 'Sprint 60 m x 6' })).toBe('SPRINT');
  });
  test('conos → AGI', () => {
    expect(Patron.clasificar({ nombre: 'Conos en T' })).toBe('AGI');
  });
  test('box jump → PLYO', () => {
    expect(Patron.clasificar({ nombre: 'Box jump 60 cm' })).toBe('PLYO');
  });
  test('plancha → ANTI_EXT', () => {
    expect(Patron.clasificar({ nombre: 'Plancha frontal' })).toBe('ANTI_EXT');
  });
  test('farmer walk → CARRY', () => {
    expect(Patron.clasificar({ nombre: 'Farmer walk con mancuernas' })).toBe('CARRY');
  });
  test('antagonistas correctos', () => {
    expect(Patron.antagonista('PUSH_H')).toBe('PULL_H');
    expect(Patron.antagonista('SQUAT')).toBe('HINGE');
  });
  test('auditarBalance detecta 3 push sin pull', () => {
    const r = Patron.auditarBalance(['PUSH_H', 'PUSH_V', 'PUSH_H']);
    expect(r.warnings).toContain('PUSH_SIN_PULL');
  });
  test('auditarBalance ok cuando hay equilibrio', () => {
    const r = Patron.auditarBalance(['PUSH_H', 'PULL_H', 'SQUAT', 'HINGE']);
    expect(r.ok).toBe(true);
  });
});

describe('PrescripcionProService — tempo, RPE, rango RM', () => {
  test('press banca como 1er ejercicio → fuerza, tempo 3-0-X-0, RPE 8', () => {
    const out = PrescripcionPro.enriquecer(
      { series: 4, repeticiones: 5, descanso: 150, unidad: 'reps' },
      { nombre: 'Press banca plano con barra', pilar: 'FUERZA' },
      { posicion: 1, weekIdx: 1 }
    );
    expect(out.objetivo).toBe('fuerza');
    expect(out.tempo).toBe('3-0-X-0');
    expect(out.rpe_objetivo).toBe(8);
    expect(out.rango_rm).toContain('80');
  });

  test('curl accesorio (posición 5) → resistencia o hipertrofia, RPE moderado', () => {
    const out = PrescripcionPro.enriquecer(
      { series: 3, repeticiones: 12, descanso: 60, unidad: 'reps' },
      { nombre: 'Curl con mancuernas', pilar: 'FUERZA', grupo_muscular: 'Brazos' },
      { posicion: 5 }
    );
    expect(out.rpe_objetivo).toBeLessThanOrEqual(7);
  });

  test('movilidad no devuelve rango_rm', () => {
    const out = PrescripcionPro.enriquecer(
      { series: 2, repeticiones: 45, descanso: 30, unidad: 's' },
      { nombre: 'Movilidad de cadera', pilar: 'MOVILIDAD' },
      { posicion: 1 }
    );
    expect(out.rango_rm).toBeNull();
  });
});

describe('CalentamientoService — RAMP + cooldown', () => {
  test('FUERZA tiene 4 bloques RAMP', () => {
    const w = Calentamiento.calentamiento('FUERZA');
    expect(w.length).toBe(4);
    expect(w.map((b) => b.rampa)).toEqual(['R', 'A', 'M', 'P']);
  });

  test('VELOCIDAD tiene aproximaciones de sprint', () => {
    const w = Calentamiento.calentamiento('VELOCIDAD');
    expect(w.some((b) => /progresiv|30 m/i.test(b.titulo))).toBe(true);
  });

  test('cada warmup tiene duracion total razonable (2-15 min)', () => {
    for (const enfoque of ['FUERZA', 'VELOCIDAD', 'RESISTENCIA', 'CORE']) {
      const total = Calentamiento.duracionTotalWarmup(enfoque);
      expect(total).toBeGreaterThanOrEqual(120);
      expect(total).toBeLessThanOrEqual(900);
    }
  });

  test('cooldown FUERZA incluye respiración nasal', () => {
    const c = Calentamiento.vueltaACalma('FUERZA');
    expect(c.some((b) => /respiracion|respiración/i.test(b.titulo))).toBe(true);
  });
});

describe('ProgresionService — regresiones / progresiones', () => {
  test('dominada tiene regresión asistida y progresión con lastre', () => {
    expect(Progresion.regresionDe('Dominadas pronas').nombre).toMatch(/asistid/i);
    expect(Progresion.progresionDe('Dominadas pronas').nombre).toMatch(/lastr/i);
  });

  test('flexión tiene regresión inclinada', () => {
    expect(Progresion.regresionDe('Flexiones estándar').nombre).toMatch(/inclinad/i);
  });

  test('sentadilla tiene progresión frontal', () => {
    expect(Progresion.progresionDe('Sentadilla con barra').nombre).toMatch(/frontal/i);
  });

  test('ejercicio desconocido devuelve null', () => {
    expect(Progresion.regresionDe('No existe este ejercicio')).toBeNull();
  });
});

describe('Integración con aplicarInteligencia', () => {
  test('una dominada sale con patrón, tempo, RPE, fase y regresión', () => {
    const ej = aplicarInteligencia(
      { nombre: 'Dominadas pronas', pilar: 'FUERZA', grupo_muscular: 'Espalda' },
      { seed: 1, weekIdx: 1, posicion: 1 }
    );
    expect(ej.patron_movimiento).toBe('PULL_V');
    expect(ej.tempo).toBeTruthy();
    expect(ej.rpe_objetivo).toBeGreaterThanOrEqual(6);
    expect(ej.fase_mesociclo).toBe('MEV');
    expect(ej.regresion).toBeTruthy();
    expect(ej.progresion).toBeTruthy();
  });

  test('semana de deload reduce series respecto a semana base', () => {
    const base = aplicarInteligencia(
      { nombre: 'Press banca plano con barra', pilar: 'FUERZA' },
      { seed: 7, weekIdx: 1, posicion: 1 }
    );
    const deload = aplicarInteligencia(
      { nombre: 'Press banca plano con barra', pilar: 'FUERZA' },
      { seed: 7, weekIdx: 4, posicion: 1 }
    );
    expect(deload.deload).toBe(true);
    expect(deload.series).toBeLessThanOrEqual(base.series);
    expect(deload.rpe_objetivo).toBeLessThanOrEqual(base.rpe_objetivo);
  });
});
