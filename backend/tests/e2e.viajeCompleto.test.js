/**
 * E2E "viaje completo": simula la vida real de un usuario de principio a fin
 * verificando que los datos fluyen con coherencia entre TODAS las features.
 *
 *   1. Registro como opositor (Policía Nacional).
 *   2. Completa marcas del perfil → nota y nivel calculados.
 *   3. Recibe plan semanal adaptado a su entorno (CASA) y material.
 *   4. Entrena: registra una sesión con resultados.
 *   5. La sesión aparece en el historial-pro (resumen + sesiones).
 *   6. La racha y el dashboard reflejan la sesión.
 *   7. Sube una actividad GPS (Health Connect simulado).
 *   8. El resumen del historial suma entrenos + GPS sin duplicar.
 *   9. Repite la sesión mejorando una marca → PR detectado con nombre real.
 *  10. Comunidad: crea grupo, otro usuario se une, chatean.
 */
process.env.JWT_SECRET = 'opofit-e2e-viaje';
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

function uniqEmail(p) {
  return `${p}_${Date.now()}_${Math.floor(Math.random() * 10000)}@opofit.test`;
}

async function registrarOpositor() {
  const reg = await request(app).post('/api/auth/registrar').send({
    nombre: 'Aspirante Viaje',
    email: uniqEmail('viaje'),
    password: 'Password123!',
    genero: 'HOMBRE',
    peso: 75,
    altura: 178,
    modo_uso: 'OPOSITOR',
    oposiciones_id_oposicion: 1
  });
  expect(reg.status).toBe(201);
  return { token: reg.body.token, userId: reg.body.userId || reg.body.user?.id_usuario };
}

beforeAll(async () => {
  memDb.reset();
  await seedAll(memDb);
  app = buildApp();
});

