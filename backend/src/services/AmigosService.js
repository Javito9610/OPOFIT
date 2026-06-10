const db = require('../config/db');
const InAppNotifService = require('./InAppNotifService');
// Lazy-require para evitar ciclos y permitir tests sin Firebase Admin.
const NotificationService = () => require('./NotificationService');

class AmigosService {
  static ordenPar(idA, idB) {
    return idA < idB ? [idA, idB] : [idB, idA];
  }

  static puedenSerAmigos(usuarioA, usuarioB) {
    const modoA = usuarioA?.modo_uso || 'OPOSITOR';
    const modoB = usuarioB?.modo_uso || 'OPOSITOR';
    if (modoA === 'FITNESS' && modoB === 'FITNESS') return true;
    if (modoA === 'FITNESS' || modoB === 'FITNESS') return true;
    return Number(usuarioA?.oposiciones_id_oposicion) === Number(usuarioB?.oposiciones_id_oposicion);
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
      'SELECT id_usuario, oposiciones_id_oposicion, modo_uso FROM usuarios WHERE id_usuario = ?',
      [receptorId]
    );
    if (!receptor?.length) throw new Error('USUARIO_NO_ENCONTRADO');
    const [yo] = await db.query(
      'SELECT oposiciones_id_oposicion, modo_uso FROM usuarios WHERE id_usuario = ?',
      [solicitanteId]
    );
    if (!AmigosService.puedenSerAmigos(yo[0], receptor[0])) {
      throw new Error('DISTINTA_OPOSICION');
    }
    const [a, b] = AmigosService.ordenPar(solicitanteId, receptorId);
    await db.query(
      `INSERT INTO amistades (id_usuario_a, id_usuario_b, estado, solicitante_id)
       VALUES (?, ?, 'PENDIENTE', ?)
       ON DUPLICATE KEY UPDATE estado = IF(estado = 'RECHAZADA', 'PENDIENTE', estado), solicitante_id = VALUES(solicitante_id)`,
      [a, b, solicitanteId]
    );
    await InAppNotifService.crear({
      idUsuario: receptorId,
      tipo: 'SOLICITUD_AMISTAD',
      titulo: 'Tienes una nueva solicitud de amistad',
      cuerpo: null,
      actorId: solicitanteId
    });
    // Push FCM (best-effort).
    try {
      const [solicitante] = await db.query(
        'SELECT nombre FROM usuarios WHERE id_usuario = ?', [solicitanteId]
      );
      if (solicitante?.[0]) {
        await NotificationService().notificarSolicitudAmistad({
          idDestino: receptorId,
          nombreSolicitante: solicitante[0].nombre
        });
      }
    } catch (_) { /* silencioso */ }
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
    if (aceptar) {
      await InAppNotifService.crear({
        idUsuario: row[0].solicitante_id,
        tipo: 'AMISTAD_ACEPTADA',
        titulo: 'Han aceptado tu solicitud de amistad',
        cuerpo: null,
        actorId: userId
      });
      try {
        const [aceptante] = await db.query(
          'SELECT nombre FROM usuarios WHERE id_usuario = ?', [userId]
        );
        if (aceptante?.[0]) {
          await NotificationService().notificarAmistadAceptada({
            idSolicitante: row[0].solicitante_id,
            nombreAceptante: aceptante[0].nombre
          });
        }
      } catch (_) { /* silencioso */ }
    }
    return { ok: true };
  }

  /**
   * Busca usuarios por nombre Y/O email. Si el texto parece email (contiene
   * @ o se ve como uno) se prioriza la coincidencia exacta. Si no, LIKE en
   * nombre + LIKE en email. El usuario quería buscar amigos por las dos vías.
   *
   * Además, ya NO restringimos por modo_uso: cualquier usuario es buscable
   * sea opositor o fitness — el usuario quería poder agregar amigos
   * independientemente del modo.
   */
  static async buscarPorNombre(userId, consulta, idOposicion, limite = 20) {
    const term = `%${consulta}%`;
    const esEmail = consulta.includes('@');
    // Si la consulta tiene @ lo tratamos como búsqueda de email — match exacto
    // primero, parcial después. Ordenamos por exactitud.
    if (esEmail) {
      const [rows] = await db.query(
        `SELECT u.id_usuario, u.nombre, u.email, u.perfil_publico, u.modo_uso,
                u.avatar_url, u.oposiciones_id_oposicion AS id_oposicion
         FROM usuarios u
         WHERE u.id_usuario != ?
           AND (u.email = ? OR u.email LIKE ?)
         ORDER BY (u.email = ?) DESC, u.nombre ASC
         LIMIT ?`,
        [userId, consulta, term, consulta, limite]
      );
      return rows || [];
    }
    // Si NO es email: buscamos por nombre o email parcial (ambos LIKE).
    const params = idOposicion
      ? [userId, term, term, idOposicion, limite]
      : [userId, term, term, limite];
    const sql = idOposicion
      ? `SELECT u.id_usuario, u.nombre, u.email, u.perfil_publico, u.modo_uso,
                u.avatar_url, u.oposiciones_id_oposicion AS id_oposicion
         FROM usuarios u
         WHERE u.id_usuario != ?
           AND (u.nombre LIKE ? OR u.email LIKE ?)
           AND u.oposiciones_id_oposicion = ?
         ORDER BY u.nombre ASC
         LIMIT ?`
      : `SELECT u.id_usuario, u.nombre, u.email, u.perfil_publico, u.modo_uso,
                u.avatar_url, u.oposiciones_id_oposicion AS id_oposicion
         FROM usuarios u
         WHERE u.id_usuario != ?
           AND (u.nombre LIKE ? OR u.email LIKE ?)
         ORDER BY u.nombre ASC
         LIMIT ?`;
    const [rows] = await db.query(sql, params);
    return rows || [];
  }

  static async enviarMensaje(remitenteId, destinatarioId, texto) {
    const limpio = String(texto || '').trim();
    if (!limpio) throw new Error('MENSAJE_VACIO');
    const amigos = await AmigosService.listarAmigos(remitenteId);
    if (!amigos.some((a) => a.amigo_id === destinatarioId)) {
      throw new Error('NO_SOIS_AMIGOS');
    }
    const [ins] = await db.query(
      'INSERT INTO mensajes_chat (id_remitente, id_destinatario, texto) VALUES (?, ?, ?)',
      [remitenteId, destinatarioId, limpio.substring(0, 1000)]
    );
    // Push al destinatario (best-effort).
    try {
      const [remitente] = await db.query(
        'SELECT nombre FROM usuarios WHERE id_usuario = ?', [remitenteId]
      );
      if (remitente?.[0]) {
        await NotificationService().notificarMensajeDirecto({
          idDestino: destinatarioId,
          nombreRemitente: remitente[0].nombre,
          texto: limpio
        });
      }
    } catch (_) { /* silencioso */ }
    return { idMensaje: ins.insertId };
  }

  static async feedActividad(userId, limite = 25) {
    const amigos = await AmigosService.listarAmigos(userId);
    const ids = amigos.map((a) => a.amigo_id);
    if (!ids.length) return [];
    const placeholders = ids.map(() => '?').join(',');
    const [entrenos] = await db.query(
      `SELECT 'ENTRENO' AS tipo, h.fecha_entreno AS fecha, u.nombre AS usuario_nombre,
              u.id_usuario AS usuario_id, h.duracion_oficial AS valor1, h.tipo_rutina AS valor2
       FROM historial_sesiones h
       JOIN usuarios u ON h.usuarios_id_usuario = u.id_usuario
       WHERE h.usuarios_id_usuario IN (${placeholders})
         AND h.fecha_entreno >= DATE_SUB(NOW(), INTERVAL 30 DAY)`,
      ids
    );
    const [sims] = await db.query(
      `SELECT 'SIMULACRO' AS tipo, s.fecha AS fecha, u.nombre AS usuario_nombre,
              u.id_usuario AS usuario_id, s.nota_media AS valor1, s.oposiciones_id_oposicion AS valor2
       FROM simulacros s
       JOIN usuarios u ON s.usuarios_id_usuario = u.id_usuario
       WHERE s.usuarios_id_usuario IN (${placeholders})
         AND s.fecha >= DATE_SUB(NOW(), INTERVAL 30 DAY)`,
      ids
    );
    const merged = [...(entrenos || []), ...(sims || [])]
      .sort((a, b) => new Date(b.fecha) - new Date(a.fecha))
      .slice(0, limite);
    return merged.map((r) => ({
      tipo: r.tipo,
      fecha: r.fecha,
      usuarioNombre: r.usuario_nombre,
      usuarioId: r.usuario_id,
      detalle: r.tipo === 'ENTRENO'
        ? `${r.valor2 || 'OPO'} · ${r.valor1} min`
        : `Simulacro · nota ${r.valor1}/10`
    }));
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
