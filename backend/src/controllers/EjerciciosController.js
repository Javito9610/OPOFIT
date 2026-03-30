const EjerciciosService = require('../services/EjerciciosService');

const listarEjercicios = async (req, res) => {
    try {
        const ejercicios = await EjerciciosService.listarTodos();
        res.status(200).json({ ok: true, data: ejercicios });
    } catch (error) {
        console.error("Error al listar ejercicios:", error);
        res.status(500).json({ ok: false, msg: "Error al obtener los ejercicios" });
    }
};

module.exports = { listarEjercicios };
