const GpsService = require('../services/GpsService');

const guardar = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    const r = await GpsService.guardar(userId, req.body || {});
    res.json({ ok: true, data: r });
  } catch (e) {
    console.error('Gps guardar:', e.message);
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const listar = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    const limit = Math.min(Number(req.query.limit) || 50, 200);
    const offset = Math.max(Number(req.query.offset) || 0, 0);
    const items = await GpsService.listar(userId, { limit, offset });
    res.json({ ok: true, data: items });
  } catch (e) {
    console.error('Gps listar:', e.message);
    res.status(500).json({ ok: false, msg: 'No se pudo listar las actividades GPS' });
  }
};

const detalle = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    const a = await GpsService.detalle(userId, req.params.uuid);
    if (!a) return res.status(404).json({ ok: false, msg: 'Actividad no encontrada' });
    res.json({ ok: true, data: a });
  } catch (e) {
    console.error('Gps detalle:', e.message);
    res.status(500).json({ ok: false, msg: 'No se pudo cargar la actividad' });
  }
};

const borrar = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    const ok = await GpsService.borrar(userId, req.params.uuid);
    if (!ok) return res.status(404).json({ ok: false, msg: 'Actividad no encontrada' });
    res.json({ ok: true });
  } catch (e) {
    console.error('Gps borrar:', e.message);
    res.status(500).json({ ok: false, msg: 'No se pudo borrar la actividad' });
  }
};

module.exports = { guardar, listar, detalle, borrar };
