/**
 * Tests del AuthService.registrar y normalizacion de email.
 */
jest.mock('../src/config/db');
jest.mock('bcryptjs', () => ({
  hash: jest.fn().mockResolvedValue('hashed'),
  compare: jest.fn()
}));

const db = require('../src/config/db');
const bcrypt = require('bcryptjs');
const AuthService = require('../src/services/AuthService');

describe('AuthService.registrar', () => {
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

  test('crea usuario FITNESS sin oposicion y calcula IMC', async () => {
    conn.query
      .mockResolvedValueOnce([{ insertId: 50 }]) // INSERT usuarios
      .mockResolvedValueOnce([{ affectedRows: 1 }]); // INSERT settings
    db.query.mockResolvedValueOnce([[{ id_usuario: 50, email: 'a@b.com', modo_uso: 'FITNESS' }]]);
    const r = await AuthService.registrar({
      nombre: 'X',
      email: 'A@B.COM',
      password: 'secreto',
      genero: 'HOMBRE',
      peso: 70,
      altura: 175,
      modo_uso: 'FITNESS'
    });
    expect(r.userId).toBe(50);
    expect(r.user.password).toBeUndefined();
    // Email normalizado a lowercase trim
    const insertCall = conn.query.mock.calls[0];
    expect(insertCall[1][1]).toBe('a@b.com');
    // IMC = 70 / (1.75^2) = 22.86
    expect(insertCall[1][6]).toBe('22.86');
  });

  test('rechaza oposicion invalida', async () => {
    conn.query.mockResolvedValueOnce([[]]); // SELECT oposiciones -> vacio
    await expect(
      AuthService.registrar({
        email: 'a@b.com',
        password: 'x',
        peso: 70,
        altura: 175,
        oposiciones_id_oposicion: 999
      })
    ).rejects.toThrow(/Oposici.n no v.lida/);
    expect(conn.rollback).toHaveBeenCalled();
  });

  test('inserta marcas iniciales si se proporcionan', async () => {
    conn.query
      .mockResolvedValueOnce([[{ id_oposicion: 1 }]]) // SELECT oposiciones
      .mockResolvedValueOnce([{ insertId: 50 }]) // INSERT usuarios
      .mockResolvedValueOnce([{ affectedRows: 1 }]) // INSERT settings
      .mockResolvedValueOnce([{ affectedRows: 1 }]) // INSERT marca 1
      .mockResolvedValueOnce([{ affectedRows: 1 }]); // INSERT marca 2
    db.query.mockResolvedValueOnce([[{ id_usuario: 50 }]]);
    await AuthService.registrar({
      nombre: 'X',
      email: 'a@b.com',
      password: 'x',
      peso: 70,
      altura: 175,
      oposiciones_id_oposicion: 1,
      marcasIniciales: [
        { id_prueba: 1, valor: 10 },
        { id_prueba: 2, valor: 12 }
      ]
    });
    expect(conn.commit).toHaveBeenCalled();
    // 1 select + 1 insert + 1 settings + 2 marcas
    expect(conn.query).toHaveBeenCalledTimes(5);
  });
});

describe('AuthService.login', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    db.query.mockReset();
    bcrypt.compare.mockReset();
  });

  test('Usuario no encontrado si sin filas', async () => {
    db.query.mockResolvedValueOnce([[]]);
    await expect(AuthService.login('x@y.com', 'pw')).rejects.toThrow('Usuario no encontrado');
  });

  test('Contrasena incorrecta', async () => {
    db.query.mockResolvedValueOnce([[{ id_usuario: 1, email: 'x@y.com', password: 'h' }]]);
    bcrypt.compare.mockResolvedValueOnce(false);
    await expect(AuthService.login('x@y.com', 'pw')).rejects.toThrow('Contrase');
  });

  test('OK quita password', async () => {
    db.query.mockResolvedValueOnce([[{ id_usuario: 1, email: 'x@y.com', password: 'h', nombre: 'X' }]]);
    bcrypt.compare.mockResolvedValueOnce(true);
    const r = await AuthService.login('  X@Y.COM ', 'pw');
    expect(r.password).toBeUndefined();
    expect(r.id_usuario).toBe(1);
  });
});
