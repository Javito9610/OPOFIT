/**
 * Normaliza nombres, infiere grupo muscular e instrucciones reales para ejercicios.
 */
const EntornoEntreno = require('../utils/EntornoEntreno');
const EjercicioInteligenteService = require('./EjercicioInteligenteService');

const INSTRUCCION_GENERICAS = [
  /^técnica controlada/i,
  /^prescripción del banco opofit/i,
  /^ejercicio del banco opofit/i,
  /^extensión completa\.?$/i,
  /^extensores de muñeca\.?$/i,
  /^flexores de muñeca\.?$/i,
  /^agarre oposición\.?$/i,
  /^cuerpo inclinado, codos fijos\.?$/i,
  /^antebrazos?\.?$/i
];

function normalizarNombreEjercicio(nombre) {
  let n = String(nombre || '').trim();
  if (!n) return n;
  // "Fuerza inicial tren superior: Dominadas asistidas con goma" -> "Dominadas asistidas con goma"
  const idx = n.lastIndexOf(':');
  if (idx > 0 && idx < n.length - 2) {
    const tail = n.slice(idx + 1).trim();
    if (tail.length >= 4 && tail.length < n.length * 0.85) n = tail;
  }
  // Quitar prefijos de sesión frecuentes
  n = n.replace(
    /^(fuerza inicial tren superior|fuerza|resistencia|velocidad|cardio|core)\s*:\s*/i,
    ''
  );
  return n.trim().slice(0, 200);
}

function inferirGrupoMuscular(grupo, nombre, pilar) {
  const g = String(grupo || '').trim();
  if (g && g.toLowerCase() !== 'general' && g.toLowerCase() !== 'variable') return g;
  const n = String(nombre || '').toLowerCase();
  const pil = String(pilar || '').toUpperCase();
  if (pil === 'RESISTENCIA' || pil === 'VELOCIDAD') return 'Cardio';
  if (pil === 'CORE') return 'Core';
  if (/dominada|remo|jalón|pull|face pull|australian|muscle-up/.test(n)) return 'Espalda';
  if (/press|flexion|fondos|bench|pecho|aperturas/.test(n)) return 'Pecho';
  if (/sentadilla|zancada|step-up|step up|prensa|pierna|gemelo|lunges|hip thrust|peso muerto/.test(n))
    return 'Pierna';
  if (/curl|bíceps|tríceps|martillo/.test(n)) return 'Brazos';
  if (/militar|hombro|elevaciones laterales|face pull/.test(n)) return 'Hombros';
  if (/plancha|hollow|pallof|ab wheel|crunch|core/.test(n)) return 'Core';
  if (/carrera|rodaje|natac|sprint|fartlek|burpee|bicicleta/.test(n)) return 'Cardio';
  if (/glúteo|gluteo/.test(n)) return 'Glúteo';
  return g || 'General';
}

