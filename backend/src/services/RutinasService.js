const db = require('../config/db');

class RutinaService {
    
    static async calcularNotaYNivel(userId, idOposicion) {
        
        const [user] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
        if (!user || user.length === 0) throw new Error("Usuario no encontrado");
        const generoOriginal = user[0].genero;
        const generoDB = generoOriginal;

        const [marcas] = await db.query(`
            SELECT m.valord_record, p.id_pruebas_oficiales, p.mejor_si_es_menor
            FROM marcas_perfil m
            JOIN pruebas_oficiales p ON m.pruebas_oficiales_id_pruebas_oficiales = p.id_pruebas_oficiales
            WHERE m.usuarios_id_usuario = ? AND p.oposiciones_id_oposicion = ?
        `, [userId, idOposicion]);

        let sumaNotas = 0;
        let contadorPruebas = 0;

        for (let m of marcas) {
            
            const [baremoResultado] = await db.query(`
                SELECT b.nota 
                FROM baremos_puntuacion b
                JOIN pruebas_oficiales p ON b.pruebas_oficiales_id_pruebas_oficiales = p.id_pruebas_oficiales
                WHERE b.pruebas_oficiales_id_pruebas_oficiales = ? 
                AND b.genero = ? 
                AND (
                    (p.mejor_si_es_menor = 1 AND ? <= b.marca_valor)
                    OR 
                    (p.mejor_si_es_menor = 0 AND ? >= b.marca_valor)
                )
                ORDER BY b.nota DESC 
                LIMIT 1`, 
                [
                    m.id_pruebas_oficiales, 
                    generoDB, 
                    m.valord_record,
                    m.valord_record
                ]
            );

            if (baremoResultado && baremoResultado.length > 0) {
                sumaNotas += baremoResultado[0].nota;
                contadorPruebas++;
            }
        }

        const notaMedia = contadorPruebas > 0 ? (sumaNotas / contadorPruebas) : 0;

        let nivelSugerido = 'BASICO';

        if (notaMedia >= 5 && notaMedia < 8) {
            nivelSugerido = 'INTERMEDIO';
        } else if (notaMedia >= 8) {
            nivelSugerido = 'AVANZADO';
        }

        return { 
            notaMedia: notaMedia.toFixed(2),
            nivelSugerido,
            genero: generoOriginal
        };
    }

    static async obtenerRutinaCompleta(idOposicion, nivel, genero) {
        const [rutinas] = await db.query(
            'SELECT DISTINCT id_rutina_opo, enfoque_tipo FROM rutinas_opo WHERE oposiciones_id_oposicion = ? AND nivel = ? AND genero = ?',
            [idOposicion, nivel, genero]
        );

        if (!rutinas || rutinas.length === 0) return null;
        const planCompleto=[];

        for(let r of rutinas){
            const [ejercicios] = await db.query(`
                SELECT e.id_ejercicio, e.nombre, e.video_url, d.series, d.repeticiones, d.descanso
                FROM detalle_rutina_opo d
                JOIN ejercicios e ON d.ejercicios_id_ejercicio = e.id_ejercicio
                WHERE d.rutinas_opo_id_rutina_opo = ?
            `, [r.id_rutina_opo]);

                planCompleto.push({
                    id_rutina_opo: r.id_rutina_opo,
                    bloque: r.enfoque_tipo,
                    ejercicios: ejercicios
                });
        }
        return planCompleto
    }
}

module.exports = RutinaService;
