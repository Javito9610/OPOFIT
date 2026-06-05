jest.mock('../src/config/db');
jest.mock('../src/services/NotificationService');
jest.mock('../src/services/PlanesService');
jest.mock('../src/services/RutinasService');
jest.mock('../src/services/PremiumService');
jest.mock('../src/services/HistorialAvanzadoService');

const db = require('../src/config/db');
const NotificationService = require('../src/services/NotificationService');
const PlanesService = require('../src/services/PlanesService');
const RutinaService = require('../src/services/RutinasService');
const PremiumService = require('../src/services/PremiumService');
const HistorialAvanzadoService = require('../src/services/HistorialAvanzadoService');

const AdminController = require('../src/controllers/AdminController');
const PlanesController = require('../src/controllers/PlanesController');
const HistorialAvanzadoController = require('../src/controllers/HistorialAvanzadoController');
const NotificationController = require('../src/controllers/NotificationController');
const { mockReq, mockRes } = require('./helpers/mockReqRes');

describe('AdminController', () => {
  beforeEach(() => jest.clearAllMocks());

  test('listOposiciones devuelve listado', async () => {
    db.query.mockResolvedValue([[{ id_oposicion: 1 }]]);
    const res = mockRes();
    await AdminController.listOposiciones({}, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: [{ id_oposicion: 1 }] });
  });

  test('upsertOposicion update path', async () => {
    db.query.mockResolvedValue([{ affectedRows: 1 }]);
    const req = mockReq({ body: { id_oposicion: 1, nombre: 'X', incluida_gratis: true } });
    const res = mockRes();
    await AdminController.upsertOposicion(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, id: 1 });
  });

  test('upsertOposicion insert path', async () => {
    db.query.mockResolvedValue([{ insertId: 7 }]);
    const req = mockReq({ body: { nombre: 'Nueva', incluida_gratis: false } });
    const res = mockRes();
    await AdminController.upsertOposicion(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, id: 7 });
  });

  test('listEjercicios', async () => {
    db.query.mockResolvedValue([[{ id_ejercicio: 1 }]]);
    const res = mockRes();
    await AdminController.listEjercicios({}, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: [{ id_ejercicio: 1 }] });
  });

  test('upsertEjercicio insert', async () => {
    db.query.mockResolvedValue([{ insertId: 3 }]);
    const req = mockReq({ body: { nombre: 'A' } });
    const res = mockRes();
    await AdminController.upsertEjercicio(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, id: 3 });
  });

  test('listPruebas filtra por oposicion', async () => {
    db.query.mockResolvedValue([[{ id_pruebas_oficiales: 1 }]]);
    const req = mockReq({ query: { oposicion: '1' } });
    const res = mockRes();
    await AdminController.listPruebas(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: [{ id_pruebas_oficiales: 1 }] });
  });

  test('upsertBaremo insert', async () => {
    db.query.mockResolvedValue([{ insertId: 5 }]);
    const req = mockReq({
      body: {
        pruebas_oficiales_id_pruebas_oficiales: 1,
        genero: 'HOMBRE',
        marca_valor: 11.5,
        nota: 5
      }
    });
    const res = mockRes();
    await AdminController.upsertBaremo(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, id: 5 });
  });

  test('enviarRecordatorios', async () => {
    NotificationService.enviarRecordatorioEntreno.mockResolvedValue({ enviados: 4 });
    const res = mockRes();
    await AdminController.enviarRecordatorios({}, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, enviados: 4 });
  });

  test('enviarNoticia', async () => {
    NotificationService.enviarNoticiaOposicion.mockResolvedValue({ enviados: 10 });
    const req = mockReq({ body: { idOposicion: 1, titulo: 't', cuerpo: 'c' } });
    const res = mockRes();
    await AdminController.enviarNoticia(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, enviados: 10 });
  });
});

