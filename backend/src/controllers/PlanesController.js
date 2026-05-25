const PlanesService = require('../services/PlanesService');
const RutinaService = require('../services/RutinasService');
const PremiumService = require('../services/PremiumService');

const getCalendario = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const { idOposicion } = req.params;
    const { year, month } = req.query;
    if (!userId || !idOposicion) {
      return res.status(400).json({ ok: false, msg: 'Parámetros inválidos' });
    }
    const calc = await RutinaService.calcularNotaYNivel(userId, idOposicion);
    if ((calc.pruebasFaltantes ?? 0) > 0) {
      return res.status(200).json({ ok: true, data: null, msg: 'Completa las marcas del perfil' });
    }
    const premium = await PremiumService.getEstadoPremium(userId);
    const nivel =
      !premium.esPremium && calc.nivelSugerido !== 'BASICO' ? 'BASICO' : calc.nivelSugerido;
    const cal = await PlanesService.obtenerCalendarioMes(
      userId,
      idOposicion,
      nivel,
      calc.genero,
      year,
      month
    );
    return res.status(200).json({ ok: true, data: cal });
  } catch (e) {
    console.error('getCalendario:', e.message);
    return res.status(500).json({ ok: false, msg: 'Error al cargar calendario' });
  }
};

module.exports = { getCalendario };
