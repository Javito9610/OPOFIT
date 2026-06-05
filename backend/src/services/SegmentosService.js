const db = require('../config/db');
const crypto = require('crypto');

const SEGMENTOS_VIRTUALES = [
  { slug: '50m', nombre: 'Mejor 50 m', distanciaM: 50, mejorMenor: 1, categoria: 'VELOCIDAD' },
  { slug: '100m', nombre: 'Mejor 100 m', distanciaM: 100, mejorMenor: 1, categoria: 'VELOCIDAD' },
  { slug: '1km', nombre: 'Mejor 1 km', distanciaM: 1000, mejorMenor: 1, categoria: 'CARRERA' },
  { slug: '5km', nombre: 'Mejor 5 km', distanciaM: 5000, mejorMenor: 1, categoria: 'CARRERA' },
  { slug: '10km', nombre: 'Mejor 10 km', distanciaM: 10000, mejorMenor: 1, categoria: 'CARRERA' },
  { slug: '21km', nombre: 'Mejor media maratón', distanciaM: 21097.5, mejorMenor: 1, categoria: 'CARRERA' },
  { slug: '42km', nombre: 'Mejor maratón', distanciaM: 42195, mejorMenor: 1, categoria: 'CARRERA' }
];

const RADIO_PUERTA_M = 45;

