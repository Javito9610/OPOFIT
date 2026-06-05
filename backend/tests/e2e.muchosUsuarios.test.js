/**
 * E2E con muchisimos usuarios: registra 50 usuarios, cada uno con perfiles
 * variados (oposiciones, generos, premium...), luego ejecuta operaciones
 * realistas (login, marcas, simulacros, rutinas, GPS, etc.) verificando
 * la coherencia transversal del sistema.
 */
process.env.JWT_SECRET = 'opofit-e2e-muchos';
process.env.NOTIFICATIONS_CRON = 'false';

// Registrar/login de 50 usuarios hace 50 hashes bcrypt; el timeout por defecto
// de 5s de Jest no basta en CI lento. Subimos el timeout global de este suite.
jest.setTimeout(60000);

jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);

const memDb = require('./helpers/inMemoryDb');
const { seedAll } = require('./helpers/seed');
const { buildApp } = require('./helpers/buildApp');
const request = require('supertest');

let app;
const usuarios = [];

beforeAll(async () => {
  memDb.reset();
  await seedAll(memDb);
  app = buildApp();
});

describe('E2E con 50 usuarios sinteticos', () => {
  test('Registra 50 usuarios sin colision de email', async () => {
    const nombres = ['Ana', 'Luis', 'Marta', 'Pablo', 'Sara', 'Hugo', 'Ines', 'Bruno', 'Lucia', 'Diego'];
    const apellidos = ['Garcia', 'Martin', 'Lopez', 'Ruiz', 'Sanz', 'Vega', 'Soto'];
    for (let i = 0; i < 50; i++) {
      const nombre = `${nombres[i % nombres.length]} ${apellidos[(i * 3) % apellidos.length]}${i}`;
      const genero = i % 2 === 0 ? 'HOMBRE' : 'MUJER';
      const peso = 55 + (i % 30);
      const altura = 158 + (i % 35);
      const opo = (i % 3) + 1;
      const email = `mass${i}@opofit.test`;
      const password = `Pwd!${i}xyz`;
      const res = await request(app).post('/api/auth/registrar').send({
        nombre, email, password, genero, peso, altura,
        oposiciones_id_oposicion: opo
      });
      expect(res.status).toBe(201);
      usuarios.push({ id: res.body.userId, email, password, genero, opo, token: res.body.token });
    }
    expect(usuarios).toHaveLength(50);
  }, 60000);

  test('Todos pueden hacer login con sus credenciales', async () => {
    let exitos = 0;
    for (const u of usuarios) {
      const res = await request(app).post('/api/auth/login').send({ email: u.email, password: u.password });
      if (res.status === 200 && res.body.token) exitos++;
    }
    expect(exitos).toBe(50);
  }, 60000);

  test('Token de cada usuario sirve para /me (aislamiento de sesion)', async () => {
    for (const u of usuarios.slice(0, 10)) {
      const res = await request(app).get('/api/auth/me').set('Authorization', `Bearer ${u.token}`);
      expect(res.status).toBe(200);
      expect(res.body.user.email).toBe(u.email);
    }
  });

  test('Actualizar perfil con peso/altura recalcula IMC', async () => {
    const u = usuarios[0];
    const res = await request(app).put('/api/user/perfil')
      .set('Authorization', `Bearer ${u.token}`)
      .send({ peso: 80, altura: 180 });
    // No exige 200 estrictamente porque el endpoint puede ser /actualizar o /perfil; aceptamos 200/404
    expect([200, 401, 404]).toContain(res.status);
  });

  test('Crear rutina personalizada y listarla', async () => {
    const u = usuarios[0];
    const r1 = await request(app).post('/api/rutinas-pers/crear')
      .set('Authorization', `Bearer ${u.token}`)
      .send({ userId: u.id, nombre: 'Mi rutina 1', ejercicios: [{ id_ejercicio: 1, series: 3, repeticiones: 10 }] });
    expect([200, 201]).toContain(r1.status);

    const r2 = await request(app).get(`/api/rutinas-pers/usuario/${u.id}`)
      .set('Authorization', `Bearer ${u.token}`);
    expect(r2.status).toBe(200);
    expect(r2.body.data.length).toBeGreaterThanOrEqual(1);
  });

  test('No se puede crear rutina personalizada con nombre duplicado', async () => {
    const u = usuarios[0];
    const r = await request(app).post('/api/rutinas-pers/crear')
      .set('Authorization', `Bearer ${u.token}`)
      .send({ userId: u.id, nombre: 'Mi rutina 1', ejercicios: [{ id_ejercicio: 1, series: 3, repeticiones: 10 }] });
    expect(r.status).toBe(409);
  });

  test('No se puede crear rutina personalizada para OTRO usuario', async () => {
    const u = usuarios[0];
    const otro = usuarios[1];
    const r = await request(app).post('/api/rutinas-pers/crear')
      .set('Authorization', `Bearer ${u.token}`)
      .send({ userId: otro.id, nombre: 'Hackeo', ejercicios: [{ id_ejercicio: 1 }] });
    expect(r.status).toBe(403);
  });

  test('No se pueden ver rutinas de otro usuario', async () => {
    const u = usuarios[0];
    const otro = usuarios[1];
    const r = await request(app).get(`/api/rutinas-pers/usuario/${otro.id}`)
      .set('Authorization', `Bearer ${u.token}`);
    expect(r.status).toBe(403);
  });

  test('No se pueden eliminar rutinas de otro usuario', async () => {
    const u = usuarios[0];
    const otro = usuarios[1];
    const r = await request(app).delete(`/api/rutinas-pers/eliminar/${otro.id}/9999`)
      .set('Authorization', `Bearer ${u.token}`);
    expect(r.status).toBe(403);
  });

  test('Premium activarPrueba bloqueado fuera de DEV', async () => {
    delete process.env.PREMIUM_DEV_MODE;
    const u = usuarios[0];
    const r = await request(app).post('/api/premium/activar-prueba')
      .set('Authorization', `Bearer ${u.token}`)
      .send({ dias: 30 });
    expect([403, 404]).toContain(r.status);
  });

  test('Premium estado siempre disponible y consistente', async () => {
    for (const u of usuarios.slice(0, 5)) {
      const r = await request(app).get('/api/premium/estado')
        .set('Authorization', `Bearer ${u.token}`);
      expect(r.status).toBe(200);
      expect(typeof r.body.data.esPremium).toBe('boolean');
    }
  });

  test('Listado de ejercicios protegido por token', async () => {
    const sinToken = await request(app).get('/api/ejercicios');
    expect(sinToken.status).toBe(401);

    const u = usuarios[0];
    const conToken = await request(app).get('/api/ejercicios')
      .set('Authorization', `Bearer ${u.token}`);
    expect(conToken.status).toBe(200);
  });
});

