// Este archivo lógico contiene las tablas: rutina_pers y detalle_rutina_pers. PARA GESTIONAR LAS RUTINAS PERSONALES. NO HE METIDO EJERCICIOS POR EL PRINCIPIO DRY

class RutinaPers{
    constructor(id_rutina_pers, nombre_personalizado, usuarios_id_usuario){
        this.id_rutina_pers=id_rutina_pers;
        this.nombre_personalizado=nombre_personalizado;
        this.usuarios_id_usuario=usuarios_id_usuario;
    }
}

class DetallesRutinaPers{
    constructor(rutinas_pers_id_rutina_pers, ejercicios_id_ejercicio, series, repeticiones, id_detalle_rutina_pers){
        this.rutinas_pers_id_rutina_pers=rutinas_pers_id_rutina_pers;
        this.ejercicios_id_ejercicio=ejercicios_id_ejercicio;
        this.series=series;
        this.repeticiones=repeticiones;
        this.id_detalle_rutina_pers=id_detalle_rutina_pers;
    }
}

module.exports={
    RutinaPers,
    DetallesRutinaPers
}