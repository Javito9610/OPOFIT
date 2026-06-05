const SegmentosService = require('../services/SegmentosService');

const listar = async (_req, res) => {
  try {
    const data = await SegmentosService.listar();
    res.json({ ok: true, data });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const ranking = async (req, res) => {
  try {
    const data = await SegmentosService.ranking(Number(req.params.idSegmento));
    res.json({ ok: true, data });
  } catch (e) {
    const status = e.message === 'SEGMENTO_NO_ENCONTRADO' ? 404 : 500;
    res.status(status).json({ ok: false, msg: e.message });
  }
};

const registrarEsfuerzo = async (req, res) => {
  try {
    const { duracionMs, gpsUuid } = req.body || {};
    const data = await SegmentosService.registrarEsfuerzo(
      req.usuario.id,
      Number(req.params.idSegmento),
      duracionMs,
      gpsUuid
    );
    res.status(201).json({ ok: true, data });
  } catch (e) {
    const status = e.message === 'SEGMENTO_NO_ENCONTRADO' ? 404 : 400;
    res.status(status).json({ ok: false, msg: e.message });
  }
};

const registrarDesdeActividad = async (req, res) => {
  try {
    const { gpsUuid, esfuerzos } = req.body || {};
    const data = await SegmentosService.registrarDesdeActividad(
      req.usuario.id,
      gpsUuid,
      esfuerzos
    );
    res.json({ ok: true, data });
  } catch (e) {
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const crearGeografico = async (req, res) => {
  try {
    const data = await SegmentosService.crearGeografico(req.usuario.id, req.body || {});
    res.status(201).json({ ok: true, data });
  } catch (e) {
    const status = ['NOMBRE_OBLIGATORIO', 'COORDENADAS_INVALIDAS', 'SEGMENTO_MUY_CORTO'].includes(e.message)
      ? 400 : 500;
    res.status(status).json({ ok: false, msg: e.message });
  }
};

module.exports = { listar, ranking, registrarEsfuerzo, registrarDesdeActividad, crearGeografico };
