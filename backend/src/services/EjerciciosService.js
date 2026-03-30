const db = require('../config/db');

class EjerciciosService {
    static async listarTodos() {
        const [rows] = await db.query('SELECT id_ejercicio, nombre, video_url, instrucciones_tecnicas FROM ejercicios ORDER BY nombre ASC');
        return rows;
    }
}

module.exports = EjerciciosService;
