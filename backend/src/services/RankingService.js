const db = require('../config/db');
const BaremoService = require('./BaremoService');
const UnidadPruebaHelper = require('../utils/UnidadPruebaHelper');

class RankingService {
  /** Notas de todas las pruebas oficiales de la oposición del usuario */
  static async notasUsuarioEnOposicion(userId, idOposicion) {
    const [marcas] = await db.query(
      `SELECT m.valord_record, m.fecha_logro, u.genero,
              p.id_pruebas_oficiales, p.nombre_prueba, p.mejor_si_es_menor, p.unidad_entrada
       FROM marcas_perfil m
       JOIN usuarios u ON m.usuarios_id_usuario = u.id_usuario
       JOIN pruebas_oficiales p ON m.pruebas_oficiales_id_pruebas_oficiales = p.id_pruebas_oficiales
       WHERE m.usuarios_id_usuario = ? AND p.oposiciones_id_oposicion = ?`,
      [userId, idOposicion]
    );
    const detalle = [];
    for (const m of marcas || []) {
      const nota = await BaremoService.calcularNotaPrueba(
        m.id_pruebas_oficiales,
        m.genero,
        m.valord_record
      );
      if (nota == null) continue;
      const unidad = UnidadPruebaHelper.resolver(m, m.genero);
      detalle.push({
        idPrueba: m.id_pruebas_oficiales,
        nombrePrueba: m.nombre_prueba,
        valor: Number(m.valord_record),
        nota,
        unidad,
        fechaLogro: m.fecha_logro
      });
    }
    return detalle;
  }

  /**
   * Ranking por oposición: solo usuarios de esa oposición con perfil público.
   * Orden: nota media global de todas sus pruebas registradas.
   */
  static async obtenerRanking(idOposicion, limite = 50) {
    const [usuarios] = await db.query(
      `SELECT u.id_usuario, u.nombre
       FROM usuarios u
       WHERE u.oposiciones_id_oposicion = ? AND u.perfil_publico = 1`,
      [idOposicion]
    );
    const filas = [];
    for (const u of usuarios || []) {
      const detalle = await RankingService.notasUsuarioEnOposicion(u.id_usuario, idOposicion);
      if (detalle.length === 0) continue;
      const suma = detalle.reduce((s, d) => s + d.nota, 0);
      const notaMedia = Number((suma / detalle.length).toFixed(2));
      filas.push({
        userId: u.id_usuario,
        nombre: u.nombre,
        notaMedia,
        pruebasCompletadas: detalle.length,
        totalPruebasOpo: detalle.length
      });
    }
    filas.sort((a, b) => b.notaMedia - a.notaMedia);
    return filas.slice(0, limite).map((r, i) => ({
      posicion: i + 1,
      userId: r.userId,
      nombre: r.nombre,
      notaMedia: r.notaMedia,
      pruebasCompletadas: r.pruebasCompletadas
    }));
  }

  static async detalleUsuario(userId, idOposicion) {
    const [u] = await db.query(
      `SELECT nombre, perfil_publico, oposiciones_id_oposicion FROM usuarios WHERE id_usuario = ?`,
      [userId]
    );
    if (!u?.length) return null;
    const user = u[0];
    if (Number(user.oposiciones_id_oposicion) !== Number(idOposicion)) {
      return { error: 'DISTINTA_OPOSICION' };
    }
    const detalle = await RankingService.notasUsuarioEnOposicion(userId, idOposicion);
    const notaMedia =
      detalle.length > 0
        ? Number((detalle.reduce((s, d) => s + d.nota, 0) / detalle.length).toFixed(2))
        : null;
    return {
      userId,
      nombre: user.nombre,
      perfilPublico: Number(user.perfil_publico) === 1,
      notaMedia,
      pruebas: detalle
    };
  }

  static async posicionUsuario(userId, idOposicion) {
    const ranking = await RankingService.obtenerRanking(idOposicion, 500);
    const idx = ranking.findIndex((r) => r.userId === userId);
    return idx >= 0
      ? { posicion: idx + 1, total: ranking.length, notaMedia: ranking[idx].notaMedia }
      : { posicion: null, total: ranking.length, notaMedia: null };
  }
}

module.exports = RankingService;
