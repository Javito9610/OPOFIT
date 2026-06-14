/**
 * EjercicioExplicacionService — Coach virtual a nivel Strava/Caliber/Future.
 *
 * Para cada ejercicio devuelve 5 SECCIONES profesionales:
 *
 *   1. SETUP — posición inicial: dónde colocarse, qué agarrar, qué activar.
 *   2. EJECUCIÓN — la repetición paso a paso, con respiración.
 *   3. COACHING CUES — 2-4 frases cortas que un entrenador soltaría en
 *      mitad de la serie ("rodilla afuera", "pecho alto", "empuja el suelo").
 *      Son las que cambian el resultado del set. Apple Fitness+ las usa.
 *   4. ERRORES COMUNES — qué evitar. Caliber dedica una pestaña entera.
 *   5. POR QUÉ ESTE EJERCICIO — la conexión con la prueba/oposición. Si el
 *      usuario no entiende para qué entrena, abandona.
 *
 * Antes el banco devolvía 1 frase suelta tipo "Escápulas retraídas, toque
 * pecho controlado." — lo que el usuario llamó "una mierda". Ahora cada
 * ejercicio sale con 4-6 líneas profesionales agrupadas y jerarquizadas.
 *
 * La clave: NO es texto genérico — se genera a partir del patrón de
 * movimiento, el grupo muscular y el nombre concreto. Un press banca y
 * una flexión no comparten texto.
 */

const Patron = require('./PatronMovimientoService');

