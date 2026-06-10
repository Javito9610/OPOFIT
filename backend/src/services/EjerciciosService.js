const db = require('../config/db');
const EntornoEntreno = require('../utils/EntornoEntreno');
const EjercicioInteligenteService = require('./EjercicioInteligenteService');

/**
 * Mapeo equipamiento (string libre del banco) → códigos canonical de material.
 * Sirve para responder: "¿el usuario que solo tiene KB+COMBA puede hacer este ejercicio?"
 */
const EQUIP_MAP = [
  { re: /barra dominadas|barra horizontal|barra vertical/i,    cod: 'BARRA_DOMINADAS' },
  { re: /barra olímpica|barra olimpica|barra\+banco|banco\+barra|barra\b/i, cod: 'BARRA_OLIMPICA' },
  { re: /\bkb\b|kettlebell/i,                                  cod: 'KB' },
  { re: /mancuerna/i,                                          cod: 'MANCUERNAS' },
  { re: /\btrx\b/i,                                            cod: 'TRX' },
  { re: /anilla/i,                                             cod: 'ANILLAS' },
  { re: /goma/i,                                               cod: 'GOMAS' },
  { re: /comba/i,                                              cod: 'COMBA' },
  { re: /saco|sandbag/i,                                       cod: 'SACO' },
  { re: /foam roller|foam/i,                                   cod: 'FOAM' },
  { re: /banco/i,                                              cod: 'BANCO' },
  { re: /caja|box/i,                                           cod: 'CAJA' },
  { re: /bici|bicicleta/i,                                     cod: 'BICI' },
  { re: /remo concept|remo c2|remo$/i,                         cod: 'REMO' },
  { re: /echo bike|assault bike/i,                             cod: 'ECHO_BIKE' },
  { re: /ski erg/i,                                            cod: 'SKI_ERG' },
  { re: /piscina/i,                                            cod: 'PISCINA' },
  { re: /pista|tartán|tartan|atletismo/i,                      cod: 'PISTA' },
  { re: /montaña|montana|trail|sendero|monte/i,                cod: 'MONTANA' }
];

function inferirMaterialDesdeEquip(equip) {
  if (!equip || equip === '—') return ['NADA'];
  const lst = [];
  for (const m of EQUIP_MAP) {
    if (m.re.test(equip)) lst.push(m.cod);
  }
  return lst.length ? lst : ['NADA'];
}

function esEquipamientoCubierto(equip, userMaterial) {
  // No material requerido → siempre cubierto
  const reqs = inferirMaterialDesdeEquip(equip);
  if (reqs.includes('NADA') && reqs.length === 1) return true;
  // El usuario tiene al menos UNO de los materiales requeridos
  return reqs.some((r) => userMaterial.includes(r));
}

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
  /**
   * Filtra ejercicios. Acepta:
   *  - categoria, pilar, busqueda, grupo_muscular, entorno: filtros básicos.
   *  - modalidad: wod | calistenia | emom | amrap | tabata | for_time |
   *               crossfit_lift | movilidad | cardio | convencional.
   *  - material: CSV de códigos (KB,MANCUERNAS,GOMAS,...) — devuelve solo
   *              ejercicios cuyo equipamiento se cubra con el material que
   *              el usuario marcó como disponible. NADA == solo peso corporal.
   */
  static async listarTodos(filtros = {}) {
    const { categoria, pilar, busqueda, grupo_muscular, entorno, modalidad, material, limite = 1000 } = filtros;
    let sql = `SELECT id_ejercicio, nombre, video_url, instrucciones_tecnicas,
                      categoria, pilar, grupo_muscular, equipamiento, entornos, tipo_ilustracion,
                      modalidad, score_tipo
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
    if (modalidad) {
      sql += ' AND modalidad = ?';
      params.push(String(modalidad).trim().toLowerCase());
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
    let filtrados = !ent || ent === 'MIXTO'
      ? rows
      : rows.filter((e) => {
          const csv = e.entornos || EntornoEntreno.inferirEntornosDesdeEquipamiento(e.equipamiento, e.pilar).join(',');
          return EntornoEntreno.ejercicioCompatible(csv, ent)
              && EntornoEntreno.ejercicioRealistaParaEntorno(e.nombre, e.equipamiento, ent);
        });

    // Filtrado por material disponible (CSV de códigos)
    if (material && String(material).trim()) {
      const userMat = String(material).toUpperCase().split(',').map((m) => m.trim()).filter(Boolean);
      // Si tiene GIMNASIO_COMPLETO o nada → no filtra
      if (!userMat.includes('GIMNASIO_COMPLETO')) {
        filtrados = filtrados.filter((e) => esEquipamientoCubierto(e.equipamiento, userMat));
      }
    }

    return enriquecerInstrucciones(filtrados);
  }

  static async obtenerPorId(idEjercicio) {
    const [rows] = await db.query(
      `SELECT id_ejercicio, nombre, video_url, instrucciones_tecnicas,
              categoria, pilar, grupo_muscular, equipamiento, entornos, tipo_ilustracion,
              modalidad, score_tipo
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
