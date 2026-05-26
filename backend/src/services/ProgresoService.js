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
      const [prev] = await db.query(
        `SELECT r.valor_conseguido, e.nombre AS nombre_ejercicio
         FROM registro_resultados r
         JOIN historial_sesiones h ON r.historial_sesiones_id_historial_sesiones = h.id_historial_sesion
         JOIN ejercicios e ON r.ejercicios_id_ejercicio = e.id_ejercicio
         WHERE h.usuarios_id_usuario = ? AND r.ejercicios_id_ejercicio = ?
         ORDER BY h.fecha_entreno DESC
         LIMIT 1`,
        [userId, item.id_ejercicio]
      );
      const nombre = prev?.[0]?.nombre_ejercicio || `Ejercicio ${item.id_ejercicio}`;
      const anterior = prev?.[0]?.valor_conseguido != null ? Number(prev[0].valor_conseguido) : null;
      if (ProgresoService.esMejorMarca(nombre, item.valor, anterior)) {
        records.push({
          idEjercicio: item.id_ejercicio,
          nombreEjercicio: nombre,
          valorAnterior: anterior,
          valorNuevo: Number(item.valor)
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
}
module.exports = ProgresoService;
