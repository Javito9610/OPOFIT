const db = require("../config/db");
class ProgresoService{
    static async registrarEntreno(datos){
        const {userId, tipoRutina, idRutina, duracion, ejercicios}= datos;
        const connection = await db.getConnection();
        try{

            const sqlCheck = `
                SELECT id_historial_sesion 
                FROM historial_sesiones 
                WHERE usuarios_id_usuario = ? 
                AND ${tipoRutina === 'OPO' ? 'rutinas_opo_id_rutina_opo' : 'rutinas_pers_id_rutina_pers'} = ?
                AND DATE(fecha_entreno) = CURDATE()`;
            
            const [existente] = await connection.query(sqlCheck, [userId, idRutina]);

            if (existente.length > 0) {
                throw new Error("Ya has registrado este entrenamiento hoy. ¡Mañana más!");
            }

            await connection.beginTransaction();
            const sqlHistorial= `INSERT INTO historial_sesiones 
                (fecha_entreno, tipo_rutina, duracion_oficial, usuarios_id_usuario, ${tipoRutina === 'OPO' ? 'rutinas_opo_id_rutina_opo' : 'rutinas_pers_id_rutina_pers'})
                VALUES (NOW(), ?, ?, ?, ?)`;
            const [resHistorial]= await connection.query(sqlHistorial,[tipoRutina, duracion, userId, idRutina]);
            const idHistorial= resHistorial.insertId;
            const sqlResultados = `
                INSERT INTO registro_resultados 
                (ejercicios_id_ejercicio, historial_sesiones_id_historial_sesiones, valor_conseguido) 
                VALUES (?, ?, ?)`;
            for (const item of ejercicios) {
                await connection.query(sqlResultados, [item.id_ejercicio, idHistorial, item.valor]);
            }
            await connection.commit();
            return {success: true, idHistorial}
        }catch(error){
            await connection.rollback();
            throw error;
        }finally{
            connection.release();
        }
    }

    static async obtenerEvolucionEntreno(userId, idEjercicio){
       const sql= `SELECT h.fecha_entreno, r.valor_conseguido, e.nombre AS nombre_ejercicio
            FROM registro_resultados r
            JOIN historial_sesiones h ON r.historial_sesiones_id_historial_sesiones = h.id_historial_sesion
            JOIN ejercicios e ON r.ejercicios_id_ejercicio = e.id_ejercicio
            WHERE h.usuarios_id_usuario = ? AND r.ejercicios_id_ejercicio = ?
            ORDER BY h.fecha_entreno ASC`; 
        const [rows]= await db.query(sql,[userId, idEjercicio])
        return rows
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
module.exports=ProgresoService;
