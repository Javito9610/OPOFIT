const db = require('../config/db');
const SegmentosService = require('./SegmentosService');

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
      elevationLossM,
      elevationMinM,
      elevationMaxM,
      avgCadenceSpm,
      maxCadenceSpm,
      avgHrBpm,
      maxHrBpm,
      minHrBpm,
      kcal,
      avgStrideM,
      avgInclinePct,
      points,
      splits,
      splitsMile,
      splitsTime,
      bestSegments
    } = payload || {};

    if (!id || !type || !endedAtMs) {
      throw new Error('Datos de actividad incompletos');
    }
    const tipo = ['RUN', 'WALK', 'BIKE'].includes(type) ? type : 'RUN';

    await db.query(
      `INSERT INTO gps_actividades
        (uuid_local, usuarios_id_usuario, tipo,
         iniciada_en, finalizada_en, duracion_seg, movimiento_seg,
         distancia_m, velocidad_media_mps, velocidad_max_mps,
         ritmo_medio_spkm, ritmo_min_spkm, ritmo_max_spkm,
         desnivel_pos_m, desnivel_neg_m, altitud_min_m, altitud_max_m,
         cadencia_media_ppm, cadencia_max_ppm,
         ritmo_cardiaco_medio, ritmo_cardiaco_max, ritmo_cardiaco_min,
         kcal, zancada_media_m, pendiente_media_pct,
         polyline_json, splits_json, splits_milla_json, splits_tiempo_json, mejores_segmentos_json)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
         desnivel_neg_m = VALUES(desnivel_neg_m),
         altitud_min_m = VALUES(altitud_min_m),
         altitud_max_m = VALUES(altitud_max_m),
         cadencia_media_ppm = VALUES(cadencia_media_ppm),
         cadencia_max_ppm = VALUES(cadencia_max_ppm),
         ritmo_cardiaco_medio = VALUES(ritmo_cardiaco_medio),
         ritmo_cardiaco_max = VALUES(ritmo_cardiaco_max),
         ritmo_cardiaco_min = VALUES(ritmo_cardiaco_min),
         kcal = VALUES(kcal),
         zancada_media_m = VALUES(zancada_media_m),
         pendiente_media_pct = VALUES(pendiente_media_pct),
         polyline_json = VALUES(polyline_json),
         splits_json = VALUES(splits_json),
         splits_milla_json = VALUES(splits_milla_json),
         splits_tiempo_json = VALUES(splits_tiempo_json),
         mejores_segmentos_json = VALUES(mejores_segmentos_json)`,
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
        Number(elevationLossM || 0),
        elevationMinM == null ? null : Number(elevationMinM),
        elevationMaxM == null ? null : Number(elevationMaxM),
        avgCadenceSpm == null ? null : Number(avgCadenceSpm),
        maxCadenceSpm == null ? null : Number(maxCadenceSpm),
        avgHrBpm == null ? null : Number(avgHrBpm),
        maxHrBpm == null ? null : Number(maxHrBpm),
        minHrBpm == null ? null : Number(minHrBpm),
        kcal == null ? null : Number(kcal),
        avgStrideM == null ? null : Number(avgStrideM),
        avgInclinePct == null ? null : Number(avgInclinePct),
        points ? JSON.stringify(points) : null,
        splits ? JSON.stringify(splits) : null,
        splitsMile ? JSON.stringify(splitsMile) : null,
        splitsTime ? JSON.stringify(splitsTime) : null,
        bestSegments ? JSON.stringify(bestSegments) : null
      ]
    );

    const [[row]] = await db.query(
      'SELECT id_actividad FROM gps_actividades WHERE usuarios_id_usuario = ? AND uuid_local = ?',
      [userId, id]
    );

    let segmentosGeo = [];
    try {
      const pts = Array.isArray(points) ? points : [];
      if (pts.length >= 2) {
        segmentosGeo = await SegmentosService.detectarDesdeActividad(userId, id, pts);
      }
    } catch (e) {
      console.warn('Gps segmentos geo:', e.message);
    }

    return { idActividad: row?.id_actividad, uuid: id, segmentosGeo };
  }

  static async listar(userId, { limit = 50, offset = 0 } = {}) {
    const [rows] = await db.query(
      `SELECT id_actividad, uuid_local, tipo, iniciada_en, finalizada_en,
              duracion_seg, distancia_m, ritmo_medio_spkm, velocidad_media_mps,
              desnivel_pos_m, desnivel_neg_m, altitud_min_m, altitud_max_m,
              ritmo_cardiaco_medio, kcal, zancada_media_m, pendiente_media_pct
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
      elevationLossM: Number(r.desnivel_neg_m || 0),
      elevationMinM: r.altitud_min_m == null ? null : Number(r.altitud_min_m),
      elevationMaxM: r.altitud_max_m == null ? null : Number(r.altitud_max_m),
      avgHrBpm: r.ritmo_cardiaco_medio,
      kcal: r.kcal,
      avgStrideM: r.zancada_media_m == null ? null : Number(r.zancada_media_m),
      avgInclinePct: r.pendiente_media_pct == null ? null : Number(r.pendiente_media_pct)
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
      elevationLossM: Number(r.desnivel_neg_m || 0),
      elevationMinM: r.altitud_min_m == null ? null : Number(r.altitud_min_m),
      elevationMaxM: r.altitud_max_m == null ? null : Number(r.altitud_max_m),
      avgCadenceSpm: r.cadencia_media_ppm == null ? null : Number(r.cadencia_media_ppm),
      maxCadenceSpm: r.cadencia_max_ppm,
      avgHrBpm: r.ritmo_cardiaco_medio,
      maxHrBpm: r.ritmo_cardiaco_max,
      minHrBpm: r.ritmo_cardiaco_min,
      kcal: r.kcal,
      avgStrideM: r.zancada_media_m == null ? null : Number(r.zancada_media_m),
      avgInclinePct: r.pendiente_media_pct == null ? null : Number(r.pendiente_media_pct),
      points: r.polyline_json ? JSON.parse(r.polyline_json) : [],
      splits: r.splits_json ? JSON.parse(r.splits_json) : [],
      splitsMile: r.splits_milla_json ? JSON.parse(r.splits_milla_json) : [],
      splitsTime: r.splits_tiempo_json ? JSON.parse(r.splits_tiempo_json) : [],
      bestSegments: r.mejores_segmentos_json ? JSON.parse(r.mejores_segmentos_json) : []
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
