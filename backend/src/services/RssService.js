const Parser = require('rss-parser');
const https = require('https');
const http = require('http');
const zlib = require('zlib');
const crypto = require('crypto');
const { URL } = require('url');
const db = require('../config/db');
const NotificationService = require('./NotificationService');

const parser = new Parser({
  timeout: 10000,
  headers: {
    'User-Agent': 'OpoFit/1.0',
    Accept: 'application/rss+xml, application/xml, text/xml'
  }
});

const ITEMS_PER_FEED = 15;
const MAX_TOTAL_NEWS = 25;

/** Palabras clave para filtrar entradas del BOE genérico */
const KEYWORDS_BY_OPO = {
  1: [
    'policía nacional',
    'policia nacional',
    'cuerpo nacional de policía',
    'escala básica',
    'cnsp',
    'ingreso policía'
  ],
  2: ['guardia civil', 'acceso libre', 'ingreso guardia civil', 'escala de cabos'],
  3: ['bombero', 'bomberos', 'servicio de extinción', 'incendios', 'conductor bomberos'],
  4: ['bombero', 'bomberos', 'ayuntamiento de madrid', 'cuerpo de bomberos'],
  5: [
    'instituciones penitenciarias',
    'penitenciarias',
    'ayudante de instituciones',
    'funcionario de prisiones'
  ],
  6: [
    'ejército de tierra',
    'ejercito de tierra',
    'tropa y marinería',
    'tropa y marineria',
    'ingreso militar',
    'fuerzas armadas'
  ]
};

const RSS_FEEDS = {
  1: [
    { url: 'https://www.boe.es/rss/canal.php?c=policia', nombre: 'BOE - Policía Nacional' }
  ],
  2: [
    { url: 'https://www.boe.es/rss/canal.php?c=defensa', nombre: 'BOE - Defensa' },
    {
      url: 'https://web.guardiacivil.es/es/administracion/atom_noticias.html',
      nombre: 'Guardia Civil - Noticias'
    }
  ],
  3: [
    { url: 'https://www.boe.es/rss/canal.php?c=empleo_publico', nombre: 'BOE - Empleo público' },
    { url: 'https://www.bocm.es/boletin/rss/bocm.xml', nombre: 'BOCM - Comunidad de Madrid' }
  ],
  4: [
    { url: 'https://www.boe.es/rss/canal.php?c=empleo_publico', nombre: 'BOE - Empleo público' },
    { url: 'https://www.bocm.es/boletin/rss/bocm.xml', nombre: 'BOCM - Madrid' }
  ],
  5: [
    { url: 'https://www.boe.es/rss/canal.php?c=personal', nombre: 'BOE - Personal' },
    { url: 'https://www.boe.es/rss/canal.php?c=empleo_publico', nombre: 'BOE - Empleo público' }
  ],
  6: [
    { url: 'https://www.boe.es/rss/canal.php?c=defensa', nombre: 'BOE - Defensa' },
    { url: 'https://www.boe.es/rss/canal.php?c=empleo_publico', nombre: 'BOE - Empleo público' }
  ]
};

const FEEDS_GENERICOS_FILTRAR = new Set([
  'https://www.boe.es/rss/boe.php?s=2B',
  'https://www.boe.es/rss/canal.php?c=empleo_publico',
  'https://www.boe.es/rss/canal.php?c=personal'
]);

/** Títulos ya notificados por push (persistencia ligera en memoria + hash) */
const _alertasEnviadas = new Set();

/** In-memory news cache with 30-minute TTL per oposicion ID. */
const _newsCache = new Map();
const CACHE_TTL_MS = 30 * 60 * 1000;

function getCached(id) {
  const entry = _newsCache.get(id);
  if (!entry) return null;
  if (Date.now() - entry.ts > CACHE_TTL_MS) {
    _newsCache.delete(id);
    return null;
  }
  return entry.data;
}

function setCache(id, data) {
  _newsCache.set(id, { ts: Date.now(), data });
}

const PATRONES_CATEGORIA = {
  convocatoria: [
    'convocatoria',
    'proceso selectivo',
    'oposición',
    'oposicion',
    'ingreso',
    'plaza',
    'acceso libre',
    'escala básica',
    'escala basica'
  ],
  plazo: [
    'plazo',
    'inscripción',
    'inscripcion',
    'solicitud',
    'fecha límite',
    'fecha limite',
    'hasta el día',
    'hasta el dia',
    'presentación de instancias'
  ]
};

class RssService {
  static _matchesOposicion(texto, idOposicion) {
    const t = (texto || '').toLowerCase();
    const keys = KEYWORDS_BY_OPO[idOposicion] || KEYWORDS_BY_OPO[1];
    return keys.some((k) => t.includes(k));
  }

