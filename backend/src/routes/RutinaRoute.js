const express = require("express");
const router = express.Router();
const rutinaController = require('../controllers/RutinaController');
const {
  validarToken
} = require('../middleware/authMiddleware');
router.get("/mi-entrenamiento/:userId/:idOposicion", validarToken, rutinaController.getMiEntrenamiento);
module.exports = router;
