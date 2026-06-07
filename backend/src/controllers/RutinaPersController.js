const rutinaPersService = require("../services/RutinaPersService");
const nuevaRutinaPersonalizada = async (req, res) => {
  try {
    const {
      userId,
      nombre,
      ejercicios,
      entorno
    } = req.body;
    if (!userId || !nombre || !ejercicios || ejercicios.length === 0) {
      return res.status(400).json({
        ok: false,
        msg: "La rutina debe tener un nombre y al menos un ejercicio seleccionado"
      });
    }
    if (parseInt(userId) !== req.usuario.id) {
      return res.status(403).json({
        ok: false,
        msg: "No tienes permiso para crear rutinas para otro usuario"
      });
    }
    const id = await rutinaPersService.crearRutinaPropia(userId, nombre, ejercicios, entorno);
    res.status(201).json({
      ok: true,
      msg: "Rutina creada con exito",
      id
    });
  } catch (error) {
    console.error("Error en nuevaRutinaPersonalizada:", error.message);
    const msg = error.message || '';
    if (msg.includes('Ya tienes una rutina con este nombre')) {
      return res.status(409).json({
        ok: false,
        msg: msg
      });
    }
    res.status(500).json({
      ok: false,
      msg: "No se pudo guardar la rutina personalizada"
    });
  }
};
const misRutinas = async (req, res) => {
  try {
    const {
      userId
    } = req.params;
    if (!userId) {
      return res.status(400).json({
        ok: false,
        msg: "ID de usuario necesario"
      });
    }
    if (parseInt(userId) !== req.usuario.id) {
      return res.status(403).json({
        ok: false,
        msg: "No tienes permiso para ver rutinas de otro usuario"
      });
    }
    const lista = await rutinaPersService.listarMisRutinas(userId);
    if (!lista || lista.length === 0) {
      return res.status(200).json({
        ok: true,
        data: [],
        msg: "Aún no has creado ninguna rutina personalizada"
      });
    }
    const grouped = new Map();
    for (const row of lista) {
      if (!grouped.has(row.id_rutina_pers)) {
        grouped.set(row.id_rutina_pers, {
          id_rutina_pers: row.id_rutina_pers,
          nombre_personalizado: row.nombre_personalizado,
          entorno_entreno: row.entorno_entreno || null,
          ejercicios: []
        });
      }
      if (row.ejercicios_id_ejercicio != null) {
        grouped.get(row.id_rutina_pers).ejercicios.push({
          id_ejercicio: row.ejercicios_id_ejercicio,
          nombre_ejercicio: row.nombre_ejercicio,
          series: row.series,
          repeticiones: row.repeticiones,
          descanso: row.descanso
        });
      }
    }
    res.status(200).json({
      ok: true,
      data: Array.from(grouped.values())
    });
  } catch (error) {
    console.error("Error en misRutinas:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al listar rutinas"
    });
  }
};
const eliminarRutina = async (req, res) => {
  try {
    const {
      userId,
      idRutina
    } = req.params;
    if (!userId || !idRutina) {
      return res.status(400).json({
        ok: false,
        msg: "Faltan datos para eliminar la rutina"
      });
    }
    if (parseInt(userId) !== req.usuario.id) {
      return res.status(403).json({
        ok: false,
        msg: "No tienes permiso para eliminar rutinas de otro usuario"
      });
    }
    await rutinaPersService.eliminarRutina(userId, idRutina);
    res.status(200).json({
      ok: true,
      msg: "Rutina eliminada correctamente"
    });
  } catch (error) {
    console.error("Error en eliminarRutina:", error.message);
    if (error.code === 'NOT_FOUND') {
      return res.status(404).json({
        ok: false,
        msg: error.message
      });
    }
    res.status(500).json({
      ok: false,
      msg: "No se pudo eliminar la rutina"
    });
  }
};
module.exports = {
  nuevaRutinaPersonalizada,
  misRutinas,
  eliminarRutina
};