  static _extractLink(item) {
    if (!item) return '';
    if (typeof item.link === 'string' && item.link) return item.link;
    const links = item.links;
    if (Array.isArray(links) && links.length > 0) {
      const first = links[0];
      if (typeof first === 'string') return first;
      if (first?.url) return first.url;
      if (first?.href) return first.href;
    }
    if (typeof item.guid === 'string' && item.guid.startsWith('http')) return item.guid;
    return '';
  }

  static _extractDescription(item) {
    const raw =
      item?.contentSnippet || item?.summary || item?.content || item?.['content:encoded'] || '';
    return RssService.stripHtmlTags(String(raw)).substring(0, 500);
  }

  static _resumen(texto, max = 140) {
    const t = RssService.stripHtmlTags(String(texto || '')).replace(/\s+/g, ' ').trim();
    if (!t) return '';
    const cortado = t.length <= max ? t : `${t.substring(0, max - 1).trim()}…`;
    const punto = cortado.indexOf('. ');
    if (punto > 40 && punto < max) return cortado.substring(0, punto + 1);
    return cortado;
  }

  static _clasificar(titulo, descripcion, tipoBase = 'rss') {
    if (tipoBase === 'curada') return 'convocatoria';
    const t = `${titulo} ${descripcion}`.toLowerCase();
    if (PATRONES_CATEGORIA.plazo.some((k) => t.includes(k))) return 'plazo';
    if (PATRONES_CATEGORIA.convocatoria.some((k) => t.includes(k))) return 'convocatoria';
    return 'noticia';
  }

  static _esUrgente(categoria) {
    return categoria === 'convocatoria' || categoria === 'plazo';
  }

  static _hashAlerta(idOposicion, titulo) {
    return crypto.createHash('sha1').update(`${idOposicion}:${titulo}`).digest('hex');
  }

  static _enriquecerNoticia(noticia, idOposicion) {
    const descripcion = noticia.descripcion || '';
    const categoria = RssService._clasificar(noticia.titulo, descripcion, noticia.tipo);
    return {
      ...noticia,
      categoria,
      resumen: RssService._resumen(descripcion || noticia.titulo),
      urgente: RssService._esUrgente(categoria),
      relevancia: RssService._matchesOposicion(`${noticia.titulo} ${descripcion}`, idOposicion)
        ? 'alta'
        : 'media'
    };
  }

  static stripHtmlTags(text) {
    let result = text;
    let previous;
    do {
      previous = result;
      result = result.replace(/<[^>]*>/g, '');
    } while (result !== previous);
    return result;
  }

  static _httpGetText(url, maxRedirects = 5) {
    return new Promise((resolve, reject) => {
      const u = new URL(url);
      const lib = u.protocol === 'http:' ? http : https;
      const req = lib.request(
        {
          protocol: u.protocol,
          hostname: u.hostname,
          path: `${u.pathname}${u.search}`,
          method: 'GET',
          headers: {
            'User-Agent': 'OpoFit/1.0',
            Accept: 'application/rss+xml, application/xml, text/xml, */*',
            'Accept-Language': 'es-ES,es;q=0.9'
          },
          timeout: 15000
        },
        (res) => {
          const status = res.statusCode || 0;
          const location = res.headers.location;
          if ([301, 302, 303, 307, 308].includes(status) && location && maxRedirects > 0) {
            res.resume();
            const nextUrl = location.startsWith('http')
              ? location
              : new URL(location, url).toString();
            return resolve(RssService._httpGetText(nextUrl, maxRedirects - 1));
          }
          const chunks = [];
          res.on('data', (chunk) => chunks.push(chunk));
          res.on('end', () => {
            if (status >= 400) return reject(new Error(`HTTP ${status}`));
            const buf = Buffer.concat(chunks);
            const enc = (res.headers['content-encoding'] || '').toString().toLowerCase();
            try {
              let out = buf;
              if (enc.includes('gzip')) out = zlib.gunzipSync(buf);
              else if (enc.includes('deflate')) out = zlib.inflateSync(buf);
              else if (enc.includes('br')) out = zlib.brotliDecompressSync(buf);
              resolve(out.toString('utf8'));
            } catch {
              resolve(buf.toString('utf8'));
            }
          });
        }
      );
      req.on('error', reject);
      req.on('timeout', () => {
        try {
          req.destroy(new Error('Timeout'));
        } catch (_) {}
      });
      req.end();
    });
  }

