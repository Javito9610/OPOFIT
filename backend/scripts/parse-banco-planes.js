/**
 * Parsea opofit_banco_planes.md → JSON para importación en BD.
 * Uso: node scripts/parse-banco-planes.js
 */
const fs = require('fs');
const path = require('path');

const MD_PATH = path.resolve(__dirname, '../../../../opofit_banco_planes.md');
const OUT_PATH = path.resolve(__dirname, '../data/banco-planes-parsed.json');

const OPOSICIONES = [
  { section: '3.1', slug: 'pn_basica', nombre: 'Policía Nacional - Escala Básica', idPreferido: 1 },
  { section: '3.2', slug: 'pn_inspector', nombre: 'Policía Nacional - Escala Ejecutiva', idPreferido: 7 },
  { section: '3.3', slug: 'guardia_civil', nombre: 'Guardia Civil - Acceso Libre', idPreferido: 2 },
  { section: '3.4', slug: 'mossos', nombre: "Mossos d'Esquadra", idPreferido: 8 },
  { section: '3.5', slug: 'ertzaintza', nombre: 'Ertzaintza', idPreferido: 9 },
  { section: '3.6', slug: 'policia_navarra', nombre: 'Policía Foral de Navarra', idPreferido: 10 },
  { section: '3.7', slug: 'policia_local_madrid', nombre: 'Policía Local - Madrid', idPreferido: 11 },
  { section: '3.8', slug: 'bomberos_comun', nombre: 'Bomberos - Modelo común', idPreferido: 3 },
  { section: '3.9', slug: 'ejercito_tierra', nombre: 'Ejército de Tierra - Tropa y Marinería', idPreferido: 6 },
  { section: '3.10', slug: 'ejercito_aire_armada', nombre: 'Ejército del Aire / Armada', idPreferido: 12 },
  { section: '3.11', slug: 'policia_canaria', nombre: 'Policía Canaria', idPreferido: 13 },
  { section: '3.12', slug: 'penitenciarias', nombre: 'Ayudante de Instituciones Penitenciarias', idPreferido: 5 },
  { section: '3.13', slug: 'forestales', nombre: 'Agentes Forestales / Medio Ambiente', idPreferido: 14 },
  { section: '3.14', slug: 'vigilantes', nombre: 'Vigilantes de Seguridad', idPreferido: 15 }
];

const DIA_MAP = {
  lunes: 1,
  martes: 2,
  miércoles: 3,
  miercoles: 3,
  jueves: 4,
  viernes: 5,
  sábado: 6,
  sabado: 6,
  domingo: 7
};

function normalizarGenero(t) {
  if (/mujer/i.test(t)) return 'MUJER';
  return 'HOMBRE';
}

function normalizarNivel(t) {
  if (/avanzado/i.test(t)) return 'AVANZADO';
  if (/intermedio/i.test(t)) return 'INTERMEDIO';
  return 'BASICO';
}

function normalizarPilar(raw) {
  const p = String(raw || '').toUpperCase();
  if (p.startsWith('V')) return 'VELOCIDAD';
  if (p.startsWith('R')) return 'RESISTENCIA';
  return 'FUERZA';
}

