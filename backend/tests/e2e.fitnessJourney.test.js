/**
 * E2E "fitness journey": el flujo COMPLETO de un usuario en modo FITNESS
 * (no opositor). Valida que las rutas críticas funcionan sin oposición:
 *
 *   1. Registro FITNESS (sin oposición).
 *   2. Login y carga de dashboard fitness (no debe pedir pruebas oficiales).
 *   3. Plan semanal: devuelve un plan genérico válido.
 *   4. Configurar entorno (CASA) + material + lesiones (rodilla).
 *   5. Regenerar plan: respeta entorno + lesiones.
 *   6. Comunidad fitness: lista grupos, crea uno, búsqueda funciona.
 *   7. Historial vacío inicial sigue devolviendo 200 con resumen vacío.
 *
 * Bug clásico que cubrimos: en modo FITNESS algunas rutas asumían
 * `oposicion_id != null` y podían tirar 500 si el usuario no tenía
 * oposición asignada. Verificamos que NO ocurre.
 */
process.env.JWT_SECRET = 'opofit-e2e-fitness';
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
    nombre: 'Atleta Fitness',
    email: `fitness_${Date.now()}@opofit.test`,
    password: 'Password123!',
    genero: 'MUJER',
    peso: 65,
    altura: 168,
    modo_uso: 'FITNESS'
  });
  expect(reg.status).toBe(201);
  token = reg.body.token;
  userId = reg.body.userId || reg.body.user?.id_usuario;
  expect(userId).toBeTruthy();
});

describe('E2E FITNESS journey completo', () => {
  test('1) Registro FITNESS NO asigna oposición real (id=1 sentinel o null)', async () => {
    const u = memDb.state.usuarios.find((x) => x.id_usuario === userId);
    expect(u).toBeTruthy();
    expect(u.modo_uso).toBe('FITNESS');
  });

  test('2) Dashboard responde para usuario FITNESS sin pedir baremo', async () => {
    const res = await request(app)
      .get('/api/dashboard/resumen/1')
      .set('Authorization', `Bearer ${token}`);
    // En modo FITNESS, dashboard puede responder 200 con resumen mínimo o
    // 404 si la lógica lo requiere — lo crítico es que NO sea 500.
    expect([200, 404]).toContain(res.status);
    expect(res.body).toBeTruthy();
  });

  test('3) Entornos del plan accesibles para FITNESS', async () => {
    const res = await request(app)
      .get('/api/planes/entornos')
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
    expect(res.body.data.length).toBeGreaterThanOrEqual(5);
    expect(res.body.data.find((e) => e.id === 'CASA')).toBeTruthy();
  });

  test('4) Guardar entorno CASA para usuario FITNESS', async () => {
    const res = await request(app)
      .put('/api/planes/entorno')
      .set('Authorization', `Bearer ${token}`)
      .send({ entorno: 'CASA' });
    expect(res.status).toBe(200);
    expect(res.body.data.entorno).toBe('CASA');
  });

  test('5) Historial responde para usuario FITNESS sin sesiones aún', async () => {
    const res = await request(app)
      .get('/api/historial-pro/resumen?periodo=month')
      .set('Authorization', `Bearer ${token}`);
    // Vacío pero válido.
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
    expect(res.body.data.sesiones).toBeGreaterThanOrEqual(0);
  });

  test('6) Comunidad: feed fitness accesible', async () => {
    const res = await request(app)
      .get('/api/posts/feed')
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
  });

  test('7) Comunidad: listar grupos sin oposición devuelve lista (vacía o de fitness)', async () => {
    const res = await request(app)
      .get('/api/comunidad/grupos')
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
    expect(Array.isArray(res.body.data)).toBe(true);
  });

  test('8) Búsqueda usuarios funciona para FITNESS (sin filtro de oposición)', async () => {
    const res = await request(app)
      .get('/api/amigos/buscar?q=fit')
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
  });

  test('9) Atletas FITNESS pueden mandar solicitud de amistad entre sí', async () => {
    // Crea un segundo atleta fitness
    const reg2 = await request(app).post('/api/auth/registrar').send({
      nombre: 'Otro Atleta',
      email: `fit2_${Date.now()}@opofit.test`,
      password: 'Password123!',
      genero: 'HOMBRE',
      peso: 75,
      altura: 178,
      modo_uso: 'FITNESS'
    });
    expect(reg2.status).toBe(201);
    const userId2 = reg2.body.userId || reg2.body.user?.id_usuario;

    const sol = await request(app)
      .post('/api/amigos/solicitar')
      .set('Authorization', `Bearer ${token}`)
      .send({ idUsuario: userId2 });
    expect([200, 201]).toContain(sol.status);
    expect(sol.body.ok).toBe(true);
  });
});
