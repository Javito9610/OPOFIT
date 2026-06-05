/**
 * Genera backend/data/ejercicios-banco-500.json (~500 ejercicios únicos).
 * Ejecutar: node scripts/generar-banco-ejercicios-500.js
 */
const fs = require('fs');
const path = require('path');

const ENTORNOS = {
  gym: 'GYM,CROSSFIT,MIXTO',
  casa: 'CASA,MIXTO',
  cali: 'CALISTENIA,CASA,PISTA,MIXTO',
  pista: 'PISTA,MIXTO',
  cross: 'CROSSFIT,GYM,MIXTO',
  mix: 'GYM,CASA,CALISTENIA,PISTA,CROSSFIT,MIXTO',
  agua: 'PISTA,GYM,MIXTO'
};

function ej(nombre, pilar, grupo, equip, instr, entornos, ilust) {
  return { nombre, pilar, grupo_muscular: grupo, equipamiento: equip, instrucciones_tecnicas: instr, entornos, tipo_ilustracion: ilust };
}

const lista = [];
const seen = new Set();

function add(...args) {
  const e = ej(...args);
  const key = e.nombre.toLowerCase();
  if (seen.has(key)) return;
  seen.add(key);
  lista.push(e);
}

// ── FUERZA PECHO / EMPUJE ──
const pushBases = [
  ['Press banca plano con barra', 'Barra/banco', 'Escápulas retraídas, toque pecho controlado.', 'PUSH', ENTORNOS.gym],
  ['Press banca inclinado con barra', 'Barra/banco inclinado', 'Inclinación 30-45°, barra al pecho superior.', 'PUSH', ENTORNOS.gym],
  ['Press banca declinado con barra', 'Barra/banco declinado', 'Pecho inferior, codos a 45°.', 'PUSH', ENTORNOS.gym],
  ['Press banca con mancuernas', 'Mancuernas/banco', 'Rango completo, muñecas neutras.', 'PUSH', ENTORNOS.gym],
  ['Press inclinado con mancuernas', 'Mancuernas/banco', 'Empuje convergente suave arriba.', 'PUSH', ENTORNOS.gym],
  ['Press declinado con mancuernas', 'Mancuernas/banco', 'Control en la bajada.', 'PUSH', ENTORNOS.gym],
  ['Press en máquina convergente', 'Máquina pecho', 'Empuje sin bloquear codos.', 'PUSH', ENTORNOS.gym],
  ['Cruce de poleas alto', 'Polea', 'Codos ligeramente flexionados.', 'PUSH', ENTORNOS.gym],
  ['Cruce de poleas bajo', 'Polea', 'Pecho inferior, contracción al centro.', 'PUSH', ENTORNOS.gym],
  ['Aperturas con mancuernas', 'Mancuernas/banco', 'Arco amplio, codo fijo.', 'PUSH', ENTORNOS.gym],
  ['Pullover con mancuerna', 'Mancuerna/banco', 'Brazos semiflexionados, foco dorsal.', 'PULL', ENTORNOS.gym],
  ['Flexiones estándar', 'Suelo', 'Cuerpo recto, pecho al suelo.', 'PUSH', ENTORNOS.cali],
  ['Flexiones inclinadas', 'Banco/mesa', 'Manos elevadas, progresión.', 'PUSH', ENTORNOS.casa],
  ['Flexiones declinadas', 'Banco', 'Pies elevados, core activo.', 'PUSH', ENTORNOS.cali],
  ['Flexiones diamante', 'Suelo', 'Manos juntas, tríceps y pecho interno.', 'PUSH', ENTORNOS.casa],
  ['Flexiones archer', 'Suelo', 'Peso lateral alternado.', 'PUSH', ENTORNOS.cali],
  ['Flexiones con palmada', 'Suelo', 'Explosivas, aterrizaje suave.', 'PUSH', ENTORNOS.cali],
  ['Flexiones pseudo planche', 'Suelo', 'Manos a la altura de cadera.', 'PUSH', ENTORNOS.cali],
  ['Flexiones en anillas', 'Anillas', 'Estabilidad máxima, pecho profundo.', 'PUSH', ENTORNOS.cali],
  ['Fondos en paralelas', 'Paralelas', 'Descenso 90°, hombros abajo.', 'PUSH', ENTORNOS.cali],
  ['Fondos en banco', 'Banco', 'Piernas extendidas, codos atrás.', 'PUSH', ENTORNOS.casa],
  ['Press landmine unilateral', 'Barra/landmine', 'Empuje angular, anti-rotación.', 'PUSH', ENTORNOS.cross],
  ['Press pallof con banda', 'Banda', 'Anti-rotación, brazos extendidos.', 'PLANK', ENTORNOS.casa],
  ['Flexiones con banda', 'Banda', 'Resistencia en la subida.', 'PUSH', ENTORNOS.casa],
  ['Press en TRX', 'TRX', 'Inclinación corporal, core firme.', 'PUSH', ENTORNOS.cross]
];
pushBases.forEach(([n, eq, ins, il, ent]) => add(n, 'FUERZA', 'Pecho', eq, ins, ent, il));

const pressMilitar = [
  ['Press militar con barra', 'Barra/rack', 'Barra frente, glúteos activos.'],
  ['Press Arnold', 'Mancuernas', 'Rotación controlada al subir.'],
  ['Press con mancuernas sentado', 'Mancuernas/banco', 'Espalda apoyada, sin arqueo.'],
  ['Press en máquina de hombros', 'Máquina', 'Empuje vertical controlado.'],
  ['Elevaciones laterales', 'Mancuernas', 'Codos ligeramente flexionados.'],
  ['Elevaciones frontales', 'Mancuernas', 'Hasta altura de hombros.'],
  ['Pájaros con mancuernas', 'Mancuernas', 'Deltoides posterior, pecho al banco.'],
  ['Face pull en polea', 'Polea', 'Codos altos, rotación externa.'],
  ['Remo al mentón con barra', 'Barra', 'Codos altos, sin balanceo.'],
  ['Handstand push-up en pared', 'Pared', 'Progresión vertical, core activo.'],
  ['Pike push-up', 'Suelo', 'Cadera alta, cabeza al suelo.'],
  ['Elevaciones laterales con banda', 'Banda', 'Tensión constante.'],
  ['Y-raise en banco inclinado', 'Mancuernas/banco', 'Deltoides posterior y trapecio medio.'],
  ['Cuban press', 'Mancuernas', 'Rotación externa + press.'],
  ['Push press con barra', 'Barra', 'Impulso de piernas, bloqueo arriba.']
];
pressMilitar.forEach(([n, eq, ins]) => add(n, 'FUERZA', 'Hombros', eq, ins, ENTORNOS.gym, 'PUSH'));

