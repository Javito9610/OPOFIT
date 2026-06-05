const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/AnalisisController');

router.post('/zonas-fc', validarToken, ctrl.zonasFC);
router.post('/zonas-distribucion', validarToken, ctrl.zonasDistribucion);
router.post('/tss', validarToken, ctrl.tss);
router.post('/prediccion', validarToken, ctrl.prediccion);
router.post('/vo2max', validarToken, ctrl.vo2max);

module.exports = router;
