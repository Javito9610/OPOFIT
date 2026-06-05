jest.mock('../src/services/GpsService');
jest.mock('../src/services/EjerciciosService');
jest.mock('../src/services/SimulacroService');
jest.mock('../src/services/PremiumService');
jest.mock('../src/services/MarcasPerfilService');

const GpsService = require('../src/services/GpsService');
const EjerciciosService = require('../src/services/EjerciciosService');
const SimulacroService = require('../src/services/SimulacroService');
const PremiumService = require('../src/services/PremiumService');
const MarcasPerfilService = require('../src/services/MarcasPerfilService');

const GpsController = require('../src/controllers/GpsController');
const EjerciciosController = require('../src/controllers/EjerciciosController');
const SimulacroController = require('../src/controllers/SimulacroController');
const { mockReq, mockRes } = require('./helpers/mockReqRes');

describe('GpsController', () => {
  beforeEach(() => jest.clearAllMocks());

  test('guardar 401 sin usuario', async () => {
    const req = mockReq({ usuario: null });
    const res = mockRes();
    await GpsController.guardar(req, res);
    expect(res.status).toHaveBeenCalledWith(401);
  });

  test('guardar 200', async () => {
    GpsService.guardar.mockResolvedValue({ idActividad: 5, uuid: 'abc' });
    const req = mockReq({ usuario: { id: 1 }, body: { type: 'RUN', id: 'abc' } });
    const res = mockRes();
    await GpsController.guardar(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: { idActividad: 5, uuid: 'abc' } });
  });

  test('guardar 400 si servicio rechaza', async () => {
    GpsService.guardar.mockRejectedValue(new Error('Datos incompletos'));
    const req = mockReq({ usuario: { id: 1 }, body: {} });
    const res = mockRes();
    await GpsController.guardar(req, res);
    expect(res.status).toHaveBeenCalledWith(400);
  });

  test('listar 200 con limit clamp', async () => {
    GpsService.listar.mockResolvedValue([]);
    const req = mockReq({ usuario: { id: 1 }, query: { limit: '999', offset: '-5' } });
    const res = mockRes();
    await GpsController.listar(req, res);
    expect(GpsService.listar).toHaveBeenCalledWith(1, { limit: 200, offset: 0 });
  });

  test('detalle 404 si no existe', async () => {
    GpsService.detalle.mockResolvedValue(null);
    const req = mockReq({ usuario: { id: 1 }, params: { uuid: 'x' } });
    const res = mockRes();
    await GpsController.detalle(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
  });

  test('borrar 200', async () => {
    GpsService.borrar.mockResolvedValue(true);
    const req = mockReq({ usuario: { id: 1 }, params: { uuid: 'x' } });
    const res = mockRes();
    await GpsController.borrar(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true });
  });

  test('borrar 404 si no existe', async () => {
    GpsService.borrar.mockResolvedValue(false);
    const req = mockReq({ usuario: { id: 1 }, params: { uuid: 'x' } });
    const res = mockRes();
    await GpsController.borrar(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
  });
});

describe('EjerciciosController', () => {
  beforeEach(() => jest.clearAllMocks());

  test('listar 200', async () => {
    EjerciciosService.listarTodos.mockResolvedValue([{ id_ejercicio: 1, nombre: 'PushUp' }]);
    const req = mockReq({ query: { categoria: 'FUERZA' } });
    const res = mockRes();
    await EjerciciosController.listarEjercicios(req, res);
    expect(res.json).toHaveBeenCalledWith({
      ok: true,
      data: [{ id_ejercicio: 1, nombre: 'PushUp' }]
    });
  });

  test('listar 500 si falla', async () => {
    EjerciciosService.listarTodos.mockRejectedValue(new Error('X'));
    const req = mockReq({ query: {} });
    const res = mockRes();
    await EjerciciosController.listarEjercicios(req, res);
    expect(res.status).toHaveBeenCalledWith(500);
  });
});

describe('SimulacroController', () => {
  beforeEach(() => jest.clearAllMocks());

  test('listarPruebas 200', async () => {
    SimulacroService.listarPruebas.mockResolvedValue([{ id_pruebas_oficiales: 1 }]);
    const req = mockReq({ usuario: { id: 1 }, params: { idOposicion: '1' } });
    const res = mockRes();
    await SimulacroController.listarPruebas(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: [{ id_pruebas_oficiales: 1 }] });
  });

  test('listarPruebas 404 si oposicion no existe', async () => {
    SimulacroService.listarPruebas.mockRejectedValue(new Error('OPOSICION_NOT_FOUND'));
    const req = mockReq({ usuario: { id: 1 }, params: { idOposicion: '99' } });
    const res = mockRes();
    await SimulacroController.listarPruebas(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
  });

  test('guardar 400 si datos incompletos', async () => {
    const req = mockReq({ usuario: { id: 1 }, body: { idOposicion: 1, resultados: [] } });
    const res = mockRes();
    await SimulacroController.guardar(req, res);
    expect(res.status).toHaveBeenCalledWith(400);
  });

  test('guardar 200 con resultados', async () => {
    SimulacroService.guardarSimulacro.mockResolvedValue({ idSimulacro: 7, notaMedia: '7.50' });
    const req = mockReq({
      usuario: { id: 1 },
      body: { idOposicion: 1, resultados: [{ id_prueba: 1, valor: 10 }] }
    });
    const res = mockRes();
    await SimulacroController.guardar(req, res);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({ ok: true, data: expect.objectContaining({ idSimulacro: 7 }) })
    );
  });

  test('historial 402 si no es premium', async () => {
    PremiumService.puedeVerHistorialSimulacros.mockResolvedValue(false);
    const req = mockReq({ usuario: { id: 1 }, params: { idOposicion: '1' } });
    const res = mockRes();
    await SimulacroController.historial(req, res);
    expect(res.status).toHaveBeenCalledWith(402);
  });

  test('historial 200 si premium', async () => {
    PremiumService.puedeVerHistorialSimulacros.mockResolvedValue(true);
    SimulacroService.historial.mockResolvedValue([{ id_simulacro: 1 }]);
    const req = mockReq({ usuario: { id: 1 }, params: { idOposicion: '1' } });
    const res = mockRes();
    await SimulacroController.historial(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: [{ id_simulacro: 1 }] });
  });

  test('aplicarMarcasPerfil 400 si datos incompletos', async () => {
    const req = mockReq({ usuario: { id: 1 }, body: { resultados: [] } });
    const res = mockRes();
    await SimulacroController.aplicarMarcasPerfil(req, res);
    expect(res.status).toHaveBeenCalledWith(400);
  });

  test('aplicarMarcasPerfil 200 con subida de nivel', async () => {
    MarcasPerfilService.aplicarMarcasDesdeSimulacro.mockResolvedValue({
      nivelActual: 'BASICO',
      notaMediaActual: '4.00',
      mejoras: [],
      nivelTrasActualizar: 'INTERMEDIO',
      notaMediaTrasActualizar: '6.00',
      subirNivel: true,
      pruebasCompletadas: 3,
      marcasActualizadas: 1
    });
    const req = mockReq({
      usuario: { id: 1 },
      body: { idOposicion: 1, resultados: [{ id_prueba: 1, valor: 9 }] }
    });
    const res = mockRes();
    await SimulacroController.aplicarMarcasPerfil(req, res);
    const body = res.json.mock.calls[0][0];
    expect(body.ok).toBe(true);
    expect(body.data.perfil.subirNivel).toBe(true);
  });
});
