/**
 * EjercicioExplicacionAiService — genera con IA (Groq/Claude/OpenAI/Gemini) la
 * ficha explicativa de un ejercicio y la CACHEA en la BD (columna
 * ejercicios.explicacion_json). Se genera UNA vez por ejercicio; después se
 * sirve desde la BD (0 latencia, 0 coste recurrente).
 *
 * Prioridad de servido (en EjerciciosService): curada a mano > cache IA > patrón.
 *
 * Sin clave de IA configurada, todo esto simplemente no hace nada y la app cae
 * al texto por patrón (no rompe).
 */
const db = require('../config/db');
const PlanIaService = require('./PlanIaService');
const { CURADAS } = require('./EjercicioExplicacionesCuradas');

function normalizar(s) {
  return String(s || '').normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim();
}

const SYSTEM = `Eres COACH OpoFit, preparador físico certificado (CSCS/NSCA) especializado
en oposiciones físicas españolas (Policía Nacional/Local, Guardia Civil, Bomberos,
Mossos, Ejército). Escribes fichas de ejercicio claras, técnicas y en ESPAÑOL de
España, para usuarios reales. Nada de relleno. Responde SOLO con JSON válido.`;

function disciplinaHint(ej) {
  const n = String(ej.nombre || '').toLowerCase();
  if (/nataci|crol|nadar|\bnado\b|braza|espalda \(agua\)|respiraci[oó]n bilateral|brazada|patada de (crol|nado|espalda)|piscina/.test(n)) {
    return ' IMPORTANTE: es un ejercicio de NATACIÓN (en piscina/agua). Explícalo como técnica de nado, no de gimnasio ni de carrera.';
  }
  if (/bici|ciclismo|el[íi]ptica|remo\s*erg|assault|ski\s*erg|step\s*mill|stairmaster|cinta|tapiz/.test(n)) {
    return ' IMPORTANTE: es cardio en MÁQUINA. Explícalo sobre la máquina indicada, no como pesas.';
  }
  if (/carrera|correr|trote|rodaje|fartlek|sprint|cuesta/.test(n)) {
    return ' IMPORTANTE: es un ejercicio de CARRERA. Explícalo como técnica de carrera.';
  }
  return '';
}

function construirPrompt(ej) {
  return `Genera la ficha del ejercicio "${ej.nombre}"${ej.grupo_muscular ? ` (grupo: ${ej.grupo_muscular})` : ''}${ej.pilar ? `, pilar: ${ej.pilar}` : ''}${ej.equipamiento && ej.equipamiento !== '—' ? `, material: ${ej.equipamiento}` : ''}.${disciplinaHint(ej)}
Devuelve EXACTAMENTE este JSON (en español, específico de ESTE ejercicio, sin genérico):
{
  "setup": "1-2 frases: posición inicial, colocación, qué activar",
  "ejecucion": "2-3 frases: la repetición paso a paso, con respiración",
  "coaching_cues": ["3-4 claves cortas de entrenador"],
  "errores_comunes": ["3-4 errores típicos a evitar"],
  "porque": "1-2 frases: para qué sirve y su conexión con las pruebas de oposición"
}`;
}

function validar(obj) {
  if (!obj || typeof obj !== 'object') return null;
  const s = (x) => (typeof x === 'string' ? x.trim() : '');
  const arr = (x) => (Array.isArray(x) ? x.map((i) => s(i)).filter(Boolean).slice(0, 5) : []);
  const setup = s(obj.setup);
  const ejecucion = s(obj.ejecucion);
  const porque = s(obj.porque);
  const cues = arr(obj.coaching_cues);
  const errores = arr(obj.errores_comunes);
  // Mínimos de calidad para no cachear basura.
  if (setup.length < 15 || ejecucion.length < 20 || porque.length < 15 || cues.length < 2) {
    return null;
  }
  return { setup, ejecucion, coaching_cues: cues, errores_comunes: errores, porque };
}

class EjercicioExplicacionAiService {
  static disponible() {
    return PlanIaService.iaDisponible();
  }

  /** Genera la ficha de un ejercicio vía IA. Devuelve el objeto validado o null. */
  static async generar(ej) {
    const parsed = await PlanIaService.generarJson(construirPrompt(ej), {
      systemPrompt: SYSTEM,
      maxTokens: 500,
      temperature: 0.5
    });
    return validar(parsed);
  }

  static async guardarEnBd(idEjercicio, obj) {
    try {
      await db.query('UPDATE ejercicios SET explicacion_json = ? WHERE id_ejercicio = ?', [
        JSON.stringify(obj),
        idEjercicio
      ]);
      return true;
    } catch (e) {
      console.warn('[explic-ai] guardar', e.message);
      return false;
    }
  }

  /**
   * Procesa un lote de ejercicios SIN ficha (ni curada ni cacheada) y la genera.
   * @param {number} limite  máximo de ejercicios a generar en esta llamada.
   * @param {number} pausaMs pausa entre llamadas (respetar rate limits del free tier).
   * @returns {Promise<{generados:number, fallidos:number, restantes:number}>}
   */
  static async procesarLote(limite = 50, pausaMs = 350) {
    if (!EjercicioExplicacionAiService.disponible()) {
      return { generados: 0, fallidos: 0, restantes: 0, sinClave: true };
    }
    const [rows] = await db.query(
      `SELECT id_ejercicio, nombre, grupo_muscular, equipamiento, pilar
         FROM ejercicios
        WHERE explicacion_json IS NULL
        ORDER BY id_ejercicio ASC
        LIMIT ?`,
      [Math.max(1, Math.min(200, Number(limite) || 50))]
    );
    let generados = 0;
    let fallidos = 0;
    for (const ej of rows) {
      // Saltamos los que ya tienen ficha CURADA a mano (no gastamos IA en ellos).
      if (CURADAS[normalizar(ej.nombre)]) {
        // Marcamos como cacheada la curada para no volver a evaluarla.
        await EjercicioExplicacionAiService.guardarEnBd(ej.id_ejercicio, {
          _curada: true, ...CURADAS[normalizar(ej.nombre)]
        });
        continue;
      }
      const ficha = await EjercicioExplicacionAiService.generar(ej);
      if (ficha) {
        await EjercicioExplicacionAiService.guardarEnBd(ej.id_ejercicio, ficha);
        generados++;
      } else {
        fallidos++;
      }
      if (pausaMs) await new Promise((r) => setTimeout(r, pausaMs));
    }
    const [[{ restantes }]] = await db.query(
      'SELECT COUNT(*) AS restantes FROM ejercicios WHERE explicacion_json IS NULL'
    );
    return { generados, fallidos, restantes: Number(restantes) };
  }
}

module.exports = EjercicioExplicacionAiService;
