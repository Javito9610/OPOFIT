const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/RankingController');

router.get('/:idOposicion', validarToken, ctrl.getRanking);
router.get('/:idOposicion/mi-posicion', validarToken, ctrl.getMiPosicion);
router.put('/perfil-publico', validarToken, ctrl.togglePerfilPublico);

module.exports = router;
