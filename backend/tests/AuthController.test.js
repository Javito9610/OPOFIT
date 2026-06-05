const jwt = require('jsonwebtoken');
jest.mock('jsonwebtoken');
jest.mock('../src/services/AuthService');
const AuthService = require('../src/services/AuthService');
const {
  registrar,
  login,
  loginConGoogle
} = require('../src/controllers/AuthController');
describe('AuthController', () => {
  let req, res;
  beforeEach(() => {
    req = {
      body: {}
    };
    res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn().mockReturnThis()
    };
    process.env.JWT_SECRET = 'test-secret';
    jest.clearAllMocks();
  });
  describe('registrar', () => {
    test('debería devolver 400 si falta email', async () => {
      req.body = {
        password: '123456'
      };
      await registrar(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
      expect(res.json).toHaveBeenCalledWith({
        ok: false,
        msg: 'Faltan campos obligatorios (email o contraseña)'
      });
    });
    test('debería devolver 400 si falta password', async () => {
      req.body = {
        email: 'test@test.com'
      };
      await registrar(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });
    test('debería devolver 201 si el registro es exitoso', async () => {
      req.body = {
        nombre: 'Test',
        email: 'test@test.com',
        password: '123456',
        genero: 'HOMBRE',
        peso: 70,
        altura: 175,
        oposiciones_id_oposicion: 1
      };
      AuthService.registrar.mockResolvedValue({
        userId: 1
      });
      await registrar(req, res);
      expect(res.status).toHaveBeenCalledWith(201);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
        ok: true,
        userId: 1
      }));
    });
    test('debería devolver 409 si el email ya está registrado', async () => {
      req.body = {
        nombre: 'Test',
        email: 'test@test.com',
        password: '123456',
        genero: 'HOMBRE',
        peso: 70,
        altura: 175,
        oposiciones_id_oposicion: 1
      };
      AuthService.registrar.mockRejectedValue(new Error('Duplicate entry'));
      await registrar(req, res);
      expect(res.status).toHaveBeenCalledWith(409);
      expect(res.json).toHaveBeenCalledWith({
        ok: false,
        msg: 'Este correo electrónico ya está registrado'
      });
    });
    test('debería devolver 500 sin detalles del error interno', async () => {
      req.body = {
        nombre: 'Test',
        email: 'test@test.com',
        password: '123456',
        genero: 'HOMBRE',
        peso: 70,
        altura: 175,
        oposiciones_id_oposicion: 1
      };
      AuthService.registrar.mockRejectedValue(new Error('Connection refused'));
      await registrar(req, res);
      expect(res.status).toHaveBeenCalledWith(500);
      const responseBody = res.json.mock.calls[0][0];
      expect(responseBody.ok).toBe(false);
      expect(responseBody.msg).toBe('Error en el proceso de registro');
      expect(responseBody.error).toBeUndefined();
    });
  });
  describe('login', () => {
    test('debería devolver 400 si falta email o password', async () => {
      req.body = {
        email: ''
      };
      await login(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
      expect(res.json).toHaveBeenCalledWith({
        ok: false,
        msg: 'Email y contraseña requeridos'
      });
    });
    test('debería devolver 200 con token si las credenciales son correctas', async () => {
      req.body = {
        email: 'test@test.com',
        password: '123456'
      };
      const usuario = {
        id_usuario: 1,
        email: 'test@test.com',
        nombre: 'Test'
      };
      AuthService.login.mockResolvedValue(usuario);
      jwt.sign.mockReturnValue('token-generado');
      await login(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({
        ok: true,
        user: usuario,
        token: 'token-generado'
      });
    });
    test('debería devolver 401 si las credenciales son incorrectas', async () => {
      req.body = {
        email: 'test@test.com',
        password: 'incorrecta'
      };
      AuthService.login.mockRejectedValue(new Error('Contraseña incorrecta'));
      await login(req, res);
      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.json).toHaveBeenCalledWith({
        ok: false,
        msg: 'Credenciales incorrectas'
      });
    });
    test('debería devolver 500 si JWT_SECRET no está definido', async () => {
      delete process.env.JWT_SECRET;
      req.body = {
        email: 'test@test.com',
        password: '123456'
      };
      AuthService.login.mockResolvedValue({
        id_usuario: 1,
        email: 'test@test.com'
      });
      await login(req, res);
      expect(res.status).toHaveBeenCalledWith(500);
    });
  });
  describe('loginConGoogle', () => {
    test('debería devolver 400 si falta googleToken', async () => {
      req.body = {
        email: 'test@test.com'
      };
      await loginConGoogle(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
      expect(res.json).toHaveBeenCalledWith({
        ok: false,
        msg: 'Token de Google y email son requeridos'
      });
    });
    test('debería devolver 400 si falta email', async () => {
      req.body = {
        googleToken: 'token-google'
      };
      await loginConGoogle(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });
    test('debería devolver 200 con token si Google login es exitoso', async () => {
      req.body = {
        googleToken: 'token-google',
        email: 'test@gmail.com',
        nombre: 'Test'
      };
      const usuario = {
        id_usuario: 1,
        email: 'test@gmail.com',
        nombre: 'Test'
      };
      AuthService.loginConGoogle.mockResolvedValue(usuario);
      jwt.sign.mockReturnValue('jwt-token');
      await loginConGoogle(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({
        ok: true,
        user: usuario,
        userId: 1,
        token: 'jwt-token'
      });
    });
    test('debería devolver 500 sin detalles del error interno', async () => {
      req.body = {
        googleToken: 'token-invalido',
        email: 'test@gmail.com'
      };
      AuthService.loginConGoogle.mockRejectedValue(new Error('Token verification failed'));
      await loginConGoogle(req, res);
      expect(res.status).toHaveBeenCalledWith(500);
      const responseBody = res.json.mock.calls[0][0];
      expect(responseBody.ok).toBe(false);
      expect(responseBody.msg).toBe('Error al autenticar con Google');
      expect(responseBody.error).toBeUndefined();
    });
    test('debería usar nombre por defecto si no se proporciona', async () => {
      req.body = {
        googleToken: 'token-google',
        email: 'test@gmail.com'
      };
      const usuario = {
        id_usuario: 1,
        email: 'test@gmail.com'
      };
      AuthService.loginConGoogle.mockResolvedValue(usuario);
      jwt.sign.mockReturnValue('jwt-token');
      await loginConGoogle(req, res);
      expect(AuthService.loginConGoogle).toHaveBeenCalledWith('token-google', 'test@gmail.com', 'Usuario Google');
    });
  });
});
