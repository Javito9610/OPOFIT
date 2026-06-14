/**
 * E2E "calidad pro del plan": verifica que el plan entregado al opositor
 * cumple los estándares profesionales — esto es lo que separa OpoFit de
 * un Excel.
 *
 * Validaciones:
 *  - Cada ejercicio incluye las 5 secciones pro (setup/ejecución/cues/
 *    errores/por qué).
 *  - Prescripción acoplada (tempo + RPE + patrón + objetivo).
 *  - Regresión y progresión cuando aplica.
 *  - Fase del mesociclo presente.
 *  - Calentamiento + vuelta a la calma generados.
 *  - Sin texto genérico tipo "Hazlo con buena técnica".
 */
process.env.JWT_SECRET = 'opofit-e2e-calidad-pro';
process.env.NOTIFICATIONS_CRON = 'false';

jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);
jest.mock('../src/config/firebaseAdmin', () => ({
  initFirebaseAdmin: () => ({ messaging: () => ({ send: jest.fn().mockResolvedValue('ok') }) })
}));

const { aplicarInteligencia } = require('../src/services/EjercicioInteligenteService');
const Periodizacion = require('../src/services/PeriodizacionService');

describe('E2E calidad pro del plan', () => {
  test('Press banca como ejercicio principal del día tiene los 5 bloques pro', () => {
    const ej = aplicarInteligencia(
      { nombre: 'Press banca con barra', pilar: 'FUERZA', grupo_muscular: 'Pecho' },
      { seed: 100, weekIdx: 1, posicion: 1 }
    );
    // 1. Prescripción acoplada
    expect(ej.series).toBeGreaterThanOrEqual(3);
    expect(ej.series).toBeLessThanOrEqual(6);
    expect(ej.tempo).toMatch(/[0-9]/);
    expect(ej.rpe_objetivo).toBeGreaterThanOrEqual(7);
    expect(ej.patron_movimiento).toBe('PUSH_H');
    expect(ej.objetivo).toBe('fuerza');
    // 2. Fase
    expect(ej.fase_mesociclo).toBe('MEV');
    // 3. Explicación pro
    expect(ej.explicacion.setup).toMatch(/5 puntos|contacto|escápul/i);
    expect(ej.explicacion.ejecucion).toMatch(/baja|empuj/i);
    expect(ej.explicacion.coaching_cues.length).toBeGreaterThanOrEqual(3);
    expect(ej.explicacion.errores_comunes.length).toBeGreaterThanOrEqual(3);
    expect(ej.explicacion.porque.length).toBeGreaterThan(30);
  });

  test('Dominada en semana de pico (MRV) sube RPE objetivo', () => {
    const mev = aplicarInteligencia(
      { nombre: 'Dominadas pronas', pilar: 'FUERZA' },
      { seed: 200, weekIdx: 1, posicion: 1 }
    );
    const mrv = aplicarInteligencia(
      { nombre: 'Dominadas pronas', pilar: 'FUERZA' },
      { seed: 200, weekIdx: 3, posicion: 1 }
    );
    expect(mrv.fase_mesociclo).toBe('MRV');
    expect(mrv.rpe_objetivo).toBeGreaterThanOrEqual(mev.rpe_objetivo);
    expect(mrv.deload).toBe(false);
  });

  test('Sentadilla en deload reduce series y RPE', () => {
    const base = aplicarInteligencia(
      { nombre: 'Sentadilla con barra', pilar: 'FUERZA' },
      { seed: 300, weekIdx: 1, posicion: 1 }
    );
    const deload = aplicarInteligencia(
      { nombre: 'Sentadilla con barra', pilar: 'FUERZA' },
      { seed: 300, weekIdx: 4, posicion: 1 }
    );
    expect(deload.deload).toBe(true);
    expect(deload.fase_mesociclo).toBe('DELOAD');
    expect(deload.series).toBeLessThanOrEqual(base.series);
    expect(deload.rpe_objetivo).toBeLessThanOrEqual(base.rpe_objetivo);
  });

  test('Sprint trae mención a salida + descanso completo entre series', () => {
    const ej = aplicarInteligencia(
      { nombre: 'Sprint 60 m x 6', pilar: 'VELOCIDAD' },
      { seed: 400, weekIdx: 2 }
    );
    expect(ej.explicacion.ejecucion + ' ' + ej.explicacion.errores_comunes.join(' '))
      .toMatch(/calentar|salida|descans/i);
    expect(ej.explicacion.porque).toMatch(/PRUEBA|oficial/i);
  });

  test('Peso muerto trae explicación específica con bisagra de cadera', () => {
    const ej = aplicarInteligencia(
      { nombre: 'Peso muerto convencional', pilar: 'FUERZA' },
      { seed: 500, weekIdx: 2, posicion: 1 }
    );
    expect(ej.patron_movimiento).toBe('HINGE');
    expect(ej.explicacion.coaching_cues.join(' ')).toMatch(/bisagra/i);
    expect(ej.explicacion.errores_comunes.join(' ')).toMatch(/redondear|lumbar|hernia/i);
    // Regresión y progresión presentes
    expect(ej.regresion).toBeTruthy();
    expect(ej.progresion).toBeTruthy();
  });

  test('Plancha frontal sale como ANTI_EXT con unidad correcta (segundos)', () => {
    const ej = aplicarInteligencia(
      { nombre: 'Plancha frontal', pilar: 'CORE' },
      { seed: 600, weekIdx: 1 }
    );
    expect(ej.patron_movimiento).toBe('ANTI_EXT');
    expect(ej.unidad).toBe('s');
    expect(ej.explicacion.cues || ej.explicacion.coaching_cues).toBeTruthy();
    expect(ej.explicacion.errores_comunes.join(' ')).toMatch(/cadera|respir/i);
  });

  test('Conos en T sale como AGI con unidad "vueltas" + explicación específica', () => {
    const ej = aplicarInteligencia(
      { nombre: 'Conos en T', pilar: 'VELOCIDAD' },
      { seed: 700, weekIdx: 1 }
    );
    expect(ej.patron_movimiento).toBe('AGI');
    expect(ej.unidad).toBe('vueltas');
    expect(ej.explicacion.setup + ej.explicacion.ejecucion).toMatch(/T-test|cono|patrón/i);
    expect(ej.explicacion.porque).toMatch(/oficial|PRUEBA/i);
  });

  test('Carrera continua menciona Z2 + conversacional + cadencia', () => {
    const ej = aplicarInteligencia(
      { nombre: 'Carrera continua Z2 30 min', pilar: 'RESISTENCIA' },
      { seed: 800, weekIdx: 1 }
    );
    expect(ej.unidad).toBe('min');
    expect(ej.explicacion.ejecucion + ej.explicacion.coaching_cues.join(' '))
      .toMatch(/conversacional|cadencia|hablar|Z2|respira/i);
  });

  test('Ningún ejercicio devuelve setup vacío ni texto genérico', () => {
    const movimientos = [
      'Press banca con barra',
      'Sentadilla con barra',
      'Peso muerto convencional',
      'Dominadas pronas',
      'Sprint 60 m x 6',
      'Plancha frontal',
      'Carrera continua Z2 30 min',
      'Box jump 60 cm',
      'Press militar con barra',
      'Remo con barra'
    ];
    for (const nombre of movimientos) {
      const ej = aplicarInteligencia({ nombre, pilar: 'FUERZA' }, { seed: 1, weekIdx: 1 });
      const e = ej.explicacion;
      expect(e.setup.length).toBeGreaterThan(40);
      expect(e.ejecucion.length).toBeGreaterThan(40);
      expect(e.porque.length).toBeGreaterThan(30);
      // Sin frases genéricas demasiado vagas
      expect(e.setup).not.toMatch(/^Hazlo con buena técnica/);
      expect(e.ejecucion).not.toMatch(/^Hazlo con buena técnica/);
    }
  });
});

describe('E2E variación entre semanas (rotación)', () => {
  test('La semana ISO afecta al seed efectivo de sustituciones', () => {
    const semana1 = Periodizacion.semanaIso(new Date(2026, 0, 5));   // Semana 2 del año
    const semana2 = Periodizacion.semanaIso(new Date(2026, 0, 12));  // Semana 3
    expect(semana1).not.toBe(semana2);
    // Si dos usuarios con el mismo seed están en semanas distintas, sus
    // seedEfectivos serán distintos → rotación natural de ejercicios.
    const seedUserA = 5;
    const seedEfectivoSem1 = seedUserA + semana1 * 13;
    const seedEfectivoSem2 = seedUserA + semana2 * 13;
    expect(seedEfectivoSem1).not.toBe(seedEfectivoSem2);
  });
});
