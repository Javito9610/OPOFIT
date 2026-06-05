/**
 * Entornos de entrenamiento y utilidades para filtrar/sustituir ejercicios.
 */
const ENTORNOS_VALIDOS = ['GYM', 'CROSSFIT', 'CALISTENIA', 'CASA', 'PISTA', 'MIXTO'];

const ENTORNO_META = {
  GYM: { etiqueta: 'Gimnasio', emoji: '🏋️', descripcion: 'Máquinas, barras, mancuernas y racks' },
  CROSSFIT: { etiqueta: 'CrossFit / Box', emoji: '🏋️‍♀️', descripcion: 'Cajones, kettlebells, cuerdas y WODs' },
  CALISTENIA: { etiqueta: 'Parque calistenia', emoji: '🤸', descripcion: 'Barras, paralelas y peso corporal' },
  CASA: { etiqueta: 'En casa', emoji: '🏠', descripcion: 'Mínimo material: suelo, bandas, mochila' },
  PISTA: { etiqueta: 'Pista / exterior', emoji: '🏃', descripcion: 'Carrera, sprints y trabajo en campo' },
  MIXTO: { etiqueta: 'Mixto', emoji: '🔀', descripcion: 'Combina varios entornos según el día' }
};

const EQUIP_A_ENTORNOS = [
  [/barra fija|paralelas|barra baja|trx/i, ['CALISTENIA', 'CROSSFIT', 'PISTA', 'MIXTO']],
  [/suelo|—|sin material/i, ['CASA', 'CALISTENIA', 'PISTA', 'MIXTO']],
  [/banda|mochila|escalera/i, ['CASA', 'MIXTO']],
  [/mancuerna|kettlebell|balón/i, ['GYM', 'CASA', 'CROSSFIT', 'MIXTO']],
  [/barra|rack|máquina|polea|prensa|elíptica|bici|step/i, ['GYM', 'CROSSFIT', 'MIXTO']],
  [/pista|cuesta|conos|vallas|paracaídas|escalera/i, ['PISTA', 'CALISTENIA', 'MIXTO']],
  [/piscina/i, ['PISTA', 'GYM', 'MIXTO']],
  [/cuerdas|trineo|landmine|assault/i, ['CROSSFIT', 'GYM', 'MIXTO']],
  [/cajón|pliometr/i, ['CROSSFIT', 'GYM', 'PISTA', 'CALISTENIA', 'MIXTO']]
];

function normalizarEntorno(v) {
  const e = String(v || '').toUpperCase().trim();
  return ENTORNOS_VALIDOS.includes(e) ? e : null;
}

/** Unifica pilares legacy (TREN_SUPERIOR, CARDIO…) para sustitución y agrupación. */
function normalizarPilar(pilar) {
  const p = String(pilar || '').toUpperCase().trim();
  if (p === 'TREN_SUPERIOR' || p === 'TREN_INFERIOR' || p === 'POTENCIA') return 'FUERZA';
  if (p === 'CARDIO') return 'RESISTENCIA';
  if (['FUERZA', 'RESISTENCIA', 'VELOCIDAD', 'MOVILIDAD', 'CORE'].includes(p)) return p;
  return p || 'FUERZA';
}

function inferirEntornosDesdeEquipamiento(equipamiento, pilar) {
  const eq = String(equipamiento || '').trim();
  const pil = String(pilar || '').toUpperCase();
  if (!eq || eq === '—' || eq === '-') {
    if (pil === 'RESISTENCIA' || pil === 'VELOCIDAD') return ['PISTA', 'CASA', 'GYM', 'MIXTO'];
    return ['CASA', 'CALISTENIA', 'MIXTO'];
  }
  for (const [re, entornos] of EQUIP_A_ENTORNOS) {
    if (re.test(eq)) return entornos;
  }
  return ['GYM', 'CROSSFIT', 'MIXTO'];
}

