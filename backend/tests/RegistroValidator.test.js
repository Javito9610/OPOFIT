/**
 * Tests unitarios del validador de registro (coherencia de datos de usuario).
 */
const { validarRegistro } = require('../src/utils/RegistroValidator');

const base = {
  nombre: 'Ana',
  email: 'ana@opofit.test',
  password: 'cualquiera',
  genero: 'MUJER',
  peso: 62,
  altura: 168
};

describe('RegistroValidator.validarRegistro: casos validos', () => {
  test('datos correctos pasan y normaliza', () => {
    const r = validarRegistro(base);
    expect(r.ok).toBe(true);
    expect(r.datos.email).toBe('ana@opofit.test');
    expect(r.datos.genero).toBe('MUJER');
  });
  test('genero en minusculas se normaliza', () => {
    expect(validarRegistro({ ...base, genero: 'hombre' }).ok).toBe(true);
  });
  test('limites de peso/altura incluidos', () => {
    expect(validarRegistro({ ...base, peso: 25, altura: 100 }).ok).toBe(true);
    expect(validarRegistro({ ...base, peso: 350, altura: 260 }).ok).toBe(true);
  });
});

describe('RegistroValidator.validarRegistro: rechazos', () => {
  test('nombre vacio', () => {
    expect(validarRegistro({ ...base, nombre: '   ' }).ok).toBe(false);
  });
  test('email mal formado', () => {
    expect(validarRegistro({ ...base, email: 'no-es-email' }).ok).toBe(false);
    expect(validarRegistro({ ...base, email: 'a@b' }).ok).toBe(false);
    expect(validarRegistro({ ...base, email: '@opofit.test' }).ok).toBe(false);
  });
  test('genero invalido', () => {
    expect(validarRegistro({ ...base, genero: 'X' }).ok).toBe(false);
    expect(validarRegistro({ ...base, genero: '' }).ok).toBe(false);
  });
  test('peso imposible', () => {
    expect(validarRegistro({ ...base, peso: 0 }).ok).toBe(false);
    expect(validarRegistro({ ...base, peso: -10 }).ok).toBe(false);
    expect(validarRegistro({ ...base, peso: 500 }).ok).toBe(false);
    expect(validarRegistro({ ...base, peso: 'gordo' }).ok).toBe(false);
  });
  test('altura imposible', () => {
    expect(validarRegistro({ ...base, altura: 30 }).ok).toBe(false);
    expect(validarRegistro({ ...base, altura: 300 }).ok).toBe(false);
    expect(validarRegistro({ ...base, altura: NaN }).ok).toBe(false);
  });
});
