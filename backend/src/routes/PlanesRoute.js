const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const { getCalendario } = require('../controllers/PlanesController');

router.get('/calendario/:idOposicion', validarToken, getCalendario);

module.exports = router;
