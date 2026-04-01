const Parser = require('rss-parser');
const https = require('https');
const http = require('http');
const zlib = require('zlib');
const {
  URL
} = require('url');
const parser = new Parser({
  timeout: 10000,
  headers: {
    'User-Agent': 'OpoFit/1.0',
    'Accept': 'application/rss+xml, application/xml, text/xml'
  }
});
const ITEMS_PER_FEED = 12;
const MAX_TOTAL_NEWS = 30;
const MAX_DESCRIPTION_LENGTH = 300;
const RSS_FEEDS = {
  1: [{
    url: 'https://www.boe.es/rss/canal.php?c=policia',
    nombre: 'BOE - Policía Nacional'
  }, {
    url: 'https://www.boe.es/rss/boe.php?s=2B',
    nombre: 'BOE - Oposiciones y concursos'
  }],
  2: [{
    url: 'https://web.guardiacivil.es/es/administracion/atom_noticias.html',
    nombre: 'Guardia Civil - Noticias'
  }, {
    url: 'https://www.boe.es/rss/boe.php?s=2B',
    nombre: 'BOE - Oposiciones y concursos'
  }, {
    url: 'https://www.boe.es/rss/canal.php?c=empleo_publico',
    nombre: 'BOE - Empleo público'
  }, {
    url: 'https://www.boe.es/rss/canal.php?c=personal',
    nombre: 'BOE - Personal'
  }]
};
const RSS_DEBUG = String(process.env.RSS_DEBUG || '').toLowerCase() === 'true';
class RssService {
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
    if (typeof item.id === 'string' && item.id.startsWith('http')) return item.id;
    return '';
  }
  static _extractDescription(item) {
    if (!item) return '';
    const raw = item.contentSnippet || item.summary || item.content || item['content:encoded'] || '';
    return RssService.stripHtmlTags(String(raw)).substring(0, MAX_DESCRIPTION_LENGTH);
  }
  static _httpGetText(url, maxRedirects = 5) {
    return new Promise((resolve, reject) => {
      const u = new URL(url);
      const lib = u.protocol === 'http:' ? http : https;
      const req = lib.request({
        protocol: u.protocol,
        hostname: u.hostname,
        path: `${u.pathname}${u.search}`,
        method: 'GET',
        headers: {
          'User-Agent': 'OpoFit/1.0',
          'Accept': 'application/rss+xml, application/xml, text/xml, */*',
          'Accept-Language': 'es-ES,es;q=0.9,en;q=0.8',
          'Accept-Encoding': 'gzip, deflate, br'
        },
        timeout: 15000
      }, res => {
        const status = res.statusCode || 0;
        const location = res.headers.location;
        if ([301, 302, 303, 307, 308].includes(status) && location && maxRedirects > 0) {
          res.resume();
          const nextUrl = location.startsWith('http') ? location : new URL(location, url).toString();
          return resolve(RssService._httpGetText(nextUrl, maxRedirects - 1));
        }
        const chunks = [];
        res.on('data', chunk => chunks.push(chunk));
        res.on('end', () => {
          if (status >= 400) return reject(new Error(`HTTP ${status}`));
          const buf = Buffer.concat(chunks);
          const enc = (res.headers['content-encoding'] || '').toString().toLowerCase();
          try {
            let out = buf;
            if (enc.includes('gzip')) out = zlib.gunzipSync(buf);else if (enc.includes('deflate')) out = zlib.inflateSync(buf);else if (enc.includes('br')) out = zlib.brotliDecompressSync(buf);
            resolve(out.toString('utf8'));
          } catch (e) {
            resolve(buf.toString('utf8'));
          }
        });
      });
      req.on('error', reject);
      req.on('timeout', () => {
        try {
          req.destroy(new Error('Timeout'));
        } catch (_) {}
      });
      req.end();
    });
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
  static async obtenerNoticiasRss(idOposicion) {
    const feeds = RSS_FEEDS[idOposicion] || RSS_FEEDS[1];
    const todasNoticias = [];
    for (const feedConfig of feeds) {
      try {
        let feed;
        try {
          feed = await parser.parseURL(feedConfig.url);
        } catch (e) {
          const xml = await RssService._httpGetText(feedConfig.url);
          const trimmed = (xml || '').trim();
          if (!trimmed.startsWith('<') || trimmed.toLowerCase().includes('<html')) {
            throw new Error('Respuesta no XML (posible HTML/intermedio)');
          }
          const sanitized = trimmed.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '');
          feed = await parser.parseString(sanitized);
        }
        if (!feed?.items || feed.items.length === 0) {
          const xml = await RssService._httpGetText(feedConfig.url);
          const trimmed = (xml || '').trim();
          if (trimmed.startsWith('<') && !trimmed.toLowerCase().includes('<html')) {
            const sanitized = trimmed.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '');
            const retryFeed = await parser.parseString(sanitized);
            if (retryFeed?.items && retryFeed.items.length > 0) {
              feed = retryFeed;
            }
          }
        }
        const items = (feed.items || []).slice(0, ITEMS_PER_FEED).map(item => ({
          titulo: item.title || 'Sin título',
          enlace: RssService._extractLink(item),
          fecha: item.pubDate || item.isoDate || '',
          fuente: feedConfig.nombre,
          descripcion: RssService._extractDescription(item)
        }));
        todasNoticias.push(...items);
      } catch (error) {
        if (RSS_DEBUG) {
          console.warn(`RSS: error en ${feedConfig.url}: ${error.message}`);
        }
      }
    }
    todasNoticias.sort((a, b) => {
      const dateA = a.fecha ? new Date(a.fecha) : new Date(0);
      const dateB = b.fecha ? new Date(b.fecha) : new Date(0);
      return dateB - dateA;
    });
    const pool = todasNoticias.slice(0, MAX_TOTAL_NEWS);
    for (let i = pool.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [pool[i], pool[j]] = [pool[j], pool[i]];
    }
    return pool;
  }
}
module.exports = RssService;
