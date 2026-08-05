/**
 * OrdenSesionService — ordena los ejercicios de una sesión con criterio
 * profesional (NSCA Essentials, ACSM, teoría del entrenamiento / INEF):
 *
 *   1. POTENCIA / VELOCIDAD / PLIOMETRÍA — máxima exigencia neuromuscular y
 *      técnica: van EN FRESCO (fatiga arruina la potencia y la técnica).
 *   2. COMPUESTOS GRANDES multiarticulares (sentadilla, peso muerto, dominada,
 *      press) — mayor demanda y transferencia.
 *   3. COMPUESTOS MEDIOS / unilaterales (zancada, remo, press vertical).
 *   4. ACCESORIOS / AISLAMIENTO (curl, extensiones, elevaciones, gemelo).
 *   5. CORE / anti-extensión / rotación — al final para no fatigar el estabilizador
 *      antes de los grandes levantamientos.
 *   6. MOVILIDAD / vuelta a la calma.
 *
 * Regla de oro: "de lo más neural y técnico a lo más metabólico y aislado".
 */

const Patron = require('./PatronMovimientoService');

function norm(s) {
  return String(s || '').normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
}

const RE_AISLAMIENTO = /curl|extensi[oó]n|elevaci[oó]n|patada|p[aá]jaro|face pull|gemelo|femoral tumbad|craneal|pull[- ]?over|encogimiento|aparto|apertura/;

function prioridad(ej) {
  const patron = Patron.clasificar(ej);
  const n = norm(ej.nombre);
  const aislamiento = RE_AISLAMIENTO.test(n);
  switch (patron) {
    case 'SPRINT':
    case 'PLYO':
    case 'AGI':
      return 1; // potencia / velocidad → primero, en fresco
    case 'SQUAT':
    case 'HINGE':
    case 'PULL_V':
      return 2; // compuestos grandes
    case 'PUSH_H':
    case 'PUSH_V':
    case 'PULL_H':
    case 'LUNGE':
    case 'CARRY':
      return aislamiento ? 4 : 3; // compuesto medio, o accesorio si es aislamiento
    case 'ANTI_EXT':
    case 'ROT':
      return 5; // core al final
    case 'LOCO':
      return 5; // cardio al final en días de fuerza (en días de cardio son todos igual)
    case 'MOB':
      return 6; // movilidad / vuelta a la calma
    default:
      return aislamiento ? 4 : 3;
  }
}

/**
 * Ordena una lista de ejercicios de la sesión. Estable dentro de la misma
 * prioridad (respeta el orden original entre iguales).
 * @param {Array} ejercicios
 * @returns {Array} nueva lista ordenada
 */
function ordenar(ejercicios) {
  if (!Array.isArray(ejercicios) || ejercicios.length < 2) return ejercicios || [];
  return ejercicios
    .map((ej, i) => ({ ej, i, p: prioridad(ej) }))
    .sort((a, b) => (a.p - b.p) || (a.i - b.i))
    .map((x) => x.ej);
}

module.exports = { ordenar, prioridad };
