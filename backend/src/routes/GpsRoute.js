const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/GpsController');

router.post('/actividades', validarToken, ctrl.guardar);
router.get('/actividades', validarToken, ctrl.listar);
router.get('/actividades/:uuid', validarToken, ctrl.detalle);
router.delete('/actividades/:uuid', validarToken, ctrl.borrar);

module.exports = router;
