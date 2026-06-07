#!/usr/bin/env node
/**
 * Amplía el banco de ejercicios añadiendo ejercicios profesionales nuevos
 * (sin tocar los existentes). Output: data/ejercicios-banco-500.json actualizado.
 *
 * Categorías que estaban infra-representadas y ahora se refuerzan:
 *   - CORE       (25 → +35  = 60)
 *   - HOMBROS    (15 → +20  = 35)
 *   - AGILIDAD   (12 → +18  = 30)
 *   - PLIOMETRÍA ( 7 → +18  = 25)
 *   - PIERNA, PECHO, ESPALDA, BRAZOS → +variantes específicas
 *   - Movilidad oposiciones específica → +rutinas de calentamiento
 *
 * Total añadidos: ~200 ejercicios coherentes deportivamente para cada disciplina.
 */
const fs = require('fs');
const path = require('path');

const file = path.resolve(__dirname, '../data/ejercicios-banco-500.json');
const data = JSON.parse(fs.readFileSync(file, 'utf8'));
const existentes = new Set(
  data.ejercicios.map((e) =>
    String(e.nombre || '').normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim()
  )
);

function add(nombre, pilar, grupo, equip, entornos, ilust, instr) {
  const key = nombre.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim();
  if (existentes.has(key)) return false;
  existentes.add(key);
  data.ejercicios.push({
    nombre,
    pilar,
    grupo_muscular: grupo,
    equipamiento: equip,
    entornos: entornos.join(','),
    tipo_ilustracion: ilust,
    instrucciones_tecnicas: instr || null
  });
  return true;
}

