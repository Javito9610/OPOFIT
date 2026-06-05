/**
 * Lugares de entreno cercanos (Google Places) + rutas sugeridas.
 * Requiere GOOGLE_MAPS_API_KEY en Railway/.env
 */
const crypto = require('crypto');

const TIPOS_LUGAR = {
  GYM: { placeType: 'gym', keyword: null, etiqueta: 'Gimnasio' },
  CROSSFIT: { placeType: 'gym', keyword: 'crossfit', etiqueta: 'CrossFit / Box' },
  PISTA: {
    placeType: 'stadium',
    keyword: 'pista atletismo',
    keywordsAlt: ['athletic track', 'running track', 'estadio'],
    etiqueta: 'Pista / Estadio'
  },
  CALISTENIA: {
    placeType: 'park',
    keyword: 'calisthenics',
    keywordsAlt: ['street workout', 'outdoor gym', 'calistenia', 'pull up bar park', 'parque calistenia'],
    textQueries: ['calisthenics park', 'street workout park', 'outdoor fitness park'],
    etiqueta: 'Parque / Calistenia'
  },
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
    const radio = Math.min(15000, Math.max(500, Number(radioM) || 5000));

    if (!key) {
      return MapasService.lugaresFallback(latN, lngN, tipo);
    }

    const vistos = new Set();
    const acumulado = [];

    const agregar = (results) => {
      for (const p of results || []) {
        if (!p?.place_id || vistos.has(p.place_id)) continue;
        vistos.add(p.place_id);
        acumulado.push({
          id: p.place_id,
          nombre: p.name,
          direccion: p.vicinity || p.formatted_address || '',
          lat: p.geometry?.location?.lat,
          lng: p.geometry?.location?.lng,
          rating: p.rating ?? null,
          abierto: p.opening_hours?.open_now ?? null,
          tipo,
          distanciaM: distanciaM(latN, lngN, p.geometry?.location?.lat, p.geometry?.location?.lng)
        });
      }
    };

    const keywords = [
      meta.keyword,
      ...(meta.keywordsAlt || [])
    ].filter(Boolean);

    for (const kw of keywords) {
      if (acumulado.length >= 20) break;
      const data = await MapasService.nearbySearch(latN, lngN, radio, meta.placeType, kw, key);
      agregar(data.results);
    }

    if (acumulado.length === 0 && meta.keyword == null && meta.placeType) {
      const data = await MapasService.nearbySearch(latN, lngN, radio, meta.placeType, null, key);
      agregar(data.results);
    }

    for (const query of meta.textQueries || []) {
      if (acumulado.length >= 20) break;
      const data = await MapasService.textSearch(latN, lngN, radio, query, key);
      agregar(data.results);
    }

    return acumulado
      .sort((a, b) => a.distanciaM - b.distanciaM)
      .slice(0, 20);
  }

  static async nearbySearch(lat, lng, radio, placeType, keyword, key) {
    let url =
      `https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${lat},${lng}` +
      `&radius=${radio}&type=${placeType}&key=${key}`;
    if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;
    const data = await fetchJson(url);
    if (data.status !== 'OK' && data.status !== 'ZERO_RESULTS') {
      throw new Error(data.error_message || `Places: ${data.status}`);
    }
    return data;
  }

  static async textSearch(lat, lng, radio, query, key) {
    const url =
      `https://maps.googleapis.com/maps/api/place/textsearch/json?query=${encodeURIComponent(query)}` +
      `&location=${lat},${lng}&radius=${radio}&key=${key}`;
    const data = await fetchJson(url);
    if (data.status !== 'OK' && data.status !== 'ZERO_RESULTS') {
      throw new Error(data.error_message || `Places text: ${data.status}`);
    }
    return data;
  }

  /** Fallback sin API key: puntos orientativos OSM-style simulados */
  static lugaresFallback(lat, lng, tipo) {
    const porTipo = {
      GYM: [
        [0.012, -0.004, 'Gimnasio local'],
        [-0.006, 0.012, 'Centro deportivo']
      ],
      CROSSFIT: [
        [0.01, 0.006, 'Box CrossFit demo'],
        [-0.008, 0.01, 'CrossFit local']
      ],
      PISTA: [
        [-0.01, -0.008, 'Pista de atletismo'],
        [0.014, 0.002, 'Estadio municipal']
      ],
      CALISTENIA: [
        [0.008, 0.005, 'Parque de calistenia'],
        [0.005, -0.009, 'Zona de barras al aire libre'],
        [-0.007, 0.006, 'Street workout park']
      ],
      PARQUE: [
        [0.008, 0.005, 'Parque municipal'],
        [-0.005, -0.007, 'Parque urbano']
      ]
    };
    const offsets = porTipo[tipo] || porTipo.PARQUE;
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
  static generarRutaSugerida(lat, lng, distKm = 5, variacion = 0) {
    const latN = Number(lat);
    const lngN = Number(lng);
    const km = Math.min(21, Math.max(1, Number(distKm) || 5));
    const seed = Math.max(0, Number(variacion) || 0);
    const puntos = MapasService.puntosCircuito(latN, lngN, km, seed);
    const distanciaTotalM = MapasService.longitudPolyline(puntos);
    const etiqueta = seed > 0 ? ` (opción ${(seed % 8) + 1})` : '';
    return {
      id: crypto.randomBytes(8).toString('hex'),
      nombre: `Rodaje ${km} km${etiqueta}`,
      distanciaKm: Number((distanciaTotalM / 1000).toFixed(2)),
      distanciaObjetivoKm: km,
      variacion: seed,
      puntos,
      origen: 'sugerida'
    };
  }

  /** Circuito con desplazamiento y giro según variacion → otra propuesta distinta. */
  static puntosCircuito(lat, lng, distKm, variacion = 0) {
    const n = 32;
    const seed = Math.max(0, Number(variacion) || 0);
    const radioM = (distKm * 1000) / (2 * Math.PI);
    const mPorGradoLat = 111320;
    const mPorGradoLng = 111320 * Math.cos((lat * Math.PI) / 180);
    const anguloBase = (seed % 16) * (Math.PI / 8);
    const centroLat = lat + Math.sin(seed * 0.61) * 0.004;
    const centroLng = lng + Math.cos(seed * 0.43) * 0.004;
    const pts = [];
    for (let i = 0; i <= n; i++) {
      const ang = (2 * Math.PI * i) / n + anguloBase;
      const dLat = (radioM * Math.sin(ang)) / mPorGradoLat;
      const dLng = (radioM * Math.cos(ang)) / mPorGradoLng;
      pts.push({ lat: centroLat + dLat, lng: centroLng + dLng });
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
