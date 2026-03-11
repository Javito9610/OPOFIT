const db=require('../config/db');

//Le pedimos a la base de datos los datos de cualquier oposicion para reflejarlos en una vista de información.

class InfoPruebasService{

    static async getInfoPruebas(idOposicion, genero){

        const [getInfoPruebas]= await db.query(`SELECT p.nombre_prueba, p.descripcion, p.trucos, b.genero, b.marca_valor, b.nota
            FROM pruebas_oficiales p
            JOIN  baremos_puntuacion b ON p.id_pruebas_oficiales=b.pruebas_oficiales_id_pruebas_oficiales
            WHERE b.genero=? AND p.oposiciones_id_oposicion=?
            ORDER BY p.nombre_prueba ASC, b.nota ASC`, [genero,idOposicion]);
            return getInfoPruebas;
    }
}
module.exports=InfoPruebasService;