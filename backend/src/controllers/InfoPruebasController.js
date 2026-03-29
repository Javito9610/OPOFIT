const infoPruebasService=require('../services/InfoPruebasService');

const getInfoPruebas= async (req,res)=>{
    try{
        const{idOposicion,genero}=req.params; //Los datos suelen venir por params en una consulta GET
        
        if(!idOposicion||!genero){
            return res.status(400).json(
                {
                    ok: false,
                    msg:"Oposición o género no especificados"
                }
            )
        }
        const listaInfoOpo= await infoPruebasService.getInfoPruebas(idOposicion, genero); //Adquisición de la lista mediante su service

        if (!listaInfoOpo || listaInfoOpo.length === 0) {
            return res.status(404).json({
                ok: false,
                msg: "No se encontraron pruebas para esta oposición o género"
            });
        }

        res.status(200).json({
            ok: true,
            data: listaInfoOpo
        });
    }catch(error){
        console.error("Error en getInfoPruebas:", error.message);
        res.status(500).json({
            ok: false,
            msg: "Error al obtener la información de la oposición"
        })
    }
    
}
module.exports = { getInfoPruebas };