/**
 * E2E auditoría: coherencia entre plan → entreno → historial → GPS.
 */
process.env.JWT_SECRET = 'opofit-e2e-auditoria';
process.env.NOTIFICATIONS_CRON = 'false';

jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);

const memDb = require('./helpers/inMemoryDb');
const { seedAll } = require('./helpers/seed');
const { buildApp } = require('./helpers/buildApp');
const request = require('supertest');

let app;
let token;
let userId;

function seedPlanBasico() {
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
      plan_dias_id: 1,
      orden: 2,
      nombre_prescripcion: 'Sentadilla trasera',
      ejercicios_id_ejercicio: 6,
      series: 4,
      repeticiones: 6,
      descanso: 120,
      notas: 'reps'
    },
    {
      plan_dias_id: 2,
      orden: 1,
      nombre_prescripcion: 'Carrera continua Z2 30 min',
      ejercicios_id_ejercicio: 4,
      series: 1,
      repeticiones: 30,
      descanso: 0,
      notas: 'min'
    }
  );
  s.ejercicios.forEach((e) => {
    if (!e.modalidad) e.modalidad = e.pilar === 'RESISTENCIA' ? 'cardio' : 'series';
    if (!e.score_tipo) e.score_tipo = e.pilar === 'RESISTENCIA' ? 'tiempo' : 'reps';
    if (!e.entornos) e.entornos = 'GYM,MIXTO';
    if (!e.instrucciones_tecnicas) e.instrucciones_tecnicas = `Instrucciones de ${e.nombre}.`;
  });
}

beforeAll(async () => {
  memDb.reset();
  await seedAll(memDb);
  seedPlanBasico();
  app = buildApp();

  const login = await request(app).post('/api/auth/login').send({
    email: 'user1@opofit.test',
    password: 'Password123!'
  });
  token = login.body.token;
  userId = login.body.user?.id_usuario;
  const u = memDb.state.usuarios.find((x) => x.id_usuario === userId);
  if (u) u.es_premium = 0;

  await request(app)
    .put('/api/planes/entorno')
    .set('Authorization', `Bearer ${token}`)
    .send({ entorno: 'GYM' });
  await request(app)
    .put('/api/user/perfil')
    .set('Authorization', `Bearer ${token}`)
    .send({
      nuevasMarcas: [
        { id_prueba: 1, valor: 12.5 },
        { id_prueba: 2, valor: 12 },
        { id_prueba: 3, valor: 15.5 }
      ]
    });
});

const auth = () => ({ Authorization: `Bearer ${token}` });

describe('Auditoría E2E: plan → historial → GPS', () => {
  test('plan del día incluye ejercicios con nombre y metadatos', async () => {
    const res = await request(app)
      .get(`/api/rutinas/mi-entrenamiento/${userId}/1`)
      .set(auth());
    expect(res.status).toBe(200);
    expect(res.body.data.planSemanal).toBeTruthy();
    const ej = res.body.data.planSemanal.semana.flatMap((d) => d.ejercicios);
    expect(ej.length).toBeGreaterThan(0);
    ej.forEach((e) => {
      expect(e.nombre).toBeTruthy();
      expect(e.series).toBeGreaterThan(0);
    });
    expect(res.body.data.planSemanal.personalizacion?.explicacion_ia).toBeTruthy();
  });

  test('registrar entreno refleja nombres reales en historial de sesiones', async () => {
    const plan = await request(app)
      .get(`/api/rutinas/mi-entrenamiento/${userId}/1`)
      .set(auth());
    const hoy =
      plan.body.data.planSemanal.semana.find((d) => d.es_hoy) ||
      plan.body.data.planSemanal.semana[0];
    const ejercicios = hoy.ejercicios.slice(0, 2).map((e) => ({
      id_ejercicio: e.id_ejercicio,
      valor: e.pilar === 'RESISTENCIA' ? 25 : 10
    }));

    const reg = await request(app)
      .post('/api/historial/registrar')
      .set(auth())
      .send({
        tipoRutina: 'OPO',
        idRutina: hoy.rutinas_opo_id || 1,
        duracion: 45,
        ejercicios
      });
    expect(reg.status).toBe(200);

    const hist = await request(app).get(`/api/historial/sesiones/${userId}`).set(auth());
    expect(hist.status).toBe(200);
    expect(hist.body.data?.length).toBeGreaterThan(0);
    const nombres = hist.body.data.flatMap((s) =>
      (s.ejercicios || []).map((e) => e.nombre_ejercicio)
    );
    nombres.forEach((n) => expect(n).not.toMatch(/^Ejercicio \d+$/));
  });

  test('actividad GPS se guarda y aparece en el listado', async () => {
    const ahora = Date.now();
    const uuid = `audit-gps-${ahora}`;
    const gps = await request(app)
      .post('/api/gps/actividades')
      .set(auth())
      .send({
        id: uuid,
        type: 'RUN',
        startedAtMs: ahora - 3600000,
        endedAtMs: ahora,
        durationSec: 1800,
        distanceM: 5200
      });
    expect(gps.status).toBe(200);

    const list = await request(app).get('/api/gps/actividades').set(auth());
    expect(list.status).toBe(200);
    expect(list.body.data.some((a) => a.id === uuid || a.uuid === uuid)).toBe(true);
  });

  test('segundo registro del mismo ejercicio no genera falso récord PR', async () => {
    const ejId = memDb.state.ejercicios[0].id_ejercicio;
    await request(app)
      .post('/api/historial/registrar')
      .set(auth())
      .send({
        tipoRutina: 'OPO',
        idRutina: 1,
        duracion: 30,
        ejercicios: [{ id_ejercicio: ejId, valor: 12 }]
      });
    const segundo = await request(app)
      .post('/api/historial/registrar')
      .set(auth())
      .send({
        tipoRutina: 'OPO',
        idRutina: 1,
        duracion: 30,
        ejercicios: [{ id_ejercicio: ejId, valor: 11 }]
      });
    expect(segundo.status).toBe(200);
    const records = segundo.body.data?.recordsRotos || [];
    expect(records.length).toBe(0);
  });

  test('listado de ejercicios incluye instrucciones técnicas', async () => {
    const res = await request(app).get('/api/ejercicios').set(auth());
    expect(res.status).toBe(200);
    const conInstr = res.body.data?.find((e) => e.instrucciones_tecnicas?.length > 10);
    expect(conInstr).toBeTruthy();
  });
});
