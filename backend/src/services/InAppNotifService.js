/**
 * Centro de notificaciones in-app.
 * Persiste en `notificaciones_app` y se consume desde la app (/api/notif-app).
 *
 * Usado por:
 *  - PostsService al crear like / comentario.
 *  - AmigosService al recibir solicitud / aceptación.
 *  - PostsModerService al ocultar un post reportado.
 *
 * Se mantiene aparte de FCM (push): los push son intrusivos, esto es feed.
 */
const db = require('../config/db');

const TIPOS_VALIDOS = [
  'LIKE',
  'COMENTARIO',
  'SOLICITUD_AMISTAD',
  'AMISTAD_ACEPTADA',
  'POST_REPORTE',
  'SISTEMA'
];

class InAppNotifService {
  static async crear({ idUsuario, tipo, titulo, cuerpo = null, refId = null, actorId = null }) {
    if (!idUsuario) return null;
    if (!TIPOS_VALIDOS.includes(tipo)) tipo = 'SISTEMA';
    if (actorId && Number(actorId) === Number(idUsuario)) return null; // no notificarse a uno mismo
    try {
      const [ins] = await db.query(
        `INSERT INTO notificaciones_app (id_usuario, tipo, titulo, cuerpo, ref_id, actor_id)
         VALUES (?, ?, ?, ?, ?, ?)`,
        [idUsuario, tipo, String(titulo).slice(0, 160), cuerpo ? String(cuerpo).slice(0, 400) : null, refId, actorId]
      );
      return ins.insertId;
    } catch (e) {
      // No queremos romper el flujo origen (like, comentario...) si la notif falla.
      console.warn('[InAppNotif crear]', e.message);
      return null;
    }
  }

  static async listar(idUsuario, { limite = 50, soloNoLeidas = false } = {}) {
    const where = soloNoLeidas ? 'AND n.leida = 0' : '';
    const [rows] = await db.query(
      `SELECT n.id_notificacion, n.tipo, n.titulo, n.cuerpo, n.ref_id, n.actor_id,
              n.leida, n.creada_en,
              u.nombre AS actor_nombre, u.avatar_url AS actor_avatar
         FROM notificaciones_app n
         LEFT JOIN usuarios u ON u.id_usuario = n.actor_id
        WHERE n.id_usuario = ? ${where}
        ORDER BY n.creada_en DESC
        LIMIT ?`,
      [idUsuario, Number(limite)]
    );
    return rows.map((r) => ({
      idNotificacion: r.id_notificacion,
      tipo: r.tipo,
      titulo: r.titulo,
      cuerpo: r.cuerpo,
      refId: r.ref_id,
      actorId: r.actor_id,
      actorNombre: r.actor_nombre,
      actorAvatar: r.actor_avatar,
      leida: !!r.leida,
      creadaEn: r.creada_en
    }));
  }

  static async contarNoLeidas(idUsuario) {
    const [[r]] = await db.query(
      'SELECT COUNT(*) AS c FROM notificaciones_app WHERE id_usuario = ? AND leida = 0',
      [idUsuario]
    );
    return Number(r.c || 0);
  }

  static async marcarLeida(idUsuario, idNotificacion) {
    await db.query(
      'UPDATE notificaciones_app SET leida = 1 WHERE id_notificacion = ? AND id_usuario = ?',
      [idNotificacion, idUsuario]
    );
    return true;
  }

  static async marcarTodasLeidas(idUsuario) {
    await db.query(
      'UPDATE notificaciones_app SET leida = 1 WHERE id_usuario = ? AND leida = 0',
      [idUsuario]
    );
    return true;
  }

  static async eliminar(idUsuario, idNotificacion) {
    await db.query(
      'DELETE FROM notificaciones_app WHERE id_notificacion = ? AND id_usuario = ?',
      [idNotificacion, idUsuario]
    );
    return true;
  }
}

module.exports = InAppNotifService;