// ── FUERZA ESPALDA / TIRÓN ──
const pullBases = [
  ['Dominadas pronas', 'Barra fija', 'Barbilla sobre barra, sin kipping.'],
  ['Dominadas supinas', 'Barra fija', 'Chin-up controlado.'],
  ['Dominadas neutras', 'Barra fija', 'Agarre paralelo, hombros abajo.'],
  ['Dominadas asistidas con banda', 'Barra/banda', 'Progresión hacia dominada libre.'],
  ['Dominadas con lastre', 'Barra/discos', 'RIR 1-2, técnica estricta.'],
  ['Dominadas explosivas', 'Barra fija', 'Pecho a barra con intención.'],
  ['Dominadas australianas', 'Barra baja', 'Cuerpo recto, pecho a barra.'],
  ['Dominadas tipo L-sit', 'Barra fija', 'Piernas extendidas, core activo.'],
  ['Muscle-up en barra', 'Barra fija', 'Transición limpia, falso agarre.'],
  ['Remo con barra', 'Barra', 'Bisagra, barra al ombligo.'],
  ['Remo pendlay', 'Barra', 'Torso paralelo al suelo, tirón explosivo.'],
  ['Remo mancuerna unilateral', 'Mancuerna/banco', 'Sin rotar tronco.'],
  ['Remo en máquina sentado', 'Máquina', 'Pecho apoyado, escápulas juntas.'],
  ['Remo en T-bar', 'Barra/T-bar', 'Agarre neutro, tirón al pecho.'],
  ['Jalón al pecho agarre ancho', 'Polea', 'Pecho alto, codos abajo.'],
  ['Jalón al pecho agarre neutro', 'Polea', 'Codos al bolsillo.'],
  ['Jalón al pecho agarre supino', 'Polea', 'Mayor implicación bíceps.'],
  ['Pull-over en polea alta', 'Polea', 'Brazos rectos, foco dorsal.'],
  ['Remo invertido en mesa', 'Mesa', 'Cuerpo recto bajo mesa.'],
  ['Remo con banda anclada', 'Banda', 'Tirón al abdomen.'],
  ['Remo con mochila', 'Mochila', 'Bisagra de cadera en casa.'],
  ['Pulldown con banda', 'Banda', 'Simula jalón en casa.'],
  ['Encogimientos con barra', 'Barra', 'Trapecio superior, sin balanceo.'],
  ['Encogimientos con mancuernas', 'Mancuernas', 'Pausa arriba.'],
  ['Hiperextensiones en banco 45°', 'Banco 45°', 'Glúteos y erectores, sin hiperextender.'],
  ['Superman hold', 'Suelo', 'Brazos y piernas elevados.'],
  ['Good morning con barra', 'Barra', 'Bisagra de cadera, espalda neutra.'],
  ['Remo en TRX', 'TRX', 'Cuerpo inclinado, tirón al pecho.'],
  ['Front lever tuck', 'Barra fija', 'Isometría, rodillas al pecho.'],
  ['Remo con kettlebell', 'Kettlebell', 'Tirón unilateral controlado.']
];
pullBases.forEach(([n, eq, ins]) => add(n, 'FUERZA', 'Espalda', eq, ins, ENTORNOS.mix, 'PULL'));

// ── FUERZA PIERNA ──
const squatBases = [
  ['Sentadilla trasera con barra', 'Barra/rack', 'Profundidad paralela o más.'],
  ['Sentadilla frontal con barra', 'Barra/rack', 'Codos altos, torso vertical.'],
  ['Sentadilla goblet', 'Mancuerna/kettlebell', 'Carga al pecho.'],
  ['Sentadilla con pausa', 'Barra/rack', 'Pausa 2 s en el fondo.'],
  ['Sentadilla búlgara', 'Mancuernas/banco', 'Rodilla trasera casi al suelo.'],
  ['Sentadilla sumo con barra', 'Barra', 'Pies anchos, rodillas alineadas.'],
  ['Sentadilla con mochila', 'Mochila', 'Peso en espalda, profundidad controlada.'],
  ['Sentadilla pistol asistida', 'Barra fija', 'Una pierna, agarre para equilibrio.'],
  ['Sentadilla en cajón', 'Barra/cajón', 'Profundidad consistente.'],
  ['Sentadilla hack en máquina', 'Máquina', 'Pies medios en plataforma.'],
  ['Prensa de piernas', 'Máquina', 'Sin bloquear rodillas.'],
  ['Prensa unilateral', 'Máquina', 'Corregir desequilibrios.'],
  ['Zancadas caminando', 'Mancuernas', 'Paso largo, torso erguido.'],
  ['Zancadas en el sitio', 'Suelo', 'Alternar piernas sin desplazarte.'],
  ['Zancadas laterales', 'Mancuernas', 'Paso lateral, cadera atrás.'],
  ['Zancadas inversas', 'Mancuernas', 'Paso atrás, rodilla estable.'],
  ['Step-up con mancuernas', 'Mancuernas/cajón', 'Empuje con pierna de apoyo.'],
  ['Peso muerto convencional', 'Barra', 'Lumbar neutra, barra pegada.'],
  ['Peso muerto sumo', 'Barra', 'Pies anchos, agarre dentro de rodillas.'],
  ['Peso muerto rumano', 'Barra/mancuernas', 'Bisagra, isquios estirados.'],
  ['Peso muerto con mochila', 'Mochila', 'Bisagra en casa.'],
  ['Peso muerto trap bar', 'Barra hexagonal', 'Torso más vertical.'],
  ['Hip thrust con barra', 'Barra/banco', 'Pausa arriba, mentón al pecho.'],
  ['Puente de glúteo', 'Suelo', 'Empuje de cadera, glúteos activos.'],
  ['Patada de glúteo en polea', 'Polea', 'Cadera estable, extensión completa.'],
  ['Abducción en máquina', 'Máquina', 'Glúteo medio, controlado.'],
  ['Curl femoral tumbado', 'Máquina', 'Isquios, sin despegar cadera.'],
  ['Curl femoral sentado', 'Máquina', 'Rango completo.'],
  ['Nordic curl', 'Suelo/compañero', 'Excéntrico controlado.'],
  ['Curl nórdico asistido con banda', 'Banda', 'Progresión nordic.'],
  ['Extensión de cuádriceps', 'Máquina', 'Sin bloquear rodilla arriba.'],
  ['Sissy squat', 'Suelo', 'Cuádriceps, rodillas adelante.'],
  ['Gemelos de pie en máquina', 'Máquina', 'Rango completo, pausa arriba.'],
  ['Gemelos en prensa', 'Prensa', 'Puntillas en borde de plataforma.'],
  ['Gemelos unilateral', 'Mancuerna', 'Máxima elongación abajo.'],
  ['Tibial anterior con banda', 'Banda', 'Prevención shin splints.'],
  ['Sentadilla con banda en rodillas', 'Banda', 'Empuje contra banda.'],
  ['Caminata del granjero', 'Mancuernas/kettlebells', 'Hombros cuadrados.'],
  ['Sled push', 'Trineo', 'Inclinación baja, máxima intención.'],
  ['Sled drag marcha atrás', 'Trineo', 'Cadena posterior.'],
  ['Wall sit 60 s', 'Pared', 'Isometría cuádriceps.'],
  ['Cossack squat', 'Suelo', 'Movilidad y fuerza lateral.'],
  ['Split squat con mancuernas', 'Mancuernas', 'Pierna delantera carga principal.']
];
squatBases.forEach(([n, eq, ins]) => add(n, 'FUERZA', 'Pierna', eq, ins, ENTORNOS.mix, 'SQUAT'));

