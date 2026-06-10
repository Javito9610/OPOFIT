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

  /**
   * Envía push a un único usuario por su id. Lookup del token + delivery.
   * Usado por: solicitud de amistad, aceptar amistad, invitación a grupo,
   * mensaje directo en chat 1-a-1, etc.
   */
  static async enviarAUsuario(idUsuario, titulo, cuerpo, data = {}) {
    const [rows] = await db.query(
      'SELECT fcm_token FROM usuarios WHERE id_usuario = ? AND fcm_token IS NOT NULL AND fcm_token != ""',
      [idUsuario]
    );
    const token = rows?.[0]?.fcm_token;
    if (!token) return { ok: false, error: 'sin_token' };
    return NotificationService.enviarAToken(token, titulo, cuerpo, data);
  }

  /**
   * Notifica un nuevo mensaje a todos los miembros del grupo excepto al autor.
   * Reloj-friendly: canal "comunidad_messages" en el cliente para que vibre
   * con prioridad HIGH y se replique al wearable.
   */
  static async notificarMensajeGrupo({ idGrupo, idAutor, nombreAutor, nombreGrupo, texto }) {
    const [rows] = await db.query(
      `SELECT u.id_usuario, u.fcm_token
         FROM grupo_miembros gm
         JOIN usuarios u ON u.id_usuario = gm.id_usuario
        WHERE gm.id_grupo = ?
          AND gm.id_usuario != ?
          AND u.fcm_token IS NOT NULL AND u.fcm_token != ""`,
      [idGrupo, idAutor]
    );
    const previewTexto = String(texto || '').slice(0, 120);
    let enviados = 0;
    for (const u of rows || []) {
      const r = await NotificationService.enviarAToken(
        u.fcm_token,
        `${nombreAutor} · ${nombreGrupo}`,
        previewTexto,
        {
          tipo: 'mensaje_grupo',
          canal: 'comunidad_messages',
          idGrupo: String(idGrupo)
        }
      );
      if (r.ok) enviados += 1;
    }
    return { enviados, total: rows?.length || 0 };
  }

  /** Notifica al destinatario una nueva solicitud de amistad. */
  static async notificarSolicitudAmistad({ idDestino, nombreSolicitante }) {
    return NotificationService.enviarAUsuario(
      idDestino,
      'Nueva solicitud de amistad',
      `${nombreSolicitante} quiere ser tu amigo en OpoFit`,
      { tipo: 'solicitud_amistad', canal: 'comunidad_messages' }
    );
  }

  /** Notifica al solicitante que su solicitud fue aceptada. */
  static async notificarAmistadAceptada({ idSolicitante, nombreAceptante }) {
    return NotificationService.enviarAUsuario(
      idSolicitante,
      '¡Tienes un nuevo amigo!',
      `${nombreAceptante} ha aceptado tu solicitud`,
      { tipo: 'amistad_aceptada', canal: 'comunidad_messages' }
    );
  }

  /** Notifica al invitado que ha sido añadido a un grupo privado. */
  static async notificarInvitacionGrupo({ idDestino, nombreGrupo, nombreInvitador }) {
    return NotificationService.enviarAUsuario(
      idDestino,
      `Te han invitado a "${nombreGrupo}"`,
      `${nombreInvitador} te ha añadido al grupo`,
      { tipo: 'invitacion_grupo', canal: 'comunidad_messages' }
    );
  }

  /** Notifica al destinatario de un nuevo mensaje 1-a-1. */
  static async notificarMensajeDirecto({ idDestino, nombreRemitente, texto }) {
    return NotificationService.enviarAUsuario(
      idDestino,
      nombreRemitente,
      String(texto || '').slice(0, 120),
      { tipo: 'mensaje_directo', canal: 'comunidad_messages' }
    );
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