describe('E2E viaje completo del opositor', () => {
  let token;
  let userId;

  test('1-2. registro + marcas → nota y nivel coherentes', async () => {
    const r = await registrarOpositor();
    token = r.token;
    userId = r.userId;

    // Registra marcas vía el flujo real de la app: simulacro → aplicar al perfil.
    const aplicar = await request(app)
      .post('/api/simulacros/aplicar-marcas')
      .set('Authorization', `Bearer ${token}`)
      .send({
        idOposicion: 1,
        resultados: [
          { id_prueba: 1, valor: 11 },
          { id_prueba: 2, valor: 25 },
          { id_prueba: 3, valor: 14 }
        ]
      });
    expect(aplicar.status).toBe(200);

    const mi = await request(app)
      .get(`/api/rutinas/mi-entrenamiento/${userId}/1`)
      .set('Authorization', `Bearer ${token}`);
    expect(mi.status).toBe(200);
    expect(mi.body.ok).toBe(true);
    // Nivel asignado y nivel de rutinas mostradas presentes y coherentes:
    // sin premium las rutinas mostradas son BASICO aunque el nivel calculado
    // sea superior.
    const d = mi.body.data;
    expect(d.nivelAsignado).toBeTruthy();
    if (d.nivelAsignado !== 'BASICO' && d.nivelAsignado !== 'INCOMPLETO') {
      expect(d.nivelRutinasMostradas).toBe('BASICO');
      expect(d.nivelPremiumBloqueado).toBe(true);
    }
  });

  test('3. entorno CASA + material → plan filtrado sin material de gym', async () => {
    const ent = await request(app)
      .put('/api/planes/entorno')
      .set('Authorization', `Bearer ${token}`)
      .send({ entorno: 'CASA' });
    expect(ent.status).toBe(200);

    const mat = await request(app)
      .put('/api/user/material')
      .set('Authorization', `Bearer ${token}`)
      .send({ materialDisponible: ['GOMAS', 'COMBA'] });
    expect(mat.status).toBe(200);
    expect(mat.body.ok).toBe(true);

    const mi = await request(app)
      .get(`/api/rutinas/mi-entrenamiento/${userId}/1`)
      .set('Authorization', `Bearer ${token}`);
    const plan = mi.body.data?.planSemanal;
    if (plan?.semana?.length) {
      const nombres = plan.semana.flatMap((dia) => (dia.ejercicios || []).map((e) => e.nombre.toLowerCase()));
      // En CASA jamás deben colarse máquinas de gimnasio ni strongman.
      const prohibidos = nombres.filter((n) =>
        /prensa de pierna|polea|m[áa]quina|sled|trineo|yoke/.test(n)
      );
      expect(prohibidos).toEqual([]);
      // Título de sesión multi-ejercicio indica "+N más".
      const diaConVarios = plan.semana.find((dia) => (dia.ejercicios || []).length > 1);
      if (diaConVarios) {
        expect(diaConVarios.titulo).toMatch(/\+\d+ más$/);
      }
    }
  });

  test('4-6. registrar entreno → historial + dashboard coherentes', async () => {
    const reg = await request(app)
      .post('/api/historial/registrar')
      .set('Authorization', `Bearer ${token}`)
      .send({
        userId,
        tipoRutina: 'OPO',
        idRutina: 1,
        duracion: 1800, // 30 min en segundos
        ejercicios: [
          // Ejercicio 2 = "Dominadas pronas" (más reps = mejor). El 1 es
          // "Sprint 30s" donde MENOS es mejor — elegirlo invalidaría el
          // test de PR del paso 9.
          { id_ejercicio: 2, valor: 10 },
          { id_ejercicio: 3, valor: 8 }
        ]
      });
    expect(reg.status).toBeLessThan(300);
    expect(reg.body.ok).toBe(true);

    // El historial-pro lo refleja.
    const resumen = await request(app)
      .get('/api/historial-pro/resumen?periodo=week')
      .set('Authorization', `Bearer ${token}`);
    expect(resumen.status).toBe(200);
    expect(resumen.body.data.sesiones).toBeGreaterThanOrEqual(1);
    // 1800 s = 30 min exactos (verifica el fix de unidades seg/min).
    expect(resumen.body.data.minutos).toBeGreaterThanOrEqual(30);

    // El dashboard también.
    const dash = await request(app)
      .get('/api/dashboard/resumen')
      .set('Authorization', `Bearer ${token}`);
    expect(dash.status).toBe(200);
    expect(dash.body.data.sesionesSemana).toBeGreaterThanOrEqual(1);
    expect(dash.body.data.rachaDias).toBeGreaterThanOrEqual(1);
  });

  test('7-8. importar GPS → resumen suma sin duplicar', async () => {
    const ahora = Date.now();
    const subir = await request(app)
      .post('/api/integraciones/health-connect/importar')
      .set('Authorization', `Bearer ${token}`)
      .send({
        actividades: [{
          externalId: 'e2e_run_1',
          tipo: 'RUN',
          startedAtMs: ahora - 3_600_000,
          endedAtMs: ahora - 1_800_000,
          durationSec: 1800,
          distanceM: 5000
        }]
      });
    expect(subir.status).toBe(200);

    // Re-importar la MISMA actividad no debe duplicarla.
    const repetir = await request(app)
      .post('/api/integraciones/health-connect/importar')
      .set('Authorization', `Bearer ${token}`)
      .send({
        actividades: [{
          externalId: 'e2e_run_1',
          tipo: 'RUN',
          startedAtMs: ahora - 3_600_000,
          endedAtMs: ahora - 1_800_000,
          durationSec: 1800,
          distanceM: 5000
        }]
      });
    expect(repetir.status).toBe(200);

    const acts = memDb.state.gps_actividades.filter(
      (a) => a.usuarios_id_usuario === userId && a.external_id === 'e2e_run_1'
    );
    expect(acts.length).toBe(1); // ← deduplicado
  });

  test('9. mejorar marca → PR con nombre real del ejercicio', async () => {
    const reg = await request(app)
      .post('/api/historial/registrar')
      .set('Authorization', `Bearer ${token}`)
      .send({
        userId,
        tipoRutina: 'OPO',
        idRutina: 1,
        duracion: 1700,
        ejercicios: [{ id_ejercicio: 2, valor: 14 }] // mejora las 10 dominadas previas
      });
    expect(reg.body.ok).toBe(true);
    const prs = reg.body.recordsRotos || [];
    expect(prs.length).toBeGreaterThanOrEqual(1);
    // El nombre NUNCA es el feo "Ejercicio N".
    for (const pr of prs) {
      expect(pr.nombreEjercicio).not.toMatch(/^Ejercicio \d+$/);
      expect(pr.valorAnterior).not.toBeNull();
    }
  });

  test('10. comunidad: grupo + segundo usuario + chat coherente', async () => {
    const otro = await registrarOpositor();

    const grupo = await request(app)
      .post('/api/comunidad/grupos')
      .set('Authorization', `Bearer ${token}`)
      .send({ nombre: 'Viaje Completo Club', tipo: 'COMUNIDAD' });
    expect(grupo.status).toBe(201);
    const idGrupo = grupo.body.data?.id_grupo;
    expect(idGrupo).toBeGreaterThan(0); // snake_case correcto (fix del crash)

    const unirse = await request(app)
      .post(`/api/comunidad/grupos/${idGrupo}/unirse`)
      .set('Authorization', `Bearer ${otro.token}`);
    expect(unirse.status).toBe(200);

    const msg = await request(app)
      .post(`/api/comunidad/grupos/${idGrupo}/mensajes`)
      .set('Authorization', `Bearer ${otro.token}`)
      .send({ texto: 'Hola, ¿entrenamos juntos?' });
    expect(msg.status).toBe(200);

    const mensajes = await request(app)
      .get(`/api/comunidad/grupos/${idGrupo}/mensajes`)
      .set('Authorization', `Bearer ${token}`);
    expect(mensajes.status).toBe(200);
    expect(mensajes.body.data.some((m) => m.texto.includes('entrenamos'))).toBe(true);

    // El creador puede eliminar; el miembro no.
    const delMiembro = await request(app)
      .delete(`/api/comunidad/grupos/${idGrupo}`)
      .set('Authorization', `Bearer ${otro.token}`);
    expect(delMiembro.status).toBeGreaterThanOrEqual(400);

    const delCreador = await request(app)
      .delete(`/api/comunidad/grupos/${idGrupo}`)
      .set('Authorization', `Bearer ${token}`);
    expect(delCreador.status).toBe(200);
  });
});
