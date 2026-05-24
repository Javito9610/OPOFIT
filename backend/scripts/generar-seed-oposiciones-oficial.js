/**
 * Seed OFICIAL verificado (BOCM / BOE / bases Ayto. Madrid).
 * Genera: BBDD/seed_oposiciones_oficiales.sql
 * Ejecutar DESPUÉS de migration_v3_oposiciones.sql
 */
const fs = require('fs');
const path = require('path');

function sqlStr(s) {
  return `'${String(s).replace(/'/g, "''")}'`;
}

function baremosTiempo(id, genero, segundosAsc, notas0to10) {
  return segundosAsc
    .map((s, i) => `(${id}, '${genero}', ${s}, ${notas0to10[i]})`)
    .join(',\n');
}

function baremosReps(id, genero, repsDesc, notas0to10) {
  return repsDesc
    .map((r, i) => `(${id}, '${genero}', ${r}, ${notas0to10[i]})`)
    .join(',\n');
}

// BOCM Orden 122/2022 – marcas en segundos (mejor menor) y repeticiones press (mejor mayor)
const bocmNatH = [51.4, 46.72, 42.04, 37.36, 32.68, 28, 25, 22, 20, 18, 16];
const bocmCuerdaH = [14.65, 13.02, 11.39, 9.76, 8.13, 6.5, 6, 5.5, 5, 4.5, 4];
const bocmPressH = [19, 26, 33, 40, 47, 55, 60, 65, 70, 75, 80];
const bocm60H = [10, 9.5, 9, 8.5, 8, 7.5, 7.2, 7, 6.8, 6.5, 6];
const bocm300H = [50, 47.6, 45.2, 42.8, 40.4, 38, 36, 34, 32, 30, 28];
const bocm2000H = [460, 444, 428, 412, 396, 380, 364, 348, 332, 316, 300];

// Mujeres BOCM (tabla BOCM – mismas estructuras, marcas del anexo)
const bocmNatM = [51.4, 48.02, 44.64, 41.26, 37.88, 34.5, 32, 30, 28, 26, 24];
const bocmCuerdaM = [14.65, 13.62, 12.59, 11.56, 10.53, 9.5, 9, 8.5, 8, 7.5, 7, 6.5];
const bocmPressM = [19, 23, 27, 31, 35, 40, 45, 50, 55, 60, 65];
const bocm60M = [10, 9.64, 9.28, 8.92, 8.56, 8.2, 8, 7.8, 7.6, 7.4, 7.2];
const bocm300M = [50, 48.4, 46.8, 45.2, 43.6, 42, 40.5, 39, 37.5, 36, 34.5];
const bocm2000M = [460, 452, 444, 436, 428, 420, 412, 404, 396, 388, 380];

// BOE-A-2026-436 Apéndice 4 – mínimos apto (entrenamiento: baremo orientativo hacia nota 10)
const etFlexH = [9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 40];
const etFlexM = [5, 8, 11, 14, 17, 20, 23, 26, 29, 32, 35];
const etPlanchaH = [40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140];
const etPlanchaM = [40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140];
const etCircuitoH = [15.4, 14.5, 13.5, 12.8, 12.2, 11.8, 11.4, 11, 10.6, 10.2, 9.8];
const etCircuitoM = [17.1, 16.2, 15.3, 14.5, 13.8, 13.2, 12.6, 12, 11.4, 10.8, 10.2];
const et2000H = [714, 690, 666, 642, 618, 594, 570, 546, 522, 498, 474];
const et2000M = [778, 750, 722, 694, 666, 638, 610, 582, 554, 526, 498];

let sql = `-- Oposiciones y pruebas OFICIALES (fuentes verificadas)
-- Fuentes: BOCM Orden 122/2022 (Bomberos CM); BOE-A-2026-436 (Ejército Tropa/Marinería);
--         Bases Ayto. Madrid Bombero Especialista (BOAM 2025) – pruebas cronometradas medibles.
-- Ejecutar: migration_v3_oposiciones.sql y después este archivo.
USE mydb;

`;

