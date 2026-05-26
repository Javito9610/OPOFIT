const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/HistorialAvanzadoController');

router.get('/resumen', validarToken, ctrl.resumen);
router.get('/sesiones', validarToken, ctrl.listarSesiones);
router.get('/sesion/:id', validarToken, ctrl.detalleSesion);
router.get('/ejercicio/:idEjercicio', validarToken, ctrl.historialEjercicio);
router.get('/plan/:idPlan', validarToken, ctrl.historialPlan);

module.exports = router;
