const db = require('../config/db');
class RutinaService {
  static inferUnidad(nombre) {
    // Normalizamos quitando acentos para que "natación" vs "natacion" se trate igual.
    const n = String(nombre || '')
      .normalize('NFD')
      .replace(/\p{Diacritic}/gu, '')
      .toLowerCase();
    if (/\bmin\b/.test(n) || /\d+\s*min/.test(n)) return 'min';
    if (/\bseg\b/.test(n) || (n.includes('sprint') && /\d+\s*s\b/.test(n)) || n.includes('segundo')) return 's';
    if (/\bkm\b/.test(n) || /\d+\s*km/.test(n)) return 'km';
    if (n.includes('natacion') || n.includes('nadar')) return 'm';
    if (n.includes('metro') || /\d+\s*m\b/.test(n)) return 'm';
    if (/\b(run|running|tempo|jog|jogging|trote|rodaje|fartlek|carrera|trail|interval|hiit|marcha|rucking)\b/.test(n)) {
      return 'min';
    }
    return 'reps';
  }
  static async calcularNotaYNivel(userId, idOposicion) {
    const [user] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
    if (!user || user.length === 0) {
      return { error: 'USER_NOT_FOUND' };
    }
    const generoOriginal = user[0].genero;
    const generoDB = generoOriginal;
    const [[{ total_pruebas }]] = await db.query(
      'SELECT COUNT(*) AS total_pruebas FROM pruebas_oficiales WHERE oposiciones_id_oposicion = ?',
      [idOposicion]
    );
    const MarcasPerfilService = require('./MarcasPerfilService');
    const BaremoService = require('./BaremoService');
    const marcas = await MarcasPerfilService.obtenerMarcasPorPrueba(userId, idOposicion);
    const total = Number(total_pruebas || 0);
    const completadas = new Set(marcas.map((m) => m.id_pruebas_oficiales)).size;
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
    let countNotas = 0;
    for (const m of marcas) {
      const nota = await BaremoService.calcularNotaPrueba(
        m.id_pruebas_oficiales,
        generoDB,
        m.valord_record
      );
      if (nota != null) {
        sumaNotas += nota;
        countNotas++;
      }
    }
    // Denominator uses only pruebas with real baremo data, matching SimulacroService.
    const notaMedia = countNotas > 0 ? sumaNotas / countNotas : 0;
    let nivelSugerido = 'BASICO';
    if (notaMedia >= 5 && notaMedia < 8) nivelSugerido = 'INTERMEDIO';
    else if (notaMedia >= 8) nivelSugerido = 'AVANZADO';
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

    const candidatos = [];
    for (const r of rutinas) {
      const [ejercicios] = await db.query(
        `SELECT e.id_ejercicio, e.nombre, e.video_url, d.series, d.repeticiones, d.descanso
         FROM detalle_rutina_opo d
         JOIN ejercicios e ON d.ejercicios_id_ejercicio = e.id_ejercicio
         WHERE d.rutinas_opo_id_rutina_opo = ?`,
        [r.id_rutina_opo]
      );
      candidatos.push({
        rutina: r,
        ejercicios: ejercicios.map((e) => ({
          ...e,
          unidad: RutinaService.inferUnidad(e.nombre)
        }))
      });
    }

    // Tras importar planes pueden existir varias rutinas_opo con el mismo enfoque.
    // Devolvemos solo una por FUERZA / RESISTENCIA / VELOCIDAD (la más completa).
    const ordenEnfoque = ['FUERZA', 'RESISTENCIA', 'VELOCIDAD'];
    const mejorPorEnfoque = new Map();
    for (const c of candidatos) {
      const key = c.rutina.enfoque_tipo;
      const prev = mejorPorEnfoque.get(key);
      if (
        !prev
        || c.ejercicios.length > prev.ejercicios.length
        || (c.ejercicios.length === prev.ejercicios.length
          && c.rutina.id_rutina_opo < prev.rutina.id_rutina_opo)
      ) {
        mejorPorEnfoque.set(key, c);
      }
    }

    return ordenEnfoque
      .filter((enfoque) => mejorPorEnfoque.has(enfoque))
      .map((enfoque) => {
        const c = mejorPorEnfoque.get(enfoque);
        return {
          id_rutina_opo: c.rutina.id_rutina_opo,
          bloque: c.rutina.enfoque_tipo,
          nivel: c.rutina.nivel,
          ejercicios: c.ejercicios
        };
      });
  }
}
module.exports = RutinaService;
