const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/SegmentosController');

router.get('/', validarToken, ctrl.listar);
router.post('/geo', validarToken, ctrl.crearGeografico);
router.post('/desde-actividad', validarToken, ctrl.registrarDesdeActividad);
router.get('/:idSegmento/ranking', validarToken, ctrl.ranking);
router.post('/:idSegmento/esfuerzo', validarToken, ctrl.registrarEsfuerzo);

module.exports = router;
