/**
 * E2E de coherencia: el backend debe rechazar marcas/resultados absurdos o
 * imposibles a traves de los endpoints reales (simulacro, perfil, historial).
 */
process.env.JWT_SECRET = 'opofit-e2e-coherencia';
process.env.NOTIFICATIONS_CRON = 'false';

jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);

const memDb = require('./helpers/inMemoryDb');
const { seedAll } = require('./helpers/seed');
const { buildApp } = require('./helpers/buildApp');
const request = require('supertest');

let app;
let token;

beforeAll(async () => {
  memDb.reset();
  await seedAll(memDb);
  app = buildApp();
  const reg = await request(app).post('/api/auth/registrar').send({
    nombre: 'Coherencia Tester',
    email: 'coherencia@opofit.test',
    password: 'Pwd!coherencia1',
    genero: 'HOMBRE',
    peso: 75,
    altura: 180,
    oposiciones_id_oposicion: 1
  });
  token = reg.body.token;
});

const auth = () => ({ Authorization: `Bearer ${token}` });

describe('Registro: rechaza datos imposibles', () => {
  const valido = {
    nombre: 'Persona Valida', password: 'Pwd!valido1',
    genero: 'HOMBRE', peso: 75, altura: 180, oposiciones_id_oposicion: 1
  };
  test('peso negativo -> 400', async () => {
    const r = await request(app).post('/api/auth/registrar')
      .send({ ...valido, email: 'pesoneg@opofit.test', peso: -20 });
    expect(r.status).toBe(400);
  });
  test('altura imposible (300 cm) -> 400', async () => {
    const r = await request(app).post('/api/auth/registrar')
      .send({ ...valido, email: 'altura@opofit.test', altura: 300 });
    expect(r.status).toBe(400);
  });
  test('genero invalido -> 400', async () => {
    const r = await request(app).post('/api/auth/registrar')
      .send({ ...valido, email: 'genero@opofit.test', genero: 'OTRO' });
    expect(r.status).toBe(400);
  });
  test('email mal formado -> 400', async () => {
    const r = await request(app).post('/api/auth/registrar')
      .send({ ...valido, email: 'no-es-un-email' });
    expect(r.status).toBe(400);
  });
  test('datos coherentes -> 201', async () => {
    const r = await request(app).post('/api/auth/registrar')
      .send({ ...valido, email: 'coherente-ok@opofit.test' });
    expect(r.status).toBe(201);
  });
});

describe('Simulacro: rechaza valores imposibles', () => {
  test('100m en segundos negativos -> 400', async () => {
    const r = await request(app).post('/api/simulacros/guardar').set(auth())
      .send({ idOposicion: 1, resultados: [{ id_prueba: 1, valor: -5 }] });
    expect(r.status).toBe(400);
  });

  test('dominadas imposibles (99999) -> 400', async () => {
    const r = await request(app).post('/api/simulacros/guardar').set(auth())
      .send({ idOposicion: 1, resultados: [{ id_prueba: 2, valor: 99999 }] });
    expect(r.status).toBe(400);
  });

  test('valor de texto -> 400', async () => {
    const r = await request(app).post('/api/simulacros/guardar').set(auth())
      .send({ idOposicion: 1, resultados: [{ id_prueba: 1, valor: 'rapidisimo' }] });
    expect(r.status).toBe(400);
  });

  test('dominadas decimales -> 400', async () => {
    const r = await request(app).post('/api/simulacros/guardar').set(auth())
      .send({ idOposicion: 1, resultados: [{ id_prueba: 2, valor: 12.5 }] });
    expect(r.status).toBe(400);
  });

  test('valores realistas -> 200', async () => {
    const r = await request(app).post('/api/simulacros/guardar').set(auth())
      .send({ idOposicion: 1, resultados: [{ id_prueba: 1, valor: 12 }, { id_prueba: 2, valor: 15 }] });
    expect(r.status).toBe(200);
  });
});

describe('Aplicar marcas al perfil: coherencia', () => {
  test('tiempo imposible (3 horas en 100m) -> 400', async () => {
    const r = await request(app).post('/api/simulacros/aplicar-marcas').set(auth())
      .send({ idOposicion: 1, resultados: [{ id_prueba: 1, valor: 10800 }] });
    expect(r.status).toBe(400);
  });

  test('marca realista -> 200', async () => {
    const r = await request(app).post('/api/simulacros/aplicar-marcas').set(auth())
      .send({ idOposicion: 1, resultados: [{ id_prueba: 2, valor: 20 }] });
    expect(r.status).toBe(200);
  });
});

describe('Perfil: nuevasMarcas coherentes', () => {
  test('marca negativa -> 400', async () => {
    const r = await request(app).put('/api/user/perfil').set(auth())
      .send({ oposicionId: 1, nuevasMarcas: [{ id_prueba: 1, valor: -3 }] });
    expect(r.status).toBe(400);
  });

  test('marca realista -> 200', async () => {
    const r = await request(app).put('/api/user/perfil').set(auth())
      .send({ oposicionId: 1, nuevasMarcas: [{ id_prueba: 1, valor: 13 }] });
    expect(r.status).toBe(200);
  });
});

describe('Historial: resultados de ejercicio coherentes', () => {
  test('valor negativo en ejercicio -> 400', async () => {
    const r = await request(app).post('/api/historial/registrar').set(auth())
      .send({ tipoRutina: 'OPO', idRutina: 1, duracion: 30, ejercicios: [{ id_ejercicio: 1, valor: -10 }] });
    expect(r.status).toBe(400);
  });

  test('valor disparatado -> 400', async () => {
    const r = await request(app).post('/api/historial/registrar').set(auth())
      .send({ tipoRutina: 'OPO', idRutina: 1, duracion: 30, ejercicios: [{ id_ejercicio: 1, valor: 9999999 }] });
    expect(r.status).toBe(400);
  });

  test('valores normales -> 200', async () => {
    const r = await request(app).post('/api/historial/registrar').set(auth())
      .send({ tipoRutina: 'OPO', idRutina: 1, duracion: 45, ejercicios: [{ id_ejercicio: 1, valor: 12 }] });
    expect(r.status).toBe(200);
  });
});
