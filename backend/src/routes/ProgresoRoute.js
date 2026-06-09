const express = require("express");
const router = express.Router();
const progresoController = require('../controllers/ProgresoController');
const {
  validarToken
} = require('../middleware/authMiddleware');
router.post('/registrar', validarToken, progresoController.guardarEntrenamiento);
router.get('/evolucion/:userId/:idEjercicio', validarToken, progresoController.verEvolucion);
router.get('/sesiones/:userId', validarToken, progresoController.verHistorialSesiones);
router.delete('/sesion/:id', validarToken, progresoController.borrarSesion);
router.delete('/sesiones', validarToken, progresoController.vaciarHistorial);
module.exports = router;
