/**
 * MarcaValidator: valida que una marca/resultado sea coherente y posible.
 *
 * Evita que se registren valores absurdos o imposibles (negativos, cero, NaN,
 * texto, repeticiones decimales, o cifras fuera de todo rango humano como
 * "1000 km" o "100000 dominadas").
 *
 * Funciones PURAS para poder testearlas a fondo sin BD.
 */

const UnidadPruebaHelper = require('./UnidadPruebaHelper');

// Limites por unidad. Son generosos: solo descartan lo imposible, no marcas reales.
const LIMITES = {
  s: { min: 1, max: 7200, entero: false, etiqueta: 'segundos' }, // hasta 2h
  min: { min: 0.1, max: 600, entero: false, etiqueta: 'minutos' }, // hasta 10h
  reps: { min: 1, max: 10000, entero: true, etiqueta: 'repeticiones' },
  m: { min: 1, max: 100000, entero: false, etiqueta: 'metros' }, // hasta 100 km
  km: { min: 0.01, max: 1000, etiqueta: 'kilometros', entero: false }
};

const LIMITE_DEFECTO = { min: 0, max: 1000000, entero: false, etiqueta: '' };

/** Normaliza sinonimos de unidad a la clave canonica, o null si desconocida. */
function normalizarUnidad(unidad) {
  const x = String(unidad || '').toLowerCase().trim();
  if (['s', 'seg', 'segs', 'segundo', 'segundos'].includes(x)) return 's';
  if (['min', 'mins', 'minuto', 'minutos'].includes(x)) return 'min';
  if (['rep', 'reps', 'repeticion', 'repeticiones'].includes(x)) return 'reps';
  if (['m', 'metro', 'metros'].includes(x)) return 'm';
  if (['km', 'kilometro', 'kilometros'].includes(x)) return 'km';
  return null;
}

/**
 * Resuelve la unidad efectiva de una prueba (respeta distancias/min y maneja
 * el caso de dominadas/suspension que depende del genero).
 */
function unidadDePrueba(prueba, genero = null) {
  const cruda = normalizarUnidad(prueba?.unidad_entrada);
  if (cruda === 'm' || cruda === 'km' || cruda === 'min') return cruda;
  // Para 's'/'reps' (o desconocida) usamos el helper que aplica la regla por genero.
  try {
    return normalizarUnidad(UnidadPruebaHelper.resolver(prueba || {}, genero)) || cruda || 'reps';
  } catch {
    return cruda || 'reps';
  }
}

/**
 * Valida un valor numerico para una unidad dada.
 * @returns {{ok:boolean, valor?:number, msg?:string, codigo?:string}}
 */
function validarValor(valorRaw, unidad) {
  if (valorRaw === null || valorRaw === undefined || valorRaw === '') {
    return { ok: false, codigo: 'VACIO', msg: 'Falta el valor de la marca' };
  }
  // Number('12abc') -> NaN; Number(' 12 ') -> 12; Number(true) -> 1 (lo rechazamos)
  if (typeof valorRaw === 'boolean') {
    return { ok: false, codigo: 'NO_NUMERO', msg: 'El valor debe ser un numero' };
  }
  const valor = Number(valorRaw);
  if (!Number.isFinite(valor)) {
    return { ok: false, codigo: 'NO_NUMERO', msg: 'El valor debe ser un numero valido' };
  }
  if (valor <= 0) {
    return { ok: false, codigo: 'NO_POSITIVO', msg: 'El valor debe ser mayor que 0' };
  }
  const u = normalizarUnidad(unidad);
  const lim = (u && LIMITES[u]) || LIMITE_DEFECTO;
  if (lim.entero && !Number.isInteger(valor)) {
    return { ok: false, codigo: 'NO_ENTERO', msg: `Las ${lim.etiqueta} deben ser un numero entero` };
  }
  if (valor < lim.min) {
    return { ok: false, codigo: 'DEMASIADO_BAJO', msg: `Valor demasiado bajo (minimo ${lim.min} ${lim.etiqueta})`.trim() };
  }
  if (valor > lim.max) {
    return { ok: false, codigo: 'DEMASIADO_ALTO', msg: `Valor imposible (maximo ${lim.max} ${lim.etiqueta})`.trim() };
  }
  return { ok: true, valor };
}

/** Valida un valor para una prueba concreta (resuelve unidad y nombra el error). */
function validarMarcaPrueba(prueba, valorRaw, genero = null) {
  const unidad = unidadDePrueba(prueba, genero);
  const r = validarValor(valorRaw, unidad);
  if (!r.ok) {
    const nombre = prueba?.nombre_prueba ? ` (${prueba.nombre_prueba})` : '';
    return { ...r, unidad, msg: `${r.msg}${nombre}` };
  }
  return { ...r, unidad };
}

/**
 * Valida una lista de resultados contra un mapa de pruebas (id -> prueba).
 * @returns {{ok:boolean, errores:Array<{id_prueba:number,msg:string}>}}
 */
function validarResultados(resultados, pruebasPorId, genero = null) {
  const errores = [];
  const lista = Array.isArray(resultados) ? resultados : [];
  for (const r of lista) {
    const idPrueba = Number(r?.id_prueba);
    const prueba = pruebasPorId instanceof Map ? pruebasPorId.get(idPrueba) : pruebasPorId?.[idPrueba];
    if (!prueba) {
      errores.push({ id_prueba: idPrueba, msg: `Prueba ${idPrueba} no existe en esta oposicion` });
      continue;
    }
    const v = validarMarcaPrueba(prueba, r?.valor, genero);
    if (!v.ok) errores.push({ id_prueba: idPrueba, msg: v.msg });
  }
  return { ok: errores.length === 0, errores };
}

module.exports = {
  LIMITES,
  normalizarUnidad,
  unidadDePrueba,
  validarValor,
  validarMarcaPrueba,
  validarResultados
};
