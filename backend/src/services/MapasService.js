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

  /** Genera ruta circular sugerida siguiendo calles (Directions / OSRM). */
  static async generarRutaSugerida(lat, lng, distKm = 5, variacion = 0) {
    const latN = Number(lat);
    const lngN = Number(lng);
    const km = MapasService.redondearKm(distKm);
    const seed = Math.max(0, Number(variacion) || 0);
    const etiqueta = seed > 0 ? ` (opción ${(seed % 8) + 1})` : '';

    let puntos = null;
    let origen = 'sugerida_geometrica';

    try {
      const porCalles = await MapasService.rutaCircularPorCalles(latN, lngN, km, seed);
      if (porCalles?.puntos?.length >= 2) {
        puntos = porCalles.puntos;
        origen = porCalles.origen;
      }
    } catch (e) {
      console.warn('Ruta por calles:', e.message);
    }

    if (!puntos) {
      puntos = MapasService.puntosCircuito(latN, lngN, km, seed);
    }

    const distanciaTotalM = MapasService.longitudPolyline(puntos);
    return {
      id: crypto.randomBytes(8).toString('hex'),
      nombre: `Rodaje ${km} km${etiqueta}`,
      distanciaKm: Number((distanciaTotalM / 1000).toFixed(2)),
      distanciaObjetivoKm: km,
      variacion: seed,
      puntos: MapasService.simplificarPolyline(puntos),
      origen
    };
  }

  static redondearKm(distKm) {
    const km = Math.min(21, Math.max(1, Number(distKm) || 5));
    return Math.round(km * 10) / 10;
  }

  /** Busca un bucle por calles cercano a la distancia objetivo. */
  static async rutaCircularPorCalles(lat, lng, distKm, seed) {
    const key = apiKey();
    const numWp = 4 + (seed % 3);
    const anguloBase = (seed % 16) * (Math.PI / 8);
    const centroLat = lat + Math.sin(seed * 0.61) * 0.003;
    const centroLng = lng + Math.cos(seed * 0.43) * 0.003;
    const scales = [0.9, 1.05, 1.2, 1.35, 1.5, 1.7];
    let mejor = null;
    let mejorDiff = Infinity;

    for (const scale of scales) {
      const radioM = ((distKm * 1000) / (2 * Math.PI)) * scale;
      const anillo = MapasService.waypointsAnillo(centroLat, centroLng, radioM, numWp, anguloBase);
      const wps = [{ lat, lng }, ...anillo, { lat, lng }];

      let puntos = null;
      let origen = null;
      try {
        if (key) {
          puntos = await MapasService.directionsRoute(wps, key, 'walking');
          origen = 'sugerida_calles';
        }
      } catch (_e) {
        puntos = null;
      }
      if (!puntos) {
        try {
          puntos = await MapasService.osrmRoute(wps);
          origen = 'sugerida_osrm';
        } catch (_e) {
          continue;
        }
      }
      if (!puntos?.length) continue;

      const distM = MapasService.longitudPolyline(puntos);
      const diff = Math.abs(distM / 1000 - distKm);
      if (diff < mejorDiff) {
        mejorDiff = diff;
        mejor = { puntos, origen };
        if (diff <= distKm * 0.1) break;
      }
    }

    return mejor;
  }

  static waypointsAnillo(lat, lng, radioM, n, anguloBase = 0) {
    const mPorGradoLat = 111320;
    const mPorGradoLng = 111320 * Math.cos((lat * Math.PI) / 180);
    const pts = [];
    for (let i = 0; i < n; i++) {
      const ang = anguloBase + (2 * Math.PI * i) / n;
      pts.push({
        lat: lat + (radioM * Math.sin(ang)) / mPorGradoLat,
        lng: lng + (radioM * Math.cos(ang)) / mPorGradoLng
      });
    }
    return pts;
  }

  static async directionsRoute(waypoints, key, mode = 'walking') {
    const wps = (waypoints || []).filter(
      (p) => Number.isFinite(Number(p.lat)) && Number.isFinite(Number(p.lng))
    );
    if (wps.length < 2) throw new Error('Waypoints insuficientes');

    const origin = `${wps[0].lat},${wps[0].lng}`;
    const destination = `${wps[wps.length - 1].lat},${wps[wps.length - 1].lng}`;
    const middle = wps.slice(1, -1).map((p) => `${p.lat},${p.lng}`).join('|');

    let url =
      `https://maps.googleapis.com/maps/api/directions/json?origin=${origin}` +
      `&destination=${destination}&mode=${mode}&key=${key}`;
    if (middle) url += `&waypoints=${encodeURIComponent(middle)}`;

    const data = await fetchJson(url);
    if (data.status !== 'OK') {
      throw new Error(data.error_message || `Directions: ${data.status}`);
    }

    const encoded = data.routes?.[0]?.overview_polyline?.points;
    if (!encoded) throw new Error('Directions sin polyline');
    return decodePolyline(encoded);
  }

  static async osrmRoute(waypoints) {
    const wps = (waypoints || []).filter(
      (p) => Number.isFinite(Number(p.lat)) && Number.isFinite(Number(p.lng))
    );
    if (wps.length < 2) throw new Error('Waypoints insuficientes');

    const coords = wps.map((p) => `${p.lng},${p.lat}`).join(';');
    const url =
      `https://router.project-osrm.org/route/v1/foot/${coords}` +
      '?overview=full&geometries=geojson&steps=false';
    const res = await fetch(url, { headers: { 'User-Agent': 'OpoFit/1.0' } });
    if (!res.ok) throw new Error(`OSRM ${res.status}`);
    const data = await res.json();
    if (data.code !== 'Ok' || !data.routes?.[0]) {
      throw new Error(data.message || 'OSRM sin ruta');
    }
    return data.routes[0].geometry.coordinates.map(([lng, lat]) => ({ lat, lng }));
  }

  static simplificarPolyline(puntos, max = 450) {
    if (!puntos?.length || puntos.length <= max) return puntos || [];
    const step = Math.ceil(puntos.length / max);
    return puntos.filter((_, i) => i % step === 0 || i === puntos.length - 1);
  }

  /** Fallback geométrico si no hay routing disponible. */
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

  static async rutaEntreWaypoints(waypoints, nombre = 'Ruta personalizada') {
    const pts = (waypoints || [])
      .filter((p) => Number.isFinite(Number(p.lat)) && Number.isFinite(Number(p.lng)))
      .map((p) => ({ lat: Number(p.lat), lng: Number(p.lng) }));
    if (pts.length < 2) throw new Error('Se necesitan al menos 2 puntos');

    let routed = pts;
    let origen = 'personalizada';
    const key = apiKey();
    try {
      if (key) {
        routed = await MapasService.directionsRoute(pts, key, 'walking');
        origen = 'personalizada_calles';
      } else {
        routed = await MapasService.osrmRoute(pts);
        origen = 'personalizada_osrm';
      }
    } catch (e) {
      console.warn('Ruta personalizada por calles:', e.message);
    }

    return {
      id: crypto.randomBytes(8).toString('hex'),
      nombre,
      distanciaKm: Number((MapasService.longitudPolyline(routed) / 1000).toFixed(2)),
      puntos: MapasService.simplificarPolyline(routed),
      origen
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

/** Decodifica polyline de Google Directions. */
function decodePolyline(encoded) {
  const points = [];
  let index = 0;
  let lat = 0;
  let lng = 0;
  while (index < encoded.length) {
    let shift = 0;
    let result = 0;
    let b;
    do {
      b = encoded.charCodeAt(index++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    const dlat = (result & 1) ? ~(result >> 1) : (result >> 1);
    lat += dlat;
    shift = 0;
    result = 0;
    do {
      b = encoded.charCodeAt(index++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    const dlng = (result & 1) ? ~(result >> 1) : (result >> 1);
    lng += dlng;
    points.push({ lat: lat / 1e5, lng: lng / 1e5 });
  }
  return points;
}

module.exports = MapasService;
