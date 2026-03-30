const RutinaService= require('../services/RutinasService');
const db= require('../config/db');

const getMiEntrenamiento = async(req, res)=>{
    try{
        const {userId, idOposicion}= req.params;

        if (!userId || !idOposicion) {
            return res.status(400).json({ 
                ok: false, 
                msg: "Faltan datos obligatorios (userId o idOposicion)" 
            });
        }

        if (parseInt(userId) !== req.usuario.id) {
            return res.status(403).json({ ok: false, msg: "No tienes permiso para acceder a estos datos" });
        }

        const resultadoCalculo = await RutinaService.calcularNotaYNivel(userId, idOposicion);

        if (!resultadoCalculo) {
            return res.status(404).json({
                ok: false,
                msg: "No se pudieron calcular las marcas del usuario. Revisa si tiene marcas registradas."
            });
        }

        const {notaMedia,nivelSugerido, genero}= resultadoCalculo;
        
        const rutinaCompleta = await RutinaService.obtenerRutinaCompleta(idOposicion,nivelSugerido,genero);

        if(!rutinaCompleta){
            return res.status(404).json({
                ok:false,
                msg: `No se encontro ninguna rutina para el nivel ${nivelSugerido} en esta oposición`
            });
        }
        return res.status(200).json({
            ok:true,
            data:{
                notaActual: notaMedia,
                nivelAsignado: nivelSugerido,
                rutinaCompleta
            }    
        })
    }catch(error){
        console.error("Error en RutinaController:", error);
        res.status(500).json({
            ok: false,
            msg: "Hubo un problema al generar tu entrenamiento personalizado"
        });
    }
}
module.exports = { getMiEntrenamiento };
