/**
 * CalentamientoService — bloques de warmup (modelo RAMP de Ian Jeffreys)
 * y vuelta a la calma adaptados al enfoque de la sesión.
 *
 * Antes la sesión iba directa al primer ejercicio pesado: ni preparador
 * con un mes de carrera prescribiría eso. Ahora cada sesión tiene 3-4
 * min de warmup específico + 4-5 min de cooldown.
 *
 * RAMP:
 *  R · Raise — eleva temperatura corporal
 *  A · Activate — activa la musculatura del enfoque
 *  M · Mobilise — movilidad articular específica
 *  P · Potentiate — preparación neural (aproximaciones, drills)
 */

const WARMUP = {
  FUERZA: [
    { rampa: 'R', titulo: 'Movilidad dinámica + 60 saltos de comba', detalle: 'Eleva pulso a 110-120 lpm.', duracion_s: 180 },
    { rampa: 'A', titulo: 'Activación de glúteo con banda (clamshells, monster walks)', detalle: '2×15 cada lado, banda por encima de rodillas.', duracion_s: 90 },
    { rampa: 'M', titulo: 'Movilidad torácica + cat-cow', detalle: '8 rep cada lado lentas.', duracion_s: 90 },
    { rampa: 'P', titulo: 'Aproximaciones al primer ejercicio', detalle: '2 series con 40% y 60% de la carga objetivo.', duracion_s: 180 }
  ],
  VELOCIDAD: [
    { rampa: 'R', titulo: 'Trote progresivo 5 min', detalle: 'De suave a aeróbico.', duracion_s: 300 },
    { rampa: 'A', titulo: 'Skipping bajo + alto + talones al glúteo', detalle: '3×20 m de cada técnica.', duracion_s: 180 },
    { rampa: 'M', titulo: 'Movilidad de cadera 4 direcciones', detalle: '8 rep cada plano.', duracion_s: 90 },
    { rampa: 'P', titulo: '3×30 m progresivos (60-80-90%)', detalle: 'Recuperación completa entre cada uno.', duracion_s: 180 }
  ],
  RESISTENCIA: [
    { rampa: 'R', titulo: 'Carrera suave Z1 8-10 min', detalle: 'Frecuencia conversacional, RPE 3-4.', duracion_s: 540 },
    { rampa: 'A', titulo: 'Drills técnicos A-B-C', detalle: '2×20 m de cada drill.', duracion_s: 180 },
    { rampa: 'M', titulo: 'Tobillo + cadera dinámica', detalle: '10 rep por articulación.', duracion_s: 90 }
  ],
  CORE: [
    { rampa: 'R', titulo: 'Bear crawl 2×30 s', detalle: 'Lento, con control, espalda neutra.', duracion_s: 90 },
    { rampa: 'A', titulo: 'Dead bug 2×8 cada lado', detalle: 'Activación de transverso.', duracion_s: 120 },
    { rampa: 'M', titulo: 'Movilidad lumbar + torácica', detalle: 'Cat-cow + thread the needle.', duracion_s: 90 }
  ],
  MOVILIDAD: [
    { rampa: 'R', titulo: 'Caminar respirando 3 min', detalle: 'Respiración nasal profunda.', duracion_s: 180 }
  ]
};

const COOLDOWN = {
  FUERZA: [
    { titulo: 'Respiración nasal 5 min sentado', detalle: 'Inhalar 4 s · exhalar 6 s. Reduce cortisol post entreno.', duracion_s: 300 },
    { titulo: 'Estiramientos estáticos de músculos trabajados', detalle: '30 s por grupo, sin rebotar.', duracion_s: 240 }
  ],
  VELOCIDAD: [
    { titulo: 'Trote regenerativo 3 min Z1', detalle: 'Hidratarte mientras bajas el pulso.', duracion_s: 180 },
    { titulo: 'Estiramientos isquios/cuádriceps/gemelos/psoas', detalle: '30 s por grupo.', duracion_s: 300 }
  ],
  RESISTENCIA: [
    { titulo: 'Caminar 5 min desacelerando', detalle: 'Baja el pulso a <120 lpm.', duracion_s: 300 },
    { titulo: 'Estiramientos cadena posterior + cuádriceps', detalle: '30 s por grupo.', duracion_s: 240 }
  ],
  CORE: [
    { titulo: 'Postura del niño + cobra', detalle: '60 s cada postura, respiración profunda.', duracion_s: 180 }
  ],
  MOVILIDAD: [
    { titulo: 'Savasana 3 min', detalle: 'Tumbado, respiración natural.', duracion_s: 180 }
  ]
};

function calentamiento(enfoque) {
  const k = String(enfoque || '').toUpperCase();
  return JSON.parse(JSON.stringify(WARMUP[k] || WARMUP.FUERZA));
}

function vueltaACalma(enfoque) {
  const k = String(enfoque || '').toUpperCase();
  return JSON.parse(JSON.stringify(COOLDOWN[k] || COOLDOWN.FUERZA));
}

function duracionTotalWarmup(enfoque) {
  return calentamiento(enfoque).reduce((acc, b) => acc + (b.duracion_s || 0), 0);
}

function duracionTotalCooldown(enfoque) {
  return vueltaACalma(enfoque).reduce((acc, b) => acc + (b.duracion_s || 0), 0);
}

module.exports = {
  WARMUP,
  COOLDOWN,
  calentamiento,
  vueltaACalma,
  duracionTotalWarmup,
  duracionTotalCooldown
};
