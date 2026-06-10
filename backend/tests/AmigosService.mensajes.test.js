jest.mock('../src/config/db');
const db = require('../src/config/db');
const AmigosService = require('../src/services/AmigosService');

describe('AmigosService.enviarMensaje - validacion de texto', () => {
  beforeEach(() => jest.clearAllMocks());

  test('rechaza texto vacio con MENSAJE_VACIO', async () => {
    await expect(AmigosService.enviarMensaje(1, 2, '')).rejects.toThrow('MENSAJE_VACIO');
  });

  test('rechaza texto solo con espacios', async () => {
    await expect(AmigosService.enviarMensaje(1, 2, '   \t\n  ')).rejects.toThrow('MENSAJE_VACIO');
  });

  test('rechaza null/undefined', async () => {
    await expect(AmigosService.enviarMensaje(1, 2, null)).rejects.toThrow('MENSAJE_VACIO');
    await expect(AmigosService.enviarMensaje(1, 2, undefined)).rejects.toThrow('MENSAJE_VACIO');
  });

  test('NO_SOIS_AMIGOS si destino no esta en lista', async () => {
    db.query.mockResolvedValueOnce([[]]);
    await expect(AmigosService.enviarMensaje(1, 2, 'hola')).rejects.toThrow('NO_SOIS_AMIGOS');
  });

  test('insert ok cuando son amigos y texto valido', async () => {
    db.query
      .mockResolvedValueOnce([[{ amigo_id: 2 }]])           // listarAmigos
      .mockResolvedValueOnce([{ insertId: 99 }])            // INSERT mensaje
      .mockResolvedValueOnce([[{ nombre: 'Yo' }]]);         // SELECT remitente (push)
    const r = await AmigosService.enviarMensaje(1, 2, '  hola  ');
    expect(r).toEqual({ idMensaje: 99 });
    // Buscamos el INSERT específico (ya no es la última llamada porque
    // detrás va el SELECT del remitente para el push FCM).
    const insertCall = db.query.mock.calls.find((c) =>
      String(c[0] || '').toLowerCase().startsWith('insert into mensajes_chat')
    );
    expect(insertCall[1][2]).toBe('hola');
  });

  test('trunca a 1000 caracteres', async () => {
    db.query
      .mockResolvedValueOnce([[{ amigo_id: 2 }]])
      .mockResolvedValueOnce([{ insertId: 100 }])
      .mockResolvedValueOnce([[{ nombre: 'Yo' }]]);
    const largo = 'a'.repeat(5000);
    await AmigosService.enviarMensaje(1, 2, largo);
    const insertCall = db.query.mock.calls.find((c) =>
      String(c[0] || '').toLowerCase().startsWith('insert into mensajes_chat')
    );
    expect(insertCall[1][2].length).toBe(1000);
  });
});