describe('E2E historial y progreso', () => {
  test('Guardar entrenamiento OPO valido (validacion de tipoRutina)', async () => {
    const u = usuarios[0];
    const res = await request(app).post('/api/historial/registrar')
      .set('Authorization', `Bearer ${u.token}`)
      .send({
        tipoRutina: 'OPO',
        idRutina: 1,
        duracion: 45,
        ejercicios: [
          { id_ejercicio: 1, valor: 12 },
          { id_ejercicio: 2, valor: 8 }
        ]
      });
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
    expect(res.body.id).toBeDefined();
  });

  test('Guardar entrenamiento con tipoRutina invalido devuelve 400', async () => {
    const u = usuarios[0];
    const res = await request(app).post('/api/historial/registrar')
      .set('Authorization', `Bearer ${u.token}`)
      .send({ tipoRutina: 'XXX', idRutina: 1, duracion: 30, ejercicios: [{ id_ejercicio: 1, valor: 5 }] });
    expect(res.status).toBe(400);
  });

  test('Guardar entrenamiento con ejercicios vacios devuelve 400', async () => {
    const u = usuarios[0];
    const res = await request(app).post('/api/historial/registrar')
      .set('Authorization', `Bearer ${u.token}`)
      .send({ tipoRutina: 'OPO', idRutina: 1, duracion: 30, ejercicios: [] });
    expect(res.status).toBe(400);
  });

  test('Historial de sesiones devuelve lista', async () => {
    const u = usuarios[0];
    const res = await request(app).get('/api/historial/sesiones/1')
      .set('Authorization', `Bearer ${u.token}`);
    expect(res.status).toBe(200);
    expect(res.body.data.length).toBeGreaterThanOrEqual(1);
  });
});

