const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/PremiumController');

router.get('/estado', validarToken, ctrl.estado);
router.post('/activar-prueba', validarToken, ctrl.activarPrueba);

module.exports = router;