// ── FUERZA BRAZOS ──
const brazos = [
  ['Curl con barra Z', 'Barra Z', 'Sin balanceo.'],
  ['Curl con mancuernas alterno', 'Mancuernas', 'Supinación al subir.'],
  ['Curl martillo', 'Mancuernas', 'Agarre neutro.'],
  ['Curl concentrado', 'Mancuerna/banco', 'Codo apoyado en muslo.'],
  ['Curl en polea baja', 'Polea', 'Tensión constante.'],
  ['Curl con banda', 'Banda', 'Resistencia progresiva.'],
  ['Curl con botellas de agua', 'Botellas', 'Peso improvisado en casa.'],
  ['Curl spider en banco', 'Mancuernas/banco', 'Brazos perpendiculares al suelo.'],
  ['Curl 21s con barra', 'Barra', '7+7+7 parcial y completo.'],
  ['Extensión de tríceps en polea', 'Polea', 'Codos fijos.'],
  ['Extensión de tríceps con banda', 'Banda', 'Extensión completa.'],
  ['Press francés con barra Z', 'Barra Z/banco', 'Codos apuntan al techo.'],
  ['Extensión tras nuca con mancuerna', 'Mancuerna', 'Codos cerrados.'],
  ['Patada de tríceps', 'Mancuerna/banco', 'Codo fijo, extensión completa.'],
  ['Fondos en banco pies elevados', 'Banco', 'Tríceps y pecho inferior.'],
  ['Extensión en TRX', 'TRX', 'Cuerpo inclinado, codos fijos.'],
  ['Wrist curl con barra', 'Barra', 'Agarre oposición.'],
  ['Reverse wrist curl', 'Barra', 'Extensores de muñeca.'],
  ['Colgado en barra máx tiempo', 'Barra fija', 'Agarre y antebrazo.'],
  ['Farmer hold isométrico', 'Mancuernas', 'Agarre máximo tiempo.'],
  ['Pinch grip con discos', 'Discos', 'Agarre de pinza.'],
  ['Towel pull-up', 'Barra/toalla', 'Agarre reforzado.'],
  ['Chin-up con pausa arriba', 'Barra fija', 'Isometría bíceps.'],
  ['Dips asistidos con banda', 'Paralelas/banda', 'Progresión fondos.'],
  ['Close grip bench press', 'Barra/banco', 'Tríceps y pecho interno.']
];
brazos.forEach(([n, eq, ins]) => add(n, 'FUERZA', 'Brazos', eq, ins, ENTORNOS.mix, n.includes('tríceps') || n.includes('Extensión') || n.includes('fondos') || n.includes('Dips') ? 'PUSH' : 'PULL'));

// ── CORE ──
const coreEj = [
  ['Plancha frontal', 'Suelo', 'Cadera neutra, glúteos activos.'],
  ['Plancha lateral', 'Suelo', 'Cadera elevada.'],
  ['Plancha con elevación de pierna', 'Suelo', 'Sin rotar cadera.'],
  ['Plancha con toque de hombro', 'Suelo', 'Anti-rotación.'],
  ['Hollow body hold', 'Suelo', 'Lumbar pegada al suelo.'],
  ['Dead bug', 'Suelo', 'Movimiento contralateral controlado.'],
  ['Bird dog', 'Suelo', 'Extensión opuesta, cadera estable.'],
  ['Ab wheel rollout', 'Rueda abdominal', 'Rango controlado.'],
  ['Crunch en polea alta', 'Polea', 'Flexión de tronco, no tirar con brazos.'],
  ['Elevación de piernas colgado', 'Barra fija', 'Sin balanceo.'],
  ['Knee raise en paralelas', 'Paralelas', 'Pelvis posterior.'],
  ['Russian twist con disco', 'Disco', 'Rotación controlada.'],
  ['Pallof press', 'Polea/banda', 'Anti-rotación.'],
  ['Suitcase carry', 'Mancuerna/kettlebell', 'Anti-lateral flexión.'],
  ['Turkish get-up', 'Kettlebell', 'Movimiento lento y completo.'],
  ['V-ups', 'Suelo', 'Tocar pies con manos.'],
  ['Bicycle crunch', 'Suelo', 'Codo a rodilla opuesta.'],
  ['Plancha con reach through', 'Suelo', 'Alcance bajo el cuerpo.'],
  ['Copenhagen plank', 'Banco', 'Aductores y core lateral.'],
  ['Dragon flag progresión', 'Banco', 'Control excéntrico.'],
  ['L-sit en paralelas', 'Paralelas', 'Isometría core y flexores.'],
  ['Hanging windshield wiper', 'Barra fija', 'Rotación controlada.'],
  ['Cable woodchop', 'Polea', 'Rotación desde cadera.'],
  ['Bear crawl 20 m', 'Suelo', 'Rodillas cerca del suelo.'],
  ['Mountain climbers', 'Suelo', 'Cadera baja, ritmo controlado.']
];
coreEj.forEach(([n, eq, ins]) => add(n, 'CORE', 'Core', eq, ins, ENTORNOS.mix, 'PLANK'));

// ── RESISTENCIA CARRERA ──
const distancias = [100, 200, 300, 400, 600, 800, 1000, 1200, 1600, 2000, 3000, 5000];
const repsSeries = [3, 4, 5, 6, 8, 10, 12];
distancias.forEach((d) => {
  repsSeries.forEach((r) => {
    add(`Series ${d} m x ${r}`, 'RESISTENCIA', 'Cardio', 'Pista', `Ritmo umbral, recuperación ${d < 400 ? '90 s' : '2-3 min'}.`, ENTORNOS.pista, 'RUN');
  });
});
[15, 20, 25, 30, 35, 40, 45, 50, 60, 75, 90].forEach((min) => {
  add(`Carrera continua Z2 ${min} min`, 'RESISTENCIA', 'Cardio', 'Pista', 'RPE 4-5, conversación posible.', ENTORNOS.pista, 'RUN');
  add(`Trote suave ${min} min`, 'RESISTENCIA', 'Cardio', 'Pista', 'Rodaje regenerativo.', ENTORNOS.pista, 'RUN');
  add(`Carrera en el sitio Z2 ${min} min`, 'RESISTENCIA', 'Cardio', 'Suelo', 'Rodaje indoor sin desplazamiento.', ENTORNOS.casa, 'RUN');
});
[10, 15, 20, 25, 30].forEach((min) => {
  add(`Fartlek ${min} min`, 'RESISTENCIA', 'Cardio', 'Pista', 'Alternar Z2 y Z3 libremente.', ENTORNOS.pista, 'RUN');
  add(`Cambios de ritmo ${min} min`, 'RESISTENCIA', 'Cardio', 'Pista', '2 min rápido / 2 min suave.', ENTORNOS.pista, 'RUN');
  add(`Tempo run ${min} min`, 'RESISTENCIA', 'Cardio', 'Pista', 'Ritmo umbral sostenido.', ENTORNOS.pista, 'RUN');
});
[6, 8, 10, 12, 15].forEach((pct) => {
  add(`Cuestas ${pct} x 100 m`, 'RESISTENCIA', 'Cardio', 'Cuesta 6-8 %', 'Potencia resistida, bajada suave.', ENTORNOS.pista, 'RUN');
});
['Test 1000 m', 'Test 1600 m', 'Test 2000 m', 'Test 3000 m', 'Test Cooper 12 min'].forEach((n) => {
  add(n, 'RESISTENCIA', 'Cardio', 'Pista', 'Ritmo de prueba oficial oposición.', ENTORNOS.pista, 'RUN');
});
[8, 10, 12, 15, 20].forEach((min) => {
  add(`HIIT carrera ${min} min`, 'RESISTENCIA', 'Cardio', 'Pista', '30 s fuerte / 30 s suave.', ENTORNOS.pista, 'RUN');
  add(`HIIT 4x4 min`, 'RESISTENCIA', 'Cardio', 'Pista', '4 min al 95 % FCmáx, R 3 min.', ENTORNOS.pista, 'RUN');
});
['Caminata rápida 30 min', 'Caminata rápida 45 min', 'Marcha nórdica 40 min', 'Escaleras 10 min', 'Escaleras 15 min', 'Escaleras 20 min'].forEach((n) => {
  add(n, 'RESISTENCIA', 'Cardio', 'Exterior', 'RPE 4-6, técnica activa.', ENTORNOS.pista, 'RUN');
});

