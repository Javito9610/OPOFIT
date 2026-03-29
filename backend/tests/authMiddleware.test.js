const jwt = require('jsonwebtoken');

// Mock de jsonwebtoken
jest.mock('jsonwebtoken');

const { validarToken } = require('../src/middleware/authMiddleware');

describe('authMiddleware - validarToken', () => {
    let req, res, next;

    beforeEach(() => {
        req = { header: jest.fn() };
        res = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn().mockReturnThis()
        };
        next = jest.fn();
        process.env.JWT_SECRET = 'test-secret';
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    test('debería devolver 401 si no hay cabecera Authorization', () => {
        req.header.mockReturnValue(undefined);

        validarToken(req, res, next);

        expect(res.status).toHaveBeenCalledWith(401);
        expect(res.json).toHaveBeenCalledWith({
            ok: false,
            msg: 'No hay token en la petición. Acceso denegado.'
        });
        expect(next).not.toHaveBeenCalled();
    });

    test('debería devolver 500 si JWT_SECRET no está definido', () => {
        delete process.env.JWT_SECRET;
        req.header.mockReturnValue('Bearer token-valido');

        validarToken(req, res, next);

        expect(res.status).toHaveBeenCalledWith(500);
        expect(res.json).toHaveBeenCalledWith({
            ok: false,
            msg: 'Error de configuración del servidor: JWT_SECRET no definido.'
        });
        expect(next).not.toHaveBeenCalled();
    });

    test('debería devolver 401 si el token no es válido', () => {
        req.header.mockReturnValue('Bearer token-invalido');
        jwt.verify.mockImplementation(() => { throw new Error('invalid token'); });

        validarToken(req, res, next);

        expect(res.status).toHaveBeenCalledWith(401);
        expect(res.json).toHaveBeenCalledWith({
            ok: false,
            msg: 'Token no válido o caducado.'
        });
        expect(next).not.toHaveBeenCalled();
    });

    test('debería llamar a next() si el token es válido', () => {
        req.header.mockReturnValue('Bearer token-valido');
        const decoded = { id: 1, email: 'test@test.com' };
        jwt.verify.mockReturnValue(decoded);

        validarToken(req, res, next);

        expect(req.usuario).toEqual(decoded);
        expect(next).toHaveBeenCalled();
        expect(res.status).not.toHaveBeenCalled();
    });

    test('debería extraer correctamente el token del formato Bearer', () => {
        const token = 'mi-token-jwt-super-largo';
        req.header.mockReturnValue(`Bearer ${token}`);
        jwt.verify.mockReturnValue({ id: 1 });

        validarToken(req, res, next);

        expect(jwt.verify).toHaveBeenCalledWith(token, 'test-secret');
    });
});
