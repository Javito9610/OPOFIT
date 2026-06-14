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
  postDisenarSesionIA,
  putOnboarding
} = require('../controllers/PlanesController');

router.get('/calendario/:idOposicion', validarToken, getCalendario);
router.get('/entornos', validarToken, getEntornos);
router.get('/entorno', validarToken, getEntornoUsuario);
router.put('/entorno', validarToken, putEntornoUsuario);
// Onboarding Freeletics: guarda objetivo + días + tiempo + lesiones en una sola llamada.
router.put('/onboarding', validarToken, putOnboarding);
router.post('/regenerar/:idOposicion', validarToken, postRegenerarPlan);
router.post('/regenerar-dia/:idOposicion/:idPlanDia', validarToken, postRegenerarDia);
router.post('/ia-disena/:idOposicion/:idPlanDia', validarToken, postDisenarSesionIA);

module.exports = router;