// ── RESISTENCIA OTROS CARDIO ──
const cardioOtros = [
  ['Bicicleta estática Z2 20 min', 'Bici', 'Cadencia 80-90 rpm.'],
  ['Bicicleta estática Z2 30 min', 'Bici', 'Rodaje bajo impacto.'],
  ['Bicicleta estática intervalos 25 min', 'Bici', '1 min fuerte / 2 min suave.'],
  ['Elíptica Z2 20 min', 'Elíptica', 'Recuperación activa.'],
  ['Elíptica Z2 30 min', 'Elíptica', 'Sin agarrar apoyos.'],
  ['Remo ergómetro 2000 m', 'Remo', 'Ritmo umbral.'],
  ['Remo ergómetro 500 m x 4', 'Remo', 'RPE 8, R 3 min.'],
  ['Assault bike 10 min Z2', 'Bici assault', 'Rodaje en box.'],
  ['Assault bike 20 min intervalos', 'Bici assault', '30:30 x 20.'],
  ['Step mill 15 min', 'Step', 'Intensidad moderada-alta.'],
  ['Step mill 25 min', 'Step', 'RPE 6-7.'],
  ['Natación crol continua 20 min', 'Piscina', 'Ritmo cómodo.'],
  ['Natación crol continua 30 min', 'Piscina', 'Técnica y respiración bilateral.'],
  ['Natación técnica 400 m', 'Piscina', 'Series 50 m técnica.'],
  ['Natación técnica 800 m', 'Piscina', 'Drills: cucharada, un brazo.'],
  ['Natación 50 m series x 8', 'Piscina', 'Ritmo prueba oficial.'],
  ['Natación 50 m series x 12', 'Piscina', 'Velocidad con descanso 45 s.'],
  ['Natación 25 m sprint x 10', 'Piscina', 'Máxima velocidad.'],
  ['Natación espalda 100 m x 4', 'Piscina', 'Técnica y alineación.'],
  ['Aqua jogging 20 min', 'Piscina', 'Carrera en profundo con cinturón.'],
  ['Battle ropes 30:30 x 10', 'Cuerdas', 'Onda alterna, core firme.'],
  ['Battle ropes 40:20 x 12', 'Cuerdas', 'Intervalos de potencia.'],
  ['Burpees AMRAP 8 min', 'Suelo', 'Ritmo sostenible.'],
  ['Burpees AMRAP 12 min', 'Suelo', 'Técnica limpia.'],
  ['Saltos a comba 5 min', 'Comba', 'Calentamiento dinámico.'],
  ['Saltos a comba 10 min', 'Comba', 'Ritmo moderado.'],
  ['Saltos a comba intervalos 15 min', 'Comba', '1 min rápido / 30 s lento.'],
  ['Kettlebell swing 4x20', 'Kettlebell', 'Impulso de cadera.'],
  ['Kettlebell swing EMOM 12 min', 'Kettlebell', '15 reps cada minuto.'],
  ['Wall ball 4x15', 'Balón 3-5 kg', 'Sentadilla + lanzamiento.'],
  ['Devil press 4x8', 'Mancuernas', 'Burpee + snatch overhead.'],
  ['Thruster con mancuernas 4x12', 'Mancuernas', 'Sentadilla + press.'],
  ['Circuito CrossFit 15 min', 'Variable', '3-4 movimientos, AMRAP.'],
  ['Circuito CrossFit 20 min', 'Variable', 'WOD oposición, técnica primero.'],
  ['Rucking 45 min', 'Mochila 10-15 kg', 'Caminata con carga.'],
  ['Rucking 60 min', 'Mochila 10-15 kg', 'Resistencia militar.']
];
cardioOtros.forEach(([n, eq, ins]) => add(n, 'RESISTENCIA', 'Cardio', eq, ins, ENTORNOS.mix, 'RUN'));

// ── VELOCIDAD / AGILIDAD / PLIOMETRÍA ──
[20, 30, 40, 50, 60, 80, 100].forEach((d) => {
  [4, 5, 6, 8, 10].forEach((r) => {
    add(`Sprint ${d} m x ${r}`, 'VELOCIDAD', 'Velocidad', 'Pista', 'Aceleración máxima, R 90 s-2 min.', ENTORNOS.pista, 'AGILITY');
  });
});
const velAgil = [
  ['Sprint con resistencia paracaídas 30 m', 'Pista/paracaídas', 'Técnica alta, 20-30 m.'],
  ['Sprint arranque acostado 20 m', 'Pista', 'Salida explosiva.'],
  ['Sprint arranque de pie 30 m', 'Pista', 'Primera zancada potente.'],
  ['Sprint con trineo ligero 20 m', 'Trineo', 'Resistencia específica.'],
  ['Skipping A 3x20 m', 'Pista', 'Rodillas altas.'],
  ['Skipping B 3x20 m', 'Pista', 'Talón al glúteo.'],
  ['Skipping C 3x20 m', 'Pista', 'Multisaltos.'],
  ['Carioca 3x20 m', 'Pista', 'Cadera baja.'],
  ['Carioca 4x30 m', 'Pista', 'Pies rápidos.'],
  ['Conos en T 4 vueltas', 'Conos', 'Cambios 90° y 180°.'],
  ['Conos en zigzag 6x', 'Conos', 'Cambios de dirección.'],
  ['Conos 5-10-5 pro-agility', 'Conos', 'Test agilidad estándar.'],
  ['Circuito de agilidad cronometrado', 'Conos/vallas', 'Ritmo prueba oficial.'],
  ['Agilidad con conos caseros', 'Conos', 'Botellas o conos improvisados.'],
  ['Escalera de coordinación 5 min', 'Escalera', 'Patrones variados.'],
  ['Escalera lateral 4x', 'Escalera', 'Pies rápidos sin mirar.'],
  ['Vallas bajas 4x8', 'Vallas', 'Ritmo de zancada constante.'],
  ['Vallas altas 3x6', 'Vallas', 'Técnica de paso.'],
  ['Saltos al cajón 4x5', 'Cajón', 'Aterrizaje suave.'],
  ['Saltos al cajón 5x3', 'Cajón', 'Altura máxima controlada.'],
  ['Box jump step-down', 'Cajón', 'Bajada controlada, sin rebote.'],
  ['CMJ 4x5', 'Suelo', 'Countermovement jump máximo.'],
  ['SJ 4x4', 'Suelo', 'Squat jump sin contramovimiento.'],
  ['Triple salto 3x3', 'Suelo', 'Contactos mínimos.'],
  ['Drop jump 4x5', 'Cajón 30 cm', 'Rebote inmediato.'],
  ['Lanzamiento balón medicinal 4x6', 'Balón 3-5 kg', 'Explosivo desde pecho.'],
  ['Lanzamiento balón medicinal rotacional', 'Balón', 'Rotación desde cadera.'],
  ['Salto vertical máximo x 5', 'Suelo', 'Alcance máximo.'],
  ['Salto horizontal máximo x 5', 'Suelo', 'Distancia máxima.'],
  ['Multisaltos 4x20 m', 'Pista', 'Zancada rítmica.'],
  ['Sprint en el sitio 20 s x 8', 'Suelo', 'Rodillas altas máximas.'],
  ['Reacción sprint 10 m x 6', 'Pista', 'Salida a señal.'],
  ['Cambio de dirección 90° x 8', 'Conos', 'Frenada y aceleración.'],
  ['L drill agilidad', 'Conos', 'Patrón en L cronometrado.'],
  ['T-test agilidad', 'Conos', 'Test estándar oposición.'],
  ['Salto lateral sobre valla x 10', 'Valla baja', 'Pliometría lateral.'],
  ['Pogo jumps 3x15', 'Suelo', 'Tobillos rígidos, mínima flexión rodilla.'],
  ['Broad jump 4x4', 'Suelo', 'Salto largo desde parado.'],
  ['Sprint con cambio de ritmo 80 m', 'Pista', '40 m aceleración + 40 m máximo.']
];
velAgil.forEach(([n, eq, ins]) => add(n, 'VELOCIDAD', eq.includes('Conos') || eq.includes('Escalera') || eq.includes('Vallas') ? 'Agilidad' : eq.includes('Cajón') || ins.includes('salto') || ins.includes('jump') ? 'Pliometría' : 'Velocidad', eq, ins, ENTORNOS.mix, 'AGILITY'));

