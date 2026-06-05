/**
 * Motor inteligente de prescripción e instrucciones por ejercicio.
 * Cada movimiento recibe series/reps realistas (variadas por nombre) y texto técnico propio.
 */
const EntornoEntreno = require('../utils/EntornoEntreno');

const { hashSeed } = EntornoEntreno;

function meta() {
  return require('./EjercicioMetadataService');
}

function variar(seed, key, min, max) {
  if (min >= max) return min;
  return min + (hashSeed(seed, key) % (max - min + 1));
}

function normalizarTexto(nombre) {
  return String(nombre || '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();
}

/** Extrae prescripción embebida en el nombre (cardio, sprints, series en metros…). */
function parsearPrescripcionNombre(nombre) {
  const n = normalizarTexto(nombre);
  let m = n.match(/series?\s*(\d+)\s*m\s*x\s*(\d+)/);
  if (m) return { series: +m[2], repeticiones: +m[1], unidad: 'm', descanso: +m[1] < 400 ? 90 : 150 };
  m = n.match(/(\d+)\s*m\s*(?:series?|x)\s*(\d+)/);
  if (m) return { series: +m[2], repeticiones: +m[1], unidad: 'm', descanso: 90 };
  m = n.match(/sprint\s*(\d+)\s*m\s*x\s*(\d+)/);
  if (m) return { series: +m[2], repeticiones: +m[1], unidad: 'm', descanso: 120 };
  m = n.match(/sprint\s*(\d+)\s*m/);
  if (m) return { series: variar(+m[1], 'sp', 4, 8), repeticiones: +m[1], unidad: 'm', descanso: 120 };
  m = n.match(/(\d+)\s*x\s*(\d+)\s*m/);
  if (m) return { series: +m[1], repeticiones: +m[2], unidad: 'm', descanso: 90 };
  m = n.match(/(\d+)\s*min\b/);
  if (m) return { series: 1, repeticiones: +m[1], unidad: 'min', descanso: 0 };
  m = n.match(/(\d+)\s*km\b/);
  if (m) return { series: 1, repeticiones: +m[1], unidad: 'km', descanso: 0 };
  m = n.match(/(\d+)\s*m\b/);
  if (m && !/press|remo|dominada|flexion|sentadilla/.test(n)) {
    return { series: variar(+m[1], 'md', 3, 6), repeticiones: +m[1], unidad: 'm', descanso: 90 };
  }
  m = n.match(/(\d+)\s*s\b/);
  if (m) return { series: variar(+m[1], 'ss', 3, 4), repeticiones: +m[1], unidad: 's', descanso: 60 };
  m = n.match(/(\d+)\s*x\s*(\d+)/);
  if (m) return { series: +m[1], repeticiones: +m[2], unidad: 'reps', descanso: 75 };
  if (/amrap|emom|al fallo|a fallo|max tiempo|máx tiempo/.test(n)) {
    return { series: 1, repeticiones: 99, unidad: 'amrap', descanso: 0 };
  }
  return null;
}

/** Clasifica el ejercicio en un perfil de prescripción. */
function clasificarPerfil(ej) {
  const nombre = normalizarTexto(ej.nombre);
  const pilar = EntornoEntreno.normalizarPilar(ej.pilar || ej.categoria);
  const grupo = normalizarTexto(
    ej.grupo_muscular || meta().inferirGrupoMuscular(null, ej.nombre, pilar)
  );

  const parsed = parsearPrescripcionNombre(ej.nombre);
  if (parsed) return { perfil: 'PARSED', ...parsed };

  if (pilar === 'RESISTENCIA') {
    if (/hiit|fartlek|interval|tempo|cambios/.test(nombre)) return { perfil: 'CARDIO_INTERVAL', unidad: 'min' };
    if (/carrera|trote|rodaje|z2|continua|marcha|rucking/.test(nombre)) return { perfil: 'CARDIO_CONTINUO', unidad: 'min' };
    if (/natac|remo erg|bicicleta|eliptica|assault|step mill|battle rope|comba/.test(nombre)) {
      return { perfil: 'CARDIO_CONTINUO', unidad: 'min' };
    }
    return { perfil: 'CARDIO_CONTINUO', unidad: 'min' };
  }
  if (pilar === 'VELOCIDAD') {
    if (/sprint|arranque|skipping/.test(nombre)) return { perfil: 'SPRINT', unidad: 'm' };
    if (/salt|pliometr|cmj|box jump|salto/.test(nombre)) return { perfil: 'PLIOMETRIA', unidad: 'reps' };
    if (/agilidad|conos|carioca|vallas|escalera|propiocept/.test(nombre)) return { perfil: 'AGILIDAD', unidad: 'm' };
    return { perfil: 'SPRINT', unidad: 'm' };
  }
  if (pilar === 'MOVILIDAD') return { perfil: 'MOVILIDAD', unidad: 's' };

  if (/plancha|hollow|l-sit|colgado.*max|farmer hold|wall sit|isometr|hold/.test(nombre)) {
    return { perfil: 'ISOMETRICO', unidad: 's' };
  }
  if (/wrist curl|reverse wrist|pinch grip/.test(nombre)) return { perfil: 'WRIST', unidad: 'reps' };
  if (/curl|martillo|chin-up/.test(nombre) && grupo.includes('brazo')) return { perfil: 'CURL', unidad: 'reps' };
  if (/triceps|tríceps|fondos|dip|extension.*brazo|press francés|patada de triceps/.test(nombre)) {
    return { perfil: 'TRICEPS', unidad: 'reps' };
  }
  if (/elevacion|lateral|pájaros|face pull|militar|arnold|cuban|y-raise|remo al menton/.test(nombre)) {
    return { perfil: 'HOMBRO', unidad: 'reps' };
  }
  if (/dominada|muscle-up|towel pull/.test(nombre)) return { perfil: 'DOMINADA', unidad: 'reps' };
  if (/remo|jalon|jalon|pullover|encogimiento|hiperextension|good morning|superman/.test(nombre)) {
    return { perfil: 'REMO', unidad: 'reps' };
  }
  if (/press|flexion|fondos en banco|bench|apertura|cruce de polea|landmine|handstand|pike push/.test(nombre)) {
    return { perfil: 'PRESS', unidad: 'reps' };
  }
  if (/sentadilla|prensa|zancada|step-up|step up|split squat|cossack|wall sit/.test(nombre)) {
    return { perfil: 'PIERNA', unidad: 'reps' };
  }
  if (/peso muerto|hip thrust|puente de gluteo|puente gluteo|patada de gluteo|abduccion/.test(nombre)) {
    return { perfil: 'CADENA_POSTERIOR', unidad: 'reps' };
  }
  if (/gemelo|curl femoral|nordic|extension de cuadriceps|sissy squat|tibial/.test(nombre)) {
    return { perfil: 'AISLAMIENTO_PIERNA', unidad: 'reps' };
  }
  if (/burpee|mountain climber|thruster|devil press|wall ball|kettlebell swing|circuito/.test(nombre)) {
    return { perfil: 'METCON', unidad: 'reps' };
  }
  if (/crunch|v-up|bicycle|russian twist|dead bug|bird dog|ab wheel|elevacion de pierna|woodchop|bear crawl|turkish/.test(nombre)) {
    return { perfil: 'CORE_REPS', unidad: 'reps' };
  }
  if (pilar === 'CORE') return { perfil: 'CORE_REPS', unidad: 'reps' };
  if (grupo.includes('pecho')) return { perfil: 'PRESS', unidad: 'reps' };
  if (grupo.includes('espalda')) return { perfil: 'REMO', unidad: 'reps' };
  if (grupo.includes('pierna') || grupo.includes('gluteo')) return { perfil: 'PIERNA', unidad: 'reps' };
  if (grupo.includes('hombro')) return { perfil: 'HOMBRO', unidad: 'reps' };
  if (grupo.includes('brazo')) return { perfil: 'CURL', unidad: 'reps' };
  return { perfil: 'FUERZA_GENERAL', unidad: 'reps' };
}

const RANGOS = {
  PARSED: null,
  CARDIO_INTERVAL: { series: [1, 1], reps: [12, 25], descanso: [0, 0] },
  CARDIO_CONTINUO: { series: [1, 1], reps: [20, 45], descanso: [0, 0] },
  SPRINT: { series: [4, 8], reps: [30, 80], descanso: [90, 150] },
  PLIOMETRIA: { series: [3, 5], reps: [5, 10], descanso: [90, 120] },
  AGILIDAD: { series: [3, 6], reps: [20, 40], descanso: [60, 90] },
  MOVILIDAD: { series: [2, 3], reps: [30, 60], descanso: [30, 45] },
  ISOMETRICO: { series: [3, 4], reps: [25, 50], descanso: [45, 75] },
  WRIST: { series: [2, 3], reps: [12, 18], descanso: [45, 60] },
  CURL: { series: [3, 4], reps: [10, 15], descanso: [60, 75] },
  TRICEPS: { series: [3, 4], reps: [10, 15], descanso: [60, 75] },
  HOMBRO: { series: [3, 4], reps: [10, 15], descanso: [60, 90] },
  DOMINADA: { series: [3, 5], reps: [4, 8], descanso: [120, 180] },
  REMO: { series: [3, 4], reps: [8, 12], descanso: [90, 120] },
  PRESS: { series: [3, 4], reps: [6, 12], descanso: [90, 120] },
  PIERNA: { series: [3, 4], reps: [8, 12], descanso: [90, 150] },
  CADENA_POSTERIOR: { series: [3, 4], reps: [6, 10], descanso: [120, 180] },
  AISLAMIENTO_PIERNA: { series: [3, 4], reps: [10, 15], descanso: [60, 90] },
  METCON: { series: [3, 5], reps: [8, 15], descanso: [60, 90] },
  CORE_REPS: { series: [3, 4], reps: [12, 20], descanso: [45, 60] },
  FUERZA_GENERAL: { series: [3, 4], reps: [8, 12], descanso: [75, 90] }
};

function generarPrescripcion(ej, ctx = {}) {
  const seed = Number(ctx.seed ?? ej.orden ?? ej.id_ejercicio ?? 0);
  const key = meta().normalizarNombreEjercicio(ej.nombre) || 'ej';
  const clasif = clasificarPerfil(ej);

  if (clasif.perfil === 'PARSED') {
    return {
      series: clasif.series,
      repeticiones: clasif.repeticiones,
      unidad: clasif.unidad,
      descanso: clasif.descanso ?? 90
    };
  }

  const rango = RANGOS[clasif.perfil] || RANGOS.FUERZA_GENERAL;
  const series = variar(seed, `${key}|s`, rango.series[0], rango.series[1]);
  const repeticiones = variar(seed, `${key}|r`, rango.reps[0], rango.reps[1]);
  const descanso = variar(seed, `${key}|d`, rango.descanso[0], rango.descanso[1]);

  return {
    series,
    repeticiones,
    unidad: clasif.unidad || 'reps',
    descanso
  };
}

function contextoEquipamiento(equipamiento) {
  const eq = normalizarTexto(equipamiento);
  if (!eq || eq === 'variable' || eq === '-') return '';
  if (/barra/.test(eq)) return 'Agarre firme, muñecas neutras y control del recorrido.';
  if (/mancuerna|kettlebell/.test(eq)) return 'Mueve cada lado con simetría; evita impulso con la espalda.';
  if (/banda/.test(eq)) return 'Mantén tensión en todo el rango; no dejes que la banda te arrastre.';
  if (/polea|trx/.test(eq)) return 'Torso estable; no uses inercia del cable.';
  if (/maquina|prensa/.test(eq)) return 'Ajusta la máquina a tu anatomía antes de empezar.';
  if (/suelo|sin material/.test(eq)) return 'Busca un espacio despejado y superficie antideslizante.';
  if (/pista|cuesta|conos/.test(eq)) return 'Calienta 5-10 min; prioriza técnica sobre velocidad al inicio.';
  if (/piscina/.test(eq)) return 'Hidrátate; técnica y respiración rítmica en todo el bloque.';
  return `Material: ${equipamiento}. Adapta la carga a tu nivel manteniendo técnica limpia.`;
}

function textoMovimiento(nombre, pilar, grupo, equipamiento) {
  const n = normalizarTexto(nombre);
  const g = normalizarTexto(grupo);
  const pil = EntornoEntreno.normalizarPilar(pilar);

  if (/dominada.*asistid/.test(n)) {
    return 'Colgado en barra prono. Coloca la banda bajo rodillas o pies para asistir. Sube hasta barbilla sobre la barra sin balanceo y baja 2-3 s. Reduce ayuda de la banda cuando progreses.';
  }
  if (/dominada.*explosiv|muscle-up/.test(n)) {
    return 'Genera impulso controlado desde hombros y cadera. Pecho a la barra con técnica limpia. Descansa bien entre series para mantener explosividad.';
  }
  if (/dominada|towel pull|chin-up/.test(n)) {
    return 'Escápulas activas antes de tirar. Sube hasta barbilla o pecho a la barra sin kipping. Baja controlando; no dejes caer los hombros.';
  }
  if (/press banca|bench/.test(n)) {
    return 'Escápulas retraídas en el banco. Baja la carga al pecho con codos ~45° y empuja en línea recta. Pies firmes en el suelo, glúteos activos.';
  }
  if (/press inclinado|press declinado/.test(n)) {
    return 'Ajusta el banco al ángulo indicado. Controla la bajada 2 s y empuja sin rebotar. Mantén muñecas alineadas con antebrazos.';
  }
  if (/flexion.*diamante|flexion.*archer|flexion.*palmada|flexion.*planche|flexion.*anillas/.test(n)) {
    return 'Manos en la posición indicada, cuerpo en línea. Baja el pecho controlando y sube sin perder alineación de cadera. Progresión exigente: prioriza calidad.';
  }
  if (/flexion|push-up/.test(n)) {
    return 'Manos bajo hombros, abdomen y glúteos activos. Baja el pecho al suelo y sube extendiendo codos. Cuello neutro, mirada al suelo.';
  }
  if (/fondos|dip/.test(n)) {
    return 'Hombros abajo y atrás al inicio. Baja hasta ~90° en codos sin dolor en hombro. Sube con control; no bloquees bruscamente arriba.';
  }
  if (/press militar|arnold|push press|cuban press|handstand|pike push/.test(n)) {
    return 'Empuje vertical con core firme. No arquees lumbar; glúteos y abdomen activos. Baja hasta barbilla/cuello con codos controlados.';
  }
  if (/elevacion lateral|elevacion frontal|pájaros|y-raise|face pull/.test(n)) {
    return 'Codos ligeramente flexionados. Eleva hasta altura de hombros sin impulso. Pausa 1 s arriba y baja lento; deltoides hace el trabajo.';
  }
  if (/remo invertido|australian|remo en mesa/.test(n)) {
    return 'Cuerpo recto bajo barra o mesa. Tira el pecho hacia la barra apretando omóplatos. No hundas cadera ni gires el tronco.';
  }
  if (/remo|jalon|jalon|pullover/.test(n)) {
    return 'Torso estable, pecho alto. Tira hacia el abdomen bajo o pecho según el ejercicio. Aprieta omóplatos al final sin balancear.';
  }
  if (/sentadilla|prensa|hack squat|sissy squat/.test(n)) {
    return 'Pies bien apoyados, pecho alto. Baja controlando hasta profundidad segura y sube empujando el suelo. Rodillas alineadas con pies.';
  }
  if (/step-up|step up/.test(n)) {
    return 'De pie frente a cajón o escalón estable. Sube con todo el pie en el escalón, empuja con cuádriceps y glúteo hasta extender cadera. Baja controlado con la misma pierna.';
  }
  if (/zancada|split squat|cossack/.test(n)) {
    return 'Paso largo y torso erguido. Rodilla delantera sigue la punta del pie. Empuja con la pierna de trabajo para volver.';
  }
  if (/peso muerto|good morning/.test(n)) {
    return 'Bisagra de cadera con espalda neutra. Barra cerca del cuerpo. Empuja el suelo con piernas y extiende cadera al subir.';
  }
  if (/hip thrust|puente de gluteo|puente gluteo|patada de gluteo/.test(n)) {
    return 'Espalda apoyada, pies firmes. Eleva cadera apretando glúteos arriba sin hiperextender lumbar. Pausa 1 s en la cima.';
  }
  if (/gemelo|curl femoral|nordic|extension de cuadriceps/.test(n)) {
    return 'Movimiento controlado en todo el rango. Pausa breve en la contracción máxima. Carga moderada: la técnica no debe romperse.';
  }
  if (/reverse wrist/.test(n)) {
    return 'Antebrazos en muslos, palmas abajo. Extiende muñecas hacia arriba sin mover codos. Carga ligera, rango completo.';
  }
  if (/wrist curl/.test(n)) {
    return 'Palmas arriba, antebrazos apoyados. Flexiona muñecas subiendo el peso y baja en 2 s. Solo se mueven las muñecas.';
  }
  if (/curl|martillo/.test(n)) {
    return 'Codos fijos al costado. Sube sin balancear el tronco y baja controlando 2 s. Aprieta en la contracción máxima.';
  }
  if (/triceps|tríceps|press francés|patada de triceps|extension.*brazo/.test(n)) {
    return 'Codos apuntan al techo o quedan pegados al cuerpo según variante. Extiende antebrazos sin abrir codos. Controla la fase excéntrica.';
  }
  if (/plancha(?!.*reach)/.test(n) || /hollow|l-sit|farmer hold|wall sit/.test(n)) {
    return 'Cuerpo alineado, abdomen y glúteos activos. Respira sin perder posición. Si la cadera cae, para y reinicia la serie.';
  }
  if (/dead bug|bird dog|pallof|suitcase|woodchop|bear crawl/.test(n)) {
    return 'Movimiento lento y controlado. Resiste rotación o extensión lumbar. Calidad del patrón por encima de velocidad.';
  }
  if (/crunch|v-up|bicycle|russian twist|elevacion de pierna|ab wheel|mountain climber/.test(n)) {
    return 'Inicia el movimiento desde el abdomen, no del cuello. Exhala en el esfuerzo. Si lumbar se levanta, reduce rango.';
  }
  if (/burpee|thruster|devil press|wall ball|kettlebell swing|circuito/.test(n)) {
    return 'Ritmo sostenible según series. Técnica limpia en cada repetición; para si la forma se deteriora.';
  }
  if (/sprint|arranque|skipping/.test(n)) {
    return 'Calienta bien. Acelera progresivamente y mantén técnica de carrera (rodilla alta, brazos activos). Descanso completo entre series.';
  }
  if (/salt|pliometr|box jump|salto|cmj/.test(n)) {
    return 'Aterrizaje suave con rodillas flexionadas. Usa brazos para impulso. Series cortas y explosivas, descanso amplio.';
  }
  if (/agilidad|conos|carioca|vallas|escalera/.test(n)) {
    return 'Cadera baja, pies rápidos y ligeros. Mira el recorrido, no los pies. Cambios de dirección con control.';
  }
  if (/carrera continua|trote|rodaje|z2|marcha|rucking/.test(n)) {
    return 'Ritmo aeróbico: puedes hablar en frases cortas (RPE 4-6). Postura erguida, pisada media. Hidrátate.';
  }
  if (/fartlek|hiit|interval|tempo|cambios de ritmo/.test(n)) {
    return 'Alterna tramos fuertes y suaves según prescripción. No arranques demasiado rápido; distribuye el esfuerzo.';
  }
  if (/natac/.test(n)) {
    return 'Cuerpo horizontal, rotación controlada. Respiración rítmica; técnica de brazada limpia en todo el bloque.';
  }
  if (/bicicleta|eliptica|assault|remo erg|step mill|battle rope|comba/.test(n)) {
    return 'Intensidad según prescripción. Postura estable, core activo. Cadencia o ritmo constante salvo intervalos indicados.';
  }
  if (pil === 'MOVILIDAD' || /movilidad|estiramiento|stretch/.test(n)) {
    return 'Movimiento lento sin dolor. Respira profundo y aumenta rango progresivamente. No rebotes al final del estiramiento.';
  }
  if (pil === 'RESISTENCIA') {
    return 'Mantén intensidad moderada y constante. Respiración controlada; ritmo que puedas sostener toda la serie.';
  }
  if (pil === 'VELOCIDAD') {
    return 'Calidad y explosividad primero. Series cortas al máximo técnico, descanso completo entre ellas.';
  }
  if (g.includes('pecho')) {
    return 'Empuje controlado con escápulas estables. Codos en ángulo seguro (~45°). No rebotes en el punto de cambio.';
  }
  if (g.includes('espalda')) {
    return 'Inicia el tirón con omóplatos, no solo con brazos. Aprieta la espalda al final del recorrido.';
  }
  if (g.includes('pierna') || g.includes('gluteo')) {
    return 'Pies firmes, rodillas alineadas. Controla la bajada y empuja con intención en la subida.';
  }
  if (g.includes('hombro')) {
    return 'Hombros abajo y atrás al inicio. Rango sin dolor; carga que puedas controlar en todo el movimiento.';
  }
  if (g.includes('brazo')) {
    return 'Codos estables, movimiento solo en codo o muñeca según el ejercicio. Sin balanceo del tronco.';
  }
  if (g.includes('core')) {
    return 'Activa abdomen y glúteos antes de moverte. Evita que lumbar se despegue o arquee.';
  }
  return null;
}

function expandirPistaBanco(banco, nombre) {
  const t = String(banco || '').trim();
  if (!t || meta().esInstruccionGenerica(t)) return null;
  if (t.length >= 80) return t;
  return `Detalle técnico: ${t.replace(/\.$/, '')} en cada repetición de ${nombre}.`;
}

function generarInstrucciones(ej) {
  const nombre = meta().normalizarNombreEjercicio(ej.nombre);
  const pilar = ej.pilar || ej.categoria || 'FUERZA';
  const grupo = meta().inferirGrupoMuscular(ej.grupo_muscular, nombre, pilar);
  const equip = ej.equipamiento || '';

  const nucleo =
    textoMovimiento(nombre, pilar, grupo, equip) ||
    meta().instruccionesDesdeNombre(nombre, pilar);

  const pista = expandirPistaBanco(ej.instrucciones_tecnicas, nombre);
  const equipo = contextoEquipamiento(equip);

  const partes = [nucleo, pista, equipo].filter(Boolean);
  if (partes.length) return partes.join(' ');

  return `Ejecuta ${nombre} con técnica controlada, rango completo y carga acorde a tu nivel. Descansa lo indicado entre series.`;
}

/** Aplica prescripción e instrucciones inteligentes al ejercicio. */
function aplicarInteligencia(ej, ctx = {}) {
  const nombre = meta().normalizarNombreEjercicio(ej.nombre);
  const prescripcion = generarPrescripcion({ ...ej, nombre }, ctx);
  const instrucciones = generarInstrucciones({ ...ej, nombre });
  const grupo = meta().inferirGrupoMuscular(ej.grupo_muscular, nombre, ej.pilar || ej.categoria);

  return {
    ...ej,
    nombre,
    grupo_muscular: grupo,
    series: prescripcion.series,
    repeticiones: prescripcion.repeticiones,
    unidad: prescripcion.unidad,
    descanso: prescripcion.descanso ?? ej.descanso ?? 90,
    instrucciones_tecnicas: instrucciones,
    prescripcion_inteligente: true
  };
}

module.exports = {
  clasificarPerfil,
  parsearPrescripcionNombre,
  generarPrescripcion,
  generarInstrucciones,
  aplicarInteligencia,
  variar
};
