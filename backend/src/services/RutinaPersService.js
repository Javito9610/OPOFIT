const db = require("../config/db");
class RutinaPersService {
  static async crearRutinaPropia(userId, nombre, ejercicios) {
    const connection = await db.getConnection();
    try {
      const sqlCheck = 'SELECT id_rutina_pers FROM rutinas_pers WHERE usuarios_id_usuario = ? AND nombre_personalizado = ?';
      const [existente] = await connection.query(sqlCheck, [userId, nombre]);
      if (existente.length > 0) {
        throw new Error("Ya tienes una rutina con este nombre. ¡Prueba uno diferente!");
      }
      await connection.beginTransaction();
      const sqlRutina = `INSERT INTO rutinas_pers (nombre_personalizado, usuarios_id_usuario) VALUES (?, ?)`;
      const [resRutina] = await connection.query(sqlRutina, [nombre, userId]);
      const idRutinaPers = resRutina.insertId;
      const sqlDetalleEjercicios = `INSERT INTO detalle_rutina_pers (rutinas_pers_id_rutina_pers, ejercicios_id_ejercicio, series, repeticiones) VALUES (?,?,?,?)`;
      for (const ej of ejercicios) {
        await connection.query(sqlDetalleEjercicios, [idRutinaPers, ej.id_ejercicio, ej.series, ej.repeticiones]);
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
      const sqlListar = `SELECT r.id_rutina_pers, r.nombre_personalizado, d.ejercicios_id_ejercicio, e.nombre AS nombre_ejercicio, d.series, d.repeticiones, d.descanso
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
      const [rutina] = await connection.query('SELECT id_rutina_pers FROM rutinas_pers WHERE id_rutina_pers = ? AND usuarios_id_usuario = ?', [idRutina, userId]);
      if (rutina.length === 0) {
        throw new Error("No se encontró la rutina o no tienes permiso para eliminarla");
      }
      await connection.query('DELETE FROM detalle_rutina_pers WHERE rutinas_pers_id_rutina_pers = ?', [idRutina]);
      await connection.query('DELETE FROM rutinas_pers WHERE id_rutina_pers = ? AND usuarios_id_usuario = ?', [idRutina, userId]);
      await connection.commit();
      return {
        success: true
      };
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }
}
module.exports = RutinaPersService;
