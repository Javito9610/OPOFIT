// Mocks
jest.mock('../src/config/db');
jest.mock('../src/services/RutinasService');

const db = require('../src/config/db');
const RutinaService = require('../src/services/RutinasService');
const { actualizarPerfil, actualizarSettings } = require('../src/controllers/UsuarioController');
const { getOposiciones, getInfoOposiciones, getRequisitos } = require('../src/controllers/OposicionController');
const OposicionesService = require('../src/services/OposicionService');

jest.mock('../src/services/OposicionService');

describe('UsuarioController', () => {
    let req, res;

    beforeEach(() => {
        req = { body: {}, params: {}, usuario: { id: 1 } };
        res = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn().mockReturnThis()
        };
        jest.clearAllMocks();
    });

    describe('actualizarPerfil', () => {
        test('debería devolver 400 si faltan datos obligatorios', async () => {
            req.body = { userId: 1, peso: 75 };

            await actualizarPerfil(req, res);

            expect(res.status).toHaveBeenCalledWith(400);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                ok: false
            }));
        });

        test('debería devolver 403 si el userId no coincide con el usuario autenticado', async () => {
            req.body = {
                userId: 999, peso: 75, altura: 180,
                nuevasMarcas: [{ id_prueba: 1, valor: 10 }]
            };

            await actualizarPerfil(req, res);

            expect(res.status).toHaveBeenCalledWith(403);
        });

        test('debería devolver 200 si la actualización es exitosa', async () => {
            req.body = {
                userId: 1, peso: 75, altura: 180, oposicionId: 1,
                nuevasMarcas: [{ id_prueba: 1, valor: 10 }]
            };
            db.query.mockResolvedValue([[]]);
            RutinaService.calcularNotaYNivel.mockResolvedValue({
                nivelSugerido: 'INTERMEDIO',
                notaMedia: 6.5
            });

            await actualizarPerfil(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                ok: true
            }));
        });

        test('debería devolver 500 sin detalles del error interno', async () => {
            req.body = {
                userId: 1, peso: 75, altura: 180, oposicionId: 1,
                nuevasMarcas: [{ id_prueba: 1, valor: 10 }]
            };
            db.query.mockRejectedValue(new Error('Database error'));

            await actualizarPerfil(req, res);

            expect(res.status).toHaveBeenCalledWith(500);
            const responseBody = res.json.mock.calls[0][0];
            expect(responseBody.error).toBeUndefined();
            expect(responseBody.msg).toBeDefined();
        });
    });

    describe('actualizarSettings', () => {
        test('debería devolver 400 si faltan datos', async () => {
            req.body = { userId: 1 };

            await actualizarSettings(req, res);

            expect(res.status).toHaveBeenCalledWith(400);
        });

        test('debería devolver 403 si el userId no coincide', async () => {
            req.body = { userId: 999, unidadPeso: 'kg', unidadDistancia: 'km' };

            await actualizarSettings(req, res);

            expect(res.status).toHaveBeenCalledWith(403);
        });

        test('debería devolver 200 si se guardan correctamente', async () => {
            req.body = { userId: 1, unidadPeso: 'kg', unidadDistancia: 'km' };
            db.query.mockResolvedValue([{ affectedRows: 1 }]);

            await actualizarSettings(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith({
                ok: true,
                msg: 'Ajustes guardados'
            });
        });

        test('debería devolver 500 sin detalles del error interno', async () => {
            req.body = { userId: 1, unidadPeso: 'kg', unidadDistancia: 'km' };
            db.query.mockRejectedValue(new Error('Connection lost'));

            await actualizarSettings(req, res);

            expect(res.status).toHaveBeenCalledWith(500);
            const responseBody = res.json.mock.calls[0][0];
            expect(responseBody.error).toBeUndefined();
        });
    });
});

describe('OposicionController', () => {
    let req, res;

    beforeEach(() => {
        req = { body: {}, params: {} };
        res = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn().mockReturnThis()
        };
        jest.clearAllMocks();
    });

    describe('getOposiciones', () => {
        test('debería devolver 200 con las oposiciones', async () => {
            const opos = [
                { id_oposicion: 1, nombre: 'Policía Nacional - Escala Básica' },
                { id_oposicion: 2, nombre: 'Guardia Civil - Acceso Libre' }
            ];
            OposicionesService.obtenerTodas.mockResolvedValue(opos);

            await getOposiciones(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith({
                ok: true,
                data: opos
            });
        });

        test('debería devolver 404 si no hay oposiciones', async () => {
            OposicionesService.obtenerTodas.mockResolvedValue([]);

            await getOposiciones(req, res);

            expect(res.status).toHaveBeenCalledWith(404);
        });

        test('debería devolver 500 sin error.message', async () => {
            OposicionesService.obtenerTodas.mockRejectedValue(new Error('DB error'));

            await getOposiciones(req, res);

            expect(res.status).toHaveBeenCalledWith(500);
            const responseBody = res.json.mock.calls[0][0];
            expect(responseBody.error).not.toEqual('DB error');
        });
    });

    describe('getInfoOposiciones', () => {
        test('debería devolver 400 si falta el id', async () => {
            req.params = {};

            await getInfoOposiciones(req, res);

            expect(res.status).toHaveBeenCalledWith(400);
        });

        test('debería devolver 200 con detalle completo', async () => {
            req.params = { id: 1 };
            OposicionesService.obtenerDetalleCompleto.mockResolvedValue({
                pruebas: [{ id: 1, nombre: 'Circuito' }],
                noticias: [{ id: 1, titulo: 'Noticia' }]
            });

            await getInfoOposiciones(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
        });

        test('debería devolver 404 si la oposición no existe', async () => {
            req.params = { id: 999 };
            OposicionesService.obtenerDetalleCompleto.mockResolvedValue(null);

            await getInfoOposiciones(req, res);

            expect(res.status).toHaveBeenCalledWith(404);
        });
    });

    describe('getRequisitos', () => {
        test('debería devolver 400 si faltan parámetros', async () => {
            req.params = { id: 1 };

            await getRequisitos(req, res);

            expect(res.status).toHaveBeenCalledWith(400);
        });

        test('debería devolver 200 con requisitos', async () => {
            req.params = { id: 1, genero: 'HOMBRE' };
            OposicionesService.obtenerRequisitosPrueba.mockResolvedValue([
                { nivel_exigencia: 1, valor_objetivo: 11.40 }
            ]);

            await getRequisitos(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                ok: true
            }));
        });

        test('debería devolver 404 si no hay requisitos', async () => {
            req.params = { id: 1, genero: 'HOMBRE' };
            OposicionesService.obtenerRequisitosPrueba.mockResolvedValue([]);

            await getRequisitos(req, res);

            expect(res.status).toHaveBeenCalledWith(404);
        });
    });
});
