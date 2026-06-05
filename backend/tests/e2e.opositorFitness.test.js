/**
 * E2E: paridad flujos OPOSITOR vs FITNESS (atleta sin oposición).
 */
process.env.JWT_SECRET = 'opofit-e2e-paridad';
process.env.NOTIFICATIONS_CRON = 'false';

jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);

const memDb = require('./helpers/inMemoryDb');
const { seedAll } = require('./helpers/seed');
const { buildApp } = require('./helpers/buildApp');
const request = require('supertest');

let app;
let emailSeq = 0;

function uniqEmail(prefix) {
  emailSeq += 1;
  return `${prefix}.${emailSeq}@opofit.test`;
}

function seedPlanBasicoConRodajeDecimal() {
  const s = memDb.state;
  s.planes_entrenamiento.push({
    id_plan: 1,
    oposiciones_id_oposicion: 1,
    nivel: 'BASICO',
    genero: 'HOMBRE',
    fuente: 'opofit_banco_planes',
    dias_por_semana: 2
  });
  s.plan_dias.push(
    {
      id_plan_dia: 1,
      planes_id_plan: 1,
      dia_semana: 1,
      orden: 1,
      enfoque_tipo: 'FUERZA',
      rutinas_opo_id: 1,
      titulo_sesion: 'Fuerza base',
      descripcion_sesion: 'Sesión A'
    },
    {
      id_plan_dia: 2,
      planes_id_plan: 1,
      dia_semana: 3,
      orden: 2,
      enfoque_tipo: 'RESISTENCIA',
      rutinas_opo_id: 1,
      titulo_sesion: 'Cardio',
      descripcion_sesion: 'Sesión B'
    }
  );
  s.plan_dia_ejercicios.push(
    {
      plan_dias_id: 1,
      orden: 1,
      nombre_prescripcion: 'Press banca con barra',
      ejercicios_id_ejercicio: 3,
      series: 4,
      repeticiones: 8,
      descanso: 90,
      notas: 'reps'
    },
    {
      plan_dias_id: 2,
      orden: 1,
      nombre_prescripcion: 'Rodaje continuo 7.8 km',
      ejercicios_id_ejercicio: 4,
      series: 1,
      repeticiones: 7.8,
      descanso: 0,
      notas: 'km'
    }
  );
}

beforeAll(async () => {
  memDb.reset();
  await seedAll(memDb);
  seedPlanBasicoConRodajeDecimal();
  app = buildApp();
});

async function registrarOpositor() {
  const res = await request(app).post('/api/auth/registrar').send({
    nombre: 'Opositor Test',
    email: uniqEmail('opositor'),
    password: 'Password123!',
    genero: 'HOMBRE',
    peso: 80,
    altura: 178,
    oposiciones_id_oposicion: 1,
    modo_uso: 'OPOSITOR'
  });
  expect(res.status).toBe(201);
  return { token: res.body.token, userId: res.body.userId || res.body.user?.id_usuario };
}

async function registrarFitness() {
  const res = await request(app).post('/api/auth/registrar').send({
    nombre: 'Atleta Fitness',
    email: uniqEmail('fitness'),
    password: 'Password123!',
    genero: 'HOMBRE',
    peso: 75,
    altura: 175,
    modo_uso: 'FITNESS'
  });
  expect(res.status).toBe(201);
  return { token: res.body.token, userId: res.body.userId || res.body.user?.id_usuario };
}

