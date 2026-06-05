jest.mock('../src/config/db');
jest.mock('bcryptjs', () => ({
  hash: jest.fn().mockResolvedValue('new-hash'),
  compare: jest.fn()
}));

const db = require('../src/config/db');
const bcrypt = require('bcryptjs');
const AuthService = require('../src/services/AuthService');

describe('AuthService.cambiarPassword', () => {
  beforeEach(() => jest.clearAllMocks());

  test('actualiza password si la actual es correcta', async () => {
    db.query
      .mockResolvedValueOnce([[{ password: 'old-hash' }]])
      .mockResolvedValueOnce([{ affectedRows: 1 }]);
    bcrypt.compare.mockResolvedValueOnce(true);
    const r = await AuthService.cambiarPassword(1, 'actual', 'nueva123');
    expect(r).toEqual({ ok: true });
    expect(bcrypt.hash).toHaveBeenCalledWith('nueva123', 10);
    expect(db.query.mock.calls[1][0]).toContain('UPDATE usuarios SET password');
  });

  test('PASSWORD_ACTUAL_INCORRECTA', async () => {
    db.query.mockResolvedValueOnce([[{ password: 'old-hash' }]]);
    bcrypt.compare.mockResolvedValueOnce(false);
    await expect(AuthService.cambiarPassword(1, 'mala', 'nueva123')).rejects.toThrow('PASSWORD_ACTUAL_INCORRECTA');
  });

  test('PASSWORD_CORTA si nueva tiene menos de 6 caracteres', async () => {
    await expect(AuthService.cambiarPassword(1, 'actual', '123')).rejects.toThrow('PASSWORD_CORTA');
  });

  test('USUARIO_NO_ENCONTRADO', async () => {
    db.query.mockResolvedValueOnce([[]]);
    await expect(AuthService.cambiarPassword(99, 'actual', 'nueva123')).rejects.toThrow('USUARIO_NO_ENCONTRADO');
  });
});
