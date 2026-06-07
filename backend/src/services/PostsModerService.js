/**
 * Moderación de posts y comentarios.
 * - Reportar: cualquier usuario marca un post/comentario como problemático.
 * - Listar reportes pendientes: admin.
 * - Resolver: admin oculta o desestima.
 *
 * Si un post tiene >= UMBRAL_AUTO_OCULTAR reportes únicos → se oculta automáticamente
 * y queda en cola de revisión humana.
 */
const db = require('../config/db');
const InAppNotifService = require('./InAppNotifService');

const UMBRAL_AUTO_OCULTAR = 3;
const MOTIVOS_VALIDOS = ['SPAM', 'OFENSIVO', 'VIOLENCIA', 'FALSA_INFO', 'OTRO'];

class PostsModerService {
  static normalizarMotivo(m) {
    const v = String(m || '').toUpperCase();
    return MOTIVOS_VALIDOS.includes(v) ? v : 'OTRO';
  }

  static async reportarPost(idUsuarioReporta, idPost, motivo, detalle) {
    const [[post]] = await db.query(
      'SELECT id_post, id_usuario FROM actividad_posts WHERE id_post = ?',
      [idPost]
    );
    if (!post) throw new Error('POST_NO_ENCONTRADO');
    if (Number(post.id_usuario) === Number(idUsuarioReporta)) {
      throw new Error('NO_PUEDES_REPORTAR_TU_POST');
    }

    // Deduplicar: un usuario no puede reportar el mismo post más de una vez
    const [[ya]] = await db.query(
      `SELECT id_reporte FROM post_reportes
        WHERE id_post = ? AND id_usuario_reporta = ?
        LIMIT 1`,
      [idPost, idUsuarioReporta]
    );
    if (ya) return { ok: true, duplicado: true };

    await db.query(
      `INSERT INTO post_reportes (id_post, id_usuario_reporta, motivo, detalle)
       VALUES (?, ?, ?, ?)`,
      [idPost, idUsuarioReporta, PostsModerService.normalizarMotivo(motivo), detalle ? String(detalle).slice(0, 400) : null]
    );

    // ¿Llegamos al umbral?
    const [[c]] = await db.query(
      `SELECT COUNT(*) AS n FROM post_reportes
        WHERE id_post = ? AND estado IN ('PENDIENTE','REVISADO')`,
      [idPost]
    );
    if (Number(c.n) >= UMBRAL_AUTO_OCULTAR) {
      await db.query('UPDATE actividad_posts SET oculto = 1 WHERE id_post = ?', [idPost]);
      await InAppNotifService.crear({
        idUsuario: post.id_usuario,
        tipo: 'POST_REPORTE',
        titulo: 'Tu publicación se ha ocultado',
        cuerpo: 'Se ha ocultado tras varios reportes. Un moderador la revisará.',
        refId: idPost
      });
      return { ok: true, ocultadoAuto: true };
    }
    return { ok: true };
  }

  static async reportarComentario(idUsuarioReporta, idComentario, motivo, detalle) {
    const [[c]] = await db.query(
      'SELECT id_comentario, id_usuario FROM post_comentarios WHERE id_comentario = ?',
      [idComentario]
    );
    if (!c) throw new Error('COMENTARIO_NO_ENCONTRADO');
    if (Number(c.id_usuario) === Number(idUsuarioReporta)) {
      throw new Error('NO_PUEDES_REPORTAR_TU_COMENTARIO');
    }

    const [[ya]] = await db.query(
      `SELECT id_reporte FROM post_reportes
        WHERE id_comentario = ? AND id_usuario_reporta = ?
        LIMIT 1`,
      [idComentario, idUsuarioReporta]
    );
    if (ya) return { ok: true, duplicado: true };

    await db.query(
      `INSERT INTO post_reportes (id_comentario, id_usuario_reporta, motivo, detalle)
       VALUES (?, ?, ?, ?)`,
      [idComentario, idUsuarioReporta, PostsModerService.normalizarMotivo(motivo), detalle ? String(detalle).slice(0, 400) : null]
    );

    const [[cnt]] = await db.query(
      `SELECT COUNT(*) AS n FROM post_reportes
        WHERE id_comentario = ? AND estado IN ('PENDIENTE','REVISADO')`,
      [idComentario]
    );
    if (Number(cnt.n) >= UMBRAL_AUTO_OCULTAR) {
      await db.query('UPDATE post_comentarios SET oculto = 1 WHERE id_comentario = ?', [idComentario]);
      return { ok: true, ocultadoAuto: true };
    }
    return { ok: true };
  }

  static async listarPendientes({ limite = 50 } = {}) {
    const [rows] = await db.query(
      `SELECT r.id_reporte, r.id_post, r.id_comentario, r.id_usuario_reporta,
              r.motivo, r.detalle, r.estado, r.creado_en,
              u.nombre AS usuario_nombre
         FROM post_reportes r
         JOIN usuarios u ON u.id_usuario = r.id_usuario_reporta
        WHERE r.estado = 'PENDIENTE'
        ORDER BY r.creado_en DESC
        LIMIT ?`,
      [Number(limite)]
    );
    return rows;
  }

  static async resolver(idReporte, accion) {
    const acciones = { ocultar: 'OCULTADO', desestimar: 'DESESTIMADO', revisar: 'REVISADO' };
    const estado = acciones[String(accion || '').toLowerCase()];
    if (!estado) throw new Error('ACCION_INVALIDA');
    const [[rep]] = await db.query(
      'SELECT id_post, id_comentario FROM post_reportes WHERE id_reporte = ?',
      [idReporte]
    );
    if (!rep) throw new Error('REPORTE_NO_ENCONTRADO');
    if (estado === 'OCULTADO') {
      if (rep.id_post) await db.query('UPDATE actividad_posts SET oculto = 1 WHERE id_post = ?', [rep.id_post]);
      if (rep.id_comentario) await db.query('UPDATE post_comentarios SET oculto = 1 WHERE id_comentario = ?', [rep.id_comentario]);
    } else if (estado === 'DESESTIMADO') {
      // Re-publicamos si lo habíamos auto-ocultado
      if (rep.id_post) await db.query('UPDATE actividad_posts SET oculto = 0 WHERE id_post = ?', [rep.id_post]);
      if (rep.id_comentario) await db.query('UPDATE post_comentarios SET oculto = 0 WHERE id_comentario = ?', [rep.id_comentario]);
    }
    await db.query(
      `UPDATE post_reportes SET estado = ?, revisado_en = NOW() WHERE id_reporte = ?`,
      [estado, idReporte]
    );
    return { ok: true, estado };
  }
}

module.exports = PostsModerService;
