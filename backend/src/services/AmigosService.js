const db = require('../config/db');

class AmigosService {
  static ordenPar(idA, idB) {
    return idA < idB ? [idA, idB] : [idB, idA];
  }

  static async listarAmigos(userId) {
    const [rows] = await db.query(
      `SELECT a.id_amistad, a.estado,
              CASE WHEN a.id_usuario_a = ? THEN u2.id_usuario ELSE u1.id_usuario END AS amigo_id,
              CASE WHEN a.id_usuario_a = ? THEN u2.nombre ELSE u1.nombre END AS amigo_nombre,
              CASE WHEN a.id_usuario_a = ? THEN u2.oposiciones_id_oposicion ELSE u1.oposiciones_id_oposicion END AS oposicion_id
       FROM amistades a
       JOIN usuarios u1 ON a.id_usuario_a = u1.id_usuario
       JOIN usuarios u2 ON a.id_usuario_b = u2.id_usuario
       WHERE (a.id_usuario_a = ? OR a.id_usuario_b = ?) AND a.estado = 'ACEPTADA'`,
      [userId, userId, userId, userId, userId]
    );
    return rows || [];
  }

  static async solicitudesPendientes(userId) {
    const [rows] = await db.query(
      `SELECT a.id_amistad, a.solicitante_id, u.nombre AS solicitante_nombre
       FROM amistades a
       JOIN usuarios u ON a.solicitante_id = u.id_usuario
       WHERE (a.id_usuario_a = ? OR a.id_usuario_b = ?)
         AND a.estado = 'PENDIENTE' AND a.solicitante_id != ?`,
      [userId, userId, userId]
    );
    return rows || [];
  }

  static async enviarSolicitud(solicitanteId, receptorId) {
    if (solicitanteId === receptorId) throw new Error('NO_AUTO_AMISTAD');
    const [receptor] = await db.query(
      'SELECT id_usuario, oposiciones_id_oposicion FROM usuarios WHERE id_usuario = ?',
      [receptorId]
    );
    if (!receptor?.length) throw new Error('USUARIO_NO_ENCONTRADO');
    const [yo] = await db.query(
      'SELECT oposiciones_id_oposicion FROM usuarios WHERE id_usuario = ?',
      [solicitanteId]
    );
    if (Number(yo[0].oposiciones_id_oposicion) !== Number(receptor[0].oposiciones_id_oposicion)) {
      throw new Error('DISTINTA_OPOSICION');
    }
    const [a, b] = AmigosService.ordenPar(solicitanteId, receptorId);
    await db.query(
      `INSERT INTO amistades (id_usuario_a, id_usuario_b, estado, solicitante_id)
       VALUES (?, ?, 'PENDIENTE', ?)
       ON DUPLICATE KEY UPDATE estado = IF(estado = 'RECHAZADA', 'PENDIENTE', estado), solicitante_id = VALUES(solicitante_id)`,
      [a, b, solicitanteId]
    );
    return { ok: true };
  }

  static async responderSolicitud(userId, idAmistad, aceptar) {
    const [row] = await db.query(
      'SELECT * FROM amistades WHERE id_amistad = ? AND (id_usuario_a = ? OR id_usuario_b = ?)',
      [idAmistad, userId, userId]
    );
    if (!row?.length) throw new Error('SOLICITUD_NO_ENCONTRADA');
    if (row[0].solicitante_id === userId) throw new Error('NO_PUEDES_ACEPTAR_TU_SOLICITUD');
    await db.query('UPDATE amistades SET estado = ? WHERE id_amistad = ?', [
      aceptar ? 'ACEPTADA' : 'RECHAZADA',
      idAmistad
    ]);
    return { ok: true };
  }

  static async buscarPorNombre(userId, nombre, idOposicion, limite = 20) {
    const [rows] = await db.query(
      `SELECT u.id_usuario, u.nombre, u.perfil_publico
       FROM usuarios u
       WHERE u.oposiciones_id_oposicion = ?
         AND u.id_usuario != ?
         AND u.nombre LIKE ?
       LIMIT ?`,
      [idOposicion, userId, `%${nombre}%`, limite]
    );
    return rows || [];
  }

  static async enviarMensaje(remitenteId, destinatarioId, texto) {
    const amigos = await AmigosService.listarAmigos(remitenteId);
    if (!amigos.some((a) => a.amigo_id === destinatarioId)) {
      throw new Error('NO_SOIS_AMIGOS');
    }
    const [ins] = await db.query(
      'INSERT INTO mensajes_chat (id_remitente, id_destinatario, texto) VALUES (?, ?, ?)',
      [remitenteId, destinatarioId, texto.substring(0, 1000)]
    );
    return { idMensaje: ins.insertId };
  }

  static async obtenerChat(userId, otroId, limite = 50) {
    const [rows] = await db.query(
      `SELECT id_mensaje, id_remitente, id_destinatario, texto, enviado_en
       FROM mensajes_chat
       WHERE (id_remitente = ? AND id_destinatario = ?)
          OR (id_remitente = ? AND id_destinatario = ?)
       ORDER BY enviado_en ASC
       LIMIT ?`,
      [userId, otroId, otroId, userId, limite]
    );
    return rows || [];
  }
}

module.exports = AmigosService;
