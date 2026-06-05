/**
 * Generador híbrido: banco experto + catálogo filtrado por entorno + variación.
 * La IA (PlanIaService) solo explica; no inventa ejercicios.
 */
const db = require('../config/db');
const EntornoEntreno = require('../utils/EntornoEntreno');
const PlanIaService = require('./PlanIaService');
const RutinaService = require('./RutinasService');

function yearWeek() {
  const d = new Date();
  const onejan = new Date(d.getFullYear(), 0, 1);
  const week = Math.ceil(((d - onejan) / 86400000 + onejan.getDay() + 1) / 7);
  return d.getFullYear() * 100 + week;
}

class PlanGeneradorService {
  static async obtenerPrefsUsuario(userId) {
    const [rows] = await db.query(
      'SELECT entorno_entreno, plan_variacion_seed FROM usuarios WHERE id_usuario = ?',
      [userId]
    );
    const u = rows[0] || {};
    return {
      entorno: EntornoEntreno.normalizarEntorno(u.entorno_entreno),
      seed: Number(u.plan_variacion_seed || 0)
    };
  }

  static async guardarEntorno(userId, entorno) {
    const ent = EntornoEntreno.normalizarEntorno(entorno);
    if (!ent) throw new Error('Entorno no válido');
    await db.query('UPDATE usuarios SET entorno_entreno = ? WHERE id_usuario = ?', [ent, userId]);
    await db.query('DELETE FROM planes_generados_cache WHERE usuarios_id_usuario = ?', [userId]);
    return ent;
  }

  /** Fusiona ejercicios cacheados con el plan actual (marcas, racha, completadas frescas). */
  static combinarPlanConCache(planActual, planCache) {
    if (!planCache?.semana?.length || !planActual?.semana?.length) return planActual;
    const hoy = planActual.dia_hoy;
    const semana = planActual.semana.map((dia) => {
      const cached = planCache.semana.find((c) => c.id_plan_dia === dia.id_plan_dia);
      if (!cached?.ejercicios?.length) return dia;
      return { ...dia, ejercicios: cached.ejercicios };
    });
    const sesion_hoy = semana.find((s) => s.es_hoy) || null;
    const proxima =
      semana.find((s) => !s.completada && s.dia_semana >= hoy) ||
      semana.find((s) => !s.completada) ||
      null;
    return { ...planActual, semana, sesion_hoy, proxima_sesion: proxima };
  }

  static async cargarCatalogo(entorno) {
    const [rows] = await db.query(
      `SELECT id_ejercicio, nombre, pilar, grupo_muscular, equipamiento,
              entornos, tipo_ilustracion, video_url, animacion_url, instrucciones_tecnicas
       FROM ejercicios`
    );
    return rows
      .filter((e) => EntornoEntreno.ejercicioCompatible(e.entornos, entorno))
      .map((e) => ({
        ...e,
        pilar: e.pilar || 'FUERZA',
        grupo_muscular: e.grupo_muscular || 'General'
      }));
  }

  static indexarCatalogo(catalogo) {
    const porClave = new Map();
    const porPilar = new Map();
    for (const e of catalogo) {
      const clave = EntornoEntreno.grupoClave(e.pilar, e.grupo_muscular, e.nombre);
      if (!porClave.has(clave)) porClave.set(clave, []);
      porClave.get(clave).push(e);
      const pil = String(e.pilar).toUpperCase();
      if (!porPilar.has(pil)) porPilar.set(pil, []);
      porPilar.get(pil).push(e);
    }
    return { porClave, porPilar };
  }

  static buscarSustituto(ejBase, indice, { porClave, porPilar }, entorno, seed) {
    const pilar = String(ejBase.pilar || ejBase.categoria || 'FUERZA').toUpperCase();
    const clave = EntornoEntreno.grupoClave(pilar, ejBase.grupo_muscular, ejBase.nombre);
    let candidatos = [...(porClave.get(clave) || [])];
    if (candidatos.length < 2) {
      candidatos = [...(porPilar.get(pilar) || [])];
    }
    candidatos = candidatos.filter((c) => c.nombre !== ejBase.nombre);
    if (!candidatos.length) return null;
    const key = `${entorno}|${indice}|${ejBase.nombre}|${clave}`;
    return EntornoEntreno.seededPick(candidatos, seed, key);
  }

