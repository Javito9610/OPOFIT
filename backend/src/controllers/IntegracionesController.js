const jwt = require('jsonwebtoken');
const StravaService = require('../services/StravaService');
const PolarService = require('../services/PolarService');
const IntegracionesStore = require('../services/IntegracionesStore');

const JWT_SECRET = process.env.JWT_SECRET || 'opofit-dev';

function signState(payload) {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: '10m' });
}
function verifyState(token) {
  try { return jwt.verify(token, JWT_SECRET); } catch { return null; }
}

function htmlPage(ok, providerLabel) {
  const titulo = ok ? `${providerLabel} conectado` : `Error conectando ${providerLabel}`;
  const cuerpo = ok
    ? '<p>Listo. Puedes cerrar esta pestaña y volver a la app OpoFit.</p>'
    : '<p>Algo ha ido mal. Vuelve a la app e inténtalo de nuevo.</p>';
  return `<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width,initial-scale=1" />
  <title>${titulo}</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
           background: #f4f6f9; color: #102a43; padding: 32px; text-align: center; }
    .card { background: white; border-radius: 14px; padding: 24px; max-width: 420px;
            margin: 32px auto; box-shadow: 0 8px 18px rgba(0,0,0,0.06); }
    .ok { color: #1565C0; font-size: 22px; font-weight: 700; }
    .ko { color: #c62828; font-size: 22px; font-weight: 700; }
  </style>
</head>
<body>
  <div class="card">
    <div class="${ok ? 'ok' : 'ko'}">${titulo}</div>
    ${cuerpo}
  </div>
</body>
</html>`;
}

const estado = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    const items = await IntegracionesStore.listEstado(userId);
    res.json({
      ok: true,
      data: {
        proveedores: items,
        stravaConfigured: StravaService.isConfigured(),
        polarConfigured: PolarService.isConfigured()
      }
    });
  } catch (e) {
    res.status(500).json({ ok: false, msg: 'No se pudo cargar el estado de integraciones' });
  }
};

const stravaStart = async (req, res) => {
  const userId = req.usuario?.id;
  if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
  if (!StravaService.isConfigured()) {
    return res.status(503).json({ ok: false, msg: 'Strava no está configurado en el servidor' });
  }
  const state = signState({ userId, provider: 'STRAVA' });
  const url = StravaService.buildAuthorizeUrl(userId);
  const finalUrl = url.replace(/state=[^&]+/, `state=${encodeURIComponent(state)}`);
  res.redirect(finalUrl);
};

const stravaCallback = async (req, res) => {
  const { code, state, error } = req.query || {};
  if (error || !code || !state) return res.status(400).send(htmlPage(false, 'Strava'));
  const payload = verifyState(String(state));
  if (!payload || payload.provider !== 'STRAVA') return res.status(400).send(htmlPage(false, 'Strava'));
  try {
    await StravaService.handleCallback(payload.userId, String(code));
    res.send(htmlPage(true, 'Strava'));
  } catch (e) {
    console.error('Strava callback:', e.message);
    res.status(500).send(htmlPage(false, 'Strava'));
  }
};

const stravaSync = async (req, res) => {
  const userId = req.usuario?.id;
  if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
  try {
    const r = await StravaService.syncActivities(userId);
    res.json({ ok: true, data: r });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message || 'Error sync Strava' });
  }
};

const stravaDisconnect = async (req, res) => {
  const userId = req.usuario?.id;
  if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
  await StravaService.disconnect(userId);
  res.json({ ok: true });
};

const polarStart = async (req, res) => {
  const userId = req.usuario?.id;
  if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
  if (!PolarService.isConfigured()) {
    return res.status(503).json({ ok: false, msg: 'Polar no está configurado en el servidor' });
  }
  const state = signState({ userId, provider: 'POLAR' });
  const url = PolarService.buildAuthorizeUrl(userId);
  const finalUrl = url.replace(/state=[^&]+/, `state=${encodeURIComponent(state)}`);
  res.redirect(finalUrl);
};

const polarCallback = async (req, res) => {
  const { code, state, error } = req.query || {};
  if (error || !code || !state) return res.status(400).send(htmlPage(false, 'Polar'));
  const payload = verifyState(String(state));
  if (!payload || payload.provider !== 'POLAR') return res.status(400).send(htmlPage(false, 'Polar'));
  try {
    await PolarService.handleCallback(payload.userId, String(code));
    res.send(htmlPage(true, 'Polar'));
  } catch (e) {
    console.error('Polar callback:', e.message);
    res.status(500).send(htmlPage(false, 'Polar'));
  }
};

const polarSync = async (req, res) => {
  const userId = req.usuario?.id;
  if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
  try {
    const r = await PolarService.syncActivities(userId);
    res.json({ ok: true, data: r });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message || 'Error sync Polar' });
  }
};

const polarDisconnect = async (req, res) => {
  const userId = req.usuario?.id;
  if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
  await PolarService.disconnect(userId);
  res.json({ ok: true });
};

/** Importa actividades desde Health Connect (subidas por la app). */
const importarHealthConnect = async (req, res) => {
  const userId = req.usuario?.id;
  if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
  const db = require('../config/db');
  const items = Array.isArray(req.body?.actividades) ? req.body.actividades : [];
  let importadas = 0;
  let saltadas = 0;
  for (const a of items) {
    const externalId = String(a.externalId || a.id || '');
    if (!externalId) continue;
    const [exists] = await db.query(
      `SELECT id_actividad FROM gps_actividades
       WHERE usuarios_id_usuario = ? AND external_id = ? AND origen = 'HEALTH_CONNECT' LIMIT 1`,
      [userId, externalId]
    );
    if (exists.length) { saltadas += 1; continue; }
    await db.query(
      `INSERT INTO gps_actividades
        (uuid_local, usuarios_id_usuario, tipo,
         iniciada_en, finalizada_en, duracion_seg, movimiento_seg,
         distancia_m, velocidad_media_mps, velocidad_max_mps,
         ritmo_medio_spkm, ritmo_min_spkm, ritmo_max_spkm,
         desnivel_pos_m, altitud_min_m, altitud_max_m,
         cadencia_media_ppm, polyline_json, splits_json,
         origen, external_id)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'HEALTH_CONNECT', ?)`,
      [
        `hc_${externalId}`,
        userId,
        a.tipo || 'RUN',
        Number(a.startedAtMs || Date.now()),
        Number(a.endedAtMs || Date.now()),
        Number(a.durationSec || 0),
        Number(a.movingSec || a.durationSec || 0),
        Number(a.distanceM || 0),
        Number(a.avgSpeedMps || 0),
        Number(a.maxSpeedMps || a.avgSpeedMps || 0),
        Number(a.avgPaceSecPerKm || 0),
        Number(a.minPaceSecPerKm || a.avgPaceSecPerKm || 0),
        Number(a.maxPaceSecPerKm || a.avgPaceSecPerKm || 0),
        Number(a.elevationGainM || 0),
        a.elevationMinM == null ? null : Number(a.elevationMinM),
        a.elevationMaxM == null ? null : Number(a.elevationMaxM),
        a.avgCadenceSpm == null ? null : Number(a.avgCadenceSpm),
        null,
        null,
        externalId
      ]
    );
    importadas += 1;
  }
  res.json({ ok: true, data: { importadas, saltadas } });
};

module.exports = {
  estado,
  stravaStart,
  stravaCallback,
  stravaSync,
  stravaDisconnect,
  polarStart,
  polarCallback,
  polarSync,
  polarDisconnect,
  importarHealthConnect
};
