const Parser = require('rss-parser');
const https = require('https');
const http = require('http');
const zlib = require('zlib');
const { URL } = require('url');
const db = require('../config/db');

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
    return RssService.stripHtmlTags(String(raw)).substring(0, 280);
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
    return (rows || []).map((n) => ({
      titulo: n.titulo,
      enlace: '',
      fecha: n.fecha_publicacion ? new Date(n.fecha_publicacion).toISOString() : '',
      fuente: 'OpoFit - Convocatoria',
      descripcion: (n.contenido || '').substring(0, 280),
      tipo: 'curada'
    }));
  }

  static async obtenerNoticiasRss(idOposicion) {
    const id = Number(idOposicion) || 1;
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
          todasNoticias.push({
            titulo,
            enlace: RssService._extractLink(item),
            fecha: item.pubDate || item.isoDate || '',
            fuente: feedConfig.nombre,
            descripcion: RssService._extractDescription(item),
            tipo: 'rss'
          });
        }
      } catch (error) {
        console.warn(`RSS ${feedConfig.url}: ${error.message}`);
      }
    }

    todasNoticias.sort((a, b) => {
      const da = a.fecha ? new Date(a.fecha) : new Date(0);
      const db = b.fecha ? new Date(b.fecha) : new Date(0);
      return db - da;
    });

    return todasNoticias.slice(0, MAX_TOTAL_NEWS);
  }
}

module.exports = RssService;