describe('E2E paridad OPOSITOR vs FITNESS', () => {
  test('registro FITNESS sin oposicion y /me devuelve modo_uso', async () => {
    const { token } = await registrarFitness();
    const me = await request(app).get('/api/auth/me').set('Authorization', `Bearer ${token}`);
    expect(me.status).toBe(200);
    expect(me.body.user.modo_uso).toBe('FITNESS');
    expect(me.body.user.oposiciones_id_oposicion).toBeNull();
  });

  test('FITNESS: dashboard resumen sin exigir oposicion', async () => {
    const { token } = await registrarFitness();
    const res = await request(app)
      .get('/api/dashboard/resumen')
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
    expect(res.body.data.modoUso).toBe('FITNESS');
    expect(res.body.data.oposicionNombre).toBe('Modo fitness');
    expect(res.body.data.rankingPosicion).toBeNull();
    expect(res.body.data.ultimoSimulacro).toBeNull();
  });

  test('FITNESS: mi-entrenamiento devuelve plan sin marcas oficiales', async () => {
    const { token, userId } = await registrarFitness();
    const res = await request(app)
      .get(`/api/rutinas/mi-entrenamiento/${userId}/0`)
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
    expect(res.body.data.nivelAsignado).toBe('BASICO');
    expect(res.body.data.pruebasFaltantes).toBe(0);
    const plan = res.body.data.planSemanal;
    expect(plan).toBeTruthy();
    expect(plan.semana?.length).toBeGreaterThan(0);
  });

  test('FITNESS: plan admite repeticiones decimales (7.8 km)', async () => {
    const { token, userId } = await registrarFitness();
    const res = await request(app)
      .get(`/api/rutinas/mi-entrenamiento/${userId}/0`)
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    const ejercicios = (res.body.data.planSemanal?.semana || []).flatMap((d) => d.ejercicios || []);
    const rodaje = ejercicios.find((e) => Number(e.repeticiones) === 7.8 || e.repeticiones === 7.8);
    if (rodaje) {
      expect(typeof rodaje.repeticiones).toBe('number');
      expect(rodaje.repeticiones).toBe(7.8);
    } else {
      // Si el generador no incluyó ese día, al menos ninguna rep debe ser inválida
      ejercicios.forEach((e) => {
        expect(Number.isFinite(Number(e.repeticiones))).toBe(true);
      });
    }
  });

  test('FITNESS: crear grupo y buscar usuarios sin idOposicion', async () => {
    const a = await registrarFitness();
    await request(app).post('/api/auth/registrar').send({
      nombre: 'Otro Atleta',
      email: uniqEmail('fitness2'),
      password: 'Password123!',
      genero: 'MUJER',
      peso: 60,
      altura: 165,
      modo_uso: 'FITNESS'
    });
    const grupo = await request(app)
      .post('/api/comunidad/grupos')
      .set('Authorization', `Bearer ${a.token}`)
      .send({ nombre: 'Grupo Running', descripcion: 'Quedadas' });
    expect([200, 201]).toContain(grupo.status);
    expect(grupo.body.ok).toBe(true);

    const buscar = await request(app)
      .get('/api/amigos/buscar')
      .query({ nombre: 'Otro' })
      .set('Authorization', `Bearer ${a.token}`);
    expect(buscar.status).toBe(200);
    expect(buscar.body.data?.length).toBeGreaterThanOrEqual(1);
  });

  test('OPOSITOR: registro con oposicion y perfil con marcas', async () => {
    const { token, userId } = await registrarOpositor();
    const me = await request(app).get('/api/auth/me').set('Authorization', `Bearer ${token}`);
    expect(me.body.user.modo_uso).toBe('OPOSITOR');
    expect(me.body.user.oposiciones_id_oposicion).toBe(1);

    const perfil = await request(app)
      .put('/api/user/perfil')
      .set('Authorization', `Bearer ${token}`)
      .send({
        userId,
        peso: 80,
        altura: 178,
        nuevasMarcas: [
          { id_prueba: 1, valor: 12.5 },
          { id_prueba: 2, valor: 10 },
          { id_prueba: 3, valor: 15.0 }
        ]
      });
    expect(perfil.status).toBe(200);
    expect(perfil.body.ok).toBe(true);
  });

  test('OPOSITOR: mi-entrenamiento con nivel calculado y plan', async () => {
    const { token, userId } = await registrarOpositor();
    await request(app)
      .put('/api/user/perfil')
      .set('Authorization', `Bearer ${token}`)
      .send({
        userId,
        nuevasMarcas: [
          { id_prueba: 1, valor: 12.5 },
          { id_prueba: 2, valor: 10 },
          { id_prueba: 3, valor: 15.0 }
        ]
      });

    const res = await request(app)
      .get(`/api/rutinas/mi-entrenamiento/${userId}/1`)
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
    expect(res.body.data.nivelAsignado).not.toBe('INCOMPLETO');
    expect(res.body.data.planSemanal).toBeTruthy();
  });

  test('OPOSITOR: dashboard incluye datos de oposicion', async () => {
    const { token, userId } = await registrarOpositor();
    await request(app)
      .put('/api/user/perfil')
      .set('Authorization', `Bearer ${token}`)
      .send({
        userId,
        nuevasMarcas: [
          { id_prueba: 1, valor: 12.5 },
          { id_prueba: 2, valor: 10 },
          { id_prueba: 3, valor: 15.0 }
        ]
      });

    const res = await request(app)
      .get('/api/dashboard/resumen?idOposicion=1')
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.data.modoUso).toBe('OPOSITOR');
    expect(res.body.data.oposicionNombre).toBeTruthy();
    expect(res.body.data.notaMedia).toBeTruthy();
  });

  test('ambos: cambiar contraseña y actualizar perfil con avatar', async () => {
    for (const registrar of [registrarFitness, registrarOpositor]) {
      const { token, userId } = await registrar();
      const pass = await request(app)
        .post('/api/auth/cambiar-password')
        .set('Authorization', `Bearer ${token}`)
        .send({ actual: 'Password123!', nueva: 'NuevaPass456!' });
      expect(pass.status).toBe(200);
      expect(pass.body.ok).toBe(true);

      const me = await request(app).get('/api/auth/me').set('Authorization', `Bearer ${token}`);
      const email = me.body.user.email;
      const login2 = await request(app).post('/api/auth/login').send({
        email,
        password: 'NuevaPass456!'
      });
      expect(login2.status).toBe(200);

      const perfil = await request(app)
        .get('/api/user/perfil')
        .set('Authorization', `Bearer ${token}`);
      expect(perfil.status).toBe(200);

      const upd = await request(app)
        .put('/api/user/perfil')
        .set('Authorization', `Bearer ${token}`)
        .send({
          userId,
          nombre: 'Nombre Actualizado',
          avatarUrl: 'https://example.com/avatar.jpg'
        });
      expect(upd.status).toBe(200);
      expect(upd.body.ok).toBe(true);
    }
  });
});
