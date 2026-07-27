const { Resend } = require('resend');

// Remitente por defecto: el dominio de pruebas de Resend. Para producción con
// usuarios reales hay que verificar un dominio propio en Resend y cambiar EMAIL_FROM.
const FROM = process.env.EMAIL_FROM || 'OpoFit <onboarding@resend.dev>';

function getClient() {
  const key = process.env.RESEND_API_KEY;
  if (!key) return null;
  return new Resend(key);
}

/**
 * Envía el código de recuperación de contraseña por email.
 * Si RESEND_API_KEY no está configurada, no falla: sólo avisa por log.
 */
async function enviarCodigoRecuperacion(email, codigo) {
  const resend = getClient();
  if (!resend) {
    console.warn('[email] RESEND_API_KEY no configurada; no se envía el código de recuperación');
    return { ok: false, skipped: true };
  }
  const html = `
    <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px;background:#0A0E14;color:#F1F5F9;border-radius:16px">
      <h2 style="color:#F97316;margin:0 0 8px">OpoFit</h2>
      <p style="margin:0 0 16px;color:#97A3B6">Recuperación de contraseña</p>
      <p>Usa este código para restablecer tu contraseña. Caduca en 15 minutos:</p>
      <div style="font-size:34px;font-weight:800;letter-spacing:8px;text-align:center;background:#121722;border-radius:12px;padding:18px;margin:16px 0;color:#F97316">${codigo}</div>
      <p style="color:#97A3B6;font-size:13px">Si no has solicitado este cambio, ignora este correo. Tu contraseña seguirá siendo la misma.</p>
    </div>`;
  try {
    await resend.emails.send({
      from: FROM,
      to: email,
      subject: 'Tu código de recuperación · OpoFit',
      html
    });
    return { ok: true };
  } catch (e) {
    console.error('[email] error enviando código:', e.message);
    return { ok: false, error: e.message };
  }
}

module.exports = { enviarCodigoRecuperacion };
