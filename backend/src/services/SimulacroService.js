const db = require('../config/db');
const BaremoService = require('./BaremoService');
const PremiumService = require('./PremiumService');
const MarcasPerfilService = require('./MarcasPerfilService');
const RutinaService = require('./RutinasService');
const UnidadPruebaHelper = require('../utils/UnidadPruebaHelper');
const MarcaValidator = require('../utils/MarcaValidator');

/**
 * Carga el catalogo de pruebas (id -> prueba) de una oposicion para validar.
 * Si no hay pruebas (entorno sin catalogo), devuelve null y se omite la validacion.
 */
async function cargarCatalogo(idOposicion) {
  const [pruebas] = await db.query(
    `SELECT id_pruebas_oficiales, nombre_prueba, descripcion, mejor_si_es_menor,
            unidad_entrada, tipo_baremo, convocatoria_ref
     FROM pruebas_oficiales
     WHERE oposiciones_id_oposicion = ?
     ORDER BY id_pruebas_oficiales ASC`,
    [idOposicion]
  );
  if (!pruebas || pruebas.length === 0) return null;
  const mapa = new Map();
  for (const p of pruebas) mapa.set(Number(p.id_pruebas_oficiales), p);
  return mapa;
}

/** Lanza un error MARCA_INVALIDA legible si algun resultado no es coherente. */
function exigirMarcasValidas(resultados, catalogo, genero) {
  if (!catalogo) return;
  const { ok, errores } = MarcaValidator.validarResultados(resultados, catalogo, genero);
  if (!ok) {
    const err = new Error('MARCA_INVALIDA: ' + errores.map((e) => e.msg).join('; '));
    err.codigo = 'MARCA_INVALIDA';
    err.errores = errores;
    throw err;
  }
}

class SimulacroService {
  static async listarPruebas(idOposicion, userId) {
    const existe = await PremiumService.puedeAccederOposicion(userId, idOposicion);
    if (!existe) throw new Error('OPOSICION_NOT_FOUND');
    const [user] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
    const genero = user?.[0]?.genero || 'HOMBRE';
    const [pruebas] = await db.query(
      `SELECT id_pruebas_oficiales, nombre_prueba, descripcion, mejor_si_es_menor,
              unidad_entrada, tipo_baremo, convocatoria_ref
       FROM pruebas_oficiales
       WHERE oposiciones_id_oposicion = ?
       ORDER BY id_pruebas_oficiales ASC`,
      [idOposicion]
    );
    return (pruebas || []).map((p) => ({
      id_pruebas_oficiales: p.id_pruebas_oficiales,
      nombre_prueba: p.nombre_prueba,
      descripcion: p.descripcion,
      mejor_si_es_menor: p.mejor_si_es_menor,
      tipo_baremo: p.tipo_baremo || 'PUNTUACION',
      convocatoria_ref: p.convocatoria_ref,
      unidad: UnidadPruebaHelper.resolver(p, genero),
      unidadEtiqueta: UnidadPruebaHelper.etiqueta(UnidadPruebaHelper.resolver(p, genero))
    }));
  }

  static async guardarSimulacro(userId, idOposicion, resultados) {
    const existe = await PremiumService.puedeAccederOposicion(userId, idOposicion);
    if (!existe) throw new Error('OPOSICION_NOT_FOUND');
    const [user] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
    if (!user?.length) throw new Error('USER_NOT_FOUND');
    const genero = user[0].genero;

    // Coherencia: rechazamos valores imposibles antes de persistir nada.
    const catalogo = await cargarCatalogo(idOposicion);
    exigirMarcasValidas(resultados, catalogo, genero);

    const connection = await db.getConnection();
    try {
      await connection.beginTransaction();
      let sumaNotas = 0;
      let countNotas = 0;
      const detalle = [];
      for (const r of resultados) {
        const nota = await BaremoService.calcularNotaPrueba(r.id_prueba, genero, r.valor);
        if (nota != null) {
          sumaNotas += nota;
          countNotas++;
        }
        detalle.push({ id_prueba: r.id_prueba, valor: r.valor, nota });
      }
      const notaMedia = countNotas > 0 ? (sumaNotas / countNotas).toFixed(2) : null;
      const [ins] = await connection.query(
        `INSERT INTO simulacros (fecha, nota_media, usuarios_id_usuario, oposiciones_id_oposicion)
         VALUES (NOW(), ?, ?, ?)`,
        [notaMedia, userId, idOposicion]
      );
      const idSimulacro = ins.insertId;
      for (const d of detalle) {
        await connection.query(
          `INSERT INTO simulacro_pruebas
           (valor_registrado, nota_obtenida, simulacros_id_simulacro, pruebas_oficiales_id_pruebas_oficiales)
           VALUES (?, ?, ?, ?)`,
          [d.valor, d.nota, idSimulacro, d.id_prueba]
        );
      }
      await connection.commit();

      const resultadosNorm = resultados.map((r) => ({
        id_prueba: r.id_prueba,
        valor: r.valor
      }));
      const perfil = await MarcasPerfilService.analizarMejorasTrasSimulacro(
        userId,
        idOposicion,
        resultadosNorm
      );
      const nivelProyectado = await MarcasPerfilService.nivelProyectadoConSimulacro(
        userId,
        idOposicion,
        resultadosNorm
      );
      const subirNivel =
        perfil.nivelActual &&
        perfil.nivelActual !== 'INCOMPLETO' &&
        nivelOrden(nivelProyectado.nivelSugerido) > nivelOrden(perfil.nivelActual);

      return {
        idSimulacro,
        notaMedia,
        detalle,
        perfil: {
          ...perfil,
          nivelTrasSimulacro: nivelProyectado.nivelSugerido,
          notaMediaTrasSimulacro: nivelProyectado.notaMedia,
          subirNivel,
          totalPruebasOpo: nivelProyectado.totalPruebas,
          pruebasCompletadasPerfil: nivelProyectado.pruebasCompletadas
        }
      };
    } catch (e) {
      await connection.rollback();
      throw e;
    } finally {
      connection.release();
    }
  }

  static async historial(userId, idOposicion) {
    const [rows] = await db.query(
      `SELECT s.id_simulacro, s.fecha, s.nota_media
       FROM simulacros s
       WHERE s.usuarios_id_usuario = ? AND s.oposiciones_id_oposicion = ?
       ORDER BY s.fecha DESC
       LIMIT 20`,
      [userId, idOposicion]
    );
    return rows || [];
  }
}

function nivelOrden(n) {
  return { BASICO: 1, INTERMEDIO: 2, AVANZADO: 3, INCOMPLETO: 0 }[n] || 0;
}

module.exports = SimulacroService;
