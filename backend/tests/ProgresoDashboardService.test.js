jest.mock('../src/config/db');
jest.mock('../src/services/RutinasService');
jest.mock('../src/services/PlanesService');
jest.mock('../src/services/PremiumService');
jest.mock('../src/services/RankingService');

const db = require('../src/config/db');
const RutinaService = require('../src/services/RutinasService');
const PlanesService = require('../src/services/PlanesService');
const PremiumService = require('../src/services/PremiumService');
const RankingService = require('../src/services/RankingService');

const ProgresoService = require('../src/services/ProgresoService');
const DashboardService = require('../src/services/DashboardService');

describe('ProgresoService.esMejorMarca', () => {
  test('null/undefined siempre mejor', () => {
    expect(ProgresoService.esMejorMarca('press', 10, null)).toBe(true);
  });

  test('carrera/sprint: menor es mejor', () => {
    expect(ProgresoService.esMejorMarca('Carrera 100m', 11, 12)).toBe(true);
    expect(ProgresoService.esMejorMarca('Sprint', 13, 12)).toBe(false);
    expect(ProgresoService.esMejorMarca('Trote rodaje', 25, 30)).toBe(true);
    expect(ProgresoService.esMejorMarca('Natacion 50m', 40, 50)).toBe(true);
  });

  test('press / pesas: mayor es mejor', () => {
    expect(ProgresoService.esMejorMarca('Press banca', 100, 90)).toBe(true);
    expect(ProgresoService.esMejorMarca('Press banca', 80, 90)).toBe(false);
  });
});

describe('ProgresoService.registrarEntreno', () => {
  let conn;
  beforeEach(() => {
    jest.clearAllMocks();
    conn = {
      query: jest.fn(),
      beginTransaction: jest.fn(),
      commit: jest.fn(),
      rollback: jest.fn(),
      release: jest.fn()
    };
    db.getConnection.mockResolvedValue(conn);
  });

  test('inserta historial y resultados', async () => {
    // detectarRecords -> usa db.query no conn.query; para cada ejercicio una query
    db.query.mockResolvedValueOnce([[]]); // sin marca previa
    conn.query
      .mockResolvedValueOnce([{ insertId: 7 }]) // INSERT historial
      .mockResolvedValueOnce([{ affectedRows: 1 }]); // INSERT resultado
    const r = await ProgresoService.registrarEntreno({
      userId: 1,
      tipoRutina: 'OPO',
      idRutina: 1,
      duracion: 30,
      ejercicios: [{ id_ejercicio: 1, valor: 10 }]
    });
    expect(r.success).toBe(true);
    expect(r.idHistorial).toBe(7);
    expect(r.recordsRotos).toHaveLength(1);
  });

  test('rollback ante error', async () => {
    db.query.mockResolvedValueOnce([[]]);
    conn.query.mockRejectedValueOnce(new Error('FK fail'));
    await expect(
      ProgresoService.registrarEntreno({
        userId: 1,
        tipoRutina: 'OPO',
        idRutina: 1,
        duracion: 30,
        ejercicios: [{ id_ejercicio: 1, valor: 10 }]
      })
    ).rejects.toThrow('FK fail');
    expect(conn.rollback).toHaveBeenCalled();
  });
});

describe('DashboardService.calcularRacha', () => {
  test('Lista vacia -> 0', () => {
    expect(DashboardService.calcularRacha([])).toBe(0);
    expect(DashboardService.calcularRacha(null)).toBe(0);
  });

  test('Hoy entrenado -> 1', () => {
    const hoy = new Date();
    expect(DashboardService.calcularRacha([{ d: hoy }])).toBe(1);
  });

  test('Solo ayer pero no hoy -> 0', () => {
    const ayer = new Date();
    ayer.setDate(ayer.getDate() - 1);
    expect(DashboardService.calcularRacha([{ d: ayer }])).toBe(0);
  });

  test('Hoy + ayer + antier -> 3', () => {
    const fechas = [0, 1, 2].map((i) => {
      const d = new Date();
      d.setDate(d.getDate() - i);
      return { d };
    });
    expect(DashboardService.calcularRacha(fechas)).toBe(3);
  });

  test('Hoy + hace 3 dias (gap) -> 1', () => {
    const hoy = new Date();
    const hace3 = new Date();
    hace3.setDate(hace3.getDate() - 3);
    expect(DashboardService.calcularRacha([{ d: hoy }, { d: hace3 }])).toBe(1);
  });
});

describe('DashboardService.obtenerResumen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    PremiumService.getEstadoPremium.mockResolvedValue({ esPremium: false });
    PlanesService.obtenerPlanSemanal.mockResolvedValue(null);
    RankingService.posicionUsuario.mockResolvedValue({ posicion: 1, total: 10, notaMedia: 7 });
  });

  test('devuelve estructura completa con datos minimos', async () => {
    db.query
      .mockResolvedValueOnce([[{ sesiones: 2, minutos: 60 }]]) // semana
      .mockResolvedValueOnce([[{ sesiones: 20 }]]) // total
      .mockResolvedValueOnce([[]]) // fechas
      .mockResolvedValueOnce([[]]) // ultima sesion
      .mockResolvedValueOnce([[]]) // ultimo sim
      .mockResolvedValueOnce([[{ nombre: 'Opo 1' }]]); // opo nombre
    RutinaService.calcularNotaYNivel.mockResolvedValue({
      nivelSugerido: 'BASICO',
      notaMedia: '5.00',
      pruebasFaltantes: 0,
      pruebasCompletadas: 3,
      totalPruebas: 3,
      genero: 'HOMBRE'
    });
    db.query.mockResolvedValueOnce([[]]); // grafica semanal
    const r = await DashboardService.obtenerResumen(1, 1);
    expect(r.oposicionNombre).toBe('Opo 1');
    expect(r.sesionesSemana).toBe(2);
    expect(r.minutosSemana).toBe(60);
    expect(r.graficaSemanal).toHaveLength(7);
  });
});
