jest.mock('../src/services/AmigosService');
const AmigosService = require('../src/services/AmigosService');
const {
  listar,
  buscar,
  solicitar,
  responder,
  chat,
  enviarMensaje,
  feed
} = require('../src/controllers/AmigosController');
const { mockReq, mockRes } = require('./helpers/mockReqRes');

describe('AmigosController', () => {
  beforeEach(() => jest.clearAllMocks());

  describe('listar', () => {
    test('200 con amigos y pendientes', async () => {
      AmigosService.listarAmigos.mockResolvedValue([{ amigo_id: 2 }]);
      AmigosService.solicitudesPendientes.mockResolvedValue([{ id_amistad: 9 }]);
      const req = mockReq({ usuario: { id: 1 } });
      const res = mockRes();
      await listar(req, res);
      expect(res.json).toHaveBeenCalledWith({
        ok: true,
        data: { amigos: [{ amigo_id: 2 }], pendientes: [{ id_amistad: 9 }] }
      });
    });

    test('500 si servicio falla', async () => {
      AmigosService.listarAmigos.mockRejectedValue(new Error('DB'));
      const req = mockReq({ usuario: { id: 1 } });
      const res = mockRes();
      await listar(req, res);
      expect(res.status).toHaveBeenCalledWith(500);
    });
  });

  describe('buscar', () => {
    test('400 si faltan params', async () => {
      const req = mockReq({ usuario: { id: 1 }, query: {} });
      const res = mockRes();
      await buscar(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('200 con resultados', async () => {
      AmigosService.buscarPorNombre.mockResolvedValue([{ id_usuario: 5 }]);
      const req = mockReq({
        usuario: { id: 1 },
        query: { nombre: 'Sara', idOposicion: '1' }
      });
      const res = mockRes();
      await buscar(req, res);
      expect(res.json).toHaveBeenCalledWith({ ok: true, data: [{ id_usuario: 5 }] });
    });
  });

  describe('solicitar', () => {
    test('ok envia solicitud', async () => {
      AmigosService.enviarSolicitud.mockResolvedValue(undefined);
      const req = mockReq({ usuario: { id: 1 }, body: { idUsuario: 2 } });
      const res = mockRes();
      await solicitar(req, res);
      expect(res.json).toHaveBeenCalledWith({ ok: true, msg: 'Solicitud enviada' });
    });

    test('400 si DISTINTA_OPOSICION', async () => {
      AmigosService.enviarSolicitud.mockRejectedValue(new Error('DISTINTA_OPOSICION'));
      const req = mockReq({ usuario: { id: 1 }, body: { idUsuario: 2 } });
      const res = mockRes();
      await solicitar(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('404 si USUARIO_NO_ENCONTRADO', async () => {
      AmigosService.enviarSolicitud.mockRejectedValue(new Error('USUARIO_NO_ENCONTRADO'));
      const req = mockReq({ usuario: { id: 1 }, body: { idUsuario: 999 } });
      const res = mockRes();
      await solicitar(req, res);
      expect(res.status).toHaveBeenCalledWith(404);
    });
  });

  describe('responder', () => {
    test('ok responde', async () => {
      AmigosService.responderSolicitud.mockResolvedValue(undefined);
      const req = mockReq({ usuario: { id: 1 }, body: { idAmistad: 1, aceptar: true } });
      const res = mockRes();
      await responder(req, res);
      expect(res.json).toHaveBeenCalledWith({ ok: true });
    });

    test('400 si servicio rechaza', async () => {
      AmigosService.responderSolicitud.mockRejectedValue(new Error('SOLICITUD_NO_ENCONTRADA'));
      const req = mockReq({ usuario: { id: 1 }, body: { idAmistad: 1, aceptar: true } });
      const res = mockRes();
      await responder(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });
  });

  describe('chat & mensaje', () => {
    test('chat 200', async () => {
      AmigosService.obtenerChat.mockResolvedValue([]);
      const req = mockReq({ usuario: { id: 1 }, params: { otroId: '2' } });
      const res = mockRes();
      await chat(req, res);
      expect(res.json).toHaveBeenCalledWith({ ok: true, data: [] });
    });

    test('enviarMensaje 200', async () => {
      AmigosService.enviarMensaje.mockResolvedValue({ idMensaje: 7 });
      const req = mockReq({ usuario: { id: 1 }, body: { idDestinatario: 2, texto: 'hola' } });
      const res = mockRes();
      await enviarMensaje(req, res);
      expect(res.json).toHaveBeenCalledWith({ ok: true, data: { idMensaje: 7 } });
    });

    test('enviarMensaje 400 si no son amigos', async () => {
      AmigosService.enviarMensaje.mockRejectedValue(new Error('NO_SOIS_AMIGOS'));
      const req = mockReq({ usuario: { id: 1 }, body: { idDestinatario: 2, texto: 'hi' } });
      const res = mockRes();
      await enviarMensaje(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });
  });

  describe('feed', () => {
    test('200 con feed', async () => {
      AmigosService.feedActividad.mockResolvedValue([]);
      const req = mockReq({ usuario: { id: 1 } });
      const res = mockRes();
      await feed(req, res);
      expect(res.json).toHaveBeenCalledWith({ ok: true, data: [] });
    });
  });
});
