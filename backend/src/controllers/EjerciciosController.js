const EjerciciosService = require('../services/EjerciciosService');
const listarEjercicios = async (req, res) => {
  try {
    const { categoria, pilar, busqueda, limite, entorno, grupo_muscular } = req.query;
    const ejercicios = await EjerciciosService.listarTodos({
      categoria,
      pilar,
      busqueda,
      limite,
      entorno,
      grupo_muscular
    });
    res.status(200).json({
      ok: true,
      data: ejercicios
    });
  } catch (error) {
    console.error("Error al listar ejercicios:", error);
    res.status(500).json({
      ok: false,
      msg: "Error al obtener los ejercicios"
    });
  }
};
module.exports = {
  listarEjercicios
};
