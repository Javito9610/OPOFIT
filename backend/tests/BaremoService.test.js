jest.mock('../src/config/db');
const db = require('../src/config/db');
const BaremoService = require('../src/services/BaremoService');

describe('BaremoService.calcularNotaPrueba', () => {
  beforeEach(() => jest.clearAllMocks());

  test('devuelve null si valor no es numerico', async () => {
    const r = await BaremoService.calcularNotaPrueba(1, 'HOMBRE', 'abc');
    expect(r).toBeNull();
  });

  test('devuelve null si prueba no existe', async () => {
    db.query.mockResolvedValueOnce([[]]); // pruebaRows vacio
    const r = await BaremoService.calcularNotaPrueba(99, 'HOMBRE', 10);
    expect(r).toBeNull();
  });

  test('devuelve null si no hay baremos', async () => {
    db.query
      .mockResolvedValueOnce([[{ mejor_si_es_menor: 1 }]])
      .mockResolvedValueOnce([[]]);
    const r = await BaremoService.calcularNotaPrueba(1, 'HOMBRE', 10);
    expect(r).toBeNull();
  });

  // Pruebas de tiempo (menor=mejor): 100m, carreras...
  describe('cuando menor es mejor (tiempos)', () => {
    const setupTiempo = () => {
      db.query
        .mockResolvedValueOnce([[{ mejor_si_es_menor: 1 }]])
        .mockResolvedValueOnce([[
          { marca_valor: 12.0, nota: 10 },
          { marca_valor: 13.0, nota: 8 },
          { marca_valor: 14.0, nota: 6 },
          { marca_valor: 15.0, nota: 4 },
          { marca_valor: 16.0, nota: 2 }
        ]]);
    };

    test('valor mejor que el mejor de la tabla -> 10', async () => {
      setupTiempo();
      expect(await BaremoService.calcularNotaPrueba(1, 'HOMBRE', 11.0)).toBe(10);
    });

    test('valor peor que el peor -> 0', async () => {
      setupTiempo();
      expect(await BaremoService.calcularNotaPrueba(1, 'HOMBRE', 99)).toBe(0);
    });

    test('valor justo en la frontera (igual al mejor)', async () => {
      setupTiempo();
      expect(await BaremoService.calcularNotaPrueba(1, 'HOMBRE', 12.0)).toBe(10);
    });

    test('valor intermedio: 13.5 obtiene la nota del primer baremo >= valor', async () => {
      setupTiempo();
      // valor=13.5 -> elegida sera la fila con marca_valor=14.0 (primera >= 13.5)
      // y luego sigue pasando hasta encontrar la ultima que cumpla; pero como pasa de 14 a 15 sale
      // El algoritmo guarda elegida = f.nota mientras v <= f.marca_valor
      // v=13.5: 12.0(F:no), 13.0(F:no), 14.0(T:elegida=6), 15.0(T:elegida=4), 16.0(T:elegida=2)
      expect(await BaremoService.calcularNotaPrueba(1, 'HOMBRE', 13.5)).toBe(2);
    });
  });

  describe('cuando mayor es mejor (potencia/repeticiones)', () => {
    const setupReps = () => {
      db.query
        .mockResolvedValueOnce([[{ mejor_si_es_menor: 0 }]])
        .mockResolvedValueOnce([[
          { marca_valor: 5, nota: 2 },
          { marca_valor: 10, nota: 4 },
          { marca_valor: 15, nota: 6 },
          { marca_valor: 20, nota: 8 },
          { marca_valor: 25, nota: 10 }
        ]]);
    };

    test('valor mejor que el maximo -> 10', async () => {
      setupReps();
      expect(await BaremoService.calcularNotaPrueba(1, 'HOMBRE', 30)).toBe(10);
    });

    test('valor peor que el minimo -> 0', async () => {
      setupReps();
      expect(await BaremoService.calcularNotaPrueba(1, 'HOMBRE', 1)).toBe(0);
    });

    test('valor intermedio progresivo', async () => {
      setupReps();
      // v=17: barre 5(elegida=2),10(elegida=4),15(elegida=6),20(stop). Resultado=6.
      expect(await BaremoService.calcularNotaPrueba(1, 'HOMBRE', 17)).toBe(6);
    });
  });
});

describe('BaremoService.calcularNotaMediaOposicion', () => {
  beforeEach(() => jest.clearAllMocks());

  test('USER_NOT_FOUND si usuario no existe', async () => {
    db.query.mockResolvedValueOnce([[]]);
    const r = await BaremoService.calcularNotaMediaOposicion(99, 1);
    expect(r).toEqual({ error: 'USER_NOT_FOUND' });
  });
});
