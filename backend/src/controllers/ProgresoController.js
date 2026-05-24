const progresoService = require("../services/ProgresoService");
const guardarEntrenamiento = async (req, res) => {
  try {
    const {
      tipoRutina,
      idRutina,
      duracion,
      ejercicios
    } = req.body ?? {};
    const userId = req.usuario?.id;
    if (userId == null) return res.status(401).json({
      ok: false,
      msg: "Sesión inválida. Vuelve a iniciar sesión."
    });
    if (!tipoRutina || !['OPO', 'PERS'].includes(tipoRutina)) {
      return res.status(400).json({
        ok: false,
        msg: "tipoRutina inválido (usa 'OPO' o 'PERS')"
      });
    }
    if (idRutina == null || !Number.isFinite(Number(idRutina))) {
      return res.status(400).json({
        ok: false,
        msg: "Falta idRutina o no es válido"
      });
    }
    if (duracion == null || !Number.isFinite(Number(duracion)) || Number(duracion) < 0) {
      return res.status(400).json({
        ok: false,
        msg: "Falta duracion o no es válida"
      });
    }
    if (!Array.isArray(ejercicios) || ejercicios.length === 0) {
      return res.status(400).json({
        ok: false,
        msg: "Datos de entrenamiento incompletos o vacíos"
      });
    }
    const resultado = await progresoService.registrarEntreno({
      userId: Number(userId),
      tipoRutina,
      idRutina: Number(idRutina),
      duracion: Number(duracion),
      ejercicios
    });
    res.status(200).json({
      ok: true,
      msg: "Entrenamiento guardado correctamente",
      id: resultado.idHistorial,
      recordsRotos: resultado.recordsRotos || []
    });
  } catch (error) {
    console.error("Error en guardarEntrenamiento:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al registrar el entrenamiento"
    });
  }
};
const verEvolucion = async (req, res) => {
  try {
    const {
      idEjercicio
    } = req.params;
    const userId = req.usuario?.id;
    if (!userId || !idEjercicio) {
      return res.status(400).json({
        ok: false,
        msg: "Faltan identificadores (Usuario o Ejercicio)"
      });
    }
    const datos = await progresoService.obtenerEvolucionEntreno(userId, idEjercicio);
    if (!datos || datos.length === 0) {
      return res.status(200).json({
        ok: true,
        data: [],
        msg: "Aún no hay registros de progreso para este ejercicio"
      });
    }
    res.status(200).json({
      ok: true,
      data: datos
    });
  } catch (error) {
    console.error("Error en verEvolucion:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al obtener progreso"
    });
  }
};
const verHistorialSesiones = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({
      ok: false,
      msg: "Sesión inválida. Vuelve a iniciar sesión."
    });
    const datos = await progresoService.obtenerHistorialSesiones(userId);
    res.status(200).json({
      ok: true,
      data: datos
    });
  } catch (error) {
    console.error("Error en verHistorialSesiones:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al obtener el historial"
    });
  }
};
module.exports = {
  guardarEntrenamiento,
  verEvolucion,
  verHistorialSesiones
};
