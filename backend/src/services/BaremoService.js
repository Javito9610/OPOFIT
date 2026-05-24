const db = require('../config/db');

class BaremoService {
  static async calcularNotaPrueba(idPrueba, genero, valor) {
    const [pruebaRows] = await db.query(
      'SELECT mejor_si_es_menor FROM pruebas_oficiales WHERE id_pruebas_oficiales = ?',
      [idPrueba]
    );
    if (!pruebaRows?.length) return null;
    const mejorSiEsMenor = Number(pruebaRows[0].mejor_si_es_menor) === 1;
    const [baremoResultado] = await db.query(
      `SELECT b.nota
       FROM baremos_puntuacion b
       JOIN pruebas_oficiales p ON b.pruebas_oficiales_id_pruebas_oficiales = p.id_pruebas_oficiales
       WHERE b.pruebas_oficiales_id_pruebas_oficiales = ?
         AND b.genero = ?
         AND (
           (p.mejor_si_es_menor = 1 AND ? <= b.marca_valor)
           OR (p.mejor_si_es_menor = 0 AND ? >= b.marca_valor)
         )
       ORDER BY b.nota DESC
       LIMIT 1`,
      [idPrueba, genero, valor, valor]
    );
    if (!baremoResultado?.length) return null;
    return baremoResultado[0].nota;
  }

  static async calcularNotaMediaOposicion(userId, idOposicion) {
    const [user] = await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]);
    if (!user?.length) return { error: 'USER_NOT_FOUND' };
    const genero = user[0].genero;
    const [marcas] = await db.query(
      `SELECT m.valord_record, p.id_pruebas_oficiales
       FROM marcas_perfil m
       JOIN pruebas_oficiales p ON m.pruebas_oficiales_id_pruebas_oficiales = p.id_pruebas_oficiales
       WHERE m.usuarios_id_usuario = ? AND p.oposiciones_id_oposicion = ?`,
      [userId, idOposicion]
    );
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
