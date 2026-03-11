const progresoService= require("../services/ProgresoService");

const guardarEntrenamiento= async (req, res)=>{
    try{

        if (!req.body.userId || !req.body.ejercicios || req.body.ejercicios.length === 0) {
            return res.status(400).json({
                ok: false,
                msg: "Datos de entrenamiento incompletos o vacíos"
            });
        }

        const resultado= await progresoService.registrarEntreno(req.body); //req.body contiene todo el JSON que le envía el movil del entrenamiento.
        res.status(200).json({
            ok: true,
            msg:"Entrenamiento guardado correctamente",
            id: resultado.idHistorial
        })
    }catch(error){
        console.error("Error en guardarEntrenamiento:", error.message);
        res.status(500).json({
            ok:false,
            msg: "Error al registrar",
            error: error.message
        })
    }
};

const verEvolucion=async(req, res)=>{
    try{
        const {userId, idEjercicio}=req.params;

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

        res.status(200).json({ ok: true, data: datos });
    }catch(error){
        res.status(500).json({ 
            ok: false, 
            msg: "Error al obtener progreso", 
            error: error.message
        });
    }
};
module.exports = { guardarEntrenamiento, verEvolucion };