function haversineM(lat1, lng1, lat2, lng2) {
  const R = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function distanciaAPunto(lat, lng, pLat, pLng) {
  return haversineM(lat, lng, pLat, pLng);
}

function slugify(texto) {
  const base = String(texto || 'segmento')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .substring(0, 24) || 'segmento';
  return `${base}-${crypto.randomBytes(3).toString('hex')}`;
}

class SegmentosService {
  static async seedVirtuales() {
    for (const s of SEGMENTOS_VIRTUALES) {
      await db.query(
        `INSERT INTO segmentos (slug, nombre, tipo, distancia_m, mejor_si_menor, categoria, activo)
         VALUES (?, ?, 'VIRTUAL', ?, ?, ?, 1)
         ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), distancia_m = VALUES(distancia_m)`,
        [s.slug, s.nombre, s.distanciaM, s.mejorMenor, s.categoria]
      );
    }
  }

  static async listar() {
    const [rows] = await db.query(
      `SELECT id_segmento, slug, nombre, tipo, distancia_m, mejor_si_menor, categoria,
              lat_inicio, lng_inicio, lat_fin, lng_fin
       FROM segmentos WHERE activo = 1 ORDER BY distancia_m ASC`
    );
    return rows || [];
  }

  static async crearGeografico(userId, body) {
    const nombre = String(body.nombre || '').trim().substring(0, 120);
    const latInicio = Number(body.latInicio);
    const lngInicio = Number(body.lngInicio);
    const latFin = Number(body.latFin);
    const lngFin = Number(body.lngFin);
    if (!nombre) throw new Error('NOMBRE_OBLIGATORIO');
    if (![latInicio, lngInicio, latFin, lngFin].every(Number.isFinite)) {
      throw new Error('COORDENADAS_INVALIDAS');
    }
    const distM = haversineM(latInicio, lngInicio, latFin, lngFin);
    if (distM < 50) throw new Error('SEGMENTO_MUY_CORTO');

    const slug = slugify(nombre);
    const categoria = String(body.categoria || 'CARRERA').substring(0, 32);

    const [ins] = await db.query(
      `INSERT INTO segmentos
        (slug, nombre, tipo, distancia_m, mejor_si_menor, categoria,
         lat_inicio, lng_inicio, lat_fin, lng_fin, activo)
       VALUES (?, ?, 'GPS', ?, 1, ?, ?, ?, ?, ?, 1)`,
      [slug, nombre, distM, categoria, latInicio, lngInicio, latFin, lngFin]
    );

    return {
      idSegmento: ins.insertId,
      slug,
      nombre,
      distanciaM: distM,
      latInicio,
      lngInicio,
      latFin,
      lngFin,
      creadorId: userId
    };
  }

  static detectarPaso(points, segmento, radioM = RADIO_PUERTA_M) {
    const latIni = Number(segmento.lat_inicio);
    const lngIni = Number(segmento.lng_inicio);
    const latFin = Number(segmento.lat_fin);
    const lngFin = Number(segmento.lng_fin);
    if (![latIni, lngIni, latFin, lngFin].every(Number.isFinite)) return null;

    let startIdx = -1;
    for (let i = 0; i < points.length; i++) {
      const p = points[i];
      const plat = Number(p.lat);
      const plng = Number(p.lng);
      if (!Number.isFinite(plat) || !Number.isFinite(plng)) continue;
      if (distanciaAPunto(plat, plng, latIni, lngIni) <= radioM) {
        startIdx = i;
        break;
      }
    }
    if (startIdx < 0) return null;

    let endIdx = -1;
    for (let j = startIdx + 1; j < points.length; j++) {
      const p = points[j];
      const plat = Number(p.lat);
      const plng = Number(p.lng);
      if (!Number.isFinite(plat) || !Number.isFinite(plng)) continue;
      if (distanciaAPunto(plat, plng, latFin, lngFin) <= radioM) {
        endIdx = j;
        break;
      }
    }
    if (endIdx < 0) return null;

    const t0 = Number(points[startIdx].timestampMs || 0);
    const t1 = Number(points[endIdx].timestampMs || 0);
    const duracionMs = t1 - t0;
    if (!Number.isFinite(duracionMs) || duracionMs < 3000) return null;

    let distRecorrida = 0;
    for (let k = startIdx + 1; k <= endIdx; k++) {
      const a = points[k - 1];
      const b = points[k];
      distRecorrida += haversineM(Number(a.lat), Number(a.lng), Number(b.lat), Number(b.lng));
    }
    const distSeg = Number(segmento.distancia_m) || haversineM(latIni, lngIni, latFin, lngFin);
    if (distRecorrida < distSeg * 0.45) return null;

    return { duracionMs, startIdx, endIdx, distRecorridaM: distRecorrida };
  }

  static async detectarDesdeActividad(userId, gpsUuid, points) {
    if (!gpsUuid || !Array.isArray(points) || points.length < 2) return [];

    const [segmentos] = await db.query(
      `SELECT id_segmento, slug, nombre, lat_inicio, lng_inicio, lat_fin, lng_fin,
              distancia_m, mejor_si_menor
       FROM segmentos WHERE activo = 1 AND tipo = 'GPS'`
    );

    const resultados = [];
    for (const seg of segmentos || []) {
      const paso = SegmentosService.detectarPaso(points, seg);
      if (!paso) continue;
      const r = await SegmentosService.registrarEsfuerzo(
        userId,
        seg.id_segmento,
        paso.duracionMs,
        gpsUuid
      );
      resultados.push({ slug: seg.slug, nombre: seg.nombre, ...r });
    }
    return resultados;
  }

  static async ranking(idSegmento, limite = 50) {
    const [[seg]] = await db.query(
      'SELECT * FROM segmentos WHERE id_segmento = ? AND activo = 1',
      [idSegmento]
    );
    if (!seg) throw new Error('SEGMENTO_NO_ENCONTRADO');

    const orden = seg.mejor_si_menor ? 'ASC' : 'DESC';
    const [rows] = await db.query(
      `SELECT e.id_esfuerzo, e.duracion_ms, e.creado_en, e.gps_uuid,
              u.id_usuario, u.nombre AS usuario_nombre, u.avatar_url
       FROM segmento_esfuerzos e
       JOIN usuarios u ON e.id_usuario = u.id_usuario
       WHERE e.id_segmento = ?
       ORDER BY e.duracion_ms ${orden}, e.creado_en ASC
       LIMIT ?`,
      [idSegmento, Number(limite)]
    );

    const mejor = rows?.[0]?.duracion_ms ?? null;
    return {
      segmento: {
        idSegmento: seg.id_segmento,
        slug: seg.slug,
        nombre: seg.nombre,
        tipo: seg.tipo,
        distanciaM: Number(seg.distancia_m),
        mejorSiMenor: !!seg.mejor_si_menor,
        categoria: seg.categoria,
        latInicio: seg.lat_inicio == null ? null : Number(seg.lat_inicio),
        lngInicio: seg.lng_inicio == null ? null : Number(seg.lng_inicio),
        latFin: seg.lat_fin == null ? null : Number(seg.lat_fin),
        lngFin: seg.lng_fin == null ? null : Number(seg.lng_fin)
      },
      ranking: (rows || []).map((r, i) => ({
        posicion: i + 1,
        usuarioId: r.id_usuario,
        usuarioNombre: r.usuario_nombre,
        avatarUrl: r.avatar_url,
        duracionMs: Number(r.duracion_ms),
        duracionSec: Number((r.duracion_ms / 1000).toFixed(2)),
        gpsUuid: r.gps_uuid,
        creadoEn: r.creado_en,
        esRecord: mejor != null && Number(r.duracion_ms) === Number(mejor)
      }))
    };
  }

  static async registrarEsfuerzo(userId, idSegmento, duracionMs, gpsUuid = null) {
    const ms = Number(duracionMs);
    if (!Number.isFinite(ms) || ms <= 0) throw new Error('TIEMPO_INVALIDO');

    const [[seg]] = await db.query(
      'SELECT id_segmento, mejor_si_menor FROM segmentos WHERE id_segmento = ? AND activo = 1',
      [idSegmento]
    );
    if (!seg) throw new Error('SEGMENTO_NO_ENCONTRADO');

    const [prev] = await db.query(
      'SELECT duracion_ms FROM segmento_esfuerzos WHERE id_usuario = ? AND id_segmento = ? ORDER BY duracion_ms ASC LIMIT 1',
      [userId, idSegmento]
    );
    const anterior = prev?.[0]?.duracion_ms != null ? Number(prev[0].duracion_ms) : null;
    const esMejor = anterior == null || ms < anterior;

    await db.query(
      `INSERT INTO segmento_esfuerzos (id_segmento, id_usuario, duracion_ms, gps_uuid)
       VALUES (?, ?, ?, ?)`,
      [idSegmento, userId, ms, gpsUuid ? String(gpsUuid).substring(0, 64) : null]
    );

    return { esMejor, duracionMs: ms, duracionAnteriorMs: anterior };
  }

  static async registrarDesdeActividad(userId, gpsUuid, esfuerzos) {
    if (!gpsUuid || !Array.isArray(esfuerzos) || !esfuerzos.length) {
      throw new Error('DATOS_INCOMPLETOS');
    }
    const resultados = [];
    for (const e of esfuerzos) {
      const slug = String(e.slug || e.segmentoSlug || '').trim();
      const duracionMs = Number(e.duracionMs ?? (Number(e.duracionSec) * 1000));
      if (!slug || !Number.isFinite(duracionMs)) continue;

      const [[seg]] = await db.query(
        'SELECT id_segmento FROM segmentos WHERE slug = ? AND activo = 1',
        [slug]
      );
      if (!seg) continue;

      const r = await SegmentosService.registrarEsfuerzo(userId, seg.id_segmento, duracionMs, gpsUuid);
      resultados.push({ slug, ...r });
    }
    return resultados;
  }

  static slugPorDistanciaM(distM) {
    const map = {
      50: '50m',
      100: '100m',
      1000: '1km',
      5000: '5km',
      10000: '10km',
      21097.5: '21km',
      42195: '42km'
    };
    return map[distM] || null;
  }
}

module.exports = SegmentosService;
