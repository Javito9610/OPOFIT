/**
 * Generador híbrido: banco experto + catálogo filtrado por entorno + variación.
 * La IA (PlanIaService) solo explica; no inventa ejercicios.
 */
const db = require('../config/db');
const EntornoEntreno = require('../utils/EntornoEntreno');
const EjercicioMetadataService = require('./EjercicioMetadataService');
const PlanIaService = require('./PlanIaService');
const PlanPersonalizadorService = require('./PlanPersonalizadorService');
const EjercicioInteligenteService = require('./EjercicioInteligenteService');
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
    // Material disponible: consulta opcional, no rompe si la columna no existe
    // (test, BD pre-migración). En esos casos asumimos solo peso corporal.
    let material = ['NADA'];
    try {
      const [matRows] = await db.query(
        'SELECT material_disponible FROM settings WHERE usuarios_id_usuario = ?',
        [userId]
      );
      const raw = matRows[0]?.material_disponible;
      if (raw) {
        material = String(raw).split(',').map((m) => m.trim().toUpperCase()).filter(Boolean);
        if (!material.length) material = ['NADA'];
      }
    } catch (_) {
      // Columna no existe aún → seguimos con peso corporal.
    }
    return {
      entorno: EntornoEntreno.normalizarEntorno(u.entorno_entreno),
      seed: Number(u.plan_variacion_seed || 0),
      materialDisponible: material
    };
  }

  static async obtenerUltimasSesiones(userId, limite = 3) {
    const [rows] = await db.query(
      `SELECT DATE(fecha_entreno) AS fecha, tipo_rutina, duracion_oficial
       FROM historial_sesiones
       WHERE usuarios_id_usuario = ?
       ORDER BY fecha_entreno DESC
       LIMIT ?`,
      [userId, limite]
    );
    return rows || [];
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
      return {
        ...dia,
        ejercicios: cached.ejercicios,
        titulo: cached.titulo || dia.titulo,
        descripcion: cached.descripcion ?? dia.descripcion
      };
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
      .map((e) => {
        const nombre = EjercicioMetadataService.normalizarNombreEjercicio(e.nombre);
        const pilar = EntornoEntreno.normalizarPilar(e.pilar || 'FUERZA');
        return {
          ...e,
          nombre,
          pilar,
          grupo_muscular: EjercicioMetadataService.inferirGrupoMuscular(
            e.grupo_muscular,
            nombre,
            pilar
          )
        };
      });
  }

  static indexarCatalogo(catalogo) {
    const porClave = new Map();
    const porPilar = new Map();
    for (const e of catalogo) {
      const clave = EntornoEntreno.grupoClave(e.pilar, e.grupo_muscular, e.nombre);
      if (!porClave.has(clave)) porClave.set(clave, []);
      porClave.get(clave).push(e);
      const pil = EntornoEntreno.normalizarPilar(e.pilar);
      if (!porPilar.has(pil)) porPilar.set(pil, []);
      porPilar.get(pil).push(e);
    }
    return { porClave, porPilar };
  }

  static buscarSustituto(ejBase, idx, { porClave, porPilar }, entorno, seed) {
    const pilar = EntornoEntreno.normalizarPilar(ejBase.pilar || ejBase.categoria || 'FUERZA');
    const nombreLimpio = EjercicioMetadataService.normalizarNombreEjercicio(ejBase.nombre);
    const grupo = EjercicioMetadataService.inferirGrupoMuscular(
      ejBase.grupo_muscular,
      nombreLimpio,
      pilar
    );
    const clave = EntornoEntreno.grupoClave(pilar, grupo, nombreLimpio);
    const distinto = (c) =>
      EjercicioMetadataService.normalizarNombreEjercicio(c.nombre).toLowerCase() !==
      nombreLimpio.toLowerCase();

    let candidatos = [...(porClave.get(clave) || [])].filter(distinto);
    if (!candidatos.length) {
      candidatos = [...(porPilar.get(pilar) || [])].filter(distinto);
    }
    if (!candidatos.length) return null;

    const key = `${entorno}|${idx}|${nombreLimpio}|${clave}|v${seed}`;
    return EntornoEntreno.seededPick(candidatos, seed, key);
  }

  static resumenDiaDesdeEjercicios(dia, ejercicios) {
    const nombres = (ejercicios || []).map((e) => e.nombre).filter(Boolean);
    if (!nombres.length) {
      return { titulo: dia.titulo, descripcion: dia.descripcion };
    }
    const titulo = nombres[0];
    const descripcion = nombres.slice(0, 4).join(' · ');
    return { titulo, descripcion };
  }

  static mapearEjercicio(ej, sustituto, entorno) {
    const base = EjercicioMetadataService.enriquecerEjercicio(ej);
    if (!sustituto) {
      return EjercicioInteligenteService.aplicarInteligencia(
        { ...base, entorno_aplicado: entorno, sustituido: false },
        { seed: ej.orden || 1 }
      );
    }
    const grupo = EjercicioMetadataService.inferirGrupoMuscular(
      base.grupo_muscular,
      base.nombre,
      base.pilar
    );
    const mapped = EjercicioMetadataService.enriquecerEjercicio({
      ...ej,
      id_ejercicio: sustituto.id_ejercicio,
      nombre: sustituto.nombre,
      video_url: sustituto.video_url || ej.video_url,
      animacion_url: sustituto.animacion_url || null,
      instrucciones_tecnicas: sustituto.instrucciones_tecnicas || null,
      tipo_ilustracion: sustituto.tipo_ilustracion,
      categoria: sustituto.categoria || ej.categoria,
      pilar: sustituto.pilar || ej.pilar,
      grupo_muscular: sustituto.grupo_muscular,
      equipamiento: sustituto.equipamiento,
      entorno_aplicado: entorno,
      sustituido: true,
      nombre_original: base.nombre,
      motivo_sustitucion: EjercicioMetadataService.motivoSustitucion(
        entorno,
        grupo,
        base.nombre,
        sustituto.nombre
      )
    });
    return EjercicioInteligenteService.aplicarInteligencia(mapped, { seed: ej.orden || 0 });
  }

  static async generarSemana(planBase, userId, entorno, seed, opts = {}) {
    const { soloDiaId } = opts;
    if (!entorno || entorno === 'MIXTO') {
      return { plan: planBase, sustituciones: 0 };
    }
    const catalogo = await PlanGeneradorService.cargarCatalogo(entorno);
    if (catalogo.length < 8) return { plan: planBase, sustituciones: 0 };
    const indice = PlanGeneradorService.indexarCatalogo(catalogo);
    let sustituciones = 0;
    let idx = 0;

    const semana = (planBase.semana || []).map((dia) => {
      if (soloDiaId != null && dia.id_plan_dia !== soloDiaId) return dia;
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
      const resumen = PlanGeneradorService.resumenDiaDesdeEjercicios(dia, ejercicios);
      return { ...dia, ejercicios, titulo: resumen.titulo, descripcion: resumen.descripcion };
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

  static sinEmojis4Bytes(str) {
    if (!str) return str;
    return str.replace(/[\u{10000}-\u{10FFFF}]/gu, '').replace(
      /[\uD800-\uDBFF][\uDC00-\uDFFF]/g,
      ''
    );
  }

  static async guardarCache(userId, idOposicion, yw, entorno, seed, plan, explicacion) {
    const json = JSON.stringify(plan);
    const params = [userId, idOposicion, yw, seed, entorno, json, explicacion || null];
    const sql = `INSERT INTO planes_generados_cache
         (usuarios_id_usuario, oposiciones_id_oposicion, yearweek, variacion_seed, entorno_entreno, plan_json, explicacion_ia)
       VALUES (?, ?, ?, ?, ?, ?, ?)
       ON DUPLICATE KEY UPDATE
         variacion_seed = VALUES(variacion_seed),
         entorno_entreno = VALUES(entorno_entreno),
         plan_json = VALUES(plan_json),
         explicacion_ia = VALUES(explicacion_ia),
         created_at = CURRENT_TIMESTAMP`;
    try {
      await db.query(sql, params);
    } catch (e) {
      const msg = e.message || '';
      if (msg.includes('Incorrect string value') || msg.includes('ER_TRUNCATED_WRONG_VALUE_FOR_FIELD')) {
        const safeJson = PlanGeneradorService.sinEmojis4Bytes(json);
        const safeExpl = explicacion ? PlanGeneradorService.sinEmojis4Bytes(explicacion) : null;
        await db.query(sql, [userId, idOposicion, yw, seed, entorno, safeJson, safeExpl]);
        return;
      }
      throw e;
    }
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
    const ultimasSesiones = await PlanGeneradorService.obtenerUltimasSesiones(userId, 3);
    const coaching = await PlanIaService.generarCoaching({
      entorno,
      resumen: plan.personalizacion?.resumen,
      pilaresDebiles: plan.personalizacion?.pilares_debiles,
      pilaresFuertes: plan.personalizacion?.pilares_fuertes,
      dias: plan.semana,
      rachaDias: plan.personalizacion?.racha_dias,
      sesionesSemana: plan.personalizacion?.sesiones_semana,
      nivel: plan.personalizacion?.nivel_usado,
      ultimasSesiones
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

  static async regenerarDia(userId, idOposicion, idPlanDia, nivel, genero) {
    const PlanesService = require('./PlanesService');
    const prefs = await PlanGeneradorService.obtenerPrefsUsuario(userId);
    if (!prefs.entorno) {
      throw new Error('Configura primero dónde entrenas');
    }

    const planActual = await PlanesService.obtenerPlanSemanal(userId, idOposicion, nivel, genero);
    const idDia = Number(idPlanDia);
    const dia = planActual.semana?.find((d) => d.id_plan_dia === idDia);
    if (!dia) throw new Error('Día no encontrado en el plan');
    if (dia.completada) throw new Error('No puedes cambiar un día ya completado');

    await db.query(
      'UPDATE usuarios SET plan_variacion_seed = COALESCE(plan_variacion_seed, 0) + 1 WHERE id_usuario = ?',
      [userId]
    );
    const { seed, entorno } = await PlanGeneradorService.obtenerPrefsUsuario(userId);
    const daySeed = seed + idDia * 31;

    const { plan: planParcial, sustituciones } = await PlanGeneradorService.generarSemana(
      planActual,
      userId,
      entorno,
      daySeed,
      { soloDiaId: idDia }
    );

    const nuevoDia = planParcial.semana.find((d) => d.id_plan_dia === idDia);
    if (!nuevoDia) throw new Error('No se pudo regenerar el día');

    const semana = planActual.semana.map((d) =>
      d.id_plan_dia === idDia
        ? {
            ...d,
            ...nuevoDia,
            ejercicios: nuevoDia.ejercicios,
            titulo: nuevoDia.titulo || d.titulo,
            descripcion: nuevoDia.descripcion || d.descripcion
          }
        : d
    );
    const hoy = planActual.dia_hoy;
    const sesion_hoy = semana.find((s) => s.es_hoy) || null;
    const proxima =
      semana.find((s) => !s.completada && s.dia_semana >= hoy) ||
      semana.find((s) => !s.completada) ||
      null;

    const resultado = {
      ...planActual,
      semana,
      sesion_hoy,
      proxima_sesion: proxima,
      generacion: {
        ...(planActual.generacion || {}),
        variacion_seed: seed,
        sustituciones_dia: sustituciones
      }
    };

    if (resultado.personalizacion) {
      resultado.personalizacion = {
        ...resultado.personalizacion,
        variacion_seed: seed,
        sustituciones: (resultado.personalizacion.sustituciones || 0) + sustituciones
      };
    }

    const yw = yearWeek();
    const cache = await PlanGeneradorService.leerCache(userId, idOposicion, yw);
    const explicacion = cache?.explicacion_ia || planActual.personalizacion?.explicacion_ia || null;
    const sanitizado = PlanesService.sanitizarPlan(resultado);
    await PlanGeneradorService.guardarCache(userId, idOposicion, yw, entorno, seed, sanitizado, explicacion);

    return sanitizado;
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

  /**
   * Modo "IA diseña sesión" (item 22).
   * Recibe un día del plan y lo redibuja con `PlanIaService.disenarSesion`.
   * El resultado se mapea/normaliza con el mismo pipeline que el resto del plan.
   */
  static async disenarSesionIA(userId, idOposicion, idPlanDia, nivel, genero) {
    const PlanesService = require('./PlanesService');
    const { entorno, seed, materialDisponible } = await PlanGeneradorService.obtenerPrefsUsuario(userId);
    if (!entorno) throw new Error('Configura primero dónde entrenas');

    const planActual = await PlanesService.obtenerPlanSemanal(userId, idOposicion, nivel, genero);
    const idDia = Number(idPlanDia);
    const dia = planActual.semana?.find((d) => d.id_plan_dia === idDia);
    if (!dia) throw new Error('Día no encontrado en el plan');
    if (dia.completada) throw new Error('No puedes cambiar un día ya completado');

    const catalogo = await PlanGeneradorService.cargarCatalogo(entorno);
    if (catalogo.length < 8) throw new Error('Catálogo insuficiente para diseñar la sesión');

    const { sesion, fuente } = await PlanIaService.disenarSesion({
      enfoque: dia.enfoque,
      nivel: nivel || 'INTERMEDIO',
      entorno,
      catalogo,
      pilaresDebiles: planActual.personalizacion?.pilares_debiles || [],
      pilaresFuertes: planActual.personalizacion?.pilares_fuertes || [],
      materialDisponible, // ← la IA filtrará ejercicios por material real del usuario
      seed: seed + idDia,
      usarIA: true
    });

    // Aplicar pipeline de enriquecimiento/inteligencia a cada ejercicio diseñado.
    const ejerciciosFinales = sesion.ejercicios.map((ej, idx) => {
      const enriquecido = EjercicioMetadataService.enriquecerEjercicio({
        ...ej,
        orden: idx + 1,
        sustituido: true,
        ia_diseno: true,
        ia_origen: ej.ia_origen || fuente,
        entorno_aplicado: entorno
      });
      return EjercicioInteligenteService.aplicarInteligencia(enriquecido, {
        seed: idx + 1,
        respetarSeriesReps: true
      });
    });

    const semana = planActual.semana.map((d) =>
      d.id_plan_dia === idDia
        ? {
            ...d,
            ejercicios: ejerciciosFinales,
            titulo: PlanGeneradorService.resumenDiaDesdeEjercicios(d, ejerciciosFinales).titulo,
            descripcion: PlanGeneradorService.resumenDiaDesdeEjercicios(d, ejerciciosFinales).descripcion
          }
        : d
    );

    const hoy = planActual.dia_hoy;
    const sesion_hoy = semana.find((s) => s.es_hoy) || null;
    const proxima =
      semana.find((s) => !s.completada && s.dia_semana >= hoy) ||
      semana.find((s) => !s.completada) ||
      null;

    const resultado = {
      ...planActual,
      semana,
      sesion_hoy,
      proxima_sesion: proxima,
      generacion: {
        ...(planActual.generacion || {}),
        ia_diseno_dia: idDia,
        ia_diseno_fuente: fuente
      }
    };

    const sanitizado = PlanesService.sanitizarPlan(resultado);
    const yw = yearWeek();
    const cache = await PlanGeneradorService.leerCache(userId, idOposicion, yw);
    const explicacion = cache?.explicacion_ia || planActual.personalizacion?.explicacion_ia || null;
    await PlanGeneradorService.guardarCache(userId, idOposicion, yw, entorno, seed, sanitizado, explicacion);
    return sanitizado;
  }
}

module.exports = PlanGeneradorService;
