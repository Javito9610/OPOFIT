jest.mock('../src/config/db');
jest.mock('../src/services/RutinaPersService');
jest.mock('../src/services/ProgresoService');
jest.mock('../src/services/RutinasService');
jest.mock('../src/services/PlanesService');
jest.mock('../src/services/PremiumService');
jest.mock('../src/services/InfoPruebasService');

const rutinaPersService = require('../src/services/RutinaPersService');
const progresoService = require('../src/services/ProgresoService');
const RutinaService = require('../src/services/RutinasService');
const PlanesService = require('../src/services/PlanesService');
const PremiumService = require('../src/services/PremiumService');
const infoPruebasService = require('../src/services/InfoPruebasService');

const {
  nuevaRutinaPersonalizada,
  misRutinas,
  eliminarRutina
} = require('../src/controllers/RutinaPersController');
const {
  guardarEntrenamiento,
  verEvolucion
} = require('../src/controllers/ProgresoController');
const { getMiEntrenamiento } = require('../src/controllers/RutinaController');
const { getInfoPruebas } = require('../src/controllers/InfoPruebasController');

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
    test('400 si faltan campos', async () => {
      req.body = { userId: 1, nombre: 'Rutina' };
      await nuevaRutinaPersonalizada(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('403 si userId no coincide', async () => {
      req.body = { userId: 999, nombre: 'X', ejercicios: [{ id_ejercicio: 1 }] };
      await nuevaRutinaPersonalizada(req, res);
      expect(res.status).toHaveBeenCalledWith(403);
    });

    test('201 al crear rutina', async () => {
      req.body = { userId: 1, nombre: 'X', ejercicios: [{ id_ejercicio: 1 }] };
      rutinaPersService.crearRutinaPropia.mockResolvedValue(42);
      await nuevaRutinaPersonalizada(req, res);
      expect(res.status).toHaveBeenCalledWith(201);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ ok: true, id: 42 }));
    });

    test('409 si nombre duplicado', async () => {
      req.body = { userId: 1, nombre: 'X', ejercicios: [{ id_ejercicio: 1 }] };
      rutinaPersService.crearRutinaPropia.mockRejectedValue(
        new Error('Ya tienes una rutina con este nombre. Cambialo!')
      );
      await nuevaRutinaPersonalizada(req, res);
      expect(res.status).toHaveBeenCalledWith(409);
    });
  });

  describe('misRutinas', () => {
    test('400 sin userId', async () => {
      req.params = {};
      await misRutinas(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('403 si userId no coincide', async () => {
      req.params = { userId: '999' };
      await misRutinas(req, res);
      expect(res.status).toHaveBeenCalledWith(403);
    });

    test('200 con lista vacia', async () => {
      req.params = { userId: '1' };
      rutinaPersService.listarMisRutinas.mockResolvedValue([]);
      await misRutinas(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ ok: true, data: [] }));
    });

    test('200 con rutinas agrupadas', async () => {
      req.params = { userId: '1' };
      rutinaPersService.listarMisRutinas.mockResolvedValue([
        {
          id_rutina_pers: 1,
          nombre_personalizado: 'Mi rutina',
          ejercicios_id_ejercicio: 10,
          nombre_ejercicio: 'Push up',
          series: 3,
          repeticiones: 10,
          descanso: 30
        },
        {
          id_rutina_pers: 1,
          nombre_personalizado: 'Mi rutina',
          ejercicios_id_ejercicio: 11,
          nombre_ejercicio: 'Pull up',
          series: 4,
          repeticiones: 6,
          descanso: 45
        }
      ]);
      await misRutinas(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      const body = res.json.mock.calls[0][0];
      expect(body.data).toHaveLength(1);
      expect(body.data[0].ejercicios).toHaveLength(2);
    });
  });

  describe('eliminarRutina', () => {
    test('400 si faltan parametros', async () => {
      req.params = { userId: '1' };
      await eliminarRutina(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('403 si userId no coincide', async () => {
      req.params = { userId: '999', idRutina: '1' };
      await eliminarRutina(req, res);
      expect(res.status).toHaveBeenCalledWith(403);
    });

    test('200 al eliminar', async () => {
      req.params = { userId: '1', idRutina: '5' };
      rutinaPersService.eliminarRutina.mockResolvedValue({ success: true });
      await eliminarRutina(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
    });

    test('404 si NOT_FOUND', async () => {
      req.params = { userId: '1', idRutina: '5' };
      const err = new Error('No encontrada');
      err.code = 'NOT_FOUND';
      rutinaPersService.eliminarRutina.mockRejectedValue(err);
      await eliminarRutina(req, res);
      expect(res.status).toHaveBeenCalledWith(404);
    });

    test('500 sin error.message', async () => {
      req.params = { userId: '1', idRutina: '5' };
      rutinaPersService.eliminarRutina.mockRejectedValue(new Error('DB error'));
      await eliminarRutina(req, res);
      expect(res.status).toHaveBeenCalledWith(500);
      const body = res.json.mock.calls[0][0];
      expect(body.error).toBeUndefined();
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
    test('400 si faltan datos', async () => {
      req.body = {};
      await guardarEntrenamiento(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('400 si tipoRutina invalido', async () => {
      req.body = { tipoRutina: 'XXX', idRutina: 1, duracion: 30, ejercicios: [{ id_ejercicio: 1, valor: 10 }] };
      await guardarEntrenamiento(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('400 si ejercicios vacios', async () => {
      req.body = { tipoRutina: 'OPO', idRutina: 1, duracion: 30, ejercicios: [] };
      await guardarEntrenamiento(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('200 al guardar correctamente', async () => {
      req.body = {
        tipoRutina: 'OPO',
        idRutina: 1,
        duracion: 30,
        ejercicios: [{ id_ejercicio: 1, valor: 10 }]
      };
      progresoService.registrarEntreno.mockResolvedValue({
        idHistorial: 7,
        recordsRotos: []
      });
      await guardarEntrenamiento(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ ok: true, id: 7 }));
    });

    test('500 sin error.message', async () => {
      req.body = {
        tipoRutina: 'OPO',
        idRutina: 1,
        duracion: 30,
        ejercicios: [{ id_ejercicio: 1, valor: 10 }]
      };
      progresoService.registrarEntreno.mockRejectedValue(new Error('DB error'));
      await guardarEntrenamiento(req, res);
      expect(res.status).toHaveBeenCalledWith(500);
      const body = res.json.mock.calls[0][0];
      expect(body.error).toBeUndefined();
    });
  });

  describe('verEvolucion', () => {
    test('400 sin idEjercicio', async () => {
      req.params = {};
      await verEvolucion(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('200 con datos vacios', async () => {
      req.params = { idEjercicio: '1' };
      progresoService.obtenerEvolucionEntreno.mockResolvedValue([]);
      await verEvolucion(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ ok: true, data: [] }));
    });

    test('200 con datos', async () => {
      req.params = { idEjercicio: '1' };
      const datos = [
        { fecha_entreno: '2026-01-01', valor_conseguido: 8 },
        { fecha_entreno: '2026-01-05', valor_conseguido: 10 }
      ];
      progresoService.obtenerEvolucionEntreno.mockResolvedValue(datos);
      await verEvolucion(req, res);
      expect(res.json).toHaveBeenCalledWith({ ok: true, data: datos });
    });

    test('500 sin error.message', async () => {
      req.params = { idEjercicio: '1' };
      progresoService.obtenerEvolucionEntreno.mockRejectedValue(new Error('Error'));
      await verEvolucion(req, res);
      expect(res.status).toHaveBeenCalledWith(500);
      const body = res.json.mock.calls[0][0];
      expect(body.error).toBeUndefined();
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
    PremiumService.puedeAccederOposicion.mockResolvedValue(true);
    PremiumService.filtrarRutinaPorPremium.mockImplementation((_uid, rutina) => rutina);
    PremiumService.getEstadoPremium.mockResolvedValue({ esPremium: false });
    PlanesService.obtenerPlanSemanal.mockResolvedValue(null);
  });

  describe('getMiEntrenamiento', () => {
    test('400 si faltan parametros', async () => {
      req.params = { userId: '1' };
      await getMiEntrenamiento(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('404 si la oposicion no existe', async () => {
      req.params = { userId: '1', idOposicion: '999' };
      PremiumService.puedeAccederOposicion.mockResolvedValueOnce(false);
      await getMiEntrenamiento(req, res);
      expect(res.status).toHaveBeenCalledWith(404);
    });

    test('200 con rutina completa', async () => {
      req.params = { userId: '1', idOposicion: '1' };
      RutinaService.calcularNotaYNivel.mockResolvedValue({
        notaMedia: '7.50',
        nivelSugerido: 'INTERMEDIO',
        genero: 'HOMBRE',
        totalPruebas: 3,
        pruebasCompletadas: 3,
        pruebasFaltantes: 0
      });
      RutinaService.obtenerRutinaCompleta.mockResolvedValue([
        { id_rutina_opo: 2, bloque: 'FUERZA', ejercicios: [] }
      ]);
      await getMiEntrenamiento(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ ok: true }));
    });

    test('404 si calcularNotaYNivel devuelve null', async () => {
      req.params = { userId: '1', idOposicion: '1' };
      RutinaService.calcularNotaYNivel.mockResolvedValue(null);
      await getMiEntrenamiento(req, res);
      expect(res.status).toHaveBeenCalledWith(404);
    });

    test('200 con INCOMPLETO si faltan pruebas', async () => {
      req.params = { userId: '1', idOposicion: '1' };
      RutinaService.calcularNotaYNivel.mockResolvedValue({
        notaMedia: null,
        nivelSugerido: null,
        genero: 'MUJER',
        totalPruebas: 4,
        pruebasCompletadas: 1,
        pruebasFaltantes: 3
      });
      await getMiEntrenamiento(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      const body = res.json.mock.calls[0][0];
      expect(body.data.nivelAsignado).toBe('INCOMPLETO');
    });

    test('500 sin error.message', async () => {
      req.params = { userId: '1', idOposicion: '1' };
      RutinaService.calcularNotaYNivel.mockRejectedValue(new Error('Error'));
      await getMiEntrenamiento(req, res);
      expect(res.status).toHaveBeenCalledWith(500);
      const body = res.json.mock.calls[0][0];
      expect(body.error).toBeUndefined();
    });
  });
});

describe('InfoPruebasController', () => {
  let req, res;
  beforeEach(() => {
    req = { params: {}, usuario: { id: 1 } };
    res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn().mockReturnThis()
    };
    jest.clearAllMocks();
    PremiumService.getEstadoPremium.mockResolvedValue({ esPremium: false });
    PremiumService.limitarBaremos.mockImplementation((_p, lista) => lista);
  });

  describe('getInfoPruebas', () => {
    test('400 si faltan parametros', async () => {
      req.params = { idOposicion: '1' };
      await getInfoPruebas(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('200 con datos', async () => {
      req.params = { idOposicion: '1', genero: 'HOMBRE' };
      const info = [{ nombre_prueba: 'Circuito', nota: 5 }];
      infoPruebasService.getInfoPruebas.mockResolvedValue(info);
      await getInfoPruebas(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({
        ok: true,
        data: info,
        esPremium: false,
        baremosLimitados: false
      });
    });

    test('404 si no hay datos', async () => {
      req.params = { idOposicion: '1', genero: 'HOMBRE' };
      infoPruebasService.getInfoPruebas.mockResolvedValue([]);
      await getInfoPruebas(req, res);
      expect(res.status).toHaveBeenCalledWith(404);
    });

    test('500 sin error.message', async () => {
      req.params = { idOposicion: '1', genero: 'HOMBRE' };
      infoPruebasService.getInfoPruebas.mockRejectedValue(new Error('Error'));
      await getInfoPruebas(req, res);
      expect(res.status).toHaveBeenCalledWith(500);
      const body = res.json.mock.calls[0][0];
      expect(body.error).toBeUndefined();
    });
  });
});
