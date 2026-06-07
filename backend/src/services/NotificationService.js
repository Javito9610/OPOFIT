const db = require('../config/db');
const { initFirebaseAdmin } = require('../config/firebaseAdmin');
const PlanesService = require('./PlanesService');
const RutinaService = require('./RutinasService');
const PremiumService = require('./PremiumService');

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
    // Bug previo: `new Date().getHours()` devuelve la hora del SERVIDOR.
    // En Railway corre en UTC → un usuario que pidió recordatorio a las 18:00 (Madrid)
    // recibía el push a las 18:00 UTC (20:00 Madrid en verano). Calculamos la hora
    // en la zona horaria de España. (Override con TZ_USUARIOS si los usuarios están en otra zona).
    const tz = process.env.TZ_USUARIOS || 'Europe/Madrid';
    const horaActual = parseInt(
      new Intl.DateTimeFormat('es-ES', { timeZone: tz, hour: '2-digit', hour12: false }).format(new Date()),
      10
    );
    const [usuarios] = await db.query(
      `SELECT id_usuario, fcm_token, nombre, oposiciones_id_oposicion,
              COALESCE(hora_recordatorio_entreno, '18:00:00') AS hora_rec,
              COALESCE(recordatorio_entreno_activo, 1) AS rec_activo
       FROM usuarios
       WHERE fcm_token IS NOT NULL AND fcm_token != ''
         AND COALESCE(recordatorio_entreno_activo, 1) = 1
         AND oposiciones_id_oposicion IS NOT NULL`
    );

    let enviados = 0;
    for (const u of usuarios || []) {
      const horaUser = parseInt(String(u.hora_rec).split(':')[0], 10);
      if (horaUser !== horaActual) continue;

      let titulo = 'OpoFit — Hora de entrenar';
      let cuerpo = `¡${u.nombre || 'Aspirante'}, toca mover el cuerpo! Abre el plan y registra tu sesión.`;

      try {
        const calc = await RutinaService.calcularNotaYNivel(
          u.id_usuario,
          u.oposiciones_id_oposicion
        );
        if ((calc.pruebasFaltantes ?? 0) === 0 && calc.nivelSugerido) {
          const premium = await PremiumService.getEstadoPremium(u.id_usuario);
          const nivel =
            !premium.esPremium && calc.nivelSugerido !== 'BASICO'
              ? 'BASICO'
              : calc.nivelSugerido;
          const plan = await PlanesService.obtenerPlanSemanal(
            u.id_usuario,
            u.oposiciones_id_oposicion,
            nivel,
            calc.genero
          );
          const sesion = plan?.sesion_hoy || plan?.proxima_sesion;
          if (sesion) {
            if (sesion.completada) continue;
            const pilar =
              sesion.enfoque === 'FUERZA'
                ? '💪 Fuerza'
                : sesion.enfoque === 'RESISTENCIA'
                  ? '🏃 Resistencia'
                  : '⚡ Velocidad';
            titulo = `Hoy toca ${pilar}`;
            cuerpo = `${sesion.nombre_dia}: ${sesion.titulo?.slice(0, 80) || sesion.enfoque}. ¡A por ello!`;
          }
        }
      } catch (_e) {
        /* mensaje genérico */
      }

      const r = await NotificationService.enviarAToken(
        u.fcm_token,
        titulo,
        cuerpo,
        { tipo: 'recordatorio_entreno', idOposicion: String(u.oposiciones_id_oposicion) }
      );
      if (r.ok) enviados++;
    }
    return { enviados, total: usuarios?.length || 0, hora: horaActual };
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