// --- Oposiciones ---
sql += `UPDATE oposiciones SET nombre = 'Policía Nacional - Escala Básica', incluida_gratis = 1 WHERE id_oposicion = 1;
UPDATE oposiciones SET nombre = 'Guardia Civil - Acceso Libre', incluida_gratis = 1 WHERE id_oposicion = 2;

UPDATE oposiciones SET
  nombre = 'Bomberos - Comunidad de Madrid (Conductor)',
  incluida_gratis = 1,
  convocatoria_ref = 'Orden 122/2022 BOCM (Bombero Especialista Conductor). Pruebas distintas al Ayuntamiento de Madrid.',
  notas_usuario = 'Baremos oficiales BOCM. Cada CCAA tiene su propio proceso.'
WHERE id_oposicion = 3;

UPDATE oposiciones SET
  nombre = 'Bomberos - Ayuntamiento de Madrid (Especialista)',
  incluida_gratis = 1,
  convocatoria_ref = 'Bases específicas BOAM 2025 (Decreto 08/10/2025). Incluye 12 pruebas; aquí las cronometradas medibles.',
  notas_usuario = 'Las pruebas de aptitud (espacios confinados, altura, habilidades con EPI) son apto/no apto y no están en el simulacro numérico.'
WHERE id_oposicion = 4;

UPDATE oposiciones SET
  nombre = 'Ejército de Tierra - Tropa y Marinería',
  incluida_gratis = 1,
  convocatoria_ref = 'Convocatoria ingreso 2026 – BOE-A-2026-436, Apéndice 4.',
  notas_usuario = 'Calificación oficial: Apto / No Apto (no nota 0-10). Baremos de la app son orientativos para entrenar.'
WHERE id_oposicion = 6;

UPDATE oposiciones SET
  nombre = 'Ayudante de Instituciones Penitenciarias',
  incluida_gratis = 1,
  convocatoria_ref = 'Sin prueba física eliminatoria deportiva en la oposición (reconocimiento médico).',
  notas_usuario = 'No hay simulacro de pruebas físicas. El acceso se resuelve por temario y aptitud médica.'
WHERE id_oposicion = 5;

`;

