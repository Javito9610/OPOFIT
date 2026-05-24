const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/NotificationController');

router.post('/token', validarToken, ctrl.registrarToken);

module.exports = router;
