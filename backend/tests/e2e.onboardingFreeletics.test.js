/**
 * E2E PUT /api/planes/onboarding — wizard Freeletics que guarda objetivo,
 * días, tiempo y lesiones del usuario en una sola llamada.
 */
process.env.JWT_SECRET = 'opofit-e2e-onb';
process.env.NOTIFICATIONS_CRON = 'false';
process.env.NOTICIAS_CRON_DISABLED = 'true';

jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);
jest.mock('../src/config/firebaseAdmin', () => ({
  initFirebaseAdmin: () => ({ messaging: () => ({ send: jest.fn().mockResolvedValue('ok') }) })
}));

const memDb = require('./helpers/inMemoryDb');
const { seedAll } = require('./helpers/seed');
const { buildApp } = require('./helpers/buildApp');
const request = require('supertest');

let app;
let token;
let userId;

beforeAll(async () => {
  memDb.reset();
  await seedAll(memDb);
  app = buildApp();

  const reg = await request(app).post('/api/auth/registrar').send({
    nombre: 'Atleta Onb',
    email: `onb_${Date.now()}@opofit.test`,
    password: 'Password123!',
    genero: 'HOMBRE',
    peso: 75,
    altura: 178,
    modo_uso: 'FITNESS'
  });
  expect(reg.status).toBe(201);
  token = reg.body.token;
  userId = reg.body.userId || reg.body.user?.id_usuario;
});

describe('E2E PUT /api/planes/onboarding', () => {
  test('guarda objetivo + días + tiempo + lesiones válidos', async () => {
    const res = await request(app)
      .put('/api/planes/onboarding')
      .set('Authorization', `Bearer ${token}`)
      .send({
        objetivo: 'ganar_musculo',
        diasSemana: 4,
        tiempoMin: 45,
        lesiones: ['rodilla', 'hombro']
      });
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
    expect(res.body.data.objetivo).toBe('ganar_musculo');
    expect(res.body.data.diasSemana).toBe(4);
    expect(res.body.data.tiempoMin).toBe(45);
    expect(res.body.data.lesiones).toEqual(['rodilla', 'hombro']);
  });

  test('rechaza objetivo inválido', async () => {
    const res = await request(app)
      .put('/api/planes/onboarding')
      .set('Authorization', `Bearer ${token}`)
      .send({ objetivo: 'volar', diasSemana: 4, tiempoMin: 45 });
    expect(res.status).toBe(400);
    expect(res.body.ok).toBe(false);
  });

  test('rechaza diasSemana fuera de rango (2 o 7)', async () => {
    const r1 = await request(app)
      .put('/api/planes/onboarding')
      .set('Authorization', `Bearer ${token}`)
      .send({ objetivo: 'resistencia', diasSemana: 2, tiempoMin: 45 });
    expect(r1.status).toBe(400);

    const r2 = await request(app)
      .put('/api/planes/onboarding')
      .set('Authorization', `Bearer ${token}`)
      .send({ objetivo: 'resistencia', diasSemana: 7, tiempoMin: 45 });
    expect(r2.status).toBe(400);
  });

  test('rechaza tiempoMin fuera de rango (20 o 120)', async () => {
    const r1 = await request(app)
      .put('/api/planes/onboarding')
      .set('Authorization', `Bearer ${token}`)
      .send({ objetivo: 'rendimiento', diasSemana: 4, tiempoMin: 20 });
    expect(r1.status).toBe(400);

    const r2 = await request(app)
      .put('/api/planes/onboarding')
      .set('Authorization', `Bearer ${token}`)
      .send({ objetivo: 'rendimiento', diasSemana: 4, tiempoMin: 120 });
    expect(r2.status).toBe(400);
  });

  test('lesiones desconocidas se filtran (no rompe)', async () => {
    const res = await request(app)
      .put('/api/planes/onboarding')
      .set('Authorization', `Bearer ${token}`)
      .send({
        objetivo: 'perder_grasa',
        diasSemana: 5,
        tiempoMin: 60,
        lesiones: ['rodilla', 'nariz', 'codo', '']
      });
    expect(res.status).toBe(200);
    expect(res.body.data.lesiones).toEqual(['rodilla', 'codo']);
  });

  test('lesiones vacías acepta', async () => {
    const res = await request(app)
      .put('/api/planes/onboarding')
      .set('Authorization', `Bearer ${token}`)
      .send({ objetivo: 'resistencia', diasSemana: 3, tiempoMin: 30, lesiones: [] });
    expect(res.status).toBe(200);
    expect(res.body.data.lesiones).toEqual([]);
  });

  test('persiste en BD: actualiza usuarios.dias_entreno_semana + settings.objetivo_fitness', async () => {
    await request(app)
      .put('/api/planes/onboarding')
      .set('Authorization', `Bearer ${token}`)
      .send({ objetivo: 'rendimiento', diasSemana: 6, tiempoMin: 90, lesiones: [] });

    const u = memDb.state.usuarios.find((x) => x.id_usuario === userId);
    expect(u.dias_entreno_semana).toBe(6);

    const s = memDb.state.settings.find((x) => x.usuarios_id_usuario === userId);
    expect(s).toBeTruthy();
    expect(s.objetivo_fitness).toBe('rendimiento');
    expect(s.tiempo_disponible_min).toBe(90);
  });

  test('sin token devuelve 401', async () => {
    const res = await request(app)
      .put('/api/planes/onboarding')
      .send({ objetivo: 'resistencia', diasSemana: 3, tiempoMin: 30 });
    expect(res.status).toBe(401);
  });
});
