const DashboardService = require('../services/DashboardService');
const db = require('../config/db');

const getResumen = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const idOposicion = Number(req.query.idOposicion);
    if (!userId) {
      return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    }
    if (!idOposicion) {
      const [[u]] = await db.query(
        'SELECT oposiciones_id_oposicion FROM usuarios WHERE id_usuario = ?',
        [userId]
      );
      if (!u?.oposiciones_id_oposicion) {
        return res.status(400).json({ ok: false, msg: 'Indica idOposicion' });
      }
      const data = await DashboardService.obtenerResumen(userId, u.oposiciones_id_oposicion);
      return res.json({ ok: true, data });
    }
    const data = await DashboardService.obtenerResumen(userId, idOposicion);
    res.json({ ok: true, data });
  } catch (e) {
    console.error('Dashboard:', e.message);
    res.status(500).json({ ok: false, msg: e.message });
  }
};

module.exports = { getResumen };
