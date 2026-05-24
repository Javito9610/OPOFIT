const NotificationService = require('../services/NotificationService');

const registrarToken = async (req, res) => {
  try {
    const { fcmToken } = req.body || {};
    await NotificationService.guardarToken(req.usuario?.id, fcmToken);
    res.json({ ok: true, msg: 'Token registrado' });
  } catch (e) {
    res.status(400).json({ ok: false, msg: e.message });
  }
};

module.exports = { registrarToken };
