const express = require("express");
const router = express.Router();
const usuarioController = require('../controllers/UsuarioController');
const {
  validarToken
} = require('../middleware/authMiddleware');
router.get('/perfil', validarToken, usuarioController.obtenerPerfil);
router.put('/perfil', validarToken, usuarioController.actualizarPerfil);
router.put('/settings', validarToken, usuarioController.actualizarSettings);
router.delete('/cuenta', validarToken, usuarioController.eliminarCuenta);
module.exports = router;
