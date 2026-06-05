/**
 * E2E: plan inteligente por entorno, cache, regeneración y API.
 */
process.env.JWT_SECRET = 'opofit-e2e-plan-ia';
process.env.NOTIFICATIONS_CRON = 'false';

jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);

const memDb = require('./helpers/inMemoryDb');
const { seedAll } = require('./helpers/seed');
const { buildApp } = require('./helpers/buildApp');
const PlanGeneradorService = require('../src/services/PlanGeneradorService');
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

  const casaEjercicios = [
    ['Flexiones estándar', 'FUERZA', 'Pecho', 'Suelo', 'CASA,CALISTENIA', 'PUSH'],
    ['Sentadilla con mochila', 'FUERZA', 'Pierna', 'Mochila', 'CASA', 'SQUAT'],
    ['Carrera en el sitio Z2 15 min', 'RESISTENCIA', 'Cardio', 'Suelo', 'CASA', 'RUN'],
    ['Burpees', 'RESISTENCIA', 'Cardio', 'Suelo', 'CASA,CROSSFIT', 'RUN'],
    ['Flexiones inclinadas (mesa)', 'FUERZA', 'Pecho', 'Mesa', 'CASA', 'PUSH'],
    ['Zancadas en el sitio', 'FUERZA', 'Pierna', 'Suelo', 'CASA', 'SQUAT'],
    ['Mountain climbers', 'CORE', 'Core', 'Suelo', 'CASA', 'PLANK'],
    ['Puente de glúteo', 'FUERZA', 'Glúteo', 'Suelo', 'CASA', 'SQUAT']
  ];
  let id = s.ejercicios.length + 1;
  for (const [nombre, pilar, grupo, equip, entornos, ilust] of casaEjercicios) {
    if (s.ejercicios.some((e) => e.nombre === nombre)) continue;
    s.ejercicios.push({
      id_ejercicio: id++,
      nombre,
      pilar,
      grupo_muscular: grupo,
      equipamiento: equip,
      entornos,
      tipo_ilustracion: ilust,
      categoria: pilar === 'RESISTENCIA' ? 'Cardio' : 'Fuerza',
      video_url: null,
      instrucciones_tecnicas: `Tip: ${nombre}`,
      animacion_url: null
    });
  }
  s.ejercicios.forEach((e) => {
    if (!e.entornos) e.entornos = 'GYM,MIXTO';
    if (!e.grupo_muscular) e.grupo_muscular = 'General';
    if (!e.tipo_ilustracion) e.tipo_ilustracion = 'GENERAL';
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
  expect(login.status).toBe(200);
  token = login.body.token;
  userId = login.body.user?.id_usuario;
  expect(userId).toBeTruthy();
});

describe('E2E plan inteligente por entorno', () => {
  test('GET /api/planes/entornos lista opciones', async () => {
    const res = await request(app)
      .get('/api/planes/entornos')
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
    expect(res.body.data.length).toBeGreaterThanOrEqual(5);
    expect(res.body.data.find((e) => e.id === 'CASA')).toBeTruthy();
  });

  test('PUT /api/planes/entorno guarda preferencia', async () => {
    const res = await request(app)
      .put('/api/planes/entorno')
      .set('Authorization', `Bearer ${token}`)
      .send({ entorno: 'CASA' });
    expect(res.status).toBe(200);
    expect(res.body.data.entorno).toBe('CASA');
    const u = memDb.state.usuarios.find((x) => x.id_usuario === userId);
    expect(u.entorno_entreno).toBe('CASA');
  });

  test('mi-entrenamiento incluye plan con sustituciones para CASA', async () => {
    const marcas = [
      { id_prueba: 1, valor: 12.5 },
      { id_prueba: 2, valor: 12 },
      { id_prueba: 3, valor: 15.5 }
    ];
    await request(app)
      .put('/api/user/perfil')
      .set('Authorization', `Bearer ${token}`)
      .send({ nuevasMarcas: marcas });

    const res = await request(app)
      .get(`/api/rutinas/mi-entrenamiento/${userId}/1`)
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
    const plan = res.body.data.planSemanal;
    expect(plan).toBeTruthy();
    expect(plan.semana.length).toBeGreaterThan(0);
    const nombres = plan.semana.flatMap((d) => d.ejercicios.map((e) => e.nombre));
    const adaptado =
      nombres.some((n) => /flexion|mochila|sitio|burpee/i.test(n)) ||
      plan.personalizacion?.sustituciones > 0;
    expect(adaptado).toBe(true);
    expect(plan.personalizacion?.entorno_entreno).toBe('CASA');
    expect(plan.personalizacion?.explicacion_ia).toBeTruthy();
    const conIlust = plan.semana.flatMap((d) => d.ejercicios).find((e) => e.tipo_ilustracion);
    expect(conIlust?.tipo_ilustracion).toBeTruthy();
  });

  test('POST regenerar devuelve plan distinto', async () => {
    const res1 = await request(app)
      .get(`/api/rutinas/mi-entrenamiento/${userId}/1`)
      .set('Authorization', `Bearer ${token}`);
    const n1 = res1.body.data.planSemanal.semana
      .flatMap((d) => d.ejercicios.map((e) => e.nombre))
      .join('|');

    const reg = await request(app)
      .post('/api/planes/regenerar/1')
      .set('Authorization', `Bearer ${token}`);
    expect(reg.status).toBe(200);
    expect(reg.body.ok).toBe(true);
    expect(reg.body.data.semana.length).toBeGreaterThan(0);

    const n2 = reg.body.data.semana.flatMap((d) => d.ejercicios.map((e) => e.nombre)).join('|');
    expect(n2).not.toBe(n1);
    const u = memDb.state.usuarios.find((x) => x.id_usuario === userId);
    expect(u.plan_variacion_seed).toBeGreaterThan(0);
  });

  test('cache no pisa personalización fresca', () => {
    const planActual = {
      dia_hoy: 1,
      personalizacion: { resumen: 'Nuevo resumen', racha_dias: 5, ajustes_aplicados: 3 },
      semana: [
        {
          id_plan_dia: 1,
          dia_semana: 1,
          es_hoy: true,
          completada: true,
          ejercicios: [{ nombre: 'Original', series: 5 }]
        }
      ]
    };
    const planCache = {
      personalizacion: { resumen: 'Viejo', racha_dias: 1 },
      semana: [
        {
          id_plan_dia: 1,
          ejercicios: [{ nombre: 'Flexiones estándar', series: 4, tipo_ilustracion: 'PUSH' }]
        }
      ]
    };
    const merged = PlanGeneradorService.combinarPlanConCache(planActual, planCache);
    expect(merged.personalizacion.resumen).toBe('Nuevo resumen');
    expect(merged.semana[0].completada).toBe(true);
    expect(merged.semana[0].ejercicios[0].nombre).toBe('Flexiones estándar');
  });

  test('PUT entorno inválido devuelve 400', async () => {
    const res = await request(app)
      .put('/api/planes/entorno')
      .set('Authorization', `Bearer ${token}`)
      .send({ entorno: 'PLAYA' });
    expect(res.status).toBe(400);
  });
});
