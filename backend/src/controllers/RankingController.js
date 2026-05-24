const RankingService = require('../services/RankingService');
const db = require('../config/db');

const getRanking = async (req, res) => {
  try {
    const { idOposicion } = req.params;
    const data = await RankingService.obtenerRanking(Number(idOposicion));
    res.json({ ok: true, data });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const getMiPosicion = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const { idOposicion } = req.params;
    const pos = await RankingService.posicionUsuario(userId, Number(idOposicion));
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

const getDetalleUsuario = async (req, res) => {
  try {
    const { idOposicion, userId } = req.params;
    const data = await RankingService.detalleUsuario(Number(userId), Number(idOposicion));
    if (!data) return res.status(404).json({ ok: false, msg: 'Usuario no encontrado' });
    if (data.error === 'DISTINTA_OPOSICION') {
      return res.status(400).json({ ok: false, msg: 'El usuario no pertenece a esta oposición' });
    }
    if (!data.perfilPublico && data.userId !== req.usuario?.id) {
      return res.status(403).json({ ok: false, msg: 'Perfil no público' });
    }
    res.json({ ok: true, data });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

module.exports = { getRanking, getMiPosicion, togglePerfilPublico, getDetalleUsuario };
