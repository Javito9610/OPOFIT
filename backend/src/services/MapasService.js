/**
 * Lugares de entreno cercanos (Google Places + Overpass/OSM fallback) + rutas sugeridas.
 * Google Places se usa si GOOGLE_MAPS_API_KEY está configurada.
 * Si no, Overpass API (OpenStreetMap, gratuito) ofrece datos reales sin clave.
 */
const crypto = require('crypto');

const TIPOS_LUGAR = {
  GYM: { placeType: 'gym', keyword: null, etiqueta: 'Gimnasio' },
  CROSSFIT: {
    placeType: null,
    keyword: 'crossfit',
    keywordsAlt: ['box crossfit'],
    textQueries: ['crossfit box', 'crossfit gym'],
    etiqueta: 'CrossFit / Box'
  },
  PISTA: {
    placeType: null,
    keyword: 'pista atletismo',
    keywordsAlt: ['athletic track', 'running track', 'pista de atletismo'],
    textQueries: ['pista atletismo', 'athletic track', 'estadio atletismo'],
    etiqueta: 'Pista / Estadio'
  },
  CALISTENIA: {
    placeType: 'park',
    keyword: 'calisthenics',
    keywordsAlt: [
      'street workout',
      'outdoor gym',
      'parque calistenia',
      'barras paralelas',
      'gimnasio al aire libre'
    ],
    textQueries: [
      'calisthenics park',
      'street workout park',
      'outdoor fitness park',
      'parque calistenia',
      'parque de barras'
    ],
    radioDefault: 12000,
    etiqueta: 'Parque / Calistenia'
  },
  PARQUE: { placeType: 'park', keyword: null, etiqueta: 'Parque' }
};

