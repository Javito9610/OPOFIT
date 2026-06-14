const PlanesService = require('../services/PlanesService');
const PlanGeneradorService = require('../services/PlanGeneradorService');
const RutinaService = require('../services/RutinasService');
const PremiumService = require('../services/PremiumService');

async function resolverNivel(userId, idOposicion) {
  const calc = await RutinaService.calcularNotaYNivel(userId, idOposicion);
  if ((calc.pruebasFaltantes ?? 0) > 0) {
    return { bloqueado: true, calc };
  }
  const premium = await PremiumService.getEstadoPremium(userId);
  const nivel =
    !premium.esPremium && calc.nivelSugerido !== 'BASICO' ? 'BASICO' : calc.nivelSugerido;
  return { bloqueado: false, calc, nivel };
}

const getCalendario = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const { idOposicion } = req.params;
    const { year, month } = req.query;
    if (!userId || !idOposicion) {
      return res.status(400).json({ ok: false, msg: 'Parámetros inválidos' });
    }
    const { bloqueado, calc, nivel } = await resolverNivel(userId, idOposicion);
    if (bloqueado) {
      return res.status(200).json({ ok: true, data: null, msg: 'Completa las marcas del perfil' });
    }
    const cal = await PlanesService.obtenerCalendarioMes(
      userId,
      idOposicion,
      nivel,
      calc.genero,
      year,
      month
    );
    return res.status(200).json({ ok: true, data: cal });
  } catch (e) {
    console.error('getCalendario:', e.message);
    return res.status(500).json({ ok: false, msg: 'Error al cargar calendario' });
  }
};

const getEntornos = async (_req, res) => {
  return res.status(200).json({ ok: true, data: PlanGeneradorService.listarEntornos() });
};

const getEntornoUsuario = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'No autorizado' });
    const prefs = await PlanGeneradorService.obtenerPrefsUsuario(userId);
    return res.status(200).json({ ok: true, data: prefs });
  } catch (e) {
    return res.status(500).json({ ok: false, msg: e.message });
  }
};

const putEntornoUsuario = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const { entorno } = req.body || {};
    if (!userId) return res.status(401).json({ ok: false, msg: 'No autorizado' });
    const guardado = await PlanGeneradorService.guardarEntorno(userId, entorno);
    return res.status(200).json({
      ok: true,
      msg: 'Entorno de entrenamiento guardado',
      data: { entorno: guardado }
    });
  } catch (e) {
    return res.status(400).json({ ok: false, msg: e.message });
  }
};

const postRegenerarDia = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const { idOposicion, idPlanDia } = req.params;
    if (!userId || !idOposicion || !idPlanDia) {
      return res.status(400).json({ ok: false, msg: 'Parámetros inválidos' });
    }
    const { bloqueado, calc, nivel } = await resolverNivel(userId, idOposicion);
    if (bloqueado) {
      return res.status(400).json({ ok: false, msg: 'Completa las marcas del perfil primero' });
    }
    const plan = await PlanGeneradorService.regenerarDia(
      userId,
      idOposicion,
      idPlanDia,
      nivel,
      calc.genero
    );
    return res.status(200).json({
      ok: true,
      msg: 'Nueva opción para este día',
      data: plan
    });
  } catch (e) {
    console.error('postRegenerarDia:', e.message);
    const msg = (e.message || '').includes('Incorrect string value')
      ? 'No se pudo guardar la nueva opción. Reinicia la app o contacta soporte.'
      : e.message;
    return res.status(400).json({ ok: false, msg });
  }
};

const postRegenerarPlan = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const { idOposicion } = req.params;
    if (!userId || !idOposicion) {
      return res.status(400).json({ ok: false, msg: 'Parámetros inválidos' });
    }
    const { bloqueado, calc, nivel } = await resolverNivel(userId, idOposicion);
    if (bloqueado) {
      return res.status(400).json({ ok: false, msg: 'Completa las marcas del perfil primero' });
    }
    const plan = await PlanGeneradorService.regenerarPlan(
      userId,
      idOposicion,
      nivel,
      calc.genero
    );
    return res.status(200).json({
      ok: true,
      msg: 'Nueva semana generada',
      data: plan
    });
  } catch (e) {
    console.error('postRegenerarPlan:', e.message);
    const msg = (e.message || '').includes('Incorrect string value')
      ? 'No se pudo guardar la nueva semana. Reinicia la app o contacta soporte.'
      : e.message;
    return res.status(400).json({ ok: false, msg });
  }
};

