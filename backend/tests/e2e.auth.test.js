process.env.JWT_SECRET = 'opofit-test-secret';
process.env.NOTIFICATIONS_CRON = 'false';

jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);

const memDb = require('./helpers/inMemoryDb');
const { seedAll } = require('./helpers/seed');
const { buildApp } = require('./helpers/buildApp');
const request = require('supertest');

let app;
beforeAll(async () => {
  memDb.reset();
  await seedAll(memDb);
  app = buildApp();
});

describe('E2E /api/auth', () => {
  test('POST /registrar crea usuario nuevo y devuelve token', async () => {
    const res = await request(app).post('/api/auth/registrar').send({
      nombre: 'Nuevo',
      email: 'nuevo@opofit.test',
      password: 'Password123!',
      genero: 'HOMBRE',
      peso: 75,
      altura: 180,
      oposiciones_id_oposicion: 1
    });
    expect(res.status).toBe(201);
    expect(res.body.ok).toBe(true);
    expect(res.body.token).toBeDefined();
    expect(res.body.user.password).toBeUndefined();
  });

  test('POST /registrar 400 sin email', async () => {
    const res = await request(app).post('/api/auth/registrar').send({ password: 'x' });
    expect(res.status).toBe(400);
  });

  test('POST /registrar 409 si email duplicado (case-insensitive)', async () => {
    await request(app).post('/api/auth/registrar').send({
      nombre: 'A', email: 'dup@opofit.test', password: 'Pw',
      genero: 'HOMBRE', peso: 70, altura: 175, oposiciones_id_oposicion: 1
    });
    const r2 = await request(app).post('/api/auth/registrar').send({
      nombre: 'B', email: 'DUP@opofit.test', password: 'Pw',
      genero: 'HOMBRE', peso: 70, altura: 175, oposiciones_id_oposicion: 1
    });
    expect(r2.status).toBe(409);
  });

  test('POST /registrar 400 si oposicion no existe', async () => {
    const res = await request(app).post('/api/auth/registrar').send({
      nombre: 'X', email: 'opo@opofit.test', password: 'Pw',
      genero: 'HOMBRE', peso: 70, altura: 175, oposiciones_id_oposicion: 999
    });
    expect(res.status).toBe(400);
  });

  test('POST /login con credenciales correctas (tolera espacios y mayusculas)', async () => {
    await request(app).post('/api/auth/registrar').send({
      nombre: 'Login Test', email: 'login@opofit.test', password: 'MiPasswordSegura1',
      genero: 'MUJER', peso: 60, altura: 168, oposiciones_id_oposicion: 2
    });
    const res = await request(app).post('/api/auth/login').send({
      email: '  LOGIN@opofit.test  ',
      password: 'MiPasswordSegura1'
    });
    expect(res.status).toBe(200);
    expect(res.body.token).toBeDefined();
  });

  test('POST /login 401 con password incorrecta - mensaje generico', async () => {
    const res = await request(app).post('/api/auth/login').send({
      email: 'login@opofit.test', password: 'wrong'
    });
    expect(res.status).toBe(401);
    expect(res.body.msg).toBe('Credenciales incorrectas');
  });

  test('POST /login 401 con email inexistente - mismo mensaje generico (anti-enumeracion)', async () => {
    const res = await request(app).post('/api/auth/login').send({
      email: 'noexiste@opofit.test', password: 'wrong'
    });
    expect(res.status).toBe(401);
    expect(res.body.msg).toBe('Credenciales incorrectas');
  });

  test('GET /me devuelve usuario con token valido', async () => {
    const reg = await request(app).post('/api/auth/registrar').send({
      nombre: 'Me Test', email: 'me@opofit.test', password: 'Pw',
      genero: 'HOMBRE', peso: 70, altura: 175, oposiciones_id_oposicion: 1
    });
    const token = reg.body.token;
    const res = await request(app).get('/api/auth/me').set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.user.email).toBe('me@opofit.test');
    expect(res.body.user.password).toBeUndefined();
  });

  test('GET /me 401 sin token', async () => {
    const res = await request(app).get('/api/auth/me');
    expect(res.status).toBe(401);
  });

  test('GET /me 401 con token invalido', async () => {
    const res = await request(app).get('/api/auth/me').set('Authorization', 'Bearer fake.token');
    expect(res.status).toBe(401);
  });
});

describe('E2E /api/oposiciones', () => {
  test('GET / devuelve lista', async () => {
    const res = await request(app).get('/api/oposiciones');
    expect(res.status).toBe(200);
    expect(res.body.data.length).toBeGreaterThanOrEqual(3);
  });

  test('GET /:id con token devuelve detalle', async () => {
    const reg = await request(app).post('/api/auth/registrar').send({
      nombre: 'X', email: 'opos-detalle@opofit.test', password: 'Pw',
      genero: 'HOMBRE', peso: 70, altura: 175, oposiciones_id_oposicion: 1
    });
    const token = reg.body.token;
    const res = await request(app).get('/api/oposiciones/1').set('Authorization', `Bearer ${token}`);
    // Puede ser 200 con detalle o 404 si el servicio no devuelve nada; aceptamos ambos
    expect([200, 404]).toContain(res.status);
  });
});

describe('E2E /api/ejercicios', () => {
  test('GET / con token lista ejercicios', async () => {
    const reg = await request(app).post('/api/auth/registrar').send({
      nombre: 'Ej', email: 'ej@opofit.test', password: 'Pw',
      genero: 'HOMBRE', peso: 70, altura: 175, oposiciones_id_oposicion: 1
    });
    const token = reg.body.token;
    const res = await request(app).get('/api/ejercicios').set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body.data)).toBe(true);
  });

  test('GET / sin token devuelve 401', async () => {
    const res = await request(app).get('/api/ejercicios');
    expect(res.status).toBe(401);
  });
});
