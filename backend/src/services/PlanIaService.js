/**
 * PlanIaService
 *
 * - generarCoaching(ctx)   → texto motivacional (3-4 frases).
 * - disenarSesion(ctx)     → diseña los ejercicios de UNA sesión a partir del catálogo,
 *                            siguiendo principios científicos (banco_planes v1).
 * - disenarSemana(ctx)     → diseña los 4-5 días del microciclo (orquesta disenarSesion).
 *
 * Si hay OPENAI_API_KEY / GEMINI_API_KEY, la IA elige IDs concretos del catálogo
 * proporcionado (no inventa nombres). Si no hay clave, el fallback experto
 * construye la sesión con reglas (sobrecarga progresiva por nivel, 80/20 Seiler,
 * pilometría progresiva NSCA, factor por pilar débil, etc.).
 */
const EntornoEntreno = require('../utils/EntornoEntreno');
const Calentamiento = require('./CalentamientoService');
const Patron = require('./PatronMovimientoService');
const Periodizacion = require('./PeriodizacionService');

// Traducción material código → descripción humana para el prompt de la IA.
const MATERIAL_LABEL = {
  NADA: 'solo peso corporal',
  BARRA_DOMINADAS: 'barra de dominadas',
  BARRA_OLIMPICA: 'barra olímpica con discos',
  MANCUERNAS: 'mancuernas',
  KB: 'kettlebells',
  TRX: 'TRX',
  ANILLAS: 'anillas',
  GOMAS: 'gomas elásticas',
  COMBA: 'comba',
  SACO: 'saco de boxeo',
  FOAM: 'foam roller',
  BANCO: 'banco regulable',
  CAJA: 'caja pliométrica',
  BICI: 'bicicleta',
  REMO: 'remoergómetro',
  ECHO_BIKE: 'echo bike / assault bike',
  SKI_ERG: 'ski erg',
  PISCINA: 'piscina',
  PISTA: 'pista de atletismo',
  MONTANA: 'montaña / trail',
  GIMNASIO_COMPLETO: 'gimnasio completo (todo el material habitual)'
};

function mapearMaterialAHumano(materialArr) {
  if (!Array.isArray(materialArr) || materialArr.length === 0) {
    return 'NO especificado (asume solo peso corporal)';
  }
  if (materialArr.includes('GIMNASIO_COMPLETO')) {
    return 'gimnasio completo (todo el material habitual disponible)';
  }
  const etiquetas = materialArr
    .map((m) => MATERIAL_LABEL[String(m).toUpperCase()] || null)
    .filter(Boolean);
  if (!etiquetas.length) return 'NO especificado (asume solo peso corporal)';
  return etiquetas.join(', ');
}

const NIVEL_VOLUMEN = {
  BASICO: { series: [3, 4], reps_fuerza: [8, 12], reps_max: 18, rpe: 6 },
  INTERMEDIO: { series: [4, 5], reps_fuerza: [6, 10], reps_max: 22, rpe: 7 },
  AVANZADO: { series: [5, 6], reps_fuerza: [3, 6], reps_max: 25, rpe: 8 }
};

const DESCANSO_BASE = {
  FUERZA: { BASICO: 90, INTERMEDIO: 120, AVANZADO: 180 },
  VELOCIDAD: { BASICO: 90, INTERMEDIO: 120, AVANZADO: 150 },
  RESISTENCIA: { BASICO: 60, INTERMEDIO: 75, AVANZADO: 90 },
  CORE: { BASICO: 45, INTERMEDIO: 60, AVANZADO: 60 },
  MOVILIDAD: { BASICO: 30, INTERMEDIO: 45, AVANZADO: 45 }
};

const SESIONES_BLOQUES = {
  FUERZA: 5,
  RESISTENCIA: 3,
  VELOCIDAD: 4,
  CORE: 4,
  MOVILIDAD: 4
};

