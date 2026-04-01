const express = require("express");
const router = express.Router();
const oposicionesController = require("../controllers/OposicionController");
const {
  validarToken
} = require('../middleware/authMiddleware');
// Express 5 / path-to-regexp ya no admite /:id(\d+) en la ruta; validamos el id aquí.
function soloIdNumerico(req, res, next) {
  const {
    id
  } = req.params;
  if (!id || !/^\d+$/.test(String(id))) {
    return res.status(404).json({
      ok: false,
      msg: 'Recurso no encontrado'
    });
  }
  next();
}
// Raíz con y sin barra final.
router.get(["/", ""], oposicionesController.getOposiciones);
router.get("/rss/:id", validarToken, oposicionesController.getNoticiasRss);
router.get("/requisitos/:id/:genero", validarToken, oposicionesController.getRequisitos);
router.get("/:id", soloIdNumerico, validarToken, oposicionesController.getInfoOposiciones);
module.exports = router;
