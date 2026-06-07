jest.mock('../src/config/db');
const db = require('../src/config/db');
const GpsService = require('../src/services/GpsService');
const RutinaPersService = require('../src/services/RutinaPersService');
const RutinaService = require('../src/services/RutinasService');

describe('GpsService', () => {
  beforeEach(() => jest.clearAllMocks());

  test('rechaza payload sin id', async () => {
    await expect(GpsService.guardar(1, { type: 'RUN' })).rejects.toThrow('Datos de actividad incompletos');
  });

  test('rechaza payload sin endedAtMs', async () => {
    await expect(GpsService.guardar(1, { id: 'a', type: 'RUN' })).rejects.toThrow('Datos de actividad incompletos');
  });

  test('normaliza tipo invalido a RUN', async () => {
    db.query
      .mockResolvedValueOnce([{ insertId: 1 }])
      .mockResolvedValueOnce([[{ id_actividad: 99 }]]);
    const r = await GpsService.guardar(1, {
      id: 'abc',
      type: 'AVION',
      endedAtMs: Date.now()
    });
    const insertParams = db.query.mock.calls[0][1];
    expect(insertParams[2]).toBe('RUN');
    expect(r.idActividad).toBe(99);
  });

  test('lista actividades', async () => {
    db.query.mockResolvedValueOnce([[
      {
        id_actividad: 1,
        uuid_local: 'a',
        tipo: 'RUN',
        iniciada_en: 100,
        finalizada_en: 200,
        duracion_seg: 100,
        distancia_m: 1000,
        ritmo_medio_spkm: 300,
        velocidad_media_mps: 3,
        desnivel_pos_m: 10,
        altitud_min_m: 0,
        altitud_max_m: 100
      }
    ]]);
    const r = await GpsService.listar(1);
    expect(r).toHaveLength(1);
    expect(r[0].id).toBe('a');
    expect(r[0].distanceM).toBe(1000);
  });

  test('detalle devuelve null si no existe', async () => {
    db.query.mockResolvedValueOnce([[undefined]]);
    const r = await GpsService.detalle(1, 'x');
    expect(r).toBeNull();
  });

  test('borrar devuelve true cuando affectedRows > 0', async () => {
    db.query.mockResolvedValueOnce([{ affectedRows: 1 }]);
    expect(await GpsService.borrar(1, 'a')).toBe(true);
    db.query.mockResolvedValueOnce([{ affectedRows: 0 }]);
    expect(await GpsService.borrar(1, 'a')).toBe(false);
  });
});

describe('RutinaPersService', () => {
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

  test('crearRutinaPropia inserta detalle ejercicios', async () => {
    // El servicio hace SELECT ejercicios + INSERT detalle por cada ejercicio
    // (para aplicar la inteligencia de prescripción al fallback).
    conn.query
      .mockResolvedValueOnce([[]]) // SELECT existente: no existe
      .mockResolvedValueOnce([{ insertId: 99 }]) // INSERT rutina
      .mockResolvedValueOnce([[{ id_ejercicio: 1, nombre: 'Sentadilla', pilar: 'FUERZA' }]]) // SELECT ej1
      .mockResolvedValueOnce([{ affectedRows: 1 }]) // INSERT detalle ej1
      .mockResolvedValueOnce([[{ id_ejercicio: 2, nombre: 'Press banca', pilar: 'FUERZA' }]]) // SELECT ej2
      .mockResolvedValueOnce([{ affectedRows: 1 }]); // INSERT detalle ej2
    const id = await RutinaPersService.crearRutinaPropia(1, 'Rutina X', [
      { id_ejercicio: 1, series: 3, repeticiones: 10 },
      { id_ejercicio: 2, series: 4, repeticiones: 8 }
    ]);
    expect(id).toBe(99);
    expect(conn.commit).toHaveBeenCalled();
  });

  test('crearRutinaPropia rechaza nombre duplicado', async () => {
    conn.query.mockResolvedValueOnce([[{ id_rutina_pers: 7 }]]);
    await expect(
      RutinaPersService.crearRutinaPropia(1, 'X', [{ id_ejercicio: 1, series: 1, repeticiones: 1 }])
    ).rejects.toThrow('Ya tienes una rutina con este nombre');
    expect(conn.rollback).toHaveBeenCalled();
  });

  test('eliminarRutina NOT_FOUND', async () => {
    conn.query.mockResolvedValueOnce([[]]);
    try {
      await RutinaPersService.eliminarRutina(1, 5);
      throw new Error('debio fallar');
    } catch (e) {
      expect(e.code).toBe('NOT_FOUND');
    }
  });

  test('eliminarRutina ok desvincula historial', async () => {
    conn.query
      .mockResolvedValueOnce([[{ id_rutina_pers: 5 }]]) // existe
      .mockResolvedValueOnce([{ affectedRows: 1 }]) // update historial
      .mockResolvedValueOnce([{ affectedRows: 0 }]) // delete compartidas
      .mockResolvedValueOnce([{ affectedRows: 2 }]) // delete detalle
      .mockResolvedValueOnce([{ affectedRows: 1 }]); // delete rutina
    const r = await RutinaPersService.eliminarRutina(1, 5);
    expect(r).toEqual({ success: true });
  });
});

describe('RutinaService.inferUnidad', () => {
  test('detecta unidades por nombre', () => {
    expect(RutinaService.inferUnidad('Trote 5 min')).toBe('min');
    expect(RutinaService.inferUnidad('Sprint 30 segundos')).toBe('s');
    expect(RutinaService.inferUnidad('Carrera 3km')).toBe('km');
    expect(RutinaService.inferUnidad('Natacion 100')).toBe('m');
    expect(RutinaService.inferUnidad('Press banca')).toBe('reps');
    expect(RutinaService.inferUnidad('200 m lisos')).toBe('m');
  });
});

describe('RutinaService.calcularNotaYNivel', () => {
  beforeEach(() => jest.clearAllMocks());

  test('USER_NOT_FOUND si no existe usuario', async () => {
    db.query.mockResolvedValueOnce([[]]);
    const r = await RutinaService.calcularNotaYNivel(1, 1);
    expect(r.error).toBe('USER_NOT_FOUND');
  });

  test('marca pruebas faltantes', async () => {
    db.query
      .mockResolvedValueOnce([[{ genero: 'HOMBRE' }]]) // user
      .mockResolvedValueOnce([[{ total_pruebas: 3 }]]) // total
      .mockResolvedValueOnce([[]]); // sin marcas via obtenerMarcasPorPrueba (1 query interna)
    const r = await RutinaService.calcularNotaYNivel(1, 1);
    expect(r.pruebasFaltantes).toBe(3);
  });
});
