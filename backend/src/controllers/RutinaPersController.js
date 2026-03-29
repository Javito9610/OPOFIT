
const rutinaPersService= require("../services/RutinaPersService");

const nuevaRutinaPersonalizada=async (req, res)=>{
    try{
        const{userId, nombre, ejercicios}=req.body;

        if (!userId || !nombre || !ejercicios || ejercicios.length === 0) {
            return res.status(400).json({
                ok: false,
                msg: "La rutina debe tener un nombre y al menos un ejercicio seleccionado"
            });
        }

        // Validar que el usuario autenticado accede a sus propios datos
        if (parseInt(userId) !== req.usuario.id) {
            return res.status(403).json({ ok: false, msg: "No tienes permiso para crear rutinas para otro usuario" });
        }

        const id = await rutinaPersService.crearRutinaPropia(userId, nombre, ejercicios);


        res.status(201).json({
            ok:true,
            msg:"Rutina creada con exito",
            id
        })
    }catch(error){
        console.error("Error en nuevaRutinaPersonalizada:", error.message);
        res.status(500).json({ 
            ok: false,
            msg: "No se pudo guardar la rutina personalizada",
            error: error.message });
    }
};

const misRutinas= async (req, res)=>{
    try{
        const {userId}=req.params;

        if (!userId) {
            return res.status(400).json({ ok: false, msg: "ID de usuario necesario" });
        }

        // Validar que el usuario autenticado accede a sus propios datos
        if (parseInt(userId) !== req.usuario.id) {
            return res.status(403).json({ ok: false, msg: "No tienes permiso para ver rutinas de otro usuario" });
        }

        const lista = await rutinaPersService.listarMisRutinas(userId);

        if (!lista || lista.length === 0) {
            return res.status(200).json({
                ok: true,
                data: [],
                msg: "Aún no has creado ninguna rutina personalizada"
            });
        }

        res.status(200).json({
            ok:true,
            data:lista
        })
    }catch(error){
        res.status(500).json({ 
            ok: false,
            msg: "Error al listar rutinas",
            error: error.message
        });
    }
};

const eliminarRutina = async (req, res) => {
    try {
        const { userId, idRutina } = req.params;

        if (!userId || !idRutina) {
            return res.status(400).json({ ok: false, msg: "Faltan datos para eliminar la rutina" });
        }

        // Validar que el usuario autenticado accede a sus propios datos
        if (parseInt(userId) !== req.usuario.id) {
            return res.status(403).json({ ok: false, msg: "No tienes permiso para eliminar rutinas de otro usuario" });
        }

        await rutinaPersService.eliminarRutina(userId, idRutina);

        res.status(200).json({
            ok: true,
            msg: "Rutina eliminada correctamente"
        });
    } catch (error) {
        console.error("Error en eliminarRutina:", error.message);
        res.status(500).json({
            ok: false,
            msg: "No se pudo eliminar la rutina",
            error: error.message
        });
    }
};

module.exports= {nuevaRutinaPersonalizada, misRutinas, eliminarRutina};

