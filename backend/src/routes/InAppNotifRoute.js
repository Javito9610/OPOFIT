const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/InAppNotifController');

router.get('/', validarToken, ctrl.listar);
router.get('/contador', validarToken, ctrl.contadorNoLeidas);
router.put('/:id/leida', validarToken, ctrl.marcarLeida);
router.put('/leidas/todas', validarToken, ctrl.marcarTodasLeidas);
router.delete('/:id', validarToken, ctrl.eliminar);

module.exports = router;
