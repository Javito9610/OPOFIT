const express = require("express");
const router = express.Router();
const oposicionesController = require("../controllers/OposicionController");
const {
  validarToken
} = require('../middleware/authMiddleware');
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
router.get(["/", ""], oposicionesController.getOposiciones);
router.get("/rss/:id", validarToken, oposicionesController.getNoticiasRss);
router.get("/requisitos/:id/:genero", validarToken, oposicionesController.getRequisitos);
router.get("/:id", soloIdNumerico, validarToken, oposicionesController.getInfoOposiciones);
module.exports = router;
