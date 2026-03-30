const db = require("../config/db");

class OposicionService{

    static async obtenerTodas(){
        const sqlObtenerTodas="SELECT * FROM oposiciones ORDER BY nombre ASC";
        const[rows]=await db.query(sqlObtenerTodas);
        return rows;
    }

    static async obtenerDetalleCompleto(idOposicion){

        const sqlPruebasOficiales="SELECT * FROM pruebas_oficiales WHERE oposiciones_id_oposicion = ?";
        const [pruebas]=await db.query(sqlPruebasOficiales, [idOposicion]);

        const sqlNoticiasRecientes="SELECT * FROM noticias WHERE oposiciones_id_oposicion = ? ORDER BY fecha_publicacion DESC";
        const [noticias]=await db.query(sqlNoticiasRecientes, [idOposicion]);

        return {pruebas, noticias};

    }

    static async obtenerRequisitosPrueba(idPrueba, genero){
        const sqlObtenerRequisitosPrueba="SELECT nivel_exigencia, valor_objetivo FROM requisitos_nivel WHERE pruebas_oficiales_id_pruebas_oficiales=? AND genero=? ORDER BY nivel_exigencia ASC";
        const [requisitos]=await db.query(sqlObtenerRequisitosPrueba, [idPrueba, genero]);
        return requisitos;
    }
}
module.exports=OposicionService;