// OSM tags queried via Overpass for each tipo
const OVERPASS_TAGS = {
  GYM: [
    'node["amenity"="gym"]',
    'way["amenity"="gym"]',
    'node["leisure"="fitness_centre"]',
    'way["leisure"="fitness_centre"]'
  ],
  CROSSFIT: [
    'node["sport"="crossfit"]',
    'way["sport"="crossfit"]',
    'node["amenity"="gym"]["sport"="crossfit"]',
    'way["amenity"="gym"]["sport"="crossfit"]'
  ],
  PISTA: [
    'node["leisure"="track"]',
    'way["leisure"="track"]',
    'node["sport"="athletics"]',
    'way["sport"="athletics"]',
    'node["leisure"="sports_centre"]["sport"="athletics"]',
    'way["leisure"="sports_centre"]["sport"="athletics"]'
  ],
  CALISTENIA: [
    'node["leisure"="fitness_station"]',
    'way["leisure"="fitness_station"]',
    'node["sport"="calisthenics"]',
    'node["leisure"="outdoor_gym"]',
    'way["leisure"="outdoor_gym"]'
  ],
  PARQUE: [
    'node["leisure"="park"]',
    'way["leisure"="park"]',
    'node["leisure"="garden"]',
    'way["leisure"="garden"]'
  ]
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
    const radioDefecto = meta.radioDefault || 5000;
    const radio = Math.min(20000, Math.max(500, Number(radioM) || radioDefecto));

    // 1. Google Places (requires API key with Places API enabled)
    if (key) {
      try {
        const result = await MapasService.buscarConGoogle(latN, lngN, radio, tipo, meta, key);
        if (result.length > 0) return result;
      } catch (e) {
        console.warn(`Google Places error (tipo=${tipo}): ${e.message} — usando Overpass`);
      }
    }

    // 2. Overpass API (OpenStreetMap, gratuito, sin clave)
    try {
      const result = await MapasService.overpassLugares(latN, lngN, radio, tipo);
      if (result.length > 0) return result;
    } catch (e) {
      console.warn(`Overpass error (tipo=${tipo}): ${e.message} — usando fallback geométrico`);
    }

    // 3. Fallback geométrico (datos de demostración)
    return MapasService.lugaresFallback(latN, lngN, tipo);
  }

  static async buscarConGoogle(latN, lngN, radio, tipo, meta, key) {
    const vistos = new Set();
    const acumulado = [];

    const agregar = (results) => {
      for (const p of results || []) {
        if (!p?.place_id || vistos.has(p.place_id)) continue;
        const pLat = p.geometry?.location?.lat;
        const pLng = p.geometry?.location?.lng;
        if (!Number.isFinite(pLat) || !Number.isFinite(pLng)) continue;
        vistos.add(p.place_id);
        acumulado.push({
          id: p.place_id,
          nombre: p.name,
          direccion: p.vicinity || p.formatted_address || '',
          lat: pLat,
          lng: pLng,
          rating: p.rating ?? null,
          abierto: p.opening_hours?.open_now ?? null,
          tipo,
          distanciaM: distanciaM(latN, lngN, pLat, pLng)
        });
      }
    };

    const keywords = [meta.keyword, ...(meta.keywordsAlt || [])].filter(Boolean);

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

    return acumulado.sort((a, b) => a.distanciaM - b.distanciaM).slice(0, 20);
  }

  static async overpassLugares(lat, lng, radioM, tipo) {
    const tags = OVERPASS_TAGS[tipo] || OVERPASS_TAGS.GYM;
    const aroundFilter = `(around:${radioM},${lat},${lng})`;
    const unionBody = tags.map((t) => `  ${t}${aroundFilter};`).join('\n');
    const query = `[out:json][timeout:20];\n(\n${unionBody}\n);\nout center 20;`;

    const resp = await fetch('https://overpass-api.de/api/interpreter', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': 'OpoFit/1.0' },
      body: `data=${encodeURIComponent(query)}`
    });
    if (!resp.ok) throw new Error(`Overpass HTTP ${resp.status}`);
    const data = await resp.json();

    const etiqueta = (TIPOS_LUGAR[tipo] || TIPOS_LUGAR.GYM).etiqueta;
    const seen = new Set();
    return (data.elements || [])
      .map((el) => {
        const elLat = el.lat ?? el.center?.lat;
        const elLng = el.lon ?? el.center?.lon;
        if (!Number.isFinite(elLat) || !Number.isFinite(elLng)) return null;
        const nombre = el.tags?.name || el.tags?.['name:es'] || etiqueta;
        const street = el.tags?.['addr:street'] || '';
        const num = el.tags?.['addr:housenumber'] || '';
        const direccion = [street, num].filter(Boolean).join(' ');
        return {
          id: `osm_${el.id}`,
          nombre,
          direccion,
          lat: elLat,
          lng: elLng,
          rating: null,
          abierto: null,
          tipo,
          distanciaM: distanciaM(lat, lng, elLat, elLng)
        };
      })
      .filter((l) => l !== null && !seen.has(l.id) && seen.add(l.id))
      .sort((a, b) => a.distanciaM - b.distanciaM)
      .slice(0, 20);
  }

  static async nearbySearch(lat, lng, radio, placeType, keyword, key) {
    let url =
      `https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${lat},${lng}` +
      `&radius=${radio}&key=${key}`;
    if (placeType) url += `&type=${placeType}`;
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

  static limitesRuta(actividad = 'CARRERA', terreno = 'CIUDAD') {
    const act = String(actividad || 'CARRERA').toUpperCase();
    const ter = String(terreno || 'CIUDAD').toUpperCase();
    if (act === 'BICI' || act === 'BIKE' || act === 'BICICLETA') {
      return ter === 'MONTANA' || ter === 'MONTAÑA'
        ? { min: 1, max: 100, label: 'Bici montaña' }
        : { min: 1, max: 180, label: 'Bici carretera' };
    }
    return { min: 1, max: 42, label: ter === 'MONTANA' || ter === 'MONTAÑA' ? 'Carrera montaña' : 'Carrera ciudad' };
  }

  /** Genera ruta circular sugerida siguiendo calles (Directions / OSRM). */
  static async generarRutaSugerida(lat, lng, distKm = 5, variacion = 0, actividad = 'CARRERA', terreno = 'CIUDAD') {
    const latN = Number(lat);
    const lngN = Number(lng);
    const km = MapasService.redondearKm(distKm, actividad, terreno);
    const seed = Math.max(0, Number(variacion) || 0);
    const limites = MapasService.limitesRuta(actividad, terreno);
    const etiqueta = seed > 0 ? ` · opción ${(seed % 8) + 1}` : '';

    let puntos = null;
    let origen = 'sugerida_geometrica';

    try {
      const porCalles = await MapasService.rutaCircularPorCalles(latN, lngN, km, seed, actividad, terreno);
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
    const act = String(actividad || 'CARRERA').toUpperCase();
    let prefijo = 'Carrera';
    if (act === 'BICI' || act === 'BIKE' || act === 'BICICLETA') prefijo = 'Ruta bici';
    else if (act === 'CAMINAR' || act === 'WALK') prefijo = 'Caminata';
    return {
      id: crypto.randomBytes(8).toString('hex'),
      nombre: `${prefijo} ${km} km${etiqueta}`,
      distanciaKm: Number((distanciaTotalM / 1000).toFixed(2)),
      distanciaObjetivoKm: km,
      variacion: seed,
      actividad: String(actividad || 'CARRERA').toUpperCase(),
      terreno: String(terreno || 'CIUDAD').toUpperCase(),
      puntos: MapasService.simplificarPolyline(puntos),
      origen
    };
  }

  static redondearKm(distKm, actividad = 'CARRERA', terreno = 'CIUDAD') {
    const { min, max } = MapasService.limitesRuta(actividad, terreno);
    const km = Math.min(max, Math.max(min, Number(distKm) || 5));
    return Math.round(km * 10) / 10;
  }

  static modoRuta(actividad) {
    const act = String(actividad || 'CARRERA').toUpperCase();
    return act === 'BICI' || act === 'BIKE' || act === 'BICICLETA' ? 'bicycling' : 'walking';
  }

  static perfilOsrm(actividad) {
    const act = String(actividad || 'CARRERA').toUpperCase();
    return act === 'BICI' || act === 'BIKE' || act === 'BICICLETA' ? 'bike' : 'foot';
  }

  /** Busca un bucle por calles cercano a la distancia objetivo. */
  static async rutaCircularPorCalles(lat, lng, distKm, seed, actividad = 'CARRERA', terreno = 'CIUDAD') {
    const key = apiKey();
    const numWp = 4 + (seed % 3);
    const anguloBase = (seed % 16) * (Math.PI / 8);
    const centroLat = lat + Math.sin(seed * 0.61) * 0.003;
    const centroLng = lng + Math.cos(seed * 0.43) * 0.003;
    const esMontana = terreno === 'MONTANA' || terreno === 'MONTAÑA';
    const scalesBase = esMontana
      ? [0.55, 0.65, 0.75, 0.85, 0.95, 1.05, 1.2, 1.4, 1.65]
      : [0.42, 0.50, 0.58, 0.66, 0.74, 0.82, 0.90, 0.98, 1.08, 1.18, 1.30];
    const mode = MapasService.modoRuta(actividad);
    const osrmProfile = MapasService.perfilOsrm(actividad);

    const probarScales = async (scales) => {
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
            puntos = await MapasService.directionsRoute(wps, key, mode);
            origen = 'sugerida_calles';
          }
        } catch (_e) {
          puntos = null;
        }
        if (!puntos) {
          try {
            puntos = await MapasService.osrmRoute(wps, osrmProfile);
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
      return { mejor, mejorDiff };
    };

    let { mejor, mejorDiff } = await probarScales(scalesBase);

    if (mejor && mejorDiff > distKm * 0.12) {
      const actualKm = MapasService.longitudPolyline(mejor.puntos) / 1000;
      const ratio = actualKm > 0 ? distKm / actualKm : 1;
      const refinados = scalesBase
        .map((s) => Math.round(s * ratio * 100) / 100)
        .filter((s, i, arr) => s >= 0.35 && s <= 1.85 && arr.indexOf(s) === i);
      const segundo = await probarScales(refinados);
      if (segundo.mejor && segundo.mejorDiff < mejorDiff) {
        mejor = segundo.mejor;
        mejorDiff = segundo.mejorDiff;
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

  static async osrmRoute(waypoints, profile = 'foot') {
    const wps = (waypoints || []).filter(
      (p) => Number.isFinite(Number(p.lat)) && Number.isFinite(Number(p.lng))
    );
    if (wps.length < 2) throw new Error('Waypoints insuficientes');

    const prof = profile === 'bike' ? 'bike' : 'foot';
    const coords = wps.map((p) => `${p.lng},${p.lat}`).join(';');
    const url =
      `https://router.project-osrm.org/route/v1/${prof}/${coords}` +
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

  static async rutaEntreWaypoints(waypoints, nombre = 'Ruta personalizada', actividad = 'CARRERA') {
    const pts = (waypoints || [])
      .filter((p) => Number.isFinite(Number(p.lat)) && Number.isFinite(Number(p.lng)))
      .map((p) => ({ lat: Number(p.lat), lng: Number(p.lng) }));
    if (pts.length < 2) throw new Error('Se necesitan al menos 2 puntos');

    let routed = pts;
    let origen = 'personalizada';
    const key = apiKey();
    const mode = MapasService.modoRuta(actividad);
    const osrmProfile = MapasService.perfilOsrm(actividad);
    try {
      if (key) {
        routed = await MapasService.directionsRoute(pts, key, mode);
        origen = 'personalizada_calles';
      } else {
        routed = await MapasService.osrmRoute(pts, osrmProfile);
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
