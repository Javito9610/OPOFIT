/**
 * Ejercicios extra por entorno (casa, calistenia, crossfit) + metadatos de ilustración.
 * Formato: [nombre, pilar, grupo, equipamiento, instrucciones, entornos, tipo_ilustracion]
 */
const db = require('../config/db');
const EntornoEntreno = require('../utils/EntornoEntreno');

const EJERCICIOS_ENTORNO = [
  ['Flexiones estándar', 'FUERZA', 'Pecho', 'Suelo', 'Cuerpo recto, pecho al suelo.', 'CASA,CALISTENIA,MIXTO', 'PUSH'],
  ['Flexiones inclinadas (mesa)', 'FUERZA', 'Pecho', 'Mesa', 'Manos en superficie elevada.', 'CASA,MIXTO', 'PUSH'],
  ['Flexiones pike', 'FUERZA', 'Hombros', 'Suelo', 'Cadera alta, cabeza hacia el suelo.', 'CASA,CALISTENIA,MIXTO', 'PUSH'],
  ['Fondos en silla', 'FUERZA', 'Tríceps', 'Silla', 'Manos en borde, codos atrás.', 'CASA,MIXTO', 'PUSH'],
  ['Sentadilla con mochila', 'FUERZA', 'Pierna', 'Mochila', 'Peso en espalda, profundidad controlada.', 'CASA,MIXTO', 'SQUAT'],
  ['Sentadilla búlgara (silla)', 'FUERZA', 'Pierna', 'Silla', 'Pie trasero elevado.', 'CASA,MIXTO', 'SQUAT'],
  ['Zancadas en el sitio', 'FUERZA', 'Pierna', 'Suelo', 'Alternar piernas sin desplazarte.', 'CASA,CALISTENIA,MIXTO', 'SQUAT'],
  ['Puente de glúteo', 'FUERZA', 'Glúteo', 'Suelo', 'Empuje de cadera, pausa arriba.', 'CASA,MIXTO', 'SQUAT'],
  ['Peso muerto con mochila', 'FUERZA', 'Cadena posterior', 'Mochila', 'Bisagra de cadera con carga.', 'CASA,MIXTO', 'SQUAT'],
  ['Remo con mochila', 'FUERZA', 'Espalda', 'Mochila', 'Bisagra, tirón al abdomen.', 'CASA,MIXTO', 'PULL'],
  ['Remo invertido (mesa)', 'FUERZA', 'Espalda', 'Mesa', 'Cuerpo bajo mesa, tirón al pecho.', 'CASA,MIXTO', 'PULL'],
  ['Superman hold', 'FUERZA', 'Espalda', 'Suelo', 'Brazos y piernas elevados.', 'CASA,MIXTO', 'PULL'],
  ['Plancha con elevación de pierna', 'CORE', 'Core', 'Suelo', 'Alternar elevaciones sin rotar cadera.', 'CASA,CALISTENIA,MIXTO', 'PLANK'],
  ['Mountain climbers', 'CORE', 'Core', 'Suelo', 'Ritmo controlado, cadera baja.', 'CASA,CALISTENIA,MIXTO', 'PLANK'],
  ['Dead bug', 'CORE', 'Core', 'Suelo', 'Lumbar pegada, movimiento contralateral.', 'CASA,MIXTO', 'PLANK'],
  ['Burpees', 'RESISTENCIA', 'Cardio', 'Suelo', 'Flexión + salto, técnica limpia.', 'CASA,CROSSFIT,CALISTENIA,MIXTO', 'RUN'],
  ['Jumping jacks', 'RESISTENCIA', 'Cardio', 'Suelo', 'Calentamiento dinámico.', 'CASA,MIXTO', 'RUN'],
  ['Saltos al cajón (escalón)', 'VELOCIDAD', 'Pliometría', 'Escalón', 'Usa escalón de 30-40 cm.', 'CASA,CROSSFIT,MIXTO', 'AGILITY'],
  ['Sprint en el sitio', 'VELOCIDAD', 'Velocidad', 'Suelo', 'Rodillas altas 20 s x 6.', 'CASA,MIXTO', 'AGILITY'],
  ['Skipping en el sitio', 'VELOCIDAD', 'Velocidad', 'Suelo', 'Activación A/B sin desplazamiento.', 'CASA,PISTA,MIXTO', 'AGILITY'],
  ['Carrera en el sitio Z2 15 min', 'RESISTENCIA', 'Cardio', 'Suelo', 'Rodaje suave indoor.', 'CASA,MIXTO', 'RUN'],
  ['Escaleras 10 min', 'RESISTENCIA', 'Cardio', 'Escalera', 'Subidas controladas.', 'CASA,PISTA,MIXTO', 'RUN'],
  ['Banda elástica press pecho', 'FUERZA', 'Pecho', 'Banda', 'Anclaje a puerta o espalda.', 'CASA,MIXTO', 'PUSH'],
  ['Banda elástica remo', 'FUERZA', 'Espalda', 'Banda', 'Tirón al abdomen, escápulas juntas.', 'CASA,MIXTO', 'PULL'],
  ['Banda elástica sentadilla', 'FUERZA', 'Pierna', 'Banda', 'Banda bajo pies, resistencia al subir.', 'CASA,MIXTO', 'SQUAT'],
  ['Banda elástica face pull', 'FUERZA', 'Hombros', 'Banda', 'Codos altos, rotación externa.', 'CASA,GYM,MIXTO', 'PULL'],
  ['Muscle-up progresión (negativa)', 'FUERZA', 'Espalda', 'Barra fija', 'Descenso controlado desde barra alta.', 'CALISTENIA,MIXTO', 'PULL'],
  ['L-sit tuck (barra)', 'FUERZA', 'Core', 'Barra fija', 'Rodillas al pecho, isometría.', 'CALISTENIA,MIXTO', 'PLANK'],
  ['Front lever tuck', 'FUERZA', 'Espalda', 'Barra fija', 'Cuerpo horizontal, rodillas al pecho.', 'CALISTENIA,MIXTO', 'PULL'],
  ['Muscle snatch con escoba', 'FUERZA', 'Cuerpo completo', 'Barra/escoba', 'Técnica olímpica ligera.', 'CASA,CROSSFIT,MIXTO', 'GENERAL'],
  ['Wall ball (balón)', 'FUERZA', 'Pierna', 'Balón 3-5 kg', 'Sentadilla + lanzamiento a pared.', 'CROSSFIT,GYM,MIXTO', 'SQUAT'],
  ['Box jump (cajón)', 'VELOCIDAD', 'Pliometría', 'Cajón', 'Salto explosivo, bajada controlada.', 'CROSSFIT,GYM,MIXTO', 'AGILITY'],
  ['Devil press mancuernas', 'FUERZA', 'Cuerpo completo', 'Mancuernas', 'Burpee + snatch overhead.', 'CROSSFIT,GYM,MIXTO', 'GENERAL'],
  ['Assault bike 10 min Z2', 'RESISTENCIA', 'Cardio', 'Bici', 'Rodaje bajo impacto en box.', 'CROSSFIT,GYM,MIXTO', 'RUN'],
  ['Row ergómetro 2000 m', 'RESISTENCIA', 'Cardio', 'Remo', 'Ritmo umbral sostenido.', 'CROSSFIT,GYM,MIXTO', 'RUN'],
  ['Farmer carry kettlebells', 'FUERZA', 'Agarre/Core', 'Kettlebell', 'Caminata con peso, hombros cuadrados.', 'CROSSFIT,GYM,CASA,MIXTO', 'GENERAL'],
  ['Caminata rápida 30 min', 'RESISTENCIA', 'Cardio', '—', 'RPE 4-5, alternativa sin pista.', 'CASA,PISTA,MIXTO', 'RUN'],
  ['Trote suave 20 min', 'RESISTENCIA', 'Cardio', 'Pista', 'Rodaje aeróbico base.', 'PISTA,MIXTO', 'RUN'],
  ['Trote suave 35 min', 'RESISTENCIA', 'Cardio', 'Pista', 'Rodaje oposición PN/GC.', 'PISTA,MIXTO', 'RUN'],
  ['Series 100 m x 8', 'VELOCIDAD', 'Velocidad', 'Pista', 'RPE 8, recuperación caminando.', 'PISTA,MIXTO', 'AGILITY'],
  ['Tempo run 15 min', 'RESISTENCIA', 'Cardio', 'Pista', 'Ritmo controlado umbral bajo.', 'PISTA,MIXTO', 'RUN'],
  ['Agilidad con conos caseros', 'VELOCIDAD', 'Agilidad', 'Conos', 'Usa botellas o conos improvisados.', 'PISTA,CASA,MIXTO', 'AGILITY'],
  ['Pull-over con toalla en barra', 'FUERZA', 'Espalda', 'Barra fija', 'Toalla sobre barra, tirón controlado.', 'CALISTENIA,MIXTO', 'PULL'],
  ['Dips en paralelas', 'FUERZA', 'Pecho/Tríceps', 'Paralelas', 'Descenso 90°, hombros abajo.', 'CALISTENIA,CROSSFIT,MIXTO', 'PUSH'],
  ['Handstand push-up progresión', 'FUERZA', 'Hombros', 'Pared', 'Pies en pared, flexión vertical.', 'CALISTENIA,CASA,MIXTO', 'PUSH'],
  ['Pistol squat asistido', 'FUERZA', 'Pierna', 'Barra fija', 'Agarre barra para equilibrio.', 'CALISTENIA,GYM,MIXTO', 'SQUAT'],
  ['Curl con botellas de agua', 'FUERZA', 'Bíceps', 'Mochila', 'Peso improvisado, sin balanceo.', 'CASA,MIXTO', 'GENERAL'],
  ['Extensiones tríceps con banda', 'FUERZA', 'Tríceps', 'Banda', 'Codos fijos, extensión completa.', 'CASA,MIXTO', 'PUSH'],
  ['Yoga flow movilidad 10 min', 'MOVILIDAD', 'Movilidad', 'Suelo', 'Flujo suave cadera y hombro.', 'CASA,CALISTENIA,MIXTO', 'MOBILITY'],
  ['Press banca con mancuernas', 'FUERZA', 'Pecho', 'Mancuernas', 'Banco plano, codos 45°, control en el descenso.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['Remo con mancuerna unilateral', 'FUERZA', 'Espalda', 'Mancuerna', 'Apoyo en banco, tirón al cadera.', 'GYM,CASA,MIXTO', 'PULL'],
  ['Prensa de piernas', 'FUERZA', 'Pierna', 'Máquina', 'Pies media anchura, profundidad sin despegar lumbar.', 'GYM,MIXTO', 'SQUAT'],
  ['Jalón al pecho', 'FUERZA', 'Espalda', 'Polea', 'Tirón al esternón, escápulas abajo.', 'GYM,MIXTO', 'PULL'],
  ['Elevaciones laterales', 'FUERZA', 'Hombros', 'Mancuernas', 'Codos ligeramente flexionados, sin balanceo.', 'GYM,CASA,MIXTO', 'PUSH'],
  ['Curl martillo', 'FUERZA', 'Bíceps', 'Mancuernas', 'Palmas neutras, subida controlada.', 'GYM,CASA,MIXTO', 'GENERAL'],
  ['Patada de tríceps en polea', 'FUERZA', 'Tríceps', 'Polea', 'Codos pegados, extensión completa.', 'GYM,MIXTO', 'PUSH'],
  ['Hip thrust en banco', 'FUERZA', 'Glúteo', 'Banco', 'Espalda alta en banco, empuje de cadera.', 'GYM,CASA,MIXTO', 'SQUAT'],
  ['Caminata en cinta 25 min', 'RESISTENCIA', 'Cardio', 'Cinta', 'RPE 4-5, postura erguida.', 'GYM,CASA,MIXTO', 'RUN'],
  ['Bici estática 30 min Z2', 'RESISTENCIA', 'Cardio', 'Bici', 'Cadencia 80-90 rpm, conversación posible.', 'GYM,CASA,MIXTO', 'RUN'],
  ['Elíptica 20 min', 'RESISTENCIA', 'Cardio', 'Elíptica', 'Brazos y piernas coordinados, ritmo constante.', 'GYM,MIXTO', 'RUN'],
  ['Natación técnica 800 m', 'RESISTENCIA', 'Cardio', 'Piscina', 'Series de 100 m con descanso 20 s.', 'GYM,PISTA,MIXTO', 'RUN'],
  ['Drill skipping A/B 6 x 30 m', 'VELOCIDAD', 'Velocidad', 'Pista', 'Rodillas altas y talones al glúteo alternados.', 'PISTA,MIXTO', 'AGILITY'],
  ['Vallas bajas 5 x 6', 'VELOCIDAD', 'Agilidad', 'Vallas', 'Ritmo fluido, sin frenar entre vallas.', 'PISTA,MIXTO', 'AGILITY'],
  ['Propiocepción unipodal 3 x 45 s', 'VELOCIDAD', 'Agilidad', 'Suelo', 'Equilibrio sobre una pierna, rodilla blanda.', 'CASA,PISTA,MIXTO', 'AGILITY'],
  ['Thruster con mancuernas', 'FUERZA', 'Cuerpo completo', 'Mancuernas', 'Sentadilla frontal + press overhead.', 'CROSSFIT,GYM,CASA,MIXTO', 'GENERAL'],
  ['Swing con kettlebell', 'FUERZA', 'Cadena posterior', 'Kettlebell', 'Bisagra explosiva, brazos relajados.', 'CROSSFIT,GYM,CASA,MIXTO', 'SQUAT'],
  ['Battle rope 8 x 30 s', 'RESISTENCIA', 'Cardio', 'Cuerda', 'Ondas alternas, core activo.', 'CROSSFIT,GYM,MIXTO', 'RUN'],
  ['Sled push 4 x 20 m', 'VELOCIDAD', 'Pliometría', 'Trineo', 'Empuje bajo, pasos cortos y potentes.', 'CROSSFIT,GYM,MIXTO', 'AGILITY'],
  ['Dominadas asistidas con banda', 'FUERZA', 'Espalda', 'Banda', 'Banda bajo rodillas o pies, pecho a la barra.', 'GYM,CALISTENIA,MIXTO', 'PULL'],
  ['Face pull con banda', 'FUERZA', 'Hombros', 'Banda', 'Tirón a la cara, rotación externa.', 'CASA,GYM,MIXTO', 'PULL'],
  ['Estiramiento isquios 2 x 45 s', 'MOVILIDAD', 'Movilidad', 'Suelo', 'Sin rebotes, respiración lenta.', 'CASA,GYM,PISTA,MIXTO', 'MOBILITY'],
  ['Foam roller cuádriceps 2 min', 'MOVILIDAD', 'Movilidad', 'Rodillo', 'Presión moderada, pausas en puntos tensos.', 'GYM,CASA,MIXTO', 'MOBILITY']
];

class EjerciciosEntornoCatalogo {
  static async seedEjerciciosEntorno() {
    for (const [nombre, pilar, grupo, equip, instr, entornos, ilust] of EJERCICIOS_ENTORNO) {
      const [exists] = await db.query('SELECT id_ejercicio FROM ejercicios WHERE nombre = ? LIMIT 1', [nombre]);
      const cat = pilar === 'MOVILIDAD' ? 'Movilidad' : pilar === 'CORE' ? 'Core' : pilar === 'RESISTENCIA' ? 'Cardio' : 'Fuerza';
      if (exists.length) {
        await db.query(
          `UPDATE ejercicios SET entornos = COALESCE(entornos, ?), tipo_ilustracion = COALESCE(tipo_ilustracion, ?),
           equipamiento = COALESCE(equipamiento, ?), instrucciones_tecnicas = COALESCE(instrucciones_tecnicas, ?)
           WHERE id_ejercicio = ?`,
          [entornos, ilust, equip, instr, exists[0].id_ejercicio]
        );
        continue;
      }
      await db.query(
        `INSERT INTO ejercicios (nombre, video_url, instrucciones_tecnicas, categoria, pilar, grupo_muscular, equipamiento, entornos, tipo_ilustracion)
         VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?)`,
        [nombre, instr, cat, pilar, grupo, equip, entornos, ilust]
      );
    }
    console.log('[ejercicios] Catálogo por entorno actualizado');
  }

  static async seedMetadatosExistentes() {
    const [rows] = await db.query(
      'SELECT id_ejercicio, nombre, pilar, grupo_muscular, equipamiento, entornos, tipo_ilustracion FROM ejercicios'
    );
    for (const e of rows) {
      const entornos =
        e.entornos ||
        EntornoEntreno.inferirEntornosDesdeEquipamiento(e.equipamiento, e.pilar).join(',');
      const ilust =
        e.tipo_ilustracion ||
        EntornoEntreno.inferirTipoIlustracion(e.nombre, e.pilar, e.grupo_muscular);
      await db.query('UPDATE ejercicios SET entornos = ?, tipo_ilustracion = ? WHERE id_ejercicio = ?', [
        entornos,
        ilust,
        e.id_ejercicio
      ]);
    }
    console.log('[ejercicios] Metadatos entorno/ilustración aplicados a catálogo');
  }
}

module.exports = EjerciciosEntornoCatalogo;