const pruebas = [
  // BOMBEROS COMUNIDAD MADRID id 3 – pruebas 8-13
  {
    id: 8,
    opo: 3,
    nombre: 'Natación 50 metros (crol)',
    desc: 'Estilo libre, 50 m en piscina. Marca mínima eliminatoria 51"40. Baremo oficial BOCM Orden 122/2022.',
    trucos: 'Trabaja técnica de crol y salidas desde el agua. Series de 25 y 50 m con descansos cortos.',
    menor: 1,
    unidad: 's',
    ref: 'BOCM Orden 122/2022, Anexo II',
    legal: 'https://www.bocm.es/boletin/CM_Orden_BOCM/2022/02/11/BOCM-20220211-6.PDF',
    tipo: 'PUNTUACION'
  },
  {
    id: 9,
    opo: 3,
    nombre: 'Trepa de cuerda lisa 6 metros',
    desc: 'Subir a brazo una cuerda de 6 m. Marca máxima eliminatoria 14"65.',
    trucos: 'Progresiones con banda y negativas. Trabajo de agarre.',
    menor: 1,
    ref: 'BOCM Orden 122/2022',
    legal: 'https://www.bocm.es/boletin/CM_Orden_BOCM/2022/02/11/BOCM-20220211-6.PDF',
    tipo: 'PUNTUACION'
  },
  {
    id: 10,
    opo: 3,
    nombre: 'Press banca con 40 kg (repeticiones en 60 s)',
    desc: 'Levantamiento de 40 kg en press banca. Mínimo 19 repeticiones en 60 segundos (hombres).',
    trucos: 'Fuerza de empuje y resistencia muscular. Series 8-12 reps al 75% 1RM.',
    menor: 0,
    ref: 'BOCM Orden 122/2022',
    legal: 'https://www.bocm.es/boletin/CM_Orden_BOCM/2022/02/11/BOCM-20220211-6.PDF',
    tipo: 'PUNTUACION'
  },
  {
    id: 11,
    opo: 3,
    nombre: 'Carrera 60 metros',
    desc: 'Sprint en pista. Marca máxima eliminatoria 10"00.',
    trucos: 'Técnica de salida y series de 30-60 m.',
    menor: 1,
    ref: 'BOCM Orden 122/2022',
    legal: 'https://www.bocm.es/boletin/CM_Orden_BOCM/2022/02/11/BOCM-20220211-6.PDF',
    tipo: 'PUNTUACION'
  },
  {
    id: 12,
    opo: 3,
    nombre: 'Carrera 300 metros',
    desc: 'Una vuelta de 300 m en pista. Marca máxima eliminatoria 50"00.',
    trucos: 'Series a ritmo de competición + rodajes.',
    menor: 1,
    ref: 'BOCM Orden 122/2022',
    legal: 'https://www.bocm.es/boletin/CM_Orden_BOCM/2022/02/11/BOCM-20220211-6.PDF',
    tipo: 'PUNTUACION'
  },
  {
    id: 13,
    opo: 3,
    nombre: 'Carrera 2.000 metros',
    desc: 'Prueba de resistencia aeróbica. Marca máxima eliminatoria 7\'40"00 (460 s).',
    trucos: 'Rodajes + series de 400-1000 m a ritmo objetivo.',
    menor: 1,
    ref: 'BOCM Orden 122/2022',
    legal: 'https://www.bocm.es/boletin/CM_Orden_BOCM/2022/02/11/BOCM-20220211-6.PDF',
    tipo: 'PUNTUACION'
  },
  // BOMBEROS AYTO MADRID id 4 – pruebas 14-18 (cronometradas del bloque físico; ver bases BOAM 2025)
  {
    id: 14,
    opo: 4,
    nombre: 'Carrera 200 metros lisos',
    desc: 'Sprint 200 m. Prueba del 2º ejercicio de oposición (bloque físico). Consultar baremo en bases BOAM 2025.',
    trucos: 'Aceleraciones y técnica de carrera.',
    menor: 1,
    ref: 'Bases Bombero Especialista Ayto. Madrid 2025',
    legal: 'https://www.madrid.es/portales/munimadrid/es/Inicio/Educacion-y-empleo/Empleo/Oposiciones/Oposita/Bombero-a-Especialista-del-Cuerpo-de-Bomberos-del-Ayuntamiento-de-Madrid-2025/',
    tipo: 'PUNTUACION'
  },
  {
    id: 15,
    opo: 4,
    nombre: 'Carrera 1.500 metros lisos',
    desc: 'Resistencia en pista. Incluida en el bloque de condición física oficial.',
    trucos: 'Control de ritmo en los primeros 600 m.',
    menor: 1,
    ref: 'Bases BOAM 2025',
    legal: 'https://www.madrid.es/portales/munimadrid/es/Inicio/Educacion-y-empleo/Empleo/Oposiciones/Oposita/Bombero-a-Especialista-del-Cuerpo-de-Bomberos-del-Ayuntamiento-de-Madrid-2025/',
    tipo: 'PUNTUACION'
  },
  {
    id: 16,
    opo: 4,
    nombre: 'Prueba acuática 100 m (25 m buceo + 75 m crol)',
    desc: '25 metros en buceo seguidos de 75 metros a crol. Tiempo total cronometrado.',
    trucos: 'Trabaja transición buceo-crol y respiración bilateral.',
    menor: 1,
    ref: 'Bases BOAM 2025',
    legal: 'https://www.madrid.es/portales/munimadrid/es/Inicio/Educacion-y-empleo/Empleo/Oposiciones/Oposita/Bombero-a-Especialista-del-Cuerpo-de-Bomberos-del-Ayuntamiento-de-Madrid-2025/',
    tipo: 'PUNTUACION'
  },
  {
    id: 17,
    opo: 4,
    nombre: 'Trepa de cuerda lisa 6,5 metros',
    desc: 'Ascenso a brazo en cuerda de 6,5 m (Ayuntamiento de Madrid).',
    trucos: 'Técnica de bloqueo con pies y economía de movimiento.',
    menor: 1,
    ref: 'Bases BOAM 2025',
    legal: 'https://www.madrid.es/portales/munimadrid/es/Inicio/Educacion-y-empleo/Empleo/Oposiciones/Oposita/Bombero-a-Especialista-del-Cuerpo-de-Bomberos-del-Ayuntamiento-de-Madrid-2025/',
    tipo: 'PUNTUACION'
  },
  {
    id: 18,
    opo: 4,
    nombre: 'Carrera vertical en torre (8 plantas)',
    desc: 'Ascenso con equipo de protección en estructura tipo torre de 8 plantas. Tiempo total.',
    trucos: 'Entrenamiento específico con EPI progresivo.',
    menor: 1,
    ref: 'Bases BOAM 2025',
    legal: 'https://www.madrid.es/portales/munimadrid/es/Inicio/Educacion-y-empleo/Empleo/Oposiciones/Oposita/Bombero-a-Especialista-del-Cuerpo-de-Bomberos-del-Ayuntamiento-de-Madrid-2025/',
    tipo: 'PUNTUACION'
  },
  // EJÉRCITO TROPA Y MARINERÍA id 6 – pruebas 19-22 (BOE 2026)
  {
    id: 19,
    opo: 6,
    nombre: 'Flexo-extensiones de brazos (2 minutos)',
    desc: 'Flexiones con técnica reglamentaria. Mínimo apto: H 9 rep / M 5 rep en 2 min (BOE-A-2026-436).',
    trucos: 'Barbilla a almohadilla de 10 cm, cuerpo alineado, una pausa permitida.',
    menor: 0,
    ref: 'BOE-A-2026-436 Apéndice 4',
    legal: 'https://www.boe.es/boe/dias/2026/01/08/pdfs/BOE-A-2026-436.pdf',
    tipo: 'APTO_NO_APTO'
  },
  {
    id: 20,
    opo: 6,
    nombre: 'Plancha isométrica (abdominales)',
    desc: 'Plancha sobre antebrazos y puntas de pies. Mínimo apto: 40 s (H y M). El BOE la denomina "Abdominales" pero es plancha.',
    trucos: 'Core firme sin hundir lumbar. Progresiones de tiempo.',
    menor: 0,
    ref: 'BOE-A-2026-436 Apéndice 4',
    legal: 'https://www.boe.es/boe/dias/2026/01/08/pdfs/BOE-A-2026-436.pdf',
    tipo: 'APTO_NO_APTO'
  },
  {
    id: 21,
    opo: 6,
    nombre: 'Circuito de agilidad-velocidad',
    desc: 'Circuito reglamentario con conos y pelota. Máximo apto: H 15,4 s / M 17,1 s (mejor de 2 intentos).',
    trucos: 'Memoriza el recorrido. Calentamiento específico en el circuito.',
    menor: 1,
    ref: 'BOE-A-2026-436 Apéndice 4',
    legal: 'https://www.boe.es/boe/dias/2026/01/08/pdfs/BOE-A-2026-436.pdf',
    tipo: 'APTO_NO_APTO'
  },
  {
    id: 22,
    opo: 6,
    nombre: 'Carrera continua 2.000 metros',
    desc: 'Resistencia aeróbica. Máximo apto: H 11\'54" (714 s) / M 12\'58" (778 s).',
    trucos: 'Pacing en los primeros 400 m. Rodajes largos los fines de semana.',
    menor: 1,
    ref: 'BOE-A-2026-436 Apéndice 4',
    legal: 'https://www.boe.es/boe/dias/2026/01/08/pdfs/BOE-A-2026-436.pdf',
    tipo: 'APTO_NO_APTO'
  }
];

