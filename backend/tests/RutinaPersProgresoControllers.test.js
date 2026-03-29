// Mocks
jest.mock('../src/services/RutinaPersService');
jest.mock('../src/services/ProgresoService');
jest.mock('../src/services/RutinasService');
jest.mock('../src/config/db');

const rutinaPersService = require('../src/services/RutinaPersService');
const progresoService = require('../src/services/ProgresoService');
const RutinaService = require('../src/services/RutinasService');
const { nuevaRutinaPersonalizada, misRutinas, eliminarRutina } = require('../src/controllers/RutinaPersController');
const { guardarEntrenamiento, verEvolucion } = require('../src/controllers/ProgresoController');
const { getMiEntrenamiento } = require('../src/controllers/RutinaController');
const { getInfoPruebas } = require('../src/controllers/InfoPruebasController');
const infoPruebasService = require('../src/services/InfoPruebasService');

jest.mock('../src/services/InfoPruebasService');

describe('RutinaPersController', () => {
    let req, res;

    beforeEach(() => {
        req = { body: {}, params: {}, usuario: { id: 1 } };
        res = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn().mockReturnThis()
        };
        jest.clearAllMocks();
    });

    describe('nuevaRutinaPersonalizada', () => {
        test('debería devolver 400 si faltan datos', async () => {
            req.body = { userId: 1, nombre: 'Mi rutina' };

            await nuevaRutinaPersonalizada(req, res);

            expect(res.status).toHaveBeenCalledWith(400);
        });

        test('debería devolver 403 si userId no coincide', async () => {
            req.body = {
                userId: 999, nombre: 'Rutina', ejercicios: [{ id_ejercicio: 1, series: 3, repeticiones: 10 }]
            };

            await nuevaRutinaPersonalizada(req, res);

            expect(res.status).toHaveBeenCalledWith(403);
        });

        test('debería devolver 201 si se crea correctamente', async () => {
            req.body = {
                userId: 1, nombre: 'Mi rutina',
                ejercicios: [{ id_ejercicio: 1, series: 3, repeticiones: 10 }]
            };
            rutinaPersService.crearRutinaPropia.mockResolvedValue(5);

            await nuevaRutinaPersonalizada(req, res);

            expect(res.status).toHaveBeenCalledWith(201);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                ok: true,
                id: 5
            }));
        });

        test('debería devolver 500 sin error.message', async () => {
            req.body = {
                userId: 1, nombre: 'Rutina',
                ejercicios: [{ id_ejercicio: 1, series: 3, repeticiones: 10 }]
            };
            rutinaPersService.crearRutinaPropia.mockRejectedValue(new Error('DB error'));

            await nuevaRutinaPersonalizada(req, res);

            expect(res.status).toHaveBeenCalledWith(500);
            const responseBody = res.json.mock.calls[0][0];
            expect(responseBody.error).toBeUndefined();
        });
    });

    describe('misRutinas', () => {
        test('debería devolver 400 si falta userId', async () => {
            req.params = {};

            await misRutinas(req, res);

            expect(res.status).toHaveBeenCalledWith(400);
        });

        test('debería devolver 403 si userId no coincide', async () => {
            req.params = { userId: '999' };

            await misRutinas(req, res);

            expect(res.status).toHaveBeenCalledWith(403);
        });

        test('debería devolver 200 con lista vacía si no hay rutinas', async () => {
            req.params = { userId: '1' };
            rutinaPersService.listarMisRutinas.mockResolvedValue([]);

            await misRutinas(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                ok: true,
                data: []
            }));
        });

        test('debería devolver 200 con rutinas', async () => {
            req.params = { userId: '1' };
            const rutinas = [{ id_rutina_pers: 1, nombre_personalizado: 'Mi rutina' }];
            rutinaPersService.listarMisRutinas.mockResolvedValue(rutinas);

            await misRutinas(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith({
                ok: true,
                data: rutinas
            });
        });
    });

    describe('eliminarRutina', () => {
        test('debería devolver 400 si faltan parámetros', async () => {
            req.params = { userId: '1' };

            await eliminarRutina(req, res);

            expect(res.status).toHaveBeenCalledWith(400);
        });

        test('debería devolver 403 si userId no coincide', async () => {
            req.params = { userId: '999', idRutina: '1' };

            await eliminarRutina(req, res);

            expect(res.status).toHaveBeenCalledWith(403);
        });

        test('debería devolver 200 al eliminar correctamente', async () => {
            req.params = { userId: '1', idRutina: '5' };
            rutinaPersService.eliminarRutina.mockResolvedValue({ success: true });

            await eliminarRutina(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith({
                ok: true,
                msg: 'Rutina eliminada correctamente'
            });
        });

        test('debería devolver 500 sin error.message', async () => {
            req.params = { userId: '1', idRutina: '5' };
            rutinaPersService.eliminarRutina.mockRejectedValue(new Error('DB error'));

            await eliminarRutina(req, res);

            expect(res.status).toHaveBeenCalledWith(500);
            const responseBody = res.json.mock.calls[0][0];
            expect(responseBody.error).toBeUndefined();
        });
    });
});

