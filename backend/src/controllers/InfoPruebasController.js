const infoPruebasService = require('../services/InfoPruebasService');
const getInfoPruebas = async (req, res) => {
  try {
    const {
      idOposicion,
      genero
    } = req.params;
    if (!idOposicion || !genero) {
      return res.status(400).json({
        ok: false,
        msg: "Oposición o género no especificados"
      });
    }
    const listaInfoOpo = await infoPruebasService.getInfoPruebas(idOposicion, genero);
    if (!listaInfoOpo || listaInfoOpo.length === 0) {
      return res.status(404).json({
        ok: false,
        msg: "No se encontraron pruebas para esta oposición o género"
      });
    }
    res.status(200).json({
      ok: true,
      data: listaInfoOpo
    });
  } catch (error) {
    console.error("Error en getInfoPruebas:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al obtener la información de la oposición"
    });
  }
};
const getMarcasUsuario = async (req, res) => {
  try {
    const {
      userId,
      idOposicion
    } = req.params;
    if (!userId || !idOposicion) {
      return res.status(400).json({
        ok: false,
        msg: "Faltan datos obligatorios (userId o idOposicion)"
      });
    }
    const parsedUserId = parseInt(userId, 10);
    if (isNaN(parsedUserId) || parsedUserId !== req.usuario.id) {
      return res.status(403).json({
        ok: false,
        msg: "No tienes permiso para ver marcas de otro usuario"
      });
    }
    const marcas = await infoPruebasService.getMarcasUsuario(userId, idOposicion);
    res.status(200).json({
      ok: true,
      data: marcas || []
    });
  } catch (error) {
    console.error("Error en getMarcasUsuario:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al obtener las marcas del usuario"
    });
  }
};
module.exports = {
  getInfoPruebas,
  getMarcasUsuario
};
