const db = require('../config/db');
// Lazy-require para evitar ciclos: NotificationService → PlanesService → ...
// y para que los tests puedan correr sin Firebase inicializado.
const NotificationService = () => require('./NotificationService');

/**
 * Helper: serializa una fila a snake_case. Convenio consistente con el resto
 * del backend OpoFit y, sobre todo, con los modelos del frontend (Kotlin con
 * Gson default usa snake_case). Antes camelCase causaba `id_grupo = 0` en el
 * cliente y al pulsar "Unirse" se mandaba id=0 → crash.
 */
function serializarGrupo(r, miRol) {
  return {
    id_grupo: r.id_grupo,
    nombre: r.nombre,
    descripcion: r.descripcion,
    id_oposicion: r.id_oposicion,
    tipo: r.tipo || 'COMUNIDAD',
    creador_id: r.creador_id,
    creado_en: r.creado_en,
    miembros: Number(r.num_miembros ?? 0),
    soy_miembro: !!miRol,
    soy_creador: r.creador_id != null && miRol === 'ADMIN',
    mi_rol: miRol || null
  };
}

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
    // Nota: NO filtramos por id_oposicion. Antes solo se veían grupos del MISMO
    // id_oposicion del usuario → un opositor de Policía no veía grupos genéricos
    // ni comunidades públicas. Ahora devolvemos:
    //   - todos los COMUNIDAD (públicos, visibles para descubrir)
    //   - los PRIVADO en los que ya soy miembro
    //   - los de mi oposición (si paso idOposicion)
    const params = [userId, userId];
    let extra = '';
    if (opoParam != null) {
      extra = ' OR g.id_oposicion = ?';
      params.push(opoParam);
    }
    const [rows] = await db.query(
      `SELECT g.id_grupo, g.nombre, g.descripcion, g.id_oposicion,
              COALESCE(g.tipo, 'COMUNIDAD') AS tipo,
              g.creador_id, g.creado_en,
              (SELECT COUNT(*) FROM grupo_miembros gm WHERE gm.id_grupo = g.id_grupo) AS num_miembros,
              gm_self.rol AS mi_rol
       FROM grupos_comunidad g
       LEFT JOIN grupo_miembros gm_self ON gm_self.id_grupo = g.id_grupo AND gm_self.id_usuario = ?
       WHERE COALESCE(g.tipo, 'COMUNIDAD') = 'COMUNIDAD'
          OR gm_self.id_usuario = ?
          ${extra}
       ORDER BY (gm_self.rol IS NOT NULL) DESC, g.creado_en DESC`,
      params
    );
    return (rows || []).map((r) => serializarGrupo(r, r.mi_rol));
  }

  static async obtenerGrupo(userId, idGrupo) {
    const [rows] = await db.query(
      `SELECT g.id_grupo, g.nombre, g.descripcion, g.id_oposicion,
              COALESCE(g.tipo, 'COMUNIDAD') AS tipo,
              g.creador_id, g.creado_en,
              (SELECT COUNT(*) FROM grupo_miembros gm WHERE gm.id_grupo = g.id_grupo) AS num_miembros,
              gm_self.rol AS mi_rol
       FROM grupos_comunidad g
       LEFT JOIN grupo_miembros gm_self ON gm_self.id_grupo = g.id_grupo AND gm_self.id_usuario = ?
       WHERE g.id_grupo = ?
       LIMIT 1`,
      [userId, idGrupo]
    );
    if (!rows?.length) return null;
    return serializarGrupo(rows[0], rows[0].mi_rol);
  }

  static async crearGrupo(userId, { nombre, descripcion, idOposicion, tipo }) {
    const limpio = String(nombre || '').trim();
    if (!limpio) throw new Error('NOMBRE_OBLIGATORIO');
    const tipoLimpio = (String(tipo || 'COMUNIDAD').toUpperCase() === 'PRIVADO')
      ? 'PRIVADO' : 'COMUNIDAD';
    const opo = idOposicion == null ? null : Number(idOposicion);
    if (opo != null) {
      const [op] = await db.query('SELECT id_oposicion FROM oposiciones WHERE id_oposicion = ?', [opo]);
      if (!op?.length) throw new Error('OPOSICION_NO_VALIDA');
    }
    const connection = await db.getConnection();
    try {
      await connection.beginTransaction();
      const [ins] = await connection.query(
        `INSERT INTO grupos_comunidad (nombre, descripcion, id_oposicion, creador_id, tipo)
         VALUES (?, ?, ?, ?, ?)`,
        [
          limpio.substring(0, 120),
          String(descripcion || '').trim().substring(0, 500) || null,
          opo,
          userId,
          tipoLimpio
        ]
      );
      const idGrupo = ins.insertId;
      await connection.query(
        `INSERT INTO grupo_miembros (id_grupo, id_usuario, rol) VALUES (?, ?, 'ADMIN')`,
        [idGrupo, userId]
      );
      await connection.commit();
      // Devolvemos el grupo COMPLETO (no solo {idGrupo}) para que el frontend
      // pueda insertarlo en la lista sin tener que refrescar.
      return await GruposService.obtenerGrupo(userId, idGrupo);
    } catch (e) {
      await connection.rollback();
      throw e;
    } finally {
      connection.release();
    }
  }

  static async unirse(userId, idGrupo) {
    const [grupo] = await db.query(
      `SELECT id_grupo, COALESCE(tipo, 'COMUNIDAD') AS tipo FROM grupos_comunidad WHERE id_grupo = ?`,
      [idGrupo]
    );
    if (!grupo?.length) throw new Error('GRUPO_NO_ENCONTRADO');
    // Grupos PRIVADO solo permiten unión por invitación del creador.
    if (grupo[0].tipo === 'PRIVADO') throw new Error('GRUPO_SOLO_POR_INVITACION');
    const miembro = await GruposService.esMiembro(userId, idGrupo);
    if (miembro) throw new Error('YA_ERES_MIEMBRO');
    await db.query(
      `INSERT INTO grupo_miembros (id_grupo, id_usuario, rol) VALUES (?, ?, 'MIEMBRO')`,
      [idGrupo, userId]
    );
    return { ok: true };
  }

  static async invitarAmigo(userId, idGrupo, idAmigo) {
    const miembro = await GruposService.esMiembro(userId, idGrupo);
    if (!miembro) throw new Error('NO_ERES_MIEMBRO');
    if (miembro.rol !== 'ADMIN') throw new Error('SOLO_ADMIN');
    const [amigo] = await db.query('SELECT id_usuario, nombre FROM usuarios WHERE id_usuario = ?', [idAmigo]);
    if (!amigo?.length) throw new Error('USUARIO_NO_ENCONTRADO');
    const ya = await GruposService.esMiembro(idAmigo, idGrupo);
    if (ya) throw new Error('YA_ES_MIEMBRO');
    await db.query(
      `INSERT INTO grupo_miembros (id_grupo, id_usuario, rol) VALUES (?, ?, 'MIEMBRO')`,
      [idGrupo, idAmigo]
    );
    // Push (best-effort, no rompe el flujo si falla).
    try {
      const [grupo] = await db.query('SELECT nombre FROM grupos_comunidad WHERE id_grupo = ?', [idGrupo]);
      const [invitador] = await db.query('SELECT nombre FROM usuarios WHERE id_usuario = ?', [userId]);
      if (grupo?.[0] && invitador?.[0]) {
        await NotificationService().notificarInvitacionGrupo({
          idDestino: idAmigo,
          nombreGrupo: grupo[0].nombre,
          nombreInvitador: invitador[0].nombre
        });
      }
    } catch (_) { /* silencioso */ }
    return { ok: true };
  }

  static async salir(userId, idGrupo) {
    const miembro = await GruposService.esMiembro(userId, idGrupo);
    if (!miembro) throw new Error('NO_ERES_MIEMBRO');
    // El ADMIN/creador no puede "salir" — debe eliminar el grupo o transferir
    // la propiedad. Si lo permitiéramos quedaría un grupo huérfano sin admin.
    if (miembro.rol === 'ADMIN') throw new Error('CREADOR_DEBE_ELIMINAR_GRUPO');
    await db.query('DELETE FROM grupo_miembros WHERE id_grupo = ? AND id_usuario = ?', [idGrupo, userId]);
    return { ok: true };
  }

  static async eliminarGrupo(userId, idGrupo) {
    const [g] = await db.query(
      'SELECT creador_id FROM grupos_comunidad WHERE id_grupo = ?',
      [idGrupo]
    );
    if (!g?.length) throw new Error('GRUPO_NO_ENCONTRADO');
    if (Number(g[0].creador_id) !== Number(userId)) throw new Error('SOLO_CREADOR_PUEDE_ELIMINAR');
    const connection = await db.getConnection();
    try {
      await connection.beginTransaction();
      // Borramos en orden hijos → padre para respetar FKs.
      await connection.query('DELETE FROM grupo_mensajes WHERE id_grupo = ?', [idGrupo]).catch(() => {});
      await connection.query('DELETE FROM quedadas WHERE id_grupo = ?', [idGrupo]).catch(() => {});
      await connection.query('DELETE FROM grupo_miembros WHERE id_grupo = ?', [idGrupo]);
      await connection.query('DELETE FROM grupos_comunidad WHERE id_grupo = ?', [idGrupo]);
      await connection.commit();
      return { ok: true };
    } catch (e) {
      await connection.rollback();
      throw e;
    } finally {
      connection.release();
    }
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
    // Push a todos los miembros del grupo (excepto al autor).
    try {
      const [autor] = await db.query('SELECT nombre FROM usuarios WHERE id_usuario = ?', [userId]);
      const [grupo] = await db.query('SELECT nombre FROM grupos_comunidad WHERE id_grupo = ?', [idGrupo]);
      if (autor?.[0] && grupo?.[0]) {
        await NotificationService().notificarMensajeGrupo({
          idGrupo,
          idAutor: userId,
          nombreAutor: autor[0].nombre,
          nombreGrupo: grupo[0].nombre,
          texto: limpio
        });
      }
    } catch (_) { /* silencioso */ }
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
