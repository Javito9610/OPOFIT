const db = require('../config/db');

class EjerciciosService {
  static async listarTodos(filtros = {}) {
    const { categoria, pilar, busqueda, limite = 500 } = filtros;
    let sql = `SELECT id_ejercicio, nombre, video_url, instrucciones_tecnicas,
                      categoria, pilar, grupo_muscular, equipamiento
               FROM ejercicios WHERE 1=1`;
    const params = [];

    if (categoria) {
      sql += ' AND categoria = ?';
      params.push(categoria);
    }
    if (pilar) {
      sql += ' AND pilar = ?';
      params.push(pilar);
    }
    if (busqueda && String(busqueda).trim()) {
      sql += ' AND (nombre LIKE ? OR grupo_muscular LIKE ? OR equipamiento LIKE ?)';
      const q = `%${String(busqueda).trim()}%`;
      params.push(q, q, q);
    }
    sql += ' ORDER BY pilar ASC, categoria ASC, nombre ASC LIMIT ?';
    params.push(Math.min(Number(limite) || 500, 500));

    const [rows] = await db.query(sql, params);
    return rows;
  }

  static async listarCategorias() {
    const [rows] = await db.query(
      `SELECT DISTINCT categoria FROM ejercicios WHERE categoria IS NOT NULL ORDER BY categoria`
    );
    const [pilares] = await db.query(
      `SELECT DISTINCT pilar FROM ejercicios WHERE pilar IS NOT NULL ORDER BY pilar`
    );
    return {
      categorias: rows.map((r) => r.categoria),
      pilares: pilares.map((r) => r.pilar)
    };
  }
}

module.exports = EjerciciosService;