function fallbackCoaching(ctx) {
  const {
    entorno, resumen, pilaresDebiles, pilaresFuertes, dias,
    rachaDias, sesionesSemana, nivel, ultimasSesiones, diasEntrenoSemana
  } = ctx;
  const meta = EntornoEntreno.ENTORNO_META[entorno] || EntornoEntreno.ENTORNO_META.MIXTO;
  const partes = [
    `Plan adaptado para entrenar en ${meta.etiqueta.toLowerCase()} ${meta.emoji}.`,
    resumen || 'Priorizamos tus puntos débiles según el baremo.'
  ];
  if (nivel) partes.push(`Nivel de referencia: ${nivel}.`);
  if (Number(diasEntrenoSemana) >= 1 && Number(diasEntrenoSemana) <= 7) {
    partes.push(`Microciclo de ${diasEntrenoSemana} día(s) de entreno según tu disponibilidad.`);
  }
  if (Number(rachaDias) > 0) {
    partes.push(`Llevas ${rachaDias} día(s) de racha — mantén el ritmo.`);
  } else if (Number(sesionesSemana) > 0) {
    partes.push(`Esta semana ya has completado ${sesionesSemana} sesión(es).`);
  }
  if (pilaresDebiles?.length) {
    const d = pilaresDebiles[0];
    partes.push(
      `Refuerza ${d.etiqueta || d.pilar} (${d.pruebas?.slice(0, 2).join(', ') || 'pruebas clave'}).`
    );
  }
  if (pilaresFuertes?.length) {
    const f = pilaresFuertes[0];
    partes.push(`Mantén ${f.etiqueta || f.pilar}, donde ya vas fuerte.`);
  }
  if (dias?.length) {
    const focos = [...new Set(dias.map((d) => d.enfoque))].slice(0, 3);
    partes.push(`Microciclo: ${focos.join(' · ').toLowerCase()}.`);
  }
  if (ultimasSesiones?.length) {
    const ult = ultimasSesiones[0];
    if (ult?.fecha) {
      partes.push(`Última sesión: ${ult.fecha}${ult.tipo_rutina ? ` (${ult.tipo_rutina})` : ''}.`);
    }
  }
  partes.push('Si un ejercicio no encaja, pulsa «Generar otra semana».');
  return partes.join(' ');
}

async function llamarOpenAI(apiKey, prompt, opts = {}) {
  const body = {
    model: process.env.OPENAI_MODEL || 'gpt-4o-mini',
    messages: [
      {
        role: 'system',
        content: opts.systemPrompt ||
          'Eres un preparador físico de oposiciones en España. Responde en español, máximo 4 frases, motivador y concreto. No inventes ejercicios ni cargas fuera del JSON.'
      },
      { role: 'user', content: prompt }
    ],
    max_tokens: opts.maxTokens || 220,
    temperature: opts.temperature ?? 0.7
  };
  if (opts.json) body.response_format = { type: 'json_object' };
  const res = await fetch('https://api.openai.com/v1/chat/completions', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  });
  if (!res.ok) throw new Error(`OpenAI ${res.status}`);
  const data = await res.json();
  return data.choices?.[0]?.message?.content?.trim() || null;
}

async function llamarGemini(apiKey, prompt, opts = {}) {
  const model = process.env.GEMINI_MODEL || 'gemini-2.0-flash';
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
  const generationConfig = {
    maxOutputTokens: opts.maxTokens || 220,
    temperature: opts.temperature ?? 0.7
  };
  if (opts.json) generationConfig.responseMimeType = 'application/json';
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig
    })
  });
  if (!res.ok) throw new Error(`Gemini ${res.status}`);
  const data = await res.json();
  return data.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || null;
}

/**
 * Llamada a Groq (https://groq.com).
 *
 * ¿Por qué Groq?
 *   ✅ GRATIS. Tier free generoso (~14.400 requests/día con Llama 3.3 70B).
 *   ✅ NO requiere tarjeta de crédito al registrarte.
 *   ✅ ULTRARÁPIDO (LPU hardware) — 5-10x más rápido que OpenAI/Claude.
 *   ✅ Soporta Llama 3.3 70B — calidad muy cercana a GPT-4 / Claude 3.5
 *      Sonnet, especialmente buena en prompts estructurados como el v14.
 *   ✅ API compatible con OpenAI (Chat Completions).
 *
 * Setup:
 *   1. Crear cuenta gratuita en https://console.groq.com (con email o GitHub).
 *   2. Crear API key (botón "API Keys" en el dashboard).
 *   3. Añadir al .env:
 *        GROQ_API_KEY=gsk_...
 *        GROQ_MODEL=llama-3.3-70b-versatile (default, opcional)
 *
 * Sin clave, el código cae a Claude → OpenAI → Gemini → reglas.
 */
async function llamarGroq(apiKey, prompt, opts = {}) {
  const model = process.env.GROQ_MODEL || 'llama-3.3-70b-versatile';
  const body = {
    model,
    messages: [
      { role: 'system', content: opts.systemPrompt ||
        'Eres un preparador físico CSCS / NSCA. Devuelves siempre JSON válido.' },
      { role: 'user', content: prompt }
    ],
    max_tokens: opts.maxTokens || 1200,
    temperature: opts.temperature ?? 0.4
  };
  if (opts.json) body.response_format = { type: 'json_object' };
  const res = await fetch('https://api.groq.com/openai/v1/chat/completions', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  });
  if (!res.ok) throw new Error(`Groq ${res.status}`);
  const data = await res.json();
  return data.choices?.[0]?.message?.content?.trim() || null;
}

