jest.mock('../src/config/db');
jest.mock('../src/services/RutinasService');
jest.mock('../src/services/BaremoService');

const db = require('../src/config/db');
const RutinaService = require('../src/services/RutinasService');
const BaremoService = require('../src/services/BaremoService');
const MarcasPerfilService = require('../src/services/MarcasPerfilService');

describe('MarcasPerfilService.esMejorMarca', () => {
  test('Sin marca anterior siempre es mejor', () => {
    expect(MarcasPerfilService.esMejorMarca(1, 10, null)).toBe(true);
    expect(MarcasPerfilService.esMejorMarca(0, 10, undefined)).toBe(true);
  });

  test('Cuando menor es mejor', () => {
    expect(MarcasPerfilService.esMejorMarca(1, 11, 12)).toBe(true);
    expect(MarcasPerfilService.esMejorMarca(1, 13, 12)).toBe(false);
    expect(MarcasPerfilService.esMejorMarca(1, 12, 12)).toBe(false);
  });

  test('Cuando mayor es mejor', () => {
    expect(MarcasPerfilService.esMejorMarca(0, 20, 15)).toBe(true);
    expect(MarcasPerfilService.esMejorMarca(0, 10, 15)).toBe(false);
  });
});

describe('MarcasPerfilService.obtenerMarcasPorPrueba', () => {
  beforeEach(() => jest.clearAllMocks());

  test('deduplica por prueba (queda solo la mas reciente)', async () => {
    db.query.mockResolvedValueOnce([[
      { id_pruebas_oficiales: 1, valord_record: 10, fecha_logro: '2026-02-01' },
      { id_pruebas_oficiales: 1, valord_record: 12, fecha_logro: '2026-01-15' },
      { id_pruebas_oficiales: 2, valord_record: 5, fecha_logro: '2026-02-10' }
    ]]);
    const r = await MarcasPerfilService.obtenerMarcasPorPrueba(1, 1);
    expect(r).toHaveLength(2);
    expect(r[0].valord_record).toBe(10);
    expect(r[1].valord_record).toBe(5);
  });
});

describe('MarcasPerfilService.nivelProyectadoConSimulacro', () => {
  beforeEach(() => jest.clearAllMocks());

  test('basico cuando nota media baja', async () => {
    db.query
      .mockResolvedValueOnce([[{ genero: 'HOMBRE' }]]) // genero
      .mockResolvedValueOnce([[]]); // marcas vacias
    BaremoService.calcularNotaPrueba.mockResolvedValue(2);
    db.query
      .mockResolvedValueOnce([[{ mejor_si_es_menor: 0 }]]) // prueba existe
      .mockResolvedValueOnce([[{ total: 1 }]]); // total pruebas
    const r = await MarcasPerfilService.nivelProyectadoConSimulacro(1, 1, [
      { id_prueba: 1, valor: 5 }
    ]);
    expect(r.nivelSugerido).toBe('BASICO');
  });

  test('avanzado cuando media >= 8', async () => {
    db.query
      .mockResolvedValueOnce([[{ genero: 'HOMBRE' }]])
      .mockResolvedValueOnce([[]]);
    BaremoService.calcularNotaPrueba.mockResolvedValue(9);
    db.query
      .mockResolvedValueOnce([[{ mejor_si_es_menor: 0 }]])
      .mockResolvedValueOnce([[{ total: 1 }]]);
    const r = await MarcasPerfilService.nivelProyectadoConSimulacro(1, 1, [
      { id_prueba: 1, valor: 25 }
    ]);
    expect(r.nivelSugerido).toBe('AVANZADO');
  });

  test('intermedio entre 5 y 8', async () => {
    db.query
      .mockResolvedValueOnce([[{ genero: 'HOMBRE' }]])
      .mockResolvedValueOnce([[]]);
    BaremoService.calcularNotaPrueba.mockResolvedValue(6);
    db.query
      .mockResolvedValueOnce([[{ mejor_si_es_menor: 0 }]])
      .mockResolvedValueOnce([[{ total: 1 }]]);
    const r = await MarcasPerfilService.nivelProyectadoConSimulacro(1, 1, [
      { id_prueba: 1, valor: 15 }
    ]);
    expect(r.nivelSugerido).toBe('INTERMEDIO');
  });
});

describe('MarcasPerfilService.analizarMejorasTrasSimulacro', () => {
  beforeEach(() => jest.clearAllMocks());

  test('considera marca nueva como mejora', async () => {
    db.query
      .mockResolvedValueOnce([[{ genero: 'HOMBRE' }]]) // genero
      .mockResolvedValueOnce([[]]); // marcas actuales vacias
    RutinaService.calcularNotaYNivel.mockResolvedValue({
      nivelSugerido: 'BASICO',
      notaMedia: '3.00'
    });
    db.query.mockResolvedValueOnce([[
      { nombre_prueba: 'Press', mejor_si_es_menor: 0, unidad_entrada: 'reps' }
    ]]);
    BaremoService.calcularNotaPrueba.mockResolvedValue(7);
    const r = await MarcasPerfilService.analizarMejorasTrasSimulacro(1, 1, [
      { id_prueba: 1, valor: 18 }
    ]);
    expect(r.hayMejoras).toBe(true);
    expect(r.mejoras[0].esNueva).toBe(true);
    expect(r.mejoras[0].notaNueva).toBe(7);
  });

  test('descarta marca peor cuando ya existe una mejor', async () => {
    db.query
      .mockResolvedValueOnce([[{ genero: 'HOMBRE' }]])
      .mockResolvedValueOnce([[
        {
          id_pruebas_oficiales: 1,
          valord_record: 20, // ya tiene 20 reps, intentamos guardar 15
          fecha_logro: '2026-01-01',
          nombre_prueba: 'Press',
          mejor_si_es_menor: 0,
          unidad_entrada: 'reps'
        }
      ]]);
    RutinaService.calcularNotaYNivel.mockResolvedValue({
      nivelSugerido: 'INTERMEDIO',
      notaMedia: '6.00'
    });
    db.query.mockResolvedValueOnce([[
      { nombre_prueba: 'Press', mejor_si_es_menor: 0, unidad_entrada: 'reps' }
    ]]);
    const r = await MarcasPerfilService.analizarMejorasTrasSimulacro(1, 1, [
      { id_prueba: 1, valor: 15 }
    ]);
    expect(r.hayMejoras).toBe(false);
  });
});
