#!/usr/bin/env node
/**
 * Segunda ampliación del banco de ejercicios (v2).
 *
 * Áreas que se refuerzan:
 *   - Específicos por oposición (Policía, GC, Bomberos, Ejército).
 *   - Cardio variado (natación, remo, escalera, elíptica, asalto bike).
 *   - Funcionales tipo CrossFit (WODs).
 *   - Aductores y rotadores externos (prevención lesiones).
 *   - Estiramientos por grupo muscular.
 *   - Specific drills atletismo (vallas, salidas tacos, pasos cortos).
 *   - Específicos para natación (50m libres, mariposa, virajes).
 *   - Específicos para circuitos agilidad (test Barrow).
 *
 * Total añadidos: ~150 ejercicios con criterio profesional.
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
    nombre, pilar, grupo_muscular: grupo, equipamiento: equip,
    entornos: entornos.join(','), tipo_ilustracion: ilust,
    instrucciones_tecnicas: instr || null
  });
  return true;
}

// ============ ATLETISMO ESPECÍFICO (drills + técnica) ============
const ATLETISMO = [
  ['Skipping rodillas altas en sitio rápido', 'Velocidad', '—', ['PISTA','CASA','MIXTO'], 'RUN', 'En sitio, rodillas a la cintura, tempo máximo. 4×15s con 30s descanso. Activación neural.'],
  ['Skipping con avance', 'Velocidad', '—', ['PISTA','MIXTO'], 'RUN', 'Avanzando 20m, rodillas altas. Foco técnica de zancada. 4 series.'],
  ['Talones al glúteo en sitio', 'Velocidad', '—', ['PISTA','CASA','MIXTO'], 'RUN', 'En sitio, tocar talón con glúteo alternando. 30s tempo alto. 4 series.'],
  ['Pasos cortos rápidos', 'Velocidad', '—', ['PISTA','MIXTO'], 'RUN', 'Pasos muy cortos y rápidos durante 20m. Foco frecuencia. 4 series.'],
  ['Salidas desde tacos', 'Velocidad', 'Tacos salida', ['PISTA','MIXTO'], 'RUN', 'Salida explosiva desde tacos hasta los 30m. 5 series con 2 min descanso.'],
  ['Salidas tumbado boca abajo', 'Velocidad', '—', ['PISTA','CASA','MIXTO'], 'RUN', 'Tumbado boca abajo. A señal levanta y sprint 20m. Mide reacción.'],
  ['Salidas desde sentado', 'Velocidad', '—', ['PISTA','MIXTO'], 'RUN', 'Sentado, a señal sprint 30m. Trabaja reactividad y aceleración.'],
  ['Salidas con cambio de dirección', 'Velocidad', 'Conos', ['PISTA','MIXTO'], 'RUN', 'Sprint 10m, frenada con squat, vuelta sprint. 6 series.'],
  ['Sprint en cuesta 30m', 'Velocidad', '—', ['PISTA','MIXTO'], 'RUN', 'Sprint 30m en cuesta 6-8%. 6 series con 90s descanso. Potencia muscular.'],
  ['Sprint en cuesta abajo', 'Velocidad', '—', ['PISTA','MIXTO'], 'RUN', 'Cuesta 3-5% bajada. Sprint 30m. Mejora frecuencia de zancada. 5 series.'],
  ['Carrera con paracaídas', 'Velocidad', 'Paracaídas', ['PISTA','MIXTO'], 'RUN', 'Sprint 30m con paracaídas. Sobrecarga resistencia. 6 series con 2 min descanso.'],
  ['Carrera con trineo lastrado', 'Velocidad', 'Trineo', ['PISTA','CROSSFIT','MIXTO'], 'RUN', 'Arrastra trineo con peso (10-15% peso corporal) durante 20-30m. 5 series.'],
  ['Series 100m al 90%', 'Resistencia', '—', ['PISTA','MIXTO'], 'RUN', '6×100m al 90% con 90s descanso. Trabajo de tolerancia láctica.'],
  ['Series 200m al 80%', 'Resistencia', '—', ['PISTA','MIXTO'], 'RUN', '6×200m al 80% con 2 min descanso. Capacidad anaeróbica.'],
  ['Series 400m al ritmo objetivo', 'Resistencia', '—', ['PISTA','MIXTO'], 'RUN', '5×400m al ritmo objetivo de 1000m. 2 min descanso. VO2max.'],
  ['Series 600m', 'Resistencia', '—', ['PISTA','MIXTO'], 'RUN', '5×600m a ritmo 2000m. Trabajo umbral. 2-3 min descanso.'],
  ['Series 800m a ritmo objetivo', 'Resistencia', '—', ['PISTA','MIXTO'], 'RUN', '4×800m a ritmo 2000m objetivo. 2 min descanso. Resistencia específica.'],
  ['Series 1000m', 'Resistencia', '—', ['PISTA','MIXTO'], 'RUN', '4×1000m al 95% VO2max (Helgerud). 90s descanso. Para mejorar VO2max.'],
  ['Series 2000m', 'Resistencia', '—', ['PISTA','MIXTO'], 'RUN', '3×2000m a ritmo de competición de 5km. 3 min descanso. Capacidad aeróbica.'],
  ['Test 1000m máximo', 'Resistencia', '—', ['PISTA','MIXTO'], 'RUN', 'Solo. Calienta bien, 1000m al máximo. Anota tiempo cada 2 semanas.'],
  ['Test 2000m máximo', 'Resistencia', '—', ['PISTA','MIXTO'], 'RUN', 'Solo. 2000m al máximo. Test GC clásico. Tiempo de referencia.'],
  ['Fartlek 30-30 corto', 'Resistencia', '—', ['PISTA','CASA','MIXTO'], 'RUN', '30s rápido / 30s suave durante 15 min. Mejora VO2max sin tanto desgaste.'],
  ['Fartlek 1-1 medio', 'Resistencia', '—', ['PISTA','MIXTO'], 'RUN', '1 min rápido / 1 min suave durante 20 min. Tolerancia láctica.'],
  ['Fartlek 2-1', 'Resistencia', '—', ['PISTA','MIXTO'], 'RUN', '2 min rápido / 1 min suave. 6-8 reps. Resistencia específica.'],
  ['Fartlek piramidal', 'Resistencia', '—', ['PISTA','MIXTO'], 'RUN', '1-2-3-4-3-2-1 min al 90% con 1 min suave entre. Trabajo completo.']
];
ATLETISMO.forEach(([n,g,eq,ent,il,ins]) => add(n,'VELOCIDAD',g,eq,ent,il,ins));

// ============ NATACIÓN (específica para GC, PN, Bomberos, Armada) ============
const NATACION = [
  ['Crol 200m suave técnico', 'Resistencia', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', 'Crol 200m suave, foco posición horizontal y respiración bilateral.'],
  ['Crol 50m al máximo', 'Velocidad', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', '50m crol al 95%. Trabajo específico oposición. 6 series con 1 min descanso.'],
  ['Crol 100m al ritmo objetivo', 'Resistencia', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', '100m crol al ritmo objetivo de 50m+5seg. 5 series con 1 min descanso.'],
  ['Crol 200m progresivo', 'Resistencia', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', '200m crol empezando suave terminando al 90%. 4 series con 2 min descanso.'],
  ['Crol 400m continuo', 'Resistencia', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', '400m crol técnico continuo. Foco velocidad de viraje y respiración.'],
  ['Series 4x50m crol', 'Resistencia', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', '4×50m al 90% con 30s descanso. Salidas de pared cronometradas.'],
  ['Series 8x50m crol', 'Resistencia', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', '8×50m al 85% con 30s descanso. Resistencia específica.'],
  ['Series 6x100m crol', 'Resistencia', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', '6×100m al 85% con 30s descanso. Capacidad aeróbica.'],
  ['Series 4x200m crol', 'Resistencia', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', '4×200m al 80% con 1 min descanso. Resistencia aeróbica.'],
  ['Buceo dinámico 25m sin respirar', 'Resistencia', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', 'Buceo crol horizontal 25m. Trabajo apnea + técnica. Crucial Foral.'],
  ['Buceo 10m + 40m crol', 'Resistencia', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', '10m buceo dinámico salida + 40m crol. Específico prueba Foral.'],
  ['Apnea estática 30s', 'Resistencia', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', 'Pulmones llenos, cabeza dentro, 30s. Trabajo CO2 tolerance. Progresar.'],
  ['Apnea estática progresiva', 'Resistencia', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', '30-45-60-45-30s con 1 min descanso. Adaptación.'],
  ['Patada de espalda con tabla', 'Resistencia', 'Tabla natación', ['PISTA','GYM','MIXTO'], 'RUN', 'De espalda con tabla en pecho. 4×50m. Trabajo de piernas + posición.'],
  ['Brazada solo (pull buoy)', 'Resistencia', 'Pull buoy', ['PISTA','GYM','MIXTO'], 'RUN', '4×100m solo brazada con pull buoy. Foco técnica de brazada.'],
  ['Patada solo (tabla)', 'Resistencia', 'Tabla natación', ['PISTA','GYM','MIXTO'], 'RUN', '4×50m solo patada con tabla. Foco propulsión piernas.'],
  ['Series virajes', 'Velocidad', 'Piscina', ['PISTA','GYM','MIXTO'], 'RUN', '10 virajes encadenados con 15m crol. Foco salida pared explosiva.']
];
NATACION.forEach(([n,g,eq,ent,il,ins]) => add(n,'RESISTENCIA',g,eq,ent,il,ins));

// ============ ESPECÍFICOS BOMBEROS (trepa cuerda + circuito) ============
const BOMBEROS = [
  ['Trepa de cuerda 5m con piernas', 'Fuerza', 'Cuerda', ['CROSSFIT','GYM','PISTA','MIXTO'], 'PULL', 'Trepa cuerda usando técnica J-hook con piernas. 4×3 reps. Específico bomberos.'],
  ['Trepa de cuerda sin piernas', 'Fuerza', 'Cuerda', ['CROSSFIT','GYM','MIXTO'], 'PULL', 'Trepa solo brazos. Avanzado. 3×2 reps. Máximo trabajo dorsal.'],
  ['Descenso cuerda controlado', 'Fuerza', 'Cuerda', ['CROSSFIT','GYM','MIXTO'], 'PULL', 'Bajada lenta de cuerda con técnica. 3×3 reps. Excéntrico de espalda.'],
  ['Transporte maniquí', 'Fuerza', 'Maniquí', ['CROSSFIT','GYM','MIXTO'], 'SQUAT', 'Transporta maniquí 30kg durante 20m. 5 series. Específico rescate bombero.'],
  ['Transporte saco arena', 'Fuerza', 'Saco', ['CROSSFIT','GYM','PISTA','MIXTO'], 'SQUAT', 'Saco 25kg al hombro, transporta 30m. 5 series. Cuerpos de seguridad.'],
  ['Press de banca + dominada bomberos', 'Fuerza', 'Barra', ['GYM','MIXTO'], 'PUSH', 'Press 12 reps al 50% peso corporal + dominada máxima. 5 series. Circuito.'],
  ['Subida con bombona', 'Fuerza', 'Bombona/Mochila', ['CROSSFIT','GYM','PISTA','MIXTO'], 'SQUAT', 'Subir escaleras con mochila 15kg. 5 pisos × 5 reps. Específico bombero.'],
  ['Squat con bombona pesada', 'Fuerza', 'Bombona', ['CROSSFIT','GYM','MIXTO'], 'SQUAT', 'Sentadilla con bombona/saco 20-25kg al pecho. 4×8. Específico bombero.'],
  ['Pase manguera', 'Velocidad', 'Manguera', ['CROSSFIT','PISTA','MIXTO'], 'RUN', 'Despliega manguera 20m a velocidad. Específico bombero.'],
  ['Escalada palanca/escalera', 'Fuerza', 'Escalera', ['CROSSFIT','PISTA','MIXTO'], 'PULL', 'Sube escalera 3m con manos. Trabajo agarre + brazos.']
];
BOMBEROS.forEach(([n,g,eq,ent,il,ins]) => add(n,'FUERZA',g,eq,ent,il,ins));

// ============ FUNCIONALES TIPO CROSSFIT ============
const CROSSFIT = [
  ['Burpee box jump', 'Velocidad', 'Cajón', ['CROSSFIT','GYM','MIXTO'], 'JUMP', 'Burpee + salto al cajón. Continuo. 5×6 reps.'],
  ['Burpee pull-up', 'Fuerza', 'Barra fija', ['CROSSFIT','CALISTENIA','MIXTO'], 'PULL', 'Burpee + dominada estricta. Continuo. 5×5 reps.'],
  ['Burpee tuck jump', 'Velocidad', '—', ['CROSSFIT','CASA','MIXTO'], 'JUMP', 'Burpee + tuck jump rodillas al pecho. Continuo. 5×8 reps.'],
  ['Thruster', 'Fuerza', 'Mancuerna', ['CROSSFIT','GYM','MIXTO'], 'SQUAT', 'Front squat + push press explosivo encadenado. 5×8. Compuesto rey CrossFit.'],
  ['Thruster con barra', 'Fuerza', 'Barra', ['CROSSFIT','GYM','MIXTO'], 'SQUAT', 'Front squat + push press con barra. 5×5 al 70%. Específico CrossFit.'],
  ['Wallball', 'Resistencia', 'Balón medicinal', ['CROSSFIT','CASA','MIXTO'], 'SQUAT', 'Sentadilla + lanzamiento balón medicinal arriba contra diana. 5×10 reps.'],
  ['Box step-up con peso', 'Fuerza', 'Cajón + mancuerna', ['CROSSFIT','GYM','MIXTO'], 'SQUAT', 'Step-up al cajón con mancuernas. 4×10/pierna.'],
  ['Devil press', 'Fuerza', 'Mancuerna', ['CROSSFIT','GYM','MIXTO'], 'PUSH', 'Burpee con mancuerna + snatch hasta arriba. 5×6 reps. Compuesto.'],
  ['Manhattan WOD', 'Resistencia', 'Variado', ['CROSSFIT','MIXTO'], 'GENERAL', 'AMRAP 20 min: 400m run + 21 KB swing + 12 pull-up. Test resistencia.'],
  ['Fran WOD', 'Resistencia', 'Barra fija', ['CROSSFIT','GYM','MIXTO'], 'GENERAL', '21-15-9 thrusters + pull-ups. Para tiempo. WOD clásico CrossFit.'],
  ['Cindy WOD', 'Resistencia', '—', ['CROSSFIT','CALISTENIA','MIXTO'], 'GENERAL', 'AMRAP 20 min: 5 pull-up + 10 push-up + 15 air squat. Resistencia.'],
  ['Murph WOD', 'Resistencia', 'Variado', ['CROSSFIT','MIXTO'], 'GENERAL', '1mi run + 100 pull-up + 200 push-up + 300 squat + 1mi run. WOD heroico.'],
  ['Toes to bar', 'Core', 'Barra fija', ['CROSSFIT','CALISTENIA','MIXTO'], 'PULL', 'Cuelgues de barra. Llevar pies al barra. 4×8. Core + tracción.'],
  ['Knees to elbows', 'Core', 'Barra fija', ['CROSSFIT','CALISTENIA','MIXTO'], 'PULL', 'Cuelgues. Llevar rodillas a codos. 4×10. Más fácil que toes to bar.'],
  ['Kettlebell swing ruso', 'Fuerza', 'Kettlebell', ['CROSSFIT','GYM','CASA','MIXTO'], 'SQUAT', 'Swing hasta horizontal. 4×15. Foco caderas, no brazos.'],
  ['Kettlebell swing americano', 'Fuerza', 'Kettlebell', ['CROSSFIT','GYM','MIXTO'], 'SQUAT', 'Swing hasta sobre cabeza. 4×12. Más exigente. Olímpico CrossFit.'],
  ['Kettlebell snatch', 'Fuerza', 'Kettlebell', ['CROSSFIT','GYM','MIXTO'], 'PUSH', 'Snatch KB de suelo a arriba en un movimiento. 4×6/brazo.'],
  ['Kettlebell clean', 'Fuerza', 'Kettlebell', ['CROSSFIT','GYM','MIXTO'], 'PULL', 'Clean KB al rack position. 4×8/brazo. Trabajo de potencia.'],
  ['Push press', 'Fuerza', 'Barra', ['CROSSFIT','GYM','MIXTO'], 'PUSH', 'Push press con barra: cuarto squat + push militar. 5×5 al 75%.'],
  ['Power clean', 'Fuerza', 'Barra', ['CROSSFIT','GYM','MIXTO'], 'PULL', 'Clean explosivo a media sentadilla. 5×3 al 80%. Olímpico.'],
  ['Hang power clean', 'Fuerza', 'Barra', ['CROSSFIT','GYM','MIXTO'], 'PULL', 'Clean desde el muslo (no del suelo). 5×3. Técnica olímpica.'],
  ['Power snatch', 'Fuerza', 'Barra', ['CROSSFIT','GYM','MIXTO'], 'PUSH', 'Snatch explosivo a media sentadilla. 5×3 al 75%. Olímpico avanzado.']
];
CROSSFIT.forEach(([n,g,eq,ent,il,ins]) => add(n,'FUERZA',g,eq,ent,il,ins));

// ============ CARDIO VARIADO ============
const CARDIO = [
  ['Remo ergometro 500m', 'Resistencia', 'Remo', ['CROSSFIT','GYM','MIXTO'], 'RUN', '500m remo al 90%. Trabajo full body anaeróbico. 5 series con 2 min descanso.'],
  ['Remo 1000m al 80%', 'Resistencia', 'Remo', ['CROSSFIT','GYM','MIXTO'], 'RUN', '1000m remo al 80%. Capacidad aeróbica + fuerza-resistencia.'],
  ['Remo 2000m test', 'Resistencia', 'Remo', ['CROSSFIT','GYM','MIXTO'], 'RUN', 'Test 2000m al máximo. Anota tiempo. Estándar CrossFit.'],
  ['Remo HIIT 250m x10', 'Resistencia', 'Remo', ['CROSSFIT','GYM','MIXTO'], 'RUN', '10×250m al 95% con 1 min descanso. HIIT intenso.'],
  ['Assault bike 30s sprint', 'Resistencia', 'Assault bike', ['CROSSFIT','GYM','MIXTO'], 'RUN', '8×30s al máximo con 30s descanso. Pura potencia anaeróbica.'],
  ['Assault bike 5 min al 80%', 'Resistencia', 'Assault bike', ['CROSSFIT','GYM','MIXTO'], 'RUN', '5 min steady al 80%. Capacidad aeróbica.'],
  ['Sky erg ergometro', 'Resistencia', 'SkyErg', ['CROSSFIT','GYM','MIXTO'], 'PULL', '500m con SkyErg al 80%. Trabajo tirón + pulmonar. Específico CrossFit.'],
  ['Escalera mecánica 10 min', 'Resistencia', 'Escalera mecánica', ['GYM','MIXTO'], 'RUN', '10 min escalera mecánica nivel medio. Trabajo glúteos + cardio.'],
  ['Escalera mecánica HIIT', 'Resistencia', 'Escalera mecánica', ['GYM','MIXTO'], 'RUN', '5×1 min max / 1 min suave. Capacidad anaeróbica.'],
  ['Eliptica 20 min Z2', 'Resistencia', 'Elíptica', ['GYM','MIXTO'], 'RUN', '20 min elíptica zona 2 (conversacional). Cardio bajo impacto.'],
  ['Bici sala 4x4 Helgerud', 'Resistencia', 'Bici sala', ['GYM','CASA','MIXTO'], 'RUN', '4×4 min al 95% FCmax con 3 min recuperación. Mejora VO2max.'],
  ['Spinning HIIT', 'Resistencia', 'Bici sala', ['GYM','MIXTO'], 'RUN', '40 min mezclando llanos + cuestas + sprints. Quema 500+ kcal.'],
  ['Cinta inclinada caminata', 'Resistencia', 'Cinta', ['GYM','CASA','MIXTO'], 'RUN', '20 min cinta inclinada 12% al 5km/h. Trabajo gemelos + cardio.'],
  ['Cinta carrera 5km Z2', 'Resistencia', 'Cinta', ['GYM','CASA','MIXTO'], 'RUN', '5km cinta en zona 2. Largo lento aeróbico.'],
  ['Saltar a la comba 5x100', 'Resistencia', 'Comba', ['CROSSFIT','GYM','CASA','MIXTO'], 'JUMP', '5×100 saltos con 1 min descanso. Coordinación + cardio.'],
  ['Double-unders 5x30', 'Velocidad', 'Comba', ['CROSSFIT','GYM','CASA','MIXTO'], 'JUMP', 'Saltos comba con doble paso. 5×30 reps. Coordinación avanzada.']
];
CARDIO.forEach(([n,g,eq,ent,il,ins]) => add(n,'RESISTENCIA',g,eq,ent,il,ins));

// ============ PREVENCIÓN LESIONES (aductores + rotadores) ============
const PREVENCION = [
  ['Copenhague aductor', 'Core', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'PLANK', 'Plancha lateral con pie superior en banco. 30s/lado. Prevención aductor.'],
  ['Side plank aductor isométrico', 'Core', '—', ['CASA','GYM','MIXTO'], 'PLANK', 'Plancha lateral con pierna inferior apretando. 30s/lado. Foco aductor.'],
  ['Squeeze pelota entre rodillas', 'Core', 'Pelota', ['CASA','GYM','MIXTO'], 'PLANK', 'Sentado, pelota entre rodillas. Aprieta 5s × 15 reps. Aductor fácil.'],
  ['Rotaciones externas hombro con banda', 'Fuerza', 'Banda', ['CASA','GYM','MIXTO'], 'PULL', 'Codo a 90°, rota hacia fuera con banda. 3×15. Rehab hombro.'],
  ['Rotaciones internas hombro con banda', 'Fuerza', 'Banda', ['CASA','GYM','MIXTO'], 'PULL', 'Codo a 90°, rota hacia dentro con banda. 3×15. Equilibra hombro.'],
  ['L-sit a 90°', 'Fuerza', 'Paralelas', ['CALISTENIA','CROSSFIT','MIXTO'], 'PLANK', 'En paralelas o suelo. Piernas a 90°. Mantén 10-20s. Core extremo.'],
  ['Glute bridge unilateral', 'Glúteo', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'SQUAT', 'Bridge con una pierna en suelo. 3×12/pierna. Prevención lumbar.'],
  ['Clamshell con banda', 'Glúteo', 'Banda', ['CASA','GYM','MIXTO'], 'SQUAT', 'Tumbado lateral, rodillas flexionadas, abre con banda. 3×15. Glúteo medio.'],
  ['Monster walk con banda', 'Glúteo', 'Banda', ['CASA','GYM','CROSSFIT','MIXTO'], 'SQUAT', 'Banda en tobillos. Pasos laterales + atrás. 4×10m. Activación glúteo medio.'],
  ['Single leg deadlift', 'Glúteo', 'Mancuerna', ['CASA','GYM','MIXTO'], 'SQUAT', 'Peso muerto rumano con una pierna. 3×10/pierna. Equilibrio + isquios.'],
  ['Nordic hamstring asistido', 'Glúteo', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'SQUAT', 'Pies sujetos, baja torso controlando, usa manos para empujar arriba. 3×5.'],
  ['Calf raise unilateral', 'Pierna', '—', ['CASA','GYM','CALISTENIA','MIXTO'], 'SQUAT', 'Pantorrilla con una pierna. 3×15/pierna. Prevención tendón Aquiles.'],
  ['Tibial anterior (puntas arriba)', 'Pierna', '—', ['CASA','GYM','MIXTO'], 'SQUAT', 'Sentado, talones en suelo. Sube puntas controlando. 3×15. Prevención periostitis.']
];
PREVENCION.forEach(([n,g,eq,ent,il,ins]) => {
  // Asignamos pilar más correcto
  const pilar = g === 'Glúteo' || g === 'Pierna' ? 'FUERZA' : 'CORE';
  add(n, pilar, g, eq, ent, il, ins);
});

// ============ ESTIRAMIENTOS POSTENTRENO ============
const ESTIRAMIENTOS = [
  ['Estiramiento isquiotibiales tumbado', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Tumbado, pierna recta arriba con banda/toalla. Mantén 30s/pierna.'],
  ['Estiramiento cuádriceps de pie', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'De pie, mano sujeta tobillo. Mantén 30s/pierna. Estabiliza pelvis.'],
  ['Estiramiento gemelos en pared', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Manos en pared, pierna atrás extendida. 30s/pierna. Para correr.'],
  ['Estiramiento soleo flexionando rodilla', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Pierna atrás con rodilla flexionada. 30s/pierna. Tendón Aquiles.'],
  ['Estiramiento glúteo cruzado', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Tumbado, tobillo cruzado sobre rodilla opuesta, tira de muslo. 30s/lado.'],
  ['Estiramiento piriforme sentado', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Sentado, tobillo en muslo opuesto, inclina hacia adelante. 30s/lado.'],
  ['Estiramiento aductor sentado', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Sentado, plantas pies juntas, baja rodillas. 30s. Aductores.'],
  ['Estiramiento pecho en marco puerta', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Antebrazo en marco, gira cuerpo. 30s/lado. Pectoral mayor.'],
  ['Estiramiento dorsal contra pared', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Manos en pared a altura cintura, sienta hacia atrás. 30s. Dorsal grande.'],
  ['Estiramiento tríceps brazo doblado', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Codo arriba, mano detrás del cuello. 30s/brazo.'],
  ['Estiramiento bíceps en pared', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Mano en pared, brazo extendido atrás. 30s/brazo.'],
  ['Estiramiento cuello lateral', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Mano lleva cabeza a hombro. 30s/lado. Trapecio superior.'],
  ['Estiramiento lumbar acurrucado', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Tumbado, rodillas al pecho. 30s. Lumbar baja + glúteo.'],
  ['Niño pose (yoga)', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Rodillas y frente al suelo, brazos extendidos. 60s. Relajación postlumbar.'],
  ['Postura cobra', 'Movilidad', '—', ['CASA','GYM','MIXTO'], 'GENERAL', 'Boca abajo, eleva tronco con brazos. 30s. Movilidad lumbar reversa.']
];
ESTIRAMIENTOS.forEach(([n,g,eq,ent,il,ins]) => add(n,'MOVILIDAD',g,eq,ent,il,ins));

// Guardar
fs.writeFileSync(file, JSON.stringify(data, null, 2));
console.log(`OK. Total ejercicios en banco: ${data.ejercicios.length}`);
const porPilar = {};
data.ejercicios.forEach(e => { porPilar[e.pilar] = (porPilar[e.pilar] || 0) + 1; });
console.log('Por pilar:', porPilar);
