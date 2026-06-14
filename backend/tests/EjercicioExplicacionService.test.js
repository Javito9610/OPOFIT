/**
 * Tests del motor pro de explicaciones de ejercicios.
 * Antes: 1 frase suelta tipo "Escápulas retraídas, toque pecho controlado.".
 * Ahora: 5 secciones jerarquizadas (Setup / Ejecución / Cues / Errores / Por qué).
 */
const Explicacion = require('../src/services/EjercicioExplicacionService');
const { aplicarInteligencia } = require('../src/services/EjercicioInteligenteService');

describe('EjercicioExplicacionService — 5 secciones pro', () => {
  test('dominada estricta sale con setup específico y mención a oposición', () => {
    const r = Explicacion.explicar({ nombre: 'Dominadas pronas', pilar: 'FUERZA' });
    expect(r.setup).toMatch(/prono|palmas/i);
    expect(r.porque).toMatch(/Polic|Guardia|Mossos|oposicion|oposición/i);
    expect(r.coaching_cues.length).toBeGreaterThanOrEqual(3);
    expect(r.errores_comunes.length).toBeGreaterThanOrEqual(3);
  });

  test('dominada asistida tiene setup distinto a dominada estricta', () => {
    const estricta = Explicacion.explicar({ nombre: 'Dominadas pronas' });
    const asistida = Explicacion.explicar({ nombre: 'Dominada asistida con banda' });
    expect(estricta.setup).not.toBe(asistida.setup);
    expect(asistida.setup).toMatch(/banda|resistencia/i);
  });

  test('press banca lleva los 5 puntos de contacto', () => {
    const r = Explicacion.explicar({ nombre: 'Press banca con barra' });
    expect(r.setup).toMatch(/5 puntos|contacto|cabeza|hombros|glúteos/i);
    expect(r.patron_movimiento).toBe('PUSH_H');
  });

  test('sentadilla menciona "rodilla con el pie" y "valgo"', () => {
    const r = Explicacion.explicar({ nombre: 'Sentadilla con barra' });
    expect(r.coaching_cues.some((c) => /rodilla|pie/i.test(c))).toBe(true);
    expect(r.errores_comunes.some((e) => /valgo|hacia dentro/i.test(e))).toBe(true);
  });

  test('peso muerto incluye explícitamente "bisagra de cadera"', () => {
    const r = Explicacion.explicar({ nombre: 'Peso muerto convencional' });
    expect(r.coaching_cues.some((c) => /bisagra/i.test(c))).toBe(true);
    expect(r.errores_comunes.some((e) => /redondear|lumbar|hernia/i.test(e))).toBe(true);
  });

  test('sprint menciona técnica de carrera y descanso completo', () => {
    const r = Explicacion.explicar({ nombre: 'Sprint 60 m x 6' });
    expect(r.ejecucion + r.coaching_cues.join(' ')).toMatch(/rodilla alta|brazo|talón/i);
    expect(r.errores_comunes.join(' ')).toMatch(/calentar|descans/i);
  });

  test('conos en T menciona el patrón oficial', () => {
    const r = Explicacion.explicar({ nombre: 'Conos en T' });
    expect(r.setup + r.ejecucion).toMatch(/T-test|cono|patrón/i);
  });

  test('cada explicación tiene las 5 secciones no vacías', () => {
    const movimientos = [
      'Sentadilla con barra',
      'Peso muerto convencional',
      'Press militar con barra',
      'Remo con barra',
      'Dominadas pronas',
      'Plancha frontal',
      'Box jump 60 cm',
      'Carrera continua Z2 30 min',
      'Farmer walk con mancuernas',
      'Zancada con mancuernas'
    ];
    for (const nombre of movimientos) {
      const r = Explicacion.explicar({ nombre });
      expect(r.setup.length).toBeGreaterThan(40);
      expect(r.ejecucion.length).toBeGreaterThan(40);
      expect(r.coaching_cues.length).toBeGreaterThanOrEqual(2);
      expect(r.errores_comunes.length).toBeGreaterThanOrEqual(2);
      expect(r.porque.length).toBeGreaterThan(30);
    }
  });

  test('explicarPlano genera string concatenado con todas las secciones', () => {
    const txt = Explicacion.explicarPlano({ nombre: 'Sentadilla con barra' });
    expect(txt).toMatch(/Setup:/);
    expect(txt).toMatch(/Ejecución:/);
    expect(txt).toMatch(/entrenador|Claves/);
    expect(txt).toMatch(/Errores/);
    expect(txt).toMatch(/Por qué/);
  });
});

describe('Integración con aplicarInteligencia', () => {
  test('un ejercicio del plan sale con campo `explicacion` con 5 secciones', () => {
    const ej = aplicarInteligencia(
      { nombre: 'Dominadas pronas', pilar: 'FUERZA', grupo_muscular: 'Espalda' },
      { seed: 1 }
    );
    expect(ej.explicacion).toBeDefined();
    expect(ej.explicacion.setup).toBeTruthy();
    expect(ej.explicacion.ejecucion).toBeTruthy();
    expect(ej.explicacion.coaching_cues.length).toBeGreaterThanOrEqual(3);
    expect(ej.explicacion.errores_comunes.length).toBeGreaterThanOrEqual(3);
    expect(ej.explicacion.porque).toBeTruthy();
  });

  test('press banca y dominada tienen explicaciones DISTINTAS (no genéricas)', () => {
    const press = aplicarInteligencia(
      { nombre: 'Press banca con barra', pilar: 'FUERZA' },
      { seed: 1 }
    );
    const dom = aplicarInteligencia(
      { nombre: 'Dominadas pronas', pilar: 'FUERZA' },
      { seed: 1 }
    );
    expect(press.explicacion.setup).not.toBe(dom.explicacion.setup);
    expect(press.explicacion.porque).not.toBe(dom.explicacion.porque);
  });
});
