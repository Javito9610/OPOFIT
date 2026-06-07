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
      // Filas ordenadas ASC (marca_valor más bajo = mejor marca = nota más alta).
      // Buscamos el PRIMER umbral que el usuario supera (v <= umbral) y paramos.
      // Sin el break, el bucle seguía sobreescribiendo elegida con notas cada vez
      // más bajas (el usuario obtenía una nota menor a la que le correspondía).
      const peor = filas[filas.length - 1];
      const mejor = filas[0];
      if (v <= mejor.marca_valor) return 10;
      if (v > peor.marca_valor) return 0;
      let elegida = 0;
      for (const f of filas) {
        if (v <= f.marca_valor) {
          elegida = f.nota;
          break; // CORRECCIÓN: tomar la primera franja alcanzada, no la última
        }
      }
      return elegida;
    }

    // Para pruebas donde mayor valor = mejor (repeticiones, distancia, etc.)
    // Filas ASC: sobreescribir mientras v >= umbral da la franja correcta.
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
    if (marcas.length === 0) return { notaMedia: null, genero };
    // Usar mismo denominador que SimulacroService (solo pruebas con baremo real):
    let suma = 0;
    let count = 0;
    for (const m of marcas) {
      const nota = await BaremoService.calcularNotaPrueba(
        m.id_pruebas_oficiales,
        genero,
        m.valord_record
      );
      if (nota != null) {
        suma += nota;
        count++;
      }
    }
    if (count === 0) return { notaMedia: null, genero };
    return { notaMedia: (suma / count).toFixed(2), genero };
  }
}

module.exports = BaremoService;
