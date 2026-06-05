/**
 * E2E de los endpoints de Analisis (stateless) y Logros.
 */
process.env.JWT_SECRET = 'opofit-e2e-analisis';
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
    nombre: 'Tester Analisis',
    email: 'analisis@opofit.test',
    password: 'Pwd!analisis1',
    genero: 'HOMBRE',
    peso: 72,
    altura: 178,
    oposiciones_id_oposicion: 1
  });
  token = reg.body.token;
});

const auth = () => ({ Authorization: `Bearer ${token}` });

describe('Analisis: zonas de FC', () => {
  test('calcula zonas con FCmax dada', async () => {
    const r = await request(app).post('/api/analisis/zonas-fc').set(auth()).send({ fcMax: 190 });
    expect(r.status).toBe(200);
    expect(r.body.data.zonas).toHaveLength(5);
    expect(r.body.data.fcMax).toBe(190);
  });
  test('calcula zonas a partir de la edad', async () => {
    const r = await request(app).post('/api/analisis/zonas-fc').set(auth()).send({ edad: 30 });
    expect(r.status).toBe(200);
    expect(r.body.data.fcMax).toBe(187);
  });
  test('400 si no hay FCmax ni edad', async () => {
    const r = await request(app).post('/api/analisis/zonas-fc').set(auth()).send({});
    expect(r.status).toBe(400);
  });
  test('401 sin token', async () => {
    const r = await request(app).post('/api/analisis/zonas-fc').send({ fcMax: 190 });
    expect(r.status).toBe(401);
  });
});

describe('Analisis: prediccion de marca', () => {
  test('predice 10k a partir de 5k', async () => {
    const r = await request(app).post('/api/analisis/prediccion').set(auth())
      .send({ distanciaM: 5000, tiempoSeg: 1500, objetivoM: 10000 });
    expect(r.status).toBe(200);
    expect(r.body.data.tiempoEstimadoSeg).toBeGreaterThan(3000);
  });
  test('400 con datos invalidos', async () => {
    const r = await request(app).post('/api/analisis/prediccion').set(auth())
      .send({ distanciaM: 0, tiempoSeg: 1500, objetivoM: 10000 });
    expect(r.status).toBe(400);
  });
});

describe('Analisis: TSS, distribucion y vo2max', () => {
  test('TSS coherente', async () => {
    const r = await request(app).post('/api/analisis/tss').set(auth())
      .send({ durSeg: 3600, avgHr: 160, fcReposo: 60, fcMax: 190 });
    expect(r.status).toBe(200);
    expect(r.body.data.tss).toBeGreaterThan(0);
  });
  test('distribucion por zonas', async () => {
    const r = await request(app).post('/api/analisis/zonas-distribucion').set(auth())
      .send({ muestras: [110, 150, 150, 195], fcMax: 200 });
    expect(r.status).toBe(200);
    expect(r.body.data.muestras).toBe(4);
  });
  test('vo2max Cooper', async () => {
    const r = await request(app).post('/api/analisis/vo2max').set(auth())
      .send({ distancia12minM: 2400 });
    expect(r.status).toBe(200);
    expect(r.body.data.vo2max).toBeGreaterThan(40);
  });
});

describe('Logros', () => {
  test('devuelve estructura de logros con medallas', async () => {
    const r = await request(app).get('/api/logros').set(auth());
    expect(r.status).toBe(200);
    expect(Array.isArray(r.body.data.medallas)).toBe(true);
    expect(r.body.data.medallasTotales).toBeGreaterThan(0);
    expect(r.body.data.rachas).toBeDefined();
    expect(typeof r.body.data.medallasDesbloqueadas).toBe('number');
  });
  test('401 sin token', async () => {
    const r = await request(app).get('/api/logros');
    expect(r.status).toBe(401);
  });
});