function parseEntornosCsv(csv) {
  if (!csv) return [];
  return String(csv)
    .split(',')
    .map((s) => normalizarEntorno(s))
    .filter(Boolean);
}

function ejercicioCompatible(entornosCsv, entornoUsuario) {
  const ent = normalizarEntorno(entornoUsuario) || 'MIXTO';
  if (ent === 'MIXTO') return true;
  const lista = parseEntornosCsv(entornosCsv);
  if (!lista.length) return true;
  return lista.includes(ent) || lista.includes('MIXTO');
}

function inferirTipoIlustracion(nombre, pilar, grupo) {
  const n = String(nombre || '').toLowerCase();
  const g = String(grupo || '').toLowerCase();
  const pil = String(pilar || '').toUpperCase();
  if (/plancha|hollow|pallof|ab wheel|core/.test(n) || pil === 'CORE') return 'PLANK';
  if (/flexion|press|fondos|push|bench|militar|landmine/.test(n) || g.includes('pecho')) return 'PUSH';
  if (/dominada|remo|jalón|pull|face pull|australian/.test(n) || g.includes('espalda')) return 'PULL';
  if (/sentadilla|zancada|prensa|hip thrust|peso muerto|squat|gemelo|pierna/.test(n) || g.includes('pierna'))
    return 'SQUAT';
  if (/sprint|velocidad|agilidad|conos|carioca|vallas|saltos|pliometr/.test(n) || pil === 'VELOCIDAD')
    return 'AGILITY';
  if (/carrera|fartlek|hiit|natac|bicicleta|elíptica|burpee|battle rope|rodaje/.test(n) || pil === 'RESISTENCIA')
    return 'RUN';
  if (/movilidad|estiramiento|ramp|stretch/.test(n) || pil === 'MOVILIDAD') return 'MOBILITY';
  if (pil === 'RESISTENCIA' || pil === 'VELOCIDAD') return 'RUN';
  return 'GENERAL';
}

function hashSeed(seed, key) {
  const s = `${seed}|${key}`;
  let h = 2166136261;
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return Math.abs(h);
}

function seededPick(arr, seed, key) {
  if (!arr?.length) return null;
  const idx = hashSeed(seed, key) % arr.length;
  return arr[idx];
}

function grupoClave(pilar, grupo, nombre) {
  const pil = normalizarPilar(pilar);
  const g = String(grupo || 'General').toLowerCase();
  const n = String(nombre || '').toLowerCase();
  if (pil === 'RESISTENCIA') {
    if (/hiit|series|fartlek|test/.test(n)) return `${pil}|intervalos`;
    if (/cuestas|bicicleta|elíptica|natac/.test(n)) return `${pil}|${n.split(' ')[0]}`;
    return `${pil}|cardio`;
  }
  if (pil === 'VELOCIDAD') {
    if (/sprint/.test(n)) return `${pil}|sprint`;
    if (/agilidad|conos|carioca|vallas|escalera/.test(n)) return `${pil}|agilidad`;
    if (/salt|pliometr|cmj|triple/.test(n)) return `${pil}|pliometria`;
    return `${pil}|velocidad`;
  }
  if (g.includes('pecho')) return `${pil}|pecho`;
  if (g.includes('espalda')) return `${pil}|espalda`;
  if (g.includes('pierna') || g.includes('glúteo') || g.includes('isquio')) return `${pil}|pierna`;
  if (g.includes('hombro')) return `${pil}|hombro`;
  if (g.includes('core')) return `${pil}|core`;
  if (g.includes('bíceps') || g.includes('tríceps')) return `${pil}|brazos`;
  return `${pil}|${g.split('/')[0].trim() || 'general'}`;
}

module.exports = {
  ENTORNOS_VALIDOS,
  ENTORNO_META,
  normalizarEntorno,
  normalizarPilar,
  inferirEntornosDesdeEquipamiento,
  parseEntornosCsv,
  ejercicioCompatible,
  inferirTipoIlustracion,
  hashSeed,
  seededPick,
  grupoClave
};
