const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/LogrosController');

router.get('/', validarToken, ctrl.misLogros);

module.exports = router;