// ── MOVILIDAD ──
const movilidad = [
  ['Movilidad cadera 8 min', 'Suelo', '90/90, frog stretch, activación glúteo.'],
  ['Movilidad cadera 12 min', 'Suelo', 'Rotaciones y estiramientos dinámicos.'],
  ['Movilidad hombro 8 min', 'Banda', 'Dislocaciones, face pull ligero.'],
  ['Movilidad hombro 12 min', 'Banda', 'Rotación interna/externa.'],
  ['Movilidad tobillo 6 min', 'Suelo', 'Rodilla al muro, elevación talones.'],
  ['Movilidad columna 8 min', 'Suelo', 'Gato-vaca, rotaciones torácicas.'],
  ['RAMP activación 10 min', 'Suelo', 'Raise-Activate-Mobilise-Potentiate.'],
  ['RAMP activación 15 min', 'Suelo', 'Preparación completa pre-entreno.'],
  ['Estiramientos post-entreno 8 min', 'Suelo', 'Isquios, cuádriceps, dorsal.'],
  ['Estiramientos post-entreno 15 min', 'Suelo', 'Vuelta a la calma global.'],
  ['Foam rolling cuádriceps 3 min', 'Foam roller', 'Presión lenta, puntos trigger.'],
  ['Foam rolling isquios 3 min', 'Foam roller', 'De glúteo a rodilla.'],
  ['Foam rolling espalda 3 min', 'Foam roller', 'Dorsal y lumbar suave.'],
  ['Yoga flow movilidad 10 min', 'Suelo', 'Flujo suave cadera y hombro.'],
  ['Yoga flow movilidad 20 min', 'Suelo', 'Sesión completa recuperación.'],
  ['World greatest stretch 5/side', 'Suelo', 'Cadera, torácica y isquios.'],
  ['Leg swings frontal 2x15', 'Suelo', 'Dinámico, controlado.'],
  ['Leg swings lateral 2x15', 'Suelo', 'Apertura de cadera.'],
  ['Arm circles 2x20', 'Suelo', 'Hombros progresivos.'],
  ['Hip circles 2x10/dirección', 'Suelo', 'Cadera en cuadrupedia.'],
  ['Thoracic rotation cuadrupedia 8/lado', 'Suelo', 'Mano detrás de cabeza.'],
  ['Couch stretch 2 min/pierna', 'Suelo', 'Flexor de cadera.'],
  ['Pigeon stretch 2 min/lado', 'Suelo', 'Glúteo y rotadores.'],
  ['Hamstring stretch dinámico 10/lado', 'Suelo', 'Patada controlada.'],
  ['Calf stretch pared 45 s/lado', 'Pared', 'Gastrocnemio y sóleo.'],
  ['Shoulder dislocaciones con banda 15', 'Banda', 'Agarre ancho, controlado.'],
  ['Band pull-aparts 3x20', 'Banda', 'Escápulas, deltoides posterior.'],
  ['Cat-cow 2x10', 'Suelo', 'Movilidad espinal.'],
  ['Deep squat hold 2 min', 'Suelo', 'Movilidad tobillo y cadera.'],
  ['Ankle mobility knee to wall 10/lado', 'Pared', 'Dorsiflexión.'],
  ['T-spine extension en foam 10', 'Foam roller', 'Extensiones torácicas.'],
  ['Hip flexor march con banda 12/lado', 'Banda', 'Activación glúteo medio.'],
  ['Glute bridge march 16', 'Suelo', 'Activación pre-fuerza.'],
  ['Clamshell con banda 15/lado', 'Banda', 'Glúteo medio.'],
  ['Fire hydrant 12/lado', 'Suelo', 'Estabilidad cadera.'],
  ['Inchworm 6 reps', 'Suelo', 'Cadena posterior dinámica.'],
  ['Walkout to plank 8', 'Suelo', 'Core y hombros.'],
  ['Lunge with twist 8/lado', 'Suelo', 'Movilidad cadera y torácica.'],
  ['High knees march 30 s', 'Suelo', 'Activación pre-carrera.'],
  ['Butt kicks 30 s', 'Suelo', 'Activación isquios.'],
  ['A-skips 20 m x 3', 'Pista', 'Técnica de carrera.'],
  ['B-skips 20 m x 3', 'Pista', 'Técnica de zancada.']
];
movilidad.forEach(([n, eq, ins]) => add(n, 'MOVILIDAD', 'Movilidad', eq, ins, ENTORNOS.mix, 'MOBILITY'));

// ── Variantes con equipamiento casa / bandas para llegar a 500 ──
const variantesCasa = [
  'Press pecho con banda', 'Remo con banda sentado', 'Sentadilla goblet con botella',
  'Zancada con mochila', 'Elevación lateral con botellas', 'Curl con mochila',
  'Extensión tríceps con mochila overhead', 'Hip thrust con mochila', 'Good morning con mochila',
  'Peso muerto una pierna con mochila', 'Flexiones con pies en silla', 'Dips entre sillas',
  'Plancha con mochila', 'Step-up en escalera', 'Calf raise en escalón',
  'Wall push-up 3x20', 'Bear hold 45 s', 'Superman pulse 15', 'Prone Y raise 12',
  'Prone T raise 12', 'Prone W raise 12', 'Banded squat walk 10 pasos',
  'Banded lateral walk 10 pasos', 'Banded monster walk 10 pasos'
];
variantesCasa.forEach((n) => {
  add(n, 'FUERZA', 'General', 'Casa/banda', 'Técnica controlada, carga moderada.', ENTORNOS.casa, 'GENERAL');
});

