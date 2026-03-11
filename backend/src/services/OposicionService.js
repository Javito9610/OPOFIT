// IMPORTACIONES:
//Importamos el archivo config/db para crear esa conexion con la BBDD a la hora de enviar las consultas
const db = require("../config/db");

class OposicionService{

    // OBTENER TODAS LAS OPOSICIONES PARA EL REGISTRO Y LA SELECCIÓN INICIAL
    static async obtenerTodas(){// async para indicar que en la función habra momentos en los que mediante un await tengamos que esperar a que se ejecute la consulta para poder continuar
        const sqlObtenerTodas="SELECT * FROM oposiciones ORDER BY nombre ASC";
        const[rows]=await db.query(sqlObtenerTodas);
        return rows;
    }

    //DETALLE COMPLETO DE UNA OPOSICIÓN (INCLUYE PRUEBAS Y NOTICIAS):
    //ESTA FUNCIÓN SE USARÁ PARA CARAGAR LA "HOME" DE LA OPOSICIÓN EN LA APP
    static async obtenerDetalleCompleto(idOposicion){

        //CONSULTAMOS LAS PRUEBAS OFICIALES:
        const sqlPruebasOficiales="SELECT * FROM pruebas_oficiales WHERE oposiciones_id_oposicion = ?";
        const [pruebas]=await db.query(sqlPruebasOficiales, [idOposicion]);

        //CONSULTAMOS LAS NOTICIAS RECIENTES:
        const sqlNoticiasRecientes="SELECT * FROM noticias WHERE oposiciones_id_oposicion = ? ORDER BY fecha_publicacion DESC";
        const [noticias]=await db.query(sqlNoticiasRecientes, [idOposicion]);

        return {pruebas, noticias};

    }

    //LÓGICA DE REQUISITOS. iMPRESCINDIBLE PARA LAS FUNCIONALIDADES 3 Y 5 DE MI DOCUMENTO PDF "funcionalidades"
    //ESTA FUNCIÓN PERMITE SABER QUE MARCAS HACEN FALTA PARA CADA NIVEL EN UNA PRUEBA. EN RESUMIDAS CUENTAS, INDICA LA PUNTUACION EN FUNCION DE LAS MARCAS PARA CADA TIPO DE OPOSICIÓN Y SEXO

    static async obtenerRequisitosPrueba(idPrueba, genero){
        const sqlObtenerRequisitosPrueba="SELECT nivel_exigencia, valor_objetivo FROM requisitos_nivel WHERE pruebas_oficiales_id_pruebas_oficiales=? AND genero=? ORDER BY nivel_exigencia ASC";
        const [requisitos]=await db.query(sqlObtenerRequisitosPrueba, [idPrueba, genero]);
        return requisitos;
    }
}
module.exports=OposicionService;
