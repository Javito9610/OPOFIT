const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/MapasController');

router.get('/tipos', validarToken, ctrl.tipos);
router.get('/lugares', validarToken, ctrl.lugares);
router.get('/rutas/sugerida', validarToken, ctrl.rutaSugerida);
router.post('/rutas/personalizada', validarToken, ctrl.rutaPersonalizada);
router.post('/rutas/export/gpx', validarToken, ctrl.exportarGpx);

module.exports = router;
