const db = require('../config/db');
const BaremoService = require('./BaremoService');
const RutinaService = require('./RutinasService');
const UnidadPruebaHelper = require('../utils/UnidadPruebaHelper');
const MarcaValidator = require('../utils/MarcaValidator');

class MarcasPerfilService {
  static esMejorMarca(mejorSiEsMenor, valorNuevo, valorAnterior) {
    if (valorAnterior == null || valorAnterior === undefined) return true;
    const menor = Number(mejorSiEsMenor) === 1;
    if (menor) return Number(valorNuevo) < Number(valorAnterior);
    return Number(valorNuevo) > Number(valorAnterior);
  }

  /** Última marca por prueba (evita duplicados en ranking) */
  static async obtenerMarcasPorPrueba(userId, idOposicion) {
    const [rows] = await db.query(
      `SELECT m.id_marcas_perfil, m.valord_record, m.fecha_logro,
              p.id_pruebas_oficiales, p.nombre_prueba, p.mejor_si_es_menor, p.unidad_entrada
       FROM marcas_perfil m
       JOIN pruebas_oficiales p ON m.pruebas_oficiales_id_pruebas_oficiales = p.id_pruebas_oficiales
       WHERE m.usuarios_id_usuario = ? AND p.oposiciones_id_oposicion = ?
       ORDER BY m.pruebas_oficiales_id_pruebas_oficiales, m.fecha_logro DESC, m.id_marcas_perfil DESC`,
      [userId, idOposicion]
    );
    const map = new Map();
    for (const r of rows || []) {
      if (!map.has(r.id_pruebas_oficiales)) map.set(r.id_pruebas_oficiales, r);
    }
    return [...map.values()];
  }

  static async nivelProyectadoConSimulacro(userId, idOposicion, resultados) {
    const [user] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
    const genero = user?.[0]?.genero || 'HOMBRE';
    const marcas = await MarcasPerfilService.obtenerMarcasPorPrueba(userId, idOposicion);
    const valores = new Map(
      marcas.map((m) => [m.id_pruebas_oficiales, Number(m.valord_record)])
    );
    for (const r of resultados) {
      const id = Number(r.id_prueba);
      const valorNuevo = Number(r.valor);
      const [pRows] = await db.query(
        'SELECT mejor_si_es_menor FROM pruebas_oficiales WHERE id_pruebas_oficiales = ?',
        [id]
      );
      if (!pRows?.length) continue;
      const anterior = valores.get(id);
      if (
        anterior == null ||
        MarcasPerfilService.esMejorMarca(pRows[0].mejor_si_es_menor, valorNuevo, anterior)
      ) {
        valores.set(id, valorNuevo);
      }
    }
    let suma = 0;
    for (const [idPrueba, val] of valores) {
      const nota = await BaremoService.calcularNotaPrueba(idPrueba, genero, val);
      suma += nota ?? 0;
    }
    const notaMedia = valores.size > 0 ? suma / valores.size : 0;
    let nivel = 'BASICO';
    if (notaMedia >= 5 && notaMedia < 8) nivel = 'INTERMEDIO';
    else if (notaMedia >= 8) nivel = 'AVANZADO';
    const [[{ total }]] = await db.query(
      'SELECT COUNT(*) AS total FROM pruebas_oficiales WHERE oposiciones_id_oposicion = ?',
      [idOposicion]
    );
    return {
      notaMedia: notaMedia.toFixed(2),
      nivelSugerido: nivel,
      pruebasCompletadas: valores.size,
      totalPruebas: Number(total || 0)
    };
  }

  static async analizarMejorasTrasSimulacro(userId, idOposicion, resultados) {
    const [user] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
    const genero = user?.[0]?.genero || 'HOMBRE';
    const marcasActuales = await MarcasPerfilService.obtenerMarcasPorPrueba(userId, idOposicion);
    const porPrueba = new Map(marcasActuales.map((m) => [m.id_pruebas_oficiales, m]));

    const nivelAntes = await RutinaService.calcularNotaYNivel(userId, idOposicion);
    const mejoras = [];

    for (const r of resultados) {
      const idPrueba = Number(r.id_prueba);
      const valorNuevo = Number(r.valor);
      const actual = porPrueba.get(idPrueba);
      const [pRows] = await db.query(
        'SELECT nombre_prueba, mejor_si_es_menor, unidad_entrada FROM pruebas_oficiales WHERE id_pruebas_oficiales = ?',
        [idPrueba]
      );
      if (!pRows?.length) continue;
      const p = pRows[0];
      const valorAnterior = actual ? Number(actual.valord_record) : null;
      const esMejora = MarcasPerfilService.esMejorMarca(p.mejor_si_es_menor, valorNuevo, valorAnterior);
      if (!esMejora && valorAnterior != null) continue;

      const notaNueva = await BaremoService.calcularNotaPrueba(idPrueba, genero, valorNuevo);
      mejoras.push({
        idPrueba,
        nombrePrueba: p.nombre_prueba,
        valorAnterior,
        valorNuevo,
        notaNueva,
        unidad: UnidadPruebaHelper.resolver(p, genero),
        esNueva: valorAnterior == null
      });
    }

    return {
      nivelActual: nivelAntes.nivelSugerido || 'INCOMPLETO',
      notaMediaActual: nivelAntes.notaMedia,
      mejoras,
      hayMejoras: mejoras.length > 0
    };
  }

