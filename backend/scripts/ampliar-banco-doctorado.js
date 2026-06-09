/**
 * Ampliación "doctorado en ciencias del deporte" del banco de ejercicios.
 *
 * Añade ~500 ejercicios profesionales cubriendo:
 *   • Calistenia (street workout, freestyle, progresiones front/back lever, planche,
 *     handstand, muscle-up, human flag, pistol squat, dragon flag, etc.)
 *   • CrossFit / Olympic lifting (snatch, clean & jerk, sus power/hang/squat variantes,
 *     gymnastics — toes-to-bar, bar/ring muscle-up, HSPU, rope climb, wall walks)
 *   • WODs clásicos benchmark (Fran, Cindy, Helen, Murph, Diane, Grace, Annie,
 *     Karen, Mary, Angie, Barbara, Chelsea, Filthy Fifty, Nancy)
 *   • Modalidades de programación: EMOM, AMRAP, For Time, Tabata, Death by,
 *     Chipper, Ladder
 *   • Material específico: TRX, kettlebell, mancuernas, gomas, comba, saco,
 *     mazas (mace), trineo (sled), anillas, bolsa búlgara
 *   • Mobility / prep articular (CARS, World's greatest, banded distractions)
 *   • Tests/pruebas físicas reales: curso de bombero, pista militar, BLA,
 *     dominadas máx., 30" abdominales test, course navette
 *
 * Cada ejercicio incluye dos campos nuevos:
 *   - modalidad: "convencional" | "calistenia" | "crossfit_lift" | "wod" |
 *                "emom" | "amrap" | "for_time" | "tabata" | "gimnastico_prog" |
 *                "test" | "movilidad" | "cardio"
 *   - score_tipo: cómo se mide la performance del ejercicio
 *                 "reps" | "tiempo" | "tiempo_max" | "rondas" | "rondas_reps" |
 *                 "peso" | "distancia" | "rpe" | "calorias"
 *
 * Estos campos los lee el frontend para mostrar el input correcto (timer para
 * AMRAP, contador de rondas para EMOM, etc.) y el historial para graficar.
 *
 * Bibliografía guía:
 *   - Schoenfeld, B. (2010). Mecanismos hipertrofia muscular.
 *   - Daniels, J. (2014). Running Formula (vVO2max, T-pace, M-pace, I-pace, R-pace).
 *   - Helgerud, J. (2007). HIIT 4×4 protocolo.
 *   - Issurin, V. (2010). Block periodization.
 *   - Kraemer & Ratamess (2004). NSCA reps × series por objetivo.
 *   - Glassman, G. (CrossFit). General Physical Preparedness.
 *   - Brewer, A. (2017). Gymnastics for everyone.
 *   - Kavadlo, A. & D. (Progressive Calisthenics). Bar Athletes.
 *   - Cuerpo Nacional de Policía / Guardia Civil / Bombero. Baremos oficiales 2024.
 */

const fs = require('fs');
const path = require('path');

const RUTA_BANCO = path.resolve(__dirname, '../data/ejercicios-banco-500.json');

// =====================================================================
//  CATÁLOGO POR CATEGORÍAS
// =====================================================================

