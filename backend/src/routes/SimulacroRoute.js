const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/SimulacroController');

router.get('/pruebas/:idOposicion', validarToken, ctrl.listarPruebas);
router.post('/guardar', validarToken, ctrl.guardar);
router.post('/aplicar-marcas', validarToken, ctrl.aplicarMarcasPerfil);
router.get('/historial/:idOposicion', validarToken, ctrl.historial);

module.exports = router;
