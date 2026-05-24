const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/RankingController');

router.get('/:idOposicion/usuario/:userId', validarToken, ctrl.getDetalleUsuario);
router.get('/:idOposicion/mi-posicion', validarToken, ctrl.getMiPosicion);
router.get('/:idOposicion', validarToken, ctrl.getRanking);
router.put('/perfil-publico', validarToken, ctrl.togglePerfilPublico);

module.exports = router;
