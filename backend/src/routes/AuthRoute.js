const express= require("express");
const router= express.Router();
const authController= require("../controllers/AuthController");

 router.post('/registrar', authController.registrar);

 router.post('/login', authController.login);

 router.post('/google', authController.loginConGoogle);

 router.post('/google/registrar', authController.registrarConGoogle);

 module.exports=router;
