/**
 * EjercicioExplicacionesCuradas — Explicaciones ESCRITAS A MANO, específicas de
 * cada ejercicio del catálogo base de oposición. Sustituyen al texto genérico
 * por patrón cuando existe una entrada aquí.
 *
 * Coste: 0 € (no usa IA de pago; escritas directamente). Se sirven desde
 * EjercicioExplicacionService.explicar, que busca aquí ANTES que las fichas por
 * patrón. Clave = nombre normalizado (minúsculas, sin acentos).
 *
 * Formato: { setup, ejecucion, cues:[], errores:[], porque }.
 */

const CURADAS = {
  // ===================== EMPUJE TREN SUPERIOR =====================
  'press banca': {
    setup: 'Túmbate en el banco con los ojos bajo la barra. Cinco puntos de apoyo: cabeza, hombros y glúteos en el banco y ambos pies firmes en el suelo. Retrae y deprime las escápulas creando un arco natural en la zona alta de la espalda. Agarre algo más ancho que los hombros.',
    ejecucion: 'Saca la barra y llévala sobre el pecho con los brazos extendidos. Baja controlando 2 s hasta rozar el esternón con los codos a ~45° del torso. Empuja hacia arriba y ligeramente atrás en línea recta, exhalando al subir.',
    cues: ['Aprieta la barra como si quisieras "doblarla"', 'Pecho alto y escápulas juntas todo el rato', 'Empuja el suelo con los pies (drive de piernas)'],
    errores: ['Rebotar la barra en el pecho', 'Levantar los glúteos del banco', 'Abrir los codos a 90° (estrés en el hombro)'],
    porque: 'Construye fuerza de empuje horizontal que se transfiere a las flexiones del baremo, a lanzamientos y a las técnicas de intervención y defensa personal.'
  },
  'press militar': {
    setup: 'De pie, pies a la anchura de la cadera, core apretado y glúteos activos para no arquear la lumbar. Barra o mancuernas a la altura de las clavículas, codos ligeramente por delante de la barra y muñecas firmes.',
    ejecucion: 'Empuja la carga en vertical, pasando la cabeza ligeramente hacia delante cuando la barra supera la frente. Bloquea arriba con la barra sobre la coronilla. Baja controlado en 2 s hasta las clavículas. Inhala antes de empujar, exhala arriba.',
    cues: ['Glúteos y abdomen duros: nada de arquear la espalda', 'Cabeza "atraviesa la ventana" al final', 'La barra sube en línea recta, no hacia delante'],
    errores: ['Arquear la lumbar para empujar', 'Empujar la barra hacia delante en vez de arriba', 'No bloquear arriba con los hombros activos'],
    porque: 'El empuje por encima de la cabeza es fuerza directa para superar muros y vallas en los circuitos de agilidad (Bomberos, Guardia Civil, Policía Local).'
  },
  'flexiones de brazos': {
    setup: 'Manos justo bajo los hombros o un poco más abiertas, dedos al frente. Cuerpo en línea recta de talones a cabeza, glúteos y abdomen apretados, mirada al suelo entre las manos.',
    ejecucion: 'Baja el pecho hacia el suelo doblando los codos a ~45° del torso hasta casi rozarlo. Empuja con fuerza extendiendo los brazos por completo sin perder la alineación del cuerpo. Inhala al bajar, exhala al subir.',
    cues: ['Cuerpo rígido como una tabla, sin hundir la cadera', 'Codos hacia atrás, no abiertos en cruz', 'Baja hasta que el pecho casi toque'],
    errores: ['Hundir la cadera o subir el culo', 'Bajar solo la cabeza y no el pecho', 'Rango corto (no bajar del todo)'],
    porque: 'Prueba oficial en muchas convocatorias (especialmente femeninas) y el mejor indicador de fuerza-resistencia del tren superior. Base para todo lo demás.'
  },
  'flexiones diamante': {
    setup: 'Junta las manos bajo el pecho formando un rombo (triángulo) con índices y pulgares. Cuerpo en línea recta, core firme, hombros alejados de las orejas.',
    ejecucion: 'Baja el pecho hacia las manos manteniendo los codos pegados al cuerpo. Roza suavemente y empuja hasta extender por completo. El recorrido es más corto pero más exigente para el tríceps.',
    cues: ['Codos pegados al torso, no se abren', 'Cuerpo firme, sin balanceo', 'Control en la bajada, sin dejarse caer'],
    errores: ['Abrir los codos (deja de trabajar el tríceps)', 'Hundir la cadera', 'Hacer medio rango por dificultad'],
    porque: 'Variante centrada en tríceps y porción interna del pecho. Fortalece el bloqueo de la flexión estándar, clave para sumar repeticiones en la prueba.'
  },
  'flexiones inclinadas': {
    setup: 'Apoya las manos en una superficie elevada estable (banco, escalón, barra baja) a la anchura de los hombros. Cuerpo en línea recta desde esa inclinación, core apretado.',
    ejecucion: 'Baja el pecho hacia la superficie doblando los codos y empuja hasta extender. Cuanto más alta la superficie, más fácil: es la progresión ideal para ganar fuerza hacia la flexión completa.',
    cues: ['Mantén la línea del cuerpo aunque estés inclinado', 'Baja el pecho, no la cabeza', 'Reduce la altura cuando ganes fuerza'],
    errores: ['Superficie inestable', 'Hundir la cadera', 'No bajar lo suficiente'],
    porque: 'Progresión perfecta para quien aún no completa flexiones estrictas. Construye el patrón y la fuerza con menos carga corporal.'
  },
  'fondos en paralelas': {
    setup: 'Súbete a unas barras paralelas con los brazos extendidos, hombros abajo y atrás (alejados de las orejas), core y glúteos activos. Piernas juntas o ligeramente cruzadas.',
    ejecucion: 'Baja doblando los codos hasta que formen unos 90° sin molestia en el hombro, con una ligera inclinación del torso al frente. Empuja hasta extender los brazos sin bloquear de golpe. Inhala al bajar, exhala al subir.',
    cues: ['Hombros lejos de las orejas todo el rato', 'Ligera inclinación al frente para el pecho', 'No bajes más de lo que el hombro tolere'],
    errores: ['Bajar demasiado y forzar el hombro', 'Encoger los hombros hacia las orejas', 'Balancear las piernas para impulsarte'],
    porque: 'Fuerza combinada de pecho, tríceps y hombro. Excelente para el empuje vertical descendente y complemento directo de flexiones y dominadas.'
  },
  'extension triceps': {
    setup: 'De pie o sentado, con mancuerna, barra o polea. Codos pegados a la cabeza (si es sobre la cabeza) o al torso (si es en polea), muñecas firmes y core estable.',
    ejecucion: 'Extiende los codos hasta estirar el brazo por completo manteniendo los codos QUIETOS. Vuelve controlando sin dejar que el codo se abra. Todo el movimiento ocurre en el codo, no en el hombro.',
    cues: ['Codos fijos: solo se mueve el antebrazo', 'Extensión completa arriba/abajo', 'Sin balanceo del torso'],
    errores: ['Mover los codos (usar el hombro)', 'Balancear el cuerpo para ayudarse', 'Rango incompleto'],
    porque: 'Aísla el tríceps, el músculo que más empuja en flexiones, fondos y press. Reforzarlo mejora el bloqueo final de esos ejercicios.'
  },
  'elevaciones laterales': {
    setup: 'De pie, mancuernas a los lados, codos muy ligeramente flexionados, core firme y hombros abajo. Ligera inclinación del torso al frente opcional.',
    ejecucion: 'Eleva los brazos hacia los lados hasta la altura de los hombros, guiando con los codos (no con las muñecas). Pausa 1 s arriba y baja lento 2-3 s. Sin impulso del cuerpo.',
    cues: ['Guía con el codo, no con la mano', 'Sube solo hasta la altura del hombro', 'Baja lento, sin dejar caer el peso'],
    errores: ['Balancear el cuerpo para subir', 'Subir por encima del hombro', 'Encoger el trapecio hacia las orejas'],
    porque: 'Aísla el deltoides medio y da anchura y estabilidad al hombro, articulación clave en empujes y en el trabajo de circuito.'
  },

  // ===================== TIRÓN TREN SUPERIOR =====================
  'dominadas estrictas': {
    setup: 'Cuélgate con agarre prono (palmas al frente) algo más ancho que los hombros. Hombros lejos de las orejas, escápulas activas, abdomen y glúteos apretados para no balancear.',
    ejecucion: 'Inicia el tirón bajando las escápulas y lleva el pecho hacia la barra hasta que la barbilla la supere. Baja controlando 2 s hasta extender los brazos del todo. Sin kipping ni balanceo.',
    cues: ['Primero baja las escápulas, luego tira', 'Pecho a la barra, no solo barbilla', 'Baja controlando, no te dejes caer'],
    errores: ['Balancearte o dar patadas (kipping)', 'No extender abajo (medio rango)', 'Encoger los hombros al subir'],
    porque: 'Prueba oficial CRÍTICA en Policía Nacional, Guardia Civil y Mossos. Es la que más separa una nota mediocre de una sobresaliente.'
  },
  'dominadas asistidas': {
    setup: 'Coloca una banda elástica anclada a la barra y apoya rodillas o pies sobre ella. Cuélgate con agarre prono, escápulas activas y cuerpo firme.',
    ejecucion: 'Tira con la MISMA técnica que la dominada estricta: la banda solo ayuda en el punto más débil (abajo). Sube hasta que la barbilla supere la barra y baja controlado.',
    cues: ['Técnica igual que la estricta, la banda solo asiste', 'Control en la bajada', 'Reduce la banda según progresas'],
    errores: ['Rebotar con la banda', 'Usar tanta ayuda que no trabajes', 'Perder la técnica por hacer más reps'],
    porque: 'Progresión obligada hasta sumar 3-5 dominadas estrictas. Construye el patrón real mejor que las negativas o el jalón.'
  },
  'dominadas supinas': {
    setup: 'Cuélgate con agarre supino (palmas hacia ti) a la anchura de los hombros. Hombros abajo, pecho alto, core apretado.',
    ejecucion: 'Tira llevando el pecho a la barra con el codo pegado al cuerpo; el bíceps participa más que en la prona. Sube hasta superar la barbilla y baja controlado a extensión completa.',
    cues: ['Codos hacia abajo y pegados', 'Pecho alto hacia la barra', 'Extiende del todo abajo'],
    errores: ['Balancearte', 'No completar el rango abajo', 'Sacar la cabeza en vez del pecho'],
    porque: 'Variante que recluta más bíceps y suele ser más accesible. Buen puente hacia las dominadas pronas del baremo.'
  },
  'suspension en barra': {
    setup: 'Cuélgate de la barra con agarre prono y los brazos flexionados, con la barbilla por encima de la barra. Hombros activos (no colgado muerto), core y glúteos apretados.',
    ejecucion: 'Aguanta la posición isométrica el mayor tiempo posible manteniendo la barbilla sobre la barra, sin balanceo. Cuando ya no puedas mantener la altura, baja controlado.',
    cues: ['Barbilla siempre por encima de la barra', 'Aprieta todo el cuerpo, sin balanceo', 'Respira de forma constante'],
    errores: ['Dejar caer la barbilla poco a poco', 'Balancearte para "descansar"', 'Aguantar la respiración'],
    porque: 'Isométrico que construye la fuerza de agarre y de bloqueo alto de la dominada. Prueba oficial de suspensión en varias convocatorias femeninas.'
  },
  'remo con barra': {
    setup: 'Bisagra de cadera: flexiona ligeramente rodillas y lleva el tronco hacia delante (~30-45°) con la espalda neutra. Barra colgando con agarre prono algo más ancho que los hombros, dorsales activados.',
    ejecucion: 'Tira de la barra hacia el ombligo/bajo abdomen llevando los codos hacia atrás y apretando las escápulas. Baja controlado sin redondear la espalda. Exhala al tirar.',
    cues: ['Espalda neutra, pecho orgulloso', 'Tira con los codos, no con las manos', 'Aprieta las escápulas al final'],
    errores: ['Redondear la lumbar', 'Dar tirones con impulso de cadera', 'Subir el torso al tirar (hacer curl de tronco)'],
    porque: 'Fuerza de tirón horizontal que equilibra el press de banca, mejora la postura y refuerza la espalda para las dominadas.'
  },
  'remo con mancuerna': {
    setup: 'Apoya una mano y una rodilla en un banco con la espalda plana y paralela al suelo. Mancuerna en la otra mano colgando, hombro estable.',
    ejecucion: 'Tira de la mancuerna hacia la cadera llevando el codo hacia atrás y arriba, apretando la escápula. Baja controlado con estiramiento completo del dorsal. Un lado y luego el otro.',
    cues: ['Codo pegado al costado, tira hacia la cadera', 'Espalda plana como una mesa', 'No rotes el torso al tirar'],
    errores: ['Rotar el tronco para levantar más peso', 'Tirar hacia el hombro en vez de la cadera', 'Redondear la espalda'],
    porque: 'Trabaja cada lado por separado corrigiendo desequilibrios de la espalda. Refuerza dorsal y postura, base para dominadas y para aguantar estudiando.'
  },
  'remo invertido': {
    setup: 'Colócate bajo una barra baja (o unas anillas/TRX) y agárrala con los brazos extendidos. Cuerpo recto y rígido, talones apoyados, más horizontal = más difícil.',
    ejecucion: 'Tira del pecho hacia la barra apretando las escápulas, manteniendo el cuerpo completamente recto. Baja controlado hasta extender los brazos. Exhala al tirar.',
    cues: ['Cuerpo rígido como una tabla', 'Pecho a la barra, escápulas juntas', 'Ajusta la dificultad con la altura de los pies'],
    errores: ['Hundir la cadera', 'No llegar con el pecho a la barra', 'Dar tirones'],
    porque: 'El mejor tirón horizontal con peso corporal y la progresión perfecta hacia la dominada. Ideal para calistenia y entreno en casa con barra baja.'
  },
  'curl biceps': {
    setup: 'De pie con mancuernas o barra, brazos extendidos, codos pegados al costado y muñecas firmes. Core estable y hombros quietos.',
    ejecucion: 'Flexiona los codos subiendo la carga hacia los hombros SIN mover los codos ni balancear el cuerpo. Aprieta arriba y baja controlado 2 s hasta extender del todo.',
    cues: ['Codos quietos y pegados al cuerpo', 'Sube con control, sin impulso', 'Extiende del todo abajo'],
    errores: ['Balancear el cuerpo para subir', 'Mover los codos hacia delante', 'Medio rango'],
    porque: 'Refuerza el bíceps, que asiste en dominadas y remos. Un bíceps fuerte mejora el agarre y la capacidad de tirón en las pruebas.'
  },

  // ===================== TREN INFERIOR =====================
  'sentadillas': {
    setup: 'De pie, pies a la anchura de los hombros con las puntas ligeramente abiertas (15-30°). Pecho alto, core apretado y mirada al frente.',
    ejecucion: 'Inicia llevando la cadera hacia atrás y flexionando las rodillas a la vez. Baja hasta que los muslos queden al menos paralelos al suelo, con la espalda neutra. Empuja el suelo con los talones para subir. Inhala al bajar, exhala al subir.',
    cues: ['Rodillas en línea con las puntas de los pies', 'Peso en el medio-talón, no en la punta', 'Pecho alto, no te vengas hacia delante'],
    errores: ['Meter las rodillas hacia dentro', 'Levantar los talones', 'No bajar hasta paralelo'],
    porque: 'El rey de los ejercicios de pierna. Base directa del salto vertical (prueba oficial), del sprint y del course-test.'
  },
  'sentadilla goblet': {
    setup: 'Sujeta una mancuerna o kettlebell contra el pecho con ambas manos, codos hacia dentro. Pies a la anchura de los hombros, puntas ligeramente abiertas, pecho alto.',
    ejecucion: 'Baja en sentadilla manteniendo el peso pegado al pecho y el torso lo más vertical posible. Los codos pasan entre las rodillas al bajar. Empuja el suelo para subir.',
    cues: ['Peso pegado al pecho, codos dentro', 'Torso vertical, no te inclines', 'Baja profundo si tu movilidad lo permite'],
    errores: ['Separar el peso del cuerpo', 'Inclinar el torso al frente', 'Redondear la espalda abajo'],
    porque: 'Enseña una sentadilla técnica y vertical con carga segura. Ideal para casa con una sola mancuerna o kettlebell.'
  },
  'zancadas': {
    setup: 'De pie, tronco erguido y core firme. Da un paso largo al frente con una pierna manteniendo el equilibrio.',
    ejecucion: 'Baja flexionando ambas rodillas a ~90°: la rodilla trasera casi toca el suelo y la delantera no rebasa la punta del pie. Empuja con el talón delantero para volver. Alterna piernas.',
    cues: ['Torso vertical, no te inclines al frente', 'Rodilla trasera hacia el suelo', 'Empuja con el talón de la pierna de delante'],
    errores: ['Dar un paso demasiado corto', 'Que la rodilla delantera se meta hacia dentro', 'Inclinar el cuerpo hacia delante'],
    porque: 'Trabaja cada pierna por separado, corrige desequilibrios y entrena el patrón de zancada del sprint y las cuestas.'
  },
  'zancada caminando': {
    setup: 'De pie, tronco erguido, core firme y espacio libre por delante para desplazarte varios metros.',
    ejecucion: 'Da un paso largo y baja en zancada hasta que la rodilla trasera casi roce el suelo. Empuja con la pierna delantera y avanza llevando el pie de atrás al siguiente paso, encadenando zancadas.',
    cues: ['Pasos largos y controlados', 'Torso vertical, mirada al frente', 'Estabilidad: no bambolees la cadera'],
    errores: ['Perder el equilibrio entre pasos', 'Pasos cortos que no bajan a 90°', 'Inclinar el torso'],
    porque: 'Versión dinámica de la zancada: añade equilibrio y resistencia unilateral, muy transferible a la carrera y a los circuitos.'
  },
  'step ups': {
    setup: 'Frente a un cajón, banco o escalón firme a la altura de la rodilla aproximadamente. Tronco erguido, core activo.',
    ejecucion: 'Sube apoyando todo el pie sobre el cajón y empujando con esa pierna (sin impulsarte con la de abajo). Extiende arriba y baja controlado. Alterna o completa un lado y cambia.',
    cues: ['Empuja con la pierna de arriba, no con la de abajo', 'Apoya todo el pie en el cajón', 'Baja controlado, no te dejes caer'],
    errores: ['Impulsarte con la pierna del suelo', 'Apoyar solo la punta', 'Bajar de golpe'],
    porque: 'Fuerza unilateral de pierna muy funcional, idéntica al gesto de subir muros, vallas y obstáculos de los circuitos.'
  },
  'peso muerto': {
    setup: 'Pies bajo la cadera, barra sobre el medio del pie. Bisagra de cadera para agarrar la barra con brazos extendidos, hombros ligeramente por delante de la barra. Lumbar neutra, dorsales activados, pecho alto.',
    ejecucion: 'Empuja el suelo con los pies manteniendo la barra pegada al cuerpo. Extiende cadera y rodillas a la vez y bloquea arriba apretando los glúteos. Baja con control invirtiendo el movimiento. Toma aire y aprieta el core antes de cada repetición.',
    cues: ['Barra pegada al cuerpo todo el recorrido', 'Empuja el suelo, no "tires" con la espalda', 'Bloquea con glúteos, no arqueando la lumbar'],
    errores: ['Redondear la espalda baja', 'Separar la barra del cuerpo', 'Hiperextender la espalda arriba'],
    porque: 'El movimiento más transferible al mundo real: levantar peso del suelo. Construye la cadena posterior (glúteo, isquios, espalda), motor del sprint y el salto.'
  },
  'peso muerto rumano': {
    setup: 'De pie con la barra o mancuernas frente a los muslos, rodillas ligeramente flexionadas (y fijas), lumbar neutra y dorsales activos.',
    ejecucion: 'Lleva la cadera hacia atrás bajando la carga pegada a las piernas hasta notar un buen estiramiento en los isquios (barra a media espinilla), sin redondear la espalda. Vuelve empujando la cadera al frente y apretando glúteos.',
    cues: ['El movimiento es de CADERA, no de rodilla', 'Barra rozando las piernas al bajar', 'Estira los isquios, no fuerces la lumbar'],
    errores: ['Doblar las rodillas como una sentadilla', 'Redondear la espalda', 'Separar la barra de las piernas'],
    porque: 'Aísla y fortalece isquios y glúteo, claves para la potencia de sprint y para prevenir lesiones de la parte posterior del muslo.'
  },
  'hip thrust': {
    setup: 'Apoya la parte alta de la espalda en un banco, pies planos a la anchura de la cadera y una carga (barra/mancuerna) sobre la cadera. Mentón metido, mirada al frente.',
    ejecucion: 'Empuja con los talones subiendo la cadera hasta que el tronco quede paralelo al suelo, apretando fuerte los glúteos arriba. Baja controlado sin apoyar del todo. Exhala al subir.',
    cues: ['Aprieta los glúteos arriba 1 s', 'Empuja con los talones', 'Mentón metido, costillas abajo (no arquees)'],
    errores: ['Arquear la lumbar en vez de usar el glúteo', 'No llegar a la horizontal', 'Empujar con las puntas de los pies'],
    porque: 'El mejor ejercicio de aislamiento de glúteo. Potencia el motor del sprint y del salto y protege la zona lumbar.'
  },
  'kettlebell swing': {
    setup: 'Kettlebell a un palmo por delante de los pies, que están algo más anchos que los hombros. Bisagra de cadera para agarrarla con los brazos largos, lumbar neutra, dorsales activos.',
    ejecucion: 'Lanza la kettlebell hacia atrás entre las piernas ("hut") y a continuación extiende la cadera de forma EXPLOSIVA para que el peso salga proyectado hasta la altura del pecho. Los brazos solo guían; la fuerza es de la cadera. Deja que baje y encadena.',
    cues: ['La potencia viene de la cadera, no de los brazos', 'Bisagra, no sentadilla', 'Aprieta glúteos en el punto alto'],
    errores: ['Levantar el peso con los brazos/hombros', 'Doblar demasiado las rodillas (squat)', 'Redondear la espalda'],
    porque: 'Desarrolla potencia explosiva de cadera y resistencia cardiovascular a la vez. Muy transferible al salto, al sprint y a los circuitos.'
  },

  // ===================== CORE =====================
  'plancha abdominal': {
    setup: 'Apóyate sobre los antebrazos (codos bajo los hombros) y las puntas de los pies. Cuerpo en línea recta de talones a cabeza.',
    ejecucion: 'Mantén la posición apretando abdomen y glúteos, con la cadera ni caída ni levantada. Respira de forma constante durante todo el tiempo indicado. Cuando la técnica se rompa, para.',
    cues: ['Aprieta glúteos y abdomen a la vez', 'Cadera en línea: ni se hunde ni sube', 'Respira, no aguantes el aire'],
    errores: ['Hundir la cadera (lumbar en riesgo)', 'Subir el culo', 'Aguantar la respiración'],
    porque: 'Estabiliza el core para transferir fuerza entre tren superior e inferior y proteger la lumbar en todas las pruebas.'
  },
  'plancha lateral': {
    setup: 'Túmbate de lado apoyado sobre un antebrazo (codo bajo el hombro) y el canto del pie. Cuerpo alineado, cadera despegada del suelo.',
    ejecucion: 'Eleva la cadera hasta formar una línea recta de pies a cabeza y mantén, apretando el oblicuo del lado de abajo. Respira constante. Cambia de lado.',
    cues: ['Cadera arriba, cuerpo en línea', 'Hombro estable, alejado de la oreja', 'Aprieta el costado de abajo'],
    errores: ['Dejar caer la cadera', 'Rotar el torso hacia el suelo', 'Aguantar la respiración'],
    porque: 'Trabaja los oblicuos y la estabilidad lateral del core, clave para los cambios de dirección de los circuitos de agilidad.'
  },
  'hollow hold': {
    setup: 'Túmbate boca arriba, brazos extendidos por encima de la cabeza y piernas juntas y estiradas. Pega toda la zona lumbar al suelo.',
    ejecucion: 'Eleva ligeramente hombros y piernas del suelo manteniendo la lumbar SIEMPRE pegada, formando una "banana" tensa. Aguanta el tiempo indicado. Si es duro, dobla las rodillas o baja los brazos.',
    cues: ['Lumbar pegada al suelo SIEMPRE', 'Cuerpo tenso, forma de banana', 'Regresión: rodillas dobladas / brazos a los lados'],
    errores: ['Despegar la lumbar (se arquea)', 'Subir demasiado las piernas', 'Aguantar la respiración'],
    porque: 'Base de la fuerza de core en calistenia y gimnasia. Enseña la tensión abdominal necesaria para dominadas estrictas y control corporal.'
  },
  'abdominales': {
    setup: 'Túmbate boca arriba con las rodillas flexionadas y los pies apoyados. Manos en el pecho o a los lados de la cabeza sin tirar del cuello.',
    ejecucion: 'Enrolla la columna elevando los hombros del suelo llevando las costillas hacia la pelvis, sin tirar del cuello. Baja controlado. Exhala al subir.',
    cues: ['Enrolla la columna, no tires del cuello', 'Costillas hacia la cadera', 'Baja controlado, no te dejes caer'],
    errores: ['Tirar de la cabeza con las manos', 'Usar impulso de brazos/cuello', 'Subir el tronco recto en vez de enrollar'],
    porque: 'Refuerza el recto abdominal, presente en muchas pruebas de abdominales cronometradas del baremo.'
  },
  'mountain climbers': {
    setup: 'Posición de plancha alta con las manos bajo los hombros, cuerpo en línea recta y core apretado.',
    ejecucion: 'Lleva una rodilla hacia el pecho y cámbiala por la otra en un movimiento rítmico y continuo, como si corrieras en el sitio en plancha. Mantén la cadera baja y estable.',
    cues: ['Cadera baja y estable, no rebota', 'Ritmo controlado y constante', 'Core apretado todo el rato'],
    errores: ['Subir el culo con cada rodilla', 'Perder la línea del cuerpo', 'Ir tan rápido que se rompe la técnica'],
    porque: 'Combina core, hombro y cardio en un solo movimiento. Muy usado en circuitos y calentamientos por su transferencia y su demanda metabólica.'
  },
  'core 10 min': {
    setup: 'Espacio libre y una esterilla. Encadena varios ejercicios de core (plancha, hollow, mountain climbers, oblicuos) durante 10 minutos.',
    ejecucion: 'Realiza el circuito de core con buena técnica, descansos breves entre ejercicios y control en cada repetición. Prioriza la calidad del movimiento sobre la velocidad.',
    cues: ['Técnica limpia por encima de velocidad', 'Respira en cada ejercicio', 'Lumbar protegida en todo momento'],
    errores: ['Ir a máxima velocidad perdiendo forma', 'Aguantar la respiración', 'Saltarte la activación del core'],
    porque: 'Bloque completo de core que mejora la estabilidad general, base para transferir fuerza y aguantar la postura en carrera y circuitos.'
  },

  // ===================== CARRERA / RESISTENCIA =====================
  'carrera continua 30 min': {
    setup: 'Calienta 5-10 min con trote muy suave y movilidad. Ropa y zapatillas cómodas, hidratación previa.',
    ejecucion: 'Corre 30 minutos a ritmo constante y CONVERSACIONAL: deberías poder hablar sin ahogarte. No busques velocidad, busca mantener el ritmo estable de principio a fin.',
    cues: ['Ritmo conversacional: podrías hablar', 'Zancada relajada, hombros sueltos', 'Respiración rítmica y profunda'],
    errores: ['Salir demasiado rápido y morir al final', 'Correr a intensidad media todos los días', 'Saltarte el calentamiento'],
    porque: 'Construye la base aeróbica: mejora la recuperación entre esfuerzos y la capacidad de sostener ritmo en las pruebas de 1000m, 2000m y course-test.'
  },
  'carrera continua 45 min': {
    setup: 'Calienta 5-10 min con trote suave. Lleva hidratación si hace calor; es una tirada de media duración.',
    ejecucion: 'Rodaje de 45 minutos a ritmo suave-moderado y constante, siempre en zona aeróbica cómoda. El objetivo es el volumen y la resistencia, no la velocidad.',
    cues: ['Ritmo cómodo y sostenible 45 min', 'Controla la respiración, sin ahogarte', 'Reparte el esfuerzo, no aceleres al final por ego'],
    errores: ['Empezar demasiado fuerte', 'Convertir el rodaje en una serie', 'No hidratarte en tiradas largas'],
    porque: 'Rodaje largo que desarrolla la resistencia aeróbica de base, fundamental para las pruebas de 1000m y 2000m y para tolerar el volumen de entreno.'
  },
  'carrera continua 60 min': {
    setup: 'Calienta 10 min y asegúrate de estar bien hidratado y alimentado: es una tirada larga.',
    ejecucion: 'Corre 60 minutos a ritmo cómodo y constante, sin llegar nunca a la fatiga alta. Si notas que te ahogas, baja el ritmo: el objetivo es el tiempo en movimiento.',
    cues: ['Ritmo cómodo toda la hora', 'Hidrátate antes y, si puedes, durante', 'Escucha al cuerpo y ajusta'],
    errores: ['Salir a ritmo de carrera corta', 'No comer/hidratarte antes', 'Forzar si aparece molestia'],
    porque: 'Máximo desarrollo de la base aeróbica. Mejora la economía de carrera y la capacidad de recuperación, pilares de toda prueba física de resistencia.'
  },
  'trote suave 20 min': {
    setup: 'Movilidad breve y unos minutos andando rápido. Es una sesión de recuperación o de calentamiento aeróbico.',
    ejecucion: 'Trota 20 minutos muy suave, a ritmo regenerativo, respirando por la nariz si puedes. Debe dejarte fresco, no cansado.',
    cues: ['Muy suave: menos de lo que crees', 'Zancada corta y relajada', 'Que termines fresco'],
    errores: ['Trotar demasiado rápido', 'Usarlo como sesión de calidad', 'Tensar hombros y brazos'],
    porque: 'Rodaje regenerativo que activa la circulación y ayuda a recuperar entre sesiones duras sin añadir fatiga.'
  },
  'fartlek 20 min': {
    setup: 'Calienta 10 min con trote suave y 3-4 progresiones. Elige un recorrido llano o con desnivel suave.',
    ejecucion: 'Alterna bloques de ritmo rápido y suave (por ejemplo 2 min fuerte / 2 min suave) durante 20 minutos. Los tramos rápidos a esfuerzo alto pero controlado; los suaves para recuperar sin pararte.',
    cues: ['Tramos rápidos exigentes pero sostenibles', 'Recupera trotando, sin pararte', 'Juega con el ritmo según sensaciones'],
    errores: ['Ir a tope en el primer bloque y morir', 'Parar del todo en los suaves', 'No calentar antes de la calidad'],
    porque: 'Entrena el cambio de ritmo y la resistencia anaeróbica, clave para apretar en la recta final del 1000m/2000m y en los circuitos.'
  },
  'cambios de ritmo 30 min': {
    setup: 'Calienta 10 min con trote y movilidad. Recorrido conocido para poder cambiar de ritmo con seguridad.',
    ejecucion: 'Durante 30 minutos alterna 2 min a ritmo rápido y 2 min a ritmo suave de forma continua. Controla que los tramos rápidos sean sostenibles todos, no solo el primero.',
    cues: ['Mismo ritmo rápido en TODOS los bloques', 'Recupera trotando', 'Respiración rítmica en los cambios'],
    errores: ['Empezar demasiado fuerte', 'Andar en los tramos suaves', 'Perder la técnica al acelerar'],
    porque: 'Mejora la capacidad de cambiar de ritmo y de recuperar en movimiento, exactamente lo que exigen las pruebas de resistencia de oposición.'
  },
  'cuestas 10×100m': {
    setup: 'Busca una cuesta de pendiente moderada de ~100 m. Calienta 12-15 min con trote y progresiones.',
    ejecucion: 'Sube los 100 m a ritmo fuerte con buena técnica (rodilla alta, brazos activos) y recupera BAJANDO andando o al trote muy suave. Repite 10 veces manteniendo la calidad.',
    cues: ['Rodilla alta y brazos activos al subir', 'Recupera del todo bajando', 'Mantén la técnica en todas las repes'],
    errores: ['No recuperar entre subidas', 'Perder la técnica por la fatiga', 'Cuesta demasiado empinada'],
    porque: 'Desarrolla fuerza específica de carrera y potencia de zancada con bajo impacto. Mejora el sprint y la economía sin castigar tanto las articulaciones.'
  },
  'series 200m': {
    setup: 'Calienta 12-15 min: trote + movilidad + 3-4 progresiones. Idealmente en pista o terreno llano medido.',
    ejecucion: 'Corre repeticiones de 200 m a ritmo rápido (por debajo de tu ritmo de competición de 1000m) con la técnica cuidada. Recupera 1:00-1:30 caminando entre series. Haz entre 6 y 10 repeticiones.',
    cues: ['Ritmo alto pero controlado, no sprint máximo', 'Técnica limpia toda la serie', 'Recupera bien para mantener el ritmo'],
    errores: ['Salir a sprint y frenar al final', 'Recortar la recuperación y morir', 'Descuidar la técnica al cansarte'],
    porque: 'Entrena la velocidad y la tolerancia al esfuerzo por encima del ritmo de prueba. Mejora directamente la marca de 1000m.'
  },
  'series 400m': {
    setup: 'Calienta 12-15 min con trote, movilidad y progresiones. Pista o tramo llano de 400 m medido.',
    ejecucion: 'Corre repeticiones de 400 m al ritmo objetivo de tu prueba de 1000m. Recupera 1:30-2:00 entre series. Haz entre 4 y 8 repeticiones manteniendo el ritmo constante en todas.',
    cues: ['Ritmo objetivo constante en cada serie', 'Reparte: no salgas a tope', 'Recuperación completa entre repes'],
    errores: ['Ir demasiado rápido las primeras y caer', 'Recuperación insuficiente', 'Ritmo irregular'],
    porque: 'La serie estrella para el 1000m: entrena a ritmo de prueba y enseña a sostenerlo, mejorando la marca de forma medible.'
  },
  'series 800m': {
    setup: 'Calienta 15 min con trote, movilidad y progresiones. Terreno llano medido o pista.',
    ejecucion: 'Corre repeticiones de 800 m a ritmo algo más suave que el de 400 (cercano al de 2000m), con recuperación de 2-3 min. Haz 3-5 repeticiones sosteniendo el ritmo.',
    cues: ['Ritmo sostenible en las 800', 'Controla la primera mitad de cada serie', 'Respiración rítmica'],
    errores: ['Salir a ritmo de 400', 'Recuperación demasiado corta', 'Ritmo desigual entre series'],
    porque: 'Trabaja la tolerancia al lactato y la resistencia específica del 2000m y del course-test. Puente entre velocidad y resistencia.'
  },
  'series 1000m': {
    setup: 'Calienta 15 min con trote, movilidad y 3-4 progresiones. Recorrido llano medido o pista.',
    ejecucion: 'Corre repeticiones de 1000 m a ritmo objetivo de prueba (o ligeramente más suave), con recuperación completa entre ellas. Haz 2-4 repeticiones manteniendo el ritmo estable.',
    cues: ['Ritmo de prueba, constante', 'Recuperación completa entre repes', 'Reparte el esfuerzo en cada 1000'],
    errores: ['Empezar por encima del ritmo objetivo', 'Recuperar poco y desfondarte', 'Ritmo irregular'],
    porque: 'Simula la exigencia de la prueba de 1000m a ritmo real, entrenando cuerpo y cabeza para sostener ese ritmo el día del examen.'
  },
  'sprints 10×60m': {
    setup: 'Calienta MUY bien 15 min: trote, movilidad dinámica y 4-5 progresiones subiendo de intensidad. El sprint en frío lesiona.',
    ejecucion: 'Corre 10 sprints de 60 m a intensidad máxima o casi, con técnica de carrera (rodilla alta, brazos activos, cuerpo ligeramente inclinado en la salida). Recupera COMPLETO (1-2 min andando) entre sprints.',
    cues: ['Calentamiento exhaustivo antes de esprintar', 'Recuperación completa entre repes', 'Técnica: brazos activos, rodilla alta'],
    errores: ['Esprintar en frío', 'Recortar la recuperación (deja de ser velocidad)', 'Contraer la cara y los hombros'],
    porque: 'Prueba oficial de velocidad en Policía Local, Guardia Civil, Mossos y Bomberos. Entrenar el sprint mejora la nota directamente.'
  },
  'tecnica de carrera 15 min': {
    setup: 'Zona llana de ~20-30 m. Calienta con trote suave. Necesitas espacio para hacer ejercicios de técnica (drills).',
    ejecucion: 'Realiza drills de técnica de carrera durante 15 min: skipping (rodilla alta), talones al glúteo, impulsiones y zancadas progresivas. Foco en la postura, el apoyo y el braceo, no en la velocidad.',
    cues: ['Postura alta, mirada al frente', 'Apoyo activo bajo el cuerpo', 'Braceo coordinado, hombros sueltos'],
    errores: ['Hacer los drills a lo loco sin técnica', 'Mirar al suelo', 'Zancada sobreextendida (frenas)'],
    porque: 'Mejora la economía de carrera: corres más rápido con el mismo esfuerzo. Pequeñas mejoras técnicas se traducen en mejor marca en las pruebas.'
  },

  // ===================== ACONDICIONAMIENTO / SALTOS =====================
  'burpees': {
    setup: 'De pie, espacio libre alrededor y core activo. Un movimiento de cuerpo completo, así que calienta antes.',
    ejecucion: 'Baja a cuclillas apoyando las manos, lanza los pies atrás a plancha, haz una flexión, recoge los pies hacia las manos y salta con los brazos arriba. Encadena con ritmo controlado.',
    cues: ['Cuerpo firme en la plancha (no hundir cadera)', 'Salto con extensión completa arriba', 'Ritmo sostenible, no a lo loco'],
    errores: ['Hundir la cadera en la plancha', 'Saltar los pies de forma descontrolada', 'Ir tan rápido que se rompe la técnica'],
    porque: 'Ejercicio de cuerpo completo que combina fuerza y cardio. Muy usado en circuitos y tests de resistencia por su alta demanda metabólica.'
  },
  'burpees por tiempo 10 min': {
    setup: 'Espacio libre y cronómetro. Calienta 8-10 min: no empieces en frío un bloque metabólico duro.',
    ejecucion: 'Haz el máximo de burpees con buena técnica en 10 minutos, gestionando el ritmo para no fundirte en el primer minuto. Mejor un ritmo constante que picos y paradas.',
    cues: ['Ritmo constante y sostenible los 10 min', 'Técnica limpia aunque canses', 'Respira con el movimiento'],
    errores: ['Salir a máxima velocidad y morir', 'Perder la forma con la fatiga', 'Parar del todo (mejor bajar el ritmo)'],
    porque: 'Test de resistencia muscular y mental. Enseña a gestionar el esfuerzo bajo fatiga, algo que aparece en circuitos y pruebas combinadas.'
  },
  'jumping jacks': {
    setup: 'De pie, brazos a los lados, espacio libre. Ideal como calentamiento dinámico.',
    ejecucion: 'Salta abriendo piernas y subiendo los brazos por encima de la cabeza a la vez, y vuelve a la posición inicial con otro salto. Ritmo continuo y ligero.',
    cues: ['Ritmo ligero y continuo', 'Aterriza suave, rodillas blandas', 'Coordina brazos y piernas'],
    errores: ['Aterrizar rígido y ruidoso', 'Descoordinar brazos y piernas', 'Encoger los hombros'],
    porque: 'Calentamiento dinámico completo que eleva la frecuencia cardíaca y prepara el cuerpo para el esfuerzo sin apenas material.'
  },
  'saltos a comba 10 min': {
    setup: 'Comba ajustada a tu altura (los mangos llegan a las axilas al pisar el centro). Superficie firme y espacio libre.',
    ejecucion: 'Salta a la comba durante 10 minutos con saltos pequeños y ligeros, girando la cuerda con las muñecas (no con los brazos). Cae sobre la parte delantera del pie con rodillas blandas.',
    cues: ['Gira con las muñecas, no con los brazos', 'Saltos bajos y ligeros', 'Cae en la punta del pie, rodillas blandas'],
    errores: ['Saltar demasiado alto y cansarte', 'Girar con todo el brazo', 'Aterrizar con los talones'],
    porque: 'Cardio de bajo coste que mejora la coordinación, el pie reactivo y la resistencia. Perfecto para casa y como calentamiento explosivo.'
  },
  'circuito hiit 12 min': {
    setup: 'Espacio libre y cronómetro. Calienta 8-10 min. Elige 3-4 ejercicios (burpees, sentadillas, mountain climbers, etc.).',
    ejecucion: 'Alterna intervalos de trabajo intenso y descanso breve (por ejemplo 40 s trabajo / 20 s descanso) durante 12 minutos. En los tramos de trabajo, alta intensidad con buena técnica; en el descanso, respira y recupera.',
    cues: ['Máxima intensidad en el trabajo, técnica intacta', 'Aprovecha el descanso para respirar', 'Reparte para llegar al final'],
    errores: ['Ir a tope el primer bloque y fundirte', 'Perder técnica por velocidad', 'Descansos más largos de la cuenta'],
    porque: 'Mejora la capacidad anaeróbica y la resistencia en poco tiempo. Simula el esfuerzo intermitente de los circuitos de agilidad.'
  },
  'farmer walk': {
    setup: 'Coge una carga pesada en cada mano (mancuernas, kettlebells). De pie, pecho alto, hombros hacia atrás y core apretado.',
    ejecucion: 'Camina en línea recta con pasos firmes y controlados, manteniendo el tronco erguido y sin bambolear la cadera. Recorre la distancia indicada, deja las cargas y descansa.',
    cues: ['Pecho alto y hombros atrás', 'Pasos cortos y firmes', 'Core apretado, sin balancearte'],
    errores: ['Encorvar la espalda por el peso', 'Bambolear el tronco al andar', 'Cargar más de lo que puedes controlar'],
    porque: 'Fortalece agarre, core y estabilidad de todo el cuerpo bajo carga. Muy transferible a acarrear material y a las pruebas de fuerza funcional.'
  },

  // ===================== AGILIDAD =====================
  'escalera de agilidad': {
    setup: 'Coloca la escalera de agilidad (o marcas equivalentes) en el suelo. Calienta tobillos y activa el pie. Postura atlética: rodillas semiflexionadas, peso en el metatarso.',
    ejecucion: 'Realiza los patrones de pisada indicados (un pie por hueco, dos pies, laterales…) lo más rápido posible manteniendo la precisión. Brazos activos acompañando el ritmo. Prioriza pisar bien sobre ir a lo loco.',
    cues: ['Pie reactivo, apoyos rápidos en el metatarso', 'Brazos activos coordinando', 'Precisión primero, velocidad después'],
    errores: ['Mirar al suelo todo el rato', 'Pisar las líneas por ir rápido', 'Apoyar con el talón'],
    porque: 'Mejora la frecuencia de pies, la coordinación y la velocidad de reacción, base de los circuitos de agilidad de Bomberos, Policía Local y Guardia Civil.'
  },
  'agilidad escalera avanzada': {
    setup: 'Escalera de agilidad y espacio libre. Calienta bien tobillos y cadera. Patrones más complejos: cruzados, laterales, in-in-out-out.',
    ejecucion: 'Ejecuta secuencias avanzadas de pisada combinando frente, laterales y cruces, a alta velocidad pero sin fallar el patrón. Coordina brazos y piernas y mantén la postura atlética baja.',
    cues: ['Postura baja y atlética', 'Apoyos cortísimos, pie reactivo', 'Coordina el patrón antes de acelerar'],
    errores: ['Perder el patrón por ir rápido', 'Erguirte demasiado', 'Apoyos largos y pesados'],
    porque: 'Nivel superior de agilidad: mejora la coordinación fina y los cambios de dirección complejos que marcan la diferencia en los circuitos cronometrados.'
  },
  'circuito de conos': {
    setup: 'Coloca 6-8 conos (o botellas) en zigzag separados ~2 m. Calienta y adopta postura atlética: centro de gravedad bajo.',
    ejecucion: 'Recorre el circuito lo más rápido posible cambiando de dirección en cada cono, bajando el centro de gravedad y empujando el suelo para reacelerar. Frena con el pie de fuera y sal con el de dentro.',
    cues: ['Baja el cuerpo al cambiar de dirección', 'Frena con el pie exterior, sal con el interior', 'Mirada al frente, no a los pies'],
    errores: ['Ir erguido (pierdes agarre al girar)', 'Frenar tarde y pasarte del cono', 'Dar pasos largos al cambiar'],
    porque: 'Entrena los cambios de dirección y la aceleración/frenada, gesto exacto de los course-test y circuitos de agilidad del baremo.'
  },

  // ===================== NATACIÓN =====================
  'natacion crol 50m': {
    setup: 'Calienta con 100-200 m suaves cambiando estilos. Gafas y gorro. La salida suele ser desde dentro del agua en las pruebas de oposición.',
    ejecucion: 'Nada 50 m a crol cronometrado con brazada larga y eficiente, respiración bilateral cada 3 brazadas y patada constante desde la cadera. Mantén el cuerpo alineado y alto en el agua.',
    cues: ['Brazada larga: alcanza y agarra el agua', 'Respiración lateral sin levantar la cabeza', 'Cuerpo alineado, cadera alta'],
    errores: ['Levantar la cabeza al respirar (hunde las piernas)', 'Brazada corta y acelerada', 'Patada desde la rodilla'],
    porque: 'Prueba oficial en Bomberos y algunas opciones de Guardia Civil. La técnica de crol es lo que más marca el crono en 50 m.'
  },
  'natacion series 50m': {
    setup: 'Calienta 200-300 m suaves. Gafas y gorro. Trabaja en piscina con espacio para hacer series con descanso.',
    ejecucion: 'Nada repeticiones de 50 m a ritmo rápido con técnica cuidada, descansando 30-45 s entre series. Haz entre 8 y 12 repeticiones manteniendo el crono estable.',
    cues: ['Técnica intacta aunque vayas rápido', 'Crono parecido en todas las series', 'Respira controlado en el descanso'],
    errores: ['Romper la técnica por la velocidad', 'Recuperación insuficiente', 'Salir a tope y caer en las últimas'],
    porque: 'Entrena la velocidad y la resistencia específica de la prueba de natación, mejorando el ritmo sostenido en el agua.'
  },
  'natacion tecnica 30 min': {
    setup: 'Gafas, gorro y, si tienes, material de técnica (tabla, pull buoy). Calentamiento suave de 200 m.',
    ejecucion: 'Durante 30 min trabaja la técnica: ejercicios de brazada, respiración, patada y alineación a ritmo suave. El objetivo es NADAR MEJOR, no cansarte. Corrige un aspecto por bloque.',
    cues: ['Nada lento y pon foco en la técnica', 'Un aspecto a mejorar por bloque', 'Respiración rítmica y relajada'],
    errores: ['Nadar rápido y descuidar la técnica', 'No corregir nada, solo dar largos', 'Tensar el cuello y los hombros'],
    porque: 'La técnica es lo que más mejora el crono en natación. 30 min de foco técnico rinden más que nadar duro con mala forma.'
  },
  'natacion continua 20 min': {
    setup: 'Calentamiento de 100-200 m. Gafas y gorro. Elige un ritmo que puedas sostener 20 minutos.',
    ejecucion: 'Nada 20 minutos de forma continua a ritmo suave-moderado, respirando rítmicamente y manteniendo la técnica. El objetivo es la capacidad aeróbica en el agua, no la velocidad.',
    cues: ['Ritmo sostenible los 20 min', 'Respiración rítmica y constante', 'Técnica estable pese al cansancio'],
    errores: ['Empezar demasiado fuerte', 'Perder la técnica al cansarte', 'Aguantar la respiración'],
    porque: 'Construye la base aeróbica específica de la natación y la capacidad respiratoria, que además mejora toda tu resistencia general.'
  },
  'natacion series 25m': {
    setup: 'Calienta 200 m suaves. Gafas y gorro. Ideal para trabajar velocidad pura en el agua.',
    ejecucion: 'Nada repeticiones de 25 m a máxima velocidad con técnica, descansando 30-45 s entre series. Haz entre 8 y 12 repeticiones. Salida potente y brazada agresiva pero limpia.',
    cues: ['Máxima velocidad con técnica limpia', 'Salida y primeras brazadas potentes', 'Recupera bien entre repes'],
    errores: ['Brazada descontrolada por ir a tope', 'Recuperación insuficiente', 'Levantar la cabeza al respirar'],
    porque: 'Desarrolla la velocidad pura en el agua, útil para arrancar fuerte y mantener ritmo en las pruebas cortas de natación.'
  },

  // ===================== MOVILIDAD =====================
  'movilidad cadera 10 min': {
    setup: 'Espacio en el suelo con una esterilla. Ropa cómoda. Ideal antes de entrenar pierna o correr, o como recuperación.',
    ejecucion: 'Realiza ejercicios de movilidad de cadera (rotaciones, estocada con rotación, 90/90, círculos) de forma lenta y controlada durante 10 min. Llega a un rango cómodo sin dolor y respira profundo.',
    cues: ['Movimiento lento y controlado', 'Sin dolor: si pincha, sales del rango', 'Respiración profunda en cada posición'],
    errores: ['Rebotar para ganar rango', 'Forzar hasta el dolor', 'Aguantar la respiración'],
    porque: 'Una cadera móvil mejora la sentadilla, la zancada y la carrera, y previene molestias lumbares y de rodilla. Base de un buen rendimiento.'
  },
  'estiramientos 15 min': {
    setup: 'Zona tranquila y esterilla. Mejor al terminar de entrenar, con los músculos calientes.',
    ejecucion: 'Estira los principales grupos musculares (isquios, cuádriceps, glúteo, gemelo, espalda, hombro) manteniendo cada posición 30-45 s sin rebotes, respirando lento y llegando a una tensión cómoda.',
    cues: ['Mantén cada estiramiento 30-45 s', 'Sin rebotes, tensión cómoda', 'Respira lento y suelta la tensión'],
    errores: ['Rebotar en el estiramiento', 'Buscar dolor', 'Aguantar la respiración'],
    porque: 'Mejora la flexibilidad y la recuperación, reduce la rigidez tras entrenar y ayuda a prevenir lesiones a largo plazo.'
  }
};

// Alias por variantes de nombre en el catálogo (mismo ejercicio, otro orden).
CURADAS['series natacion 25m'] = CURADAS['natacion series 25m'];
CURADAS['curl de biceps'] = CURADAS['curl biceps'];
CURADAS['extension de triceps'] = CURADAS['extension triceps'];
CURADAS['dominadas / suspension en barra'] = CURADAS['suspension en barra'];
CURADAS['natacion 50 metros'] = CURADAS['natacion crol 50m'];

module.exports = { CURADAS };
