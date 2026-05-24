const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/AmigosController');

router.get('/', validarToken, ctrl.listar);
router.get('/feed', validarToken, ctrl.feed);
router.get('/buscar', validarToken, ctrl.buscar);
router.post('/solicitar', validarToken, ctrl.solicitar);
router.post('/responder', validarToken, ctrl.responder);
router.get('/chat/:otroId', validarToken, ctrl.chat);
router.post('/mensaje', validarToken, ctrl.enviarMensaje);

module.exports = router;
