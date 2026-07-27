const nodemailer = require('nodemailer');

// Envío por Gmail SMTP con una cuenta dedicada (opofit.noreply@gmail.com) y su
// contraseña de aplicación. Llega a CUALQUIER destinatario, gratis, sin dominio.
const FROM = process.env.EMAIL_FROM || 'OpoFit <opofit.noreply@gmail.com>';

function getTransport() {
  const user = process.env.GMAIL_USER;
  const pass = process.env.GMAIL_APP_PASSWORD;
  if (!user || !pass) return null;
  return nodemailer.createTransport({
    service: 'gmail',
    auth: {
      user,
      // La app password de Google se muestra con espacios; los quitamos.
      pass: String(pass).replace(/\s+/g, '')
    }
  });
}

/**
 * Envía el código de recuperación de contraseña por email.
 * Si faltan las credenciales, no falla: sólo avisa por log.
 */
async function enviarCodigoRecuperacion(email, codigo) {
  const transport = getTransport();
  if (!transport) {
    console.warn('[email] GMAIL_USER/GMAIL_APP_PASSWORD no configuradas; no se envía el código');
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
    await transport.sendMail({
      from: FROM,
      to: email,
      subject: 'Tu código de recuperación · OpoFit',
      html,
      text: `Tu código de recuperación OpoFit es: ${codigo} (caduca en 15 minutos).`
    });
    return { ok: true };
  } catch (e) {
    console.error('[email] error enviando código:', e.message);
    return { ok: false, error: e.message };
  }
}

module.exports = { enviarCodigoRecuperacion };
