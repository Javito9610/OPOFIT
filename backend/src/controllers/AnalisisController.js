const AnalisisService = require('../services/AnalisisService');

function badRequest(res, msg) {
  return res.status(400).json({ ok: false, msg });
}

const traducirError = {
  FCMAX_REQUERIDA: 'Indica tu frecuencia cardiaca maxima o tu edad.',
  DURACION_INVALIDA: 'La duracion debe ser mayor que 0.',
  FC_MEDIA_INVALIDA: 'La frecuencia cardiaca media no es valida.',
  FCMAX_MENOR_QUE_REPOSO: 'La FCmax debe ser mayor que la FC en reposo.',
  DISTANCIA_INVALIDA: 'La distancia debe ser mayor que 0.',
  TIEMPO_INVALIDO: 'El tiempo debe ser mayor que 0.',
  OBJETIVO_INVALIDO: 'La distancia objetivo debe ser mayor que 0.'
};

function manejar(res, fn) {
  try {
    return res.json({ ok: true, data: fn() });
  } catch (e) {
    return badRequest(res, traducirError[e.message] || e.message || 'Datos invalidos');
  }
}

const zonasFC = (req, res) => {
  const { fcMax, edad } = req.body || {};
  return manejar(res, () => AnalisisService.zonasFC({ fcMax, edad }));
};

const zonasDistribucion = (req, res) => {
  const { muestras, fcMax, edad, intervaloSeg } = req.body || {};
  return manejar(res, () => AnalisisService.distribucionZonas(muestras, { fcMax, edad, intervaloSeg }));
};

const tss = (req, res) => {
  const { durSeg, avgHr, fcReposo, fcMax, edad } = req.body || {};
  return manejar(res, () => AnalisisService.estimarTSS({ durSeg, avgHr, fcReposo, fcMax, edad }));
};

const prediccion = (req, res) => {
  const { distanciaM, tiempoSeg, objetivoM, exponente } = req.body || {};
  return manejar(res, () => AnalisisService.predecirTiempo({ distanciaM, tiempoSeg, objetivoM, exponente }));
};

const vo2max = (req, res) => {
  const { distancia12minM } = req.body || {};
  return manejar(res, () => ({ vo2max: AnalisisService.vo2maxCooper(distancia12minM) }));
};

module.exports = { zonasFC, zonasDistribucion, tss, prediccion, vo2max };
