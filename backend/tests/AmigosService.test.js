jest.mock('../src/config/db');
jest.mock('../src/services/NotificationService', () => ({
  notificarSolicitudAmistad: jest.fn().mockResolvedValue(undefined),
  notificarAmistadAceptada: jest.fn().mockResolvedValue(undefined)
}));
const db = require('../src/config/db');
const AmigosService = require('../src/services/AmigosService');

function mockInAppNotifInsert() {
  return { insertId: 99, affectedRows: 1 };
}
function mockNombreUsuario(nombre = 'Test') {
  return [[{ nombre }]];
}

describe('AmigosService', () => {
  beforeEach(() => jest.clearAllMocks());

  test('ordenPar siempre devuelve [menor, mayor]', () => {
    expect(AmigosService.ordenPar(7, 3)).toEqual([3, 7]);
    expect(AmigosService.ordenPar(2, 5)).toEqual([2, 5]);
  });

  describe('enviarSolicitud', () => {
    test('rechaza auto-amistad', async () => {
      await expect(AmigosService.enviarSolicitud(1, 1)).rejects.toThrow('NO_AUTO_AMISTAD');
    });

    test('USUARIO_NO_ENCONTRADO si receptor no existe', async () => {
      db.query.mockResolvedValueOnce([[]]);
      await expect(AmigosService.enviarSolicitud(1, 99)).rejects.toThrow('USUARIO_NO_ENCONTRADO');
    });

    test('DISTINTA_OPOSICION si oposiciones distintas', async () => {
      db.query
        .mockResolvedValueOnce([[{ id_usuario: 2, oposiciones_id_oposicion: 2, modo_uso: 'OPOSITOR' }]])
        .mockResolvedValueOnce([[{ oposiciones_id_oposicion: 1, modo_uso: 'OPOSITOR' }]]);
      await expect(AmigosService.enviarSolicitud(1, 2)).rejects.toThrow('DISTINTA_OPOSICION');
    });

    test('inserta cuando misma oposicion', async () => {
      db.query
        .mockResolvedValueOnce([[{ id_usuario: 2, oposiciones_id_oposicion: 1, modo_uso: 'OPOSITOR' }]])
        .mockResolvedValueOnce([[{ oposiciones_id_oposicion: 1, modo_uso: 'OPOSITOR' }]])
        .mockResolvedValueOnce([mockInAppNotifInsert()])
        .mockResolvedValueOnce(mockNombreUsuario());
      const r = await AmigosService.enviarSolicitud(1, 2);
      expect(r).toEqual({ ok: true });
    });

    test('permite amistad entre usuarios FITNESS', async () => {
      db.query
        .mockResolvedValueOnce([[{ id_usuario: 2, oposiciones_id_oposicion: null, modo_uso: 'FITNESS' }]])
        .mockResolvedValueOnce([[{ oposiciones_id_oposicion: null, modo_uso: 'FITNESS' }]])
        .mockResolvedValueOnce([mockInAppNotifInsert()])
        .mockResolvedValueOnce(mockNombreUsuario());
      const r = await AmigosService.enviarSolicitud(1, 2);
      expect(r).toEqual({ ok: true });
    });

    test('permite opositor con usuario FITNESS', async () => {
      db.query
        .mockResolvedValueOnce([[{ id_usuario: 2, oposiciones_id_oposicion: null, modo_uso: 'FITNESS' }]])
        .mockResolvedValueOnce([[{ oposiciones_id_oposicion: 1, modo_uso: 'OPOSITOR' }]])
        .mockResolvedValueOnce([mockInAppNotifInsert()])
        .mockResolvedValueOnce(mockNombreUsuario());
      const r = await AmigosService.enviarSolicitud(1, 2);
      expect(r).toEqual({ ok: true });
    });
  });

  describe('responderSolicitud', () => {
    test('SOLICITUD_NO_ENCONTRADA', async () => {
      db.query.mockResolvedValueOnce([[]]);
      await expect(AmigosService.responderSolicitud(1, 7, true)).rejects.toThrow('SOLICITUD_NO_ENCONTRADA');
    });

    test('NO_PUEDES_ACEPTAR_TU_SOLICITUD', async () => {
      db.query.mockResolvedValueOnce([[{ solicitante_id: 1, id_amistad: 7 }]]);
      await expect(AmigosService.responderSolicitud(1, 7, true)).rejects.toThrow('NO_PUEDES_ACEPTAR_TU_SOLICITUD');
    });

    test('acepta solicitud entrante', async () => {
      db.query
        .mockResolvedValueOnce([[{ solicitante_id: 2, id_amistad: 7 }]])
        .mockResolvedValueOnce([{ affectedRows: 1 }])
        .mockResolvedValueOnce([mockInAppNotifInsert()])
        .mockResolvedValueOnce(mockNombreUsuario());
      const r = await AmigosService.responderSolicitud(1, 7, true);
      expect(r).toEqual({ ok: true });
    });
  });

  describe('enviarMensaje', () => {
    test('NO_SOIS_AMIGOS si destino no esta en lista', async () => {
      db.query.mockResolvedValueOnce([[]]); // listarAmigos vacio
      await expect(AmigosService.enviarMensaje(1, 2, 'hola')).rejects.toThrow('NO_SOIS_AMIGOS');
    });

    test('inserta mensaje cuando son amigos', async () => {
      db.query
        .mockResolvedValueOnce([[{ amigo_id: 2 }]])
        .mockResolvedValueOnce([{ insertId: 99 }]);
      const r = await AmigosService.enviarMensaje(1, 2, 'hola');
      expect(r).toEqual({ idMensaje: 99 });
    });

    test('trunca a 1000 caracteres', async () => {
      db.query
        .mockResolvedValueOnce([[{ amigo_id: 2 }]])           // listarAmigos
        .mockResolvedValueOnce([{ insertId: 100 }])           // INSERT mensaje
        .mockResolvedValueOnce([[{ nombre: 'Yo' }]]);         // SELECT remitente para push
      const largo = 'a'.repeat(5000);
      await AmigosService.enviarMensaje(1, 2, largo);
      // Buscamos la llamada INSERT, no la última (que ahora es el SELECT del push).
      const insertCall = db.query.mock.calls.find((c) =>
        String(c[0] || '').toLowerCase().startsWith('insert into mensajes_chat')
      );
      const params = insertCall[1];
      expect(params[2].length).toBe(1000);
    });
  });

  describe('buscarPorNombre', () => {
    test('retorna usuarios filtrados', async () => {
      db.query.mockResolvedValueOnce([[{ id_usuario: 5, nombre: 'Sara' }]]);
      const r = await AmigosService.buscarPorNombre(1, 'sa', 1);
      expect(r).toEqual([{ id_usuario: 5, nombre: 'Sara' }]);
    });

    test('busca usuarios FITNESS sin idOposicion', async () => {
      db.query.mockResolvedValueOnce([[{ id_usuario: 8, nombre: 'Ana', modo_uso: 'FITNESS' }]]);
      const r = await AmigosService.buscarPorNombre(1, 'an', null);
      expect(r[0].modo_uso).toBe('FITNESS');
    });
  });

  describe('feedActividad', () => {
    test('lista vacia si no tiene amigos', async () => {
      db.query.mockResolvedValueOnce([[]]);
      const r = await AmigosService.feedActividad(1);
      expect(r).toEqual([]);
    });
  });
});
