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

/**
 * Clasificador POR NOMBRE (autoritativo). El catálogo base (~400 ejercicios de
 * oposición) no traía entornos fiables → el filtro dejaba pasar "press banca"
 * en calistenia y ejercicios de gym en casa. Los nombres SÍ son descriptivos,
 * así que deducimos entorno+equipamiento del nombre. Orden = de más específico
 * a más genérico; gana la primera regla que casa. Todas incluyen MIXTO.
 */
const CLASIF_NOMBRE = [
  // Natación (antes que carrera para que "Natación 50m" no caiga en pista)
  [/nataci[oó]n|\bcrol\b|nadar|piscina|braza|espalda \(agua\)/i,
    ['GYM', 'PISTA', 'MIXTO'], 'Piscina'],
  // Carrera / pista / velocidad en campo (metros, series, sprints, cuestas)
  [/carrera|trote|rodaje|fartlek|\btempo\b|cuestas?|cambios? de ritmo|\bsprints?\b|\d+\s*m\b|\d+\s*km\b|1000m|2000m|800m|400m|200m|100m|marat[oó]n/i,
    ['PISTA', 'MIXTO'], 'Pista'],
  // Comba / cuerda de saltar
  [/\bcomba\b|cuerda.*salt|salt.*cuerda|double under/i,
    ['CASA', 'CROSSFIT', 'PISTA', 'CALISTENIA', 'MIXTO'], 'Comba'],
  // Barra fija / calistenia con barra (dominadas, muscle-up, remo invertido...)
  [/dominad|pull[\s-]?up|muscle[\s-]?up|front lever|back lever|l[\s-]?sit|suspensi[oó]n en barra|remo invertid|remo australian|toes to bar|colgad/i,
    ['CALISTENIA', 'GYM', 'CROSSFIT', 'MIXTO'], 'Barra fija'],
  // Paralelas / fondos
  [/fondos? en paralel|dips?( en)? paralel|paralelas/i,
    ['CALISTENIA', 'GYM', 'CROSSFIT', 'MIXTO'], 'Paralelas'],
  // Kettlebell (antes que barra/mancuerna para captar "swing con KB")
  [/kettlebell|\bkb\b|\bswing\b|turkish|goblet/i,
    ['CROSSFIT', 'GYM', 'CASA', 'MIXTO'], 'Kettlebell'],
  // Mancuernas (antes que barra: "press banca con mancuernas" → mancuernas)
  [/mancuerna|dumbbell|\bdb\b|botellas? de agua/i,
    ['GYM', 'CASA', 'CROSSFIT', 'MIXTO'], 'Mancuernas'],
  // Levantamientos con barra olímpica (gym/crossfit)
  [/press banca|press militar|press de banca|peso muerto|sentadilla con barra|hip thrust con barra|remo con barra|arranque|dos tiempos|\bclean\b|\bsnatch\b|\bjerk\b|thruster|overhead squat|front squat|back squat|barra ol[ií]mpica|hip thrust\b/i,
    ['GYM', 'CROSSFIT', 'MIXTO'], 'Barra olímpica'],
  // Máquinas de gimnasio (incluye nombres en inglés y poleas/cables)
  [/prensa|leg press|hack squat|leg extension|extensi[oó]n de cu[aá]driceps|leg curl|curl femoral|femoral (tumbad|sentad|en m)|jal[oó]n|pulldown|lat pulldown|\bpolea\b|\bcable\b|cruce de polea|apertura(s)? en m[aá]quina|pec deck|peck deck|contractor|m[aá]quina|multipower|\bsmith\b|remo en m[aá]quina|press (de )?(pecho|hombro) en m[aá]quina|chest press|shoulder press|seated|gemelo en m[aá]quina|elevaci[oó]n de gemelo en m|hip thrust en m[aá]quina/i,
    ['GYM', 'MIXTO'], 'Máquina'],
  // Material CrossFit específico
  [/wall\s?ball|box jump|caj[oó]n|assault bike|echo bike|air bike|row(ing)? erg|remo ergo|ski\s?erg|battle rope|cuerda de batalla|\bsled\b|trineo|devil press|\bwod\b|\bamrap\b|\bemom\b|for time|man\s?maker|\byoke\b|farmer/i,
    ['CROSSFIT', 'GYM', 'MIXTO'], 'Material CrossFit'],
  // Bandas elásticas / gomas
  [/banda el[aá]stica|goma el[aá]stica|\bgoma\b|resistance band/i,
    ['CASA', 'GYM', 'MIXTO'], 'Banda'],
  // Cardio de máquina de GYM/BOX (no de casa): step mill, escaladora, assault/
  // echo/air bike, ski erg, remo ergómetro.
  [/step\s*mill|stairmaster|escaladora|assault\s*bike|echo\s*bike|air\s*bike|ski\s*erg|remo\s*erg|remoergo|bici\s*sala/i,
    ['GYM', 'CROSSFIT', 'MIXTO'], 'Máquina cardio'],
  // Cardio de máquina que también puede haber en casa: cinta, bici estática, elíptica.
  [/cinta de correr|bici(cleta)? est[aá]tica|el[ií]ptica|spinning/i,
    ['GYM', 'CASA', 'MIXTO'], 'Máquina cardio'],
  // Agilidad / coordinación / velocidad de pies
  [/escalera de agilidad|\bconos\b|circuito de conos|\bvallas\b|skipping|carioca|desplazamiento lateral|agilidad|propiocep|coordinaci[oó]n|reacci[oó]n/i,
    ['PISTA', 'CALISTENIA', 'CASA', 'MIXTO'], 'Conos'],
  // Movilidad / calentamiento / estiramientos
  [/movilidad|estiramiento|estirar|foam roller|\byoga\b|calentamiento|activaci[oó]n|respiraci[oó]n/i,
    ['CASA', 'GYM', 'CALISTENIA', 'PISTA', 'MIXTO'], 'Suelo'],
  // Peso corporal (flexiones, plancha, sentadilla, burpee, abdominales...)
  [/flexion|push[\s-]?up|fondos en silla|planch|\bplank\b|abdominal|\bcrunch\b|mountain climb|sentadilla|\bsquat\b|zancada|\blunge\b|puente|glute bridge|burpee|jumping jack|sit[\s-]?up|hollow|superman|elevaci[oó]n de piernas|\bgemelo|pistol|handstand|\bpino\b|escalador/i,
    ['CASA', 'CALISTENIA', 'PISTA', 'MIXTO'], 'Suelo']
];

