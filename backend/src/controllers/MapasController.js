const MapasService = require('../services/MapasService');

const tipos = async (_req, res) => {
  res.json({ ok: true, data: MapasService.listarTipos() });
};

const lugares = async (req, res) => {
  try {
    const { lat, lng, tipo, radio } = req.query;
    const data = await MapasService.buscarLugares(lat, lng, tipo, radio);
    res.json({ ok: true, data });
  } catch (e) {
    console.error('Mapas lugares:', e.message);
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const rutaSugerida = async (req, res) => {
  try {
    const { lat, lng, distKm, variacion } = req.query;
    const data = await MapasService.generarRutaSugerida(lat, lng, distKm, variacion);
    res.json({ ok: true, data });
  } catch (e) {
    console.error('Mapas ruta sugerida:', e.message);
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const rutaPersonalizada = async (req, res) => {
  try {
    const { waypoints, nombre } = req.body || {};
    const data = await MapasService.rutaEntreWaypoints(waypoints, nombre);
    res.json({ ok: true, data });
  } catch (e) {
    console.error('Mapas ruta personalizada:', e.message);
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const exportarGpx = async (req, res) => {
  try {
    const ruta = req.body?.ruta || req.body;
    const gpx = MapasService.aGpx(ruta);
    res.setHeader('Content-Type', 'application/gpx+xml');
    res.setHeader('Content-Disposition', 'attachment; filename="opofit_ruta.gpx"');
    res.send(gpx);
  } catch (e) {
    console.error('Mapas export GPX:', e.message);
    res.status(400).json({ ok: false, msg: e.message });
  }
};

module.exports = { tipos, lugares, rutaSugerida, rutaPersonalizada, exportarGpx };