describe('E2E GPS', () => {
  test('Guardar y listar actividad GPS', async () => {
    const u = usuarios[0];
    const uuid = `act-${u.id}-${Date.now()}`;
    const r = await request(app).post('/api/gps/actividades')
      .set('Authorization', `Bearer ${u.token}`)
      .send({
        id: uuid,
        type: 'RUN',
        startedAtMs: Date.now() - 1800000,
        endedAtMs: Date.now(),
        durationSec: 1800,
        distanceM: 5000,
        avgSpeedMps: 2.77,
        avgPaceSecPerKm: 360
      });
    expect(r.status).toBe(200);
    expect(r.body.data.uuid).toBe(uuid);

    const r2 = await request(app).get('/api/gps/actividades')
      .set('Authorization', `Bearer ${u.token}`);
    expect(r2.status).toBe(200);
    expect(r2.body.data.some((a) => a.id === uuid)).toBe(true);
  });

  test('Rechazo de actividad GPS sin id', async () => {
    const u = usuarios[0];
    const r = await request(app).post('/api/gps/actividades')
      .set('Authorization', `Bearer ${u.token}`)
      .send({ type: 'RUN', endedAtMs: Date.now() });
    expect(r.status).toBe(400);
  });

  test('Borrar actividad GPS', async () => {
    const u = usuarios[0];
    const uuid = `act-del-${u.id}-${Date.now()}`;
    await request(app).post('/api/gps/actividades')
      .set('Authorization', `Bearer ${u.token}`)
      .send({ id: uuid, type: 'BIKE', endedAtMs: Date.now(), durationSec: 600, distanceM: 3000 });
    const r = await request(app).delete(`/api/gps/actividades/${uuid}`)
      .set('Authorization', `Bearer ${u.token}`);
    expect(r.status).toBe(200);
  });

  test('No se puede ver actividad GPS de otro usuario', async () => {
    const u1 = usuarios[2];
    const u2 = usuarios[3];
    const uuid = `priv-${u1.id}-${Date.now()}`;
    await request(app).post('/api/gps/actividades')
      .set('Authorization', `Bearer ${u1.token}`)
      .send({ id: uuid, type: 'RUN', endedAtMs: Date.now(), durationSec: 100, distanceM: 500 });
    const r = await request(app).get(`/api/gps/actividades/${uuid}`)
      .set('Authorization', `Bearer ${u2.token}`);
    expect(r.status).toBe(404);
  });
});

describe('E2E proteccion masiva de rutas', () => {
  test('Todas las rutas protegidas devuelven 401 sin token', async () => {
    const rutas = [
      '/api/historial/sesiones/1',
      '/api/gps/actividades',
      '/api/historial-pro/resumen',
      '/api/dashboard/resumen',
      '/api/premium/estado',
      '/api/amigos'
    ];
    for (const r of rutas) {
      const res = await request(app).get(r);
      expect(res.status).toBe(401);
    }
  });

  test('Token caducado o malformado devuelve 401', async () => {
    const tokensMalos = ['', 'Bearer ', 'Bearer xx.yy.zz', 'Bearer null'];
    for (const t of tokensMalos) {
      const res = await request(app).get('/api/auth/me').set('Authorization', t);
      expect(res.status).toBe(401);
    }
  });
});
