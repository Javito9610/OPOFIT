/**
 * E2E centrado en el flujo completo de simulacro -> marcas perfil -> nivel.
 */
process.env.JWT_SECRET = 'opofit-e2e-sim';
process.env.NOTIFICATIONS_CRON = 'false';

jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);

const memDb = require('./helpers/inMemoryDb');
const { seedAll } = require('./helpers/seed');
const { buildApp } = require('./helpers/buildApp');
const request = require('supertest');

let app;
let user;

beforeAll(async () => {
  memDb.reset();
  await seedAll(memDb);
  app = buildApp();
  const reg = await request(app).post('/api/auth/registrar').send({
    nombre: 'Sim User',
    email: 'sim@opofit.test',
    password: 'Password1',
    genero: 'HOMBRE',
    peso: 75,
    altura: 180,
    oposiciones_id_oposicion: 1
  });
  user = { id: reg.body.userId, token: reg.body.token };
});

describe('E2E Simulacro', () => {
  test('Listar pruebas del simulacro requiere oposicion valida', async () => {
    const r = await request(app).get('/api/simulacros/pruebas/1')
      .set('Authorization', `Bearer ${user.token}`);
    expect(r.status).toBe(200);
    expect(r.body.data.length).toBeGreaterThanOrEqual(3); // opo 1 tiene 3 pruebas
  });

  test('Listar pruebas con oposicion inexistente devuelve 404', async () => {
    const r = await request(app).get('/api/simulacros/pruebas/999')
      .set('Authorization', `Bearer ${user.token}`);
    expect(r.status).toBe(404);
  });

  test('Guardar simulacro completo y obtener nota media', async () => {
    const r = await request(app).post('/api/simulacros/guardar')
      .set('Authorization', `Bearer ${user.token}`)
      .send({
        idOposicion: 1,
        resultados: [
          { id_prueba: 1, valor: 12 }, // 100m a 12s -> nota 8 (en baremo HOMBRE 100m)
          { id_prueba: 2, valor: 15 }, // Dominadas 15 -> nota 6
          { id_prueba: 3, valor: 14 }  // Circuito 14s -> nota 10
        ]
      });
    expect(r.status).toBe(200);
    expect(r.body.ok).toBe(true);
    expect(r.body.data.idSimulacro).toBeDefined();
    expect(parseFloat(r.body.data.notaMedia)).toBeGreaterThan(0);
  });

  test('Datos de simulacro incompletos devuelven 400', async () => {
    const r = await request(app).post('/api/simulacros/guardar')
      .set('Authorization', `Bearer ${user.token}`)
      .send({ idOposicion: 1, resultados: [] });
    expect(r.status).toBe(400);
  });

  test('Historial de simulacros bloqueado a no-premium', async () => {
    const r = await request(app).get('/api/simulacros/historial/1')
      .set('Authorization', `Bearer ${user.token}`);
    expect(r.status).toBe(402);
    expect(r.body.code).toBe('PREMIUM_REQUIRED');
  });

  test('Aplicar marcas al perfil desde simulacro - mejora nivel', async () => {
    const r = await request(app).post('/api/simulacros/aplicar-marcas')
      .set('Authorization', `Bearer ${user.token}`)
      .send({
        idOposicion: 1,
        resultados: [
          { id_prueba: 1, valor: 11 }, // 11s -> 10
          { id_prueba: 2, valor: 25 }, // 25 reps -> 10
          { id_prueba: 3, valor: 14 }  // 14s -> 10
        ]
      });
    expect(r.status).toBe(200);
    expect(r.body.ok).toBe(true);
    expect(r.body.data.perfil.nivelTrasSimulacro).toBe('AVANZADO');
  });
});

describe('E2E Dashboard', () => {
  test('Dashboard resumen exige idOposicion en query o perfil', async () => {
    // El usuario fue creado con opo 1, asi que debe funcionar sin query
    const r = await request(app).get('/api/dashboard/resumen')
      .set('Authorization', `Bearer ${user.token}`);
    expect(r.status).toBe(200);
    expect(r.body.data).toBeDefined();
    expect(r.body.data.oposicionNombre).toBeDefined();
  });

  test('Dashboard con idOposicion en query', async () => {
    const r = await request(app).get('/api/dashboard/resumen?idOposicion=1')
      .set('Authorization', `Bearer ${user.token}`);
    expect(r.status).toBe(200);
  });
});

describe('E2E Ranking', () => {
  test('Ranking publico funciona', async () => {
    const r = await request(app).get('/api/ranking/1')
      .set('Authorization', `Bearer ${user.token}`);
    expect(r.status).toBe(200);
    expect(Array.isArray(r.body.data)).toBe(true);
  });

  test('Mi posicion en ranking', async () => {
    const r = await request(app).get('/api/ranking/1/mi-posicion')
      .set('Authorization', `Bearer ${user.token}`);
    expect(r.status).toBe(200);
    expect(r.body.data).toBeDefined();
  });

  test('Toggle perfil publico', async () => {
    const r = await request(app).put('/api/ranking/perfil-publico')
      .set('Authorization', `Bearer ${user.token}`)
      .send({ publico: false });
    expect(r.status).toBe(200);
    expect(r.body.perfilPublico).toBe(false);
  });
});

describe('E2E coherencia transversal', () => {
  test('Nivel calculado coincide entre dashboard y rutinas', async () => {
    const dash = await request(app).get('/api/dashboard/resumen?idOposicion=1')
      .set('Authorization', `Bearer ${user.token}`);
    const rut = await request(app).get(`/api/rutinas/mi-entrenamiento/${user.id}/1`)
      .set('Authorization', `Bearer ${user.token}`);
    expect(dash.status).toBe(200);
    if (rut.status === 200 && rut.body.data?.nivelAsignado) {
      expect(dash.body.data.nivel).toBe(rut.body.data.nivelAsignado);
    }
  });
});
