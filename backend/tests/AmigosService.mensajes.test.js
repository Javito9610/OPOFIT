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
      .mockResolvedValueOnce([[{ amigo_id: 2 }]])
      .mockResolvedValueOnce([{ insertId: 99 }]);
    const r = await AmigosService.enviarMensaje(1, 2, '  hola  ');
    expect(r).toEqual({ idMensaje: 99 });
    // Se trimea antes de guardar
    const params = db.query.mock.calls.at(-1)[1];
    expect(params[2]).toBe('hola');
  });

  test('trunca a 1000 caracteres', async () => {
    db.query
      .mockResolvedValueOnce([[{ amigo_id: 2 }]])
      .mockResolvedValueOnce([{ insertId: 100 }]);
    const largo = 'a'.repeat(5000);
    await AmigosService.enviarMensaje(1, 2, largo);
    const params = db.query.mock.calls.at(-1)[1];
    expect(params[2].length).toBe(1000);
  });
});
