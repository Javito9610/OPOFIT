const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const {
  getCalendario,
  getEntornos,
  getEntornoUsuario,
  putEntornoUsuario,
  postRegenerarPlan
} = require('../controllers/PlanesController');

router.get('/calendario/:idOposicion', validarToken, getCalendario);
router.get('/entornos', validarToken, getEntornos);
router.get('/entorno', validarToken, getEntornoUsuario);
router.put('/entorno', validarToken, putEntornoUsuario);
router.post('/regenerar/:idOposicion', validarToken, postRegenerarPlan);

module.exports = router;
