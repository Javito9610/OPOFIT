const {
  validarEjercicioPorNombre,
  validarDuracionMin
} = require('../src/utils/EntrenoValidator');

describe('EntrenoValidator', () => {
  test('rechaza reps decimales', () => {
    const r = validarEjercicioPorNombre('Dominadas pronas', 8.5);
    expect(r.ok).toBe(false);
    expect(r.codigo).toBe('NO_ENTERO');
  });

  test('acepta reps enteras razonables', () => {
    const r = validarEjercicioPorNombre('Flexiones', 25);
    expect(r.ok).toBe(true);
    expect(r.valor).toBe(25);
  });

  test('rechaza dominadas imposibles', () => {
    const r = validarEjercicioPorNombre('Dominadas', 50000);
    expect(r.ok).toBe(false);
  });

  test('valida carrera en km', () => {
    expect(validarEjercicioPorNombre('Carrera continua 5 km', 7.2).ok).toBe(true);
    expect(validarEjercicioPorNombre('Carrera continua 5 km', 0).ok).toBe(false);
    expect(validarEjercicioPorNombre('Carrera continua 5 km', 2000).ok).toBe(false);
  });

  test('valida tiempo en minutos', () => {
    expect(validarEjercicioPorNombre('HIIT 30:30 x 12', 25).ok).toBe(true);
    expect(validarEjercicioPorNombre('HIIT 30:30 x 12', -1).ok).toBe(false);
  });

  test('valida duracion sesion (en SEGUNDOS)', () => {
    // La duración viaja en segundos desde el fix de unidades:
    // 45 s y 900 s (15 min) son sesiones válidas; 0 no; >10 h imposible.
    expect(validarDuracionMin(45).ok).toBe(true);
    expect(validarDuracionMin(0).ok).toBe(false);
    expect(validarDuracionMin(900).ok).toBe(true);
    expect(validarDuracionMin(1800).ok).toBe(true);   // 30 min — el caso del bug
    expect(validarDuracionMin(40_000).ok).toBe(false); // > 10 h
  });
});
