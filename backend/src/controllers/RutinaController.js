const RutinaService = require('../services/RutinasService');
const db = require('../config/db');
const getMiEntrenamiento = async (req, res) => {
  try {
    const {
      userId: _ignoredUserId,
      idOposicion
    } = req.params;
    const userId = req.usuario?.id;
    if (!userId || !idOposicion) {
      return res.status(400).json({
        ok: false,
        msg: "Faltan datos obligatorios (userId o idOposicion)"
      });
    }
    const resultadoCalculo = await RutinaService.calcularNotaYNivel(userId, idOposicion);
    if (!resultadoCalculo) {
      return res.status(404).json({
        ok: false,
        msg: "No se pudieron calcular las marcas del usuario. Revisa si tiene marcas registradas."
      });
    }
    if (resultadoCalculo.error === 'USER_NOT_FOUND') {
      return res.status(401).json({
        ok: false,
        msg: "Sesión inválida o usuario no existe. Vuelve a iniciar sesión."
      });
    }
    const {
      notaMedia,
      nivelSugerido,
      genero,
      totalPruebas,
      pruebasCompletadas,
      pruebasFaltantes
    } = resultadoCalculo;
    if (pruebasFaltantes > 0) {
      const n = pruebasFaltantes;
      const pruebaWord = n === 1 ? 'prueba oficial' : 'pruebas oficiales';
      return res.status(200).json({
        ok: true,
        msg: `Completa tu perfil: ${n === 1 ? 'falta 1' : `faltan ${n}`} ${pruebaWord} por registrar para calcular tu nivel.`,
        data: {
          notaActual: "-",
          nivelAsignado: "INCOMPLETO",
          rutinaCompleta: [],
          totalPruebas,
          pruebasCompletadas,
          pruebasFaltantes
        }
      });
    }
    const rutinaCompleta = await RutinaService.obtenerRutinaCompleta(idOposicion, nivelSugerido, genero);
    if (!rutinaCompleta) {
      return res.status(404).json({
        ok: false,
        msg: `No se encontro ninguna rutina para el nivel ${nivelSugerido} en esta oposición`
      });
    }
    return res.status(200).json({
      ok: true,
      data: {
        notaActual: notaMedia,
        nivelAsignado: nivelSugerido,
        rutinaCompleta,
        totalPruebas,
        pruebasCompletadas,
        pruebasFaltantes
      }
    });
  } catch (error) {
    console.error("Error en RutinaController:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Hubo un problema al generar tu entrenamiento personalizado"
    });
  }
};
module.exports = {
  getMiEntrenamiento
};
