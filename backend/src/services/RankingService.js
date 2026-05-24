const db = require('../config/db');
const BaremoService = require('./BaremoService');
const MarcasPerfilService = require('./MarcasPerfilService');
const UnidadPruebaHelper = require('../utils/UnidadPruebaHelper');

class RankingService {
  static async totalPruebasOposicion(idOposicion) {
    const [[row]] = await db.query(
      'SELECT COUNT(*) AS n FROM pruebas_oficiales WHERE oposiciones_id_oposicion = ?',
      [idOposicion]
    );
    return Number(row?.n || 0);
  }

  static async notasUsuarioEnOposicion(userId, idOposicion) {
    const [user] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
    const genero = user?.[0]?.genero || 'HOMBRE';
    const marcas = await MarcasPerfilService.obtenerMarcasPorPrueba(userId, idOposicion);
    const detalle = [];
    for (const m of marcas) {
      const nota = await BaremoService.calcularNotaPrueba(
        m.id_pruebas_oficiales,
        genero,
        m.valord_record
      );
      detalle.push({
        idPrueba: m.id_pruebas_oficiales,
        nombrePrueba: m.nombre_prueba,
        valor: Number(m.valord_record),
        nota: nota ?? 0,
        unidad: UnidadPruebaHelper.resolver(m, genero),
        fechaLogro: m.fecha_logro
      });
    }
    return detalle;
  }

  static async obtenerRanking(idOposicion, limite = 50) {
    const totalOficial = await RankingService.totalPruebasOposicion(idOposicion);
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
      const divisor = detalle.length;
      const notaMedia = Number((suma / divisor).toFixed(2));
      filas.push({
        userId: u.id_usuario,
        nombre: u.nombre,
        notaMedia,
        pruebasCompletadas: detalle.length,
        totalPruebasOpo: totalOficial
      });
    }
    filas.sort((a, b) => b.notaMedia - a.notaMedia);
    return filas.slice(0, limite).map((r, i) => ({
      posicion: i + 1,
      userId: r.userId,
      nombre: r.nombre,
      notaMedia: r.notaMedia,
      pruebasCompletadas: r.pruebasCompletadas,
      totalPruebasOpo: r.totalPruebasOpo
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
    const totalOficial = await RankingService.totalPruebasOposicion(idOposicion);
    const notaMedia =
      detalle.length > 0
        ? Number((detalle.reduce((s, d) => s + d.nota, 0) / detalle.length).toFixed(2))
        : null;
    return {
      userId,
      nombre: user.nombre,
      perfilPublico: Number(user.perfil_publico) === 1,
      notaMedia,
      pruebasCompletadas: detalle.length,
      totalPruebasOpo: totalOficial,
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
