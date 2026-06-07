const db = require('../config/db');
const EntornoEntreno = require('../utils/EntornoEntreno');
const EjercicioInteligenteService = require('./EjercicioInteligenteService');

function enriquecerInstrucciones(rows) {
  return rows.map((e) => {
    const instr = String(e.instrucciones_tecnicas || '').trim();
    if (instr.length > 24) return e;
    return {
      ...e,
      instrucciones_tecnicas: EjercicioInteligenteService.generarInstrucciones(e)
    };
  });
}

class EjerciciosService {
  static async listarTodos(filtros = {}) {
    const { categoria, pilar, busqueda, grupo_muscular, entorno, limite = 1000 } = filtros;
    let sql = `SELECT id_ejercicio, nombre, video_url, instrucciones_tecnicas,
                      categoria, pilar, grupo_muscular, equipamiento, entornos, tipo_ilustracion
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
    if (grupo_muscular && String(grupo_muscular).trim()) {
      sql += ' AND (grupo_muscular = ? OR grupo_muscular LIKE ?)';
      const g = String(grupo_muscular).trim();
      params.push(g, `%${g}%`);
    }
    if (busqueda && String(busqueda).trim()) {
      sql += ' AND (nombre LIKE ? OR grupo_muscular LIKE ? OR equipamiento LIKE ?)';
      const q = `%${String(busqueda).trim()}%`;
      params.push(q, q, q);
    }
    sql += ' ORDER BY pilar ASC, categoria ASC, nombre ASC LIMIT ?';
    params.push(Math.min(Number(limite) || 1000, 1000));

    const [rows] = await db.query(sql, params);
    const ent = EntornoEntreno.normalizarEntorno(entorno);
    const filtrados = !ent || ent === 'MIXTO'
      ? rows
      : rows.filter((e) => {
          const csv = e.entornos || EntornoEntreno.inferirEntornosDesdeEquipamiento(e.equipamiento, e.pilar).join(',');
          return EntornoEntreno.ejercicioCompatible(csv, ent);
        });
    return enriquecerInstrucciones(filtrados);
  }

  static async obtenerPorId(idEjercicio) {
    const [rows] = await db.query(
      `SELECT id_ejercicio, nombre, video_url, instrucciones_tecnicas,
              categoria, pilar, grupo_muscular, equipamiento, entornos, tipo_ilustracion
       FROM ejercicios WHERE id_ejercicio = ? LIMIT 1`,
      [idEjercicio]
    );
    if (!rows.length) return null;
    return enriquecerInstrucciones(rows)[0];
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
