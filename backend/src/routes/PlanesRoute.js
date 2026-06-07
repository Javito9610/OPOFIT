const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const {
  getCalendario,
  getEntornos,
  getEntornoUsuario,
  putEntornoUsuario,
  postRegenerarPlan,
  postRegenerarDia,
  postDisenarSesionIA
} = require('../controllers/PlanesController');

router.get('/calendario/:idOposicion', validarToken, getCalendario);
router.get('/entornos', validarToken, getEntornos);
router.get('/entorno', validarToken, getEntornoUsuario);
router.put('/entorno', validarToken, putEntornoUsuario);
router.post('/regenerar/:idOposicion', validarToken, postRegenerarPlan);
router.post('/regenerar-dia/:idOposicion/:idPlanDia', validarToken, postRegenerarDia);
router.post('/ia-disena/:idOposicion/:idPlanDia', validarToken, postDisenarSesionIA);

module.exports = router;
