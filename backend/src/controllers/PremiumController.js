const PremiumService = require('../services/PremiumService');

const estado = async (req, res) => {
  try {
    const data = await PremiumService.getEstadoPremium(req.usuario?.id);
    res.json({ ok: true, data });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const activarPrueba = async (req, res) => {
  try {
    if (process.env.PREMIUM_DEV_MODE !== 'true') {
      return res.status(403).json({
        ok: false,
        msg: 'Activación de prueba deshabilitada en producción. Usa Google Play Billing.'
      });
    }
    const dias = Number(req.body?.dias) || 30;
    const data = await PremiumService.activarPremium(req.usuario?.id, dias);
    res.json({ ok: true, data, msg: 'Premium activado (modo desarrollo)' });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

module.exports = { estado, activarPrueba };
