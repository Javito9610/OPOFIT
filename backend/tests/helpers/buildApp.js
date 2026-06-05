const express = require('express');
const cors = require('cors');

function buildApp() {
  const app = express();
  app.use(cors());
  app.use(express.json());
  app.use('/api/auth', require('../../src/routes/AuthRoute'));
  app.use('/api/user', require('../../src/routes/UsuarioRoute'));
  app.use('/api/oposiciones', require('../../src/routes/OposicionRoute'));
  app.use('/api/rutinas', require('../../src/routes/RutinaRoute'));
  app.use('/api/rutinas-pers', require('../../src/routes/RutinaPersRoute'));
  app.use('/api/historial', require('../../src/routes/ProgresoRoute'));
  app.use('/api/info', require('../../src/routes/InfoPruebasRoute'));
  app.use('/api/ejercicios', require('../../src/routes/EjerciciosRoute'));
  app.use('/api/simulacros', require('../../src/routes/SimulacroRoute'));
  app.use('/api/ranking', require('../../src/routes/RankingRoute'));
  app.use('/api/dashboard', require('../../src/routes/DashboardRoute'));
  app.use('/api/planes', require('../../src/routes/PlanesRoute'));
  app.use('/api/gps', require('../../src/routes/GpsRoute'));
  app.use('/api/mapas', require('../../src/routes/MapasRoute'));
  app.use('/api/historial-pro', require('../../src/routes/HistorialAvanzadoRoute'));
  app.use('/api/analisis', require('../../src/routes/AnalisisRoute'));
  app.use('/api/logros', require('../../src/routes/LogrosRoute'));
  app.use('/api/amigos', require('../../src/routes/AmigosRoute'));
  app.use('/api/comunidad', require('../../src/routes/ComunidadRoute'));
  app.use('/api/premium', require('../../src/routes/PremiumRoute'));
  app.use('/api/notifications', require('../../src/routes/NotificationRoute'));
  app.use('/api/admin', require('../../src/routes/AdminRoute'));
  return app;
}

module.exports = { buildApp };
