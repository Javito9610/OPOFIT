// Importaciones:
const express= require("express"); //Necesitamos Express para usar el sistema de rutas (router)
const router= express.Router(); //Creamos un objeto 'router' para definir los caminos SIN ESTO, ESTE ARCHIVO NO VALE DE NADA
const oposicionesController= require("../controllers/OposicionController"); //Importamos el controller para saber a donde nos dirigimos y utilizarlo en la generación de la ruta 
const { validarToken } = require('../middleware/authMiddleware');
/**
 Definicion de rutas (ENDPOINTS):
 Cada ruta se compone de:
 -Un metodo (GET, POST, etc)
 -Una direccion URL
 -Una funcion a ejecutar (del controlador)
 */

 //OBTENER EL LISTADO PARA DESPLEGABLE DE REGISTRO:
 router.get("/", validarToken, oposicionesController.getOposiciones);

 //OBTENER TODA LA INFO DE UNA OPOSICION (PRUEBAS + NOTICIAS):
 router.get("/:id", validarToken, oposicionesController.getInfoOposiciones);

 //OBTENER LOS VALORES Y LAS NOTAS PARA CADA PRUEBA
 router.get("/requisitos/:id/:genero", validarToken, oposicionesController.getRequisitos);

 module.exports= router;
    