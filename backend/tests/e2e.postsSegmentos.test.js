process.env.JWT_SECRET = 'opofit-e2e-posts';

const request = require('supertest');
const jwt = require('jsonwebtoken');
const { buildApp } = require('./helpers/buildApp');
const { pool, state, reset, makeUser } = require('./helpers/inMemoryDb');

jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);

const app = buildApp();
const secret = process.env.JWT_SECRET;

function token(userId) {
  return jwt.sign({ id: userId }, secret);
}

describe('Posts y segmentos E2E', () => {
  let u1;
  let u2;

  beforeEach(() => {
    reset();
    u1 = makeUser({ nombre: 'Ana' });
    u2 = makeUser({ nombre: 'Luis' });
    state.usuarios.push(u1, u2);
    state.amistades.push({
      id_amistad: 1,
      id_usuario_a: u1.id_usuario,
      id_usuario_b: u2.id_usuario,
      estado: 'ACEPTADA',
      solicitante_id: u1.id_usuario
    });
    state.segmentos.push(
      { id_segmento: 1, slug: '1km', nombre: 'Mejor 1 km', tipo: 'VIRTUAL', distancia_m: 1000, mejor_si_menor: 1, categoria: 'CARRERA', activo: 1 },
      { id_segmento: 2, slug: '50m', nombre: 'Mejor 50 m', tipo: 'VIRTUAL', distancia_m: 50, mejor_si_menor: 1, categoria: 'VELOCIDAD', activo: 1 }
    );
  });

  test('crear post GPS y aparece en feed de amigo', async () => {
    const crear = await request(app)
      .post('/api/posts')
      .set('Authorization', `Bearer ${token(u1.id_usuario)}`)
      .send({
        titulo: 'Rodaje suave',
        texto: 'Buen día para correr',
        visibilidad: 'AMIGOS',
        fuente: 'GPS',
        gpsUuid: 'abc-123',
        stats: { distanciaM: 5000, duracionSec: 1500, tipo: 'RUN' }
      });
    expect(crear.status).toBe(201);
    expect(crear.body.ok).toBe(true);

    const feed = await request(app)
      .get('/api/posts/feed')
      .set('Authorization', `Bearer ${token(u2.id_usuario)}`);
    expect(feed.status).toBe(200);
    expect(feed.body.data.length).toBe(1);
    expect(feed.body.data[0].titulo).toBe('Rodaje suave');
  });

  test('like y comentario en post', async () => {
    state.actividad_posts.push({
      id_post: 1,
      id_usuario: u1.id_usuario,
      titulo: 'Test',
      texto: null,
      foto_url: null,
      visibilidad: 'PUBLICO',
      fuente: 'GPS',
      gps_uuid: null,
      stats_json: null,
      creado_en: new Date().toISOString()
    });

    const like = await request(app)
      .post('/api/posts/1/like')
      .set('Authorization', `Bearer ${token(u2.id_usuario)}`);
    expect(like.status).toBe(200);
    expect(like.body.data.liked).toBe(true);

    const com = await request(app)
      .post('/api/posts/1/comentarios')
      .set('Authorization', `Bearer ${token(u2.id_usuario)}`)
      .send({ texto: 'Buen ritmo!' });
    expect(com.status).toBe(201);
  });

  test('registrar esfuerzo en segmento virtual', async () => {
    const r = await request(app)
      .post('/api/segmentos/1/esfuerzo')
      .set('Authorization', `Bearer ${token(u1.id_usuario)}`)
      .send({ duracionMs: 245000 });
    expect(r.status).toBe(201);
    expect(r.body.data.esMejor).toBe(true);
  });
});
