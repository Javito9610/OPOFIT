class RutinaOpo{
    constructor(id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion){
        this.id_rutina_opo=id_rutina_opo;
        this.nivel=nivel;
        this.genero=genero;
        this.enfoque_tipo=enfoque_tipo;
        this.oposiciones_id_oposicion=oposiciones_id_oposicion;
    }
}

class DetalleRutinaOpo{
    constructor(ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, id_detalle_rutina_opo, repeticiones, series, descanso){
        this.ejercicios_id_ejercicio= ejercicios_id_ejercicio;
        this.rutinas_opo_id_rutina_opo=rutinas_opo_id_rutina_opo;
        this.id_detalle_rutina_opo= id_detalle_rutina_opo;
        this.repeticiones=repeticiones;
        this.series=series;
        this.descanso=descanso;
    }
}

class Ejercicio{
    constructor(id_ejercicio, nombre, video_url, instrucciones_tecnicas){
        this.id_ejercicio= id_ejercicio;
        this.nombre=nombre;
        this.video_url=video_url;
        this.instrucciones_tecnicas=instrucciones_tecnicas;
    }
}

module.exports={
    RutinaOpo,
    DetalleRutinaOpo,
    Ejercicio
}
