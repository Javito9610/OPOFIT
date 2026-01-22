//Este archivo lógico contendrá las tablas: historial_sesiones, registro_resultados. SE ENFOCA EN EL PROGRESO DE AMBOS ENTRENAMIENTOS (PERS/OPO).

class HistorialSesion{
    constructor(id_historial_sesion, fecha_entreno, tipo_rutina, duracion_oficial, rutinas_pers_id_rutina_pers, rutinas_opo_id_rutina_opo, usuarios_id_usuario){
        this.id_historial_sesion=id_historial_sesion;
        this.fecha_entreno=fecha_entreno;
        this.tipo_rutina=tipo_rutina;
        this.duracion_oficial=duracion_oficial;
        this.rutinas_pers_id_rutina_pers=rutinas_pers_id_rutina_pers;
        this.rutinas_opo_id_rutina_opo=rutinas_opo_id_rutina_opo;
        this.usuarios_id_usuario=usuarios_id_usuario;
    }
}

class RegistroResultado{
    constructor(ejercicios_id_ejercicio, historial_sesiones_id_historial_sesiones, id_resultado, valor_conseguido){
        this.ejercicios_id_ejercicio=ejercicios_id_ejercicio;
        this.historial_sesiones_id_historial_sesiones=historial_sesiones_id_historial_sesiones;
        this.id_resultado=id_resultado;
        this.valor_conseguido=valor_conseguido;
    }
}

module.exports={
    HistorialSesion,
    RegistroResultado
}