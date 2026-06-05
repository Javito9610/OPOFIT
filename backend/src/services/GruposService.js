const db = require('../config/db');

class GruposService {
  static async esMiembro(userId, idGrupo) {
    const [rows] = await db.query(
      'SELECT rol FROM grupo_miembros WHERE id_grupo = ? AND id_usuario = ?',
      [idGrupo, userId]
    );
    return rows?.[0] || null;
  }

  static async listarGrupos(userId, idOposicion) {
    const opoParam = idOposicion == null ? null : Number(idOposicion);
    const [rows] = await db.query(
      `SELECT g.id_grupo, g.nombre, g.descripcion, g.id_oposicion, g.creador_id, g.creado_en,
              (SELECT COUNT(*) FROM grupo_miembros gm WHERE gm.id_grupo = g.id_grupo) AS num_miembros,
              gm_self.rol AS mi_rol
       FROM grupos_comunidad g
       LEFT JOIN grupo_miembros gm_self ON gm_self.id_grupo = g.id_grupo AND gm_self.id_usuario = ?
       WHERE ${opoParam == null ? 'g.id_oposicion IS NULL' : 'g.id_oposicion = ?'}
       ORDER BY g.creado_en DESC`,
      opoParam == null ? [userId] : [userId, opoParam]
    );
    return (rows || []).map((r) => ({
      idGrupo: r.id_grupo,
      nombre: r.nombre,
      descripcion: r.descripcion,
      idOposicion: r.id_oposicion,
      creadorId: r.creador_id,
      creadoEn: r.creado_en,
      numMiembros: Number(r.num_miembros || 0),
      soyMiembro: !!r.mi_rol,
      miRol: r.mi_rol || null
    }));
  }

  static async crearGrupo(userId, { nombre, descripcion, idOposicion }) {
    const limpio = String(nombre || '').trim();
    if (!limpio) throw new Error('NOMBRE_OBLIGATORIO');
    const opo = idOposicion == null ? null : Number(idOposicion);
    if (opo != null) {
      const [op] = await db.query('SELECT id_oposicion FROM oposiciones WHERE id_oposicion = ?', [opo]);
      if (!op?.length) throw new Error('OPOSICION_NO_VALIDA');
    }
    const connection = await db.getConnection();
    try {
      await connection.beginTransaction();
      const [ins] = await connection.query(
        `INSERT INTO grupos_comunidad (nombre, descripcion, id_oposicion, creador_id)
         VALUES (?, ?, ?, ?)`,
        [limpio.substring(0, 120), String(descripcion || '').trim().substring(0, 500) || null, opo, userId]
      );
      const idGrupo = ins.insertId;
      await connection.query(
        `INSERT INTO grupo_miembros (id_grupo, id_usuario, rol) VALUES (?, ?, 'ADMIN')`,
        [idGrupo, userId]
      );
      await connection.commit();
      return { idGrupo };
    } catch (e) {
      await connection.rollback();
      throw e;
    } finally {
      connection.release();
    }
  }

  static async unirse(userId, idGrupo) {
    const [grupo] = await db.query('SELECT id_grupo FROM grupos_comunidad WHERE id_grupo = ?', [idGrupo]);
    if (!grupo?.length) throw new Error('GRUPO_NO_ENCONTRADO');
    const miembro = await GruposService.esMiembro(userId, idGrupo);
    if (miembro) throw new Error('YA_ERES_MIEMBRO');
    await db.query(
      `INSERT INTO grupo_miembros (id_grupo, id_usuario, rol) VALUES (?, ?, 'MIEMBRO')`,
      [idGrupo, userId]
    );
    return { ok: true };
  }

  static async salir(userId, idGrupo) {
    const miembro = await GruposService.esMiembro(userId, idGrupo);
    if (!miembro) throw new Error('NO_ERES_MIEMBRO');
    await db.query('DELETE FROM grupo_miembros WHERE id_grupo = ? AND id_usuario = ?', [idGrupo, userId]);
    return { ok: true };
  }

  static async mensajes(userId, idGrupo, limite = 50) {
    const miembro = await GruposService.esMiembro(userId, idGrupo);
    if (!miembro) throw new Error('NO_ERES_MIEMBRO');
    const [rows] = await db.query(
      `SELECT m.id_mensaje, m.id_usuario, u.nombre AS usuario_nombre, m.texto, m.enviado_en
       FROM grupo_mensajes m
       JOIN usuarios u ON m.id_usuario = u.id_usuario
       WHERE m.id_grupo = ?
       ORDER BY m.enviado_en ASC
       LIMIT ?`,
      [idGrupo, limite]
    );
    return (rows || []).map((r) => ({
      idMensaje: r.id_mensaje,
      idUsuario: r.id_usuario,
      usuarioNombre: r.usuario_nombre,
      texto: r.texto,
      enviadoEn: r.enviado_en
    }));
  }

  static async enviarMensaje(userId, idGrupo, texto) {
    const miembro = await GruposService.esMiembro(userId, idGrupo);
    if (!miembro) throw new Error('NO_ERES_MIEMBRO');
    const limpio = String(texto || '').trim();
    if (!limpio) throw new Error('MENSAJE_VACIO');
    const [ins] = await db.query(
      'INSERT INTO grupo_mensajes (id_grupo, id_usuario, texto) VALUES (?, ?, ?)',
      [idGrupo, userId, limpio.substring(0, 1000)]
    );
    return { idMensaje: ins.insertId };
  }

  static async crearQuedada(userId, idGrupo, datos = {}) {
    const miembro = await GruposService.esMiembro(userId, idGrupo);
    if (!miembro) throw new Error('NO_ERES_MIEMBRO');
    const titulo = String(datos.titulo || '').trim();
    if (!titulo) throw new Error('TITULO_OBLIGATORIO');
    const fechaHora = datos.fechaHora || datos.fecha_hora;
    if (!fechaHora) throw new Error('FECHA_OBLIGATORIA');
    const [ins] = await db.query(
      `INSERT INTO quedadas (id_grupo, creador_id, titulo, descripcion, fecha_hora, ubicacion_lat, ubicacion_lng)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [
        idGrupo,
        userId,
        titulo.substring(0, 120),
        String(datos.descripcion || '').trim().substring(0, 500) || null,
        fechaHora,
        datos.lat != null ? Number(datos.lat) : null,
        datos.lng != null ? Number(datos.lng) : null
      ]
    );
    return { idQuedada: ins.insertId };
  }

  static async listarQuedadas(userId, idGrupo) {
    const miembro = await GruposService.esMiembro(userId, idGrupo);
    if (!miembro) throw new Error('NO_ERES_MIEMBRO');
    const [rows] = await db.query(
      `SELECT q.id_quedada, q.titulo, q.descripcion, q.fecha_hora, q.ubicacion_lat, q.ubicacion_lng,
              q.creador_id, u.nombre AS creador_nombre, q.creado_en
       FROM quedadas q
       JOIN usuarios u ON q.creador_id = u.id_usuario
       WHERE q.id_grupo = ?
       ORDER BY q.fecha_hora ASC`,
      [idGrupo]
    );
    return (rows || []).map((r) => ({
      idQuedada: r.id_quedada,
      titulo: r.titulo,
      descripcion: r.descripcion,
      fechaHora: r.fecha_hora,
      ubicacionLat: r.ubicacion_lat,
      ubicacionLng: r.ubicacion_lng,
      creadorId: r.creador_id,
      creadorNombre: r.creador_nombre,
      creadoEn: r.creado_en
    }));
  }
}

module.exports = GruposService;
