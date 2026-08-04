/**
 * Genera con IA la ficha explicativa de TODOS los ejercicios sin ficha y la
 * cachea en la BD (ejercicios.explicacion_json). Se ejecuta UNA vez.
 *
 * Requisitos: una clave de IA en el .env (recomendado GROQ_API_KEY, gratis):
 *   GROQ_API_KEY=gsk_...    (https://console.groq.com  — gratis, sin tarjeta)
 *   o CLAUDE_API_KEY / OPENAI_API_KEY / GEMINI_API_KEY
 *
 * Uso:  node scripts/generar-explicaciones.js
 * Es RESUMIBLE: si lo paras y lo relanzas, sigue por donde iba (salta las ya hechas).
 */
require('dotenv').config();
const EjercicioExplicacionAiService = require('../src/services/EjercicioExplicacionAiService');
const db = require('../src/config/db');

(async () => {
  if (!EjercicioExplicacionAiService.disponible()) {
    console.error('\n❌ No hay clave de IA configurada en .env.');
    console.error('   Añade GROQ_API_KEY=... (gratis en https://console.groq.com) y vuelve a ejecutar.\n');
    process.exit(1);
  }
  console.log('▶ Generando explicaciones con IA (esto puede tardar unos minutos)...\n');
  let total = 0;
  let vueltas = 0;
  /* eslint-disable no-await-in-loop */
  while (true) {
    const r = await EjercicioExplicacionAiService.procesarLote(40, 300);
    total += r.generados;
    vueltas++;
    console.log(`  lote ${vueltas}: +${r.generados} generados, ${r.fallidos} fallidos, quedan ${r.restantes}`);
    if (r.restantes === 0) break;
    if (r.generados === 0 && r.fallidos === 0) break; // solo quedaban curadas/sin datos
    if (vueltas > 60) { console.log('  (límite de seguridad de vueltas alcanzado)'); break; }
  }
  console.log(`\n✅ Listo. ${total} explicaciones generadas y guardadas en la BD.\n`);
  await db.end?.();
  process.exit(0);
})().catch((e) => {
  console.error('Error:', e.message);
  process.exit(1);
});