function instruccionesDesdeNombre(nombre, pilar) {
  const n = String(nombre || '').toLowerCase();
  const pil = String(pilar || '').toUpperCase();

  if (/dominada/.test(n)) {
    return 'Colgado en barra con agarre prono, escápulas activas. Sube hasta barbilla por encima de la barra sin balanceo. Si usas goma, apóyala en rodillas o pies para asistir el movimiento. Baja controlando 2-3 s.';
  }
  if (/step-up|step up/.test(n)) {
    return 'De pie frente a cajón o escalón estable. Sube con una pierna apoyando todo el pie, empuja con cuádriceps y glúteo hasta extender cadera y rodilla. Baja controlado con la misma pierna. Alterna o completa series por lado.';
  }
  if (/press banca|press de banca/.test(n)) {
    return 'Espalda apoyada, escápulas retraídas. Baja la barra o mancuernas al pecho con codos a ~45°. Empuja en línea recta sin rebotar en el pecho.';
  }
  if (/sentadilla/.test(n)) {
    return 'Pies ancho de hombros, pecho alto. Baja flexionando cadera y rodillas hasta muslos paralelos o más. Rodillas alineadas con puntas de pies. Sube empujando el suelo con mid-foot.';
  }
  if (/flexion|flexión|push-up/.test(n)) {
    return 'Manos bajo hombros, cuerpo en línea cabeza-talones. Baja el pecho controlando y sube extendiendo codos. Core activo en todo el movimiento.';
  }
  if (/plancha/.test(n)) {
    return 'Antebrazos o manos en el suelo, cuerpo recto de cabeza a talones. Activa abdomen y glúteos. No dejes caer cadera ni eleves demasiado.';
  }
  if (/zancada|lunge/.test(n)) {
    return 'Paso largo hacia delante, baja la rodilla trasera hacia el suelo. Torso erguido, rodilla delantera no sobrepasa mucho la punta del pie. Empuja para volver.';
  }
  if (/remo invertido|remo en mesa|australian/.test(n)) {
    return 'Cuerpo recto bajo una mesa o barra baja. Agarre prono, pecho hacia la barra manteniendo línea cabeza-talones. Tira con codos hacia atrás apretando omóplatos.';
  }
  if (/remo/.test(n)) {
    return 'Torso estable, core activo. Tira del peso hacia el abdomen bajo, apretando omóplatos al final. Evita balancear el tronco para ayudarte.';
  }
  if (/fondos|dip/.test(n)) {
    return 'Manos en paralelas o sillas, codos hacia atrás. Baja hasta 90° en codos sin hundir hombros. Sube extendiendo brazos con control.';
  }
  if (/press militar|militar/.test(n)) {
    return 'Mancuernas o barra a la altura de hombros. Empuja vertical sin arquear lumbar. Baja hasta barbilla/cuello con codos ligeramente delante del cuerpo.';
  }
  if (/reverse wrist|wrist curl|curl.*(muñeca|muneca)|muñeca.*curl/.test(n)) {
    if (/reverse/.test(n)) {
      return 'Antebrazos apoyados en muslos o banco, palmas abajo. Deja caer el peso flexionando muñecas y extiende hacia arriba sin mover codos. Carga ligera, 8-15 repeticiones controladas.';
    }
    return 'Antebrazos en muslos, palmas arriba. Flexiona muñecas subiendo el peso y baja en 2 s. Codos quietos; solo se mueven las muñecas.';
  }
  if (/curl/.test(n)) {
    return 'Codos pegados al tronco, sube el peso sin balancear. Aprieta bíceps arriba y baja controlando 2 s.';
  }
  if (/tríceps|extension.*triceps/.test(n)) {
    return 'Codos fijos, extiende antebrazos sin abrir codos. Controla la bajada y evita rebotes.';
  }
  if (/hip thrust|puente de glúteo|puente gluteo/.test(n)) {
    return 'Espalda alta en banco o suelo, pies apoyados. Eleva cadera apretando glúteos arriba sin hiperextender lumbar. Baja sin tocar del todo.';
  }
  if (/peso muerto/.test(n)) {
    return 'Pies ancho caderas, barra cerca de tibias. Pecho alto, empuja el suelo con piernas y extiende cadera. Espalda neutra en todo el recorrido.';
  }
  if (/burpee/.test(n)) {
    return 'De pie a suelo: manos al suelo, salta pies atrás a plancha, flexión opcional, vuelve y salta vertical con brazos arriba. Ritmo constante según series.';
  }
  if (/mountain climber/.test(n)) {
    return 'En plancha alta, lleva rodillas al pecho alternando rápido sin elevar cadera. Core activo y hombros sobre manos.';
  }
  if (/carrera|rodaje|trote/.test(n)) {
    return 'Ritmo aeróbico controlado: puedes hablar en frases cortas. Postura erguida, pisada media, brazos relajados. Hidrátate y calienta 5-10 min antes.';
  }
  if (/sprint|velocidad/.test(n)) {
    return 'Calienta bien. Series cortas al máximo controlado, técnica de carrera limpia (rodilla alta, impulso con brazos). Descanso completo entre series.';
  }
  if (/natac/.test(n)) {
    return 'Técnica de crol o estilo indicado: cuerpo horizontal, rotación controlada, respiración rítmica. Mantén ritmo constante según series prescritas.';
  }
  if (pil === 'RESISTENCIA') {
    return 'Mantén intensidad moderada y constante según la prescripción. Respiración controlada y técnica limpia durante todo el bloque.';
  }
  if (pil === 'VELOCIDAD') {
    return 'Explosividad con buena técnica. Calidad del movimiento por encima de cantidad en series de velocidad.';
  }
  return null;
}

