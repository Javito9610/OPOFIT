const db = require("../config/db");
const EjercicioInteligenteService = require("./EjercicioInteligenteService");
const EntornoEntreno = require("../utils/EntornoEntreno");

class RutinaPersService {
  static async crearRutinaPropia(userId, nombre, ejercicios, entorno = null) {
    const connection = await db.getConnection();
    try {
      const sqlCheck = 'SELECT id_rutina_pers FROM rutinas_pers WHERE usuarios_id_usuario = ? AND nombre_personalizado = ?';
      const [existente] = await connection.query(sqlCheck, [userId, nombre]);
      if (existente.length > 0) {
        throw new Error("Ya tienes una rutina con este nombre. ¡Prueba uno diferente!");
      }
      await connection.beginTransaction();
      const entornoNorm = EntornoEntreno.normalizarEntorno(entorno);
      const sqlRutina = entornoNorm
        ? `INSERT INTO rutinas_pers (nombre_personalizado, usuarios_id_usuario, entorno_entreno) VALUES (?, ?, ?)`
        : `INSERT INTO rutinas_pers (nombre_personalizado, usuarios_id_usuario) VALUES (?, ?)`;
      const rutinaParams = entornoNorm ? [nombre, userId, entornoNorm] : [nombre, userId];
      const [resRutina] = await connection.query(sqlRutina, rutinaParams);
      const idRutinaPers = resRutina.insertId;
      const sqlDetalleEjercicios = `INSERT INTO detalle_rutina_pers (rutinas_pers_id_rutina_pers, ejercicios_id_ejercicio, series, repeticiones, descanso) VALUES (?,?,?,?,?)`;
      for (const ej of ejercicios) {
        const [ejRows] = await connection.query(
          `SELECT id_ejercicio, nombre, pilar, categoria, grupo_muscular, equipamiento, instrucciones_tecnicas
           FROM ejercicios WHERE id_ejercicio = ?`,
          [ej.id_ejercicio]
        );
        const ejData = ejRows[0] || { nombre: '', id_ejercicio: ej.id_ejercicio };
        const inteligente = EjercicioInteligenteService.aplicarInteligencia(ejData, {
          seed: ej.id_ejercicio,
          entorno: entornoNorm
        });
        const series = Number(ej.series) > 0 ? Number(ej.series) : inteligente.series;
        const repeticiones = Number(ej.repeticiones) > 0 ? Number(ej.repeticiones) : inteligente.repeticiones;
        const descanso =
          ej.descanso != null && Number(ej.descanso) >= 0
            ? Number(ej.descanso)
            : (inteligente.descanso ?? 90);
        await connection.query(sqlDetalleEjercicios, [idRutinaPers, ej.id_ejercicio, series, repeticiones, descanso]);
      }
      await connection.commit();
      return idRutinaPers;
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }
  static async listarMisRutinas(userId) {
    const connection = await db.getConnection();
    try {
      const sqlListar = `SELECT r.id_rutina_pers, r.nombre_personalizado, r.entorno_entreno, d.ejercicios_id_ejercicio, e.nombre AS nombre_ejercicio, d.series, d.repeticiones, d.descanso
                FROM rutinas_pers r
                LEFT JOIN detalle_rutina_pers d ON r.id_rutina_pers = d.rutinas_pers_id_rutina_pers
                LEFT JOIN ejercicios e ON d.ejercicios_id_ejercicio = e.id_ejercicio
                WHERE r.usuarios_id_usuario = ?
                ORDER BY r.id_rutina_pers ASC`;
      const [rows] = await connection.query(sqlListar, [userId]);
      return rows;
    } finally {
      connection.release();
    }
  }
  static async eliminarRutina(userId, idRutina) {
    const connection = await db.getConnection();
    try {
      await connection.beginTransaction();
      const [rutina] = await connection.query(
        'SELECT id_rutina_pers FROM rutinas_pers WHERE id_rutina_pers = ? AND usuarios_id_usuario = ?',
        [idRutina, userId]
      );
      if (rutina.length === 0) {
        const err = new Error("No se encontró la rutina o no tienes permiso para eliminarla");
        err.code = 'NOT_FOUND';
        throw err;
      }
      // El historial de entrenos referencia la rutina: desvincular antes de borrar (evita HTTP 500 por FK).
      await connection.query(
        'UPDATE historial_sesiones SET rutinas_pers_id_rutina_pers = NULL WHERE rutinas_pers_id_rutina_pers = ?',
        [idRutina]
      );
      try {
        await connection.query('DELETE FROM rutinas_compartidas WHERE id_rutina_pers = ?', [idRutina]);
      } catch (e) {
        if (e.code !== 'ER_NO_SUCH_TABLE') throw e;
      }
      await connection.query('DELETE FROM detalle_rutina_pers WHERE rutinas_pers_id_rutina_pers = ?', [idRutina]);
      await connection.query(
        'DELETE FROM rutinas_pers WHERE id_rutina_pers = ? AND usuarios_id_usuario = ?',
        [idRutina, userId]
      );
      await connection.commit();
      return { success: true };
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }
}
module.exports = RutinaPersService;