function clasificarEntornos(nombre, equipamiento, pilar) {
  const n = String(nombre || '').toLowerCase();
  for (const [re, entornos, equip] of CLASIF_NOMBRE) {
    if (re.test(n)) return { entornos: entornos.slice(), equip, confianza: true };
  }
  // Sin señal por nombre. Si hay equipamiento explícito, inferimos por él.
  const eq = String(equipamiento || '').trim();
  if (eq && eq !== '—' && eq !== '-') {
    return { entornos: inferirEntornosDesdeEquipamiento(equipamiento, pilar), equip: equipamiento, confianza: false };
  }
  // Sin equipo NI señal de nombre: NO metemos en CASA/CALISTENIA (evita que
  // ejercicios desconocidos —normalmente de máquina/gym— contaminen entornos
  // sin material). Cardio → pista/gym; fuerza desconocida → gym.
  const pil = normalizarPilar(pilar);
  if (pil === 'RESISTENCIA' || pil === 'VELOCIDAD') {
    return { entornos: ['PISTA', 'GYM', 'MIXTO'], equip: null, confianza: false };
  }
  return { entornos: ['GYM', 'MIXTO'], equip: null, confianza: false };
}

/** Entornos efectivos de un ejercicio: manda el nombre; si no es concluyente,
 *  usa el CSV almacenado y, en último caso, la inferencia por equipamiento. */
