const db = require('../config/db');
const { initFirebaseAdmin } = require('../config/firebaseAdmin');

class NotificationService {
  static async guardarToken(userId, fcmToken) {
    if (!fcmToken) throw new Error('Token FCM vacío');
    await db.query('UPDATE usuarios SET fcm_token = ? WHERE id_usuario = ?', [
      fcmToken,
      userId
    ]);
    return true;
  }

  static async enviarAToken(token, titulo, cuerpo, data = {}) {
    try {
      const admin = initFirebaseAdmin();
      await admin.messaging().send({
        token,
        notification: { title: titulo, body: cuerpo },
        data: Object.fromEntries(
          Object.entries(data).map(([k, v]) => [k, String(v)])
        )
      });
      return { ok: true };
    } catch (e) {
      return { ok: false, error: e.message };
    }
  }

  static async enviarRecordatorioEntreno() {
    const [usuarios] = await db.query(
      `SELECT id_usuario, fcm_token, nombre FROM usuarios
       WHERE fcm_token IS NOT NULL AND fcm_token != ''`
    );
    let enviados = 0;
    for (const u of usuarios || []) {
      const r = await NotificationService.enviarAToken(
        u.fcm_token,
        'OpoFit — Hora de entrenar',
        `¡${u.nombre || 'Aspirante'}, tu oposición no espera! Registra el entreno de hoy.`,
        { tipo: 'recordatorio_entreno' }
      );
      if (r.ok) enviados++;
    }
    return { enviados, total: usuarios?.length || 0 };
  }

  static async enviarNoticiaOposicion(idOposicion, titulo, cuerpo) {
    const [usuarios] = await db.query(
      `SELECT fcm_token FROM usuarios
       WHERE fcm_token IS NOT NULL AND fcm_token != ''
         AND oposiciones_id_oposicion = ?`,
      [idOposicion]
    );
    let enviados = 0;
    for (const u of usuarios || []) {
      const r = await NotificationService.enviarAToken(
        u.fcm_token,
        titulo,
        cuerpo,
        { tipo: 'noticia_oposicion', idOposicion: String(idOposicion) }
      );
      if (r.ok) enviados++;
    }
    return { enviados };
  }
}

module.exports = NotificationService;