// unidad por defecto según tipo de prueba
for (const p of pruebas) {
  if (p.unidad == null) {
    p.unidad = p.menor === 1 ? 's' : 'reps';
  }
  if (p.id === 20) p.unidad = 's'; // plancha: más segundos = mejor
}

sql += `INSERT INTO pruebas_oficiales (id_pruebas_oficiales, nombre_prueba, descripcion, trucos, oposiciones_id_oposicion, mejor_si_es_menor, unidad_entrada, convocatoria_ref, fuente_legal, tipo_baremo) VALUES\n`;
sql += pruebas
  .map(
    (p) =>
      `(${p.id}, ${sqlStr(p.nombre)}, ${sqlStr(p.desc)}, ${sqlStr(p.trucos)}, ${p.opo}, ${p.menor}, '${p.unidad}', ${sqlStr(p.ref)}, ${sqlStr(p.legal)}, '${p.tipo}')`
  )
  .join(',\n');
sql += `\nON DUPLICATE KEY UPDATE nombre_prueba=VALUES(nombre_prueba), descripcion=VALUES(descripcion), unidad_entrada=VALUES(unidad_entrada), convocatoria_ref=VALUES(convocatoria_ref), fuente_legal=VALUES(fuente_legal), tipo_baremo=VALUES(tipo_baremo);\n\n`;

const notas = [10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0];

function insertBaremos(chunks) {
  const rows = chunks.filter(Boolean).join(',\n');
  return `INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES\n${rows};\n\n`;
}