// Rellenar hasta 500 con combinaciones lógicas adicionales
const rellenoGym = [
  ['Cable kickback glúteo', 'Polea', 'Extensión cadera, pie flexionado.'],
  ['Máquina aductor', 'Máquina', 'Aducción controlada.'],
  ['Máquina abductor', 'Máquina', 'Abducción controlada.'],
  ['Leg press unilateral', 'Máquina', 'Corrección desequilibrios.'],
  ['Smith machine squat', 'Smith', 'Trayectoria guiada.'],
  ['Smith machine incline press', 'Smith', 'Press inclinado guiado.'],
  ['Pec deck', 'Máquina', 'Aperturas en máquina.'],
  ['Lat pulldown single arm', 'Polea', 'Unilateral, anti-rotación.'],
  ['Seated calf raise', 'Máquina', 'Sóleo, rango completo.'],
  ['Standing calf raise máquina', 'Máquina', 'Gastrocnemio.'],
  ['Hip abduction cable', 'Polea', 'Glúteo medio.'],
  ['Reverse hyperextension', 'Máquina', 'Cadena posterior.'],
  ['Glute ham raise', 'Máquina GHD', 'Isquios y glúteos.'],
  ['Back extension sin carga', 'Banco 45°', 'Erectores, sin hiperextender.'],
  ['Landmine row', 'Barra/landmine', 'Tirón unilateral.'],
  ['Landmine squat', 'Barra/landmine', 'Sentadilla con barra anclada.'],
  ['Zercher squat', 'Barra', 'Barra en codos, core activo.'],
  ['Jefferson deadlift', 'Barra', 'Agarre mixto, anti-rotación.'],
  ['Deficit deadlift', 'Barra/discos', 'Rango ampliado desde plataforma.'],
  ['Block pull deadlift', 'Barra', 'Rango reducido, sobrecarga.'],
  ['Pause bench press', 'Barra/banco', 'Pausa 2 s en pecho.'],
  ['Spoto press', 'Barra/banco', 'Pausa 2-3 cm sobre pecho.'],
  ['Floor press', 'Barra', 'Rango parcial en suelo.'],
  ['Pin press', 'Barra/rack', 'Desde pins, sin impulso.'],
  ['Board press', 'Barra/tablas', 'Rango parcial lockout.'],
  ['Incline dumbbell fly', 'Mancuernas/banco', 'Pecho superior.'],
  ['Decline dumbbell fly', 'Mancuernas/banco', 'Pecho inferior.'],
  ['Cable crossover', 'Polea', 'Contracción al centro.'],
  ['Single arm cable press', 'Polea', 'Anti-rotación.'],
  ['Kettlebell clean', 'Kettlebell', 'Transición limpia al rack.'],
  ['Kettlebell snatch', 'Kettlebell', 'Impulso de cadera, bloqueo arriba.'],
  ['Kettlebell goblet squat', 'Kettlebell', 'Profundidad y torso erguido.'],
  ['Kettlebell windmill', 'Kettlebell', 'Movilidad y estabilidad hombro.'],
  ['Sandbag carry', 'Saco arena', 'Agarre y core.'],
  ['Sandbag squat', 'Saco arena', 'Carga inestable.'],
  ['Tire flip 6x', 'Neumático', 'Potencia cuerpo completo.'],
  ['Sledgehammer tire hits 20', 'Mazo/neumático', 'Potencia rotacional.'],
  ['Rope climb', 'Cuerda', 'Fuerza de agarre y espalda.'],
  ['Rope pull sled', 'Cuerda/trineo', 'Tirón horizontal.'],
  ['Medicine ball slam 4x8', 'Balón medicinal', 'Potencia desde overhead.'],
  ['Medicine ball chest pass', 'Balón medicinal', 'Paso explosivo.'],
  ['Partner resisted sprint 20 m', 'Compañero/banda', 'Resistencia específica.'],
  ['Banded sprint 20 m', 'Banda', 'Aceleración resistida.'],
  ['Weighted vest walk 30 min', 'Chaleco lastrado', 'Resistencia específica oposición.'],
  ['Weighted vest stairs 10 min', 'Chaleco/escalera', 'Fuerza-resistencia.'],
  ['Pool running 25 min', 'Piscina', 'Carrera en profundo.'],
  ['Spinning class 45 min', 'Bici spinning', 'Intervalos guiados.'],
  ['Indoor rowing 30 min steady', 'Remo', 'Z2 sostenido.'],
  ['Ski erg 2000 m', 'Ski erg', 'Cadencia y resistencia.'],
  ['VersaClimber 15 min', 'VersaClimber', 'Cuerpo completo cardio.'],
  ['Jacob\'s ladder 10 min', 'Jacob\'s ladder', 'Intensidad progresiva.'],
  ['Prowler push 4x20 m', 'Trineo', 'Empuje horizontal.'],
  ['Prowler pull 4x20 m', 'Trineo', 'Tirón horizontal.'],
  ['Yoke walk 4x20 m', 'Yoke', 'Estabilidad bajo carga.'],
  ['Log press', 'Tronco', 'Press overhead irregular.'],
  ['Axle bar deadlift', 'Barra axle', 'Agarre grueso.'],
  ['Fat grip deadlift', 'Barra/fat grip', 'Agarre reforzado.'],
  ['Trap bar shrug', 'Barra hexagonal', 'Trapecio superior.'],
  ['Rack pull', 'Barra/rack', 'Rango parcial desde rodillas.'],
  ['Snatch grip deadlift', 'Barra', 'Mayor implicación espalda alta.'],
  ['Sumo deadlift high pull', 'Barra', 'Potencia desde suelo.'],
  ['Clean pull', 'Barra', 'Técnica olímpica, tirón.'],
  ['Hang clean', 'Barra', 'Desde rodilla, recepción rack.'],
  ['Power jerk', 'Barra', 'Impulso y bloqueo overhead.'],
  ['Push jerk', 'Barra', 'Dip-drive, bloqueo.'],
  ['Split jerk', 'Barra', 'Recepción en split.'],
  ['Overhead squat', 'Barra', 'Movilidad y estabilidad.'],
  ['Front rack carry', 'Barra/kettlebells', 'Core y posición rack.'],
  ['Overhead carry', 'Mancuernas/kettlebell', 'Estabilidad hombro.'],
  ['Zercher carry', 'Barra', 'Core y bíceps.'],
  ['Crossover carry', 'Mancuernas', 'Anti-lateral flexión.'],
  ['Waiter walk', 'Kettlebell', 'Estabilidad hombro overhead.'],
  ['Bottoms-up kettlebell press', 'Kettlebell', 'Estabilidad máxima.'],
  ['Kettlebell bottoms-up carry', 'Kettlebell', 'Agarre y hombro.'],
  ['Plate pinch carry 30 m', 'Discos', 'Agarre de pinza.'],
  ['Hub lift', 'Disco', 'Agarre central disco.'],
  ['Gripper closes 3x20', 'Gripper', 'Agarre de mano.'],
  ['Fat bar curl', 'Barra gruesa', 'Agarre y bíceps.'],
  ['Reverse curl barra Z', 'Barra Z', 'Braquiorradial.'],
  ['Preacher curl máquina', 'Máquina', 'Bíceps aislado.'],
  ['Concentration curl cable', 'Polea', 'Pico de contracción.'],
  ['Overhead tricep extension cable', 'Polea', 'Codos fijos arriba.'],
  ['JM press', 'Barra/banco', 'Híbrido press francés.'],
  ['Tate press', 'Mancuernas/banco', 'Tríceps, codos abiertos.'],
  ['Rolling tricep extension', 'Mancuernas', 'Extensión en rodillas.'],
  ['Bodyweight skull crusher', 'Barra baja', 'Tríceps peso corporal.'],
  ['Ring dip', 'Anillas', 'Estabilidad máxima.'],
  ['Ring row', 'Anillas', 'Remo con inestabilidad.'],
  ['Ring push-up', 'Anillas', 'Pecho con inestabilidad.'],
  ['TRX row', 'TRX', 'Remo inclinado.'],
  ['TRX chest press', 'TRX', 'Empuje inclinado.'],
  ['TRX squat', 'TRX', 'Asistencia sentadilla.'],
  ['TRX hamstring curl', 'TRX', 'Isquios, puente unilateral.'],
  ['TRX atomic push-up', 'TRX', 'Flexión + knee tuck.'],
  ['Bosu squat', 'Bosu', 'Inestabilidad, profundidad.'],
  ['Bosu single leg balance', 'Bosu', 'Propiocepción tobillo.'],
  ['Stability ball hamstring curl', 'Fitball', 'Isquios, puente.'],
  ['Stability ball pike', 'Fitball', 'Core, elevación cadera.'],
  ['Resistance band pull-through', 'Banda', 'Glúteo y cadena posterior.'],
  ['Banded good morning', 'Banda', 'Bisagra con resistencia.'],
  ['Banded hip thrust', 'Banda', 'Glúteo con resistencia.'],
  ['Mini band lateral monster walk', 'Mini band', 'Glúteo medio activación.'],
  ['Fire hydrant con banda', 'Mini band', 'Glúteo medio.'],
  ['Clamshell con mini band', 'Mini band', 'Estabilidad cadera.'],
  ['Banded pallof press', 'Banda', 'Anti-rotación.'],
  ['Banded overhead reach', 'Banda', 'Movilidad hombro.'],
  ['Doorway chest stretch 45 s', 'Marco puerta', 'Pectoral estático.'],
  ['Lat stretch en poste 45 s/lado', 'Poste', 'Dorsal y hombro.'],
  ['Quad stretch de pie 45 s/lado', 'Suelo', 'Cuádriceps.'],
  ['Figure-4 stretch 60 s/lado', 'Suelo', 'Glúteo y piriforme.'],
  ['Child\'s pose 60 s', 'Suelo', 'Espalda y cadera.'],
  ['Downward dog 45 s', 'Suelo', 'Cadena posterior.'],
  ['Upward dog 30 s', 'Suelo', 'Extensión lumbar suave.'],
  ['Seated spinal twist 45 s/lado', 'Suelo', 'Rotación torácica.'],
  ['Supine twist 45 s/lado', 'Suelo', 'Lumbar y glúteo.'],
  ['Neck mobility circles 5/dirección', 'Suelo', 'Cuello suave.'],
  ['Wrist mobility circles 10', 'Suelo', 'Muñecas pre-press.'],
  ['Ankle alphabet cada pie', 'Suelo', 'Movilidad tobillo.'],
  ['Toe yoga 2 min', 'Suelo', 'Control pie.'],
  ['Breathing drill box 4-4-4-4', 'Suelo', 'Recuperación parasimpática.'],
  ['Diaphragmatic breathing 5 min', 'Suelo', 'Activación diafragma.'],
  ['Nasal breathing walk 15 min', 'Exterior', 'Capacidad aeróbica nasal.'],
  ['Cold exposure protocol 3 min', '—', 'Recuperación (opcional).'],
  ['Contrast shower recovery', '—', 'Recuperación vascular.'],
  ['Active recovery bike 20 min', 'Bici', 'Z1 muy suave.'],
  ['Active recovery swim 20 min', 'Piscina', 'Nado suave técnica.'],
  ['Deload full body circuit', 'Variable', 'Semana descarga, RPE 5-6.'],
  ['Technique day dominadas', 'Barra fija', 'Series submáximas perfectas.'],
  ['Technique day sentadilla', 'Barra/rack', 'Series 5 con 60 % 1RM.'],
  ['Technique day carrera', 'Pista', 'Drills + strides 4x80 m.'],
  ['Oposición simulation circuit', 'Variable', 'Simula día de pruebas físicas.'],
  ['PN agility circuit mock', 'Conos', 'Simulacro circuito agilidad PN.'],
  ['GC obstacle course prep', 'Variable', 'Preparación pruebas GC.'],
  ['Ruck march 8 km', 'Mochila 12 kg', 'Específico GC resistencia.'],
  ['Ruck march 5 km tempo', 'Mochila 10 kg', 'Ritmo sostenido con carga.'],
  ['Sandbag over shoulder 10x', 'Saco arena', 'Potencia y agarre.'],
  ['Sandbag bear hug carry 40 m', 'Saco arena', 'Core y agarre.'],
  ['Fireman carry 40 m', 'Compañero/saco', 'Carga irregular.'],
  ['Dummy drag 20 m', 'Saco arrastre', 'Específico rescate/oposición.'],
  ['Dummy carry 20 m', 'Saco', 'Transporte carga.'],
  ['Low crawl 20 m x 4', 'Suelo', 'Agilidad baja, oposición.'],
  ['High crawl 20 m x 4', 'Suelo', 'Desplazamiento rápido bajo.'],
  ['Bear crawl 20 m x 4', 'Suelo', 'Core y hombros.'],
  ['Crab walk 20 m x 4', 'Suelo', 'Hombros y cadera.'],
  ['Army crawl under bar 10 m x 6', 'Barra/suelo', 'Simulacro obstáculo.'],
  ['Vault over barrier', 'Valla', 'Técnica salto obstáculo.'],
  ['Wall climb technique', 'Muro', 'Técnica escalada muro oposición.'],
  ['Rope technique climbs 3x', 'Cuerda', 'Técnica S y brasileña.'],
  ['Balance beam walk', 'Viga', 'Propiocepción.'],
  ['Single leg RDL mancuerna', 'Mancuerna', 'Equilibrio y isquios.'],
  ['Single leg glute bridge', 'Suelo', 'Glúteo unilateral.'],
  ['Copenhagen adduction', 'Banco', 'Aductores, prevención lesión.'],
  ['Calf raise unilateral', 'Escalón', 'Pantorrilla unilateral.'],
  ['Tibialis raise', 'Pared', 'Tibial anterior.'],
  ['Toe raise walk 20 m', 'Suelo', 'Tibial y pie.'],
  ['Heel walk 20 m', 'Suelo', 'Tibial anterior.'],
  ['Lateral bound 3x6/lado', 'Suelo', 'Potencia lateral.'],
  ['Single leg hop 3x5/lado', 'Suelo', 'Pliometría unilateral.'],
  ['Single leg box jump 3x4/lado', 'Cajón', 'Potencia pierna.'],
  ['Depth jump 4x3', 'Cajón', 'Reactividad, aterrizaje suave.'],
  ['Hurdle hop 3x6', 'Vallas', 'Rigidez reactiva.'],
  ['Ankle hop 3x15', 'Suelo', 'Tobillo reactivo.'],
  ['Scissor jump 3x8/lado', 'Suelo', 'Alternancia explosiva.'],
  ['Power skip 3x20 m', 'Pista', 'Potencia y técnica.'],
  ['Bound 3x20 m', 'Pista', 'Zancadas explosivas.'],
  ['Acceleration drill 15 m x 6', 'Pista', 'Primera fase sprint.'],
  ['Max velocity fly 30 m', 'Pista', '20 m build + 30 m máximo.'],
  ['Wicket drill 20 m', 'Vallas mini', 'Frecuencia zancada.'],
  ['Straight leg bound 3x20 m', 'Pista', 'Técnica de carrera.'],
  ['Backward sprint 20 m x 4', 'Pista', 'Activación posterior.'],
  ['Shuffle defensive 10 m x 6', 'Pista', 'Desplazamiento lateral rápido.'],
  ['Mirror drill partner 3x30 s', 'Compañero', 'Reacción y agilidad.'],
  ['Reaction ball drops 10', 'Pelota reacción', 'Reflejos.'],
  ['Ball drop sprint 10 m x 6', 'Compañero', 'Salida a estímulo visual.'],
  ['Whistle sprint 10 m x 8', 'Pista', 'Salida a estímulo auditivo.'],
  ['Shadow boxing 3x2 min', 'Suelo', 'Cardio y coordinación.'],
  ['Heavy bag 5x2 min', 'Saco boxeo', 'Potencia y resistencia.'],
  ['Jump rope double unders 5x30', 'Comba', 'Coordinación y cardio.'],
  ['Jump rope crossovers 3x20', 'Comba', 'Habilidad y ritmo.'],
  ['Swimming paddles 8x50 m', 'Piscina/paletas', 'Fuerza en agua.'],
  ['Swimming fins 8x50 m', 'Piscina/aletas', 'Técnica y velocidad.'],
  ['Swimming pull buoy 10x50 m', 'Piscina/boya', 'Foco brazos.'],
  ['Treading water 5 min', 'Piscina', 'Resistencia específica agua.'],
  ['Underwater swim 15 m x 4', 'Piscina', 'Capacidad pulmonar.'],
  ['Eggbeater kick 3 min', 'Piscina', 'Estabilidad vertical agua.'],
  ['Water polo swim 200 m', 'Piscina', 'Resistencia agua.'],
  ['Open water swim 1500 m', 'Agua abierta', 'Específico si aplica.'],
  ['Indoor track intervals', 'Pista cubierta', 'Series sin clima.'],
  ['Treadmill incline 5 % 25 min', 'Cinta', 'Cuesta simulada.'],
  ['Treadmill intervals 1 min on/off', 'Cinta', 'HIIT indoor.'],
  ['Elliptical intervals 30:30', 'Elíptica', 'Bajo impacto HIIT.'],
  ['Stair climber 20 min', 'Máquina escaleras', 'Resistencia pierna.'],
  ['Arc trainer 25 min Z2', 'Arc trainer', 'Cardio bajo impacto.'],
  ['Recumbent bike 30 min', 'Bici reclinada', 'Recuperación activa.'],
  ['Hand bike 15 min', 'Bici manos', 'Alternativa upper body.'],
  ['Rowing sprint 250 m x 8', 'Remo', 'Potencia anaeróbica.'],
  ['Rowing steady 5000 m', 'Remo', 'Resistencia aeróbica.'],
  ['Ski erg sprint 500 m x 4', 'Ski erg', 'Intervalos potencia.'],
  ['Air bike tabata 8 rondas', 'Bici assault', '20 on / 10 off.'],
  ['Circuito opositor 3 rondas', 'Variable', 'Dominadas + flexiones + sprint + core.'],
  ['Circuito fuerza-resistencia 20 min', 'Variable', '6 estaciones, 40 s trabajo.'],
  ['EMOM fuerza 20 min', 'Barra/mancuernas', '1 ejercicio cada minuto.'],
  ['AMRAP fuerza 15 min', 'Variable', 'Ciclos cortos controlados.'],
  ['Complex barbell 5 rondas', 'Barra', 'Deadlift + row + press.'],
  ['Dumbbell complex 5 rondas', 'Mancuernas', 'Snatch + lunge + press.'],
  ['Kettlebell complex 5 rondas', 'Kettlebell', 'Swing + clean + press.'],
  ['Bodyweight EMOM 16 min', 'Suelo', 'Flexiones + sentadillas + burpees.'],
  ['Prison workout circuit', 'Suelo', 'Flexiones + sentadillas + burpees AMRAP.'],
  ['Murph prep half', 'Variable', 'Carrera + dominadas + flexiones reducido.'],
  ['Cindy AMRAP 20 min', 'Barra/suelo', 'Dominadas + flexiones + sentadillas.'],
  ['Helen scaled', 'Variable', 'Carrera + kettlebell + dominadas.'],
  ['Fran scaled', 'Barra/cajón', 'Thrusters + dominadas asistidas.'],
  ['Grace scaled', 'Barra', 'Clean and jerk técnica.'],
  ['Isabel scaled', 'Barra', 'Snatch técnica con peso moderado.'],
  ['Diane scaled', 'Barra/cajón', 'Deadlift + HSPU progresión.'],
  ['Jackie scaled', 'Remo/barra', 'Row + thruster + pull-up.'],
  ['Annie double unders', 'Comba/suelo', 'Saltos + sit-ups.'],
  ['Karen wall balls', 'Balón', '150 wall balls progresivo.'],
  ['Nutts hero WOD scaled', 'Anillas/barra', 'Variante hero scaled.'],
  ['Oposición prep WOD 1', 'Variable', 'Circuito específico PN básico.'],
  ['Oposición prep WOD 2', 'Variable', 'Circuito específico GC básico.'],
  ['Oposición prep WOD 3', 'Variable', 'Circuito mixto avanzado.'],
  ['Deload week opción A', 'Variable', 'Volumen -40 %, técnica.'],
  ['Deload week opción B', 'Variable', 'Solo movilidad y Z1-Z2.'],
  ['Taper carrera 3 días', 'Pista', 'Volumen bajo, intensidad mantenida.'],
  ['Taper fuerza 5 días', 'Gym', 'Intensidad alta, volumen bajo.'],
  ['Peak week simulation', 'Variable', 'Simulacro completo pre-examen.']
];
rellenoGym.forEach(([n, eq, ins]) => {
  const pil = /carrera|trote|row|bike|swim|cardio|HIIT|interval|spinning|erg|climber|recumbent|nasal|recovery|treadmill|elliptical|stair|arc|air bike|tabata|murph|cindy|helen|fran|grace|isabel|diane|jackie|annie|karen|wod|ruck|march|walk 15|walk 30/i.test(n)
    ? 'RESISTENCIA'
    : /sprint|agility|agilidad|pliometr|jump|bound|hop|skip|drill|reaction|shuffle|velocity|wicket|fly |acceleration|CMJ|SJ |drop jump|box jump|salto|valla|cono|escalera|obstacle|vault|crawl|mirror/i.test(n)
      ? 'VELOCIDAD'
      : /movilidad|stretch|foam|yoga|mobility|breathing|breath|twist|pose|RAMP|estiramiento|deload week opción B/i.test(n)
        ? 'MOVILIDAD'
        : /plancha|plank|core|hollow|dead bug|bird dog|ab wheel|crunch|pallof|turkish|v-up|bicycle|woodchop|bear crawl|mountain climber/i.test(n)
          ? 'CORE'
          : 'FUERZA';
  const grupo = pil === 'RESISTENCIA' ? 'Cardio' : pil === 'VELOCIDAD' ? (n.includes('agil') || n.includes('Agil') || n.includes('cono') || n.includes('obstacle') ? 'Agilidad' : n.includes('jump') || n.includes('hop') || n.includes('bound') ? 'Pliometría' : 'Velocidad') : pil === 'MOVILIDAD' ? 'Movilidad' : pil === 'CORE' ? 'Core' : 'General';
  const il = pil === 'RESISTENCIA' ? 'RUN' : pil === 'VELOCIDAD' ? 'AGILITY' : pil === 'MOVILIDAD' ? 'MOBILITY' : pil === 'CORE' ? 'PLANK' : n.match(/press|push|dip|tricep|bench|fly|crossover/i) ? 'PUSH' : n.match(/row|pull|lat|dominad|remo|shrug/i) ? 'PULL' : n.match(/squat|lunge|deadlift|hip|leg|glute|calf|step/i) ? 'SQUAT' : 'GENERAL';
  add(n, pil, grupo, eq, ins, ENTORNOS.mix, il);
});

// Asegurar exactamente 500 (o más, luego recortar)
while (lista.length < 500) {
  const i = lista.length + 1;
  add(
    `Ejercicio auxiliar oposición #${i}`,
    'FUERZA',
    'General',
    'Variable',
    'Ejercicio complementario del banco OpoFit.',
    ENTORNOS.mix,
    'GENERAL'
  );
}

const final = lista.slice(0, 500);
const outPath = path.join(__dirname, '../data/ejercicios-banco-500.json');
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, JSON.stringify({ version: 1, total: final.length, ejercicios: final }, null, 2), 'utf8');
console.log(`Generados ${final.length} ejercicios → ${outPath}`);
