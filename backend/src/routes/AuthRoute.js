// Importaciones:
const express= require("express"); //Necesitamos Express para usar el sistema de rutas (router)
const router= express.Router(); //Creamos un objeto 'router' para definir los caminos SIN ESTO, ESTE ARCHIVO NO VALE DE NADA
const authController= require("../controllers/AuthController"); //Importamos el controller para saber a donde nos dirigimo y utilizarlo en la generación de la ruta 

/**
 Definicion de rutas (ENDPOINTS):
 Cada ruta se compone de:
 -Un metodo (GET, POST, etc)
 -Una direccion URL
 -Una funcion a ejecutar (del controlador)
 */

 // RUTA DE REGISTRO: http://localhost:3000/api/auth/registrar
 router.post('/registrar', authController.registrar);

 // RUTA DE LOGIN: http://localhost:3000/api/auth/login

 router.post('/login', authController.login);

 // RUTA DE LOGIN CON GOOGLE: http://localhost:3000/api/auth/google
 router.post('/google', authController.loginConGoogle);

 // EXPORTACIÓN:

 module.exports=router;