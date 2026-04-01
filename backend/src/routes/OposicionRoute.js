const express = require("express");
const router = express.Router();
const oposicionesController = require("../controllers/OposicionController");
const {
  validarToken
} = require('../middleware/authMiddleware');
// Raíz con y sin barra final: si no coincide, Express puede caer en /:id y pedir token.
router.get(["/", ""], oposicionesController.getOposiciones);
router.get("/rss/:id", validarToken, oposicionesController.getNoticiasRss);
router.get("/requisitos/:id/:genero", validarToken, oposicionesController.getRequisitos);
// Solo IDs numéricos; así no intercepta la lista ni rutas raras.
router.get("/:id(\\d+)", validarToken, oposicionesController.getInfoOposiciones);
module.exports = router;