function normalizar(s) {
  return String(s || '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();
}

// =============================================================================
// FICHAS por patrón de movimiento. Cada ficha contiene las 4 plantillas que
// luego se especializan por nombre concreto.
// =============================================================================

const FICHAS = {
  PULL_V: {
    setup: 'Cuélgate de la barra con agarre prono o supino, escápulas activas y abdomen apretado. Pies fuera del suelo y piernas estables.',
    ejecucion: 'Tira hacia arriba iniciando el movimiento desde la espalda (no de los brazos), llevando el pecho hacia la barra. Exhala al subir, inhala al bajar 2 segundos controlando la excéntrica.',
    cues: [
      'Pecho hacia la barra, no barbilla',
      'Aprieta omóplatos al final del tirón',
      'No balancees: la energía sale de la espalda'
    ],
    errores: [
      'Balanceo con kipping (rompe la técnica si no eres CrossFitter avanzado)',
      'Bajar en caída libre sin frenar (atrofia el ejercicio)',
      'Codos completamente abiertos: estresa hombros'
    ],
    porque: 'Patrón nº1 en el baremo de muchas oposiciones (Policía Nacional, Guardia Civil). Mejora pull-ups en prueba y previene desequilibrios push/pull.'
  },
  PULL_H: {
    setup: 'Tronco firme con bisagra de cadera ligera (~20°) y core activo. Escápulas listas para abrirse y cerrarse.',
    ejecucion: 'Tira llevando los codos hacia atrás y abajo, aprieta los omóplatos al final del recorrido. Inhala al alargar, exhala al tirar.',
    cues: [
      'Inicia el tirón con los omóplatos, no con los bíceps',
      'Codos cerca del torso',
      '1 segundo de pausa en contracción máxima'
    ],
    errores: [
      'Usar inercia del cuerpo para "engañar"',
      'Tirón solo de brazo sin retraer escápula',
      'Encoger hombros hacia las orejas'
    ],
    porque: 'Equilibra horizontalmente el press de pecho y construye base para dominadas. Crítico para postura de aspirantes que pasan horas estudiando.'
  },
  PUSH_H: {
    setup: 'Escápulas retraídas y deprimidas (hacia abajo y atrás), arco lumbar natural sin exagerar, pies firmes plantados. Glúteos activos.',
    ejecucion: 'Baja la carga al esternón en 2-3 segundos manteniendo codos en ángulo de ~45° con el torso. Empuja en línea recta sin rebotar. Exhala al empujar.',
    cues: [
      'Codos a 45°, no 90° (protege hombros)',
      'Aprieta la barra hasta partirla',
      'Empuja el suelo con los pies'
    ],
    errores: [
      'Rebotar la barra en el pecho',
      'Codos completamente abiertos (lesiona hombros)',
      'Levantar los glúteos del banco'
    ],
    porque: 'Patrón fundamental para empujes torácicos. Mejora capacidad de empujar (defensa personal, intervención).'
  },
  PUSH_V: {
    setup: 'De pie, pies a la anchura de cadera y core duro como una tabla. Glúteos apretados para no arquear lumbar. Barra/mancuernas a la altura de los hombros, codos ligeramente delante.',
    ejecucion: 'Empuja en línea vertical pasando la cabeza ligeramente hacia delante al final. Inhala antes, exhala al empujar. Baja con control en 2 segundos.',
    cues: [
      'Aprieta glúteos y abdomen antes de empujar',
      'Cabeza atraviesa la ventana al bloquear arriba',
      'No arquees lumbar: si no aguantas el peso, baja la carga'
    ],
    errores: [
      'Hiperextender lumbar para "ayudar"',
      'Empujar hacia delante en lugar de vertical',
      'Bloquear los codos bruscamente'
    ],
    porque: 'Empuje vertical = fuerza para superar obstáculos (muros, vallas) en pruebas de circuito (Guardia Civil, Bomberos, Mossos).'
  },
  SQUAT: {
    setup: 'Pies a anchura de hombros con puntas ligeramente abiertas (15-30°), pecho alto, core apretado. Mirada al frente. Carga sobre trapecio superior si es barra trasera.',
    ejecucion: 'Inicia con cadera hacia atrás Y rodilla flexionando a la vez (no solo cadera). Baja hasta paralelo o más profundo, mantén espalda neutra. Empuja el suelo para subir. Inhala al bajar, exhala al subir.',
    cues: [
      'Rodillas en línea con las puntas de los pies',
      'Empuja el suelo, no solo elevas la cadera',
      'Pecho alto durante TODA la repetición'
    ],
    errores: [
      'Valgo de rodilla (rodilla hacia dentro)',
      'Lumbar redondeado abajo (butt wink)',
      'Talones despegándose del suelo'
    ],
    porque: 'Rey de los ejercicios de pierna. Base para sprint, saltos y agilidad. Núcleo del baremo Policía/Bombero/Militar.'
  },
  HINGE: {
    setup: 'Pies bajo la cadera, agarre algo más ancho que las piernas si es barra. Brazos extendidos, hombros ligeramente delante de la barra. Lumbar neutra y dorsales activadas.',
    ejecucion: 'Inicia empujando la cadera hacia atrás (no doblando rodillas), mantén la barra cerca del cuerpo. Empuja el suelo y extiende cadera apretando glúteos arriba. Inhala arriba, exhala al subir.',
    cues: [
      'Barra contigo, casi pegada al muslo',
      'Empuja el suelo, no tires hacia arriba',
      'Bisagra de cadera, no sentadilla'
    ],
    errores: [
      'Redondear la espalda baja (peligro de hernia)',
      'Hiperextender lumbar arriba',
      'Iniciar con rodillas (eso es sentadilla, no PM)'
    ],
    porque: 'El patrón más transferible al mundo real: levantar peso del suelo, saltar, sprintar. Glúteo + isquios = motor del cuerpo.'
  },
  LUNGE: {
    setup: 'De pie con pies separados a la anchura de cadera, tronco erguido. Da un paso largo (zancada) o ya estás en posición split. Tronco vertical, peso sobre la pierna delantera.',
    ejecucion: 'Baja la rodilla trasera hasta casi tocar el suelo, manteniendo la pierna delantera vertical en la tibia. Empuja con el talón delantero para volver. Inhala al bajar, exhala al empujar.',
    cues: [
      'Tronco vertical, no inclinado hacia delante',
      'Rodilla delantera no pasa de la punta del pie',
      'Empuja con el talón delantero'
    ],
    errores: [
      'Inclinar el tronco para "asistir"',
      'Pasos demasiado cortos (la rodilla se mete)',
      'No bajar suficiente la rodilla trasera'
    ],
    porque: 'Unilateral: descubre asimetrías y las corrige. Esencial para deportes con cambios de dirección (oposiciones físicas + carrera).'
  },
  PLYO: {
    setup: 'Pies a anchura de cadera, ligera flexión de rodillas y cadera, brazos atrás como en plano de despegue. Mirada al frente.',
    ejecucion: 'Salta explosivamente extendiendo cadera, rodilla y tobillo a la vez. Cae con piernas semiflexionadas (no rígidas) y absorbe el impacto. Pausa entre saltos si las series son cortas.',
    cues: [
      'Aterrizaje silencioso: si suena, falló',
      'Brazos te impulsan: úsalos',
      'Calidad >>> velocidad: descansa entre reps'
    ],
    errores: [
      'Aterrizar con piernas rígidas',
      'Rodillas hacia dentro al aterrizar',
      'Encadenar saltos sin descansar'
    ],
    porque: 'La pliometría transfiere fuerza a velocidad. Mejora el SALTO VERTICAL y el SPRINT corto (ambos en baremo).'
  },
  SPRINT: {
    setup: 'Calienta 10 min hasta sudar. Posición de salida: pie adelantado a la línea, peso en el de atrás, brazos preparados a 90°.',
    ejecucion: 'Arranca explosivo con primeros 10-15m de aceleración (cuerpo inclinado, brazos cortos y rápidos). Sube progresivamente a posición erguida con técnica de carrera. Descanso COMPLETO entre sprints (1:10 a 1:20).',
    cues: [
      'Rodilla alta, talón al glúteo',
      'Brazos te marcan el ritmo, no las piernas',
      'Cada sprint al 95-100%, sino no entrenas velocidad'
    ],
    errores: [
      'Salir al 100% sin calentar = lesión segura',
      'Descansos cortos: degrada técnica y se convierte en HIIT',
      'Mirar al suelo (rompe la postura)'
    ],
    porque: 'El sprint es PRUEBA OFICIAL en casi todas las oposiciones físicas. Y trabajarlo mejora capacidad neuromuscular.'
  },
  AGI: {
    setup: 'Marca el patrón en el suelo (T-test, escalera, conos). Calienta bien la cadera con movilidad. Posición de salida lista.',
    ejecucion: 'Recorre el patrón al 80% de velocidad las 2 primeras vueltas para fijar la técnica. Luego al 100%. Mantén el cuerpo bajo en los cambios de dirección y mira el siguiente cono, no el suelo.',
    cues: [
      'Cuerpo bajo en los cambios de dirección',
      'Pies rápidos y cortos',
      'Mira el siguiente cono, no el pie'
    ],
    errores: [
      'Mirar al suelo (perdiendo velocidad y postura)',
      'Cuerpo demasiado erguido en los giros',
      'Vueltas inconsistentes en velocidad'
    ],
    porque: 'Agilidad = cambios de dirección a velocidad. Es exactamente lo que evalúa el course-test de Guardia Civil y los circuitos de Policía Local.'
  },
  LOCO: {
    setup: 'Hidrátate antes (200-300 ml 30 min antes). Calienta con 5 min suaves antes del ritmo objetivo. Ropa transpirable.',
    ejecucion: 'Mantén un ritmo que puedas hablar en frases cortas (Z2 conversacional). Pisada activa pero no estridente. Brazos a 90°, relajados, sin cruzar la línea media.',
    cues: [
      'Cadencia 170-180 ppm para correr eficiente',
      'Respiración por nariz si puedes (mejora VO2)',
      'Si no puedes hablar, baja el ritmo'
    ],
    errores: [
      'Salir demasiado rápido: el ritmo se va',
      'Pasos largos golpeando con el talón',
      'Tronco rígido (mata la mecánica)'
    ],
    porque: 'Base aeróbica = capacidad de recuperación entre esfuerzos. Mejora carrera larga (Policía Local 1000-2000m, Bomberos).'
  },
  ANTI_EXT: {
    setup: 'Posición de plancha frontal/lateral según indique el ejercicio. Cuerpo en línea recta de talones a coronilla, abdomen apretado, glúteos activos.',
    ejecucion: 'Mantén la posición isométrica respirando con normalidad (no aguantes el aire). Para cuando la cadera cae o se eleva en exceso.',
    cues: [
      'Glúteos activos, no solo abdomen',
      'Cuerpo en LÍNEA, no comba ni montaña',
      'Respira durante toda la posición'
    ],
    errores: [
      'Hundir la cadera (rompe protección lumbar)',
      'Subir demasiado la cadera (rompe carga abdominal)',
      'Aguantar la respiración'
    ],
    porque: 'El core anti-extensión PROTEGE la lumbar en cualquier carga (peso muerto, sprint, saltar). Es la "musculatura invisible" del rendimiento.'
  },
  ROT: {
    setup: 'Posición indicada (de pie con cable lateral, decúbito con pelota, etc.). Pies firmes, cadera neutra. Manos juntas o sujetando la carga.',
    ejecucion: 'Movimiento controlado en el plano horizontal, generado desde el core (no de los brazos). Pausa breve en máxima rotación, vuelve con la misma técnica.',
    cues: [
      'Rotación desde el ombligo, no de los brazos',
      'Cadera fija, solo rota el tronco',
      'Movimiento lento y controlado'
    ],
    errores: [
      'Mover los brazos sin rotar el tronco',
      'Rotar también la cadera',
      'Usar inercia para acelerar'
    ],
    porque: 'Generar potencia rotacional = lanzar, golpear, cambiar de dirección. Para la prueba de fuerza y para defensa personal.'
  },
  CARRY: {
    setup: 'Recoge la carga con técnica de peso muerto (no agachándote con espalda). Tronco vertical, escápulas activas. Cabeza alta.',
    ejecucion: 'Camina con pasos cortos y firmes, manteniendo la postura erguida. Si es una sola mano: no inclines el tronco hacia el lado pesado. Respira por nariz.',
    cues: [
      'Mira al frente, no a la carga',
      'Pasos cortos y firmes',
      'Aprieta las omóplatos hacia atrás durante todo el recorrido'
    ],
    errores: [
      'Inclinar el tronco hacia la carga',
      'Pasos largos (descontrol)',
      'Encoger hombros hacia las orejas'
    ],
    porque: 'Movimiento súper real: cargar a alguien, llevar equipo. Trabaja todo el core, agarre y postura en un solo gesto.'
  },
  MOB: {
    setup: 'Espacio cómodo, ropa que no limite movimiento. Si es un patrón complejo, mira un vídeo de referencia antes.',
    ejecucion: 'Movimiento LENTO y controlado, sin rebotes. Llega al rango cómodo sin dolor y aguanta 1-2 segundos antes de volver. Respira profundo durante todo el ejercicio.',
    cues: [
      'Sin dolor: si pincha, sales del rango',
      'Respiración profunda toda la posición',
      'Rango progresivo, no fuerces'
    ],
    errores: [
      'Rebotar al final del rango',
      'Aguantar la respiración',
      'Buscar dolor pensando que es bueno'
    ],
    porque: 'Movilidad = previene lesiones + mejora rendimiento de TODOS los demás ejercicios.'
  }
};

// =============================================================================
// Overrides por NOMBRE concreto. Si tenemos información específica de un
// movimiento (dominada vs muscle-up, sentadilla vs sentadilla goblet), aquí.
// =============================================================================

const OVERRIDES = [
  {
    match: /dominada(?!.*asistid)/,
    setup: 'Agarre PRONO (palmas mirando al frente) más ancho que los hombros. Hombros lejos de las orejas, escápulas activas, abdomen apretado.',
    porque: 'La dominada estricta es CRÍTICA en Policía Nacional, Guardia Civil y Mossos. Es lo que separa nota 5 de nota 9.'
  },
  {
    match: /dominada.*asistid/,
    setup: 'Banda de resistencia anclada a la barra, rodillas o pies apoyados sobre la banda. Cuelga con escápulas activas.',
    ejecucion: 'Tira con la misma técnica que la dominada normal — la banda solo te ayuda en el punto más débil (bajo). Concéntrate en el control, no en hacer más reps.',
    porque: 'Progresión obligada hasta sumar 3-5 dominadas estrictas. Mejor que las negativas para construir patrón.'
  },
  {
    match: /muscle.up/,
    setup: 'Agarre falso (muñecas por encima de la barra) si dominas la técnica. Si no, agarre normal. Cuelga con tensión en todo el cuerpo.',
    ejecucion: 'Tira EXPLOSIVO llevando el pecho hacia la barra y la cabeza hacia atrás. Cuando estés a la altura del esternón, transiciona los codos por encima de la barra y empuja para bloquear arriba.',
    porque: 'Skill avanzado de calistenia. No es prueba oficial pero indica fuerza relativa élite.'
  },
  {
    match: /press banca/,
    setup: 'Tumbado con ojos bajo la barra. 5 puntos de contacto: cabeza, hombros, glúteos, ambos pies. Escápulas retraídas y deprimidas creando una plataforma firme.',
    porque: 'No es prueba directa pero construye fuerza torácica que mejora flexiones, lanzamientos y empujes en intervenciones.'
  },
  {
    match: /flexion(?!.*archer|.*planche|.*diamant)/,
    setup: 'Manos bajo los hombros, cuerpo en línea recta de talones a cabeza, mirada al suelo entre las manos.',
    porque: 'Prueba oficial en oposiciones de mujer (Policía Local, Guardia Civil) y benchmark de fuerza relativa para todos.'
  },
  {
    match: /sentadilla/,
    porque: 'Mejora directamente el salto vertical (prueba oficial), la potencia de sprint y la capacidad de mantener postura con peso.'
  },
  {
    match: /peso muerto/,
    porque: 'Movimiento más eficiente del gym: carga el cuerpo entero. Construye la cadena posterior (glúteo, isquios, espalda) clave para sprint, salto y postura.'
  },
  {
    match: /sprint/,
    setup: 'Calienta 12-15 min: trote suave + movilidad + 3-4 progresiones a 60-80%. Salida desde parado o desde 3 metros andando.',
    porque: 'PRUEBA OFICIAL en Policía Local (50-60m), Guardia Civil, Mossos, Bomberos. Trabajar el sprint mejora la nota directamente.'
  },
  {
    match: /conos en t|t-test/,
    setup: 'Marca el patrón de T-test en el suelo: 3 conos formando una T con 5m de base y 10m de altura.',
    ejecucion: 'Sprint al cono central, lateral derecho al cono derecho (mirando frente), lateral izquierdo al cono izquierdo (mirando frente), lateral de vuelta al central, sprint reverso al inicio.',
    porque: 'T-test es PRUEBA OFICIAL en Bomberos, Policía Local y course-test de Guardia Civil. Medible y entrenable.'
  },
  {
    match: /carrera continua|trote|rodaje/,
    porque: 'Base aeróbica = recuperación entre intervalos + capacidad de mantener intensidad. Mejora 1000m, 2000m y course-test.'
  },
  {
    match: /natacion|natación/,
    setup: 'Calentamiento: 100-200m suaves cambiando estilos. Gafas, gorro y ropa cómoda. Hidratación previa (sí, también en piscina).',
    porque: 'Prueba oficial en Bomberos y algunas opciones de Guardia Civil. Construye capacidad respiratoria que mejora TODA la resistencia.'
  }
];

// =============================================================================
// Render: combina ficha por patrón + override por nombre.
// =============================================================================

function explicar(ejercicio, ctx = {}) {
  const patron = Patron.clasificar(ejercicio);
  const ficha = FICHAS[patron] || FICHAS.SQUAT;
  const nombre = normalizar(ejercicio.nombre);
  const override = OVERRIDES.find((o) => o.match.test(nombre)) || {};

  // Override toma prioridad por sección.
  const setup = override.setup || ficha.setup;
  const ejecucion = override.ejecucion || ficha.ejecucion;
  const cues = override.cues || ficha.cues;
  const errores = override.errores || ficha.errores;
  const porque = override.porque || ficha.porque;

  return {
    setup,
    ejecucion,
    coaching_cues: cues,
    errores_comunes: errores,
    porque,
    patron_movimiento: patron
  };
}

/** Devuelve un único string concatenado con secciones, para clientes legacy
 *  que solo aceptan `instrucciones_tecnicas` como texto. Compatibilidad.
 */
function explicarPlano(ejercicio, ctx = {}) {
  const e = explicar(ejercicio, ctx);
  const cuesTxt = e.coaching_cues.map((c) => `• ${c}`).join('\n');
  const erroresTxt = e.errores_comunes.map((c) => `• ${c}`).join('\n');
  return [
    `Setup: ${e.setup}`,
    `Ejecución: ${e.ejecucion}`,
    `Claves del entrenador:\n${cuesTxt}`,
    `Errores a evitar:\n${erroresTxt}`,
    `Por qué entrenas esto: ${e.porque}`
  ].join('\n\n');
}

module.exports = {
  FICHAS,
  OVERRIDES,
  explicar,
  explicarPlano
};
