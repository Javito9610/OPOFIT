const db = require('../config/db');
const AmigosService = require('./AmigosService');
const { guardarFotoPost } = require('./PostMediaService');

class PostsService {
  static async idsAmigos(userId) {
    const amigos = await AmigosService.listarAmigos(userId);
    return new Set(amigos.map((a) => a.amigo_id));
  }

  static async esAmigo(userId, otroId) {
    const ids = await PostsService.idsAmigos(userId);
    return ids.has(Number(otroId));
  }

  static puedeVerPost(viewerId, post, amigosIds) {
    const autor = Number(post.id_usuario);
    const yo = Number(viewerId);
    if (autor === yo) return true;
    if (post.visibilidad === 'PUBLICO') return true;
    return amigosIds.has(autor);
  }

  static mapPostRow(r, extras = {}) {
    let stats = null;
    try {
      stats = r.stats_json ? JSON.parse(r.stats_json) : null;
    } catch (_) {
      stats = null;
    }
    return {
      idPost: r.id_post,
      usuarioId: r.id_usuario,
      usuarioNombre: r.usuario_nombre,
      avatarUrl: r.avatar_url,
      titulo: r.titulo,
      texto: r.texto,
      fotoUrl: r.foto_url,
      visibilidad: r.visibilidad,
      fuente: r.fuente,
      gpsUuid: r.gps_uuid,
      idHistorialSesion: r.id_historial_sesion,
      idSimulacro: r.id_simulacro,
      stats,
      creadoEn: r.creado_en,
      likes: Number(r.likes || extras.likes || 0),
      comentarios: Number(r.comentarios || extras.comentarios || 0),
      yoDiLike: Boolean(extras.yoDiLike),
      comentariosLista: extras.comentariosLista || null
    };
  }

