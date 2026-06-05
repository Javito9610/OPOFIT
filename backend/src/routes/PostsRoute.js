const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/PostsController');

router.get('/feed', validarToken, ctrl.feed);
router.get('/usuario/:userId', validarToken, ctrl.porUsuario);
router.get('/:idPost', validarToken, ctrl.detalle);
router.post('/', validarToken, ctrl.crear);
router.post('/:idPost/like', validarToken, ctrl.toggleLike);
router.post('/:idPost/comentarios', validarToken, ctrl.comentar);

module.exports = router;
