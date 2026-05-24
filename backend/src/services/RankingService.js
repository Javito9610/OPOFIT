const db = require('../config/db');
const BaremoService = require('./BaremoService');

class RankingService {
  static async obtenerRanking(idOposicion, idPrueba = null, limite = 50) {
    let sql = `
      SELECT u.id_usuario, u.nombre, m.valord_record, m.fecha_logro,
             p.id_pruebas_oficiales, p.nombre_prueba, p.mejor_si_es_menor, u.genero
      FROM marcas_perfil m
      JOIN usuarios u ON m.usuarios_id_usuario = u.id_usuario
      JOIN pruebas_oficiales p ON m.pruebas_oficiales_id_pruebas_oficiales = p.id_pruebas_oficiales
      WHERE p.oposiciones_id_oposicion = ? AND u.perfil_publico = 1`;
    const params = [idOposicion];
    if (idPrueba) {
      sql += ' AND p.id_pruebas_oficiales = ?';
      params.push(idPrueba);
    }
    const [marcas] = await db.query(sql, params);
    const enriched = [];
    for (const m of marcas) {
      const nota = await BaremoService.calcularNotaPrueba(
        m.id_pruebas_oficiales,
        m.genero,
        m.valord_record
      );
      enriched.push({
        userId: m.id_usuario,
        nombre: m.nombre,
        idPrueba: m.id_pruebas_oficiales,
        nombrePrueba: m.nombre_prueba,
        valor: Number(m.valord_record),
        mejorSiEsMenor: Number(m.mejor_si_es_menor) === 1,
        nota: nota ?? 0,
        fechaLogro: m.fecha_logro
      });
    }
    enriched.sort((a, b) => {
      if (b.nota !== a.nota) return b.nota - a.nota;
      if (a.mejorSiEsMenor) return a.valor - b.valor;
      return b.valor - a.valor;
    });
    return enriched.slice(0, limite).map((row, idx) => ({
      posicion: idx + 1,
      userId: row.userId,
      nombre: row.nombre,
      idPrueba: row.idPrueba,
      nombrePrueba: row.nombrePrueba,
      valor: row.valor,
      nota: row.nota,
      unidad: row.mejorSiEsMenor ? 's' : 'reps'
    }));
  }

  static async posicionUsuario(userId, idOposicion, idPrueba) {
    const ranking = await RankingService.obtenerRanking(idOposicion, idPrueba, 500);
    const idx = ranking.findIndex((r) => r.userId === userId);
    return idx >= 0 ? { posicion: idx + 1, total: ranking.length } : { posicion: null, total: ranking.length };
  }
}

module.exports = RankingService;