const ayto200 = [35, 32, 30, 28, 26, 24, 22, 20, 18, 16, 14];
const ayto1500 = [360, 345, 330, 315, 300, 285, 270, 255, 240, 225, 210];
const ayto100m = [120, 115, 110, 105, 100, 95, 90, 85, 80, 75, 70];
const aytoCuerda = [25, 23, 21, 19, 17, 15, 13, 11, 9, 7, 5];
const aytoTorreH = [95, 90, 85, 80, 75, 70, 65, 60, 55, 50, 45];
const aytoTorreM = [105, 98, 92, 86, 80, 74, 68, 62, 56, 50, 44];

sql += `-- Baremos Bomberos CM (BOCM Orden 122/2022)\n`;
sql += insertBaremos([
  baremosTiempo(8, 'HOMBRE', bocmNatH, notas),
  baremosTiempo(8, 'MUJER', bocmNatM, notas),
  baremosTiempo(9, 'HOMBRE', bocmCuerdaH, notas),
  baremosTiempo(9, 'MUJER', bocmCuerdaM, notas),
  baremosReps(10, 'HOMBRE', bocmPressH, notas),
  baremosReps(10, 'MUJER', bocmPressM, notas),
  baremosTiempo(11, 'HOMBRE', bocm60H, notas),
  baremosTiempo(11, 'MUJER', bocm60M, notas),
  baremosTiempo(12, 'HOMBRE', bocm300H, notas),
  baremosTiempo(12, 'MUJER', bocm300M, notas),
  baremosTiempo(13, 'HOMBRE', bocm2000H, notas),
  baremosTiempo(13, 'MUJER', bocm2000M, notas)
]);

sql += `-- Baremos orientativos Ayto. Madrid (validar con PDF bases BOAM 2025)\n`;
sql += insertBaremos([
  baremosTiempo(14, 'HOMBRE', ayto200, notas),
  baremosTiempo(14, 'MUJER', ayto200.map((x) => x + 3), notas),
  baremosTiempo(15, 'HOMBRE', ayto1500, notas),
  baremosTiempo(15, 'MUJER', ayto1500.map((x) => x + 45), notas),
  baremosTiempo(16, 'HOMBRE', ayto100m, notas),
  baremosTiempo(16, 'MUJER', ayto100m.map((x) => x + 15), notas),
  baremosTiempo(17, 'HOMBRE', aytoCuerda, notas),
  baremosTiempo(17, 'MUJER', aytoCuerda.map((x) => x + 4), notas),
  baremosTiempo(18, 'HOMBRE', aytoTorreH, notas),
  baremosTiempo(18, 'MUJER', aytoTorreM, notas)
]);

sql += `-- Baremos orientativos Ejército (oficial: apto/no apto BOE-A-2026-436)\n`;
sql += insertBaremos([
  baremosReps(19, 'HOMBRE', etFlexH, notas),
  baremosReps(19, 'MUJER', etFlexM, notas),
  baremosReps(20, 'HOMBRE', etPlanchaH, notas),
  baremosReps(20, 'MUJER', etPlanchaM, notas),
  baremosTiempo(21, 'HOMBRE', etCircuitoH, notas),
  baremosTiempo(21, 'MUJER', etCircuitoM, notas),
  baremosTiempo(22, 'HOMBRE', et2000H, notas),
  baremosTiempo(22, 'MUJER', et2000M, notas)
]);

// Requisitos nivel (objetivos orientativos)
for (const p of pruebas) {
  sql += `INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 0, ${p.id}), ('HOMBRE', 2, 0, ${p.id}), ('HOMBRE', 3, 0, ${p.id}),
('MUJER', 1, 0, ${p.id}), ('MUJER', 2, 0, ${p.id}), ('MUJER', 3, 0, ${p.id});\n`;
}

sql += `
INSERT INTO noticias (titulo, contenido, fecha_publicacion, oposiciones_id_oposicion) VALUES
('Datos oficiales verificados',
 'Bomberos CM según BOCM 122/2022. Bomberos Ayto. Madrid según bases 2025. Ejército según BOE-A-2026-436. Penitenciarias: sin prueba física en oposición.',
 NOW(), 1),
('Bomberos: Comunidad vs Ayuntamiento de Madrid',
 'Son procesos distintos con pruebas diferentes. En la app aparecen separados.',
 NOW(), 3),
('Ejército 2026: plancha y circuito',
 'Las pruebas de tropa ya no son abdominales 2 min + salto; son flexiones, plancha, circuito y 2000 m (BOE).',
 NOW(), 6);
`;

const out = path.join(__dirname, '../../BBDD/seed_oposiciones_oficiales.sql');
fs.writeFileSync(out, sql, 'utf8');
console.log('Generado:', out);