const postDisenarSesionIA = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const { idOposicion, idPlanDia } = req.params;
    if (!userId || !idOposicion || !idPlanDia) {
      return res.status(400).json({ ok: false, msg: 'Parámetros inválidos' });
    }
    const { bloqueado, calc, nivel } = await resolverNivel(userId, idOposicion);
    if (bloqueado) {
      return res.status(400).json({ ok: false, msg: 'Completa las marcas del perfil primero' });
    }
    // Requiere premium: la IA diseña es valor añadido.
    const premium = await PremiumService.getEstadoPremium(userId);
    if (!premium.esPremium) {
      return res
        .status(402)
        .json({ ok: false, msg: 'La IA que diseña tu plan está en Premium', premium_required: true });
    }
    const plan = await PlanGeneradorService.disenarSesionIA(
      userId,
      idOposicion,
      idPlanDia,
      nivel,
      calc.genero
    );
    return res.status(200).json({
      ok: true,
      msg: 'Sesión diseñada por IA',
      data: plan
    });
  } catch (e) {
    console.error('postDisenarSesionIA:', e.message);
    return res.status(400).json({ ok: false, msg: e.message });
  }
};

/**
 * PUT /api/planes/onboarding — Onboarding wizard Freeletics.
 * Guarda en una sola llamada los 4 campos del wizard:
 *   - objetivo: perder_grasa | ganar_musculo | resistencia | rendimiento
 *   - diasSemana: int 3..6
 *   - tiempoMin: int 30..90
 *   - lesiones: ['rodilla','hombro',...]
 *
 * Validación estricta + idempotente: si la fila settings no existe, la crea.
 */
const db = require('../config/db');
const putOnboarding = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'No autorizado' });
    const body = req.body || {};

    const objetivos = ['perder_grasa', 'ganar_musculo', 'resistencia', 'rendimiento'];
    const objetivo = String(body.objetivo || '').trim().toLowerCase();
    if (!objetivos.includes(objetivo)) {
      return res.status(400).json({ ok: false, msg: 'Objetivo inválido' });
    }

    const diasSemana = Number(body.diasSemana);
    if (!Number.isFinite(diasSemana) || diasSemana < 3 || diasSemana > 6) {
      return res.status(400).json({ ok: false, msg: 'diasSemana debe estar entre 3 y 6' });
    }

    const tiempoMin = Number(body.tiempoMin);
    if (!Number.isFinite(tiempoMin) || tiempoMin < 30 || tiempoMin > 90) {
      return res.status(400).json({ ok: false, msg: 'tiempoMin debe estar entre 30 y 90' });
    }

    const lesionesValidas = ['rodilla', 'hombro', 'lumbar', 'tobillo', 'codo', 'muneca'];
    const lesionesArr = Array.isArray(body.lesiones) ? body.lesiones : [];
    const lesiones = lesionesArr
      .map((l) => String(l || '').trim().toLowerCase())
      .filter((l) => lesionesValidas.includes(l));
    const lesionesCsv = lesiones.join(',');

    // Persistir días/semana en usuarios (campo legacy) y el resto en settings.
    await db.query(
      'UPDATE usuarios SET dias_entreno_semana = ? WHERE id_usuario = ?',
      [diasSemana, userId]
    );

    // Upsert en settings con todos los campos del onboarding a la vez.
    await db.query(
      `INSERT INTO settings (
         usuarios_id_usuario, unidad_peso, unidad_distancia,
         objetivo_fitness, tiempo_disponible_min, lesiones, fatiga_previa
       ) VALUES (?, 'kg', 'km', ?, ?, ?, NULL)
       ON DUPLICATE KEY UPDATE
         objetivo_fitness = VALUES(objetivo_fitness),
         tiempo_disponible_min = VALUES(tiempo_disponible_min),
         lesiones = VALUES(lesiones)`,
      [userId, objetivo, tiempoMin, lesionesCsv || null]
    );

    // Invalidamos el cache del plan: la próxima carga lo regenerará.
    await db.query(
      'DELETE FROM planes_generados_cache WHERE usuarios_id_usuario = ?',
      [userId]
    );

    return res.status(200).json({
      ok: true,
      msg: 'Onboarding guardado',
      data: {
        objetivo,
        diasSemana,
        tiempoMin,
        lesiones
      }
    });
  } catch (e) {
    console.error('putOnboarding:', e.message);
    return res.status(500).json({ ok: false, msg: e.message });
  }
};

module.exports = {
  getCalendario,
  getEntornos,
  getEntornoUsuario,
  putEntornoUsuario,
  postRegenerarPlan,
  postRegenerarDia,
  postDisenarSesionIA,
  putOnboarding
};
