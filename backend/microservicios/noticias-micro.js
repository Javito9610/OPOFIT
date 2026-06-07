/**
 * NoticiasMicro
 *
 * Microservicio de noticias por oposición. Invalida la cache de RssService,
 * fuerza un fetch fresco y envía push por convocatorias/plazos nuevos.
 * Se puede ejecutar desde el cron interno (app.js) o como CLI:
 *   node microservicios/run.js noticias
 */
const RssService = require('../src/services/RssService');

const OPOSICIONES = [1, 2, 3, 4, 5, 6];

async function ejecutar({ logger = console } = {}) {
  const inicio = Date.now();
  let totalAlertasEnviadas = 0;
  const detalleOpo = [];

  for (const id of OPOSICIONES) {
    try {
      RssService.invalidarCache(id);
      const noticias = await RssService.obtenerNoticiasRss(id);
      detalleOpo.push({
        idOposicion: id,
        total: noticias.length,
        urgentes: noticias.filter((n) => n.urgente).length
      });
    } catch (e) {
      logger.warn?.(`[noticias-micro] opo ${id} fetch: ${e.message}`);
      detalleOpo.push({ idOposicion: id, error: e.message });
    }
  }

  try {
    const { enviados } = await RssService.pollYNotificarAlertas();
    totalAlertasEnviadas = enviados || 0;
  } catch (e) {
    logger.warn?.(`[noticias-micro] alertas: ${e.message}`);
  }

  const ms = Date.now() - inicio;
  const resumen = {
    ejecucion_ms: ms,
    oposiciones_revisadas: OPOSICIONES.length,
    detalle_oposiciones: detalleOpo,
    alertas_push_enviadas: totalAlertasEnviadas
  };
  logger.log?.('[noticias-micro] OK', JSON.stringify(resumen));
  return resumen;
}

module.exports = { ejecutar };
