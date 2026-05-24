const db = require('../config/db');

class BaremoService {
  /**
   * Calcula nota 0-10. Si la marca queda fuera de la tabla, asigna 0 o 10 (no devuelve null).
   */
  static async calcularNotaPrueba(idPrueba, genero, valor) {
    const g = String(genero || 'HOMBRE').toUpperCase();
    const v = Number(valor);
    if (Number.isNaN(v)) return null;

    const [pruebaRows] = await db.query(
      'SELECT mejor_si_es_menor FROM pruebas_oficiales WHERE id_pruebas_oficiales = ?',
      [idPrueba]
    );
    if (!pruebaRows?.length) return null;
    const mejorSiEsMenor = Number(pruebaRows[0].mejor_si_es_menor) === 1;

    const [filas] = await db.query(
      `SELECT marca_valor, nota
       FROM baremos_puntuacion
       WHERE pruebas_oficiales_id_pruebas_oficiales = ? AND genero = ?
       ORDER BY marca_valor ASC`,
      [idPrueba, g]
    );
    if (!filas?.length) return null;

    if (mejorSiEsMenor) {
      const peor = filas[filas.length - 1];
      const mejor = filas[0];
      if (v <= mejor.marca_valor) return 10;
      if (v > peor.marca_valor) return 0;
      let elegida = 0;
      for (const f of filas) {
        if (v <= f.marca_valor) elegida = f.nota;
      }
      return elegida;
    }

    const peor = filas[0];
    const mejor = filas[filas.length - 1];
    if (v >= mejor.marca_valor) return 10;
    if (v < peor.marca_valor) return 0;
    let elegida = 0;
    for (const f of filas) {
      if (v >= f.marca_valor) elegida = f.nota;
    }
    return elegida;
  }

  static async calcularNotaMediaOposicion(userId, idOposicion) {
    const MarcasPerfilService = require('./MarcasPerfilService');
    const [user] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
    if (!user?.length) return { error: 'USER_NOT_FOUND' };
    const genero = user[0].genero;
    const marcas = await MarcasPerfilService.obtenerMarcasPorPrueba(userId, idOposicion);
    let suma = 0;
    for (const m of marcas) {
      const nota = await BaremoService.calcularNotaPrueba(
        m.id_pruebas_oficiales,
        genero,
        m.valord_record
      );
      suma += nota ?? 0;
    }
    if (marcas.length === 0) return { notaMedia: null, genero };
    return { notaMedia: (suma / marcas.length).toFixed(2), genero };
  }
}

module.exports = BaremoService;
