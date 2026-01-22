// En este archivo lógico encotraremos los modelos: Ususario, settings y marcas_perfil. ENFOCADO EN EL USER
class Usuario{
    constructor(id_usuario, nombre, email, password, genero, peso, altura, imc, fecha_registro, oposiciones_id_oposicion){
        this.id_usuario=id_usuario;
        this.nombre=nombre;
        this.email=email;
        this.password=password;
        this.genero=genero;
        this.peso=peso;
        this.altura=altura;
        this.imc=imc;
        this.fecha_registro=fecha_registro;
        this.oposiciones_id_oposicion=oposiciones_id_oposicion;

    }
}

class Settings{
    constructor(id_setting, unidad_peso, unidad_distancia, usuarios_id_usuario){
        this.id_setting=id_setting;
        this.unidad_peso=unidad_peso;
        this.unidad_distancia=unidad_distancia;
        this.usuarios_id_usuario=usuarios_id_usuario;
    }
}

class MarcasPerfil{
    constructor(id_marcas_perfil, valord_record, fecha_logro, pruebas_oficiales_id_pruebas_oficiales, usuarios_id_usuario){
        this.id_marcas_perfil= id_marcas_perfil;
        this.valord_record=valord_record;
        this.fecha_logro=fecha_logro;
        this.pruebas_oficiales_id_pruebas_oficiales=pruebas_oficiales_id_pruebas_oficiales;
        this.usuarios_id_usuario=usuarios_id_usuario;

    }
}

module.exports={
    Usuario,
    Settings,
    MarcasPerfil
};