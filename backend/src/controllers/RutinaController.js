const RutinaService = require('../services/RutinasService');
const PlanesService = require('../services/PlanesService');
const PremiumService = require('../services/PremiumService');
const db = require('../config/db');
const { isFitnessModo, planOposicionId } = require('../utils/FitnessMode');
const getMiEntrenamiento = async (req, res) => {
  try {
    const {
      userId: _ignoredUserId,
      idOposicion: idOposicionParam
    } = req.params;
    const userId = req.usuario?.id;
    if (!userId) {
      return res.status(400).json({
        ok: false,
        msg: "Faltan datos obligatorios (userId)"
      });
    }
    const [[usuarioRow]] = await db.query(
      'SELECT oposiciones_id_oposicion, modo_uso, genero FROM usuarios WHERE id_usuario = ?',
      [userId]
    );
    if (!usuarioRow) {
      return res.status(401).json({ ok: false, msg: 'Sesión inválida' });
    }
    const esFitness = isFitnessModo(usuarioRow.modo_uso);
    const idOposicion = esFitness
      ? planOposicionId(usuarioRow)
      : Number(idOposicionParam || usuarioRow.oposiciones_id_oposicion);
    if (!idOposicion) {
      return res.status(400).json({
        ok: false,
        msg: "Faltan datos obligatorios (idOposicion)"
      });
    }
    const existeOpo = await PremiumService.puedeAccederOposicion(userId, idOposicion);
    if (!existeOpo) {
      return res.status(404).json({ ok: false, msg: 'Oposición no encontrada' });
    }
    const premium = await PremiumService.getEstadoPremium(userId);
    const resultadoCalculo = esFitness
      ? {
          notaMedia: null,
          nivelSugerido: 'BASICO',
          genero: usuarioRow.genero,
          totalPruebas: 0,
          pruebasCompletadas: 0,
          pruebasFaltantes: 0
        }
      : await RutinaService.calcularNotaYNivel(userId, idOposicion);
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
    if (!esFitness && pruebasFaltantes > 0) {
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
    const nivelParaRutinas =
      !premium.esPremium && nivelSugerido !== 'INCOMPLETO' && nivelSugerido !== 'BASICO'
        ? 'BASICO'
        : nivelSugerido;
    let rutinaCompleta = await RutinaService.obtenerRutinaCompleta(
      idOposicion,
      nivelParaRutinas,
      genero
    );
    if (!rutinaCompleta && nivelParaRutinas !== nivelSugerido) {
      rutinaCompleta = await RutinaService.obtenerRutinaCompleta(idOposicion, 'BASICO', genero);
    }
    if (!rutinaCompleta) {
      return res.status(404).json({
        ok: false,
        msg: `No se encontró rutina para el nivel ${nivelParaRutinas} en esta oposición`
      });
    }
    const nivelPremiumBloqueado =
      !premium.esPremium && ['INTERMEDIO', 'AVANZADO'].includes(nivelSugerido);

    let planSemanal = null;
    try {
      planSemanal = await PlanesService.obtenerPlanSemanal(
        userId,
        idOposicion,
        nivelParaRutinas,
        genero
      );
    } catch (planErr) {
      console.error('planSemanal:', planErr.message);
    }

    return res.status(200).json({
      ok: true,
      data: {
        notaActual: notaMedia,
        nivelAsignado: nivelSugerido,
        nivelRutinasMostradas: nivelParaRutinas,
        rutinaCompleta,
        planSemanal,
        totalPruebas,
        pruebasCompletadas,
        pruebasFaltantes,
        esPremium: premium.esPremium,
        nivelPremiumBloqueado,
        msgPremium:
          nivelPremiumBloqueado
            ? `Tu nivel calculado es ${nivelSugerido}. Con Premium desbloqueas planes INTERMEDIO y AVANZADO.`
            : null
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
