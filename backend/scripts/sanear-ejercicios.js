#!/usr/bin/env node
/**
 * Sanea el banco de ejercicios y el banco de planes:
 *   1. Elimina ejercicios surrealistas: combinaciones de volumen imposibles
 *      (ej. "Series 5000m × 12" = 60 km, "3000m × 12" = 36 km).
 *   2. Sustituye "vVO₂máx" / "vVO2max" por "ritmo VO2max" (más legible).
 *   3. Audita: reporta lo que cambia.
 *
 * Reglas científicas aplicadas para detectar volúmenes irreales:
 *   - 100-400m: máximo razonable de series es 12-15 (HIIT corto).
 *   - 600-1000m: máximo 8 series (típico 5-6 al ritmo objetivo).
 *   - 1200-2000m: máximo 5 series (Helgerud 4×4, 5×1000).
 *   - 3000-5000m: máximo 3 series y solo a ritmo suave.
 *   - Distancias largas (5km+) se hacen continuas, no en series.
 */
const fs = require('fs');
const path = require('path');

const ejFile = path.resolve(__dirname, '../data/ejercicios-banco-500.json');
const planesFile = path.resolve(__dirname, '../data/banco-planes-parsed.json');

// 1) Banco de ejercicios: borrar surrealistas
const ej = JSON.parse(fs.readFileSync(ejFile, 'utf8'));
const total0 = ej.ejercicios.length;

function esVolumenIrreal(nombre) {
  const m = String(nombre || '').match(/(\d+)\s*m\s*[xX×]\s*(\d+)/);
  if (!m) return false;
  const metros = parseInt(m[1], 10);
  const series = parseInt(m[2], 10);
  // Reglas de máximos razonables por distancia
  if (metros >= 5000 && series > 3) return true;
  if (metros >= 3000 && metros < 5000 && series > 5) return true;
  if (metros >= 2000 && metros < 3000 && series > 6) return true;
  if (metros >= 1200 && metros < 2000 && series > 8) return true;
  if (metros >= 600 && metros < 1200 && series > 12) return true;
  // 100-400m permitidos hasta 20 (HIIT corto)
  if (metros < 600 && series > 20) return true;
  // Volumen total = metros × series > 25000m (25km) en sesiones de series es absurdo
  if (metros * series > 25000) return true;
  return false;
}

const irreal = ej.ejercicios.filter((e) => esVolumenIrreal(e.nombre));
const lim = ej.ejercicios.filter((e) => !esVolumenIrreal(e.nombre));
ej.ejercicios = lim;
console.log(`Borrados ${irreal.length} ejercicios irreales (volumen):`);
irreal.slice(0, 15).forEach((e) => console.log('  ✗', e.nombre));
if (irreal.length > 15) console.log(`  ... y ${irreal.length - 15} más`);

// 2) Sustituir vVO2max por ritmo VO2max en NOMBRES de ejercicios
let renamedEj = 0;
ej.ejercicios.forEach((e) => {
  const original = e.nombre;
  const nuevo = original
    .replace(/vVO[₂2]m[aá]x/gi, 'ritmo VO2max')
    .replace(/\bvVO2\b/gi, 'ritmo VO2max');
  if (nuevo !== original) {
    e.nombre = nuevo;
    renamedEj += 1;
  }
});
console.log(`Renombrados ${renamedEj} ejercicios (vVO2max → ritmo VO2max)`);

fs.writeFileSync(ejFile, JSON.stringify(ej, null, 2));
console.log(`Banco ejercicios: ${total0} → ${ej.ejercicios.length}\n`);

// 3) Banco de planes: sustituir vVO2max en títulos/descripciones y nombres
const planes = JSON.parse(fs.readFileSync(planesFile, 'utf8'));
let renamedPlanes = 0;
function recorrer(obj) {
  if (obj == null) return;
  if (typeof obj === 'string') return; // strings sueltas no las podemos modificar in-place
  if (Array.isArray(obj)) {
    obj.forEach(recorrer);
    return;
  }
  for (const k of Object.keys(obj)) {
    const v = obj[k];
    if (typeof v === 'string') {
      const nuevo = v
        .replace(/vVO[₂2]m[aá]x/gi, 'ritmo VO2max')
        .replace(/\bvVO2\b/gi, 'ritmo VO2max');
      if (nuevo !== v) {
        obj[k] = nuevo;
        renamedPlanes += 1;
      }
    } else if (typeof v === 'object') {
      recorrer(v);
    }
  }
}
recorrer(planes);
console.log(`Banco planes: ${renamedPlanes} cadenas renombradas (vVO2max → ritmo VO2max)`);

fs.writeFileSync(planesFile, JSON.stringify(planes, null, 2));

// 4) Verifica que no queda nada raro
const restantes = ej.ejercicios.filter((e) => esVolumenIrreal(e.nombre));
const restantesVo = ej.ejercicios.filter((e) => /vVO/i.test(e.nombre));
console.log(`\nVerificación final:`);
console.log(`  Ejercicios surrealistas restantes: ${restantes.length}`);
console.log(`  vVO2 restantes: ${restantesVo.length}`);