/**
 * Llamada a la API de Anthropic (Claude).
 *
 * Claude tiene la ventaja de seguir prompts largos y estructurados (como el
 * v14 con bloques de ciencia y reglas de safety) mucho mejor que otros LLMs,
 * y es el más estable produciendo JSON válido sin desvíos de formato.
 *
 * Setup:
 *   1. Crear cuenta en https://console.anthropic.com
 *   2. Generar API key.
 *   3. Añadir al .env:
 *        CLAUDE_API_KEY=sk-ant-api03-...
 *        CLAUDE_MODEL=claude-3-5-sonnet-20241022  (opcional)
 *
 * Si no se configura CLAUDE_API_KEY, el código cae a OpenAI → Gemini → reglas.
 */
async function llamarClaude(apiKey, prompt, opts = {}) {
  const model = process.env.CLAUDE_MODEL || 'claude-3-5-sonnet-20241022';
  const body = {
    model,
    max_tokens: opts.maxTokens || 800,
    temperature: opts.temperature ?? 0.4,
    system: opts.systemPrompt ||
      'Eres un preparador físico CSCS / NSCA. Devuelves siempre JSON válido.',
    messages: [{ role: 'user', content: prompt }]
  };
  const res = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST',
    headers: {
      'x-api-key': apiKey,
      'anthropic-version': '2023-06-01',
      'content-type': 'application/json'
    },
    body: JSON.stringify(body)
  });
  if (!res.ok) throw new Error(`Claude ${res.status}`);
  const data = await res.json();
  return data.content?.[0]?.text?.trim() || null;
}

/** Decide cuántos bloques y volumen por enfoque, según nivel y pilares débiles. */
function plantillaSesion(enfoque, nivel, pilaresDebiles = []) {
  const pilar = EntornoEntreno.normalizarPilar(enfoque || 'FUERZA');
  const niv = NIVEL_VOLUMEN[nivel] || NIVEL_VOLUMEN.INTERMEDIO;
  const debilSet = new Set((pilaresDebiles || []).map((p) => p.pilar));
  const bloques = SESIONES_BLOQUES[pilar] || 4;
  const refuerzo = debilSet.has(pilar);
  return {
    pilar,
    bloques: refuerzo ? bloques + 1 : bloques,
    seriesRange: niv.series,
    repsRange: niv.reps_fuerza,
    repsMax: niv.reps_max,
    descansoBase: (DESCANSO_BASE[pilar] || DESCANSO_BASE.FUERZA)[nivel] || 75,
    rpeObjetivo: niv.rpe,
    refuerzo
  };
}

function pickByPilar(catalogo, pilarObjetivo, seedKey, usados) {
  const candidatos = catalogo.filter(
    (e) =>
      EntornoEntreno.normalizarPilar(e.pilar) === pilarObjetivo &&
      !usados.has(e.id_ejercicio)
  );
  if (!candidatos.length) {
    // No hay nada del pilar objetivo aún sin usar → permitir repetir
    const repe = catalogo.filter(
      (e) => EntornoEntreno.normalizarPilar(e.pilar) === pilarObjetivo
    );
    if (!repe.length) return null;
    return EntornoEntreno.seededPick(repe, 0, seedKey);
  }
  return EntornoEntreno.seededPick(candidatos, 0, seedKey);
}

