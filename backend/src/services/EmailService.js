// Envío de emails por la API HTTP de Brevo (https://api.brevo.com).
// Usa HTTPS (no SMTP), así que NO lo bloquea Render, y con el remitente
// verificado (opofit.noreply@gmail.com) llega a CUALQUIER destinatario.
const SENDER_EMAIL = process.env.EMAIL_SENDER || process.env.GMAIL_USER || 'opofit.noreply@gmail.com';
const SENDER_NAME = 'OpoFit';

/**
 * Envía el código de recuperación de contraseña por email.
 * Si falta BREVO_API_KEY, no falla: sólo avisa por log.
 */
async function enviarCodigoRecuperacion(email, codigo) {
  const key = process.env.BREVO_API_KEY;
  if (!key) {
    console.warn('[email] BREVO_API_KEY no configurada; no se envía el código');
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
    const resp = await fetch('https://api.brevo.com/v3/smtp/email', {
      method: 'POST',
      headers: {
        'api-key': key,
        'Content-Type': 'application/json',
        'accept': 'application/json'
      },
      body: JSON.stringify({
        sender: { name: SENDER_NAME, email: SENDER_EMAIL },
        to: [{ email }],
        subject: 'Tu código de recuperación · OpoFit',
        htmlContent: html,
        textContent: `Tu código de recuperación OpoFit es: ${codigo} (caduca en 15 minutos).`
      })
    });
    if (!resp.ok) {
      const detalle = await resp.text().catch(() => '');
      console.error('[email] Brevo respondió', resp.status, detalle);
      return { ok: false, error: `HTTP ${resp.status}` };
    }
    return { ok: true };
  } catch (e) {
    console.error('[email] error enviando código:', e.message);
    return { ok: false, error: e.message };
  }
}

module.exports = { enviarCodigoRecuperacion };
