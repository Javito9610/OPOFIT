const db = require('../config/db');

class NearbyService {
  static distanciaM(lat1, lng1, lat2, lng2) {
    const R = 6371000;
    const toRad = (d) => (d * Math.PI) / 180;
    const dLat = toRad(lat2 - lat1);
    const dLng = toRad(lng2 - lng1);
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  static async actualizarUbicacion(userId, lat, lng, visible) {
    if (lat == null || lng == null || lat === '' || lng === '') {
      throw new Error('COORDENADAS_INVALIDAS');
    }
    const latN = Number(lat);
    const lngN = Number(lng);
    if (!Number.isFinite(latN) || !Number.isFinite(lngN)) {
      throw new Error('COORDENADAS_INVALIDAS');
    }
    if (latN < -90 || latN > 90 || lngN < -180 || lngN > 180) {
      throw new Error('COORDENADAS_INVALIDAS');
    }
    const vis = visible ? 1 : 0;
    await db.query(
      `UPDATE usuarios
       SET ubicacion_lat = ?, ubicacion_lng = ?, ubicacion_visible = ?, ubicacion_actualizada = NOW()
       WHERE id_usuario = ?`,
      [latN, lngN, vis, userId]
    );
    return { ok: true, visible: !!vis };
  }

  static async listarCerca(userId, lat, lng, radioKm = 15) {
    if (lat == null || lng == null || lat === '' || lng === '') {
      throw new Error('COORDENADAS_INVALIDAS');
    }
    const latN = Number(lat);
    const lngN = Number(lng);
    if (!Number.isFinite(latN) || !Number.isFinite(lngN)) {
      throw new Error('COORDENADAS_INVALIDAS');
    }
    const radioM = Math.min(100, Math.max(1, Number(radioKm) || 15)) * 1000;
    const [rows] = await db.query(
      `SELECT u.id_usuario, u.nombre, u.modo_uso, u.avatar_url,
              u.ubicacion_lat, u.ubicacion_lng,
              o.nombre AS oposicion_nombre,
              (6371000 * ACOS(
                LEAST(1, GREATEST(-1,
                  COS(RADIANS(?)) * COS(RADIANS(u.ubicacion_lat))
                  * COS(RADIANS(u.ubicacion_lng) - RADIANS(?))
                  + SIN(RADIANS(?)) * SIN(RADIANS(u.ubicacion_lat))
                ))
              )) AS distanciaM
       FROM usuarios u
       LEFT JOIN oposiciones o ON u.oposiciones_id_oposicion = o.id_oposicion
       WHERE u.ubicacion_visible = 1
         AND u.ubicacion_lat IS NOT NULL
         AND u.ubicacion_lng IS NOT NULL
         AND u.id_usuario != ?
       HAVING distanciaM <= ?
       ORDER BY distanciaM ASC
       LIMIT 50`,
      [latN, lngN, latN, userId, radioM]
    );
    // Output snake_case para que coincida con el modelo Kotlin del frontend
    // (Gson default no convierte camelCase). Antes el id_usuario quedaba en 0
    // y todas las features posteriores (solicitar amistad, ver perfil) fallaban.
    return (rows || []).map((r) => ({
      id_usuario: r.id_usuario,
      nombre: r.nombre,
      modo_uso: r.modo_uso || 'OPOSITOR',
      avatar_url: r.avatar_url,
      oposicion_nombre: r.oposicion_nombre || null,
      distancia_m: Math.round(Number(r.distanciaM || 0))
    }));
  }
}

module.exports = NearbyService;
