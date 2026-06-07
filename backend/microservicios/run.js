#!/usr/bin/env node
/**
 * Orquestador CLI de los microservicios.
 * Uso:
 *   node microservicios/run.js noticias
 *   node microservicios/run.js baremos
 *   node microservicios/run.js all
 *
 * Sale con exit code != 0 si algún microservicio devolvió error grave,
 * para que CI/CD (GitHub Actions, Railway cron, etc.) lo detecte.
 */
require('dotenv').config();

async function main() {
  const arg = (process.argv[2] || 'all').toLowerCase();
  const targets = arg === 'all' ? ['noticias', 'baremos'] : [arg];
  let exitCode = 0;

  for (const t of targets) {
    try {
      let mod;
      if (t === 'noticias') mod = require('./noticias-micro');
      else if (t === 'baremos') mod = require('./baremo-check-micro');
      else throw new Error(`microservicio desconocido: ${t}`);
      await mod.ejecutar();
    } catch (e) {
      console.error(`[run] ${t} falló:`, e.message);
      exitCode = 1;
    }
  }
  // Cierre limpio del pool MySQL (si existe)
  try {
    const db = require('../src/config/db');
    if (typeof db.end === 'function') await db.end();
  } catch (_) {}
  process.exit(exitCode);
}

main().catch((e) => {
  console.error('[run] error fatal:', e.message);
  process.exit(2);
});
