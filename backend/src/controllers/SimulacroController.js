const SimulacroService = require('../services/SimulacroService');
const PremiumService = require('../services/PremiumService');

const listarPruebas = async (req, res) => {
  try {
    const { idOposicion } = req.params;
    const userId = req.usuario?.id;
    const pruebas = await SimulacroService.listarPruebas(Number(idOposicion), userId);
    res.json({ ok: true, data: pruebas });
  } catch (e) {
    if (e.message === 'OPOSICION_NOT_FOUND') {
      return res.status(404).json({ ok: false, msg: 'Oposición no encontrada' });
    }
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const guardar = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const { idOposicion, resultados } = req.body || {};
    if (!idOposicion || !Array.isArray(resultados) || resultados.length === 0) {
      return res.status(400).json({ ok: false, msg: 'Datos de simulacro incompletos' });
    }
    const data = await SimulacroService.guardarSimulacro(
      userId,
      Number(idOposicion),
      resultados.map((r) => ({
        id_prueba: Number(r.id_prueba),
        valor: Number(r.valor)
      }))
    );
    res.json({ ok: true, data });
  } catch (e) {
    if (e.message === 'OPOSICION_NOT_FOUND') {
      return res.status(404).json({ ok: false, msg: 'Oposición no encontrada' });
    }
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const historial = async (req, res) => {
  try {
    const { idOposicion } = req.params;
    const userId = req.usuario?.id;
    const puede = await PremiumService.puedeVerHistorialSimulacros(userId);
    if (!puede) {
      return res.status(402).json({
        ok: false,
        code: 'PREMIUM_REQUIRED',
        msg: 'El historial de simulacros es una función Premium'
      });
    }
    const rows = await SimulacroService.historial(userId, Number(idOposicion));
    res.json({ ok: true, data: rows });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

module.exports = { listarPruebas, guardar, historial };