function fallbackSesion(ctx) {
  const { enfoque, nivel = 'INTERMEDIO', pilaresDebiles = [], catalogo = [], seed = 1 } = ctx;
  const tpl = plantillaSesion(enfoque, nivel, pilaresDebiles);
  const usados = new Set();
  const ejercicios = [];

  // Ordenamos: pilar principal de la sesión + 1-2 accesorios (pilar débil si lo hay)
  const debilSet = new Set(pilaresDebiles.map((p) => p.pilar)).keys();
  const pilarDebil = [...debilSet].find((p) => p !== tpl.pilar) || null;

  for (let i = 0; i < tpl.bloques; i++) {
    const usarDebil = pilarDebil && (i === tpl.bloques - 1 || i === tpl.bloques - 2);
    const pilarBloque = usarDebil ? pilarDebil : tpl.pilar;
    const ej = pickByPilar(catalogo, pilarBloque, `${seed}|${i}|${pilarBloque}`, usados);
    if (!ej) continue;
    usados.add(ej.id_ejercicio);

    const seriesMin = tpl.seriesRange[0];
    const seriesMax = tpl.seriesRange[1];
    const repsMin = tpl.repsRange[0];
    const repsMax = tpl.repsRange[1];
    const series = seriesMin + ((seed + i * 7) % (seriesMax - seriesMin + 1));
    const reps =
      pilarBloque === 'RESISTENCIA' && /min|km/i.test(ej.nombre)
        ? null
        : repsMin + ((seed + i * 11) % (repsMax - repsMin + 1));

    ejercicios.push({
      id_ejercicio: ej.id_ejercicio,
      nombre: ej.nombre,
      pilar: ej.pilar,
      grupo_muscular: ej.grupo_muscular,
      series,
      repeticiones: reps,
      descanso: tpl.descansoBase,
      unidad: pilarBloque === 'RESISTENCIA' ? 'min' : 'reps',
      orden: i + 1,
      ia_origen: 'reglas'
    });
  }
  // Bloques pro de calentamiento (RAMP) y vuelta a la calma.
  // Añadidos siempre: incluso un fallback por reglas merece warmup específico.
  const calentamiento = Calentamiento.calentamiento(tpl.pilar);
  const vuelta_a_calma = Calentamiento.vueltaACalma(tpl.pilar);
  // Semana del mesociclo a partir del seed del usuario: cada usuario tiene
  // su propio ciclo 3:1 desplazado por su seed.
  const semana = Periodizacion.plantillaSemana(
    Periodizacion.semanaDelMesociclo(ctx.seed || 0)
  );
  // Auditoría de balance push/pull: si el fallback genera 3 push y 0 pull
  // exponemos el aviso en la propia sesión (los logs y el front lo verán).
  const balance = Patron.auditarBalance(ejercicios.map((e) => Patron.clasificar(e)));

  return {
    sesion: {
      enfoque: tpl.pilar,
      refuerzo_debil: tpl.refuerzo,
      ejercicios,
      calentamiento,
      vuelta_a_calma,
      fase_mesociclo: semana.fase,
      fase_label: semana.label,
      semana_idx: semana.idx,
      deload: semana.deload,
      balance_warnings: balance.warnings
    },
    fuente: 'reglas'
  };
}

class PlanIaService {
  static async generarCoaching(ctx) {
    const fb = fallbackCoaching(ctx);
    // Orden de preferencia: Groq (GRATIS + rápido) > Claude > OpenAI > Gemini.
    const groqKey   = process.env.GROQ_API_KEY;
    const claudeKey = process.env.CLAUDE_API_KEY;
    const openaiKey = process.env.OPENAI_API_KEY;
    const geminiKey = process.env.GEMINI_API_KEY;
    if (!groqKey && !claudeKey && !openaiKey && !geminiKey) {
      return { texto: fb, fuente: 'reglas' };
    }

    const meta = EntornoEntreno.ENTORNO_META[ctx.entorno] || EntornoEntreno.ENTORNO_META.MIXTO;
    const prompt = `Usuario opositor entrena en: ${meta.etiqueta}.
Nivel: ${ctx.nivel || 'no indicado'}
Días de entreno/semana elegidos: ${ctx.diasEntrenoSemana ?? 'no indicado'}
Racha actual: ${ctx.rachaDias ?? 0} días
Sesiones esta semana: ${ctx.sesionesSemana ?? 0}
Resumen motor: ${ctx.resumen || 'sin datos'}
Pilares débiles: ${JSON.stringify(ctx.pilaresDebiles || [])}
Pilares fuertes: ${JSON.stringify(ctx.pilaresFuertes || [])}
Últimas sesiones: ${JSON.stringify(ctx.ultimasSesiones || [])}
Días semana: ${JSON.stringify((ctx.dias || []).map((d) => ({ dia: d.nombre_dia, enfoque: d.enfoque, titulo: d.titulo })))}
Escribe 3-4 frases de coaching personalizado en español. Menciona racha o última sesión si aplica. No inventes ejercicios.`;

    try {
      let texto = null;
      let fuente = null;
      if (groqKey) {
        texto = await llamarGroq(groqKey, prompt, { maxTokens: 320, temperature: 0.7 });
        fuente = 'groq';
      } else if (claudeKey) {
        texto = await llamarClaude(claudeKey, prompt, { maxTokens: 220 });
        fuente = 'claude';
      } else if (openaiKey) {
        texto = await llamarOpenAI(openaiKey, prompt);
        fuente = 'openai';
      } else if (geminiKey) {
        texto = await llamarGemini(geminiKey, prompt);
        fuente = 'gemini';
      }
      if (texto && texto.length > 20) {
        return { texto, fuente };
      }
    } catch (err) {
      console.warn('[PlanIa]', err.message);
    }
    return { texto: fb, fuente: 'reglas' };
  }

