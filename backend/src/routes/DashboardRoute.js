const express = require('express');
const router = express.Router();
const { validarToken } = require('../middleware/authMiddleware');
const ctrl = require('../controllers/DashboardController');

router.get('/resumen', validarToken, ctrl.getResumen);

module.exports = router;