// ---------- 1. CALISTENIA: progresiones completas ----------
const CALISTENIA = [
  // Push: variantes de flexiones (estímulos diferentes según ángulo/inclinación)
  ['Flexiones diamante', 'Tríceps', 'Suelo', 'Manos juntas formando diamante bajo el pecho. Codos pegados al torso, tríceps trabaja máx.', 'CALISTENIA,CASA,GYM,MIXTO', 'PUSH'],
  ['Flexiones declinadas (pies elevados)', 'Pecho', 'Banco', 'Pies en banco 30-50 cm. Activa porción clavicular del pectoral mayor.', 'CALISTENIA,GYM,CASA,MIXTO', 'PUSH'],
  ['Flexiones inclinadas (manos elevadas)', 'Pecho', 'Caja', 'Manos en banco/caja. Progresión inicial hacia flexión estándar.', 'CALISTENIA,GYM,CASA,MIXTO', 'PUSH'],
  ['Flexiones pseudo planche', 'Pecho', 'Suelo', 'Manos a la altura de la cadera, dedos hacia atrás, hombros adelantados. Anti-pre-planche.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Flexiones arqueras (archer push-up)', 'Pecho', 'Suelo', 'Una mano flexiona, la otra extendida lateral. Asimétrica → unilateral.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Flexiones typewriter', 'Pecho', 'Suelo', 'Desplaza el cuerpo lateralmente de una mano a otra abajo. Excéntrica intensa.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Flexiones hindú (hindu push-up)', 'Pecho', 'Suelo', 'Movimiento ondulante perro abajo→cobra. Yoga + flexión combinadas.', 'CALISTENIA,CASA,MIXTO', 'PUSH'],
  ['Flexiones a una mano (progresión)', 'Pecho', 'Suelo', 'Pies abiertos para equilibrio. Trabajar primero en pared o banco.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Flexiones hindú a un brazo', 'Pecho', 'Suelo', 'Ondulación con un solo brazo de apoyo. Avanzado.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Pseudo planche lean (isométrico)', 'Pecho', 'Suelo', 'Plancha frontal con hombros muy adelantados. 10-30 s. Pre-planche.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Tuck planche (isométrico)', 'Pecho', 'Paralelas', 'Brazos extendidos, rodillas al pecho, suspendido. 5-20 s.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Advanced tuck planche (isométrico)', 'Pecho', 'Paralelas', 'Espalda paralela al suelo, rodillas separadas del torso. Progresión planche.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Straddle planche (isométrico)', 'Pecho', 'Paralelas', 'Piernas separadas en V, cuerpo horizontal. Pre full planche.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Full planche (isométrico)', 'Pecho', 'Paralelas', 'Cuerpo totalmente extendido y horizontal sin tocar suelo. Skill élite.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Handstand push-up estricto (HSPU)', 'Hombros', 'Pared', 'Pino contra pared, baja la cabeza al suelo y empuja. Rango completo.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'PUSH'],
  ['Handstand push-up libre', 'Hombros', 'Suelo', 'Sin apoyo en pared. Requiere control corporal avanzado.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Pike push-up', 'Hombros', 'Suelo', 'Cadera elevada (V invertida). Trabaja deltoides anterior. Pre-HSPU.', 'CALISTENIA,CASA,GYM,MIXTO', 'PUSH'],
  ['Pike push-up elevado', 'Hombros', 'Banco', 'Pies elevados, posición más vertical. Más demanda hombro.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Wall walk', 'Hombros', 'Pared', 'Desde plancha, sube los pies por la pared hasta vertical. Tocas pared con manos.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'PUSH'],

  // Pull: dominadas variantes (toda la progresión muscle-up)
  ['Dominadas supinas (chin-up)', 'Espalda', 'Barra dominadas', 'Agarre palmas hacia ti, bíceps muy involucrado.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Dominadas neutras (paralelas)', 'Espalda', 'Barra dominadas', 'Palmas enfrentadas. Tracción más amigable con hombro.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Dominadas pronas ancho', 'Espalda', 'Barra dominadas', 'Agarre más ancho que hombros. Foco dorsal ancho.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Dominadas comando (commando)', 'Espalda', 'Barra dominadas', 'Cuerpo paralelo a la barra, alterna lados al subir. Unilateral.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Dominadas arquero (archer pull-up)', 'Espalda', 'Barra dominadas', 'Sube hacia un brazo, el otro extendido. Pre dominada a una mano.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Dominadas typewriter', 'Espalda', 'Barra dominadas', 'Arriba, desplázate lateralmente de un brazo al otro. Excéntrica horizontal.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Dominadas L-sit', 'Espalda', 'Barra dominadas', 'Piernas elevadas paralelas al suelo durante la dominada. Core + dorsal.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Dominadas explosivas', 'Espalda', 'Barra dominadas', 'Tira fuerte para llevar el pecho a la barra. Pre muscle-up.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'PULL'],
  ['Dominadas con palmada (clap)', 'Espalda', 'Barra dominadas', 'Suéltate arriba y aplaude. Pliométrico avanzado.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Dominadas a la nuca (no recomendado en hombros sensibles)', 'Espalda', 'Barra dominadas', 'Baja la nuca a la barra. CUIDADO con hombro. Solo si flexibilidad torácica suficiente.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Muscle-up estricto en barra', 'Espalda', 'Barra dominadas', 'Sube de dominada a fondo sin balanceo. Requiere ~12 dominadas estrictas previas.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'PULL'],
  ['Muscle-up kipping en barra', 'Espalda', 'Barra dominadas', 'Con impulso de cadera (kip). Movimiento WOD estándar.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Muscle-up en anillas estricto', 'Espalda', 'Anillas', 'Tracción + transición + fondo en anillas, sin balanceo. Skill avanzado.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'PULL'],
  ['Dominadas negativas (excéntricas)', 'Espalda', 'Barra dominadas', 'Sube saltando, baja en 3-5 s. Para principiantes que aún no hacen dominada estricta.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Australian pull-ups (filas invertidas)', 'Espalda', 'Barra baja', 'Cuerpo bajo barra, talones apoyados. Regresión dominada.', 'CALISTENIA,CASA,GYM,MIXTO', 'PULL'],
  ['Front lever tuck (isométrico)', 'Espalda', 'Barra dominadas', 'Colgado, rodillas al pecho, cuerpo horizontal. 5-20 s.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Front lever advanced tuck (isométrico)', 'Espalda', 'Barra dominadas', 'Rodillas separadas del torso. Progresión front lever.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Front lever straddle (isométrico)', 'Espalda', 'Barra dominadas', 'Piernas abiertas en V, cuerpo horizontal. Pre full front lever.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Full front lever (isométrico)', 'Espalda', 'Barra dominadas', 'Cuerpo totalmente extendido, paralelo al suelo. Skill élite.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Back lever tuck (isométrico)', 'Espalda', 'Barra dominadas', 'De espaldas al suelo, rodillas al pecho. 5-20 s.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Back lever straddle (isométrico)', 'Espalda', 'Barra dominadas', 'De espaldas, piernas en V. Pre full back lever.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Full back lever (isométrico)', 'Espalda', 'Barra dominadas', 'De espaldas al suelo, cuerpo totalmente extendido. Avanzado.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Skin the cat', 'Espalda', 'Anillas', 'Suspendido, pasa los pies por debajo hasta back lever invertido. Movilidad escapular.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['German hang', 'Espalda', 'Anillas', 'Cuelga invertido con brazos extendidos. Apertura hombro pasiva.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Ice cream maker', 'Espalda', 'Barra dominadas', 'Desde front lever extiende a colgado y vuelve. Excéntrica tracción horizontal.', 'CALISTENIA,GYM,MIXTO', 'PULL'],

  // Fondos / Dips
  ['Fondos paralelas estrictos', 'Tríceps', 'Paralelas', 'Codos paralelos al cuerpo. Baja hasta hombros bajo codos.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Fondos paralelas lastrados', 'Tríceps', 'Paralelas+lastre', 'Con cinturón + disco/peso. Fuerza pura.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Fondos en banco (banco dips)', 'Tríceps', 'Banco', 'Manos en banco, pies adelante. Regresión fondos paralelas.', 'CALISTENIA,CASA,GYM,MIXTO', 'PUSH'],
  ['Fondos anillas', 'Tríceps', 'Anillas', 'Inestable, alta demanda de estabilizadores. Avanzado.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Fondos coreanos (korean dips)', 'Tríceps', 'Barra horizontal', 'Sobre barra horizontal con piernas elevadas. Hiperextensión hombro.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Fondos búlgaros (bulgarian dips)', 'Tríceps', 'Anillas', 'En anillas con anillas giradas hacia fuera al final. Avanzado.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Fondos a una mano (progresión)', 'Tríceps', 'Paralelas', 'Asistido con un dedo de la otra mano. Skill avanzado.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],

  // Handstand / Equilibrio
  ['Pino contra pared (handstand hold)', 'Hombros', 'Pared', 'Báscula pélvica, costillas dentro, mirada entre manos. 20-60 s.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Pino libre (free handstand)', 'Hombros', 'Suelo', 'Sin pared. Control completo. Progresión: salir de pared.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Pino caminando', 'Hombros', 'Suelo', 'Avanza en pino. Equilibrio dinámico.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Press to handstand (negativos)', 'Hombros', 'Suelo', 'Desde pino, baja a pike controlado. Excéntrica.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Frog stand (cuervo)', 'Hombros', 'Suelo', 'Cuclillas, manos al suelo, rodillas sobre codos. Equilibrio inicial.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Crow pose / Bakasana', 'Core', 'Suelo', 'Equilibrio yoga. Progresión a planche.', 'CALISTENIA,CASA,MIXTO', 'PUSH'],

  // Core avanzado
  ['L-sit (paralelas)', 'Core', 'Paralelas', 'Sentado en suspensión con piernas extendidas paralelas al suelo. 10-30 s.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['L-sit en suelo', 'Core', 'Suelo', 'Manos en suelo, levanta el cuerpo con piernas extendidas. Más fácil que paralelas.', 'CALISTENIA,CASA,GYM,MIXTO', 'PUSH'],
  ['V-sit', 'Core', 'Suelo', 'L-sit con piernas elevadas en V. Avanzado.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Manna', 'Core', 'Paralelas', 'Piernas elevadas hasta tocar manos. Skill élite gimnástico.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Dragon flag', 'Core', 'Banco', 'Acostado, cuerpo recto elevado solo por hombros. Pro Bruce Lee.', 'CALISTENIA,GYM,MIXTO', 'CORE'],
  ['Dragon flag negativos', 'Core', 'Banco', 'Solo bajar lento desde la vertical. Excéntrica.', 'CALISTENIA,GYM,MIXTO', 'CORE'],
  ['Hollow hold', 'Core', 'Suelo', 'Tumbado boca arriba, lumbar pegada, hombros y piernas elevados. 20-60 s.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'CORE'],
  ['Hollow rocks', 'Core', 'Suelo', 'En hollow, balanceo cabeza-pies sin tocar suelo. Trabajo isométrico activo.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'CORE'],
  ['Arco invertido (superman hold)', 'Core', 'Suelo', 'Boca abajo, brazos y piernas elevados. Antagónico al hollow.', 'CALISTENIA,GYM,MIXTO', 'CORE'],
  ['Toes-to-bar estrictos', 'Core', 'Barra dominadas', 'Colgado, lleva los pies a tocar la barra sin balanceo. Compresión.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'CORE'],
  ['Toes-to-bar kipping', 'Core', 'Barra dominadas', 'Con impulso de cadera. Movimiento WOD estándar.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'CORE'],
  ['Knees-to-elbows', 'Core', 'Barra dominadas', 'Colgado, lleva las rodillas hasta los codos. Pre toes-to-bar.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'CORE'],
  ['Hanging leg raise estricto', 'Core', 'Barra dominadas', 'Piernas extendidas, sube a 90°. Estricto sin balanceo.', 'CALISTENIA,GYM,MIXTO', 'CORE'],
  ['Windshield wipers', 'Core', 'Barra dominadas', 'Colgado con piernas en L, gira de un lado a otro. Oblicuos.', 'CALISTENIA,GYM,MIXTO', 'CORE'],
  ['Human flag tuck', 'Core', 'Barra vertical', 'Sujeto a barra vertical, cuerpo horizontal con rodillas al pecho. Pre full flag.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Human flag straddle', 'Core', 'Barra vertical', 'Piernas en V, cuerpo horizontal. Pre full flag.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Human flag completo', 'Core', 'Barra vertical', 'Cuerpo totalmente horizontal. Skill élite oblicuos + hombros.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],

  // Piernas: progresión pistol squat
  ['Sentadilla asistida con TRX', 'Cuádriceps', 'TRX', 'Sujetando TRX para iniciar profundidad.', 'CALISTENIA,GYM,CASA,MIXTO', 'SQUAT'],
  ['Sentadilla a caja', 'Cuádriceps', 'Caja', 'Baja hasta sentar y vuelve. Patrón controlado.', 'CALISTENIA,GYM,CROSSFIT,MIXTO', 'SQUAT'],
  ['Sentadilla cossack', 'Aductores', 'Suelo', 'Pies muy abiertos, baja a un lado con la otra pierna extendida lateral. Cadera + aductores.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'SQUAT'],
  ['Sentadilla shrimp', 'Cuádriceps', 'Suelo', 'Pierna trasera flexionada cogida con la mano. Pre pistol.', 'CALISTENIA,GYM,MIXTO', 'SQUAT'],
  ['Sentadilla pistol asistida', 'Cuádriceps', 'TRX o pared', 'Sujetándote con TRX o pared. Aprende patrón.', 'CALISTENIA,GYM,CASA,MIXTO', 'SQUAT'],
  ['Sentadilla pistol completa', 'Cuádriceps', 'Suelo', 'Una pierna extendida adelante, baja completamente con la otra. Avanzado.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'SQUAT'],
  ['Sentadilla pistol con peso', 'Cuádriceps', 'Mancuerna', 'Pistol sujetando peso al pecho para contrapeso. Variante avanzada.', 'CALISTENIA,GYM,MIXTO', 'SQUAT'],
  ['Sentadilla búlgara', 'Cuádriceps', 'Banco', 'Pie trasero en banco, baja con la pierna delantera. Unilateral muy eficaz.', 'CALISTENIA,GYM,CASA,MIXTO', 'SQUAT'],
  ['Sentadilla sissy', 'Cuádriceps', 'Suelo', 'Talones elevados, rodillas adelante, espalda inclinada atrás. Aislamiento cuádriceps.', 'CALISTENIA,GYM,MIXTO', 'SQUAT'],
  ['Salto pliométrico (jump squat)', 'Cuádriceps', 'Suelo', 'Sentadilla y salto explosivo. Potencia.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'SQUAT'],
  ['Salto a caja', 'Cuádriceps', 'Caja', 'Salto desde suelo a caja 50-75 cm. Reactividad.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'SQUAT'],
  ['Saltos sobre vallas', 'Cuádriceps', 'Vallas', 'Salto a dos pies sobre vallas 20-50 cm. Reactividad pliométrica.', 'CROSSFIT,GYM,PISTA,MIXTO', 'SQUAT'],

  // Skills extra calistenia
  ['Burpee con dominada', 'Full body', 'Barra dominadas', 'Burpee + salto a barra + dominada. Cardio + tracción.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'BURPEE'],
  ['Burpee con muscle-up', 'Full body', 'Barra dominadas', 'Burpee + muscle-up. Combo élite.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'BURPEE'],
  ['Burpee con palmada', 'Full body', 'Suelo', 'Burpee + flexión con palmada. Explosividad.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'BURPEE'],
];

// ---------- 2. CROSSFIT: Olympic lifts + gymnastics + monostructural ----------
const CROSSFIT_MOVS = [
  // Olympic lifts y variantes
  ['Power snatch', 'Full body', 'Barra olímpica', 'Arrancada con recepción en sentadilla parcial (rodillas >90°). Técnica.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Hang power snatch', 'Full body', 'Barra olímpica', 'Snatch desde colgado (rodilla/cadera). Acentúa segundo tirón.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Squat snatch (full)', 'Full body', 'Barra olímpica', 'Arrancada con recepción en sentadilla completa overhead.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Muscle snatch', 'Hombros', 'Barra olímpica', 'Snatch sin re-flexión de rodillas. Pull técnico.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Snatch balance', 'Hombros', 'Barra olímpica', 'Desde overhead, baja a sentadilla manteniendo barra arriba. Receptividad.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Overhead squat', 'Cuádriceps', 'Barra olímpica', 'Sentadilla con barra overhead. Movilidad y estabilidad combinadas.', 'CROSSFIT,GYM,MIXTO', 'SQUAT'],
  ['Power clean', 'Full body', 'Barra olímpica', 'Cargada con recepción parcial. Velocidad explosiva.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Hang power clean', 'Full body', 'Barra olímpica', 'Clean desde colgado. Aprendizaje del segundo tirón.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Squat clean (full)', 'Full body', 'Barra olímpica', 'Cargada con sentadilla completa de recepción.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Hang squat clean', 'Full body', 'Barra olímpica', 'Clean desde colgado con sentadilla completa.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Push jerk', 'Hombros', 'Barra olímpica', 'Empujón con re-flexión de rodillas para recepción overhead.', 'CROSSFIT,GYM,MIXTO', 'PUSH'],
  ['Split jerk', 'Hombros', 'Barra olímpica', 'Recepción overhead con piernas en split (zancada). Más estable.', 'CROSSFIT,GYM,MIXTO', 'PUSH'],
  ['Push press', 'Hombros', 'Barra olímpica', 'Impulso de piernas + empujón. Sin re-flexión de recepción.', 'CROSSFIT,GYM,MIXTO', 'PUSH'],
  ['Strict press (military)', 'Hombros', 'Barra olímpica', 'Press vertical sin impulso. Fuerza pura del hombro.', 'CROSSFIT,GYM,MIXTO', 'PUSH'],
  ['Clean and jerk', 'Full body', 'Barra olímpica', 'Cargada completa + envión. Movimiento olímpico completo.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Snatch grip deadlift', 'Espalda baja', 'Barra olímpica', 'Peso muerto con agarre snatch (ancho). Espalda alta.', 'CROSSFIT,GYM,MIXTO', 'PULL'],
  ['Clean grip deadlift', 'Espalda baja', 'Barra olímpica', 'Peso muerto con agarre clean (ancho hombros). Técnico.', 'CROSSFIT,GYM,MIXTO', 'PULL'],
  ['Thruster', 'Full body', 'Barra olímpica', 'Front squat + push press combinados. WOD clásico.', 'CROSSFIT,GYM,MIXTO', 'SQUAT'],
  ['Wall ball', 'Full body', 'Balón medicinal', 'Sentadilla + lanzamiento balón 9 kg a diana 3,05 m. WOD esencial.', 'CROSSFIT,GYM,MIXTO', 'SQUAT'],

  // Gymnastics
  ['Bar muscle-up estricto', 'Espalda', 'Barra dominadas', 'Sin kip. Tracción explosiva + transición + fondo.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Bar muscle-up kipping', 'Espalda', 'Barra dominadas', 'Con balanceo (kip). Movimiento WOD.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Ring muscle-up', 'Espalda', 'Anillas', 'En anillas, requiere transición técnica de codos.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Kipping pull-up', 'Espalda', 'Barra dominadas', 'Con impulso. Permite más reps en WOD.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Butterfly pull-up', 'Espalda', 'Barra dominadas', 'Patrón cíclico continuo. Eficiencia máxima en WOD.', 'CROSSFIT,GYM,MIXTO', 'PULL'],
  ['Chest-to-bar pull-up', 'Espalda', 'Barra dominadas', 'Sube hasta tocar pecho con barra. Estándar competición.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Strict HSPU', 'Hombros', 'Pared', 'Pino, baja cabeza, sube estricto sin kip.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Kipping HSPU', 'Hombros', 'Pared', 'Con impulso de rodillas. Más reps en WOD.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Deficit HSPU', 'Hombros', 'Pared+discos', 'Cabeza por debajo del nivel de manos. Más rango.', 'CROSSFIT,GYM,MIXTO', 'PUSH'],
  ['Rope climb (cuerda)', 'Full body', 'Cuerda', '4-5 m. Subir con o sin pies (legless).', 'CROSSFIT,GYM,MIXTO', 'PULL'],
  ['Legless rope climb', 'Espalda', 'Cuerda', 'Solo con brazos. Avanzado.', 'CROSSFIT,GYM,MIXTO', 'PULL'],
  ['Pistol squat (CrossFit)', 'Cuádriceps', 'Suelo', 'Sentadilla a una pierna. Estándar competición.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'SQUAT'],
  ['Box jump over', 'Cuádriceps', 'Caja', 'Salto a caja y baja al otro lado. WOD.', 'CROSSFIT,GYM,MIXTO', 'SQUAT'],
  ['Box step-up', 'Cuádriceps', 'Caja', 'Subir caminando con peso. Versión escalable.', 'CROSSFIT,GYM,MIXTO', 'SQUAT'],
  ['Double unders (comba doble)', 'Cardio', 'Comba', 'Dos pasos de cuerda por salto. WOD esencial.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'JUMP'],
  ['Single unders (comba simple)', 'Cardio', 'Comba', 'Un paso por salto. Calentamiento o escalado.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'JUMP'],

  // Monostructural / máquinas
  ['Remo Concept2 distancia', 'Cardio', 'Remo Concept2', 'Pull con piernas-tronco-brazos. Damper 5-7. Ratio 1:1.', 'CROSSFIT,GYM,MIXTO', 'RUN'],
  ['Remo Concept2 calorías', 'Cardio', 'Remo Concept2', 'Score por kcal. Más eficiente con ratio explosivo.', 'CROSSFIT,GYM,MIXTO', 'RUN'],
  ['Echo Bike / Assault Bike calorías', 'Cardio', 'Echo bike', 'Bici con brazos y piernas. Score kcal.', 'CROSSFIT,GYM,MIXTO', 'RUN'],
  ['Ski Erg distancia', 'Cardio', 'Ski Erg', 'Patrón de doble bastón de esquí. Espalda alta + core.', 'CROSSFIT,GYM,MIXTO', 'RUN'],
  ['Burpee box jump over', 'Full body', 'Caja', 'Burpee + salto a caja y bajar al otro lado.', 'CROSSFIT,GYM,MIXTO', 'BURPEE'],

  // Dumbbell movements (gym hero divisions)
  ['DB snatch alterno', 'Full body', 'Mancuernas', 'Snatch con mancuerna alternando brazo cada rep.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['DB clean and jerk', 'Full body', 'Mancuernas', 'Mancuerna desde suelo a overhead.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['DB thruster', 'Full body', 'Mancuernas', 'Front squat + press con mancuernas.', 'CROSSFIT,GYM,MIXTO', 'SQUAT'],
  ['Devil press', 'Full body', 'Mancuernas', 'Burpee + snatch con dos mancuernas. WOD agresivo.', 'CROSSFIT,GYM,MIXTO', 'BURPEE'],
  ['DB walking lunge', 'Glúteos', 'Mancuernas', 'Zancadas caminando con mancuernas.', 'CROSSFIT,GYM,MIXTO', 'SQUAT'],
  ['DB renegade row', 'Espalda', 'Mancuernas', 'En plancha sobre mancuernas, rema alterno.', 'CROSSFIT,GYM,MIXTO', 'PULL'],

  // Sled / odd objects
  ['Sled push (trineo)', 'Cuádriceps', 'Trineo', 'Empuja trineo cargado 10-20 m. Brutal piernas + cardio.', 'CROSSFIT,GYM,MIXTO', 'RUN'],
  ['Sled drag (trineo)', 'Glúteos', 'Trineo', 'Tira de trineo con cuerda. Posterior.', 'CROSSFIT,GYM,MIXTO', 'RUN'],
  ['Sandbag clean', 'Full body', 'Saco arena', 'Carga saco a hombro desde suelo.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Sandbag carry', 'Full body', 'Saco arena', 'Camina con saco al hombro o pecho.', 'CROSSFIT,GYM,MIXTO', 'RUN'],
  ['Farmer\'s walk', 'Full body', 'Mancuernas/KB pesadas', 'Camina con peso máximo en cada mano. Agarre + core.', 'CROSSFIT,GYM,MIXTO', 'RUN'],
  ['Yoke carry', 'Full body', 'Yoke', 'Camina con barra cargada sobre los trapecios. Strongman.', 'GYM,MIXTO', 'RUN'],
];

// ---------- 3. WODs CLÁSICOS BENCHMARK ----------
const WODS = [
  // The Girls
  ['WOD Fran (21-15-9 thrusters 43kg + pull-ups)', 'Full body', 'Barra+barra dominadas', '3 rondas 21-15-9: thrusters 43/29 kg + pull-ups. Sub-5 min élite.', 'CROSSFIT,GYM,MIXTO', 'BURPEE'],
  ['WOD Cindy (AMRAP 20\' 5 pull-ups + 10 push-ups + 15 air squats)', 'Full body', 'Barra dominadas', '20 min AMRAP. Mide GPP. Objetivo 20+ rondas.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'BURPEE'],
  ['WOD Helen (3 rondas 400 m + 21 KB swing 24kg + 12 pull-ups)', 'Full body', 'KB+barra dominadas+pista', 'Sub-10 min en buena forma.', 'CROSSFIT,GYM,PISTA,MIXTO', 'RUN'],
  ['WOD Murph (1 mi + 100 pull-ups + 200 push-ups + 300 air squats + 1 mi)', 'Full body', 'Barra dominadas+pista', 'Memorial Day. Con chaleco 9 kg. Reparto óptimo: 20 rondas Cindy entre mi y mi.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'RUN'],
  ['WOD Diane (21-15-9 deadlift 102kg + HSPU)', 'Full body', 'Barra+pared', 'Sub-3 min élite, sub-6 muy bueno.', 'CROSSFIT,GYM,MIXTO', 'PULL'],
  ['WOD Grace (30 clean and jerks 60kg for time)', 'Full body', 'Barra olímpica', 'Sub-2 min élite, sub-5 muy bueno.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['WOD Annie (50-40-30-20-10 double unders + sit-ups)', 'Core+cardio', 'Comba', 'Sub-6 min objetivo.', 'CROSSFIT,GYM,MIXTO', 'CORE'],
  ['WOD Karen (150 wall balls 9kg for time)', 'Full body', 'Balón medicinal', 'Una sola tarea, romperla en bloques 25-30.', 'CROSSFIT,GYM,MIXTO', 'SQUAT'],
  ['WOD Mary (AMRAP 20\' 5 HSPU + 10 pistol + 15 pull-ups)', 'Full body', 'Pared+barra dominadas', '20\' AMRAP de skills gimnásticos avanzados.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'PULL'],
  ['WOD Angie (100 pull-ups + 100 push-ups + 100 sit-ups + 100 air squats for time)', 'Full body', 'Barra dominadas', 'Linear (cada movimiento completo antes del siguiente). Sub-20 muy bueno.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'BURPEE'],
  ['WOD Barbara (5 rondas 20 pull-ups + 30 push-ups + 40 sit-ups + 50 air squats, descanso 3\' entre rondas)', 'Full body', 'Barra dominadas', '5 rondas separadas con tiempo registrado por ronda.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'BURPEE'],
  ['WOD Chelsea (EMOM 30\' 5 pull-ups + 10 push-ups + 15 air squats)', 'Full body', 'Barra dominadas', 'Cada minuto durante 30 minutos. Cindy en EMOM.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'BURPEE'],
  ['WOD Nancy (5 rondas 400 m + 15 OHS 43kg)', 'Full body', 'Barra olímpica+pista', 'OHS = overhead squat. Movilidad + cardio.', 'CROSSFIT,GYM,PISTA,MIXTO', 'RUN'],
  ['WOD Filthy Fifty (50 reps cada uno: box jump, jumping pull-up, KB swing, walking lunge, K2E, push press, back ext, wall ball, burpee, double under)', 'Full body', 'Mixto', 'Chipper de 50 reps × 10 ejercicios. ~25-40 min.', 'CROSSFIT,GYM,MIXTO', 'BURPEE'],
  ['WOD DT (5 rondas 12 deadlift + 9 hang power clean + 6 push jerk 70kg)', 'Full body', 'Barra olímpica', 'Hero WOD. Mismo peso todo. Sub-10 muy bueno.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['WOD JT (21-15-9 HSPU + ring dips + push-ups)', 'Tríceps', 'Anillas+pared', 'Hero WOD. Triple push.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['WOD Kalsu (100 thrusters 60kg + 5 burpees al inicio de cada minuto)', 'Full body', 'Barra olímpica', 'Brutal. EMOM-style con burpees como penalización.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
];

// ---------- 4. FORMATOS PROGRAMACIÓN: EMOM, AMRAP, FOR TIME, TABATA ----------
const FORMATOS = [
  ['EMOM 10\' alternando 12 swings KB + 10 burpees', 'Full body', 'KB', 'Cada minuto un ejercicio. Descansa lo que sobre.', 'CROSSFIT,GYM,MIXTO', 'BURPEE'],
  ['EMOM 15\' 5 power clean 60% 1RM', 'Full body', 'Barra olímpica', 'Trabajo técnico de potencia.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['EMOM 20\' alternando 10 cal row + 10 push-ups + 10 air squats + descanso', 'Cardio', 'Remo', '4 estaciones. Tabula tu cadencia.', 'CROSSFIT,GYM,MIXTO', 'RUN'],
  ['EMOM 30\' Chelsea (5 pull-ups + 10 push-ups + 15 squats)', 'Full body', 'Barra dominadas', 'WOD benchmark en EMOM.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'BURPEE'],
  ['AMRAP 12\' 5 pull-ups + 10 push-ups + 15 air squats (Cindy 12\')', 'Full body', 'Barra dominadas', 'Versión corta de Cindy. Estándar test.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'BURPEE'],
  ['AMRAP 7\' burpees max', 'Full body', 'Suelo', 'Suma de burpees en 7 min. Test capacidad anaeróbica.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'BURPEE'],
  ['AMRAP 15\' 10 thruster 30 kg + 10 burpees over bar', 'Full body', 'Barra olímpica', 'Cardio + fuerza.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Tabata squats (20"on / 10"off ×8 rondas)', 'Cuádriceps', 'Suelo', '4 min total. Anaeróbico láctico.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'SQUAT'],
  ['Tabata burpees (20"on / 10"off ×8)', 'Full body', 'Suelo', '4 min total. Máx burpees por ronda → puntuación.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'BURPEE'],
  ['Tabata mountain climbers', 'Core', 'Suelo', '20"/10" ×8. Cardio + core.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'CORE'],
  ['Tabata combo (squat + push-up + sit-up + burpee, cada 2 rondas)', 'Full body', 'Suelo', '8 rondas combinadas. Test ATP-PC + láctico.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'BURPEE'],
  ['For time 100 burpees', 'Full body', 'Suelo', 'Test ATP + capacidad. Sub-7 muy bueno.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'BURPEE'],
  ['For time 50 thrusters 40 kg', 'Full body', 'Barra', 'Test capacidad láctica.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Death by burpees (1 minuto 1 burpee, +1 cada minuto)', 'Full body', 'Suelo', 'Hasta no completar las reps en el minuto. Test mental.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'BURPEE'],
  ['Death by thrusters 30 kg', 'Full body', 'Barra', 'Igual pero con thrusters.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Chipper 10-9-8-7-6-5-4-3-2-1 (10 movimientos)', 'Full body', 'Mixto', 'Descendente. Empieza con 10 reps de mov 1, 9 de mov 2, etc.', 'CROSSFIT,GYM,MIXTO', 'BURPEE'],
  ['Ladder ascendente 1-2-3-...-10 (pull-up + burpee)', 'Full body', 'Barra dominadas', 'Sumas 1+2+3+...+10 = 55 reps de cada.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'BURPEE'],
];

// ---------- 5. MATERIAL ESPECÍFICO ----------
const TRX = [
  ['TRX row', 'Espalda', 'TRX', 'Pies adelantados, tira del cuerpo hacia el anclaje.', 'GYM,CASA,MIXTO', 'PULL'],
  ['TRX row a un brazo', 'Espalda', 'TRX', 'Unilateral. Asimetría = más core.', 'GYM,CASA,MIXTO', 'PULL'],
  ['TRX archer pull', 'Espalda', 'TRX', 'Un brazo flexiona, el otro se extiende lateral.', 'GYM,CASA,MIXTO', 'PULL'],
  ['TRX press de pecho', 'Pecho', 'TRX', 'De espaldas al anclaje, baja el pecho a las manos.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['TRX fly', 'Pecho', 'TRX', 'Brazos abiertos formando cruz.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['TRX atomic push-up', 'Core', 'TRX', 'Flexión + rodillas al pecho con pies en TRX.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['TRX pike', 'Core', 'TRX', 'Pies en TRX, cadera arriba a posición V invertida.', 'GYM,CASA,MIXTO', 'CORE'],
  ['TRX knee tuck', 'Core', 'TRX', 'Pies en TRX, rodillas al pecho.', 'GYM,CASA,MIXTO', 'CORE'],
  ['TRX hamstring curl', 'Isquios', 'TRX', 'Talones en TRX, eleva cadera y dobla rodillas.', 'GYM,CASA,MIXTO', 'PULL'],
  ['TRX pistol squat', 'Cuádriceps', 'TRX', 'Una pierna, sujetándote del TRX. Asistencia ideal.', 'GYM,CASA,MIXTO', 'SQUAT'],
  ['TRX bicep curl', 'Bíceps', 'TRX', 'De cara al anclaje, codos altos, lleva manos a la frente.', 'GYM,CASA,MIXTO', 'PULL'],
  ['TRX tríceps extension', 'Tríceps', 'TRX', 'De cara al anclaje, codos arriba, extiende.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['TRX Y-fly', 'Hombros', 'TRX', 'Brazos rectos hacia arriba en Y. Trapecio bajo.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['TRX T-fly', 'Hombros', 'TRX', 'Brazos rectos en T. Trapecio medio.', 'GYM,CASA,MIXTO', 'PULL'],
  ['TRX rotación lateral (anti-rotation)', 'Core', 'TRX', 'Sujeta TRX con dos manos, gira tronco contra resistencia.', 'GYM,CASA,MIXTO', 'CORE'],
];

const KETTLEBELL = [
  ['KB swing ruso', 'Glúteos', 'KB', 'Termina a altura pecho. Hip hinge explosivo.', 'CROSSFIT,GYM,CASA,MIXTO', 'PULL'],
  ['KB swing americano', 'Glúteos', 'KB', 'Termina overhead. Versión CrossFit.', 'CROSSFIT,GYM,MIXTO', 'PULL'],
  ['KB clean a hombro', 'Full body', 'KB', 'Limpia al rack desde colgado. Técnica codos pegados.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['KB jerk', 'Hombros', 'KB', 'Push jerk con KB. Una o dos KBs.', 'CROSSFIT,GYM,MIXTO', 'PUSH'],
  ['KB snatch', 'Full body', 'KB', 'De suelo a overhead en un solo movimiento. Avanzado.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
  ['Turkish get-up', 'Full body', 'KB', 'Desde tumbado a de pie sin soltar KB en mano. Movilidad + fuerza.', 'CROSSFIT,GYM,CASA,MIXTO', 'OLY'],
  ['KB windmill', 'Oblicuos', 'KB', 'KB overhead, baja torso lateral hacia pie. Movilidad + core.', 'CROSSFIT,GYM,MIXTO', 'CORE'],
  ['KB goblet squat', 'Cuádriceps', 'KB', 'KB sujeta al pecho con dos manos.', 'CROSSFIT,GYM,CASA,MIXTO', 'SQUAT'],
  ['KB front squat (rack)', 'Cuádriceps', 'KB', 'Una o dos KBs en rack a hombros.', 'CROSSFIT,GYM,MIXTO', 'SQUAT'],
  ['KB row a un brazo', 'Espalda', 'KB', 'Apoyo en banco, rema con la KB.', 'GYM,CASA,MIXTO', 'PULL'],
  ['KB high pull', 'Espalda', 'KB', 'KB sube a altura de barbilla. Trapecio.', 'CROSSFIT,GYM,MIXTO', 'PULL'],
  ['KB Russian twist', 'Oblicuos', 'KB', 'Sentado, gira tronco con KB de lado a lado.', 'CROSSFIT,GYM,CASA,MIXTO', 'CORE'],
  ['KB halo', 'Hombros', 'KB', 'Gira KB alrededor de la cabeza. Movilidad hombro.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['KB walking lunge', 'Glúteos', 'KB', 'Zancadas con KB sujeta al pecho o en racks.', 'CROSSFIT,GYM,CASA,MIXTO', 'SQUAT'],
  ['Double KB clean and jerk', 'Full body', 'KB×2', 'Dos KBs simultáneas. Avanzado.', 'CROSSFIT,GYM,MIXTO', 'OLY'],
];

const MANCUERNAS = [
  ['DB press inclinado', 'Pecho', 'Mancuernas+banco', 'Banco 30-45°. Pectoral clavicular.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['DB press declinado', 'Pecho', 'Mancuernas+banco', 'Banco -15° a -30°. Porción esternal baja.', 'GYM,MIXTO', 'PUSH'],
  ['DB press neutro (Floor press)', 'Pecho', 'Mancuernas', 'Tumbado en suelo. Codos paran al tocar suelo.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['DB fly', 'Pecho', 'Mancuernas+banco', 'Brazos en arco. Aislamiento pectoral.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['DB pullover', 'Pecho/Dorsal', 'Mancuerna+banco', 'Brazos rectos sobre la cabeza y vuelve. Caja torácica.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['DB shoulder press', 'Hombros', 'Mancuernas', 'Sentado. Rango natural de hombro.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['DB Arnold press', 'Hombros', 'Mancuernas', 'Empieza palmas a ti, gira al subir. Cubre todo el deltoides.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['DB lateral raise', 'Hombros', 'Mancuernas', 'Codos ligeramente flexionados. Deltoides medio.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['DB front raise', 'Hombros', 'Mancuernas', 'Sube al frente hasta altura ojos. Deltoides anterior.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['DB rear delt fly', 'Hombros', 'Mancuernas', 'Inclinado, abre brazos hacia atrás. Deltoides posterior.', 'GYM,CASA,MIXTO', 'PULL'],
  ['DB row a un brazo', 'Espalda', 'Mancuerna+banco', 'Una mano y rodilla en banco. Codo pega al cuerpo.', 'GYM,CASA,MIXTO', 'PULL'],
  ['DB renegade row', 'Espalda', 'Mancuernas', 'En plancha sobre mancuernas, rema alterno. Core enorme.', 'CROSSFIT,GYM,CASA,MIXTO', 'PULL'],
  ['DB hammer curl', 'Bíceps', 'Mancuernas', 'Palmas neutras. Trabaja braquial.', 'GYM,CASA,MIXTO', 'PULL'],
  ['DB Zottman curl', 'Bíceps', 'Mancuernas', 'Sube supinado, baja pronado. Cubre flexor + extensor.', 'GYM,CASA,MIXTO', 'PULL'],
  ['DB skull crusher', 'Tríceps', 'Mancuernas+banco', 'Tumbado, baja mancuernas a la frente.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['DB tríceps kickback', 'Tríceps', 'Mancuerna', 'Inclinado, codo alto, extiende hacia atrás.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['DB Romanian deadlift', 'Isquios', 'Mancuernas', 'Caderas atrás, mancuernas hasta espinilla.', 'GYM,CASA,MIXTO', 'PULL'],
  ['DB single-leg RDL', 'Isquios', 'Mancuernas', 'Una pierna apoyada, otra atrás como contrapeso. Equilibrio + isquios.', 'GYM,CASA,MIXTO', 'PULL'],
  ['DB step-up', 'Cuádriceps', 'Mancuernas+banco', 'Sube al banco con mancuernas.', 'GYM,CASA,MIXTO', 'SQUAT'],
  ['DB hip thrust', 'Glúteos', 'Mancuerna+banco', 'Espalda alta en banco, mancuerna en cadera.', 'GYM,CASA,MIXTO', 'PULL'],
];

const GOMAS = [
  ['Gomas pull-apart', 'Hombros', 'Goma elástica', 'Brazos rectos al frente, abre hasta T. Rotadores externos.', 'GYM,CASA,MIXTO', 'PULL'],
  ['Gomas face pull', 'Hombros', 'Goma elástica', 'Tira hacia la cara con codos altos. Salud hombro.', 'GYM,CASA,MIXTO', 'PULL'],
  ['Gomas hip thrust', 'Glúteos', 'Goma+banco', 'Goma sobre cadera, activación extra.', 'GYM,CASA,MIXTO', 'PULL'],
  ['Gomas monster walk', 'Glúteos', 'Goma circular', 'Pasos laterales en sentadilla. Activación medio glúteo.', 'GYM,CASA,MIXTO', 'SQUAT'],
  ['Gomas glute bridge abducted', 'Glúteos', 'Goma circular', 'Puente con goma en rodillas. Abducción + extensión.', 'GYM,CASA,MIXTO', 'PULL'],
  ['Gomas clamshell', 'Glúteos', 'Goma circular', 'Lateral, rodillas flexionadas. Abre rodilla superior.', 'GYM,CASA,MIXTO', 'PULL'],
  ['Gomas press hombro', 'Hombros', 'Goma elástica', 'Pisa goma, sube manos overhead.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['Gomas remo bajo', 'Espalda', 'Goma elástica', 'Sentado, pies estirados, tira a torso.', 'GYM,CASA,MIXTO', 'PULL'],
  ['Gomas curl bíceps', 'Bíceps', 'Goma elástica', 'Pisa goma, flexiona codos.', 'GYM,CASA,MIXTO', 'PULL'],
  ['Gomas pulldown', 'Espalda', 'Goma elástica', 'Goma anclada arriba, tira hacia el pecho. Sustituto polea.', 'GYM,CASA,MIXTO', 'PULL'],
  ['Gomas chops alta-baja', 'Core', 'Goma elástica', 'Goma anclada alta, tira diagonal hacia abajo. Anti-rotación.', 'GYM,CASA,MIXTO', 'CORE'],
  ['Gomas Pallof press', 'Core', 'Goma elástica', 'Goma lateral, empuja al frente sin girar. Anti-rotación clásico.', 'GYM,CASA,MIXTO', 'CORE'],
  ['Gomas asistidas para dominadas', 'Espalda', 'Goma+barra', 'Pasa goma por barra y pie. Asiste dominada.', 'GYM,CASA,MIXTO', 'PULL'],
  ['Gomas peso muerto', 'Isquios', 'Goma elástica', 'Pisa goma, hip hinge.', 'GYM,CASA,MIXTO', 'PULL'],
  ['Gomas patada glúteo (kickback)', 'Glúteos', 'Goma circular', 'A cuatro patas, extiende cadera contra resistencia.', 'GYM,CASA,MIXTO', 'PULL'],
];

const COMBA = [
  ['Comba single under', 'Cardio', 'Comba', 'Salto básico, un paso por salto. 60-100 spm.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'JUMP'],
  ['Comba double under', 'Cardio', 'Comba', 'Dos pasos por salto. Coordinación + potencia.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'JUMP'],
  ['Comba criss-cross', 'Cardio', 'Comba', 'Cruza brazos al saltar. Coordinación.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'JUMP'],
  ['Comba pie alterno (boxer skip)', 'Cardio', 'Comba', 'Alterna pies como en boxeo. Más ligero.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'JUMP'],
  ['Comba rodillas altas (high knees)', 'Cardio', 'Comba', 'Levanta rodillas a 90°.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'JUMP'],
  ['Comba talones al glúteo', 'Cardio', 'Comba', 'Skip atrás tocando glúteo.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'JUMP'],
  ['Comba salto a una pierna', 'Cardio', 'Comba', 'Alterna pierna. Trabajo unilateral.', 'CROSSFIT,CALISTENIA,GYM,CASA,MIXTO', 'JUMP'],
  ['Comba triple under', 'Cardio', 'Comba', 'Tres pasos por salto. Élite.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'JUMP'],
  ['Comba EMOM 10\' 50 double unders/min', 'Cardio', 'Comba', 'Cada minuto 50 DU. Test cardio.', 'CROSSFIT,CALISTENIA,GYM,MIXTO', 'JUMP'],
];

const SACO = [
  ['Saco directo 3 min × 3 rondas', 'Cardio', 'Saco boxeo', 'Jab + cross. Cardio + técnica básica.', 'CROSSFIT,CASA,GYM,MIXTO', 'PUSH'],
  ['Saco combinaciones 4 golpes 5 rondas 3 min', 'Cardio', 'Saco boxeo', 'Jab-cross-hook-uppercut. Ritmo K1.', 'CASA,GYM,MIXTO', 'PUSH'],
  ['Saco rodillazos thai', 'Cardio', 'Saco boxeo', 'Sujeta saco y rodilla. 30 cada lado × 3.', 'CASA,GYM,MIXTO', 'PUSH'],
  ['Saco patadas circulares (low + high)', 'Cardio', 'Saco boxeo', '20 cada lado × 3. Cadera + isquios.', 'CASA,GYM,MIXTO', 'PUSH'],
  ['Saco sprint 30" (golpeo a saco a máx velocidad)', 'Cardio', 'Saco boxeo', 'HIIT pegada. 30" ON / 30" OFF × 10.', 'CROSSFIT,CASA,GYM,MIXTO', 'PUSH'],
];

const ANILLAS_EXTRA = [
  ['Anillas pull-up estricto', 'Espalda', 'Anillas', 'Más demanda escapular que barra.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'PULL'],
  ['Anillas dip estricto', 'Tríceps', 'Anillas', 'Inestable, anti-balanceo.', 'CALISTENIA,CROSSFIT,GYM,MIXTO', 'PUSH'],
  ['Anillas row inverted', 'Espalda', 'Anillas', 'Pies abajo, tira al pecho. Regresión.', 'CALISTENIA,GYM,CASA,MIXTO', 'PULL'],
  ['Anillas push-up', 'Pecho', 'Anillas', 'Inestable, mucho core.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Anillas fly (iron cross progresión)', 'Pecho', 'Anillas', 'Brazos en cruz. Avanzado gimnástico.', 'CALISTENIA,GYM,MIXTO', 'PUSH'],
  ['Anillas bicep curl (false grip)', 'Bíceps', 'Anillas', 'Falsa muñeca. Pre muscle-up.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
  ['Anillas L-sit pull-up', 'Espalda', 'Anillas', 'Piernas en L durante la dominada.', 'CALISTENIA,GYM,MIXTO', 'PULL'],
];

const MAZA = [
  ['Mace 360', 'Hombros', 'Maza', 'Gira maza alrededor de la cabeza. Movilidad hombro.', 'GYM,MIXTO', 'PULL'],
  ['Mace 10-2', 'Hombros', 'Maza', 'Gira de las 10 a las 2 con cadera. Coordinación.', 'GYM,MIXTO', 'PULL'],
  ['Mace squat', 'Cuádriceps', 'Maza', 'Sentadilla con maza al hombro.', 'GYM,MIXTO', 'SQUAT'],
];

const BOLSA_BULGARA = [
  ['Bolsa búlgara swing', 'Full body', 'Bolsa búlgara', 'Swing entre piernas + lanzamiento al hombro.', 'GYM,MIXTO', 'OLY'],
  ['Bolsa búlgara halo', 'Hombros', 'Bolsa búlgara', 'Gira alrededor de la cabeza.', 'GYM,MIXTO', 'PUSH'],
  ['Bolsa búlgara clean to press', 'Full body', 'Bolsa búlgara', 'Carga + press overhead.', 'GYM,MIXTO', 'OLY'],
];

// ---------- 6. OPOSICIÓN ESPECÍFICOS AVANZADOS ----------
const OPOS = [
  // Carrera específica para baremo
  ['Test 1000 m sprint (baremo PN)', 'Cardio', 'Pista', 'Test típico Policía Nacional. Ritmo objetivo 3:00-3:40 según escala.', 'PISTA,MIXTO', 'RUN'],
  ['Test 2000 m (baremo GC)', 'Cardio', 'Pista', 'Test típico Guardia Civil. Ritmo 7:00-8:30 según baremo.', 'PISTA,MIXTO', 'RUN'],
  ['Test course-navette / léger', 'Cardio', 'Pista', 'Pitidos acelerando 20 m. Estima VO2max. Bombero ≥10.5.', 'PISTA,MIXTO', 'RUN'],
  ['Test 50 m sprint', 'Cardio', 'Pista', 'Velocidad pura. <7\" hombre, <8\" mujer.', 'PISTA,MIXTO', 'RUN'],
  ['Test 100 m sprint', 'Cardio', 'Pista', '<13\" hombre, <15\" mujer escala buena.', 'PISTA,MIXTO', 'RUN'],
  ['Test 60 m vallas (PN)', 'Cardio', 'Pista', 'Vallas 84 cm. Coordinación + velocidad.', 'PISTA,MIXTO', 'RUN'],

  // Salto y agilidad
  ['Salto horizontal pies juntos', 'Cuádriceps', 'Pista', 'Test salto largo desde parado. Bombero ≥2.20 m.', 'PISTA,GYM,MIXTO', 'SQUAT'],
  ['Salto vertical (Sargent)', 'Cuádriceps', 'Pared+tiza', 'Test salto vertical. Bombero ≥45 cm.', 'PISTA,GYM,MIXTO', 'SQUAT'],
  ['Test agilidad 4×10 m (PN)', 'Cardio', 'Pista', '4 idas y vueltas tocando línea. <11\".', 'PISTA,MIXTO', 'RUN'],

  // Fuerza específica
  ['Test press banca máx reps 40-60 kg', 'Pecho', 'Banco+barra', 'Reps máximas con peso fijo. Estándar policial.', 'GYM,MIXTO', 'PUSH'],
  ['Test dominadas máx (PN)', 'Espalda', 'Barra dominadas', 'Reps máx. Escala 1-10 según baremo.', 'GYM,CALISTENIA,MIXTO', 'PULL'],
  ['Test dominadas en 1 minuto', 'Espalda', 'Barra dominadas', 'Variante. Más cardiomedio.', 'GYM,CALISTENIA,MIXTO', 'PULL'],
  ['Test flexiones 1 minuto', 'Pecho', 'Suelo', 'Reps máx en 60". Estándar militar.', 'GYM,CALISTENIA,CASA,MIXTO', 'PUSH'],
  ['Test abdominales 1 minuto (sit-up)', 'Core', 'Suelo', 'Reps máx en 60".', 'GYM,CALISTENIA,CASA,MIXTO', 'CORE'],
  ['Test abdominales 30 segundos', 'Core', 'Suelo', 'Versión corta. Calidad técnica.', 'GYM,CALISTENIA,CASA,MIXTO', 'CORE'],
  ['Test isquios extensión (back ext) 1 min', 'Espalda baja', 'Banco', 'Reps máx en 60".', 'GYM,MIXTO', 'PULL'],

  // Natación
  ['Test 50 m libre (PN agua)', 'Cardio', 'Piscina', '<50\" hombre, <60\" mujer.', 'PISCINA,MIXTO', 'RUN'],
  ['Test 100 m libre (Bombero)', 'Cardio', 'Piscina', '<1:30 muy bueno.', 'PISCINA,MIXTO', 'RUN'],
  ['Test apnea estática 1:30', 'Cardio', 'Piscina', 'Mantener apnea 1 min 30 s estática.', 'PISCINA,MIXTO', 'RUN'],

  // Circuitos / pruebas específicas
  ['Curso de bombero (CBA)', 'Full body', 'Saco+escalera+barra', 'Arrastre + escalera + martillo + tubo + carrera. <2:30 muy bueno.', 'MIXTO', 'RUN'],
  ['Pista militar (BMR)', 'Full body', 'Mixto', '10-12 obstáculos militares. Saltos + paso de muros + reptación.', 'MIXTO', 'RUN'],
  ['Reloj bombero (combinada)', 'Full body', 'Saco', 'Burpees + arrastre saco + carrera 200 m × N rondas.', 'MIXTO', 'BURPEE'],
  ['BLA (Bombero Local Andalucía)', 'Full body', 'Mixto', '6 estaciones: trepa cuerda, salto vertical, tracción, etc.', 'MIXTO', 'RUN'],
  ['Test combinado SJM-bombero', 'Full body', 'Mixto', 'Salto largo + flexiones + carrera. Equivalente a baremo.', 'MIXTO', 'RUN'],

  // Trabajo específico de oposición
  ['Arrastre maniquí 50 kg × 50 m', 'Full body', 'Maniquí', 'Simulación rescate. Posterior + cardio.', 'MIXTO', 'RUN'],
  ['Subida escalera con saco 20 kg', 'Cardio', 'Saco+escalera', '10 plantas con saco. Específico bombero.', 'MIXTO', 'RUN'],
  ['Carga + transporte saco 30 kg × 100 m', 'Full body', 'Saco', 'Carrera con peso. Específico militar.', 'MIXTO', 'RUN'],
  ['Tracción cuerda con peso (cabrestante manual)', 'Espalda', 'Cuerda+peso', 'Específico bombero.', 'MIXTO', 'PULL'],
];

// ---------- 7. MOVILIDAD / WARM-UP estructurado ----------
const MOVILIDAD = [
  ['CARS hombro (controlled articular rotations)', 'Hombros', '—', 'Círculos máx rango activo. Salud articular.', 'GYM,CASA,CALISTENIA,MIXTO', 'PUSH'],
  ['CARS cadera', 'Glúteos', '—', 'Círculos cadera amplitud máxima.', 'GYM,CASA,CALISTENIA,MIXTO', 'PULL'],
  ['World\'s greatest stretch', 'Full body', '—', 'Zancada + rotación torácica + cuádriceps. Calentamiento global.', 'GYM,CASA,CALISTENIA,MIXTO', 'PULL'],
  ['90/90 cadera (sentado en L)', 'Cadera', '—', 'Cadera delantera 90°, trasera 90°. Movilidad rotacional.', 'GYM,CASA,CALISTENIA,MIXTO', 'PULL'],
  ['Cossack squat (movilidad)', 'Aductores', '—', 'Solo movilidad sin peso. Aductores + tobillo.', 'GYM,CASA,CALISTENIA,MIXTO', 'SQUAT'],
  ['Pose niño con brazos extendidos', 'Dorsal', '—', 'Estiramiento dorsal + columna.', 'GYM,CASA,CALISTENIA,MIXTO', 'PULL'],
  ['Cobra estiramiento', 'Espalda baja', '—', 'Bocabajo, eleva pecho. Extensión torácica.', 'GYM,CASA,CALISTENIA,MIXTO', 'PUSH'],
  ['Gato-vaca', 'Espalda baja', '—', 'Cuadrupedia, flexión-extensión torácica.', 'GYM,CASA,CALISTENIA,MIXTO', 'PULL'],
  ['Down dog → up dog flow', 'Dorsal', '—', 'Yoga flow. Calentamiento dinámico.', 'GYM,CASA,CALISTENIA,MIXTO', 'PUSH'],
  ['Banded shoulder distraction', 'Hombros', 'Goma elástica', 'Goma alta, cuerpo se aleja. Tracción articular.', 'GYM,CASA,CALISTENIA,MIXTO', 'PUSH'],
  ['Banded hip distraction', 'Cadera', 'Goma elástica', 'Goma en cadera, abre cápsula.', 'GYM,CASA,CALISTENIA,MIXTO', 'PULL'],
  ['Foam roller cuádriceps 60 s', 'Cuádriceps', 'Foam roller', 'Miofascial. Después de entreno.', 'GYM,CASA,CALISTENIA,MIXTO', 'SQUAT'],
  ['Foam roller espalda alta 60 s', 'Espalda', 'Foam roller', 'Extensión torácica pasiva.', 'GYM,CASA,CALISTENIA,MIXTO', 'PULL'],
  ['Estiramiento isquios PNF', 'Isquios', '—', 'Contrae 6", relaja, gana ROM. 3 rondas.', 'GYM,CASA,CALISTENIA,MIXTO', 'PULL'],
  ['Estiramiento psoas (zancada)', 'Cadera', '—', 'Zancada profunda, tronco vertical.', 'GYM,CASA,CALISTENIA,MIXTO', 'PULL'],
  ['Movilidad tobillo (wall ankle dorsiflexion)', 'Tobillo', 'Pared', 'Rodilla a pared sin levantar talón.', 'GYM,CASA,CALISTENIA,MIXTO', 'SQUAT'],
  ['Movilidad torácica con foam', 'Espalda', 'Foam roller', 'Foam transversal, manos en nuca, extiende.', 'GYM,CASA,CALISTENIA,MIXTO', 'PULL'],
  ['Wrist prep (preparación muñeca)', 'Muñeca', '—', '8 movimientos rotacionales. Pre planche/handstand.', 'GYM,CASA,CALISTENIA,MIXTO', 'PUSH'],
];

// =====================================================================
//  MODALIDAD y SCORE_TIPO inferidos
// =====================================================================

function inferirModalidadYScore(nombre, pilar, entornos) {
  const n = nombre.toLowerCase();
  if (n.startsWith('wod ')) return { modalidad: 'wod', score_tipo: 'tiempo' };
  if (n.startsWith('emom ')) return { modalidad: 'emom', score_tipo: 'rondas_completadas' };
  if (n.startsWith('amrap ')) return { modalidad: 'amrap', score_tipo: 'rondas_reps' };
  if (n.startsWith('tabata ')) return { modalidad: 'tabata', score_tipo: 'reps_min_ronda' };
  if (n.startsWith('for time ')) return { modalidad: 'for_time', score_tipo: 'tiempo' };
  if (n.startsWith('death by ')) return { modalidad: 'death_by', score_tipo: 'ultima_ronda' };
  if (n.startsWith('chipper ')) return { modalidad: 'chipper', score_tipo: 'tiempo' };
  if (n.startsWith('ladder ')) return { modalidad: 'ladder', score_tipo: 'tiempo' };
  if (n.startsWith('test ')) return { modalidad: 'test', score_tipo: pilar === 'RESISTENCIA' ? 'tiempo' : 'reps' };
  if (n.includes('isométrico') || n.includes('hold')) return { modalidad: 'calistenia', score_tipo: 'tiempo' };
  if (n.includes('snatch') || n.includes('clean') || n.includes('jerk') || n.includes('press') || n.includes('squat') || n.includes('deadlift')) {
    return { modalidad: 'crossfit_lift', score_tipo: 'peso' };
  }
  if ((entornos || '').includes('CALISTENIA')) return { modalidad: 'calistenia', score_tipo: 'reps' };
  if (pilar === 'MOVILIDAD') return { modalidad: 'movilidad', score_tipo: 'tiempo' };
  if (pilar === 'RESISTENCIA') return { modalidad: 'cardio', score_tipo: 'tiempo' };
  return { modalidad: 'convencional', score_tipo: 'reps' };
}

function categoriaDesdePilar(pilar) {
  switch (pilar) {
    case 'MOVILIDAD': return 'Movilidad';
    case 'CORE': return 'Core';
    case 'RESISTENCIA': return 'Cardio';
    case 'VELOCIDAD': return 'Velocidad';
    default: return 'Fuerza';
  }
}

function pilarDesdeNombre(nombre, grupoMuscular) {
  const n = nombre.toLowerCase();
  if (grupoMuscular === 'Cardio') return 'RESISTENCIA';
  if (grupoMuscular === 'Core' || grupoMuscular === 'Oblicuos') return 'CORE';
  if (n.includes('sprint') || n.includes('salto') || n.includes('jump') || n.includes('explosivo')) return 'VELOCIDAD';
  if (n.includes('movilidad') || n.includes('cars') || n.includes('stretch') || n.includes('foam') || n.includes('cobra') || n.includes('gato-vaca') || n.includes('down dog') || n.includes('distraction') || n.includes('prep')) return 'MOVILIDAD';
  return 'FUERZA';
}

// =====================================================================
//  COMPILA Y GUARDA
// =====================================================================

function tupleAEjercicio(t) {
  const [nombre, grupo, equip, instr, entornos, ilustracion] = t;
  const pilar = pilarDesdeNombre(nombre, grupo);
  const { modalidad, score_tipo } = inferirModalidadYScore(nombre, pilar, entornos);
  return {
    nombre,
    pilar,
    grupo_muscular: grupo,
    equipamiento: equip,
    instrucciones_tecnicas: instr,
    entornos,
    tipo_ilustracion: ilustracion,
    modalidad,
    score_tipo,
    categoria: categoriaDesdePilar(pilar)
  };
}

function main() {
  if (!fs.existsSync(RUTA_BANCO)) {
    console.error('No se encuentra', RUTA_BANCO);
    process.exit(1);
  }

  const banco = JSON.parse(fs.readFileSync(RUTA_BANCO, 'utf8'));
  const existentes = new Map(banco.ejercicios.map((e) => [String(e.nombre).toLowerCase().trim(), e]));

  const todasLasTuplas = [
    ...CALISTENIA, ...CROSSFIT_MOVS, ...WODS, ...FORMATOS,
    ...TRX, ...KETTLEBELL, ...MANCUERNAS, ...GOMAS, ...COMBA,
    ...SACO, ...ANILLAS_EXTRA, ...MAZA, ...BOLSA_BULGARA,
    ...OPOS, ...MOVILIDAD
  ];

  let nuevos = 0;
  let actualizados = 0;
  for (const t of todasLasTuplas) {
    const ej = tupleAEjercicio(t);
    const k = ej.nombre.toLowerCase().trim();
    if (existentes.has(k)) {
      // Enriquecer con modalidad/score si no los tenía
      const previo = existentes.get(k);
      previo.modalidad = previo.modalidad || ej.modalidad;
      previo.score_tipo = previo.score_tipo || ej.score_tipo;
      previo.instrucciones_tecnicas = previo.instrucciones_tecnicas || ej.instrucciones_tecnicas;
      previo.entornos = previo.entornos || ej.entornos;
      actualizados += 1;
    } else {
      banco.ejercicios.push(ej);
      existentes.set(k, ej);
      nuevos += 1;
    }
  }

  // Asegura que TODOS los ejercicios tengan modalidad y score_tipo
  let camposAnadidos = 0;
  for (const e of banco.ejercicios) {
    if (!e.modalidad || !e.score_tipo) {
      const { modalidad, score_tipo } = inferirModalidadYScore(e.nombre, e.pilar, e.entornos || '');
      e.modalidad = e.modalidad || modalidad;
      e.score_tipo = e.score_tipo || score_tipo;
      camposAnadidos += 1;
    }
  }

  fs.writeFileSync(RUTA_BANCO, JSON.stringify(banco, null, 2));
  console.log(`✓ Ampliación doctorado completada`);
  console.log(`  Nuevos:            ${nuevos}`);
  console.log(`  Actualizados:      ${actualizados}`);
  console.log(`  Campos añadidos a previos: ${camposAnadidos}`);
  console.log(`  Total banco:       ${banco.ejercicios.length}`);

  // Resumen por categoría
  const porModalidad = {};
  for (const e of banco.ejercicios) {
    porModalidad[e.modalidad] = (porModalidad[e.modalidad] || 0) + 1;
  }
  console.log('\nDistribución por modalidad:');
  for (const [k, v] of Object.entries(porModalidad).sort((a, b) => b[1] - a[1])) {
    console.log(`  ${k.padEnd(20)} ${v}`);
  }
}

main();
