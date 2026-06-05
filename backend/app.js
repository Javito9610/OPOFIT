require('dotenv').config();
const express = require('express');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const app = express();
const port = process.env.PORT || 3000;

// Railway (y otros proxies) envían X-Forwarded-For; express-rate-limit lo exige con trust proxy.
app.set('trust proxy', 1);

const globalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  message: {
    ok: false,
    msg: 'Demasiadas peticiones. Inténtalo de nuevo más tarde.'
  }
});
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  message: {
    ok: false,
    msg: 'Demasiados intentos de autenticación. Espera unos minutos.'
  }
});
app.use(cors());
app.use(express.json({ limit: '3mb' }));
app.use(express.static('public'));
app.use('/uploads', express.static(require('path').join(__dirname, 'uploads')));
app.use(globalLimiter);
app.use('/api/auth', authLimiter, require('./src/routes/AuthRoute'));
app.use('/api/user', require('./src/routes/UsuarioRoute'));
app.use('/api/oposiciones', require('./src/routes/OposicionRoute'));
app.use('/api/rutinas', require('./src/routes/RutinaRoute'));
app.use('/api/rutinas-pers', require('./src/routes/RutinaPersRoute'));
app.use('/api/historial', require('./src/routes/ProgresoRoute'));
app.use('/api/info', require('./src/routes/InfoPruebasRoute'));
app.use('/api/ejercicios', require('./src/routes/EjerciciosRoute'));
app.use('/api/simulacros', require('./src/routes/SimulacroRoute'));
app.use('/api/ranking', require('./src/routes/RankingRoute'));
app.use('/api/dashboard', require('./src/routes/DashboardRoute'));
app.use('/api/planes', require('./src/routes/PlanesRoute'));
app.use('/api/gps', require('./src/routes/GpsRoute'));
app.use('/api/mapas', require('./src/routes/MapasRoute'));
app.use('/api/historial-pro', require('./src/routes/HistorialAvanzadoRoute'));
app.use('/api/analisis', require('./src/routes/AnalisisRoute'));
app.use('/api/logros', require('./src/routes/LogrosRoute'));
app.use('/api/integraciones', require('./src/routes/IntegracionesRoute'));
app.use('/api/amigos', require('./src/routes/AmigosRoute'));
app.use('/api/comunidad', require('./src/routes/ComunidadRoute'));
app.use('/api/premium', require('./src/routes/PremiumRoute'));
app.use('/api/notifications', require('./src/routes/NotificationRoute'));
app.use('/api/admin', require('./src/routes/AdminRoute'));

if (process.env.NOTIFICATIONS_CRON === 'true') {
  const cron = require('node-cron');
  const NotificationService = require('./src/services/NotificationService');
  cron.schedule('0 * * * *', () => {
    NotificationService.enviarRecordatorioEntreno()
      .then((r) => console.log('[cron] Recordatorios:', r))
      .catch((e) => console.error('[cron] Error:', e.message));
  });
  const RssService = require('./src/services/RssService');
  cron.schedule('0 */6 * * *', () => {
    RssService.pollYNotificarAlertas()
      .then((r) => console.log('[cron] Alertas RSS:', r))
      .catch((e) => console.error('[cron] RSS:', e.message));
  });
  console.log('Cron de recordatorios activo (cada hora, hora preferida del usuario)');
  console.log('Cron alertas RSS activo (cada 6 horas)');
}

const DbMigrationService = require('./src/services/DbMigrationService');

async function start() {
  app.listen(port, () => {
    console.log(`Servidor ejecutandose en http://localhost:${port}`);
  });
  DbMigrationService.runOnStartup()
    .then((r) => {
      if (r?.banco?.error === 'JSON_NOT_FOUND') {
        console.error('[startup] Banco de planes: JSON no encontrado en el despliegue');
      } else if (r?.banco && !r.banco.skipped) {
        console.log('[startup] Banco de planes importado:', r.banco.dias, 'días');
      }
    })
    .catch((e) => {
      console.error('No se pudo aplicar migraciones de BD:', e.message);
    });
}

start();
