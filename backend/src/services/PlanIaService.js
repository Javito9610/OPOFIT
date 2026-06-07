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
    rachaDias, sesionesSemana, nivel, ultimasSesiones
  } = ctx;
  const meta = EntornoEntreno.ENTORNO_META[entorno] || EntornoEntreno.ENTORNO_META.MIXTO;
  const partes = [
    `Plan adaptado para entrenar en ${meta.etiqueta.toLowerCase()} ${meta.emoji}.`,
    resumen || 'Priorizamos tus puntos débiles según el baremo.'
  ];
  if (nivel) partes.push(`Nivel de referencia: ${nivel}.`);
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
  return {
    sesion: { enfoque: tpl.pilar, refuerzo_debil: tpl.refuerzo, ejercicios },
    fuente: 'reglas'
  };
}

class PlanIaService {
  static async generarCoaching(ctx) {
    const fb = fallbackCoaching(ctx);
    const openaiKey = process.env.OPENAI_API_KEY;
    const geminiKey = process.env.GEMINI_API_KEY;
    if (!openaiKey && !geminiKey) {
      return { texto: fb, fuente: 'reglas' };
    }

    const meta = EntornoEntreno.ENTORNO_META[ctx.entorno] || EntornoEntreno.ENTORNO_META.MIXTO;
    const prompt = `Usuario opositor entrena en: ${meta.etiqueta}.
Nivel: ${ctx.nivel || 'no indicado'}
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
      if (openaiKey) texto = await llamarOpenAI(openaiKey, prompt);
      else if (geminiKey) texto = await llamarGemini(geminiKey, prompt);
      if (texto && texto.length > 20) {
        return { texto, fuente: openaiKey ? 'openai' : 'gemini' };
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
    const openaiKey = process.env.OPENAI_API_KEY;
    const geminiKey = process.env.GEMINI_API_KEY;
    const useIA = (openaiKey || geminiKey) && (ctx.usarIA !== false);

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

    const systemPrompt = `Eres preparador físico de oposiciones (Policía, Guardia Civil, Bomberos, Ejército).
Diseñas UNA sesión semanal. Sigues principios: sobrecarga progresiva (Schoenfeld 2021),
polarización 80/20 (Seiler), pliometría progresiva (NSCA), descanso correcto entre series.
Devuelves SOLO JSON válido. NO inventas ejercicios: usas exclusivamente los IDs del catálogo proporcionado.`;

    const prompt = `Diseña una sesión de OPO para el siguiente contexto:
- Entorno: ${meta.etiqueta}
- Nivel: ${ctx.nivel || 'INTERMEDIO'}
- Enfoque principal: ${tpl.pilar} (${tpl.bloques} bloques, series ${tpl.seriesRange.join('-')}, reps ${tpl.repsRange.join('-')}, descanso ${tpl.descansoBase}s)
- Pilares débiles del aspirante: ${JSON.stringify((ctx.pilaresDebiles || []).map((d) => d.pilar))}
- Catálogo disponible (id, nombre, pilar, grupo_muscular, equipamiento): ${JSON.stringify(catalogo)}

Devuelve JSON:
{"ejercicios":[{"id":<int del catálogo>,"series":<int>,"reps":<int o null>,"descanso":<seg int>,"unidad":"reps"|"min"|"km"|"s"|"m","motivo":"<una frase>"}]}

Reglas:
- ${tpl.bloques} ejercicios, sin repetir id_ejercicio
- Empieza por el ejercicio más demandante del pilar principal
- Incluye 1-2 accesorios que ataquen los pilares débiles si los hay
- Series/reps coherentes con el nivel y el ejercicio (no pongas 6x20 dominadas)
- Descansos en segundos`;

    try {
      let raw = null;
      if (openaiKey) {
        raw = await llamarOpenAI(openaiKey, prompt, {
          systemPrompt,
          json: true,
          maxTokens: 800,
          temperature: 0.5
        });
      } else if (geminiKey) {
        raw = await llamarGemini(geminiKey, `${systemPrompt}\n\n${prompt}`, {
          json: true,
          maxTokens: 800,
          temperature: 0.5
        });
      }
      if (!raw) throw new Error('respuesta vacía');

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
      return {
        sesion: {
          enfoque: tpl.pilar,
          refuerzo_debil: tpl.refuerzo,
          ejercicios: ejerciciosValidados
        },
        fuente: openaiKey ? 'openai' : 'gemini'
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
      semana.push({ ...dia, ejercicios: sesion.ejercicios });
    }
    return { semana, fuente };
  }
}

module.exports = PlanIaService;
