/**
 * PatronMovimientoService — clasifica cada ejercicio en uno de los 13 patrones
 * fundamentales del movimiento humano (FMS / NSCA / StrongFirst).
 *
 * Para qué sirve:
 *  1. Balancear sesiones: una sesión pro NUNCA mete 3 push horizontales y
 *     0 tirones. Aquí se detecta y se redistribuye.
 *  2. Acoplar la prescripción al patrón (no es lo mismo programar un
 *     peso muerto que un curl: cargas, descansos y RPE distintos).
 *  3. Detectar antagonistas para supersets (push_h ↔ pull_h).
 *
 * Cubierto por test unitario (PatronMovimientoService.test.js).
 */

const PATRONES = {
  SQUAT:    'Sentadilla / dominante de cuádriceps bilateral',
  HINGE:    'Bisagra de cadera (peso muerto, hip thrust, KB swing)',
  LUNGE:    'Unilateral pierna (zancada, split squat)',
  PUSH_H:   'Empuje horizontal (press banca, flexión)',
  PUSH_V:   'Empuje vertical (press militar, HSPU)',
  PULL_H:   'Tirón horizontal (remo, australian)',
  PULL_V:   'Tirón vertical (dominada, jalón)',
  CARRY:    'Acarreo (farmer walk, sandbag)',
  ROT:      'Rotación / antirrotación de tronco',
  ANTI_EXT: 'Antiextensión de tronco (plancha, hollow)',
  LOCO:     'Locomoción cíclica (carrera, natación, bici)',
  PLYO:     'Pliometría / saltos',
  SPRINT:   'Sprint / aceleración',
  AGI:      'Agilidad / cambios de dirección (conos, escalera)',
  MOB:      'Movilidad / estiramiento'
};

