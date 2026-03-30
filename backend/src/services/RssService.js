const Parser = require('rss-parser');

const parser = new Parser({
    timeout: 10000,
    headers: {
        'User-Agent': 'OpoFit/1.0',
        'Accept': 'application/rss+xml, application/xml, text/xml'
    }
});

const ITEMS_PER_FEED = 5;
const MAX_TOTAL_NEWS = 10;
const MAX_DESCRIPTION_LENGTH = 300;

const RSS_FEEDS = {
    1: [
        {
            url: 'https://www.boe.es/rss/canal.php?c=policia',
            nombre: 'BOE - Policía Nacional'
        },
        {
            url: 'https://www.boe.es/rss/canal.php?c=boe',
            nombre: 'BOE - Boletín Oficial del Estado'
        }
    ],
    2: [
        {
            url: 'https://www.boe.es/rss/canal.php?c=guardia_civil',
            nombre: 'BOE - Guardia Civil'
        },
        {
            url: 'https://www.boe.es/rss/canal.php?c=boe',
            nombre: 'BOE - Boletín Oficial del Estado'
        }
    ]
};

class RssService {

    static async obtenerNoticiasRss(idOposicion) {
        const feeds = RSS_FEEDS[idOposicion] || RSS_FEEDS[1];
        const todasNoticias = [];

        for (const feedConfig of feeds) {
            try {
                const feed = await parser.parseURL(feedConfig.url);
                const items = (feed.items || []).slice(0, ITEMS_PER_FEED).map(item => ({
                    titulo: item.title || 'Sin título',
                    enlace: item.link || '',
                    fecha: item.pubDate || item.isoDate || '',
                    fuente: feedConfig.nombre,
                    descripcion: (item.contentSnippet || item.content || '')
                        .substring(0, MAX_DESCRIPTION_LENGTH)
                        .replace(/<[^>]*>/g, '')
                }));
                todasNoticias.push(...items);
            } catch (error) {
                console.error(`Error al obtener RSS de ${feedConfig.url}:`, error.message);
            }
        }

        todasNoticias.sort((a, b) => {
            const dateA = a.fecha ? new Date(a.fecha) : new Date(0);
            const dateB = b.fecha ? new Date(b.fecha) : new Date(0);
            return dateB - dateA;
        });

        return todasNoticias.slice(0, MAX_TOTAL_NEWS);
    }
}

module.exports = RssService;
