const InAppNotifService = require('../services/InAppNotifService');

const listar = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const soloNoLeidas = req.query?.solo_no_leidas === 'true';
    const limite = Number(req.query?.limite) || 50;
    const data = await InAppNotifService.listar(userId, { soloNoLeidas, limite });
    res.json({ ok: true, data });
  } catch (e) {
    console.error('InAppNotif listar:', e.message);
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const contadorNoLeidas = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const total = await InAppNotifService.contarNoLeidas(userId);
    res.json({ ok: true, data: { total } });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const marcarLeida = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    await InAppNotifService.marcarLeida(userId, Number(req.params.id));
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const marcarTodasLeidas = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    await InAppNotifService.marcarTodasLeidas(userId);
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const eliminar = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    await InAppNotifService.eliminar(userId, Number(req.params.id));
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

module.exports = { listar, contadorNoLeidas, marcarLeida, marcarTodasLeidas, eliminar };
