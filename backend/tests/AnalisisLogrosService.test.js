/**
 * Tests unitarios de la analitica avanzada (AnalisisService) y la gamificacion
 * (LogrosService). Son funciones puras: no necesitan BD ni mocks.
 */
const A = require('../src/services/AnalisisService');
const L = require('../src/services/LogrosService');

describe('AnalisisService.fcMaxEstimada', () => {
  test('usa la FCmax indicada si es valida', () => {
    expect(A.fcMaxEstimada({ fcMax: 190 })).toBe(190);
  });
  test('estima por edad con Tanaka (208 - 0.7*edad)', () => {
    expect(A.fcMaxEstimada({ edad: 30 })).toBe(187);
  });
  test('devuelve null sin datos validos', () => {
    expect(A.fcMaxEstimada({})).toBeNull();
    expect(A.fcMaxEstimada({ fcMax: 0, edad: 0 })).toBeNull();
  });
});

describe('AnalisisService.zonasFC', () => {
  test('genera 5 zonas crecientes y la ultima llega a FCmax', () => {
    const r = A.zonasFC({ fcMax: 200 });
    expect(r.fcMax).toBe(200);
    expect(r.zonas).toHaveLength(5);
    expect(r.zonas[0].min).toBe(100); // 50%
    expect(r.zonas[4].max).toBe(200); // ultima incluye FCmax
    for (let i = 1; i < 5; i++) {
      expect(r.zonas[i].min).toBeGreaterThan(r.zonas[i - 1].min);
    }
  });
  test('lanza si no hay forma de saber FCmax', () => {
    expect(() => A.zonasFC({})).toThrow('FCMAX_REQUERIDA');
  });
});

describe('AnalisisService.zonaDeFc', () => {
  test('clasifica correctamente segun %FCmax', () => {
    const fm = 200;
    expect(A.zonaDeFc(110, fm)).toBe(1); // 55%
    expect(A.zonaDeFc(130, fm)).toBe(2); // 65%
    expect(A.zonaDeFc(150, fm)).toBe(3); // 75%
    expect(A.zonaDeFc(170, fm)).toBe(4); // 85%
    expect(A.zonaDeFc(195, fm)).toBe(5); // 97%
  });
  test('pulsos muy bajos caen en Z1 y por encima de FCmax en Z5', () => {
    expect(A.zonaDeFc(80, 200)).toBe(1);
    expect(A.zonaDeFc(210, 200)).toBe(5);
  });
});

describe('AnalisisService.distribucionZonas', () => {
  test('reparte el tiempo por zonas y suma 100%', () => {
    const muestras = [110, 110, 150, 195]; // Z1, Z1, Z3, Z5
    const r = A.distribucionZonas(muestras, { fcMax: 200, intervaloSeg: 1 });
    expect(r.muestras).toBe(4);
    expect(r.zonas[0].segundos).toBe(2);
    expect(r.zonas[0].porcentaje).toBe(50);
    expect(r.zonas[2].segundos).toBe(1);
    expect(r.zonas[4].segundos).toBe(1);
    const totalPct = r.zonas.reduce((a, z) => a + z.porcentaje, 0);
    expect(Math.round(totalPct)).toBe(100);
  });
  test('respeta el intervalo de muestreo en segundos', () => {
    const r = A.distribucionZonas([150, 150, 150], { fcMax: 200, intervaloSeg: 5 });
    expect(r.zonas[2].segundos).toBe(15);
  });
  test('ignora valores no numericos', () => {
    const r = A.distribucionZonas([150, null, 'x', -5], { fcMax: 200 });
    expect(r.muestras).toBe(1);
  });
});

