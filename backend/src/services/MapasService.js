/**
 * Lugares de entreno cercanos (Google Places) + rutas sugeridas.
 * Requiere GOOGLE_MAPS_API_KEY en Railway/.env
 */
const crypto = require('crypto');

const TIPOS_LUGAR = {
  GYM: { placeType: 'gym', keyword: null, etiqueta: 'Gimnasio' },
  CROSSFIT: { placeType: 'gym', keyword: 'crossfit', etiqueta: 'CrossFit / Box' },
  PISTA: { placeType: 'stadium', keyword: 'pista atletismo', etiqueta: 'Pista / Estadio' },
  CALISTENIA: { placeType: 'park', keyword: 'calistenia barra', etiqueta: 'Parque / Calistenia' },
  PARQUE: { placeType: 'park', keyword: null, etiqueta: 'Parque' }
};

function apiKey() {
  return process.env.GOOGLE_MAPS_API_KEY || process.env.MAPS_API_KEY || '';
}

async function fetchJson(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Google API ${res.status}`);
  return res.json();
}

function distanciaM(lat1, lng1, lat2, lng2) {
  const R = 6371000;
  const toRad = (d) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

class MapasService {
  static listarTipos() {
    return Object.entries(TIPOS_LUGAR).map(([id, meta]) => ({
      id,
      etiqueta: meta.etiqueta
    }));
  }

  static async buscarLugares(lat, lng, tipo = 'GYM', radioM = 5000) {
    const key = apiKey();
    const latN = Number(lat);
    const lngN = Number(lng);
    if (!Number.isFinite(latN) || !Number.isFinite(lngN)) {
      throw new Error('Coordenadas inválidas');
    }
    const meta = TIPOS_LUGAR[tipo] || TIPOS_LUGAR.GYM;

    if (!key) {
      return MapasService.lugaresFallback(latN, lngN, tipo);
    }

    let url =
      `https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${latN},${lngN}` +
      `&radius=${Math.min(15000, Math.max(500, Number(radioM) || 5000))}&type=${meta.placeType}&key=${key}`;
    if (meta.keyword) url += `&keyword=${encodeURIComponent(meta.keyword)}`;

    const data = await fetchJson(url);
    if (data.status !== 'OK' && data.status !== 'ZERO_RESULTS') {
      throw new Error(data.error_message || `Places: ${data.status}`);
    }

    return (data.results || []).slice(0, 20).map((p) => ({
      id: p.place_id,
      nombre: p.name,
      direccion: p.vicinity || p.formatted_address || '',
      lat: p.geometry?.location?.lat,
      lng: p.geometry?.location?.lng,
      rating: p.rating ?? null,
      abierto: p.opening_hours?.open_now ?? null,
      tipo,
      distanciaM: distanciaM(latN, lngN, p.geometry?.location?.lat, p.geometry?.location?.lng)
    }));
  }

  /** Fallback sin API key: puntos orientativos OSM-style simulados */
  static lugaresFallback(lat, lng, tipo) {
    const offsets = [
      [0.008, 0.005, 'Parque municipal'],
      [-0.006, 0.012, 'Polideportivo municipal'],
      [0.012, -0.004, 'Gimnasio local'],
      [-0.01, -0.008, 'Pista de atletismo']
    ];
    return offsets.map(([dLat, dLng, nombre], i) => ({
      id: `fb_${tipo}_${i}`,
      nombre: `${nombre} (demo)`,
      direccion: 'Activa GOOGLE_MAPS_API_KEY para datos reales',
      lat: lat + dLat,
      lng: lng + dLng,
      rating: null,
      abierto: null,
      tipo,
      distanciaM: distanciaM(lat, lng, lat + dLat, lng + dLng),
      demo: true
    }));
  }

  /** Genera ruta circular sugerida (sin coste Directions API). */
  static generarRutaSugerida(lat, lng, distKm = 5) {
    const latN = Number(lat);
    const lngN = Number(lng);
    const km = Math.min(21, Math.max(1, Number(distKm) || 5));
    const puntos = MapasService.puntosCircuito(latN, lngN, km);
    const distanciaTotalM = MapasService.longitudPolyline(puntos);
    return {
      id: crypto.randomBytes(8).toString('hex'),
      nombre: `Rodaje ${km} km`,
      distanciaKm: Number((distanciaTotalM / 1000).toFixed(2)),
      distanciaObjetivoKm: km,
      puntos,
      origen: 'sugerida'
    };
  }

  static puntosCircuito(lat, lng, distKm) {
    const n = 32;
    const radioM = (distKm * 1000) / (2 * Math.PI);
    const mPorGradoLat = 111320;
    const mPorGradoLng = 111320 * Math.cos((lat * Math.PI) / 180);
    const pts = [];
    for (let i = 0; i <= n; i++) {
      const ang = (2 * Math.PI * i) / n;
      const dLat = (radioM * Math.sin(ang)) / mPorGradoLat;
      const dLng = (radioM * Math.cos(ang)) / mPorGradoLng;
      pts.push({ lat: lat + dLat, lng: lng + dLng });
    }
    return pts;
  }

  static longitudPolyline(puntos) {
    let m = 0;
    for (let i = 1; i < puntos.length; i++) {
      m += distanciaM(puntos[i - 1].lat, puntos[i - 1].lng, puntos[i].lat, puntos[i].lng);
    }
    return m;
  }

  static rutaEntreWaypoints(waypoints, nombre = 'Ruta personalizada') {
    const pts = (waypoints || [])
      .filter((p) => Number.isFinite(Number(p.lat)) && Number.isFinite(Number(p.lng)))
      .map((p) => ({ lat: Number(p.lat), lng: Number(p.lng) }));
    if (pts.length < 2) throw new Error('Se necesitan al menos 2 puntos');
    return {
      id: crypto.randomBytes(8).toString('hex'),
      nombre,
      distanciaKm: Number((MapasService.longitudPolyline(pts) / 1000).toFixed(2)),
      puntos: pts,
      origen: 'personalizada'
    };
  }

  static aGpx(ruta) {
    const pts = ruta.puntos || [];
    const now = new Date().toISOString();
    let body = `<?xml version="1.0" encoding="UTF-8"?>\n`;
    body += `<gpx version="1.1" creator="OpoFit" xmlns="http://www.topografix.com/GPX/1/1">\n`;
    body += `  <metadata><name>${escapeXml(ruta.nombre)}</name><time>${now}</time></metadata>\n`;
    body += `  <trk><name>${escapeXml(ruta.nombre)}</name><trkseg>\n`;
    for (const p of pts) {
      body += `    <trkpt lat="${p.lat.toFixed(7)}" lon="${p.lng.toFixed(7)}"><time>${now}</time></trkpt>\n`;
    }
    body += `  </trkseg></trk>\n</gpx>`;
    return body;
  }
}

function escapeXml(s) {
  return String(s || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

module.exports = MapasService;
