const RutinaService= require('../services/RutinasService');
const db= require('../config/db');

const getMiEntrenamiento = async(req, res)=>{
    try{
        //Recogemos datos, que envia el usuario:
        const {userId, idOposicion}= req.params;

        //Validamos que lleguen todos los datos
        if (!userId || !idOposicion) {
            return res.status(400).json({ 
                ok: false, 
                msg: "Faltan datos obligatorios (userId o idOposicion)" 
            });
        }

        // Validar que el usuario autenticado accede a sus propios datos
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

        //Calculo de nota y nivel llamando al service:
        const {notaMedia,nivelSugerido, genero}= resultadoCalculo; //Como en el service, esa función devuelve tanto nota media como nivel sugerido, aunque solo queramos nivel sugerido, tenemos que tomar las dos desestructurando la respuesta como se ve ahí
        
        
        //Con todos los datos sacados pedimos al servicio que nos traiga la rutina de ese nivel, para esa oposición y ese género

        const rutinaCompleta = await RutinaService.obtenerRutinaCompleta(idOposicion,nivelSugerido,genero);

        //Con todos los datos vamos a generar las respuestas json al movil.
        if(!rutinaCompleta){
            return res.status(404).json({
                ok:false,
                msg: `No se encontro ninguna rutina para el nivel ${nivelSugerido} en esta oposición`
            });
        }
        //Respuesta
        return res.status(200).json({
            ok:true,
            data:{
                notaActual: notaMedia,
                nivelAsignado: nivelSugerido,
                rutinaCompleta
            }    
        })
    }catch(error){
        //Si nada de lo anterior funciona y hay un error de conexión o algo:
        console.error("Error en RutinaController:", error);
        res.status(500).json({
            ok: false,
            msg: "Hubo un problema al generar tu entrenamiento personalizado"
        });
    }
}
module.exports = { getMiEntrenamiento };