describe('AnalisisService.estimarTSS', () => {
  test('1h a intensidad de umbral da ~TSS cercano a 100', () => {
    // FCmedia tal que reserva ~ 0.95 -> IF^2*100 ~ 90
    const r = A.estimarTSS({ durSeg: 3600, avgHr: 180, fcReposo: 60, fcMax: 187 });
    expect(r.tss).toBeGreaterThan(70);
    expect(r.tss).toBeLessThan(110);
  });
  test('mas duracion => mas carga', () => {
    const a = A.estimarTSS({ durSeg: 1800, avgHr: 150, fcReposo: 60, fcMax: 190 });
    const b = A.estimarTSS({ durSeg: 3600, avgHr: 150, fcReposo: 60, fcMax: 190 });
    expect(b.tss).toBeGreaterThan(a.tss);
  });
  test('valida entradas', () => {
    expect(() => A.estimarTSS({ durSeg: 0, avgHr: 150, fcMax: 190 })).toThrow('DURACION_INVALIDA');
    expect(() => A.estimarTSS({ durSeg: 600, avgHr: 0, fcMax: 190 })).toThrow('FC_MEDIA_INVALIDA');
    expect(() => A.estimarTSS({ durSeg: 600, avgHr: 150 })).toThrow('FCMAX_REQUERIDA');
    expect(() => A.estimarTSS({ durSeg: 600, avgHr: 150, fcMax: 50, fcReposo: 60 })).toThrow('FCMAX_MENOR_QUE_REPOSO');
  });
});

describe('AnalisisService.predecirTiempo (Riegel)', () => {
  test('predice un tiempo mayor para una distancia mayor', () => {
    // 5 km en 25:00 -> 10 km debe ser > 50:00 (no escala lineal)
    const r = A.predecirTiempo({ distanciaM: 5000, tiempoSeg: 1500, objetivoM: 10000 });
    expect(r.tiempoEstimadoSeg).toBeGreaterThan(3000);
    expect(r.tiempoEstimadoSeg).toBeLessThan(3200);
    expect(r.tiempoEstimado).toMatch(/^\d+:\d{2}/);
    expect(r.ritmo).toMatch(/\/km$/);
  });
  test('misma distancia => mismo tiempo', () => {
    const r = A.predecirTiempo({ distanciaM: 5000, tiempoSeg: 1500, objetivoM: 5000 });
    expect(r.tiempoEstimadoSeg).toBe(1500);
  });
  test('valida entradas', () => {
    expect(() => A.predecirTiempo({ distanciaM: 0, tiempoSeg: 100, objetivoM: 100 })).toThrow('DISTANCIA_INVALIDA');
    expect(() => A.predecirTiempo({ distanciaM: 100, tiempoSeg: 0, objetivoM: 100 })).toThrow('TIEMPO_INVALIDO');
    expect(() => A.predecirTiempo({ distanciaM: 100, tiempoSeg: 100, objetivoM: 0 })).toThrow('OBJETIVO_INVALIDO');
  });
});

describe('AnalisisService.vo2maxCooper', () => {
  test('2400m en 12 min ~ 42.4 ml/kg/min', () => {
    expect(A.vo2maxCooper(2400)).toBeCloseTo(42.4, 0);
  });
  test('valida la distancia', () => {
    expect(() => A.vo2maxCooper(0)).toThrow('DISTANCIA_INVALIDA');
  });
});

describe('AnalisisService.formatoTiempo', () => {
  test('formatea con y sin horas', () => {
    expect(A.formatoTiempo(90)).toBe('1:30');
    expect(A.formatoTiempo(3661)).toBe('1:01:01');
    expect(A.formatoTiempo(-5)).toBe('0:00');
  });
});

describe('LogrosService.recordsPorEjercicio', () => {
  test('se queda con el mejor valor (mayor=mejor por defecto)', () => {
    const recs = L.recordsPorEjercicio([
      { idEjercicio: 1, nombre: 'Flexiones', valor: 20 },
      { idEjercicio: 1, nombre: 'Flexiones', valor: 35 },
      { idEjercicio: 1, nombre: 'Flexiones', valor: 30 }
    ]);
    expect(recs).toHaveLength(1);
    expect(recs[0].valor).toBe(35);
  });
  test('respeta menorEsMejor (tiempos)', () => {
    const recs = L.recordsPorEjercicio([
      { idEjercicio: 2, nombre: '1000m', valor: 240, menorEsMejor: true },
      { idEjercicio: 2, nombre: '1000m', valor: 215, menorEsMejor: true }
    ]);
    expect(recs[0].valor).toBe(215);
  });
  test('ignora valores invalidos y agrupa por ejercicio', () => {
    const recs = L.recordsPorEjercicio([
      { idEjercicio: 1, valor: 'x' },
      { idEjercicio: 1, valor: 10 },
      { idEjercicio: 2, valor: 5 }
    ]);
    expect(recs).toHaveLength(2);
  });
});

