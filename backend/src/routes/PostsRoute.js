const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/PostsController');

router.get('/feed', validarToken, ctrl.feed);
router.get('/usuario/:userId', validarToken, ctrl.porUsuario);
router.get('/moderacion/pendientes', validarToken, ctrl.listarReportesPendientes);
router.get('/:idPost', validarToken, ctrl.detalle);
router.post('/', validarToken, ctrl.crear);
router.post('/:idPost/like', validarToken, ctrl.toggleLike);
router.post('/:idPost/comentarios', validarToken, ctrl.comentar);
router.post('/:idPost/reportar', validarToken, ctrl.reportarPost);
router.post('/comentarios/:idComentario/reportar', validarToken, ctrl.reportarComentario);
router.put('/moderacion/:idReporte', validarToken, ctrl.resolverReporte);

module.exports = router;
