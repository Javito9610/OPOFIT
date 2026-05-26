const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/IntegracionesController');

router.get('/estado', validarToken, ctrl.estado);

router.get('/strava/start', validarToken, ctrl.stravaStart);
router.get('/strava/callback', ctrl.stravaCallback);
router.post('/strava/sync', validarToken, ctrl.stravaSync);
router.delete('/strava', validarToken, ctrl.stravaDisconnect);

router.get('/polar/start', validarToken, ctrl.polarStart);
router.get('/polar/callback', ctrl.polarCallback);
router.post('/polar/sync', validarToken, ctrl.polarSync);
router.delete('/polar', validarToken, ctrl.polarDisconnect);

router.post('/health-connect/importar', validarToken, ctrl.importarHealthConnect);

module.exports = router;
