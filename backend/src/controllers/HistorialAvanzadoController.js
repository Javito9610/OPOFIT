const HistorialAvanzadoService = require('../services/HistorialAvanzadoService');

const resumen = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    const data = await HistorialAvanzadoService.resumen(userId, req.query.periodo);
    res.json({ ok: true, data });
  } catch (e) {
    console.error('historial resumen:', e.message);
    res.status(500).json({ ok: false, msg: 'No se pudo cargar el resumen' });
  }
};

const listarSesiones = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    const data = await HistorialAvanzadoService.listarSesiones(userId, {
      tipo: req.query.tipo,
      planId: req.query.planId,
      desde: req.query.desde,
      hasta: req.query.hasta,
      limit: req.query.limit
    });
    res.json({ ok: true, data });
  } catch (e) {
    console.error('historial sesiones:', e.message);
    res.status(500).json({ ok: false, msg: 'No se pudieron listar las sesiones' });
  }
};

const detalleSesion = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    const data = await HistorialAvanzadoService.detalleSesion(userId, Number(req.params.id));
    if (!data) return res.status(404).json({ ok: false, msg: 'Sesión no encontrada' });
    res.json({ ok: true, data });
  } catch (e) {
    console.error('historial detalle sesion:', e.message);
    res.status(500).json({ ok: false, msg: 'No se pudo cargar la sesión' });
  }
};

const historialEjercicio = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    const data = await HistorialAvanzadoService.historialEjercicio(userId, Number(req.params.idEjercicio));
    if (!data) return res.json({ ok: true, data: null });
    res.json({ ok: true, data });
  } catch (e) {
    console.error('historial ejercicio:', e.message);
    res.status(500).json({ ok: false, msg: 'No se pudo cargar el ejercicio' });
  }
};

const historialPlan = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    const data = await HistorialAvanzadoService.historialPlan(userId, Number(req.params.idPlan));
    res.json({ ok: true, data });
  } catch (e) {
    console.error('historial plan:', e.message);
    res.status(500).json({ ok: false, msg: 'No se pudo cargar el plan' });
  }
};

module.exports = { resumen, listarSesiones, detalleSesion, historialEjercicio, historialPlan };