  static mapearEjercicio(ej, sustituto, entorno) {
    if (!sustituto) {
      return {
        ...ej,
        tipo_ilustracion: ej.tipo_ilustracion || EntornoEntreno.inferirTipoIlustracion(ej.nombre, ej.pilar, null),
        entorno_aplicado: entorno,
        sustituido: false
      };
    }
    return {
      ...ej,
      id_ejercicio: sustituto.id_ejercicio,
      nombre: sustituto.nombre,
      video_url: sustituto.video_url || ej.video_url,
      animacion_url: sustituto.animacion_url || null,
      instrucciones_tecnicas: sustituto.instrucciones_tecnicas || null,
      tipo_ilustracion:
        sustituto.tipo_ilustracion ||
        EntornoEntreno.inferirTipoIlustracion(sustituto.nombre, sustituto.pilar, sustituto.grupo_muscular),
      categoria: sustituto.categoria || ej.categoria,
      pilar: sustituto.pilar || ej.pilar,
      equipamiento: sustituto.equipamiento,
      entorno_aplicado: entorno,
      sustituido: true,
      nombre_original: ej.nombre
    };
  }

  static async generarSemana(planBase, userId, entorno, seed) {
    if (!entorno || entorno === 'MIXTO') {
      return { plan: planBase, sustituciones: 0 };
    }
    const catalogo = await PlanGeneradorService.cargarCatalogo(entorno);
    if (catalogo.length < 8) return { plan: planBase, sustituciones: 0 };
    const indice = PlanGeneradorService.indexarCatalogo(catalogo);
    let sustituciones = 0;
    let idx = 0;

    const semana = (planBase.semana || []).map((dia) => {
      const ejercicios = (dia.ejercicios || []).map((ej) => {
        idx += 1;
        const sust = PlanGeneradorService.buscarSustituto(
          { ...ej, pilar: ej.pilar || dia.enfoque },
          idx,
          indice,
          entorno,
          seed
        );
        if (sust) sustituciones += 1;
        return PlanGeneradorService.mapearEjercicio(ej, sust, entorno);
      });
      return { ...dia, ejercicios };
    });

    const hoy = planBase.dia_hoy;
    const sesion_hoy = semana.find((s) => s.es_hoy) || null;
    const proxima =
      semana.find((s) => !s.completada && s.dia_semana >= hoy) ||
      semana.find((s) => !s.completada) ||
      null;

    return {
      plan: { ...planBase, semana, sesion_hoy, proxima_sesion: proxima },
      sustituciones
    };
  }

  static async leerCache(userId, idOposicion, yw) {
    const [rows] = await db.query(
      `SELECT plan_json, explicacion_ia, variacion_seed, entorno_entreno
       FROM planes_generados_cache
       WHERE usuarios_id_usuario = ? AND oposiciones_id_oposicion = ? AND yearweek = ?
       LIMIT 1`,
      [userId, idOposicion, yw]
    );
    if (!rows.length) return null;
    try {
      const plan = JSON.parse(rows[0].plan_json);
      return {
        plan,
        explicacion_ia: rows[0].explicacion_ia,
        variacion_seed: rows[0].variacion_seed,
        entorno_entreno: rows[0].entorno_entreno
      };
    } catch {
      return null;
    }
  }

