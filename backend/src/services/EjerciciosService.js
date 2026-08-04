const db = require('../config/db');
const EntornoEntreno = require('../utils/EntornoEntreno');
const EjercicioInteligenteService = require('./EjercicioInteligenteService');
const { CURADAS } = require('./EjercicioExplicacionesCuradas');
const { getVideoUrl } = require('./EjercicioVideoService');

function normNombre(s) {
  return String(s || '').normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim();
}

/**
 * Mapeo equipamiento (string libre del banco) → códigos canonical de material.
 * Sirve para responder: "¿el usuario que solo tiene KB+COMBA puede hacer este ejercicio?"
 */
const EQUIP_MAP = [
  { re: /barra dominadas|barra horizontal|barra vertical|barra fija|paralelas/i, cod: 'BARRA_DOMINADAS' },
  { re: /barra olímpica|barra olimpica|barra\+banco|banco\+barra|barra\b/i, cod: 'BARRA_OLIMPICA' },
  { re: /\bkb\b|kettlebell/i,                                  cod: 'KB' },
  { re: /mancuerna/i,                                          cod: 'MANCUERNAS' },
  { re: /\btrx\b/i,                                            cod: 'TRX' },
  { re: /anilla/i,                                             cod: 'ANILLAS' },
  { re: /goma|banda el[aá]stica|\bbanda\b/i,                    cod: 'GOMAS' },
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
    // Contenido COMPLETO para el detalle del ejercicio: técnica (setup/ejecución/
    // cues/errores), "por qué" y objetivo. Antes el banco solo traía
    // instrucciones planas → las pestañas "Por qué" y "Técnica" salían vacías.
    let rico = null;
    try {
      rico = EjercicioInteligenteService.aplicarInteligencia(
        {
          nombre: e.nombre,
          id_ejercicio: e.id_ejercicio,
          pilar: e.pilar,
          grupo_muscular: e.grupo_muscular,
          categoria: e.categoria,
          equipamiento: e.equipamiento
        },
        { seed: e.id_ejercicio }
      );
    } catch (_) { /* si algo falla, seguimos con lo básico */ }

    const instr = String(e.instrucciones_tecnicas || '').trim();

    // Prioridad de la explicación: curada a mano (dentro de rico) > cache IA
    // (columna explicacion_json) > patrón personalizado (rico).
    let explicacion = rico?.explicacion || null;
    if (!CURADAS[normNombre(e.nombre)] && e.explicacion_json) {
      try {
        const cache = JSON.parse(e.explicacion_json);
        if (cache && (cache.setup || cache.porque)) explicacion = cache;
      } catch (_) { /* json corrupto: usamos el de patrón */ }
    }

    const { explicacion_json, ...limpio } = e; // no exponemos el JSON crudo
    // Vídeo YouTube curado (correcto y verificado) cuando la BD no tiene uno:
    // así el botón "Vídeo" del detalle abre la demostración EXACTA del ejercicio.
    const videoCurado = (e.video_url && String(e.video_url).startsWith('http'))
      ? e.video_url
      : (getVideoUrl(e.nombre)?.url || null);
    return {
      ...limpio,
      video_url: videoCurado,
      instrucciones_tecnicas: instr.length > 24
        ? instr
        : (rico?.instrucciones_tecnicas || EjercicioInteligenteService.generarInstrucciones(e)),
      explicacion,
      objetivo: rico?.objetivo || null
    };
  });
}

class EjerciciosService {
  /**
   * ¿El ejercicio se puede hacer con el material que tiene el usuario?
   * Reutilizable desde el generador de plan. userMat = array de códigos
   * (['MANCUERNAS','COMBA',...]). GIMNASIO_COMPLETO o lista vacía = todo cubierto.
   */
  static cubreMaterial(ejercicio, userMat) {
    if (!Array.isArray(userMat) || userMat.length === 0) return true;
    if (userMat.includes('GIMNASIO_COMPLETO')) return true;
    const equip = ejercicio.equipamiento
      || EntornoEntreno.clasificarEntornos(ejercicio.nombre, ejercicio.equipamiento, ejercicio.pilar).equip;
    return esEquipamientoCubierto(equip, userMat);
  }

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
                      modalidad, score_tipo, explicacion_json
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
      : rows.filter((e) =>
          // Clasificador POR NOMBRE (autoritativo) + filtro defensivo. Antes se
          // usaba el CSV almacenado (poco fiable en el catálogo base) y colaba
          // "press banca" en calistenia. Ahora manda el nombre.
          EntornoEntreno.ejercicioCompatiblePorNombre(e, ent)
            && EntornoEntreno.ejercicioRealistaParaEntorno(e.nombre, e.equipamiento, ent)
        );

    // Filtrado por material disponible (CSV de códigos)
    if (material && String(material).trim()) {
      const userMat = String(material).toUpperCase().split(',').map((m) => m.trim()).filter(Boolean);
      // Si tiene GIMNASIO_COMPLETO o nada → no filtra
      if (!userMat.includes('GIMNASIO_COMPLETO')) {
        filtrados = filtrados.filter((e) => {
          // El catálogo base tiene equipamiento NULL → usamos el equip que
          // deduce el clasificador por nombre para que el filtro de material
          // funcione igualmente (mancuernas, kettlebell, barra, comba...).
          const equip = e.equipamiento || EntornoEntreno.clasificarEntornos(e.nombre, e.equipamiento, e.pilar).equip;
          return esEquipamientoCubierto(equip, userMat);
        });
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
