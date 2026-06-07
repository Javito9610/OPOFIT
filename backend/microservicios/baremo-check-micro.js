/**
 * BaremoCheckMicro
 *
 * Microservicio que comprueba que los baremos de cada oposición no se han
 * corrompido ni desviado de los datos esperados. Por cada oposición:
 *
 * 1) Cuenta filas por prueba en `baremos_puntuacion` (sanity check).
 * 2) Detecta pruebas sin baremo (filas = 0).
 * 3) Detecta inversiones (notas no monótonas respecto a marca_valor).
 * 4) Emite un informe consultable + warning si hay desviaciones.
 *
 * Cuando hay inversiones, se loguea para revisión manual: el operador
 * humano decide si re-importar el seed oficial.
 *
 * Útil para detectar regresiones por migraciones futuras o seeds rotos.
 */
const db = require('../src/config/db');

async function comprobarOposicion(idOposicion) {
  const [pruebas] = await db.query(
    `SELECT po.id_pruebas_oficiales AS id, po.nombre_prueba, po.mejor_si_es_menor
       FROM pruebas_oficiales po
       WHERE po.oposiciones_id_oposicion = ?`,
    [idOposicion]
  );
  if (!pruebas.length) {
    return { idOposicion, error: 'sin_pruebas_oficiales' };
  }

  const informe = {
    idOposicion,
    pruebas: pruebas.length,
    sin_baremo: [],
    inversiones: [],
    huecos_notas: []
  };

  for (const p of pruebas) {
    const [filasH] = await db.query(
      `SELECT marca_valor, nota
         FROM baremos_puntuacion
        WHERE pruebas_oficiales_id_pruebas_oficiales = ? AND genero = 'HOMBRE'
        ORDER BY marca_valor ASC`,
      [p.id]
    );
    const [filasM] = await db.query(
      `SELECT marca_valor, nota
         FROM baremos_puntuacion
        WHERE pruebas_oficiales_id_pruebas_oficiales = ? AND genero = 'MUJER'
        ORDER BY marca_valor ASC`,
      [p.id]
    );

    for (const [genero, filas] of [['HOMBRE', filasH], ['MUJER', filasM]]) {
      if (!filas.length) {
        informe.sin_baremo.push({ prueba: p.nombre_prueba, genero });
        continue;
      }
      // Inversión: con mejor_si_es_menor=1 la nota debe DECRECER al subir marca_valor;
      // con mejor_si_es_menor=0 debe CRECER.
      const decreciente = Number(p.mejor_si_es_menor) === 1;
      for (let i = 1; i < filas.length; i++) {
        const prev = Number(filas[i - 1].nota);
        const cur = Number(filas[i].nota);
        if (decreciente && cur > prev) {
          informe.inversiones.push({ prueba: p.nombre_prueba, genero, idx: i });
          break;
        }
        if (!decreciente && cur < prev) {
          informe.inversiones.push({ prueba: p.nombre_prueba, genero, idx: i });
          break;
        }
      }
      // Detecta huecos en la cobertura de notas (esperamos entre 0 y 10).
      const notas = filas.map((f) => Number(f.nota));
      const min = Math.min(...notas);
      const max = Math.max(...notas);
      if (min > 5 || max < 8) {
        informe.huecos_notas.push({
          prueba: p.nombre_prueba,
          genero,
          rango_notas: [min, max]
        });
      }
    }
  }

  informe.ok =
    informe.sin_baremo.length === 0 &&
    informe.inversiones.length === 0 &&
    informe.huecos_notas.length === 0;
  return informe;
}

async function ejecutar({ logger = console } = {}) {
  const inicio = Date.now();
  const [opos] = await db.query('SELECT id_oposicion FROM oposiciones ORDER BY id_oposicion ASC');
  const informes = [];
  for (const { id_oposicion } of opos) {
    try {
      const r = await comprobarOposicion(id_oposicion);
      informes.push(r);
      if (!r.ok) {
        logger.warn?.(
          `[baremo-check] opo ${id_oposicion} con desviaciones:`,
          JSON.stringify({
            sin_baremo: r.sin_baremo.length,
            inversiones: r.inversiones.length,
            huecos_notas: r.huecos_notas.length
          })
        );
      }
    } catch (e) {
      logger.warn?.(`[baremo-check] opo ${id_oposicion} error: ${e.message}`);
      informes.push({ idOposicion: id_oposicion, error: e.message });
    }
  }

  const resumen = {
    ejecucion_ms: Date.now() - inicio,
    oposiciones: informes.length,
    con_desviaciones: informes.filter((i) => !i.ok && !i.error).length,
    con_error: informes.filter((i) => i.error).length,
    informes
  };
  logger.log?.(
    '[baremo-check] OK',
    JSON.stringify({
      oposiciones: resumen.oposiciones,
      con_desviaciones: resumen.con_desviaciones,
      con_error: resumen.con_error
    })
  );
  return resumen;
}

module.exports = { ejecutar, comprobarOposicion };