  static async guardarCache(userId, idOposicion, yw, entorno, seed, plan, explicacion) {
    const json = JSON.stringify(plan);
    await db.query(
      `INSERT INTO planes_generados_cache
         (usuarios_id_usuario, oposiciones_id_oposicion, yearweek, variacion_seed, entorno_entreno, plan_json, explicacion_ia)
       VALUES (?, ?, ?, ?, ?, ?, ?)
       ON DUPLICATE KEY UPDATE
         variacion_seed = VALUES(variacion_seed),
         entorno_entreno = VALUES(entorno_entreno),
         plan_json = VALUES(plan_json),
         explicacion_ia = VALUES(explicacion_ia),
         created_at = CURRENT_TIMESTAMP`,
      [userId, idOposicion, yw, seed, entorno, json, explicacion || null]
    );
  }

  static async aplicarGeneracionInteligente(userId, idOposicion, planBase, nivel, opts = {}) {
    const { entorno, seed } = opts.entorno
      ? { entorno: opts.entorno, seed: opts.seed ?? 0 }
      : await PlanGeneradorService.obtenerPrefsUsuario(userId);

    if (!entorno) {
      return { ...planBase, generacion: { activa: false, motivo: 'sin_entorno' } };
    }

    const yw = yearWeek();
    const forzar = Boolean(opts.forzarRegenerar);

    const meta = EntornoEntreno.ENTORNO_META[entorno] || {};

    if (!forzar) {
      const cache = await PlanGeneradorService.leerCache(userId, idOposicion, yw);
      if (cache && cache.entorno_entreno === entorno && cache.variacion_seed === seed) {
        const merged = PlanGeneradorService.combinarPlanConCache(planBase, cache.plan);
        return {
          ...merged,
          generacion: {
            activa: true,
            entorno_entreno: entorno,
            entorno_etiqueta: meta.etiqueta,
            entorno_emoji: meta.emoji,
            variacion_seed: seed,
            sustituciones: cache.plan?.generacion?.sustituciones ?? null,
            explicacion_ia: cache.explicacion_ia,
            coaching_fuente: cache.plan?.generacion?.coaching_fuente || 'cache',
            desde_cache: true
          }
        };
      }
    }

    const { plan, sustituciones } = await PlanGeneradorService.generarSemana(planBase, userId, entorno, seed);
    const coaching = await PlanIaService.generarCoaching({
      entorno,
      resumen: plan.personalizacion?.resumen,
      pilaresDebiles: plan.personalizacion?.pilares_debiles,
      dias: plan.semana
    });

    const generacion = {
      activa: true,
      entorno_entreno: entorno,
      entorno_etiqueta: meta.etiqueta,
      entorno_emoji: meta.emoji,
      variacion_seed: seed,
      sustituciones,
      explicacion_ia: coaching.texto,
      coaching_fuente: coaching.fuente
    };

    const resultado = { ...plan, generacion };
    await PlanGeneradorService.guardarCache(
      userId,
      idOposicion,
      yw,
      entorno,
      seed,
      resultado,
      coaching.texto
    );
    return resultado;
  }

  static async regenerarPlan(userId, idOposicion, nivel, genero) {
    await db.query(
      'UPDATE usuarios SET plan_variacion_seed = COALESCE(plan_variacion_seed, 0) + 1 WHERE id_usuario = ?',
      [userId]
    );
    const prefs = await PlanGeneradorService.obtenerPrefsUsuario(userId);
    if (!prefs.entorno) {
      throw new Error('Configura primero dónde entrenas');
    }
    const yw = yearWeek();
    await db.query(
      'DELETE FROM planes_generados_cache WHERE usuarios_id_usuario = ? AND oposiciones_id_oposicion = ? AND yearweek = ?',
      [userId, idOposicion, yw]
    );
    const PlanesService = require('./PlanesService');
    return PlanesService.obtenerPlanSemanal(userId, idOposicion, nivel, genero, { forzarRegenerar: true });
  }

  static listarEntornos() {
    return EntornoEntreno.ENTORNOS_VALIDOS.filter((e) => e !== 'MIXTO').map((id) => ({
      id,
      ...EntornoEntreno.ENTORNO_META[id]
    }));
  }
}

module.exports = PlanGeneradorService;
