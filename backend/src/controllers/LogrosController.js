const HistorialAvanzadoService = require('../services/HistorialAvanzadoService');
const LogrosService = require('../services/LogrosService');

/**
 * GET /api/logros
 * Devuelve rachas, records y medallas del usuario calculados a partir de su
 * resumen historico (all-time). La logica de calculo vive en LogrosService (puro).
 */
const misLogros = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesion invalida' });

    const resumen = await HistorialAvanzadoService.resumen(userId, 'all');

    // Fechas de sesion para las rachas: una por dia activo (del heatmap).
    const fechasSesiones = (resumen.heatmap || []).map((d) => d.dia);

    // Records a partir de los mejores PRs disponibles en el resumen.
    const registros = (resumen.topPrs || []).map((p, i) => ({
      idEjercicio: p.ejercicio || `pr_${i}`,
      nombre: p.ejercicio,
      valor: p.valor,
      menorEsMejor: false
    }));

    const stats = {
      sesiones: Number(resumen.sesiones || 0),
      distanciaKm: Number(resumen.distanciaTotalKm || 0),
      desnivelM: Number(resumen.gps?.desnivelPosM || 0)
    };

    const logros = LogrosService.construirLogros({ stats, fechasSesiones, registros });
    return res.json({ ok: true, data: { ...logros, stats } });
  } catch (e) {
    console.error('Logros misLogros:', e.message);
    return res.status(500).json({ ok: false, msg: 'No se pudieron calcular los logros' });
  }
};

module.exports = { misLogros };