describe('ProgresoController', () => {
    let req, res;

    beforeEach(() => {
        req = { body: {}, params: {}, usuario: { id: 1 } };
        res = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn().mockReturnThis()
        };
        jest.clearAllMocks();
    });

    describe('guardarEntrenamiento', () => {
        test('debería devolver 400 si faltan datos', async () => {
            req.body = { userId: 1 };

            await guardarEntrenamiento(req, res);

            expect(res.status).toHaveBeenCalledWith(400);
        });

        test('debería devolver 403 si userId no coincide', async () => {
            req.body = {
                userId: 999,
                ejercicios: [{ id_ejercicio: 1, valor: 10 }]
            };

            await guardarEntrenamiento(req, res);

            expect(res.status).toHaveBeenCalledWith(403);
        });

        test('debería devolver 200 si se guarda correctamente', async () => {
            req.body = {
                userId: 1,
                ejercicios: [{ id_ejercicio: 1, valor: 10 }]
            };
            progresoService.registrarEntreno.mockResolvedValue({ idHistorial: 7 });

            await guardarEntrenamiento(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                ok: true,
                id: 7
            }));
        });

        test('debería devolver 500 sin error.message', async () => {
            req.body = {
                userId: 1,
                ejercicios: [{ id_ejercicio: 1, valor: 10 }]
            };
            progresoService.registrarEntreno.mockRejectedValue(new Error('DB error'));

            await guardarEntrenamiento(req, res);

            expect(res.status).toHaveBeenCalledWith(500);
            const responseBody = res.json.mock.calls[0][0];
            expect(responseBody.error).toBeUndefined();
        });
    });

    describe('verEvolucion', () => {
        test('debería devolver 400 si faltan parámetros', async () => {
            req.params = { userId: '1' };

            await verEvolucion(req, res);

            expect(res.status).toHaveBeenCalledWith(400);
        });

        test('debería devolver 403 si userId no coincide', async () => {
            req.params = { userId: '999', idEjercicio: '1' };

            await verEvolucion(req, res);

            expect(res.status).toHaveBeenCalledWith(403);
        });

        test('debería devolver 200 con datos vacíos si no hay registros', async () => {
            req.params = { userId: '1', idEjercicio: '1' };
            progresoService.obtenerEvolucionEntreno.mockResolvedValue([]);

            await verEvolucion(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                ok: true,
                data: []
            }));
        });

        test('debería devolver 200 con datos de evolución', async () => {
            req.params = { userId: '1', idEjercicio: '1' };
            const datos = [
                { fecha_entreno: '2026-01-01', valor_conseguido: 8 },
                { fecha_entreno: '2026-01-05', valor_conseguido: 10 }
            ];
            progresoService.obtenerEvolucionEntreno.mockResolvedValue(datos);

            await verEvolucion(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith({
                ok: true,
                data: datos
            });
        });

        test('debería devolver 500 sin error.message', async () => {
            req.params = { userId: '1', idEjercicio: '1' };
            progresoService.obtenerEvolucionEntreno.mockRejectedValue(new Error('Error'));

            await verEvolucion(req, res);

            expect(res.status).toHaveBeenCalledWith(500);
            const responseBody = res.json.mock.calls[0][0];
            expect(responseBody.error).toBeUndefined();
        });
    });
});

