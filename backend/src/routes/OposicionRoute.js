const express= require("express");
const router= express.Router();
const oposicionesController= require("../controllers/OposicionController");
const { validarToken } = require('../middleware/authMiddleware');

 router.get("/", validarToken, oposicionesController.getOposiciones);

 router.get("/:id", validarToken, oposicionesController.getInfoOposiciones);

 router.get("/requisitos/:id/:genero", validarToken, oposicionesController.getRequisitos);

 module.exports= router;
