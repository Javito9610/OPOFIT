const express = require("express");
const router = express.Router();
const ejerciciosController = require('../controllers/EjerciciosController');
const {
  validarToken
} = require('../middleware/authMiddleware');
router.get("/", validarToken, ejerciciosController.listarEjercicios);
router.get("/material", validarToken, ejerciciosController.listarMaterial);
module.exports = router;
