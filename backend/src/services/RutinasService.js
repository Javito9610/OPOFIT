const db = require('../config/db'); // importamos la conexión a la base de datos.


class RutinaService {
    
    /**
     * Calcular la nota media del usuario asignar un nivel.
     * @param {number} userId - El ID del usuario que consulta.
     * @param {number} idOposicion - La oposición (CNP o GC) que está preparando.
     */
    static async calcularNotaYNivel(userId, idOposicion) {
        
        // 1. OBTENCIÓN DE DATOS BÁSICOS
        // Necesitamos el género para saber qué tabla de baremos mirar (no es lo mismo 10 dominadas en H que en M).
        const [user] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
        if (!user || user.length === 0) throw new Error("Usuario no encontrado");
        const generoOriginal = user[0].genero;
        const generoDB = generoOriginal;

        // 2. OBTENCIÓN DE MARCAS DEL USUARIO
        // Traemos sus récords personales y los cruzamos con 'pruebas_oficiales' para saber 
        // si la prueba es de tiempo (mejor_si_es_menor = 1) o de fuerza/reps (0).
        const [marcas] = await db.query(`
            SELECT m.valord_record, p.id_pruebas_oficiales, p.mejor_si_es_menor
            FROM marcas_perfil m
            JOIN pruebas_oficiales p ON m.pruebas_oficiales_id_pruebas_oficiales = p.id_pruebas_oficiales
            WHERE m.usuarios_id_usuario = ? AND p.oposiciones_id_oposicion = ?
        `, [userId, idOposicion]);

        let sumaNotas = 0;
        let contadorPruebas = 0;

        // 3. EL BUCLE CALIFICADOR (Iteramos por cada marca que tiene el usuario)
        for (let m of marcas) {
            
            /* EXPLICACIÓN DE LA CONSULTA AL BAREMO:
               Buscamos en la tabla de baremos la nota más alta que el usuario ha conseguido.
               - Si la prueba es de TIEMPO (mejor_si_es_menor = 1): Buscamos que su marca sea MENOR o igual al baremo.
               - Si la prueba es de FUERZA (mejor_si_es_menor = 0): Buscamos que su marca sea MAYOR o igual al baremo.
               Usamos 'ORDER BY nota DESC LIMIT 1' para quedarnos con la mejor nota que ha alcanzado.
            */
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

        // 4. CÁLCULO DE LA NOTA MEDIA
        // Dividimos la suma de todas las notas entre el número de pruebas realizadas.
        const notaMedia = contadorPruebas > 0 ? (sumaNotas / contadorPruebas) : 0;

        // 5. ASIGNACIÓN LÓGICA DEL NIVEL DE ENTRENAMIENTO
        // Aquí aplicamos tu filosofía: ¿A qué nivel de entrenamiento le mandamos?
        let nivelSugerido = 'BASICO'; // Por defecto: Nivel Básico (Iniciación/Suspenso)

        if (notaMedia >= 5 && notaMedia < 8) {
            nivelSugerido = 'INTERMEDIO'; // Para los que ya aprueban pero quieren mejorar (Notable)
        } else if (notaMedia >= 8) {
            nivelSugerido = 'AVANZADO'; // Para los que buscan la excelencia (Sobresaliente)
        }

        // Devolvemos ambos datos para que el Controller decida qué mostrar
        return { 
            notaMedia: notaMedia.toFixed(2), // Limitamos a 2 decimales
            nivelSugerido,
            genero: generoOriginal
        };
    }

    /**
     * FUNCIÓN DE CARGA: Busca la rutina y sus ejercicios una vez sabemos el nivel.
     */
    static async obtenerRutinaCompleta(idOposicion, nivel, genero) {
        // Buscamos el encabezado de la rutina (nombre, nivel, etc.)
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