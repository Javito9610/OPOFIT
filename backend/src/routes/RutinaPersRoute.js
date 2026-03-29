const express = require("express");
const router= express.Router();
const rutinaPersController=require('../controllers/RutinaPersController');
const { validarToken } = require('../middleware/authMiddleware');

router.post('/crear', validarToken, rutinaPersController.nuevaRutinaPersonalizada);
router.get('/usuario/:userId', validarToken, rutinaPersController.misRutinas);
router.delete('/eliminar/:userId/:idRutina', validarToken, rutinaPersController.eliminarRutina);
module.exports = router;