  static async crear(userId, body) {
    const titulo = String(body.titulo || '').trim().substring(0, 120);
    if (!titulo) throw new Error('TITULO_OBLIGATORIO');

    const texto = String(body.texto || '').trim().substring(0, 2000) || null;
    const visibilidad = body.visibilidad === 'PUBLICO' ? 'PUBLICO' : 'AMIGOS';
    const fuente = ['GPS', 'ENTRENO', 'SIMULACRO', 'MANUAL'].includes(body.fuente)
      ? body.fuente
      : 'MANUAL';

    let fotoUrl = null;
    if (body.imagenBase64) {
      fotoUrl = guardarFotoPost(userId, body.imagenBase64);
    } else if (body.fotoUrl) {
      fotoUrl = String(body.fotoUrl).trim().substring(0, 512) || null;
    }

    const statsJson = body.stats ? JSON.stringify(body.stats) : null;
    const gpsUuid = body.gpsUuid ? String(body.gpsUuid).substring(0, 64) : null;
    const idHistorial = body.idHistorialSesion ? Number(body.idHistorialSesion) : null;
    const idSimulacro = body.idSimulacro ? Number(body.idSimulacro) : null;

    const [ins] = await db.query(
      `INSERT INTO actividad_posts
        (id_usuario, titulo, texto, foto_url, visibilidad, fuente,
         gps_uuid, id_historial_sesion, id_simulacro, stats_json)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [userId, titulo, texto, fotoUrl, visibilidad, fuente, gpsUuid, idHistorial, idSimulacro, statsJson]
    );

    const post = await PostsService.detalle(userId, ins.insertId);
    return post;
  }

  static async feed(userId, limite = 30) {
    const amigosIds = await PostsService.idsAmigos(userId);
    const idsAmigosArr = [...amigosIds];
    const placeholders = idsAmigosArr.length ? idsAmigosArr.map(() => '?').join(',') : '0';

    const [rows] = await db.query(
      `SELECT p.*, u.nombre AS usuario_nombre, u.avatar_url,
              (SELECT COUNT(*) FROM post_likes pl WHERE pl.id_post = p.id_post) AS likes,
              (SELECT COUNT(*) FROM post_comentarios pc WHERE pc.id_post = p.id_post) AS comentarios,
              EXISTS(SELECT 1 FROM post_likes pl2 WHERE pl2.id_post = p.id_post AND pl2.id_usuario = ?) AS yo_like
       FROM actividad_posts p
       JOIN usuarios u ON p.id_usuario = u.id_usuario
       WHERE p.id_usuario = ?
          OR p.visibilidad = 'PUBLICO'
          OR (p.visibilidad = 'AMIGOS' AND p.id_usuario IN (${placeholders}))
       ORDER BY p.creado_en DESC
       LIMIT ?`,
      [userId, userId, ...idsAmigosArr, Number(limite)]
    );

    return (rows || []).map((r) =>
      PostsService.mapPostRow(r, { likes: r.likes, comentarios: r.comentarios, yoDiLike: !!r.yo_like })
    );
  }

  static async porUsuario(viewerId, targetUserId, limite = 30) {
    const amigosIds = await PostsService.idsAmigos(viewerId);
    const yo = Number(viewerId);
    const target = Number(targetUserId);

    const [rows] = await db.query(
      `SELECT p.*, u.nombre AS usuario_nombre, u.avatar_url,
              (SELECT COUNT(*) FROM post_likes pl WHERE pl.id_post = p.id_post) AS likes,
              (SELECT COUNT(*) FROM post_comentarios pc WHERE pc.id_post = p.id_post) AS comentarios,
              EXISTS(SELECT 1 FROM post_likes pl2 WHERE pl2.id_post = p.id_post AND pl2.id_usuario = ?) AS yo_like
       FROM actividad_posts p
       JOIN usuarios u ON p.id_usuario = u.id_usuario
       WHERE p.id_usuario = ?
       ORDER BY p.creado_en DESC
       LIMIT ?`,
      [viewerId, target, Number(limite)]
    );

    return (rows || [])
      .filter((r) => PostsService.puedeVerPost(viewerId, r, amigosIds))
      .map((r) =>
        PostsService.mapPostRow(r, { likes: r.likes, comentarios: r.comentarios, yoDiLike: !!r.yo_like })
      );
  }

  static async detalle(viewerId, idPost) {
    const [[r]] = await db.query(
      `SELECT p.*, u.nombre AS usuario_nombre, u.avatar_url,
              (SELECT COUNT(*) FROM post_likes pl WHERE pl.id_post = p.id_post) AS likes,
              (SELECT COUNT(*) FROM post_comentarios pc WHERE pc.id_post = p.id_post) AS comentarios,
              EXISTS(SELECT 1 FROM post_likes pl2 WHERE pl2.id_post = p.id_post AND pl2.id_usuario = ?) AS yo_like
       FROM actividad_posts p
       JOIN usuarios u ON p.id_usuario = u.id_usuario
       WHERE p.id_post = ?`,
      [viewerId, idPost]
    );
    if (!r) throw new Error('POST_NO_ENCONTRADO');

    const amigosIds = await PostsService.idsAmigos(viewerId);
    if (!PostsService.puedeVerPost(viewerId, r, amigosIds)) {
      throw new Error('SIN_PERMISO');
    }

    const [comentarios] = await db.query(
      `SELECT c.id_comentario, c.id_usuario, c.texto, c.creado_en, u.nombre AS usuario_nombre, u.avatar_url
       FROM post_comentarios c
       JOIN usuarios u ON c.id_usuario = u.id_usuario
       WHERE c.id_post = ?
       ORDER BY c.creado_en ASC
       LIMIT 100`,
      [idPost]
    );

    return PostsService.mapPostRow(r, {
      likes: r.likes,
      comentarios: r.comentarios,
      yoDiLike: !!r.yo_like,
      comentariosLista: (comentarios || []).map((c) => ({
        idComentario: c.id_comentario,
        usuarioId: c.id_usuario,
        usuarioNombre: c.usuario_nombre,
        avatarUrl: c.avatar_url,
        texto: c.texto,
        creadoEn: c.creado_en
      }))
    });
  }

  static async toggleLike(userId, idPost) {
    await PostsService.detalle(userId, idPost);
    const [[exists]] = await db.query(
      'SELECT id_like FROM post_likes WHERE id_post = ? AND id_usuario = ?',
      [idPost, userId]
    );
    if (exists) {
      await db.query('DELETE FROM post_likes WHERE id_post = ? AND id_usuario = ?', [idPost, userId]);
      return { liked: false };
    }
    await db.query('INSERT INTO post_likes (id_post, id_usuario) VALUES (?, ?)', [idPost, userId]);
    return { liked: true };
  }

  static async comentar(userId, idPost, texto) {
    await PostsService.detalle(userId, idPost);
    const limpio = String(texto || '').trim().substring(0, 500);
    if (!limpio) throw new Error('COMENTARIO_VACIO');
    const [ins] = await db.query(
      'INSERT INTO post_comentarios (id_post, id_usuario, texto) VALUES (?, ?, ?)',
      [idPost, userId, limpio]
    );
    return {
      idComentario: ins.insertId,
      texto: limpio
    };
  }
}

module.exports = PostsService;