function parsePrescripcion(chunk) {
  let text = chunk.trim().replace(/\s+/g, ' ');
  if (!text) return null;

  let series = 1;
  let repeticiones = 10;
  let descanso = 90;
  let nombre = text;

  const minMatch = text.match(/^(\d+)\s*['′]\s*(.*)$/i) || text.match(/^(\d+)\s*min\b/i);
  if (minMatch) {
    const mins = parseInt(minMatch[1], 10);
    nombre = (minMatch[2] || text).trim() || `Rodaje ${mins} min`;
    if (!/min/i.test(nombre)) nombre = `${nombre} ${mins} min`;
    return { nombre, series: 1, repeticiones: mins * 60, descanso: 0, unidad: 's' };
  }

  const distSeries = text.match(/^(\d+)×\s*(\d+)\s*m\b/i);
  if (distSeries) {
    return {
      nombre: text,
      series: parseInt(distSeries[1], 10),
      repeticiones: parseInt(distSeries[2], 10),
      descanso: 120,
      unidad: 'm'
    };
  }

  const serieRep = text.match(/^(.+?)\s+(\d+)×\s*(máx|max|\d+)/i);
  if (serieRep) {
    nombre = serieRep[1].trim();
    series = parseInt(serieRep[2], 10);
    const rep = serieRep[3].toLowerCase();
    repeticiones = rep === 'máx' || rep === 'max' ? 99 : parseInt(rep, 10);
    return { nombre, series, repeticiones, descanso: 90, unidad: 'reps' };
  }

  const escalera = text.match(/escalera\s+([\d\-]+)/i);
  if (escalera) {
    return { nombre: text, series: 1, repeticiones: 50, descanso: 120, unidad: 'reps' };
  }

  const vueltas = text.match(/(\d+)\s*vueltas?/i) || text.match(/(\d+)×\s*$/i);
  if (vueltas && !serieRep) {
    series = parseInt(vueltas[1], 10) || 4;
    return { nombre: text, series, repeticiones: 1, descanso: 60, unidad: 'reps' };
  }

  return { nombre: text, series, repeticiones, descanso, unidad: 'reps' };
}

function parseSesionEjercicios(sesionText) {
  const parts = sesionText.split('·').map((p) => p.trim()).filter(Boolean);
  const ejercicios = [];
  for (const part of parts) {
    const parsed = parsePrescripcion(part);
    if (parsed) ejercicios.push(parsed);
  }
  if (ejercicios.length === 0 && sesionText.trim()) {
    ejercicios.push(parsePrescripcion(sesionText.trim()));
  }
  return ejercicios;
}

function parseMarkdown(md) {
  const result = { oposiciones: [], generadoEn: new Date().toISOString() };

  for (const opoMeta of OPOSICIONES) {
    const re = new RegExp(
      `### ${opoMeta.section.replace('.', '\\.')}\\s+([^\\n]+)([\\s\\S]*?)(?=\\n### 3\\.\\d+ |\\n## 4\\.|$)`,
      'i'
    );
    const m = md.match(re);
    if (!m) {
      console.warn('[parse] No se encontró sección', opoMeta.section);
      continue;
    }
    const bloque = m[2];
    const planes = [];

    const subRe = /####\s+[\d.]+\s+(HOMBRES|MUJERES)\s+—\s+(?:Nivel\s+)?(Básico|Intermedio|Avanzado)([\s\S]*?)(?=####|### |$)/gi;
    let sub;
    while ((sub = subRe.exec(bloque)) !== null) {
      const genero = normalizarGenero(sub[1]);
      const nivel = normalizarNivel(sub[2]);
      const subBody = sub[3];
      const dias = [];
      const rowRe = /\|\s*\*\*(Lunes|Martes|Miércoles|Miercoles|Jueves|Viernes|Sábado|Sabado|Domingo)\*\*\s*\|\s*([^|]+)\|\s*([^|]+)\|/gi;
      let row;
      let orden = 0;
      while ((row = rowRe.exec(subBody)) !== null) {
        orden += 1;
        const diaNombre = row[1].toLowerCase();
        const diaSemana = DIA_MAP[diaNombre] || orden;
        const pilar = normalizarPilar(row[2]);
        const sesion = row[3].trim();
        const ejercicios = parseSesionEjercicios(sesion);
        dias.push({
          dia_semana: diaSemana,
          nombre_dia: row[1],
          orden,
          enfoque: pilar,
          titulo: sesion.slice(0, 120),
          descripcion: sesion,
          ejercicios
        });
      }
      if (dias.length) {
        planes.push({ genero, nivel, dias });
      }
    }

    result.oposiciones.push({
      slug: opoMeta.slug,
      nombre: opoMeta.nombre,
      idPreferido: opoMeta.idPreferido,
      planes
    });
    console.log(`[parse] ${opoMeta.slug}: ${planes.length} variantes nivel/género`);
  }

  return result;
}

function main() {
  const md = fs.readFileSync(MD_PATH, 'utf8');
  const data = parseMarkdown(md);
  fs.mkdirSync(path.dirname(OUT_PATH), { recursive: true });
  fs.writeFileSync(OUT_PATH, JSON.stringify(data, null, 2), 'utf8');
  const totalDias = data.oposiciones.reduce(
    (a, o) => a + o.planes.reduce((b, p) => b + p.dias.length, 0),
    0
  );
  console.log(`[parse] Guardado ${OUT_PATH} — ${data.oposiciones.length} oposiciones, ${totalDias} días de plan`);
}

main();