function esInstruccionGenerica(instr) {
  const t = String(instr || '').trim();
  if (!t || t.length < 28) return true;
  if (INSTRUCCION_GENERICAS.some((re) => re.test(t))) return true;
  const palabras = t.split(/\s+/).length;
  if (palabras < 6 && !/[.;]/.test(t.slice(1))) return true;
  return false;
}

function enriquecerInstrucciones(nombre, pilar, instruccionesActuales, ejExtra = {}) {
  return EjercicioInteligenteService.generarInstrucciones({
    nombre,
    pilar,
    instrucciones_tecnicas: instruccionesActuales,
    ...ejExtra
  });
}

function motivoAjusteLegible(motivo) {
  if (!motivo) return null;
  const partes = String(motivo)
    .split('·')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((m) => {
      if (m === 'mantenimiento') return null;
      if (m === 'recuperacion') return 'Volumen ligeramente reducido para recuperarte mejor esta semana';
      if (m === 'racha activa') return 'Pequeño aumento por tu buena racha de entrenos';
      if (m.startsWith('prioridad ')) {
        const p = m.replace('prioridad ', '');
        return `Más volumen en ${p} porque es un punto a reforzar según tus marcas`;
      }
      if (m.startsWith('sesion ')) return null;
      return m.charAt(0).toUpperCase() + m.slice(1);
    })
    .filter(Boolean);
  return partes.length ? partes.join('. ') + '.' : null;
}

function enriquecerEjercicio(ej) {
  const nombreLimpio = normalizarNombreEjercicio(ej.nombre);
  const pilar = ej.pilar || ej.categoria || 'FUERZA';
  const grupo = inferirGrupoMuscular(ej.grupo_muscular, nombreLimpio, pilar);
  const tipo =
    ej.tipo_ilustracion ||
    EntornoEntreno.inferirTipoIlustracion(nombreLimpio, pilar, grupo);
  const motivo = motivoAjusteLegible(ej.motivo_ajuste);
  return {
    ...ej,
    nombre: nombreLimpio,
    grupo_muscular: grupo,
    tipo_ilustracion: tipo,
    motivo_ajuste: motivo,
    nombre_original: ej.nombre_original
      ? normalizarNombreEjercicio(ej.nombre_original)
      : ej.nombre_original
  };
}

function etiquetaEntorno(entorno) {
  const meta = EntornoEntreno.ENTORNO_META[entorno];
  return meta?.etiqueta || entorno;
}

function motivoSustitucion(entorno, grupo, nombreOriginal, nombreSustituto) {
  const lugar = etiquetaEntorno(entorno);
  const orig = normalizarNombreEjercicio(nombreOriginal);
  const sust = normalizarNombreEjercicio(nombreSustituto);
  if (!orig || !sust || orig.toLowerCase() === sust.toLowerCase()) return null;
  const g = grupo && grupo !== 'General' ? grupo.toLowerCase() : 'la misma zona muscular';
  return `En ${lugar} hacemos ${sust} en lugar de ${orig} (${g}).`;
}

module.exports = {
  normalizarNombreEjercicio,
  inferirGrupoMuscular,
  instruccionesDesdeNombre,
  esInstruccionGenerica,
  enriquecerInstrucciones,
  motivoAjusteLegible,
  enriquecerEjercicio,
  motivoSustitucion,
  etiquetaEntorno
};
