const express = require('express');
const router = express.Router();
const { validarAdminApiKey } = require('../middleware/adminMiddleware');
const ctrl = require('../controllers/AdminController');

router.use(validarAdminApiKey);

router.get('/oposiciones', ctrl.listOposiciones);
router.post('/oposiciones', ctrl.upsertOposicion);
router.get('/ejercicios', ctrl.listEjercicios);
router.post('/ejercicios', ctrl.upsertEjercicio);
router.get('/pruebas', ctrl.listPruebas);
router.post('/pruebas', ctrl.upsertPrueba);
router.post('/baremos', ctrl.upsertBaremo);
router.post('/notificaciones/recordatorios', ctrl.enviarRecordatorios);
router.post('/notificaciones/noticia', ctrl.enviarNoticia);

module.exports = router;
