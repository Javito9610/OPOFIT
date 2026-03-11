const express = require("express");
const router= express.Router();
const infoPruebasControler=require('../controllers/InfoPruebasController');
const { validarToken } = require('../middleware/authMiddleware');

router.get("/:idOposicion/:genero", validarToken, infoPruebasControler.getInfoPruebas)
module.exports=router;