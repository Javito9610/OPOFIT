/**
 * RegistroValidator: coherencia de los datos de registro de usuario.
 *
 * Rechaza datos imposibles (peso/altura fuera de rango humano, genero invalido,
 * email mal formado, nombre vacio). Funcion pura: sin BD.
 *
 * Nota: NO imponemos politica de contrasena aqui para no romper el contrato
 * existente de la app; eso se gestiona aparte si se decide endurecerlo.
 */

const LIMITES = {
  pesoKg: { min: 25, max: 350 },
  alturaCm: { min: 100, max: 260 },
  nombreMax: 80,
  emailMax: 254
};

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * @returns {{ok:boolean, msg?:string, datos?:object}}
 */
function validarRegistro(userData = {}) {
  const nombre = String(userData.nombre ?? '').trim();
  if (!nombre) return fail('El nombre es obligatorio');
  if (nombre.length > LIMITES.nombreMax) return fail(`El nombre es demasiado largo (max ${LIMITES.nombreMax})`);

  const email = String(userData.email ?? '').trim().toLowerCase();
  if (!email) return fail('El email es obligatorio');
  if (email.length > LIMITES.emailMax || !EMAIL_RE.test(email)) return fail('El email no tiene un formato valido');

  const genero = String(userData.genero ?? '').trim().toUpperCase();
  if (genero !== 'HOMBRE' && genero !== 'MUJER') return fail('Genero no valido (debe ser HOMBRE o MUJER)');

  const peso = Number(userData.peso);
  if (!Number.isFinite(peso) || peso < LIMITES.pesoKg.min || peso > LIMITES.pesoKg.max) {
    return fail(`Peso fuera de rango (${LIMITES.pesoKg.min}-${LIMITES.pesoKg.max} kg)`);
  }

  const altura = Number(userData.altura);
  if (!Number.isFinite(altura) || altura < LIMITES.alturaCm.min || altura > LIMITES.alturaCm.max) {
    return fail(`Altura fuera de rango (${LIMITES.alturaCm.min}-${LIMITES.alturaCm.max} cm)`);
  }

  return { ok: true, datos: { nombre, email, genero, peso, altura } };
}

function fail(msg) {
  return { ok: false, msg };
}

module.exports = { LIMITES, EMAIL_RE, validarRegistro };
