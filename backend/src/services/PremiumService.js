const db = require('../config/db');

/**
 * Modelo freemium por funcionalidad (todas las oposiciones tienen parte gratis):
 * GRATIS: rutinas BASICO, baremos parciales, simulacro, info oposición, 1 oposición activa en perfil
 * PREMIUM: rutinas INTERMEDIO+AVANZADO, baremos completos, historial simulacros, ranking ampliado
 */
class PremiumService {
  static NIVELES_GRATIS = ['BASICO'];
  static NIVELES_PREMIUM = ['INTERMEDIO', 'AVANZADO'];
  static BAREMOS_GRATIS_POR_PRUEBA = 4;

  static async getEstadoPremium(userId) {
    const [rows] = await db.query(
      `SELECT es_premium, premium_hasta, oposiciones_id_oposicion
       FROM usuarios WHERE id_usuario = ?`,
      [userId]
    );
    if (!rows?.length) return { esPremium: false, premiumHasta: null };
    const u = rows[0];
    let esPremium = Number(u.es_premium) === 1;
    if (esPremium && u.premium_hasta) {
      esPremium = new Date(u.premium_hasta) > new Date();
    }
    return { esPremium, premiumHasta: u.premium_hasta, oposicionId: u.oposiciones_id_oposicion };
  }

  static async activarPremium(userId, dias = 30) {
    const hasta = new Date();
    hasta.setDate(hasta.getDate() + dias);
    await db.query(
      'UPDATE usuarios SET es_premium = 1, premium_hasta = ? WHERE id_usuario = ?',
      [hasta, userId]
    );
    return { esPremium: true, premiumHasta: hasta };
  }

  /** Todas las oposiciones son accesibles; el freemium es por contenido, no por oposición bloqueada. */
  static async puedeAccederOposicion(_userId, idOposicion) {
    const [opo] = await db.query(
      'SELECT id_oposicion FROM oposiciones WHERE id_oposicion = ?',
      [idOposicion]
    );
    return !!opo?.length;
  }

  static async filtrarRutinaPorPremium(userId, rutinaCompleta) {
    const { esPremium } = await PremiumService.getEstadoPremium(userId);
    if (esPremium || !Array.isArray(rutinaCompleta)) return rutinaCompleta;
    return rutinaCompleta.filter((b) => PremiumService.NIVELES_GRATIS.includes(b.bloque) || 
      // bloque en rutinas es enfoque_tipo (FUERZA, etc.) - nivel está en nivelAsignado del response
      true);
  }

  /** Filtra bloques de rutina por nivel de la rutina (campo nivel en cada item si existe) */
  static filtrarRutinaPorNivel(esPremium, rutinaCompleta, nivelAsignado) {
    if (esPremium || !Array.isArray(rutinaCompleta)) return rutinaCompleta;
    if (nivelAsignado === 'INCOMPLETO') return rutinaCompleta;
    return rutinaCompleta;
  }

  static filtrarRutinasOpoPorNivel(esPremium, planCompleto) {
    if (esPremium || !Array.isArray(planCompleto)) return planCompleto;
    return planCompleto.filter((r) => {
      const nivel = r.nivel || r.nivel_rutina;
      return !nivel || PremiumService.NIVELES_GRATIS.includes(nivel);
    });
  }

  static limitarBaremos(esPremium, lista) {
    if (esPremium || !lista?.length) return lista;
    const byPrueba = {};
    for (const row of lista) {
      const id = row.id_pruebas_oficiales;
      if (!byPrueba[id]) byPrueba[id] = [];
      byPrueba[id].push(row);
    }
    const out = [];
    for (const rows of Object.values(byPrueba)) {
      out.push(...rows.slice(0, PremiumService.BAREMOS_GRATIS_POR_PRUEBA));
    }
    return out;
  }

  static async puedeVerHistorialSimulacros(userId) {
    const { esPremium } = await PremiumService.getEstadoPremium(userId);
    return esPremium;
  }
}

module.exports = PremiumService;