function entornosEfectivos(ejercicio) {
  const c = clasificarEntornos(ejercicio.nombre, ejercicio.equipamiento, ejercicio.pilar);
  if (c.confianza) return c.entornos;
  const stored = parseEntornosCsv(ejercicio.entornos);
  return stored.length ? stored : c.entornos;
}

/** Compatibilidad usando el clasificador por nombre (recomendado).
 *  OJO: MIXTO es concepto del USUARIO ("muéstramelo todo"), NO comodín del
 *  ejercicio. Por eso aquí NO se acepta lista.includes('MIXTO') — si no, todo
 *  sería compatible con todo (era el bug: "press banca" salía en calistenia). */
function ejercicioCompatiblePorNombre(ejercicio, entornoUsuario) {
  const ent = normalizarEntorno(entornoUsuario) || 'MIXTO';
  if (ent === 'MIXTO') return true;
  const lista = entornosEfectivos(ejercicio);
  return lista.includes(ent);
}

/**
 * Filtro defensivo (defense-in-depth): aunque el ejercicio diga que es
 * compatible con un entorno, descartamos los obvios falsos positivos
 * (saco de arena en gimnasio comercial, dominada con toalla en casa sin
 * barra, etc.). El usuario reportaba "y dale con los putos saquitos en el
 * gym": esta función impide que se cuelen.
 */
function ejercicioRealistaParaEntorno(nombre, equipamiento, entornoUsuario) {
  const ent = normalizarEntorno(entornoUsuario) || 'MIXTO';
  if (ent === 'MIXTO') return true;
  const n = String(nombre || '').toLowerCase();
  const eq = String(equipamiento || '').toLowerCase();

  // Material strongman / outdoor (saco arena, sandbag, trineo, sled, yoke,
  // maza, mace, bolsa búlgara) NO encaja con un gym comercial ni con CASA
  // ni con CALISTENIA estricta. Sí encaja en CROSSFIT y PISTA.
  const esStrongman =
    /saco.*(arena|kg|boxeo)|sandbag|sled|trineo|yoke|\bmaza\b|\bmace\b|bolsa\s+búlgara|bolsa\s+bulgara|bandera\s+lastrada/i
      .test(`${n} ${eq}`);
  if (esStrongman && (ent === 'GYM' || ent === 'CASA' || ent === 'CALISTENIA')) {
    return false;
  }

  // Toalla como agarre (towel pull-up / con toalla) solo tiene sentido si
  // hay barra de dominadas → CALISTENIA / CROSSFIT / GYM con barra.
  // En CASA sin barra no.
  if (/toalla|towel/.test(n) && ent === 'CASA') return false;

  // Mochila lastrada / garrafas / botellas / silla / improvisado: material
  // de andar por CASA. En un gimnasio con barras, discos y mancuernas reales
  // es ridículo proponer "peso muerto con mochila" (reporte del usuario).
  // También fuera de CROSSFIT y CALISTENIA equipada.
  const esImprovisado =
    /mochila|garraf|botella|silla\b|toalla en puerta|sof[áa]|escal[oó]n de casa|lastre casero/i
      .test(`${n} ${eq}`);
  if (esImprovisado && (ent === 'GYM' || ent === 'CROSSFIT')) return false;

  // Anillas no son típicas de un gym comercial estándar.
  if (/anilla|ring (dip|muscle|pull|row|fly)|iron cross/i.test(`${n} ${eq}`) && ent === 'GYM') {
    return false;
  }

  // Cuerda gruesa para climbing solo en boxes CrossFit típicos.
  if (/legless rope|rope climb/i.test(n) && (ent === 'GYM' || ent === 'CASA')) {
    return false;
  }

  return true;
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
  clasificarEntornos,
  entornosEfectivos,
  ejercicioCompatiblePorNombre,
  ejercicioRealistaParaEntorno,
  inferirTipoIlustracion,
  hashSeed,
  seededPick,
  grupoClave
};
