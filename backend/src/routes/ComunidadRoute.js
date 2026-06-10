const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/ComunidadController');

router.get('/grupos', validarToken, ctrl.listarGrupos);
router.post('/grupos', validarToken, ctrl.crearGrupo);
router.delete('/grupos/:idGrupo', validarToken, ctrl.eliminarGrupo);
router.post('/grupos/:idGrupo/unirse', validarToken, ctrl.unirseGrupo);
router.post('/grupos/:idGrupo/salir', validarToken, ctrl.salirGrupo);
router.post('/grupos/:idGrupo/invitar', validarToken, ctrl.invitarAmigoAGrupo);
router.get('/grupos/:idGrupo/mensajes', validarToken, ctrl.mensajesGrupo);
router.post('/grupos/:idGrupo/mensajes', validarToken, ctrl.enviarMensajeGrupo);
router.post('/grupos/:idGrupo/quedadas', validarToken, ctrl.crearQuedada);
router.get('/grupos/:idGrupo/quedadas', validarToken, ctrl.listarQuedadas);
router.put('/ubicacion', validarToken, ctrl.actualizarUbicacion);
router.get('/cerca', validarToken, ctrl.listarCerca);

module.exports = router;
