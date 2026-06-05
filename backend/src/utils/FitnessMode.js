/** Plan genérico de entrenamiento para usuarios modo FITNESS (sin oposición). */
const FITNESS_PLAN_OPOSICION = 1;

function isFitnessModo(modoUso) {
  return String(modoUso || '').trim().toUpperCase() === 'FITNESS';
}

function planOposicionId(user) {
  if (!user) return FITNESS_PLAN_OPOSICION;
  if (isFitnessModo(user.modo_uso)) return FITNESS_PLAN_OPOSICION;
  const id = user.oposiciones_id_oposicion;
  return id != null && !Number.isNaN(Number(id)) ? Number(id) : FITNESS_PLAN_OPOSICION;
}

module.exports = { FITNESS_PLAN_OPOSICION, isFitnessModo, planOposicionId };
