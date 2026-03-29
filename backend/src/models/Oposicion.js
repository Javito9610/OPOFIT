//Este archivo lógico contendrá las tablas:Oposiciones, pruebas_oficiales, requisitos_nivel, noticias ENFOCADO EN LA OPOSICIÓN
class Oposicion{
    constructor(id_oposicion, nombre){
        this.id_oposicion=id_oposicion;
        this.nombre=nombre;
    }
}

class PruebaOficial{
    constructor(id_pruebas_oficiales, nombre_prueba, descripcion, trucos, oposiciones_id_oposicion, mejor_si_es_menor){
        this.id_pruebas_oficiales=id_pruebas_oficiales;
        this.nombre_prueba=nombre_prueba;
        this.descripcion=descripcion;
        this.trucos=trucos;
        this.oposiciones_id_oposicion=oposiciones_id_oposicion;
        this.mejor_si_es_menor = mejor_si_es_menor;
    }
}

class RequisitoNivel{
    constructor(id_requisito_nivel, genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales){
        this.id_requisito_nivel=id_requisito_nivel;
        this.genero=genero;
        this.nivel_exigencia=nivel_exigencia;
        this.valor_objetivo=valor_objetivo;
        this.pruebas_oficiales_id_pruebas_oficiales=pruebas_oficiales_id_pruebas_oficiales;
    }
}

class Noticia{
    constructor(id_noticia, titulo, contenido, fecha_publicacion, oposiciones_id_oposicion){
        this.id_noticia= id_noticia;
        this.titulo=titulo;
        this.contenido=contenido;
        this.fecha_publicacion=fecha_publicacion;
        this.oposiciones_id_oposicion = oposiciones_id_oposicion;
    }
}

class BaremoPuntuacion {
    constructor(id_baremo, pruebas_id, genero, marca_valor, nota) {
        this.id_baremo = id_baremo;
        this.pruebas_id = pruebas_id;
        this.genero = genero;
        this.marca_valor = marca_valor;
        this.nota = nota;
    }
}

module.exports={
    Oposicion,
    PruebaOficial,
    RequisitoNivel,
    Noticia,
    BaremoPuntuacion
}