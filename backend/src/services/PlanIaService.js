/**
 * Coaching con LLM (OpenAI o Gemini). Si no hay API key, usa texto basado en reglas.
 */
const EntornoEntreno = require('../utils/EntornoEntreno');

function fallbackCoaching({ entorno, resumen, pilaresDebiles, dias }) {
  const meta = EntornoEntreno.ENTORNO_META[entorno] || EntornoEntreno.ENTORNO_META.MIXTO;
  const partes = [
    `Plan adaptado para entrenar en ${meta.etiqueta.toLowerCase()} ${meta.emoji}.`,
    resumen || 'Priorizamos tus puntos débiles según el baremo.'
  ];
  if (pilaresDebiles?.length) {
    const d = pilaresDebiles[0];
    partes.push(
      `Esta semana refuerza ${d.etiqueta || d.pilar} (${d.pruebas?.slice(0, 2).join(', ') || 'pruebas clave'}).`
    );
  }
  if (dias?.length) {
    const focos = [...new Set(dias.map((d) => d.enfoque))].slice(0, 3);
    partes.push(`Microciclo: ${focos.join(' · ').toLowerCase()}.`);
  }
  partes.push('Si un ejercicio no encaja, pulsa «Generar otra semana».');
  return partes.join(' ');
}

async function llamarOpenAI(apiKey, prompt) {
  const res = await fetch('https://api.openai.com/v1/chat/completions', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      model: process.env.OPENAI_MODEL || 'gpt-4o-mini',
      messages: [
        {
          role: 'system',
          content:
            'Eres un preparador físico de oposiciones en España. Responde en español, máximo 4 frases, motivador y concreto. No inventes ejercicios ni cargas fuera del JSON.'
        },
        { role: 'user', content: prompt }
      ],
      max_tokens: 220,
      temperature: 0.7
    })
  });
  if (!res.ok) throw new Error(`OpenAI ${res.status}`);
  const data = await res.json();
  return data.choices?.[0]?.message?.content?.trim() || null;
}

async function llamarGemini(apiKey, prompt) {
  const model = process.env.GEMINI_MODEL || 'gemini-2.0-flash';
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig: { maxOutputTokens: 220, temperature: 0.7 }
    })
  });
  if (!res.ok) throw new Error(`Gemini ${res.status}`);
  const data = await res.json();
  return data.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || null;
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
Resumen motor: ${ctx.resumen || 'sin datos'}
Pilares débiles: ${JSON.stringify(ctx.pilaresDebiles || [])}
Días semana: ${JSON.stringify((ctx.dias || []).map((d) => ({ dia: d.nombre_dia, enfoque: d.enfoque, titulo: d.titulo })))}
Escribe 3-4 frases de coaching personalizado en español.`;

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
}

module.exports = PlanIaService;
