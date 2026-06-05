jest.mock('../src/config/db');
jest.mock('../src/services/RutinasService');
jest.mock('../src/services/OposicionService');

const db = require('../src/config/db');
const RutinaService = require('../src/services/RutinasService');
const OposicionesService = require('../src/services/OposicionService');

const {
  actualizarPerfil,
  actualizarSettings
} = require('../src/controllers/UsuarioController');
const {
  getOposiciones,
  getInfoOposiciones,
  getRequisitos
} = require('../src/controllers/OposicionController');

describe('UsuarioController', () => {
  let req, res;
  beforeEach(() => {
    req = {
      body: {},
      params: {},
      usuario: { id: 1 }
    };
    res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn().mockReturnThis()
    };
    jest.clearAllMocks();
  });

  describe('actualizarPerfil', () => {
    test('400 si no hay peso, altura ni marcas', async () => {
      req.body = {};
      await actualizarPerfil(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ ok: false }));
    });

    test('400 si nuevasMarcas no es array', async () => {
      req.body = { nuevasMarcas: 'no-soy-array' };
      await actualizarPerfil(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('401 si el usuario no existe en BD', async () => {
      req.body = { peso: 75, altura: 180 };
      db.query.mockResolvedValueOnce([[]]); // userExists vacio
      await actualizarPerfil(req, res);
      expect(res.status).toHaveBeenCalledWith(401);
    });

    test('200 si la actualizacion es exitosa', async () => {
      req.body = {
        peso: 75,
        altura: 180,
        oposicionId: 1,
        nuevasMarcas: [{ id_prueba: 1, valor: 10 }]
      };
      db.query.mockResolvedValue([[{ id_usuario: 1 }]]);
      RutinaService.calcularNotaYNivel.mockResolvedValue({
        nivelSugerido: 'INTERMEDIO',
        notaMedia: 6.5
      });
      await actualizarPerfil(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ ok: true }));
    });

    test('500 sin detalles del error interno', async () => {
      req.body = { peso: 75, altura: 180, oposicionId: 1 };
      db.query.mockRejectedValue(new Error('Database error'));
      await actualizarPerfil(req, res);
      expect(res.status).toHaveBeenCalledWith(500);
      const body = res.json.mock.calls[0][0];
      expect(body.error).toBeUndefined();
      expect(body.msg).toBeDefined();
    });
  });

  describe('actualizarSettings', () => {
    test('400 si faltan datos', async () => {
      req.body = { userId: 1 };
      await actualizarSettings(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('403 si userId no coincide', async () => {
      req.body = { userId: 999, unidadPeso: 'kg', unidadDistancia: 'km' };
      await actualizarSettings(req, res);
      expect(res.status).toHaveBeenCalledWith(403);
    });

    test('200 si se guardan correctamente', async () => {
      req.body = { userId: 1, unidadPeso: 'kg', unidadDistancia: 'km' };
      db.query.mockResolvedValue([{ affectedRows: 1 }]);
      await actualizarSettings(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({ ok: true, msg: 'Ajustes guardados' });
    });

    test('500 sin detalles del error interno', async () => {
      req.body = { userId: 1, unidadPeso: 'kg', unidadDistancia: 'km' };
      db.query.mockRejectedValue(new Error('Connection lost'));
      await actualizarSettings(req, res);
      expect(res.status).toHaveBeenCalledWith(500);
      const body = res.json.mock.calls[0][0];
      expect(body.error).toBeUndefined();
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
    test('200 con las oposiciones', async () => {
      const opos = [
        { id_oposicion: 1, nombre: 'Policia Nacional' },
        { id_oposicion: 2, nombre: 'Guardia Civil' }
      ];
      OposicionesService.obtenerTodas.mockResolvedValue(opos);
      await getOposiciones(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({ ok: true, data: opos });
    });

    test('404 si no hay oposiciones', async () => {
      OposicionesService.obtenerTodas.mockResolvedValue([]);
      await getOposiciones(req, res);
      expect(res.status).toHaveBeenCalledWith(404);
    });

    test('500 sin error.message', async () => {
      OposicionesService.obtenerTodas.mockRejectedValue(new Error('DB error'));
      await getOposiciones(req, res);
      expect(res.status).toHaveBeenCalledWith(500);
      const body = res.json.mock.calls[0][0];
      expect(body.error).not.toEqual('DB error');
    });
  });

  describe('getInfoOposiciones', () => {
    test('400 si falta el id', async () => {
      req.params = {};
      await getInfoOposiciones(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('200 con detalle completo', async () => {
      req.params = { id: 1 };
      OposicionesService.obtenerDetalleCompleto.mockResolvedValue({
        pruebas: [{ id: 1, nombre: 'Circuito' }],
        noticias: [{ id: 1, titulo: 'Noticia' }]
      });
      await getInfoOposiciones(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
    });

    test('404 si la oposicion no existe', async () => {
      req.params = { id: 999 };
      OposicionesService.obtenerDetalleCompleto.mockResolvedValue(null);
      await getInfoOposiciones(req, res);
      expect(res.status).toHaveBeenCalledWith(404);
    });
  });

  describe('getRequisitos', () => {
    test('400 si faltan parametros', async () => {
      req.params = { id: 1 };
      await getRequisitos(req, res);
      expect(res.status).toHaveBeenCalledWith(400);
    });

    test('200 con requisitos', async () => {
      req.params = { id: 1, genero: 'HOMBRE' };
      OposicionesService.obtenerRequisitosPrueba.mockResolvedValue([{
        nivel_exigencia: 1, valor_objetivo: 11.40
      }]);
      await getRequisitos(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ ok: true }));
    });

    test('404 si no hay requisitos', async () => {
      req.params = { id: 1, genero: 'HOMBRE' };
      OposicionesService.obtenerRequisitosPrueba.mockResolvedValue([]);
      await getRequisitos(req, res);
      expect(res.status).toHaveBeenCalledWith(404);
    });
  });
});