describe('LogrosService.calcularRachas', () => {
  test('cuenta racha actual y maxima', () => {
    const hoy = new Date('2026-01-10T12:00:00Z');
    const fechas = ['2026-01-08', '2026-01-09', '2026-01-10', '2026-01-05', '2026-01-04'];
    const r = L.calcularRachas(fechas, hoy);
    expect(r.actual).toBe(3); // 8,9,10
    expect(r.maxima).toBe(3);
    expect(r.diasActivos).toBe(5);
  });
  test('racha actual 0 si el ultimo entreno fue hace 2+ dias', () => {
    const hoy = new Date('2026-01-10T12:00:00Z');
    const r = L.calcularRachas(['2026-01-01', '2026-01-02'], hoy);
    expect(r.actual).toBe(0);
    expect(r.maxima).toBe(2);
  });
  test('cuenta racha si el ultimo fue ayer', () => {
    const hoy = new Date('2026-01-10T12:00:00Z');
    const r = L.calcularRachas(['2026-01-08', '2026-01-09'], hoy);
    expect(r.actual).toBe(2);
  });
  test('lista vacia => ceros', () => {
    expect(L.calcularRachas([])).toEqual({ actual: 0, maxima: 0, diasActivos: 0 });
  });
  test('deduplica el mismo dia', () => {
    const hoy = new Date('2026-01-10T12:00:00Z');
    const r = L.calcularRachas(['2026-01-10', '2026-01-10', '2026-01-10'], hoy);
    expect(r.actual).toBe(1);
    expect(r.diasActivos).toBe(1);
  });
});

describe('LogrosService.medallas', () => {
  test('desbloquea segun umbrales', () => {
    const meds = L.medallas({ sesiones: 12, distanciaKm: 5, rachaMaxima: 0, desnivelM: 0 });
    const porId = Object.fromEntries(meds.map((m) => [m.id, m]));
    expect(porId.primer_entreno.desbloqueada).toBe(true);
    expect(porId.sesiones_10.desbloqueada).toBe(true);
    expect(porId.sesiones_50.desbloqueada).toBe(false);
    expect(porId.distancia_10.desbloqueada).toBe(false);
    expect(porId.distancia_10.progreso).toBe(0.5);
  });
  test('progreso acotado a [0,1]', () => {
    const meds = L.medallas({ sesiones: 999 });
    expect(meds.every((m) => m.progreso >= 0 && m.progreso <= 1)).toBe(true);
  });
});

describe('LogrosService.construirLogros', () => {
  test('combina rachas, records y medallas', () => {
    const hoy = new Date('2026-01-10T12:00:00Z');
    const out = L.construirLogros({
      stats: { sesiones: 10, distanciaKm: 12, desnivelM: 0 },
      fechasSesiones: ['2026-01-08', '2026-01-09', '2026-01-10'],
      registros: [{ idEjercicio: 1, nombre: 'Dominadas', valor: 12 }],
      hoy
    });
    expect(out.rachas.actual).toBe(3);
    expect(out.records).toHaveLength(1);
    expect(out.medallasTotales).toBeGreaterThan(0);
    expect(out.medallasDesbloqueadas).toBeGreaterThanOrEqual(2);
    // la racha maxima (3) debe desbloquear "racha_3"
    const racha3 = out.medallas.find((m) => m.id === 'racha_3');
    expect(racha3.desbloqueada).toBe(true);
  });
});
