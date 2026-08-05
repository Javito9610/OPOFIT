/**
 * EstructuraSeriesService — decide CÓMO se agrupan las series (estructura de
 * entrenamiento), como haría un preparador según el objetivo y el nivel:
 *
 *   - RECTA:            todas las series igual (5×5). Base, técnica, principiante.
 *   - PIRAMIDE:         reps ↓ y peso ↑ (12·10·8·6). Calienta y llega pesado. Fuerza.
 *   - PIRAMIDE_INVERSA: peso pesado primero, luego más reps (6·8·10·12). Máxima
 *                       intensidad en fresco. Hipertrofia intermedio/avanzado.
 *
 * Devuelve un esquema por serie: [{reps, intensidad}] donde intensidad es una
 * pista relativa ('ligera'|'media'|'alta') para la carga.
 *
 * Solo aplica a ejercicios de fuerza medidos en reps. Cardio / isométricos /
 * tiempo / distancia se dejan en RECTA (no tiene sentido piramidar un rodaje).
 */

function clamp(v, min, max) {
  return Math.max(min, Math.min(max, v));
}

function recta(series, reps) {
  const esquema = Array.from({ length: series }, () => ({ reps, intensidad: 'media' }));
  return { estructura: 'RECTA', estructura_label: 'Series rectas', esquema };
}

function piramide(series, repsObjetivo) {
  // La ÚLTIMA serie es la más pesada (repsObjetivo). Las anteriores suben reps
  // de 2 en 2 (más ligeras). Intensidad crece hacia el final.
  const esquema = [];
  for (let i = 0; i < series; i++) {
    const desdeArriba = series - 1 - i; // 0 en la última
    const reps = clamp(repsObjetivo + desdeArriba * 2, 1, 20);
    const intensidad = desdeArriba === 0 ? 'alta' : desdeArriba === 1 ? 'media' : 'ligera';
    esquema.push({ reps, intensidad });
  }
  return { estructura: 'PIRAMIDE', estructura_label: 'Pirámide (sube el peso)', esquema };
}

function piramideInversa(series, repsObjetivo) {
  // La PRIMERA serie es la más pesada (repsObjetivo, en fresco). Luego bajas
  // peso y subes reps.
  const esquema = [];
  for (let i = 0; i < series; i++) {
    const reps = clamp(repsObjetivo + i * 2, 1, 25);
    const intensidad = i === 0 ? 'alta' : i === 1 ? 'media' : 'ligera';
    esquema.push({ reps, intensidad });
  }
  return { estructura: 'PIRAMIDE_INVERSA', estructura_label: 'Pirámide inversa (empieza pesado)', esquema };
}

/**
 * @param {object} p
 * @param {number} p.series
 * @param {number} p.repeticiones  reps objetivo (serie principal)
 * @param {string} p.unidad        reps | s | min | m | km ...
 * @param {string} p.objetivo      fuerza | hipertrofia | resistencia
 * @param {string} p.nivel         BASICO | INTERMEDIO | AVANZADO
 * @param {string} [p.preferencia] RECTA | PIRAMIDE | PIRAMIDE_INVERSA (si el usuario elige)
 * @param {number} [p.posicion]    posición en la sesión (1 = principal)
 */
function generar(p = {}) {
  const series = Math.max(1, Number(p.series) || 1);
  const reps = Math.max(1, Number(p.repeticiones) || 1);
  const unidad = String(p.unidad || 'reps').toLowerCase();
  const objetivo = String(p.objetivo || 'hipertrofia').toLowerCase();
  const nivel = String(p.nivel || 'BASICO').toUpperCase();
  const pref = String(p.preferencia || '').toUpperCase();
  const nombre = String(p.nombre || '').toLowerCase().replace(/×/g, 'x');

  // No piramidamos cardio / isométricos / tiempo / distancia ni series únicas.
  if ((unidad !== 'reps' && unidad !== 'rep') || series < 2 || objetivo === 'resistencia') {
    return recta(series, reps);
  }
  // Si el NOMBRE ya trae un esquema explícito "NxN" (p.ej. "Vallas 4x8",
  // "Landmine press 4x8"), el preparador ya definió series rectas: NO piramidamos
  // por encima. La escalera (1-2-3-4-5) se maneja aparte en aplicarInteligencia.
  if (/\b\d+\s*x\s*\d+\b/.test(nombre) && !pref) {
    return recta(series, reps);
  }

  // 1) Preferencia explícita del usuario manda.
  if (pref === 'PIRAMIDE') return piramide(series, reps);
  if (pref === 'PIRAMIDE_INVERSA') return piramideInversa(series, reps);
  if (pref === 'RECTA') return recta(series, reps);

  // 2) Decisión del motor por nivel + objetivo:
  //    - Básico: rectas (aprende técnica, progresa lineal).
  if (nivel === 'BASICO') return recta(series, reps);
  //    - Principal de fuerza: pirámide (calienta subiendo peso hasta el top set).
  if (objetivo === 'fuerza' && (p.posicion == null || Number(p.posicion) === 1)) {
    return piramide(series, reps);
  }
  //    - Hipertrofia (intermedio/avanzado): pirámide inversa (pesado en fresco).
  if (objetivo === 'hipertrofia') return piramideInversa(series, reps);

  return recta(series, reps);
}

module.exports = { generar, recta, piramide, piramideInversa };
