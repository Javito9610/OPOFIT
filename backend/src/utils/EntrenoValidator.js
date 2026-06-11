/**
 * Valida resultados al registrar un entrenamiento (reps, min, km, s…).
 * Reutiliza los mismos límites que MarcaValidator.
 */
const db = require('../config/db');
const MarcaValidator = require('./MarcaValidator');
const RutinaService = require('../services/RutinasService');

function validarEjercicioPorNombre(nombre, valorRaw) {
  const unidad = RutinaService.inferUnidad(nombre);
  const r = MarcaValidator.validarValor(valorRaw, unidad);
  if (!r.ok) {
    const suf = nombre ? ` (${nombre})` : '';
    return { ok: false, msg: `${r.msg}${suf}`, unidad, codigo: r.codigo };
  }
  return { ok: true, valor: r.valor, unidad };
}

/**
 * Valida la duración del entreno. UNIDAD: SEGUNDOS.
 *
 * ⚠ Historia del bug: el frontend pasó a enviar segundos (fix "0 minutos" en
 * el historial) pero este validador seguía asumiendo minutos con tope 600.
 * Resultado: cualquier sesión de más de 10 minutos (600 s) devolvía 400
 * "Duración imposible" y el usuario NO podía guardar su entreno. Detectado
 * por el E2E de viaje completo.
 */
function validarDuracionMin(duracion) {
  const d = Number(duracion);
  if (!Number.isFinite(d) || d < 1) {
    return { ok: false, msg: 'La duración debe ser al menos 1 segundo' };
  }
  // 10 horas en segundos: más que cualquier sesión real.
  if (d > 36_000) {
    return { ok: false, msg: 'Duración imposible (máximo 10 h)' };
  }
  return { ok: true, valor: Math.round(d) };
}

async function validarEjerciciosEntreno(ejercicios) {
  const errores = [];
  const lista = Array.isArray(ejercicios) ? ejercicios : [];
  if (!lista.length) {
    return { ok: false, errores: [{ msg: 'No hay ejercicios para registrar' }] };
  }

  for (const ej of lista) {
    const id = Number(ej?.id_ejercicio);
    if (!Number.isFinite(id) || id <= 0) {
      errores.push({
        id_ejercicio: ej?.id_ejercicio,
        msg: 'Ejercicio sin identificador válido'
      });
      continue;
    }
    let nombre = ej?.nombre ? String(ej.nombre) : '';
    if (!nombre) {
      const [rows] = await db.query('SELECT nombre FROM ejercicios WHERE id_ejercicio = ? LIMIT 1', [id]);
      nombre = rows[0]?.nombre || '';
    }
    const v = validarEjercicioPorNombre(nombre, ej?.valor);
    if (!v.ok) {
      errores.push({ id_ejercicio: id, nombre: nombre || `Ejercicio ${id}`, msg: v.msg });
    }
  }
  return { ok: errores.length === 0, errores };
}

module.exports = {
  validarEjercicioPorNombre,
  validarDuracionMin,
  validarEjerciciosEntreno
};
