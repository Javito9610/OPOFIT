jest.mock('../src/config/db');
jest.mock('../src/services/PremiumService');
jest.mock('../src/services/DashboardService');
jest.mock('../src/services/RankingService');

const db = require('../src/config/db');
const PremiumService = require('../src/services/PremiumService');
const DashboardService = require('../src/services/DashboardService');
const RankingService = require('../src/services/RankingService');

const PremiumController = require('../src/controllers/PremiumController');
const DashboardController = require('../src/controllers/DashboardController');
const RankingController = require('../src/controllers/RankingController');
const { mockReq, mockRes } = require('./helpers/mockReqRes');

describe('PremiumController', () => {
  beforeEach(() => jest.clearAllMocks());

  test('estado: 200 con datos', async () => {
    PremiumService.getEstadoPremium.mockResolvedValue({ esPremium: true });
    const req = mockReq({ usuario: { id: 1 } });
    const res = mockRes();
    await PremiumController.estado(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: { esPremium: true } });
  });

  test('estado: 500 si falla', async () => {
    PremiumService.getEstadoPremium.mockRejectedValue(new Error('X'));
    const req = mockReq({ usuario: { id: 1 } });
    const res = mockRes();
    await PremiumController.estado(req, res);
    expect(res.status).toHaveBeenCalledWith(500);
  });

  test('activarPrueba bloqueado fuera de DEV', async () => {
    delete process.env.PREMIUM_DEV_MODE;
    const req = mockReq({ usuario: { id: 1 }, body: { dias: 30 } });
    const res = mockRes();
    await PremiumController.activarPrueba(req, res);
    expect(res.status).toHaveBeenCalledWith(403);
  });

  test('activarPrueba 200 en DEV', async () => {
    process.env.PREMIUM_DEV_MODE = 'true';
    PremiumService.activarPremium.mockResolvedValue({ esPremium: true });
    const req = mockReq({ usuario: { id: 1 }, body: { dias: 7 } });
    const res = mockRes();
    await PremiumController.activarPrueba(req, res);
    expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ ok: true }));
    delete process.env.PREMIUM_DEV_MODE;
  });
});

describe('DashboardController', () => {
  beforeEach(() => jest.clearAllMocks());

  test('401 si no hay usuario', async () => {
    const req = mockReq({ usuario: null, query: { idOposicion: '1' } });
    const res = mockRes();
    await DashboardController.getResumen(req, res);
    expect(res.status).toHaveBeenCalledWith(401);
  });

  test('400 sin idOposicion ni opo en perfil', async () => {
    db.query.mockResolvedValue([[{ oposiciones_id_oposicion: null }]]);
    const req = mockReq({ usuario: { id: 1 }, query: {} });
    const res = mockRes();
    await DashboardController.getResumen(req, res);
    expect(res.status).toHaveBeenCalledWith(400);
  });

  test('200 con datos usando opo del query', async () => {
    DashboardService.obtenerResumen.mockResolvedValue({ rachaDias: 5 });
    const req = mockReq({ usuario: { id: 1 }, query: { idOposicion: '3' } });
    const res = mockRes();
    await DashboardController.getResumen(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: { rachaDias: 5 } });
    expect(DashboardService.obtenerResumen).toHaveBeenCalledWith(1, 3);
  });

  test('200 con datos usando opo del perfil', async () => {
    db.query.mockResolvedValue([[{ oposiciones_id_oposicion: 7 }]]);
    DashboardService.obtenerResumen.mockResolvedValue({ ok: 1 });
    const req = mockReq({ usuario: { id: 1 }, query: {} });
    const res = mockRes();
    await DashboardController.getResumen(req, res);
    expect(DashboardService.obtenerResumen).toHaveBeenCalledWith(1, 7);
  });

  test('500 si servicio falla', async () => {
    DashboardService.obtenerResumen.mockRejectedValue(new Error('X'));
    const req = mockReq({ usuario: { id: 1 }, query: { idOposicion: '1' } });
    const res = mockRes();
    await DashboardController.getResumen(req, res);
    expect(res.status).toHaveBeenCalledWith(500);
  });
});

describe('RankingController', () => {
  beforeEach(() => jest.clearAllMocks());

  test('getRanking 200', async () => {
    RankingService.obtenerRanking.mockResolvedValue([{ posicion: 1, userId: 10 }]);
    const req = mockReq({ usuario: { id: 10 }, params: { idOposicion: '1' } });
    const res = mockRes();
    await RankingController.getRanking(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, data: [{ posicion: 1, userId: 10 }] });
  });

  test('getMiPosicion 200', async () => {
    RankingService.posicionUsuario.mockResolvedValue({ posicion: 3, total: 100, notaMedia: 7 });
    const req = mockReq({ usuario: { id: 1 }, params: { idOposicion: '1' } });
    const res = mockRes();
    await RankingController.getMiPosicion(req, res);
    expect(res.json).toHaveBeenCalledWith({
      ok: true,
      data: { posicion: 3, total: 100, notaMedia: 7 }
    });
  });

  test('togglePerfilPublico 200', async () => {
    db.query.mockResolvedValue([{ affectedRows: 1 }]);
    const req = mockReq({ usuario: { id: 1 }, body: { publico: true } });
    const res = mockRes();
    await RankingController.togglePerfilPublico(req, res);
    expect(res.json).toHaveBeenCalledWith({ ok: true, perfilPublico: true });
  });

  test('getDetalleUsuario 404 si no encontrado', async () => {
    RankingService.detalleUsuario.mockResolvedValue(null);
    const req = mockReq({ usuario: { id: 1 }, params: { idOposicion: '1', userId: '99' } });
    const res = mockRes();
    await RankingController.getDetalleUsuario(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
  });

  test('getDetalleUsuario 400 si DISTINTA_OPOSICION', async () => {
    RankingService.detalleUsuario.mockResolvedValue({ error: 'DISTINTA_OPOSICION' });
    const req = mockReq({ usuario: { id: 1 }, params: { idOposicion: '1', userId: '99' } });
    const res = mockRes();
    await RankingController.getDetalleUsuario(req, res);
    expect(res.status).toHaveBeenCalledWith(400);
  });

  test('getDetalleUsuario 403 si perfil no publico y no soy yo', async () => {
    RankingService.detalleUsuario.mockResolvedValue({
      perfilPublico: false,
      userId: 99
    });
    const req = mockReq({ usuario: { id: 1 }, params: { idOposicion: '1', userId: '99' } });
    const res = mockRes();
    await RankingController.getDetalleUsuario(req, res);
    expect(res.status).toHaveBeenCalledWith(403);
  });

  test('getDetalleUsuario 200 si perfil publico', async () => {
    RankingService.detalleUsuario.mockResolvedValue({
      perfilPublico: true,
      userId: 99,
      notaMedia: 8
    });
    const req = mockReq({ usuario: { id: 1 }, params: { idOposicion: '1', userId: '99' } });
    const res = mockRes();
    await RankingController.getDetalleUsuario(req, res);
    expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ ok: true }));
  });
});
