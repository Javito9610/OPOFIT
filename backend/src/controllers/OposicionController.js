// IMPORTAMOS EL SERVICE PARA PODER CONECTARNOS A EL Y ENVIARLE LAS ÓRDENES:
const OposicionesService=require("../services/OposicionService");

// PARA, EN EL REGISTRO, PODER OBTENER TODAS LAS OPCIONES DE OPOSICIONES:
const getOposiciones=async(req, res)=>{//   LA UTILIDAD DE ASYNC YA ESTA EXPLICADA EN AUTHCONTROLLER
    //PONEMOS UNA RED DE SEGURIDAD DENTRO DE LA PETICIÓN POR SI FALLA EL SERVICIO O LA PETICIÓN ES INCORRECTA, QUE NO SE ROMPA LA APP
    try {
        const opos= await OposicionesService.obtenerTodas();//ENVIAMOS LA ORDEN AL SERVICE PARA QUE EJECUTE LA QUERY

        if (!opos || opos.length === 0) {
            return res.status(404).json({
                ok: false,
                msg: "No hay oposiciones disponibles en el sistema"
            });
        }

    //SI SE GENERA CORRECTAMENTE:
    res.status(200).json({
        ok: true,
        data: opos
    });
    } catch (error) {
        console.error("Error en getOposiciones:", error.message);
        //ERROR EN EL SERVIDOR, PORQUE NO ESTA DEVOLVIENDO LOS DATOS REQUERIDOS
        res.status(500).json({ok:false, error: "Error al cargar el listado de oposiciones"});
    }
};

//PARA OBTENER INFORMACION SOBRE LAS PRUEBAS Y LAS NOTICIAS RELACIONADAS CON DICHAS OPOS
const getInfoOposiciones= async(req,res)=>{
    try {
        //EL ID VIENE EN LA URL
        const {id}=req.params;

        if (!id) {
            return res.status(400).json({ ok: false, msg: "ID de oposición no proporcionado" });
        }

        const detalle=await OposicionesService.obtenerDetalleCompleto(id);

        if (!detalle) {
            return res.status(404).json({ ok: false, msg: "Oposición no encontrada" });
        }

        res.status(200).json({
            ok: true,
            pruebas:detalle.pruebas || [],
            noticias:detalle.noticias || []
        })

    } catch (error) {
        //ERROR EN EL SERVIDOR, PORQUE NO ESTA DEVOLVIENDO LOS DATOS REQUERIDOS
        res.status(500).json({
            ok:false,
            error:error.message
        });
    }
};

//PARA OBTENER LAS NOTAS DE CADA UNO DE LOS VALORES_OBJETIVO EN DICHAS PRUEBAS REFERENTE A CADA OPOSICIÓN
const getRequisitos=async(req,res)=>{
    try {
        const{id, genero}=req.params;

        if (!id || !genero) {
            return res.status(400).json({ ok: false, msg: "ID y género son obligatorios" });
        }

        const requisitos=await OposicionesService.obtenerRequisitosPrueba(id,genero);

        if(!requisitos || requisitos.length===0){
            return  res.status(404).json({ok: false, message: "No se encontraron requisitos"});
        }

        res.status(200).json({
            ok:true,
            data: requisitos
        });
    } catch (error) {
        //ERROR EN EL SERVIDOR, PORQUE NO ESTA DEVOLVIENDO LOS DATOS REQUERIDOS
        res.status(500).json({
            ok: false,
            error:error.message
        });
    }
};

module.exports={getOposiciones,getInfoOposiciones,getRequisitos};
