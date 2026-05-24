const db = require('../config/db');
const BaremoService = require('./BaremoService');
const PremiumService = require('./PremiumService');

class SimulacroService {
  static async listarPruebas(idOposicion, userId) {
    const existe = await PremiumService.puedeAccederOposicion(userId, idOposicion);
    if (!existe) throw new Error('OPOSICION_NOT_FOUND');
    const [pruebas] = await db.query(
      `SELECT id_pruebas_oficiales, nombre_prueba, descripcion, mejor_si_es_menor,
              COALESCE(unidad_entrada, IF(mejor_si_es_menor = 1, 's', 'reps')) AS unidad_entrada,
              tipo_baremo, convocatoria_ref
       FROM pruebas_oficiales
       WHERE oposiciones_id_oposicion = ?
       ORDER BY id_pruebas_oficiales ASC`,
      [idOposicion]
    );
    return (pruebas || []).map((p) => ({
      ...p,
      unidad: p.unidad_entrada || (Number(p.mejor_si_es_menor) === 1 ? 's' : 'reps')
    }));
  }

  static async guardarSimulacro(userId, idOposicion, resultados) {
    const existe = await PremiumService.puedeAccederOposicion(userId, idOposicion);
    if (!existe) throw new Error('OPOSICION_NOT_FOUND');
    const [user] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
    if (!user?.length) throw new Error('USER_NOT_FOUND');
    const genero = user[0].genero;
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
      return { idSimulacro, notaMedia, detalle };
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

module.exports = SimulacroService;
