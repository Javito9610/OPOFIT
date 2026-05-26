const db = require('../config/db');
const IntegracionesStore = require('./IntegracionesStore');

const STRAVA_AUTH = 'https://www.strava.com/oauth/authorize';
const STRAVA_TOKEN = 'https://www.strava.com/api/v3/oauth/token';
const STRAVA_API = 'https://www.strava.com/api/v3';

function clientId() {
  return process.env.STRAVA_CLIENT_ID || '';
}
function clientSecret() {
  return process.env.STRAVA_CLIENT_SECRET || '';
}
function baseUrl() {
  return process.env.OAUTH_BASE_URL || 'http://localhost:3000';
}
function callbackUrl() {
  return `${baseUrl().replace(/\/$/, '')}/api/integraciones/strava/callback`;
}

class StravaService {
  static isConfigured() {
    return clientId() !== '' && clientSecret() !== '';
  }

  /** URL a la que abrir el navegador para empezar el OAuth. */
  static buildAuthorizeUrl(userId) {
    const params = new URLSearchParams({
      client_id: clientId(),
      response_type: 'code',
      redirect_uri: callbackUrl(),
      approval_prompt: 'auto',
      scope: 'read,activity:read_all,profile:read_all',
      state: String(userId)
    });
    return `${STRAVA_AUTH}?${params.toString()}`;
  }

  static async exchangeCode(code) {
    const body = new URLSearchParams({
      client_id: clientId(),
      client_secret: clientSecret(),
      code,
      grant_type: 'authorization_code'
    });
    const r = await fetch(STRAVA_TOKEN, { method: 'POST', body });
    if (!r.ok) throw new Error(`Strava token exchange ${r.status}`);
    return r.json();
  }

  static async refresh(refreshToken) {
    const body = new URLSearchParams({
      client_id: clientId(),
      client_secret: clientSecret(),
      grant_type: 'refresh_token',
      refresh_token: refreshToken
    });
    const r = await fetch(STRAVA_TOKEN, { method: 'POST', body });
    if (!r.ok) throw new Error(`Strava refresh ${r.status}`);
    return r.json();
  }

  static async handleCallback(userId, code) {
    const tok = await StravaService.exchangeCode(code);
    await IntegracionesStore.upsert(userId, 'STRAVA', {
      externalUserId: String(tok.athlete?.id || ''),
      accessToken: tok.access_token,
      refreshToken: tok.refresh_token,
      expiresAt: tok.expires_at ? Number(tok.expires_at) * 1000 : null,
      scope: 'read,activity:read_all,profile:read_all'
    });
  }

  static async ensureAccessToken(userId) {
    const row = await IntegracionesStore.get(userId, 'STRAVA');
    if (!row) throw new Error('Strava no conectado');
    const now = Date.now();
    if (row.expires_at && Number(row.expires_at) - 60_000 > now) {
      return row.access_token;
    }
    if (!row.refresh_token) return row.access_token;
    const tok = await StravaService.refresh(row.refresh_token);
    await IntegracionesStore.upsert(userId, 'STRAVA', {
      externalUserId: row.external_user_id,
      accessToken: tok.access_token,
      refreshToken: tok.refresh_token || row.refresh_token,
      expiresAt: tok.expires_at ? Number(tok.expires_at) * 1000 : null,
      scope: row.scope
    });
    return tok.access_token;
  }

  static stravaTypeToOpofit(type) {
    const t = String(type || '').toLowerCase();
    if (t.includes('ride') || t.includes('cycling') || t.includes('bike')) return 'BIKE';
    if (t.includes('walk') || t.includes('hike')) return 'WALK';
    return 'RUN';
  }

  /** Descarga las últimas N actividades y las inserta como GPS. */
  static async syncActivities(userId, { perPage = 30 } = {}) {
    const access = await StravaService.ensureAccessToken(userId);
    const r = await fetch(`${STRAVA_API}/athlete/activities?per_page=${perPage}`, {
      headers: { Authorization: `Bearer ${access}` }
    });
    if (!r.ok) throw new Error(`Strava activities ${r.status}`);
    const list = await r.json();
    let importadas = 0;
    let saltadas = 0;
    for (const a of list) {
      const existingId = `strava_${a.id}`;
      const [exists] = await db.query(
        `SELECT id_actividad FROM gps_actividades
         WHERE usuarios_id_usuario = ? AND external_id = ? AND origen = 'STRAVA' LIMIT 1`,
        [userId, String(a.id)]
      );
      if (exists.length) { saltadas += 1; continue; }
      const inicio = new Date(a.start_date || Date.now()).getTime();
      const fin = inicio + (Number(a.elapsed_time) || 0) * 1000;
      const tipo = StravaService.stravaTypeToOpofit(a.type);
      const avgSpeed = Number(a.average_speed) || 0;
      const maxSpeed = Number(a.max_speed) || avgSpeed;
      const dist = Number(a.distance) || 0;
      const dur = Number(a.elapsed_time) || 0;
      const moving = Number(a.moving_time) || dur;
      const pace = avgSpeed > 0 ? 1 / avgSpeed * 1000 : 0;
      await db.query(
        `INSERT INTO gps_actividades
          (uuid_local, usuarios_id_usuario, tipo,
           iniciada_en, finalizada_en, duracion_seg, movimiento_seg,
           distancia_m, velocidad_media_mps, velocidad_max_mps,
           ritmo_medio_spkm, ritmo_min_spkm, ritmo_max_spkm,
           desnivel_pos_m, altitud_min_m, altitud_max_m,
           cadencia_media_ppm, polyline_json, splits_json,
           origen, external_id)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'STRAVA', ?)`,
        [
          existingId,
          userId,
          tipo,
          inicio,
          fin,
          dur,
          moving,
          dist,
          avgSpeed,
          maxSpeed,
          pace,
          pace,
          pace,
          Number(a.total_elevation_gain) || 0,
          a.elev_low == null ? null : Number(a.elev_low),
          a.elev_high == null ? null : Number(a.elev_high),
          a.average_cadence == null ? null : Number(a.average_cadence),
          a.map?.summary_polyline ? JSON.stringify({ polyline: a.map.summary_polyline }) : null,
          null,
          String(a.id)
        ]
      );
      importadas += 1;
    }
    await IntegracionesStore.touchSync(userId, 'STRAVA');
    return { importadas, saltadas };
  }

  static async disconnect(userId) {
    await IntegracionesStore.remove(userId, 'STRAVA');
  }
}

module.exports = StravaService;
