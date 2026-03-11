
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

module.exports= {nuevaRutinaPersonalizada, misRutinas};

