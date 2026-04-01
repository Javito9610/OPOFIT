const db = require('../config/db');
class InfoPruebasService {
  static async getInfoPruebas(idOposicion, genero) {
    const [getInfoPruebas] = await db.query(`SELECT p.id_pruebas_oficiales, p.nombre_prueba, p.descripcion, p.trucos, p.mejor_si_es_menor,
            b.genero, b.marca_valor, b.nota
            FROM pruebas_oficiales p
            JOIN  baremos_puntuacion b ON p.id_pruebas_oficiales=b.pruebas_oficiales_id_pruebas_oficiales
            WHERE b.genero=? AND p.oposiciones_id_oposicion=?
            ORDER BY p.nombre_prueba ASC, b.nota ASC`, [genero, idOposicion]);
    return getInfoPruebas;
  }
  static async getMarcasUsuario(userId, idOposicion) {
    const [marcas] = await db.query(`
            SELECT m.id_marcas_perfil, m.valord_record, m.fecha_logro,
                   p.id_pruebas_oficiales, p.nombre_prueba, p.mejor_si_es_menor
            FROM marcas_perfil m
            JOIN pruebas_oficiales p ON m.pruebas_oficiales_id_pruebas_oficiales = p.id_pruebas_oficiales
            WHERE m.usuarios_id_usuario = ? AND p.oposiciones_id_oposicion = ?
            ORDER BY p.nombre_prueba ASC`, [userId, idOposicion]);
    return (marcas || []).map(m => ({
      ...m,
      unidad: Number(m.mejor_si_es_menor) === 1 ? 's' : 'reps'
    }));
  }
}
module.exports = InfoPruebasService;
