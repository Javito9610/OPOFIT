const express = require("express");
const router= express.Router();
const usuarioController=require('../controllers/UsuarioController');
const { validarToken } = require('../middleware/authMiddleware');

router.put('/perfil', validarToken, usuarioController.actualizarPerfil);
router.put('/settings', validarToken, usuarioController.actualizarSettings);

module.exports= router;