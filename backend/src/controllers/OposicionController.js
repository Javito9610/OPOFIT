const OposicionesService = require("../services/OposicionService");
const RssService = require("../services/RssService");
const getOposiciones = async (req, res) => {
  try {
    const opos = await OposicionesService.obtenerTodas();
    if (!opos || opos.length === 0) {
      return res.status(404).json({
        ok: false,
        msg: "No hay oposiciones disponibles en el sistema"
      });
    }
    res.status(200).json({
      ok: true,
      data: opos
    });
  } catch (error) {
    console.error("Error en getOposiciones:", error.message);
    res.status(500).json({
      ok: false,
      error: "Error al cargar el listado de oposiciones"
    });
  }
};
const getInfoOposiciones = async (req, res) => {
  try {
    const {
      id
    } = req.params;
    if (!id) {
      return res.status(400).json({
        ok: false,
        msg: "ID de oposición no proporcionado"
      });
    }
    const detalle = await OposicionesService.obtenerDetalleCompleto(id);
    if (!detalle) {
      return res.status(404).json({
        ok: false,
        msg: "Oposición no encontrada"
      });
    }
    res.status(200).json({
      ok: true,
      oposicion: detalle.oposicion || null,
      pruebas: detalle.pruebas || [],
      noticias: detalle.noticias || []
    });
  } catch (error) {
    console.error("Error en getInfoOposiciones:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al obtener el detalle de la oposición"
    });
  }
};
const getNoticiasRss = async (req, res) => {
  try {
    const { id } = req.params;
    if (!id) {
      return res.status(400).json({
        ok: false,
        msg: "ID de oposición no proporcionado"
      });
    }
    // ?refresh=true → invalida cache y fuerza descarga fresca de los feeds.
    // Útil para pull-to-refresh del usuario y para garantizar noticias actuales.
    if (req.query?.refresh === 'true') {
      RssService.invalidarCache(id);
    }
    const noticias = await RssService.obtenerNoticiasRss(id);
    res.status(200).json({
      ok: true,
      data: noticias
    });
  } catch (error) {
    console.error("Error en getNoticiasRss:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al obtener noticias RSS"
    });
  }
};
const getRequisitos = async (req, res) => {
  try {
    const {
      id,
      genero
    } = req.params;
    if (!id || !genero) {
      return res.status(400).json({
        ok: false,
        msg: "ID y género son obligatorios"
      });
    }
    const requisitos = await OposicionesService.obtenerRequisitosPrueba(id, genero);
    if (!requisitos || requisitos.length === 0) {
      return res.status(404).json({
        ok: false,
        message: "No se encontraron requisitos"
      });
    }
    res.status(200).json({
      ok: true,
      data: requisitos
    });
  } catch (error) {
    console.error("Error en getRequisitos:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al obtener los requisitos de la prueba"
    });
  }
};
module.exports = {
  getOposiciones,
  getInfoOposiciones,
  getRequisitos,
  getNoticiasRss
};
