/**
 * Unidades oficiales por prueba (s = segundos, reps = repeticiones).
 * PN prueba 2: hombres dominadas (reps), mujeres suspensión (s).
 */
class UnidadPruebaHelper {
  static resolver(prueba, genero = null) {
    const id = Number(prueba.id_pruebas_oficiales || prueba.id_prueba);
    if (prueba.unidad_entrada === 's' || prueba.unidad_entrada === 'reps') {
      if (id === 2 && genero === 'MUJER') return 's';
      if (id === 2 && genero === 'HOMBRE') return 'reps';
      return prueba.unidad_entrada;
    }
    if (id === 2) {
      return genero === 'MUJER' ? 's' : 'reps';
    }
    if (id === 20) return 's';
    if (Number(prueba.mejor_si_es_menor) === 1) return 's';
    return 'reps';
  }

  static etiqueta(unidad) {
    if (unidad === 's') return 'segundos';
    return 'repeticiones';
  }
}

module.exports = UnidadPruebaHelper;
