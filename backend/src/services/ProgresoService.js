const db = require("../config/db");

class ProgresoService {
  static esMejorMarca(nombreEjercicio, valorNuevo, valorAnterior) {
    if (valorAnterior == null || valorAnterior === undefined) return true;
    const n = String(nombreEjercicio || "").toLowerCase();
    const menorEsMejor =
      n.includes("carrera") ||
      n.includes("trote") ||
      n.includes("rodaje") ||
      n.includes("natación") ||
      n.includes("natacion") ||
      n.includes("sprint");
    if (menorEsMejor) return Number(valorNuevo) < Number(valorAnterior);
    return Number(valorNuevo) > Number(valorAnterior);
  }

  static async detectarRecords(userId, ejercicios) {
    const records = [];
    for (const item of ejercicios) {
      // Búsqueda con LEFT JOIN para que SIEMPRE devuelva el nombre del ejercicio,
      // incluso si es la primera vez que el usuario lo hace (sin marca previa).
      // Antes el JOIN normal daba 0 filas y caíamos en "Ejercicio 140" → feo.
      const [rows] = await db.query(
        `SELECT e.nombre AS nombre_ejercicio,
                e.modalidad AS modalidad,
                e.score_tipo AS score_tipo,
                (
                  SELECT r.valor_conseguido
                    FROM registro_resultados r
                    JOIN historial_sesiones h
                      ON r.historial_sesiones_id_historial_sesiones = h.id_historial_sesion
                   WHERE h.usuarios_id_usuario = ?
                     AND r.ejercicios_id_ejercicio = ?
                   ORDER BY h.fecha_entreno DESC
                   LIMIT 1
                ) AS valor_anterior
           FROM ejercicios e
          WHERE e.id_ejercicio = ?
          LIMIT 1`,
        [userId, item.id_ejercicio, item.id_ejercicio]
      );
      const row = rows?.[0];
      const nombre = row?.nombre_ejercicio || `Ejercicio ${item.id_ejercicio}`;
      const modalidad = row?.modalidad || null;
      const scoreTipo = row?.score_tipo || null;
      const anterior = row?.valor_anterior != null ? Number(row.valor_anterior) : null;
      // Solo PR cuando ya tenía un valor anterior y lo mejora. Antes la primera
      // vez se contaba como "récord", lo que llenaba el diálogo con N filas
      // que no eran realmente nuevos PRs (eran marcas iniciales).
      if (anterior != null && ProgresoService.esMejorMarca(nombre, item.valor, anterior)) {
        records.push({
          idEjercicio: item.id_ejercicio,
          nombreEjercicio: nombre,
          valorAnterior: anterior,
          valorNuevo: Number(item.valor),
          modalidad,
          scoreTipo,
          mejoraPorcentaje:
            anterior > 0
              ? Math.round(((Number(item.valor) - anterior) / anterior) * 1000) / 10
              : null
        });
      }
    }
    return records;
  }

