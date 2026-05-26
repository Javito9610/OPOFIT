const db = require('../config/db');

/**
 * Acceso unificado a la tabla integraciones_oauth.
 */
class IntegracionesStore {
  static async get(userId, provider) {
    const [rows] = await db.query(
      `SELECT * FROM integraciones_oauth WHERE usuarios_id_usuario = ? AND provider = ? LIMIT 1`,
      [userId, provider]
    );
    return rows[0] || null;
  }

  static async upsert(userId, provider, data) {
    const {
      externalUserId = null,
      accessToken,
      refreshToken = null,
      expiresAt = null,
      scope = null
    } = data;
    await db.query(
      `INSERT INTO integraciones_oauth
         (usuarios_id_usuario, provider, external_user_id, access_token, refresh_token, expires_at, scope)
       VALUES (?, ?, ?, ?, ?, ?, ?)
       ON DUPLICATE KEY UPDATE
         external_user_id = VALUES(external_user_id),
         access_token = VALUES(access_token),
         refresh_token = VALUES(refresh_token),
         expires_at = VALUES(expires_at),
         scope = VALUES(scope),
         updated_at = CURRENT_TIMESTAMP`,
      [userId, provider, externalUserId, accessToken, refreshToken, expiresAt, scope]
    );
  }

  static async touchSync(userId, provider) {
    await db.query(
      `UPDATE integraciones_oauth SET last_sync_at = CURRENT_TIMESTAMP
       WHERE usuarios_id_usuario = ? AND provider = ?`,
      [userId, provider]
    );
  }

  static async remove(userId, provider) {
    await db.query(
      'DELETE FROM integraciones_oauth WHERE usuarios_id_usuario = ? AND provider = ?',
      [userId, provider]
    );
  }

  static async listEstado(userId) {
    const [rows] = await db.query(
      `SELECT provider, external_user_id, last_sync_at, expires_at
       FROM integraciones_oauth WHERE usuarios_id_usuario = ?`,
      [userId]
    );
    return rows.map((r) => ({
      provider: r.provider,
      externalUserId: r.external_user_id,
      lastSyncAt: r.last_sync_at,
      expiresAt: r.expires_at ? Number(r.expires_at) : null,
      conectado: true
    }));
  }
}

module.exports = IntegracionesStore;