describe('PlanesController', () => {
  beforeEach(() => jest.clearAllMocks());

  test('400 si faltan params', async () => {
    const req = mockReq({ usuario: { id: 1 }, params: {} });
    const res = mockRes();
    await PlanesController.getCalendario(req, res);
    expect(res.status).toHaveBeenCalledWith(400);
  });

  test('200 si faltan marcas (msg vacio)', async () => {
    RutinaService.calcularNotaYNivel.mockResolvedValue({ pruebasFaltantes: 2 });
    const req = mockReq({
      usuario: { id: 1 },
      params: { idOposicion: '1' },
      query: {}
    });
    const res = mockRes();
    await PlanesController.getCalendario(req, res);
    expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ ok: true, data: null }));
  });

  test('200 con calendario', async () => {
    RutinaService.calcularNotaYNivel.mockResolvedValue({
      pruebasFaltantes: 0,
      nivelSugerido: 'BASICO',
      genero: 'HOMBRE'
    });
    PremiumService.getEstadoPremium.mockResolvedValue({ esPremium: false });
    PlanesService.obtenerCalendarioMes.mockResolvedValue({ dias: [] });
    const req = mockReq({
      usuario: { id: 1 },
      params: { idOposicion: '1' },
      query: { year: '2026', month: '5' }
    });
    const res = mockRes();
    await PlanesController.getCalendario(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: { dias: [] } });
  });

  test('500 si falla servicio', async () => {
    RutinaService.calcularNotaYNivel.mockRejectedValue(new Error('X'));
    const req = mockReq({ usuario: { id: 1 }, params: { idOposicion: '1' }, query: {} });
    const res = mockRes();
    await PlanesController.getCalendario(req, res);
    expect(res.status).toHaveBeenCalledWith(500);
  });
});

describe('HistorialAvanzadoController', () => {
  beforeEach(() => jest.clearAllMocks());

  test('resumen 401 sin usuario', async () => {
    const req = mockReq({ usuario: null });
    const res = mockRes();
    await HistorialAvanzadoController.resumen(req, res);
    expect(res.status).toHaveBeenCalledWith(401);
  });

  test('resumen 200', async () => {
    HistorialAvanzadoService.resumen.mockResolvedValue({ totalSesiones: 5 });
    const req = mockReq({ usuario: { id: 1 }, query: { periodo: '30d' } });
    const res = mockRes();
    await HistorialAvanzadoController.resumen(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: { totalSesiones: 5 } });
  });

  test('listarSesiones 200', async () => {
    HistorialAvanzadoService.listarSesiones.mockResolvedValue([]);
    const req = mockReq({ usuario: { id: 1 }, query: { tipo: 'OPO' } });
    const res = mockRes();
    await HistorialAvanzadoController.listarSesiones(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: [] });
  });

  test('detalleSesion 404 si no existe', async () => {
    HistorialAvanzadoService.detalleSesion.mockResolvedValue(null);
    const req = mockReq({ usuario: { id: 1 }, params: { id: '9' } });
    const res = mockRes();
    await HistorialAvanzadoController.detalleSesion(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
  });

  test('historialEjercicio 200 null data', async () => {
    HistorialAvanzadoService.historialEjercicio.mockResolvedValue(null);
    const req = mockReq({ usuario: { id: 1 }, params: { idEjercicio: '1' } });
    const res = mockRes();
    await HistorialAvanzadoController.historialEjercicio(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: null });
  });

  test('historialPlan 200', async () => {
    HistorialAvanzadoService.historialPlan.mockResolvedValue({ semanas: [] });
    const req = mockReq({ usuario: { id: 1 }, params: { idPlan: '7' } });
    const res = mockRes();
    await HistorialAvanzadoController.historialPlan(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: { semanas: [] } });
  });
});

describe('NotificationController', () => {
  beforeEach(() => jest.clearAllMocks());

  test('registrarToken 200', async () => {
    NotificationService.guardarToken.mockResolvedValue(undefined);
    const req = mockReq({ usuario: { id: 1 }, body: { fcmToken: 'abc' } });
    const res = mockRes();
    await NotificationController.registrarToken(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, msg: 'Token registrado' });
  });

  test('registrarToken 400 si servicio falla', async () => {
    NotificationService.guardarToken.mockRejectedValue(new Error('FCM_TOKEN_REQUIRED'));
    const req = mockReq({ usuario: { id: 1 }, body: {} });
    const res = mockRes();
    await NotificationController.registrarToken(req, res);
    expect(res.status).toHaveBeenCalledWith(400);
  });
});
