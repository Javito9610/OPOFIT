/**
 * Parser GPX 1.1 ligero (sin dependencias XML externas).
 * Usado para validar imports y poder ampliar a endpoint server-side.
 */

function parseTimeMs(raw) {
  if (!raw) return 0;
  const t = String(raw).trim();
  const d = new Date(t);
  return Number.isFinite(d.getTime()) ? d.getTime() : 0;
}

function haversineM(a, b) {
  const R = 6371000;
  const lat1 = (a.lat * Math.PI) / 180;
  const lat2 = (b.lat * Math.PI) / 180;
  const dLat = ((b.lat - a.lat) * Math.PI) / 180;
  const dLng = ((b.lng - a.lng) * Math.PI) / 180;
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
}

/** Extrae trkpt del XML GPX (regex simple, suficiente para exports estandar). */
function parseGpx(xml) {
  const text = String(xml || '');
  const points = [];
  const trkptRe = /<trkpt\s+lat="([^"]+)"\s+lon="([^"]+)"[^>]*>([\s\S]*?)<\/trkpt>/gi;
  let m;
  while ((m = trkptRe.exec(text)) !== null) {
    const lat = Number(m[1]);
    const lng = Number(m[2]);
    const inner = m[3];
    const eleM = inner.match(/<ele>([^<]+)<\/ele>/i);
    const timeM = inner.match(/<time>([^<]+)<\/time>/i);
    const hrM = inner.match(/<(?:gpxtpx:)?hr>([^<]+)<\/(?:gpxtpx:)?hr>/i);
    points.push({
      lat,
      lng,
      altitude: eleM ? Number(eleM[1]) : null,
      timestampMs: timeM ? parseTimeMs(timeM[1]) : 0,
      hrBpm: hrM ? Number(hrM[1]) : null
    });
  }
  if (points.length < 2) throw new Error('GPX sin puntos suficientes');

  const hasTimes = points.some((p) => p.timestampMs > 0);
  const normalized = hasTimes
    ? [...points].sort((a, b) => a.timestampMs - b.timestampMs)
    : points.map((p, i) => ({ ...p, timestampMs: Date.now() - (points.length - i) * 1000 }));

  let distanceM = 0;
  for (let i = 1; i < normalized.length; i++) {
    distanceM += haversineM(normalized[i - 1], normalized[i]);
  }
  if (distanceM < 25) throw new Error('Ruta demasiado corta');

  const startedAtMs = normalized[0].timestampMs;
  const endedAtMs = normalized[normalized.length - 1].timestampMs;
  const durationSec = Math.max(1, Math.round((endedAtMs - startedAtMs) / 1000));
  const avgSpeedMps = distanceM / durationSec;

  const typeM = text.match(/<type>([^<]+)<\/type>/i);
  let type = 'RUN';
  const t = (typeM?.[1] || '').toLowerCase();
  if (t.includes('cycl') || t.includes('bike')) type = 'BIKE';
  else if (t.includes('walk') || t.includes('hike')) type = 'WALK';

  return {
    id: `gpx_${startedAtMs}_${normalized.length}_${Math.round(distanceM)}`,
    type,
    startedAtMs,
    endedAtMs,
    durationSec,
    movingSec: durationSec,
    distanceM,
    avgSpeedMps,
    maxSpeedMps: avgSpeedMps,
    avgPaceSecPerKm: durationSec / (distanceM / 1000),
    points: normalized
  };
}

module.exports = { parseGpx, haversineM };