  /**
   * Diseña la lista de ejercicios de UNA sesión.
   * @param {object} ctx
   * @param {string} ctx.enfoque  FUERZA | RESISTENCIA | VELOCIDAD | CORE | MOVILIDAD
   * @param {string} ctx.nivel    BASICO | INTERMEDIO | AVANZADO
   * @param {string} ctx.entorno  GYM | CASA | CROSSFIT | CALISTENIA | PISTA | MIXTO
   * @param {Array}  ctx.catalogo Lista ejercicios filtrados por entorno y enriquecidos.
   * @param {Array}  ctx.pilaresDebiles
   * @param {number} ctx.seed
   * @returns {Promise<{sesion: {enfoque, ejercicios:Array}, fuente:string}>}
   */
  static async disenarSesion(ctx) {
    // Orden de preferencia: Groq (GRATIS + Llama 3.3 70B) > Claude > OpenAI > Gemini.
    // Groq es la opción 100% gratuita recomendada: ~14k req/día con Llama 3.3,
    // y suele bastar para una app personal o un MVP.
    const groqKey   = process.env.GROQ_API_KEY;
    const claudeKey = process.env.CLAUDE_API_KEY;
    const openaiKey = process.env.OPENAI_API_KEY;
    const geminiKey = process.env.GEMINI_API_KEY;
    const useIA = (groqKey || claudeKey || openaiKey || geminiKey) && (ctx.usarIA !== false);

    // Sin clave → siempre reglas (ya devuelve estructura válida).
    if (!useIA) return fallbackSesion(ctx);

    // Limitamos el tamaño del catálogo enviado al LLM.
    const catalogo = (ctx.catalogo || []).slice(0, 80).map((e) => ({
      id: e.id_ejercicio,
      n: e.nombre,
      p: EntornoEntreno.normalizarPilar(e.pilar),
      gm: e.grupo_muscular || null,
      eq: e.equipamiento || null
    }));

    const meta = EntornoEntreno.ENTORNO_META[ctx.entorno] || EntornoEntreno.ENTORNO_META.MIXTO;
    const tpl = plantillaSesion(ctx.enfoque, ctx.nivel, ctx.pilaresDebiles);

    // System prompt mejorado v13 — estilo Freeletics / Centr / Future:
    // identidad de COACH personal con objetivo claro de la sesión, no
    // simple lista de ejercicios. El usuario debe sentir que "su coach"
    // diseña la sesión PARA él HOY.
    // Prompt v14 — perfil de preparador físico CSCS / NSCA. Más prescriptivo,
    // basado en literatura revisada por pares. Estructurado en bloques claros
    // para que el LLM (OpenAI/Claude/Gemini) lo siga sin desviarse.
    const systemPrompt = `Eres COACH OpoFit, preparador físico certificado CSCS (NSCA) con
formación en ACSM y especialización en preparación de oposiciones físicas
españolas. Tu output va a USUARIOS REALES que entrenan en su gym/casa hoy.

═══════════════════════════════════════════════════════════════════
  MISIÓN DE LA SESIÓN
═══════════════════════════════════════════════════════════════════
Diseña UNA sesión coherente con OBJETIVO declarado en 1 frase:
"hoy construimos fuerza máxima en tren inferior" o "hoy quemamos calorías
y trabajamos potencia con metcon corto". Esa frase explica el TODO de hoy.

Cada ejercicio incluye motivo_ia (1 frase) que dice por qué está HOY:
   - "te prepara la cadera antes del peso muerto"
   - "transfiere fuerza vertical para la prueba de salto"
   - "complementa tu pilar débil PULL"

═══════════════════════════════════════════════════════════════════
  CONTEXTO ESPECIALISTA
═══════════════════════════════════════════════════════════════════
• OPOSITOR (Policía Nac./Local, Guardia Civil, Bomberos, Mossos,
  Ertzaintza, Foral, Ejército): tu output debe APROBAR la prueba.
  Prioriza el pilar débil con +1 bloque dedicado.

• FITNESS: hipertrofia / fuerza / pérdida de grasa / resistencia
  (objetivo del usuario en \`ctx.objetivoFitness\`). Sin baremo policial.

═══════════════════════════════════════════════════════════════════
  PRINCIPIOS CIENTÍFICOS (literatura revisada)
═══════════════════════════════════════════════════════════════════
1. Sobrecarga progresiva — Schoenfeld 2017, NSCA Essentials 4ª ed.
   Series con RPE 7-9 + 1-3 RIR. Subida ~2.5-5% mensual en básicos.

2. Volume landmarks — Israetel 2019.
   MEV / MAV / MRV: tu sesión está en una semana del mesociclo 3:1.
   Semana 1 = MEV (base), 2 = MAV (+10%), 3 = MRV (+20%), 4 = DELOAD (-40%).

3. Polarización aeróbica — Seiler 2010.
   80% sesiones Z1-Z2 (conversacional, RPE 4-6).
   20% sesiones Z4-Z5 (umbral / VO2max, RPE 8-10).
   NO mezcles ambos en la misma sesión salvo HIIT estructurado.

4. Velocidad / potencia — Bompa & Buzzichelli 2019.
   Sprints ≤ 6 s al 100%. Descanso 1:10 a 1:20 (sprint 6s → 60-120s descanso).
   Pliometría: 3-5 series × 3-8 reps con descanso completo.

5. Concurrencia — Wilson et al. 2012.
   Separar fuerza máxima y resistencia ≥ 6 h. Mismo día solo si fuerza
   precede al cardio (interferencia menor).

6. Balance estructural — Janda / Cressey.
   Ratio push:pull ≥ 1:1, squat:hinge ≥ 1:1. Hombros y lumbar mueren
   sin esto. Es regla DURA.

═══════════════════════════════════════════════════════════════════
  REGLAS DE SAFETY (RECHAZAR el ejercicio si se incumple)
═══════════════════════════════════════════════════════════════════
• MATERIAL: NUNCA propongas un ejercicio cuyo equipamiento no esté en
  ctx.materialDisponible. Si el usuario solo tiene gomas+suelo, las
  dominadas y peso muerto NO existen para él. Sustituye por el patrón
  equivalente realizable:
    dominada → remo invertido con goma
    peso muerto → hip thrust con peso corporal o glute bridge a 1 pierna
    press banca → flexión con tempo lento 3-0-2-0
    sentadilla con barra → sentadilla goblet con mochila / split squat búlgaro

• LESIONES del usuario en ctx.lesiones[]:
    rodilla → NO pliometría, NO sentadilla profunda, NO sprint
    lumbar → NO peso muerto pesado, NO good morning, NO KB swing pesado
    hombro → NO press militar pesado, NO HSPU, NO dominadas con kipping
    tobillo → NO sprint, NO saltos, NO carrera de impacto
    codo → NO dominadas máximas, NO dips pesados
    muñeca → NO handstand, NO planche, NO ejercicio con muñeca extendida

• TIEMPO disponible (ctx.tiempoDisponibleMin):
    ≤ 30 min → 3-4 ejercicios, sin accesorios largos
    31-60 min → 5-6 ejercicios + core final
    > 60 min → hasta 8 ejercicios + accesorios

═══════════════════════════════════════════════════════════════════
  RANGOS DE PRESCRIPCIÓN VALIDADOS
═══════════════════════════════════════════════════════════════════
Fuerza máxima:      3-6 reps × 4-6 series @ RPE 8-9, descanso 150-180s, tempo 3-1-X-0
Hipertrofia:        6-12 reps × 3-4 series @ RPE 7-8, descanso 60-90s, tempo 2-0-1-0
Resistencia muscular: 12-20 reps × 2-3 series @ RPE 6-7, descanso 30-60s
Pliometría:         3-8 reps × 3-5 series @ RPE 7, descanso 120s (calidad, no fatiga)
Sprints:            6-10s @ 95-100%, 6-10 series, descanso 60-120s
Cardio Z2:          1×20-60 min @ RPE 4-6, descanso 0
Cardio intervalos:  4-8 × 1-4 min @ RPE 8-9, descanso 1:1 a 1:2

═══════════════════════════════════════════════════════════════════
  ESTRUCTURA DE LA SESIÓN
═══════════════════════════════════════════════════════════════════
1° — Ejercicio PRINCIPAL multiarticular pesado del pilar del día (SQUAT,
     HINGE, PUSH_V/H, PULL_V/H según enfoque).
2° — Complementario antagonista para balance (si push principal → pull).
3°-4° — Accesorios del pilar débil declarado en ctx.pilaresDebiles.
5°-6° — Core (anti-extensión, anti-rotación, carry).
ÚLTIMO — Movilidad / vuelta a la calma OPCIONAL (cardio Z1 5 min).

═══════════════════════════════════════════════════════════════════
  RESTRICCIONES TÉCNICAS DEL OUTPUT
═══════════════════════════════════════════════════════════════════
• Devuelves SOLO JSON válido. Sin markdown, sin texto antes/después.
• Cada ejercicio referencia el ID del catálogo en \`ctx.catalogo\`.
  ID inexistente = el sistema cae al fallback por reglas.
• motivo_ia ≤ 90 caracteres, 1 frase, en español natural.
• Si dudas entre 2 ejercicios, elige el MÁS SEGURO para el material
  declarado, NUNCA el que requiera más equipamiento.

Tu reputación: el usuario debe sentir que un coach humano experto le
diseñó esta sesión hoy. No genérica. No copiada.`;

    const debilesTxt = (ctx.pilaresDebiles || [])
      .map((d) => `${d.pilar}(nota:${d.notaMedia?.toFixed(1) ?? '?'})`)
      .join(', ') || 'ninguno';
    const fuertesTxt = (ctx.pilaresFuertes || [])
      .map((f) => f.pilar)
      .join(', ') || 'ninguno';

    // Material disponible del usuario: lo usa la IA para filtrar.
    const materialHumano = mapearMaterialAHumano(ctx.materialDisponible);
    // Periodización: semana del mesociclo 3:1. La IA usa esto para ajustar
    // volumen e intensidad (peak en MRV, descarga en DELOAD).
    const semanaMeso = Periodizacion.plantillaSemana(
      Periodizacion.semanaDelMesociclo(ctx.seed || 0)
    );
    const fasePromptLines = semanaMeso.deload
      ? `FASE DEL MESOCICLO: SEMANA ${semanaMeso.idx}/4 — DELOAD.
- Reduce 30-40% el volumen (menos series).
- Baja la intensidad ~10% (RPE 5-6).
- Sin pliometría máxima, sin sprints al 100%, sin AMRAPs duros.
- Objetivo: supercompensación, dejar al usuario fresco para el siguiente ciclo.`
      : `FASE DEL MESOCICLO: SEMANA ${semanaMeso.idx}/4 — ${semanaMeso.label.toUpperCase()}.
- Volumen relativo: ${(semanaMeso.vol * 100).toFixed(0)}% (base = 100%).
- Intensidad relativa: ${(semanaMeso.int * 100).toFixed(0)}%.
- RPE base: ${semanaMeso.rpe_base}/10. Progresión SEMANAL +5-10% volumen vs semana anterior.`;
    const prompt = `${fasePromptLines}

CONTEXTO DEL ASPIRANTE:
- Entorno entreno: ${meta.etiqueta} (${meta.descripcion || ''})
- Material disponible REAL: ${materialHumano}
- Nivel global: ${ctx.nivel || 'INTERMEDIO'}
- Pilares DÉBILES (prioritarios para refuerzo): ${debilesTxt}
- Pilares fuertes (mantenimiento): ${fuertesTxt}

SESIÓN A DISEÑAR:
- Enfoque principal: ${tpl.pilar}
- Bloques: ${tpl.bloques} ejercicios
- Series objetivo: ${tpl.seriesRange.join('-')}
- Reps objetivo: ${tpl.repsRange.join('-')}
- Descanso base: ${tpl.descansoBase}s
- RPE objetivo: ${tpl.rpeObjetivo}/10

CATÁLOGO DISPONIBLE (filtrado por entorno ${meta.etiqueta}):
${JSON.stringify(catalogo)}

DEVUELVE EXACTAMENTE ESTE JSON:
{"ejercicios":[
  {"id":<int>, "series":<int>, "reps":<int|null>, "descanso":<seg>, "unidad":"reps"|"min"|"km"|"s"|"m", "motivo":"<por qué este ejercicio para este aspirante>"}
]}

REGLAS DE COMPOSICIÓN:
1. Genera ${tpl.bloques} ejercicios SIN repetir id.
2. El PRIMERO debe ser un compuesto pesado del pilar ${tpl.pilar}.
3. Si hay pilares débiles, mete 1-2 accesorios que los ataquen (no más).
4. Si pilar principal es VELOCIDAD, incluye 1 bloque pliométrico.
5. Si pilar principal es RESISTENCIA, alterna serie larga + intervalos.
6. Si pilar principal es FUERZA, secuencia: pesado → moderado → accesorio.
7. Termina con CORE o MOVILIDAD si quedan bloques.
8. El "motivo" debe ser UNA frase que explique POR QUÉ este ejercicio para este perfil (ej: "Para reforzar dominadas que están débiles").`;

    try {
      let raw = null;
      let llmFuente = null;
      if (groqKey) {
        // Groq Llama 3.3 70B → GRATIS, rápido y suficiente para prompt v14.
        raw = await llamarGroq(groqKey, prompt, {
          systemPrompt,
          json: true,
          maxTokens: 1500,
          temperature: 0.4
        });
        llmFuente = 'groq';
      } else if (claudeKey) {
        // Claude → mejor opción para prompt v14 estructurado (CSCS).
        raw = await llamarClaude(claudeKey, prompt, {
          systemPrompt,
          maxTokens: 1200,
          temperature: 0.4
        });
        llmFuente = 'claude';
      } else if (openaiKey) {
        raw = await llamarOpenAI(openaiKey, prompt, {
          systemPrompt,
          json: true,
          maxTokens: 800,
          temperature: 0.5
        });
        llmFuente = 'openai';
      } else if (geminiKey) {
        raw = await llamarGemini(geminiKey, `${systemPrompt}\n\n${prompt}`, {
          json: true,
          maxTokens: 800,
          temperature: 0.5
        });
        llmFuente = 'gemini';
      }
      if (!raw) throw new Error('respuesta vacía');
      // Claude a veces devuelve el JSON envuelto en \`\`\`json … \`\`\` aunque pidas plain.
      // Lo limpiamos para que JSON.parse no rompa.
      raw = String(raw).replace(/^```(?:json)?\s*/i, '').replace(/```\s*$/i, '').trim();

      const parsed = JSON.parse(raw);
      const idMap = new Map((ctx.catalogo || []).map((e) => [e.id_ejercicio, e]));
      const ejerciciosValidados = (parsed.ejercicios || [])
        .map((e, idx) => {
          const cat = idMap.get(Number(e.id));
          if (!cat) return null;
          return {
            id_ejercicio: cat.id_ejercicio,
            nombre: cat.nombre,
            pilar: cat.pilar,
            grupo_muscular: cat.grupo_muscular,
            series: Math.min(8, Math.max(1, Math.round(Number(e.series) || 4))),
            repeticiones:
              e.reps == null ? null : Math.min(99, Math.max(1, Math.round(Number(e.reps)))),
            descanso: Math.min(300, Math.max(0, Math.round(Number(e.descanso) || tpl.descansoBase))),
            unidad: ['reps', 'min', 'km', 's', 'm'].includes(e.unidad) ? e.unidad : 'reps',
            orden: idx + 1,
            motivo_ia: typeof e.motivo === 'string' ? e.motivo.slice(0, 120) : null,
            ia_origen: openaiKey ? 'openai' : 'gemini'
          };
        })
        .filter(Boolean);

      if (ejerciciosValidados.length < Math.min(3, tpl.bloques)) {
        // Validación insuficiente → fallback
        return fallbackSesion(ctx);
      }
      // Balance push/pull: si la IA escupe algo desbalanceado, lo registramos.
      const balance = Patron.auditarBalance(
        ejerciciosValidados.map((e) => Patron.clasificar(e))
      );
      const calentamiento = Calentamiento.calentamiento(tpl.pilar);
      const vuelta_a_calma = Calentamiento.vueltaACalma(tpl.pilar);
      const semana = Periodizacion.plantillaSemana(
        Periodizacion.semanaDelMesociclo(ctx.seed || 0)
      );
      return {
        sesion: {
          enfoque: tpl.pilar,
          refuerzo_debil: tpl.refuerzo,
          ejercicios: ejerciciosValidados,
          calentamiento,
          vuelta_a_calma,
          fase_mesociclo: semana.fase,
          fase_label: semana.label,
          semana_idx: semana.idx,
          deload: semana.deload,
          balance_warnings: balance.warnings
        },
        fuente: llmFuente || (openaiKey ? 'openai' : 'gemini')
      };
    } catch (err) {
      console.warn('[PlanIa diseñarSesion]', err.message);
      return fallbackSesion(ctx);
    }
  }

  /**
   * Diseña el microciclo completo (4-5 días). Reutiliza disenarSesion día a día.
   */
  static async disenarSemana(ctx) {
    const dias = ctx.dias || [];
    const semana = [];
    let fuente = 'reglas';
    for (let i = 0; i < dias.length; i++) {
      const dia = dias[i];
      const { sesion, fuente: fSes } = await PlanIaService.disenarSesion({
        ...ctx,
        enfoque: dia.enfoque,
        seed: (ctx.seed || 1) + i * 13
      });
      if (fSes !== 'reglas') fuente = fSes;
      semana.push({
        ...dia,
        ejercicios: sesion.ejercicios,
        calentamiento: sesion.calentamiento || [],
        vuelta_a_calma: sesion.vuelta_a_calma || [],
        balance_warnings: sesion.balance_warnings || [],
        fase_mesociclo: sesion.fase_mesociclo,
        fase_label: sesion.fase_label,
        semana_idx: sesion.semana_idx,
        deload: sesion.deload || false
      });
    }
    return { semana, fuente };
  }
}

module.exports = PlanIaService;
