const db = require('../config/db');
const IntegracionesStore = require('./IntegracionesStore');

const POLAR_AUTH = 'https://flow.polar.com/oauth2/authorization';
const POLAR_TOKEN = 'https://polarremote.com/v2/oauth2/token';
const POLAR_API = 'https://www.polaraccesslink.com/v3';

function clientId() {
  return process.env.POLAR_CLIENT_ID || '';
}
function clientSecret() {
  return process.env.POLAR_CLIENT_SECRET || '';
}
function baseUrl() {
  return process.env.OAUTH_BASE_URL || 'http://localhost:3000';
}
function callbackUrl() {
  return `${baseUrl().replace(/\/$/, '')}/api/integraciones/polar/callback`;
}

class PolarService {
  static isConfigured() {
    return clientId() !== '' && clientSecret() !== '';
  }

  static buildAuthorizeUrl(userId) {
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: clientId(),
      redirect_uri: callbackUrl(),
      scope: 'accesslink.read_all',
      state: String(userId)
    });
    return `${POLAR_AUTH}?${params.toString()}`;
  }

  static async exchangeCode(code) {
    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      code,
      redirect_uri: callbackUrl()
    });
    const basic = Buffer.from(`${clientId()}:${clientSecret()}`).toString('base64');
    const r = await fetch(POLAR_TOKEN, {
      method: 'POST',
      headers: {
        Authorization: `Basic ${basic}`,
        Accept: 'application/json;charset=UTF-8'
      },
      body
    });
    if (!r.ok) throw new Error(`Polar token ${r.status}`);
    return r.json();
  }

  static async registerUser(accessToken, polarUserId) {
    const r = await fetch(`${POLAR_API}/users`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
        Accept: 'application/json'
      },
      body: JSON.stringify({ 'member-id': `opofit_${polarUserId}` })
    });
    if (r.ok || r.status === 409) return;
    throw new Error(`Polar register user ${r.status}`);
  }

  static async handleCallback(userId, code) {
    const tok = await PolarService.exchangeCode(code);
    const expiresAt = tok.expires_in ? Date.now() + Number(tok.expires_in) * 1000 : null;
    try {
      await PolarService.registerUser(tok.access_token, tok.x_user_id);
    } catch (e) {
      console.warn('Polar registerUser:', e.message);
    }
    await IntegracionesStore.upsert(userId, 'POLAR', {
      externalUserId: String(tok.x_user_id || ''),
      accessToken: tok.access_token,
      refreshToken: null,
      expiresAt,
      scope: 'accesslink.read_all'
    });
  }

  static async syncActivities(userId) {
    const row = await IntegracionesStore.get(userId, 'POLAR');
    if (!row) throw new Error('Polar no conectado');
    const accessToken = row.access_token;
    const polarUserId = row.external_user_id;
    const headers = {
      Authorization: `Bearer ${accessToken}`,
      Accept: 'application/json'
    };
    const txResp = await fetch(
      `${POLAR_API}/users/${polarUserId}/exercise-transactions`,
      { method: 'POST', headers }
    );
    if (txResp.status === 204) {
      await IntegracionesStore.touchSync(userId, 'POLAR');
      return { importadas: 0, saltadas: 0 };
    }
    if (!txResp.ok) throw new Error(`Polar transaction ${txResp.status}`);
    const tx = await txResp.json();
    const transactionUrl = tx['resource-uri'] || tx.resource_uri;
    const listResp = await fetch(transactionUrl, { headers });
    if (!listResp.ok) throw new Error(`Polar list ${listResp.status}`);
    const list = await listResp.json();
    const exercises = list.exercises || [];
    let importadas = 0;
    let saltadas = 0;
    for (const url of exercises) {
      const exResp = await fetch(url, { headers });
      if (!exResp.ok) continue;
      const ex = await exResp.json();
      const [exists] = await db.query(
        `SELECT id_actividad FROM gps_actividades
         WHERE usuarios_id_usuario = ? AND external_id = ? AND origen = 'POLAR' LIMIT 1`,
        [userId, String(ex.id)]
      );
      if (exists.length) { saltadas += 1; continue; }
      const inicio = new Date(ex.start_time || Date.now()).getTime();
      const durSec = polarDurationToSec(ex.duration);
      const fin = inicio + durSec * 1000;
      const tipo = polarSportToOpofit(ex.sport);
      const dist = Number(ex.distance) || 0;
      const avgSpeed = durSec > 0 ? dist / durSec : 0;
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
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'POLAR', ?)`,
        [
          `polar_${ex.id}`,
          userId,
          tipo,
          inicio,
          fin,
          durSec,
          durSec,
          dist,
          avgSpeed,
          avgSpeed,
          pace,
          pace,
          pace,
          0,
          null,
          null,
          ex['heart-rate']?.average || null,
          null,
          null,
          String(ex.id)
        ]
      );
      importadas += 1;
    }
    // Commit transaction
    try {
      await fetch(transactionUrl, { method: 'PUT', headers });
    } catch (_) { /* noop */ }
    await IntegracionesStore.touchSync(userId, 'POLAR');
    return { importadas, saltadas };
  }

  static async disconnect(userId) {
    await IntegracionesStore.remove(userId, 'POLAR');
  }
}

function polarDurationToSec(iso) {
  if (!iso) return 0;
  // Format ISO 8601 PT#H#M#S
  const m = /PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/.exec(iso);
  if (!m) return 0;
  return (Number(m[1] || 0) * 3600) + (Number(m[2] || 0) * 60) + Math.floor(Number(m[3] || 0));
}

function polarSportToOpofit(sport) {
  const s = String(sport || '').toLowerCase();
  if (s.includes('cycl') || s.includes('bike')) return 'BIKE';
  if (s.includes('walk') || s.includes('hik')) return 'WALK';
  return 'RUN';
}

module.exports = PolarService;
