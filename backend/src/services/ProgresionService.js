/**
 * ProgresionService — para cada ejercicio clave devuelve su regresión
 * (versión más fácil) y su progresión (versión más difícil).
 *
 * Esto es lo que diferencia un plan de coach de un plan genérico:
 *  - Si el usuario no puede hacer una dominada → regresión "Dominada
 *    asistida con banda" o "Australian row".
 *  - Si el usuario ya domina dominadas → progresión "Dominada con peso"
 *    o "L-sit pull-up".
 *
 * Usado por el front en el icono "↘ más fácil" / "↗ más difícil" de
 * cada ejercicio del plan, y por la IA al sustituir.
 */

const REGLAS = [
  // Calistenia tirón
  {
    match: /dominada(?!.*(asist|banda|negat|excent|peso))/i,
    regresion: { nombre: 'Dominada asistida con banda',  motivo: 'Reduce la carga corporal mientras adquieres la fuerza necesaria.' },
    progresion: { nombre: 'Dominada con lastre 5-10 kg',  motivo: 'Añade carga externa cuando completes 8+ reps limpias.' }
  },
  {
    match: /australian row|remo invertido|remo en mesa/i,
    regresion: { nombre: 'Australian row con rodillas',  motivo: 'Reduce el ángulo del cuerpo para menos carga.' },
    progresion: { nombre: 'Dominada asistida con banda', motivo: 'Progresa hacia el tirón vertical en barra.' }
  },

  // Calistenia empuje
  {
    match: /flexion(?!.*(diamante|archer|palmada|inclinad|rodill))/i,
    regresion: { nombre: 'Flexiones inclinadas (manos en banco)', motivo: 'Eleva el plano superior para descargar peso.' },
    progresion: { nombre: 'Flexiones diamante',                  motivo: 'Cierra las manos y trabaja más tríceps.' }
  },
  {
    match: /fondos en paralel|dip(?!.*(asist|negat))/i,
    regresion: { nombre: 'Fondos en banco',          motivo: 'Apoyo de pies en suelo reduce la carga.' },
    progresion: { nombre: 'Fondos con lastre 5 kg',  motivo: 'Añade peso cuando hagas 10+ reps limpias.' }
  },
  {
    match: /press banca/i,
    regresion: { nombre: 'Press banca con mancuernas', motivo: 'Permite ajustar peso de forma más fina.' },
    progresion: { nombre: 'Press banca pausa 2 s',     motivo: 'Pausa en el pecho aumenta el tiempo bajo tensión.' }
  },
  {
    match: /press militar|push press/i,
    regresion: { nombre: 'Press militar con mancuernas', motivo: 'Menos exigencia de estabilidad que con barra.' },
    progresion: { nombre: 'Push press explosivo',        motivo: 'Añade impulso de cadera para más peso.' }
  },

  // Pierna
  {
    match: /sentadilla(?!.*(goblet|pistol|bulgar|frontal))/i,
    regresion: { nombre: 'Sentadilla goblet con mancuerna', motivo: 'Carga en el pecho mejora la postura.' },
    progresion: { nombre: 'Sentadilla frontal con barra',   motivo: 'Mayor demanda de core y cuádriceps.' }
  },
  {
    match: /peso muerto(?!.*(rumano|sumo|deficit))/i,
    regresion: { nombre: 'Peso muerto rumano con mancuernas', motivo: 'Recorrido más corto, menos carga total.' },
    progresion: { nombre: 'Peso muerto a déficit',            motivo: 'Más rango = más estímulo.' }
  },
  {
    match: /zancada|lunge/i,
    regresion: { nombre: 'Zancada estática (split squat)', motivo: 'Sin desplazamiento mejora la estabilidad.' },
    progresion: { nombre: 'Zancada caminando con peso',    motivo: 'Añade carga y desplazamiento.' }
  },
  {
    match: /hip thrust|puente de gluteo/i,
    regresion: { nombre: 'Puente de glúteo en suelo',  motivo: 'Sin banco, recorrido menor.' },
    progresion: { nombre: 'Hip thrust a una pierna',   motivo: 'Unilateral aumenta el reclutamiento de glúteo.' }
  },

  // Velocidad / pliometría
  {
    match: /box jump|salto al cajon/i,
    regresion: { nombre: 'Step-up al cajón con control', motivo: 'Subes sin salto explosivo.' },
    progresion: { nombre: 'Depth jump desde 40 cm',      motivo: 'Reactividad y stretch-shortening cycle.' }
  },
  {
    match: /^sprint\b|sprint \d+ m/i,
    regresion: { nombre: 'Carrera al 85% con técnica',  motivo: 'Submáximo permite refinar mecánica.' },
    progresion: { nombre: 'Sprint con trineo / arrastre', motivo: 'Resistencia para mayor potencia.' }
  },
  {
    match: /conos en t|t-test/i,
    regresion: { nombre: 'Conos en T al 70% velocidad', motivo: 'Domina los cambios de dirección sin perder forma.' },
    progresion: { nombre: 'Conos en T con balón',       motivo: 'Cognitivo + motor: mayor transferencia.' }
  },

  // Core
  {
    match: /plancha(?!.*(lateral|reach|paso|alt))/i,
    regresion: { nombre: 'Plancha con rodillas',        motivo: 'Reduce la palanca para mantener la línea.' },
    progresion: { nombre: 'Plancha con elevación de pierna alterna', motivo: 'Añade antirrotación.' }
  },
  {
    match: /hollow|l-sit/i,
    regresion: { nombre: 'Hollow con rodillas dobladas', motivo: 'Acorta la palanca de las piernas.' },
    progresion: { nombre: 'L-sit en paralelas',          motivo: 'Elevación total del cuerpo, brutal.' }
  },
  {
    match: /ab wheel|rueda abdominal/i,
    regresion: { nombre: 'Ab wheel de rodillas',         motivo: 'Permite controlar el descenso sin colapsar lumbar.' },
    progresion: { nombre: 'Ab wheel de pie',             motivo: 'Rango y palanca total.' }
  },

  // Cardio
  {
    match: /carrera continua|trote|rodaje/i,
    regresion: { nombre: 'Caminar rápido + trote 1:1',   motivo: 'Intervalos cortos para construir base.' },
    progresion: { nombre: 'Tempo run 4-6 km a umbral',   motivo: 'Eleva el umbral aeróbico.' }
  },
  {
    match: /natacion|natación/i,
    regresion: { nombre: 'Natación con tabla',           motivo: 'Aísla piernas para concentrarte en respiración.' },
    progresion: { nombre: 'Natación con palas',          motivo: 'Aumenta la resistencia y la tracción.' }
  }
];

function normalizar(s) {
  return String(s || '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();
}

function buscarRegla(nombre) {
  const n = normalizar(nombre);
  return REGLAS.find((r) => r.match.test(n)) || null;
}

function regresionDe(nombre) {
  const r = buscarRegla(nombre);
  return r ? r.regresion : null;
}

function progresionDe(nombre) {
  const r = buscarRegla(nombre);
  return r ? r.progresion : null;
}

function decorar(ej) {
  return {
    ...ej,
    regresion: regresionDe(ej.nombre),
    progresion: progresionDe(ej.nombre)
  };
}

module.exports = {
  REGLAS,
  regresionDe,
  progresionDe,
  decorar
};
