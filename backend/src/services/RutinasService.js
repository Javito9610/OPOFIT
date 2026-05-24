const db = require('../config/db');
class RutinaService {
  static inferUnidad(nombre) {
    const n = String(nombre || '').toLowerCase();
    if (n.includes('min')) return 'min';
    if (n.includes('seg') || n.includes('sprint') || n.includes('segundo')) return 's';
    if (n.includes('km')) return 'km';
    if (n.includes('metro') || /(\d+)\s?m\b/.test(n)) return 'm';
    if (n.includes('natación') || n.includes('nadar')) return 'm';
    return 'reps';
  }
  static async calcularNotaYNivel(userId, idOposicion) {
    const [user] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
    if (!user || user.length === 0) {
      return {
        error: 'USER_NOT_FOUND'
      };
    }
    const generoOriginal = user[0].genero;
    const generoDB = generoOriginal;
    const [[{
      total_pruebas
    }]] = await db.query(`SELECT COUNT(*) AS total_pruebas
             FROM pruebas_oficiales
             WHERE oposiciones_id_oposicion = ?`, [idOposicion]);
    const MarcasPerfilService = require('./MarcasPerfilService');
    const BaremoService = require('./BaremoService');
    const marcas = await MarcasPerfilService.obtenerMarcasPorPrueba(userId, idOposicion);
    const total = Number(total_pruebas || 0);
    const completadas = new Set(marcas.map(m => m.id_pruebas_oficiales)).size;
    const faltan = Math.max(0, total - completadas);
    if (total > 0 && faltan > 0) {
      return {
        notaMedia: null,
        nivelSugerido: null,
        genero: generoOriginal,
        totalPruebas: total,
        pruebasCompletadas: completadas,
        pruebasFaltantes: faltan
      };
    }
    let sumaNotas = 0;
    for (const m of marcas) {
      const nota = await BaremoService.calcularNotaPrueba(
        m.id_pruebas_oficiales,
        generoDB,
        m.valord_record
      );
      sumaNotas += nota ?? 0;
    }
    const notaMedia = marcas.length > 0 ? sumaNotas / marcas.length : 0;
    let nivelSugerido = 'BASICO';
    if (notaMedia >= 5 && notaMedia < 8) {
      nivelSugerido = 'INTERMEDIO';
    } else if (notaMedia >= 8) {
      nivelSugerido = 'AVANZADO';
    }
    return {
      notaMedia: notaMedia.toFixed(2),
      nivelSugerido,
      genero: generoOriginal,
      totalPruebas: total,
      pruebasCompletadas: completadas,
      pruebasFaltantes: 0
    };
  }
  static async obtenerRutinaCompleta(idOposicion, nivel, genero) {
    const [rutinas] = await db.query(
      `SELECT id_rutina_opo, enfoque_tipo, nivel
       FROM rutinas_opo
       WHERE oposiciones_id_oposicion = ? AND nivel = ? AND genero = ?`,
      [idOposicion, nivel, genero]
    );
    if (!rutinas || rutinas.length === 0) return null;
    const planCompleto = [];
    for (let r of rutinas) {
      const [ejercicios] = await db.query(`
                SELECT e.id_ejercicio, e.nombre, e.video_url, d.series, d.repeticiones, d.descanso
                FROM detalle_rutina_opo d
                JOIN ejercicios e ON d.ejercicios_id_ejercicio = e.id_ejercicio
                WHERE d.rutinas_opo_id_rutina_opo = ?
            `, [r.id_rutina_opo]);
      planCompleto.push({
        id_rutina_opo: r.id_rutina_opo,
        bloque: r.enfoque_tipo,
        nivel: r.nivel,
        ejercicios: ejercicios.map(e => ({
          ...e,
          unidad: RutinaService.inferUnidad(e.nombre)
        }))
      });
    }
    return planCompleto;
  }
}
module.exports = RutinaService;
