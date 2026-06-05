const DashboardService = require('../services/DashboardService');
const db = require('../config/db');
const { isFitnessModo, planOposicionId } = require('../utils/FitnessMode');

const getResumen = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const idOposicionQuery = Number(req.query.idOposicion);
    if (!userId) {
      return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    }
    const [[u]] = await db.query(
      'SELECT oposiciones_id_oposicion, modo_uso, genero FROM usuarios WHERE id_usuario = ?',
      [userId]
    );
    if (!u) {
      return res.status(401).json({ ok: false, msg: 'Usuario no encontrado' });
    }
    const esFitness = isFitnessModo(u.modo_uso);
    const idOposicion = idOposicionQuery || planOposicionId(u);
    if (!esFitness && !u.oposiciones_id_oposicion && !idOposicionQuery) {
      return res.status(400).json({ ok: false, msg: 'Indica idOposicion' });
    }
    const data = await DashboardService.obtenerResumen(userId, idOposicion, { esFitness, genero: u.genero });
    res.json({ ok: true, data });
  } catch (e) {
    console.error('Dashboard:', e.message, e.stack);
    res.status(500).json({ ok: false, msg: 'No se pudo cargar el resumen' });
  }
};

module.exports = { getResumen };