  static async registrarEntreno(datos) {
    const {
      userId,
      tipoRutina,
      idRutina,
      duracion,
      ejercicios,
      gpsActividadUuid = null
    } = datos;
    const recordsRotos = await ProgresoService.detectarRecords(userId, ejercicios);
    const connection = await db.getConnection();
    try {
      await connection.beginTransaction();
      const rutinaCol = tipoRutina === 'OPO' ? 'rutinas_opo_id_rutina_opo' : 'rutinas_pers_id_rutina_pers';
      const sqlHistorial = `INSERT INTO historial_sesiones
                (fecha_entreno, tipo_rutina, duracion_oficial, usuarios_id_usuario, ${rutinaCol}, gps_actividad_uuid)
                VALUES (NOW(), ?, ?, ?, ?, ?)`;
      const [resHistorial] = await connection.query(
        sqlHistorial,
        [tipoRutina, duracion, userId, idRutina, gpsActividadUuid]
      );
      const idHistorial = resHistorial.insertId;
      const sqlResultados = `
                INSERT INTO registro_resultados 
                (ejercicios_id_ejercicio, historial_sesiones_id_historial_sesiones, valor_conseguido) 
                VALUES (?, ?, ?)`;
      for (const item of ejercicios) {
        await connection.query(sqlResultados, [item.id_ejercicio, idHistorial, item.valor]);
      }
      await connection.commit();
      return {
        success: true,
        idHistorial,
        recordsRotos
      };
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }
  static async obtenerEvolucionEntreno(userId, idEjercicio) {
    const sql = `SELECT h.fecha_entreno, h.duracion_oficial, r.valor_conseguido, e.nombre AS nombre_ejercicio
            FROM registro_resultados r
            JOIN historial_sesiones h ON r.historial_sesiones_id_historial_sesiones = h.id_historial_sesion
            JOIN ejercicios e ON r.ejercicios_id_ejercicio = e.id_ejercicio
            WHERE h.usuarios_id_usuario = ? AND r.ejercicios_id_ejercicio = ?
            ORDER BY h.fecha_entreno ASC`;
    const [rows] = await db.query(sql, [userId, idEjercicio]);
    return rows;
  }
  static async obtenerHistorialSesiones(userId) {
    const sql = `
            SELECT h.id_historial_sesion, h.fecha_entreno, h.tipo_rutina, h.duracion_oficial,
                   h.rutinas_opo_id_rutina_opo, h.rutinas_pers_id_rutina_pers
            FROM historial_sesiones h
            WHERE h.usuarios_id_usuario = ?
            ORDER BY h.fecha_entreno DESC`;
    const [rows] = await db.query(sql, [userId]);
    return rows;
  }

  /**
   * Borra una sesión de historial. Valida que pertenezca al usuario.
   * Por integridad referencial elimina primero los detalles y luego la cabecera.
   */
  static async borrarSesion(userId, idSesion) {
    const [[sesion]] = await db.query(
      'SELECT usuarios_id_usuario FROM historial_sesiones WHERE id_historial_sesion = ?',
      [idSesion]
    );
    if (!sesion) return { ok: false, code: 'NO_ENCONTRADA', msg: 'Sesión no encontrada' };
    if (Number(sesion.usuarios_id_usuario) !== Number(userId)) {
      return { ok: false, code: 'NO_AUTORIZADO', msg: 'No puedes borrar sesiones de otro usuario' };
    }
    // Borramos primero los hijos (FK) y luego la cabecera. Antes faltaba
    // registro_resultados → ENTRY orphan + borrar fallaba si la tabla detalle
    // no existía. Ahora hacemos las dos tablas en orden seguro.
    try {
      await db.query(
        'DELETE FROM registro_resultados WHERE historial_sesiones_id_historial_sesiones = ?',
        [idSesion]
      );
    } catch (_) { /* ya gestionado por FK CASCADE en algunos despliegues */ }
    try {
      await db.query(
        'DELETE FROM detalle_historial_sesion WHERE historial_sesiones_id_historial_sesion = ?',
        [idSesion]
      );
    } catch (_) {
      /* Si la tabla de detalle no existe en alguna instalación, seguimos */
    }
    await db.query('DELETE FROM historial_sesiones WHERE id_historial_sesion = ?', [idSesion]);
    return { ok: true };
  }

  /**
   * Vaciado total del historial de un usuario (sesiones + sus resultados).
   * NO toca actividades GPS (esas se borran desde su propia pantalla con su
   * propio endpoint para no mezclar dominios).
   */
  static async vaciarHistorial(userId) {
    // Recuperamos los ids para borrar resultados con un único IN().
    const [sesiones] = await db.query(
      'SELECT id_historial_sesion FROM historial_sesiones WHERE usuarios_id_usuario = ?',
      [userId]
    );
    const ids = sesiones.map((s) => s.id_historial_sesion);
    if (ids.length === 0) return { ok: true, borradas: 0 };
    const placeholders = ids.map(() => '?').join(',');
    try {
      await db.query(
        `DELETE FROM registro_resultados WHERE historial_sesiones_id_historial_sesiones IN (${placeholders})`,
        ids
      );
    } catch (_) { /* idem */ }
    try {
      await db.query(
        `DELETE FROM detalle_historial_sesion WHERE historial_sesiones_id_historial_sesion IN (${placeholders})`,
        ids
      );
    } catch (_) { /* tabla opcional */ }
    const [r] = await db.query(
      'DELETE FROM historial_sesiones WHERE usuarios_id_usuario = ?',
      [userId]
    );
    return { ok: true, borradas: Number(r.affectedRows || ids.length) };
  }
}
module.exports = ProgresoService;