  static async aplicarMarcasDesdeSimulacro(userId, idOposicion, resultados) {
    // Coherencia: no permitimos guardar marcas imposibles en el perfil/ranking.
    const [pruebas] = await db.query(
      `SELECT id_pruebas_oficiales, nombre_prueba, descripcion, mejor_si_es_menor,
              unidad_entrada, tipo_baremo, convocatoria_ref
       FROM pruebas_oficiales
       WHERE oposiciones_id_oposicion = ?
       ORDER BY id_pruebas_oficiales ASC`,
      [idOposicion]
    );
    if (pruebas && pruebas.length > 0) {
      const [u] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
      const genero = u?.[0]?.genero || 'HOMBRE';
      const mapa = new Map(pruebas.map((p) => [Number(p.id_pruebas_oficiales), p]));
      const { ok, errores } = MarcaValidator.validarResultados(resultados, mapa, genero);
      if (!ok) {
        const err = new Error('MARCA_INVALIDA: ' + errores.map((e) => e.msg).join('; '));
        err.codigo = 'MARCA_INVALIDA';
        err.errores = errores;
        throw err;
      }
    }

    const analisis = await MarcasPerfilService.analizarMejorasTrasSimulacro(
      userId,
      idOposicion,
      resultados
    );
    const connection = await db.getConnection();
    try {
      await connection.beginTransaction();
      for (const m of analisis.mejoras) {
        const [upd] = await connection.query(
          `UPDATE marcas_perfil SET valord_record = ?, fecha_logro = NOW()
           WHERE usuarios_id_usuario = ? AND pruebas_oficiales_id_pruebas_oficiales = ?`,
          [m.valorNuevo, userId, m.idPrueba]
        );
        if (upd.affectedRows === 0) {
          await connection.query(
            `INSERT INTO marcas_perfil (usuarios_id_usuario, pruebas_oficiales_id_pruebas_oficiales, valord_record, fecha_logro)
             VALUES (?, ?, ?, NOW())`,
            [userId, m.idPrueba, m.valorNuevo]
          );
        }
        await connection.query(
          `DELETE mp FROM marcas_perfil mp
           JOIN (
             SELECT usuarios_id_usuario, pruebas_oficiales_id_pruebas_oficiales, MAX(id_marcas_perfil) AS keep_id
             FROM marcas_perfil
             WHERE usuarios_id_usuario = ? AND pruebas_oficiales_id_pruebas_oficiales = ?
             GROUP BY usuarios_id_usuario, pruebas_oficiales_id_pruebas_oficiales
           ) t
           ON mp.usuarios_id_usuario = t.usuarios_id_usuario
           AND mp.pruebas_oficiales_id_pruebas_oficiales = t.pruebas_oficiales_id_pruebas_oficiales
           WHERE mp.id_marcas_perfil <> t.keep_id`,
          [userId, m.idPrueba]
        );
      }
      await connection.commit();
    } catch (e) {
      await connection.rollback();
      throw e;
    } finally {
      connection.release();
    }
    const nivelDespues = await RutinaService.calcularNotaYNivel(userId, idOposicion);
    const orden = { BASICO: 1, INTERMEDIO: 2, AVANZADO: 3, INCOMPLETO: 0 };
    const nivelAntes = analisis.nivelActual || 'INCOMPLETO';
    const nivelNuevo = nivelDespues.nivelSugerido || 'INCOMPLETO';
    return {
      ...analisis,
      nivelTrasActualizar: nivelNuevo,
      notaMediaTrasActualizar: nivelDespues.notaMedia,
      pruebasCompletadas: nivelDespues.pruebasCompletadas,
      subirNivel: (orden[nivelNuevo] || 0) > (orden[nivelAntes] || 0),
      marcasActualizadas: analisis.mejoras.length
    };
  }
}

module.exports = MarcasPerfilService;
