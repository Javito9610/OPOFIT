const RankingService = require('../services/RankingService');
const db = require('../config/db');

const getRanking = async (req, res) => {
  try {
    const { idOposicion } = req.params;
    const idPrueba = req.query.idPrueba ? Number(req.query.idPrueba) : null;
    const data = await RankingService.obtenerRanking(Number(idOposicion), idPrueba);
    res.json({ ok: true, data });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const getMiPosicion = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const { idOposicion } = req.params;
    const idPrueba = req.query.idPrueba ? Number(req.query.idPrueba) : null;
    if (!idPrueba) {
      return res.status(400).json({ ok: false, msg: 'Indica idPrueba' });
    }
    const pos = await RankingService.posicionUsuario(userId, Number(idOposicion), idPrueba);
    res.json({ ok: true, data: pos });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const togglePerfilPublico = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const { publico } = req.body || {};
    await db.query('UPDATE usuarios SET perfil_publico = ? WHERE id_usuario = ?', [
      publico ? 1 : 0,
      userId
    ]);
    res.json({ ok: true, perfilPublico: !!publico });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

module.exports = { getRanking, getMiPosicion, togglePerfilPublico };
