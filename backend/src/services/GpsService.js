const db = require('../config/db');

class GpsService {
  static async guardar(userId, payload) {
    const {
      id,
      type,
      startedAtMs,
      endedAtMs,
      durationSec,
      movingSec,
      distanceM,
      avgSpeedMps,
      maxSpeedMps,
      avgPaceSecPerKm,
      minPaceSecPerKm,
      maxPaceSecPerKm,
      elevationGainM,
      elevationMinM,
      elevationMaxM,
      avgCadenceSpm,
      points,
      splits
    } = payload || {};

    if (!id || !type || !endedAtMs) {
      throw new Error('Datos de actividad incompletos');
    }
    const tipo = ['RUN', 'WALK', 'BIKE'].includes(type) ? type : 'RUN';

    const [res] = await db.query(
      `INSERT INTO gps_actividades
        (uuid_local, usuarios_id_usuario, tipo,
         iniciada_en, finalizada_en, duracion_seg, movimiento_seg,
         distancia_m, velocidad_media_mps, velocidad_max_mps,
         ritmo_medio_spkm, ritmo_min_spkm, ritmo_max_spkm,
         desnivel_pos_m, altitud_min_m, altitud_max_m,
         cadencia_media_ppm, polyline_json, splits_json)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON DUPLICATE KEY UPDATE
         iniciada_en = VALUES(iniciada_en),
         finalizada_en = VALUES(finalizada_en),
         duracion_seg = VALUES(duracion_seg),
         movimiento_seg = VALUES(movimiento_seg),
         distancia_m = VALUES(distancia_m),
         velocidad_media_mps = VALUES(velocidad_media_mps),
         velocidad_max_mps = VALUES(velocidad_max_mps),
         ritmo_medio_spkm = VALUES(ritmo_medio_spkm),
         ritmo_min_spkm = VALUES(ritmo_min_spkm),
         ritmo_max_spkm = VALUES(ritmo_max_spkm),
         desnivel_pos_m = VALUES(desnivel_pos_m),
         altitud_min_m = VALUES(altitud_min_m),
         altitud_max_m = VALUES(altitud_max_m),
         cadencia_media_ppm = VALUES(cadencia_media_ppm),
         polyline_json = VALUES(polyline_json),
         splits_json = VALUES(splits_json)`,
      [
        id,
        userId,
        tipo,
        Number(startedAtMs || 0),
        Number(endedAtMs),
        Number(durationSec || 0),
        Number(movingSec || 0),
        Number(distanceM || 0),
        Number(avgSpeedMps || 0),
        Number(maxSpeedMps || 0),
        Number(avgPaceSecPerKm || 0),
        Number(minPaceSecPerKm || 0),
        Number(maxPaceSecPerKm || 0),
        Number(elevationGainM || 0),
        elevationMinM == null ? null : Number(elevationMinM),
        elevationMaxM == null ? null : Number(elevationMaxM),
        avgCadenceSpm == null ? null : Number(avgCadenceSpm),
        points ? JSON.stringify(points) : null,
        splits ? JSON.stringify(splits) : null
      ]
    );

    const [[row]] = await db.query(
      'SELECT id_actividad FROM gps_actividades WHERE usuarios_id_usuario = ? AND uuid_local = ?',
      [userId, id]
    );
    return { idActividad: row?.id_actividad ?? res.insertId, uuid: id };
  }

  static async listar(userId, { limit = 50, offset = 0 } = {}) {
    const [rows] = await db.query(
      `SELECT id_actividad, uuid_local, tipo, iniciada_en, finalizada_en,
              duracion_seg, distancia_m, ritmo_medio_spkm, velocidad_media_mps,
              desnivel_pos_m, altitud_min_m, altitud_max_m
       FROM gps_actividades
       WHERE usuarios_id_usuario = ?
       ORDER BY iniciada_en DESC
       LIMIT ? OFFSET ?`,
      [userId, Number(limit), Number(offset)]
    );
    return rows.map((r) => ({
      id: r.uuid_local,
      idRemoto: r.id_actividad,
      type: r.tipo,
      startedAtMs: Number(r.iniciada_en),
      endedAtMs: Number(r.finalizada_en),
      durationSec: r.duracion_seg,
      distanceM: Number(r.distancia_m),
      avgPaceSecPerKm: Number(r.ritmo_medio_spkm),
      avgSpeedMps: Number(r.velocidad_media_mps),
      elevationGainM: Number(r.desnivel_pos_m || 0),
      elevationMinM: r.altitud_min_m == null ? null : Number(r.altitud_min_m),
      elevationMaxM: r.altitud_max_m == null ? null : Number(r.altitud_max_m)
    }));
  }

  static async detalle(userId, uuid) {
    const [[r]] = await db.query(
      `SELECT * FROM gps_actividades WHERE usuarios_id_usuario = ? AND uuid_local = ? LIMIT 1`,
      [userId, uuid]
    );
    if (!r) return null;
    return {
      id: r.uuid_local,
      idRemoto: r.id_actividad,
      type: r.tipo,
      startedAtMs: Number(r.iniciada_en),
      endedAtMs: Number(r.finalizada_en),
      durationSec: r.duracion_seg,
      movingSec: r.movimiento_seg,
      distanceM: Number(r.distancia_m),
      avgSpeedMps: Number(r.velocidad_media_mps),
      maxSpeedMps: Number(r.velocidad_max_mps),
      avgPaceSecPerKm: Number(r.ritmo_medio_spkm),
      minPaceSecPerKm: Number(r.ritmo_min_spkm),
      maxPaceSecPerKm: Number(r.ritmo_max_spkm),
      elevationGainM: Number(r.desnivel_pos_m || 0),
      elevationMinM: r.altitud_min_m == null ? null : Number(r.altitud_min_m),
      elevationMaxM: r.altitud_max_m == null ? null : Number(r.altitud_max_m),
      avgCadenceSpm: r.cadencia_media_ppm == null ? null : Number(r.cadencia_media_ppm),
      points: r.polyline_json ? JSON.parse(r.polyline_json) : [],
      splits: r.splits_json ? JSON.parse(r.splits_json) : []
    };
  }

  static async borrar(userId, uuid) {
    const [res] = await db.query(
      'DELETE FROM gps_actividades WHERE usuarios_id_usuario = ? AND uuid_local = ?',
      [userId, uuid]
    );
    return res.affectedRows > 0;
  }
}

module.exports = GpsService;