describe('RutinaController', () => {
    let req, res;

    beforeEach(() => {
        req = { params: {}, usuario: { id: 1 } };
        res = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn().mockReturnThis()
        };
        jest.clearAllMocks();
    });

    describe('getMiEntrenamiento', () => {
        test('debería devolver 400 si faltan parámetros', async () => {
            req.params = { userId: '1' };

            await getMiEntrenamiento(req, res);

            expect(res.status).toHaveBeenCalledWith(400);
        });

        test('debería devolver 403 si userId no coincide', async () => {
            req.params = { userId: '999', idOposicion: '1' };

            await getMiEntrenamiento(req, res);

            expect(res.status).toHaveBeenCalledWith(403);
        });

        test('debería devolver 200 con rutina completa', async () => {
            req.params = { userId: '1', idOposicion: '1' };
            RutinaService.calcularNotaYNivel.mockResolvedValue({
                notaMedia: '7.50', nivelSugerido: 'INTERMEDIO', genero: 'HOMBRE'
            });
            RutinaService.obtenerRutinaCompleta.mockResolvedValue([{
                id_rutina_opo: 2, bloque: 'FUERZA',
                ejercicios: [{ nombre: 'Dominadas', series: 4, repeticiones: 8 }]
            }]);

            await getMiEntrenamiento(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                ok: true
            }));
        });

        test('debería devolver 404 si no hay marcas', async () => {
            req.params = { userId: '1', idOposicion: '1' };
            RutinaService.calcularNotaYNivel.mockResolvedValue(null);

            await getMiEntrenamiento(req, res);

            expect(res.status).toHaveBeenCalledWith(404);
        });

        test('debería devolver 500 sin error.message', async () => {
            req.params = { userId: '1', idOposicion: '1' };
            RutinaService.calcularNotaYNivel.mockRejectedValue(new Error('Error'));

            await getMiEntrenamiento(req, res);

            expect(res.status).toHaveBeenCalledWith(500);
            const responseBody = res.json.mock.calls[0][0];
            expect(responseBody.error).toBeUndefined();
        });
    });
});

describe('InfoPruebasController', () => {
    let req, res;

    beforeEach(() => {
        req = { params: {} };
        res = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn().mockReturnThis()
        };
        jest.clearAllMocks();
    });

    describe('getInfoPruebas', () => {
        test('debería devolver 400 si faltan parámetros', async () => {
            req.params = { idOposicion: '1' };

            await getInfoPruebas(req, res);

            expect(res.status).toHaveBeenCalledWith(400);
        });

        test('debería devolver 200 con datos', async () => {
            req.params = { idOposicion: '1', genero: 'HOMBRE' };
            const info = [{ nombre_prueba: 'Circuito', nota: 5 }];
            infoPruebasService.getInfoPruebas.mockResolvedValue(info);

            await getInfoPruebas(req, res);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith({
                ok: true,
                data: info
            });
        });

        test('debería devolver 404 si no hay datos', async () => {
            req.params = { idOposicion: '1', genero: 'HOMBRE' };
            infoPruebasService.getInfoPruebas.mockResolvedValue([]);

            await getInfoPruebas(req, res);

            expect(res.status).toHaveBeenCalledWith(404);
        });

        test('debería devolver 500 sin error.message', async () => {
            req.params = { idOposicion: '1', genero: 'HOMBRE' };
            infoPruebasService.getInfoPruebas.mockRejectedValue(new Error('Error'));

            await getInfoPruebas(req, res);

            expect(res.status).toHaveBeenCalledWith(500);
            const responseBody = res.json.mock.calls[0][0];
            expect(responseBody.error).toBeUndefined();
        });
    });
});