  static async _parseFeed(url) {
    try {
      return await parser.parseURL(url);
    } catch {
      const xml = await RssService._httpGetText(url);
      const trimmed = (xml || '').trim();
      if (!trimmed.startsWith('<') || trimmed.toLowerCase().includes('<html')) {
        throw new Error('Respuesta no XML');
      }
      const sanitized = trimmed.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '');
      return parser.parseString(sanitized);
    }
  }

  static async obtenerNoticiasCuradas(idOposicion) {
    const [rows] = await db.query(
      `SELECT titulo, contenido, fecha_publicacion
       FROM noticias WHERE oposiciones_id_oposicion = ?
       ORDER BY fecha_publicacion DESC LIMIT 10`,
      [idOposicion]
    );
    return (rows || []).map((n) =>
      RssService._enriquecerNoticia(
        {
          titulo: n.titulo,
          enlace: '',
          fecha: n.fecha_publicacion ? new Date(n.fecha_publicacion).toISOString() : '',
          fuente: 'OpoFit - Convocatoria',
          descripcion: (n.contenido || '').substring(0, 500),
          tipo: 'curada'
        },
        idOposicion
      )
    );
  }

  static invalidarCache(idOposicion) {
    if (idOposicion) _newsCache.delete(Number(idOposicion));
    else _newsCache.clear();
  }

  static async obtenerNoticiasRss(idOposicion) {
    const id = Number(idOposicion) || 1;
    const cached = getCached(id);
    if (cached) return cached;
    const curadas = await RssService.obtenerNoticiasCuradas(id);
    const feeds = RSS_FEEDS[id] || RSS_FEEDS[1];
    const todasNoticias = [...curadas];
    const seen = new Set(curadas.map((n) => n.titulo?.toLowerCase()));

    for (const feedConfig of feeds) {
      try {
        const feed = await RssService._parseFeed(feedConfig.url);
        const filtrarGenerico = FEEDS_GENERICOS_FILTRAR.has(feedConfig.url);

        for (const item of (feed.items || []).slice(0, ITEMS_PER_FEED)) {
          const titulo = item.title || 'Sin título';
          const texto = `${titulo} ${RssService._extractDescription(item)}`;
          if (filtrarGenerico && !RssService._matchesOposicion(texto, id)) continue;
          if (!filtrarGenerico && feedConfig.url.includes('bocm') && !RssService._matchesOposicion(texto, id)) {
            if (id === 3 || id === 4) {
              if (!texto.toLowerCase().includes('bomber')) continue;
            }
          }
          const key = titulo.toLowerCase();
          if (seen.has(key)) continue;
          seen.add(key);
          todasNoticias.push(
            RssService._enriquecerNoticia(
              {
                titulo,
                enlace: RssService._extractLink(item),
                fecha: item.pubDate || item.isoDate || '',
                fuente: feedConfig.nombre,
                descripcion: RssService._extractDescription(item),
                tipo: 'rss'
              },
              id
            )
          );
        }
      } catch (error) {
        console.warn(`RSS ${feedConfig.url}: ${error.message}`);
      }
    }

    todasNoticias.sort((a, b) => {
      const pa = a.urgente ? 1 : 0;
      const pb = b.urgente ? 1 : 0;
      if (pb !== pa) return pb - pa;
      const da = a.fecha ? new Date(a.fecha) : new Date(0);
      const db = b.fecha ? new Date(b.fecha) : new Date(0);
      return db - da;
    });

    const result = todasNoticias.slice(0, MAX_TOTAL_NEWS);
    setCache(id, result);
    return result;
  }

  /** Envía push por nuevas convocatorias/plazos (cron cada 6 h). */
  static async pollYNotificarAlertas() {
    const ids = [1, 2, 3, 4, 5, 6];
    let enviados = 0;
    for (const id of ids) {
      try {
        // Invalidate cache so the cron always fetches fresh news.
        RssService.invalidarCache(id);
        const noticias = await RssService.obtenerNoticiasRss(id);
        const urgentes = noticias.filter((n) => n.urgente && n.relevancia === 'alta').slice(0, 3);
        for (const n of urgentes) {
          const hash = RssService._hashAlerta(id, n.titulo);
          if (_alertasEnviadas.has(hash)) continue;
          _alertasEnviadas.add(hash);
          const etiqueta = n.categoria === 'plazo' ? '⏰ Plazo' : '📢 Convocatoria';
          const r = await NotificationService.enviarNoticiaOposicion(
            id,
            `${etiqueta} — ${n.titulo}`.slice(0, 120),
            n.resumen || n.descripcion?.slice(0, 160) || 'Nueva actualización en tu oposición'
          );
          enviados += r.enviados || 0;
        }
      } catch (e) {
        console.warn(`[rss-alert] opo ${id}: ${e.message}`);
      }
    }
    return { enviados, oposiciones: ids.length };
  }
}

module.exports = RssService;
