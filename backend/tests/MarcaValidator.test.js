/**
 * Tests unitarios del validador de marcas. Cubre valores absurdos e imposibles
 * para todas las unidades. Funciones puras: sin BD.
 */
const V = require('../src/utils/MarcaValidator');

describe('MarcaValidator.normalizarUnidad', () => {
  test('reconoce sinonimos', () => {
    expect(V.normalizarUnidad('segundos')).toBe('s');
    expect(V.normalizarUnidad('SEG')).toBe('s');
    expect(V.normalizarUnidad('repeticiones')).toBe('reps');
    expect(V.normalizarUnidad('metros')).toBe('m');
    expect(V.normalizarUnidad('Km')).toBe('km');
    expect(V.normalizarUnidad('minutos')).toBe('min');
  });
  test('desconocida -> null', () => {
    expect(V.normalizarUnidad('chorradas')).toBeNull();
    expect(V.normalizarUnidad('')).toBeNull();
    expect(V.normalizarUnidad(null)).toBeNull();
  });
});

describe('MarcaValidator.validarValor: rechaza lo absurdo', () => {
  test('vacio/null/undefined', () => {
    expect(V.validarValor(null, 's').ok).toBe(false);
    expect(V.validarValor(undefined, 's').ok).toBe(false);
    expect(V.validarValor('', 's').ok).toBe(false);
  });
  test('texto y NaN', () => {
    expect(V.validarValor('abc', 's').ok).toBe(false);
    expect(V.validarValor('12abc', 's').ok).toBe(false);
    expect(V.validarValor(NaN, 's').ok).toBe(false);
    expect(V.validarValor(Infinity, 's').ok).toBe(false);
  });
  test('booleanos no cuelan como 1', () => {
    expect(V.validarValor(true, 'reps').ok).toBe(false);
  });
  test('cero y negativos', () => {
    expect(V.validarValor(0, 's').ok).toBe(false);
    expect(V.validarValor(-5, 's').ok).toBe(false);
    expect(V.validarValor(-0.1, 'km').ok).toBe(false);
  });
});

describe('MarcaValidator.validarValor: limites por unidad', () => {
  test('segundos: rango razonable', () => {
    expect(V.validarValor(12, 's').ok).toBe(true);
    expect(V.validarValor(7200, 's').ok).toBe(true);
    expect(V.validarValor(7201, 's').ok).toBe(false); // mas de 2h imposible
    expect(V.validarValor(0.5, 's').ok).toBe(false); // menos de 1s
  });
  test('repeticiones: enteras y acotadas', () => {
    expect(V.validarValor(15, 'reps').ok).toBe(true);
    expect(V.validarValor(12.5, 'reps').ok).toBe(false); // decimal
    expect(V.validarValor(10000, 'reps').ok).toBe(true);
    expect(V.validarValor(10001, 'reps').ok).toBe(false);
    expect(V.validarValor(100000, 'reps').ok).toBe(false); // 100k dominadas imposible
  });
  test('metros y km', () => {
    expect(V.validarValor(5000, 'm').ok).toBe(true);
    expect(V.validarValor(100001, 'm').ok).toBe(false);
    expect(V.validarValor(42.195, 'km').ok).toBe(true);
    expect(V.validarValor(1001, 'km').ok).toBe(false); // 1001 km de una tirada, no
  });
  test('unidad desconocida: solo exige numero positivo razonable', () => {
    expect(V.validarValor(50, 'loquesea').ok).toBe(true);
    expect(V.validarValor(2000000, 'loquesea').ok).toBe(false);
  });
});

describe('MarcaValidator.unidadDePrueba', () => {
  test('respeta distancias y minutos', () => {
    expect(V.unidadDePrueba({ unidad_entrada: 'm' })).toBe('m');
    expect(V.unidadDePrueba({ unidad_entrada: 'km' })).toBe('km');
    expect(V.unidadDePrueba({ unidad_entrada: 'min' })).toBe('min');
  });
  test('dominadas (id 2) depende del genero', () => {
    const prueba = { id_pruebas_oficiales: 2, unidad_entrada: 'reps', mejor_si_es_menor: 0 };
    expect(V.unidadDePrueba(prueba, 'HOMBRE')).toBe('reps');
    expect(V.unidadDePrueba(prueba, 'MUJER')).toBe('s');
  });
});

describe('MarcaValidator.validarMarcaPrueba', () => {
  test('100m (segundos): 11s vale, 0 no', () => {
    const p = { id_pruebas_oficiales: 1, nombre_prueba: 'Carrera 100m', unidad_entrada: 's', mejor_si_es_menor: 1 };
    expect(V.validarMarcaPrueba(p, 11).ok).toBe(true);
    const malo = V.validarMarcaPrueba(p, 0);
    expect(malo.ok).toBe(false);
    expect(malo.msg).toContain('Carrera 100m');
  });
  test('dominadas: 15 vale, 15.5 no (entero)', () => {
    const p = { id_pruebas_oficiales: 2, nombre_prueba: 'Dominadas', unidad_entrada: 'reps', mejor_si_es_menor: 0 };
    expect(V.validarMarcaPrueba(p, 15, 'HOMBRE').ok).toBe(true);
    expect(V.validarMarcaPrueba(p, 15.5, 'HOMBRE').ok).toBe(false);
  });
});

describe('MarcaValidator.validarResultados', () => {
  const catalogo = new Map([
    [1, { id_pruebas_oficiales: 1, nombre_prueba: '100m', unidad_entrada: 's', mejor_si_es_menor: 1 }],
    [2, { id_pruebas_oficiales: 2, nombre_prueba: 'Dominadas', unidad_entrada: 'reps', mejor_si_es_menor: 0 }]
  ]);
  test('lista valida pasa', () => {
    const r = V.validarResultados([{ id_prueba: 1, valor: 12 }, { id_prueba: 2, valor: 15 }], catalogo, 'HOMBRE');
    expect(r.ok).toBe(true);
    expect(r.errores).toHaveLength(0);
  });
  test('detecta varios errores a la vez', () => {
    const r = V.validarResultados(
      [{ id_prueba: 1, valor: -3 }, { id_prueba: 2, valor: 99999 }, { id_prueba: 9, valor: 5 }],
      catalogo,
      'HOMBRE'
    );
    expect(r.ok).toBe(false);
    expect(r.errores).toHaveLength(3); // negativo, demasiado, prueba inexistente
  });
});