// ====================== CORE (35 nuevos) ======================
const CORE = [
  ['Plancha frontal con elevación de pierna', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Plancha frontal estable. Eleva una pierna 5cm sin perder alineación. Alterna 10 reps por pierna.'],
  ['Plancha lateral con rotación', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Plancha lateral, brazo libre hacia el techo. Rota el tronco pasando el brazo bajo el cuerpo.'],
  ['Plancha lateral con elevación de cadera', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Plancha lateral. Baja cadera al suelo sin tocar y sube con potencia. 3-4 series.'],
  ['Plancha con toque al hombro', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Plancha alta. Toca con cada mano el hombro contrario sin mover caderas. Foco antirrotación.'],
  ['Plancha con remo TRX', 'Core', 'TRX', ['GYM','CROSSFIT','MIXTO'], 'PLANK', 'Pies en TRX, brazos extendidos. Tira al pecho como un remo manteniendo el core rígido.'],
  ['Plancha de antebrazos con disco', 'Core', 'Disco', ['GYM','CROSSFIT','MIXTO'], 'PLANK', 'Plancha de antebrazos con disco de 5-10kg en la espalda baja. Mantén 30-60s.'],
  ['Plancha invertida (puente)', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Sentado, manos detrás, eleva caderas formando línea recta. Mantén 20-40s.'],
  ['Plancha araña', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Plancha frontal. Lleva rodilla a codo del mismo lado. Alterna lento. 12 reps por lado.'],
  ['Plancha con paso lateral', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Plancha alta. Manos y pies se mueven lateralmente en bloque. 6 pasos a cada lado.'],
  ['Plancha con flexión alterna', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Plancha alta. Baja a antebrazos y vuelve a plancha alta. 10 reps controladas.'],
  ['Dead bug', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Tumbado boca arriba, brazos y piernas a 90°. Extiende brazo y pierna contraria sin tocar suelo.'],
  ['Dead bug con peso', 'Core', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PLANK', 'Dead bug clásico con mancuerna en cada mano. Estabiliza la lumbar contra el suelo.'],
  ['Bird dog', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', '4 apoyos. Extiende brazo y pierna contrarios manteniéndolos paralelos al suelo. Sostén 2s.'],
  ['Bird dog con remo', 'Core', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PLANK', 'Bird dog con mancuerna ligera. Al extender brazo, rema al cuerpo. 8 reps por lado.'],
  ['Hollow hold', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Tumbado, brazos extendidos atrás, piernas extendidas elevadas. Lumbar pegada al suelo.'],
  ['Hollow rock', 'Core', '—', ['CASA','GYM','CALISTENIA','CROSSFIT','MIXTO'], 'PLANK', 'Hollow hold mecido como una hamaca. 30-45s. Activación CrossFit clásica.'],
  ['Arco superman', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Boca abajo. Eleva pecho y piernas a la vez. 3 series de 15s en isométrico.'],
  ['Superman dinámico', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Superman con movimiento de brazos/piernas como nadando. 30-45s.'],
  ['Crunch con cable bajo', 'Core', 'Polea', ['GYM','MIXTO'], 'PLANK', 'De rodillas frente a polea alta, cuerda detrás del cuello, flexiona tronco con potencia.'],
  ['Crunch en banco declinado', 'Core', 'Banco', ['GYM','MIXTO'], 'PLANK', 'Tumbado en banco declinado, manos en sienes, flexiona tronco contra la gravedad.'],
  ['Crunch holandés (Russian twist)', 'Core', 'Disco', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Sentado, talones en suelo. Disco en manos. Toca disco al suelo a cada lado del cuerpo.'],
  ['Toques de pie tumbado', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Tumbado, piernas a 90°. Toca puntas de los pies con manos. Foco abs altos.'],
  ['Bicicleta abdominal', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Tumbado boca arriba, codo a rodilla contraria alternando. 30-45s rítmico.'],
  ['Tijeras horizontales', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Tumbado, piernas extendidas a 30°, alterna cruzar piernas tipo tijera. 30s.'],
  ['Hip thrust con piernas elevadas', 'Core', '—', ['CASA','CALISTENIA','MIXTO'], 'PLANK', 'Tumbado, pies en banco, eleva cadera apretando glúteo en la cima.'],
  ['Pallof press', 'Core', 'Banda', ['GYM','CASA','MIXTO'], 'PLANK', 'De pie lateral a polea/banda. Extiende brazos al frente resistiendo rotación. 12 reps/lado.'],
  ['Pallof press de rodillas', 'Core', 'Banda', ['CASA','GYM','MIXTO'], 'PLANK', 'De rodillas, banda lateral. Extiende brazos al frente. Activa core profundo.'],
  ['Pallof press isométrico', 'Core', 'Banda', ['CASA','GYM','MIXTO'], 'PLANK', 'Pallof press en posición extendida, mantén 30-45s contra la rotación.'],
  ['Ab wheel rollout', 'Core', 'Rueda abdominal', ['CASA','GYM','MIXTO'], 'PLANK', 'Rodillas en suelo, rueda en manos. Extiende cuerpo controlando el descenso. 6-10 reps.'],
  ['Ab wheel desde pie', 'Core', 'Rueda abdominal', ['GYM','MIXTO'], 'PLANK', 'Versión avanzada: desde pie. Solo para quien controla rollout en suelo.'],
  ['Plancha de Copenhague', 'Core', '—', ['GYM','CASA','MIXTO'], 'PLANK', 'Lateral, pie superior apoyado en banco. Eleva cadera. Foco aductores y core lateral.'],
  ['Pike abdominal con TRX', 'Core', 'TRX', ['GYM','CROSSFIT','MIXTO'], 'PLANK', 'Pies en TRX, manos en suelo. Lleva cadera arriba en pike. 8-12 reps.'],
  ['Pike abdominal con fitball', 'Core', 'Fitball', ['GYM','CASA','MIXTO'], 'PLANK', 'Espinillas en fitball, manos en suelo. Lleva pelota a manos en pike. 10 reps.'],
  ['Sit-up con disco', 'Core', 'Disco', ['CASA','GYM','MIXTO'], 'PLANK', 'Sit-up clásico abrazando disco en el pecho. 12-15 reps lentas y controladas.'],
  ['Crunch con compresión', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Tumbado, piernas elevadas. Lleva rodillas al pecho y simultáneamente eleva tronco.']
];
CORE.forEach(([n,g,eq,ent,il,ins]) => add(n,'CORE',g,eq,ent,il,ins));

// ====================== HOMBROS (20 nuevos) ======================
const HOMBROS = [
  ['Press militar con barra', 'Hombros', 'Barra', ['GYM','MIXTO'], 'PUSH', 'De pie, barra a la altura clavículas. Presiona arriba sin arquear lumbar. 5x5 al 80%.'],
  ['Press militar con mancuernas', 'Hombros', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PUSH', 'Sentado o de pie. Codos abajo, presiona arriba. Mancuernas convergen ligeramente arriba.'],
  ['Press Arnold', 'Hombros', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PUSH', 'Empieza con palmas hacia ti. Rota hacia fuera mientras presionas. Trabaja tres porciones del deltoides.'],
  ['Press push press', 'Hombros', 'Barra', ['GYM','CROSSFIT','MIXTO'], 'PUSH', 'Con impulso de piernas (cuarto squat). Empuja barra arriba explosivo. Olímpico.'],
  ['Press de hombros con kettlebell', 'Hombros', 'Kettlebell', ['CROSSFIT','CASA','MIXTO'], 'PUSH', 'KB en rack position. Presiona arriba con muñeca neutra. Foco unilateral.'],
  ['Press de hombros tras nuca', 'Hombros', 'Barra', ['GYM','MIXTO'], 'PUSH', 'Sentado en banco con respaldo. Barra detrás del cuello. CUIDADO: solo con movilidad torácica buena.'],
  ['Elevación lateral con mancuerna', 'Hombros', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PUSH', 'De pie, brazos sueltos. Eleva mancuernas hasta horizontal. Codos ligeramente flexionados.'],
  ['Elevación lateral con cable', 'Hombros', 'Polea', ['GYM','MIXTO'], 'PUSH', 'De pie con cable cruzado. Eleva brazo a horizontal. Tensión constante en deltoides medio.'],
  ['Elevación frontal con disco', 'Hombros', 'Disco', ['GYM','CASA','MIXTO'], 'PUSH', 'De pie, disco con ambas manos. Eleva hasta horizontal. Hombros, no trapecio.'],
  ['Elevación frontal alterna', 'Hombros', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PUSH', 'Mancuerna en cada mano. Eleva una al frente y luego la otra. 10 reps por brazo.'],
  ['Pájaros (rear delt fly)', 'Hombros', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PULL', 'Inclinado adelante, mancuernas colgando. Ábrelas a los lados. Aprieta omóplatos.'],
  ['Pájaros con cable cruzado', 'Hombros', 'Polea', ['GYM','MIXTO'], 'PULL', 'De pie, cables cruzados frente al cuerpo. Abre brazos como mariposa. Foco deltoide posterior.'],
  ['Face pull con cuerda', 'Hombros', 'Polea', ['GYM','MIXTO'], 'PULL', 'Cable alto con cuerda. Tira a la cara separando manos. Foco postura y deltoide posterior.'],
  ['Face pull con banda', 'Hombros', 'Banda', ['CASA','MIXTO'], 'PULL', 'Banda anclada. Tira a la cara separando manos. Salud de hombro y postura.'],
  ['Pike push-up', 'Hombros', '—', ['CASA','CALISTENIA','MIXTO'], 'PUSH', 'Posición de V invertida. Flexiones llevando cabeza al suelo. Substituto de press militar.'],
  ['Pike push-up con pies elevados', 'Hombros', '—', ['CASA','CALISTENIA','MIXTO'], 'PUSH', 'Pies en silla/banco. Pike push-up. Más resistencia. Cerca del hand-stand push-up.'],
  ['Handstand push-up apoyado', 'Hombros', '—', ['CALISTENIA','CROSSFIT','MIXTO'], 'PUSH', 'Vertical contra pared. Baja cabeza al suelo y empuja. Trabajo avanzado de hombro.'],
  ['Encogimientos de hombros con barra', 'Hombros', 'Barra', ['GYM','MIXTO'], 'PUSH', 'De pie con barra. Encoge hombros arriba sin doblar codos. Trapecio superior.'],
  ['Encogimientos con mancuernas', 'Hombros', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PUSH', 'Mancuernas a los lados. Encoge hombros. 12-15 reps, control en bajada.'],
  ['Y-T-W en banco', 'Hombros', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PULL', 'Boca abajo en banco inclinado. Mancuernas ligeras. Forma Y, luego T, luego W. Rehabilitación.']
];
HOMBROS.forEach(([n,g,eq,ent,il,ins]) => add(n,'FUERZA',g,eq,ent,il,ins));

// ====================== AGILIDAD (18 nuevos) ======================
const AGILIDAD = [
  ['Circuito en T con conos', 'Agilidad', 'Conos', ['PISTA','CASA','MIXTO'], 'RUN', '4 conos en T. Sprint adelante, lateral derecha, vuelta al centro, lateral izda, atrás.'],
  ['Circuito 5-10-5 (pro-agility)', 'Agilidad', 'Conos', ['PISTA','MIXTO'], 'RUN', 'Cono central, 5m a la derecha, 10m a la izquierda, 5m al centro. Cronómetro.'],
  ['Carioca lateral', 'Agilidad', '—', ['PISTA','CASA','MIXTO'], 'RUN', 'Pasos cruzados laterales, cadera abre/cierra. 20m por lado, calentamiento + agilidad.'],
  ['Skipping rodillas altas en sitio', 'Agilidad', '—', ['CASA','PISTA','MIXTO'], 'RUN', 'En sitio, rodillas a la cintura, brazos coordinados. 30s 4 series. Tempo alto.'],
  ['Skipping talones al glúteo', 'Agilidad', '—', ['CASA','PISTA','MIXTO'], 'RUN', 'En sitio o avanzando 20m. Tocar talón con glúteo. Bisagra de cadera y técnica de zancada.'],
  ['Cambios de dirección 90°', 'Agilidad', 'Conos', ['PISTA','MIXTO'], 'RUN', 'Sprint 10m, frenada con squat, giro 90°, sprint 10m. 5 reps por lado.'],
  ['Cambios de dirección 180°', 'Agilidad', 'Conos', ['PISTA','MIXTO'], 'RUN', 'Sprint 10m, frenada total, 180°, sprint 10m. Trabaja deceleración + reaceleración.'],
  ['Salidas reactivas a señal', 'Agilidad', '—', ['PISTA','CASA','MIXTO'], 'RUN', 'En posición de salida. A señal (visual/auditiva) sprint 10-20m. Mide tiempo de reacción.'],
  ['Slalom con conos', 'Agilidad', 'Conos', ['PISTA','MIXTO'], 'RUN', 'Conos en zig-zag. Esquiva rodeando cada cono lo más rápido posible. 6 reps.'],
  ['Escalera de coordinación', 'Agilidad', 'Escalera', ['CASA','PISTA','MIXTO'], 'RUN', 'Diferentes patrones: in-in-out-out, lateral, dos dentro uno fuera. Calentamiento + agilidad.'],
  ['Escalera in-out lateral', 'Agilidad', 'Escalera', ['CASA','PISTA','MIXTO'], 'RUN', 'Avanza lateral por escalera con patrón dentro-dentro-fuera-fuera. 4 pasadas.'],
  ['Box drill (caja de conos)', 'Agilidad', 'Conos', ['PISTA','MIXTO'], 'RUN', '4 conos formando cuadrado de 5m. Sprint, lateral, atrás, lateral. 4 patrones, 5 series.'],
  ['Sprints con cambio de dirección al frente', 'Agilidad', 'Conos', ['PISTA','MIXTO'], 'RUN', '10m sprint frontal, frena y 10m sprint atrás. 5 series.'],
  ['Frenada y salida (deceleración)', 'Agilidad', '—', ['PISTA','MIXTO'], 'RUN', 'Sprint 15m al 90%, frena en 2 pasos en posición atlética, sale otros 10m.'],
  ['Cuadrado de Illinois', 'Agilidad', 'Conos', ['PISTA','MIXTO'], 'RUN', 'Circuito clásico de selección deportiva. 10x5m con conos en cruz. Test estándar.'],
  ['Sprint con resistencia (paracaídas)', 'Agilidad', 'Paracaídas', ['PISTA','MIXTO'], 'RUN', 'Sprint 30m con paracaídas. Mejora potencia de arranque. 6 series con 90s descanso.'],
  ['Trineo o sled push', 'Agilidad', 'Trineo', ['CROSSFIT','GYM','PISTA','MIXTO'], 'RUN', 'Empuje de trineo 10-15m con carga moderada. Combina fuerza y aceleración.'],
  ['Drill T con balón medicinal', 'Agilidad', 'Balón medicinal', ['PISTA','MIXTO'], 'RUN', 'T-drill clásico llevando balón medicinal. Añade fuerza al patrón de agilidad.']
];
AGILIDAD.forEach(([n,g,eq,ent,il,ins]) => add(n,'VELOCIDAD',g,eq,ent,il,ins));

// ====================== PLIOMETRÍA (18 nuevos) ======================
const PLIO = [
  ['Salto al cajón', 'Pliometría', 'Cajón', ['CROSSFIT','GYM','CALISTENIA','PISTA','MIXTO'], 'JUMP', 'Cajón 50-70cm. Salto con dos pies y aterrizaje suave. 4x5 reps con 60s descanso.'],
  ['Salto al cajón unilateral', 'Pliometría', 'Cajón', ['CROSSFIT','GYM','PISTA','MIXTO'], 'JUMP', 'Cajón 30-40cm. Salta con una pierna. Avanzado, mucha activación de cadena posterior.'],
  ['Drop jump', 'Pliometría', 'Cajón', ['CROSSFIT','GYM','PISTA','MIXTO'], 'JUMP', 'Bájate del cajón 30-50cm, en contacto suelo rebota arriba al instante. Foco reactividad.'],
  ['Salto largo con un pie', 'Pliometría', '—', ['CALISTENIA','PISTA','CASA','MIXTO'], 'JUMP', 'Salto horizontal con una pierna. Aterriza estable, mantén 2s. 4x5/pierna.'],
  ['Salto vertical máximo (CMJ)', 'Pliometría', '—', ['CASA','PISTA','CALISTENIA','MIXTO'], 'JUMP', 'Contramovement jump. Squat profundo y salto arriba. Mide altura con app o tiza en pared.'],
  ['Triple salto a pie firme', 'Pliometría', '—', ['PISTA','CALISTENIA','MIXTO'], 'JUMP', 'Sin carrera. Tres saltos horizontales encadenados. 4 series, anota distancia.'],
  ['Multisaltos a vallas bajas', 'Pliometría', 'Vallas', ['PISTA','CROSSFIT','MIXTO'], 'JUMP', '5 vallas 30-50cm separadas 1m. Salta seguidos con mínimo contacto. 4x5 vallas.'],
  ['Saltos laterales sobre línea', 'Pliometría', '—', ['CASA','PISTA','CALISTENIA','MIXTO'], 'JUMP', 'Salta de un lado a otro de una línea pies juntos. 30s tempo alto. 4 series.'],
  ['Saltos laterales sobre banco', 'Pliometría', 'Banco', ['CROSSFIT','GYM','PISTA','MIXTO'], 'JUMP', 'Banco bajo (15-25cm). Salta de un lado a otro pasando encima. 30-45s.'],
  ['Tuck jump (rodillas al pecho)', 'Pliometría', '—', ['CASA','CALISTENIA','CROSSFIT','MIXTO'], 'JUMP', 'Salto vertical llevando rodillas al pecho. 4x6 reps. Plyo + core.'],
  ['Split jump (alternar zancada)', 'Pliometría', '—', ['CASA','PISTA','CALISTENIA','MIXTO'], 'JUMP', 'En zancada profunda. Salta y alterna pierna en el aire. 4x10 alternos.'],
  ['Burpee con salto al cajón', 'Pliometría', 'Cajón', ['CROSSFIT','MIXTO'], 'JUMP', 'Burpee clásico + salto al cajón al levantarse. WOD clásico. Reps según WOD.'],
  ['Saltos a vallas altas con freno', 'Pliometría', 'Vallas', ['PISTA','MIXTO'], 'JUMP', 'Vallas 60-90cm. Salta, aterriza y mantén 1s. Foco aterrizaje estable.'],
  ['Saltos en escalera', 'Pliometría', 'Escalera', ['CASA','PISTA','MIXTO'], 'JUMP', 'Saltos seguidos en escalera de agilidad: dos dentro, fuera, lateral. 5 pasadas.'],
  ['Bound (zancada amplia explosiva)', 'Pliometría', '—', ['PISTA','MIXTO'], 'JUMP', 'Zancadas exageradas saltando hacia adelante. 30m, 4 series. Potencia de carrera.'],
  ['Salto pliométrico con flexión', 'Pliometría', '—', ['CASA','CALISTENIA','CROSSFIT','MIXTO'], 'PUSH', 'Flexión explosiva, manos despegan del suelo. Avanzado: incluye palmada.'],
  ['Lanzamiento de balón medicinal contra pared', 'Pliometría', 'Balón medicinal', ['GYM','CROSSFIT','CASA','MIXTO'], 'PUSH', 'De pie frente a pared. Lanzamiento explosivo desde el pecho. 4x8 reps.'],
  ['Slam de balón medicinal', 'Pliometría', 'Balón medicinal', ['GYM','CROSSFIT','CASA','MIXTO'], 'JUMP', 'Balón sobre cabeza. Lánzalo contra el suelo con máxima potencia. 4x10 reps.']
];
PLIO.forEach(([n,g,eq,ent,il,ins]) => add(n,'VELOCIDAD',g,eq,ent,il,ins));

// ====================== PIERNA / GLÚTEO especializadas (25 nuevos) ======================
const PIERNA = [
  ['Sentadilla búlgara con mancuernas', 'Pierna', 'Mancuerna', ['GYM','CASA','MIXTO'], 'SQUAT', 'Pie trasero en banco. Sentadilla unilateral con mancuernas. 4x10 por pierna.'],
  ['Sentadilla búlgara con barra', 'Pierna', 'Barra', ['GYM','MIXTO'], 'SQUAT', 'Pie trasero en banco, barra en espalda. Foco cuádriceps/glúteo. 4x6/pierna.'],
  ['Sentadilla goblet profunda', 'Pierna', 'Mancuerna', ['CASA','GYM','MIXTO'], 'SQUAT', 'Mancuerna o KB al pecho. Sentadilla profunda y controlada. 4x12.'],
  ['Sentadilla pistol asistida con TRX', 'Pierna', 'TRX', ['CROSSFIT','GYM','MIXTO'], 'SQUAT', 'TRX para asistirse. Sentadilla unipodal completa. Camino hacia la pistol libre.'],
  ['Sentadilla pistol libre', 'Pierna', '—', ['CALISTENIA','CROSSFIT','MIXTO'], 'SQUAT', 'Sentadilla unipodal sin apoyo. Pierna libre extendida adelante. Avanzado.'],
  ['Sentadilla cossack', 'Pierna', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'SQUAT', 'Apertura amplia. Baja sobre una pierna manteniendo la otra extendida. Movilidad + fuerza.'],
  ['Sentadilla sumo con disco', 'Pierna', 'Disco', ['CASA','GYM','MIXTO'], 'SQUAT', 'Pies muy abiertos, puntas afuera. Disco entre piernas. Foco aductores y glúteo.'],
  ['Sentadilla zercher', 'Pierna', 'Barra', ['CROSSFIT','GYM','MIXTO'], 'SQUAT', 'Barra en pliegue de codos. Sentadilla profunda. Mucho core y posición vertical.'],
  ['Sentadilla front squat', 'Pierna', 'Barra', ['CROSSFIT','GYM','MIXTO'], 'SQUAT', 'Barra en racks anteriores. Foco cuádriceps y zona dorsal alta. 5x5 al 75%.'],
  ['Sentadilla overhead', 'Pierna', 'Barra', ['CROSSFIT','GYM','MIXTO'], 'SQUAT', 'Barra sobre cabeza brazos extendidos. Squat profundo. Movilidad y técnica avanzada.'],
  ['Hip thrust con barra', 'Glúteo', 'Barra', ['GYM','MIXTO'], 'SQUAT', 'Espalda alta en banco, barra en cadera. Empuja arriba apretando glúteo. 4x8 al 75%.'],
  ['Hip thrust con banda', 'Glúteo', 'Banda', ['CASA','MIXTO'], 'SQUAT', 'Banda alrededor de las rodillas + hip thrust. Foco glúteo medio.'],
  ['Hip thrust unilateral', 'Glúteo', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'SQUAT', 'Espalda en suelo o banco bajo. Hip thrust con una pierna. 4x10 por pierna.'],
  ['Puente glúteo con disco', 'Glúteo', 'Disco', ['CASA','GYM','MIXTO'], 'SQUAT', 'Tumbado, disco en cadera. Empuja arriba. 4x15.'],
  ['Step-up con mancuernas', 'Pierna', 'Mancuerna', ['CASA','GYM','MIXTO'], 'SQUAT', 'Sube a banco/cajón con mancuernas. 4x10/pierna. Pierna posterior no apoya.'],
  ['Step-up con barra', 'Pierna', 'Barra', ['GYM','MIXTO'], 'SQUAT', 'Step-up con barra en espalda. Más carga. Cuidado equilibrio.'],
  ['Zancada caminando con mancuernas', 'Pierna', 'Mancuerna', ['CASA','GYM','PISTA','MIXTO'], 'SQUAT', 'Mancuernas a los lados, zancada caminando 15-20m. Foco glúteo + cuádriceps.'],
  ['Zancada en reversa', 'Pierna', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'SQUAT', 'Da un paso atrás. Rodilla baja al suelo. Menos estrés en rodilla que la zancada frontal.'],
  ['Zancada con disco rotacional', 'Pierna', 'Disco', ['CASA','GYM','MIXTO'], 'SQUAT', 'Zancada + rotación de tronco con disco al frente. Core + pierna.'],
  ['Peso muerto rumano con mancuernas', 'Glúteo', 'Mancuerna', ['CASA','GYM','MIXTO'], 'SQUAT', 'Bisagra cadera, mancuernas resbalan por piernas. Foco isquiotibiales/glúteo.'],
  ['Peso muerto rumano unilateral', 'Glúteo', 'Mancuerna', ['CASA','GYM','MIXTO'], 'SQUAT', 'Sobre una pierna, pierna libre extendida atrás. Equilibrio + isquios.'],
  ['Peso muerto sumo', 'Glúteo', 'Barra', ['GYM','CROSSFIT','MIXTO'], 'SQUAT', 'Pies abiertos, manos dentro. Foco glúteo + aductores. 5x3 al 80%.'],
  ['Peso muerto convencional', 'Pierna', 'Barra', ['GYM','CROSSFIT','MIXTO'], 'SQUAT', 'Pies a la anchura, manos fuera de rodillas. Cadena posterior completa. 5x5.'],
  ['Curl femoral con fitball', 'Glúteo', 'Fitball', ['CASA','GYM','MIXTO'], 'SQUAT', 'Tumbado, talones en fitball. Eleva cadera + flexiona rodillas. Isquios + glúteo.'],
  ['Nordic hamstring', 'Glúteo', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'SQUAT', 'De rodillas, pies sujetos. Baja cuerpo controlando. Ejercicio rey de isquios.']
];
PIERNA.forEach(([n,g,eq,ent,il,ins]) => add(n,'FUERZA',g,eq,ent,il,ins));

// ====================== PECHO / TIRÓN (15 nuevos) ======================
const PECHO_TIRON = [
  ['Press banca con pausa', 'Pecho', 'Barra', ['GYM','MIXTO'], 'PUSH', 'Press banca tradicional con 2s de pausa en el pecho. 4x5. Fuerza explosiva.'],
  ['Press banca con cadenas', 'Pecho', 'Barra', ['GYM','CROSSFIT','MIXTO'], 'PUSH', 'Cadenas en los extremos. Carga variable. Avanzado.'],
  ['Press inclinado con mancuernas', 'Pecho', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PUSH', 'Banco a 30-45°. Mancuernas más estables y permiten más recorrido que barra.'],
  ['Press declinado con barra', 'Pecho', 'Barra', ['GYM','MIXTO'], 'PUSH', 'Banco declinado. Foco pectoral inferior. 4x8.'],
  ['Aperturas con mancuernas', 'Pecho', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PUSH', 'Tumbado, brazos ligeramente flexionados. Abre lateralmente y cierra apretando pecho.'],
  ['Aperturas con poleas', 'Pecho', 'Polea', ['GYM','MIXTO'], 'PUSH', 'Cables a la altura del pecho. Junta manos al frente. Tensión constante.'],
  ['Cruces de polea bajos', 'Pecho', 'Polea', ['GYM','MIXTO'], 'PUSH', 'Cables desde abajo. Cruzar manos arriba. Foco pectoral superior.'],
  ['Fondos en paralelas para pecho', 'Pecho', 'Paralelas', ['CALISTENIA','GYM','MIXTO'], 'PUSH', 'Inclínate hacia adelante en fondos. Foco pectoral inferior y tríceps.'],
  ['Flexiones diamante', 'Pecho', '—', ['CASA','CALISTENIA','MIXTO'], 'PUSH', 'Manos juntas formando diamante bajo el pecho. Foco tríceps + pecho interior.'],
  ['Flexiones declinadas', 'Pecho', '—', ['CASA','CALISTENIA','MIXTO'], 'PUSH', 'Pies elevados en banco/silla. Mayor activación del pectoral superior.'],
  ['Flexiones explosivas con palmada', 'Pecho', '—', ['CASA','CALISTENIA','CROSSFIT','MIXTO'], 'PUSH', 'Flexión + empuje explosivo + palmada en el aire. Avanzado, mucha potencia.'],
  ['Remo Pendlay', 'Espalda', 'Barra', ['GYM','CROSSFIT','MIXTO'], 'PULL', 'Barra en el suelo cada rep. Tronco horizontal. Tira a la cadera baja. Potente.'],
  ['Remo Yates', 'Espalda', 'Barra', ['GYM','MIXTO'], 'PULL', 'Tronco 45°, palmas hacia ti. Tira a la cintura. Foco dorsal grande.'],
  ['Pull-over con mancuerna', 'Espalda', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PULL', 'Tumbado, mancuerna sobre el pecho. Lleva atrás y arriba. Foco serrato + dorsal.'],
  ['Remo unilateral con apoyo en banco', 'Espalda', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PULL', 'Rodilla y mano en banco. Rema mancuerna a la cadera. 4x10/lado.']
];
PECHO_TIRON.forEach(([n,g,eq,ent,il,ins]) => add(n,'FUERZA',g,eq,ent,il,ins));

// ====================== BRAZOS (12 nuevos) ======================
const BRAZOS = [
  ['Curl bíceps con barra Z', 'Brazos', 'Barra Z', ['GYM','MIXTO'], 'PULL', 'Barra Z mejora ergonomía de muñeca. Curl estricto, codos pegados al cuerpo.'],
  ['Curl bíceps con mancuernas alterno', 'Brazos', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PULL', 'Alterna brazos. Supinación al subir. 4x10/brazo.'],
  ['Curl martillo', 'Brazos', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PULL', 'Palmas mirando al cuerpo (neutras). Foco braquial + braquiorradial.'],
  ['Curl predicador', 'Brazos', 'Barra Z', ['GYM','MIXTO'], 'PULL', 'Banco predicador. Bíceps aislado, sin trampa lumbar. 4x10.'],
  ['Curl concentrado', 'Brazos', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PULL', 'Sentado, codo apoyado en muslo interior. Foco pico del bíceps.'],
  ['Curl con cuerda en polea baja', 'Brazos', 'Polea', ['GYM','MIXTO'], 'PULL', 'Cable bajo con cuerda. Tensión constante en todo el rango. 4x12.'],
  ['Press francés con barra Z', 'Brazos', 'Barra Z', ['GYM','MIXTO'], 'PUSH', 'Tumbado, barra Z. Baja a la frente flexionando codos. Foco tríceps.'],
  ['Extensión de tríceps en polea con cuerda', 'Brazos', 'Polea', ['GYM','MIXTO'], 'PUSH', 'Cuerda en polea alta. Extiende abriendo cuerda. 4x12.'],
  ['Extensión de tríceps por encima de la cabeza', 'Brazos', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PUSH', 'Mancuerna con ambas manos por encima de cabeza. Baja detrás del cuello. Foco cabeza larga.'],
  ['Fondos en banco (dips)', 'Brazos', 'Banco', ['CASA','GYM','CALISTENIA','MIXTO'], 'PUSH', 'Manos en banco, pies en suelo. Baja codos a 90° y empuja. 4x12.'],
  ['Patada de tríceps con mancuerna', 'Brazos', 'Mancuerna', ['GYM','CASA','MIXTO'], 'PUSH', 'Inclinado, codo arriba, antebrazo extiende hacia atrás. 4x12/brazo.'],
  ['Curl en araña en banco inclinado', 'Brazos', 'Mancuerna', ['GYM','MIXTO'], 'PULL', 'Boca abajo en banco inclinado. Mancuernas colgando. Curl puro sin balanceo.']
];
BRAZOS.forEach(([n,g,eq,ent,il,ins]) => add(n,'FUERZA',g,eq,ent,il,ins));

// ====================== MOVILIDAD / CALENTAMIENTO específico oposición (15 nuevos) ======================
const MOVILIDAD = [
  ['Movilidad de cadera 90/90', 'Movilidad', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'GENERAL', 'Sentado con ambas rodillas a 90°. Rota tronco y cambia de lado. Crucial para sprints.'],
  ['Movilidad torácica con disco', 'Movilidad', 'Disco', ['CASA','GYM','MIXTO'], 'GENERAL', '4 apoyos. Disco en mano. Rota arriba abriendo pecho. 10/lado.'],
  ['Movilidad hombro con palo', 'Movilidad', 'Palo', ['CASA','GYM','MIXTO'], 'GENERAL', 'Palo en manos. Pasa por encima de la cabeza al máximo rango sin dolor.'],
  ['Rotaciones de cadera de pie', 'Movilidad', '—', ['CASA','GYM','PISTA','MIXTO'], 'GENERAL', 'De pie, eleva rodilla y rota cadera hacia fuera. 10/pierna. Calentamiento.'],
  ['Estiramiento de psoas en zancada', 'Movilidad', '—', ['CASA','GYM','CALISTENIA','PISTA','MIXTO'], 'GENERAL', 'Zancada profunda. Empuja cadera abajo. 30s/pierna. Crucial para corredores.'],
  ['Cobra dinámica', 'Movilidad', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'GENERAL', 'Boca abajo. Eleva tronco con brazos. Movilidad lumbar inversa. 8 reps.'],
  ['Camello-gato', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', '4 apoyos. Curva y arquea espalda alternando. 10 reps. Calentamiento columna.'],
  ['Wall slides', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Espalda en pared, brazos en W. Desliza arriba a Y. Movilidad escápula + hombro.'],
  ['Estiramiento mariposa', 'Movilidad', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'GENERAL', 'Sentado, plantas juntas. Inclínate adelante. Aductores + cadera.'],
  ['Sentadilla cossack lenta', 'Movilidad', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'GENERAL', 'Apertura amplia. Cambia lado lentamente. Movilidad cadera + tobillo.'],
  ['Movilidad tobillo con apoyo', 'Movilidad', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'GENERAL', 'De pie, mano en pared. Inclina rodilla sobre dedo gordo. 10/lado. Esencial para sentadillas.'],
  ['Pasada de hombro con goma', 'Movilidad', 'Banda', ['CASA','GYM','CALISTENIA','MIXTO'], 'GENERAL', 'Banda en manos. Pasa por encima cabeza hasta atrás. Movilidad hombro + activación.'],
  ['Estiramiento isquiotibial activo', 'Movilidad', '—', ['CASA','GYM','PISTA','MIXTO'], 'GENERAL', 'De pie, balancea pierna recta arriba con control. 10/pierna. Calentamiento dinámico.'],
  ['Lunge con rotación', 'Movilidad', '—', ['CASA','GYM','CALISTENIA','PISTA','MIXTO'], 'GENERAL', 'Zancada profunda + rota tronco al lado de la pierna delantera. 8/lado.'],
  ['World greatest stretch', 'Movilidad', '—', ['CASA','GYM','CALISTENIA','PISTA','MIXTO'], 'GENERAL', 'Combo: zancada + rotación + abrir cadera + isquios. 8/lado. Calentamiento completo.']
];
MOVILIDAD.forEach(([n,g,eq,ent,il,ins]) => add(n,'MOVILIDAD',g,eq,ent,il,ins));

// ====================== Categorías AGILIDAD reasignados como VELOCIDAD ======================
// (ya añadidos arriba con pilar VELOCIDAD)

// Guardar
fs.writeFileSync(file, JSON.stringify(data, null, 2));
console.log(`OK. Total ejercicios en banco: ${data.ejercicios.length}`);

// Estadística por pilar
const porPilar = {};
data.ejercicios.forEach((e) => {
  porPilar[e.pilar] = (porPilar[e.pilar] || 0) + 1;
});
console.log('Por pilar:', porPilar);
