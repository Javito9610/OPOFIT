const express = require("express");
const router = express.Router();
const authController = require("../controllers/AuthController");
const {
  validarToken
} = require('../middleware/authMiddleware');
router.post('/registrar', authController.registrar);
router.post('/login', authController.login);
router.post('/google', authController.loginConGoogle);
router.post('/google/registrar', authController.registrarConGoogle);
router.post('/google_firebase', authController.loginConFirebase);
router.post('/google_firebase/registrar', authController.registrarConFirebase);
router.get('/me', validarToken, authController.me);
router.post('/cambiar-password', validarToken, authController.cambiarPassword);
module.exports = router;