function normalizar(s) {
  return String(s || '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();
}

function clasificar(ej) {
  const n = normalizar(ej.nombre);
  if (!n) return 'SQUAT';

  if (/\bsprint|arranque|aceleracion|salida (de )?tacos/.test(n)) return 'SPRINT';
  if (/agilidad|conos|escalera|carioca|\bvallas\b|t-test|illinois|pro agility|5-10-5/.test(n)) return 'AGI';
  if (/salto|box jump|cmj|pliometr|broad jump|depth jump|tuck jump|skipping|sled push|burpee box/.test(n)) return 'PLYO';
  if (/movilidad|estiramiento|stretch|cat.cow|world.greatest|90\/90|hip flow|yoga/.test(n)) return 'MOB';
  if (/natac|carrera|trote|rodaje|fartlek|\bhiit\b|interval|bicicleta|remo erg|comba|battle rope|assault|echo bike|ski erg|step mill|rucking|marcha/.test(n)) {
    return 'LOCO';
  }
  if (/farmer|granjero|sandbag carry|yoke|carrying|paseo del granjero|suitcase carry|over.head carry/.test(n)) {
    return 'CARRY';
  }

  if (/dominada|chin-up|pull-up|jalon|jalon al pecho|muscle-up|towel pull/.test(n)) return 'PULL_V';
  if (/\bremo\b|australian|inverted row|seal row|t-bar|face pull|pendlay/.test(n)) return 'PULL_H';
  if (/press militar|push press|arnold|handstand|pike push|press hombros|hspu|press de hombros/.test(n)) {
    return 'PUSH_V';
  }
  if (/press banca|bench|flexion|push-up|fondos|\bdip\b|cruce de polea|apertura|landmine press|press inclinad|press declinad/.test(n)) {
    return 'PUSH_H';
  }

  if (/zancada|lunge|split squat|cossack|step-?up|bulgarian?|patada de gluteo/.test(n)) return 'LUNGE';
  if (/peso muerto|deadlift|good morning|hip thrust|puente de gluteo|nordic|romanian|kettlebell swing|\bswing\b|hipthrust|kb swing|hiperextension/.test(n)) {
    return 'HINGE';
  }
  if (/sentadilla|squat|prensa|hack squat|sissy|wall sit|pistol|cossack squat|goblet/.test(n)) {
    return 'SQUAT';
  }

  if (/pallof|landmine rot|woodchop|cable rot|antirrot|chop|lift/.test(n)) return 'ROT';
  if (/plancha|plank|hollow|l-sit|farmer hold|colgado|superman|dead bug|bird dog|ab wheel/.test(n)) {
    return 'ANTI_EXT';
  }
  if (/russian twist|bicycle|bicicleta abdom|sit.?up|crunch|abdom|v-up|leg raise|elevacion de pierna|mountain climber/.test(n)) {
    return 'ROT';
  }

  // Fallback por grupo muscular.
  const g = normalizar(ej.grupo_muscular);
  if (g.includes('pecho')) return 'PUSH_H';
  if (g.includes('espalda')) return 'PULL_H';
  if (g.includes('pierna') || g.includes('gluteo')) return 'SQUAT';
  if (g.includes('hombro')) return 'PUSH_V';
  if (g.includes('brazo')) return 'PULL_H';
  if (g.includes('core')) return 'ANTI_EXT';
  if (g.includes('cardio')) return 'LOCO';
  if (g.includes('movilidad')) return 'MOB';
  return 'SQUAT';
}

/** Agrupa los patrones en familias grandes (para balancear sesiones). */
function familia(patron) {
  if (['PUSH_H', 'PUSH_V'].includes(patron)) return 'PUSH';
  if (['PULL_H', 'PULL_V'].includes(patron)) return 'PULL';
  if (['SQUAT', 'LUNGE'].includes(patron)) return 'LEGS_QUAD';
  if (patron === 'HINGE') return 'LEGS_HINGE';
  if (['ANTI_EXT', 'ROT'].includes(patron)) return 'CORE';
  if (['SPRINT', 'PLYO', 'AGI'].includes(patron)) return 'POWER';
  if (patron === 'LOCO') return 'CARDIO';
  if (patron === 'CARRY') return 'CARRY';
  if (patron === 'MOB') return 'MOBILITY';
  return 'OTHER';
}

/** Antagonista del patrón (para emparejar supersets). */
function antagonista(patron) {
  switch (patron) {
    case 'PUSH_H': return 'PULL_H';
    case 'PUSH_V': return 'PULL_V';
    case 'PULL_H': return 'PUSH_H';
    case 'PULL_V': return 'PUSH_V';
    case 'SQUAT':  return 'HINGE';
    case 'HINGE':  return 'SQUAT';
    case 'LUNGE':  return 'LUNGE';
    case 'ANTI_EXT': return 'ROT';
    case 'ROT':    return 'ANTI_EXT';
    default: return null;
  }
}

/**
 * Comprueba si la composición de patrones de una sesión está balanceada.
 * Devuelve un objeto con `ok: true` + warnings (si hay desbalance).
 */
function auditarBalance(patrones) {
  const conteo = {};
  for (const p of patrones) conteo[p] = (conteo[p] || 0) + 1;
  const push = (conteo.PUSH_H || 0) + (conteo.PUSH_V || 0);
  const pull = (conteo.PULL_H || 0) + (conteo.PULL_V || 0);
  const quad = (conteo.SQUAT || 0) + (conteo.LUNGE || 0);
  const hinge = conteo.HINGE || 0;
  const warnings = [];
  if (push >= 2 && pull === 0) warnings.push('PUSH_SIN_PULL');
  if (pull >= 2 && push === 0) warnings.push('PULL_SIN_PUSH');
  if (quad >= 2 && hinge === 0 && (push + pull) >= 2) warnings.push('CUADRICEPS_SIN_HINGE');
  return { ok: warnings.length === 0, warnings, conteo };
}

module.exports = {
  PATRONES,
  clasificar,
  familia,
  antagonista,
  auditarBalance
};
