/**
 * DEPRECADO — datos genéricos incorrectos para producción.
 * Usar: node scripts/generar-seed-oposiciones-oficial.js
 *       → BBDD/seed_oposiciones_oficiales.sql (tras migration_v3_oposiciones.sql)
 */
const fs = require('fs');
const path = require('path');

function baremosTiempo(idPrueba, genero, tiemposSeg) {
  return tiemposSeg
    .map((marca, nota) => `(${idPrueba}, '${genero}', ${marca}, ${nota})`)
    .join(',\n');
}

function baremosReps(idPrueba, genero, reps) {
  return reps
    .map((marca, nota) => `(${idPrueba}, '${genero}', ${marca}, ${nota})`)
    .join(',\n');
}

function baremosSalto(idPrueba, genero, cms) {
  return cms
    .map((marca, nota) => `(${idPrueba}, '${genero}', ${marca}, ${nota})`)
    .join(',\n');
}

const t1000H = [229, 224, 219, 214, 209, 204, 199, 194, 189, 184, 174];
const t1000M = [286, 278, 270, 262, 254, 249, 241, 233, 225, 216, 204];
const t2000H = [625, 605, 585, 575, 570, 565, 540, 510, 480, 450, 420];
const t2000M = [734, 720, 706, 692, 680, 674, 650, 620, 590, 560, 530];
const domH = [4, 5, 6, 7, 9, 10, 12, 13, 14, 16, 17];
const domM = [35, 40, 44, 48, 53, 57, 65, 72, 80, 88, 95];
const flexH = [10, 12, 13, 15, 16, 18, 22, 26, 30, 35, 40];
const flexM = [7, 8, 9, 11, 12, 14, 17, 20, 23, 27, 30];
const saltoH = [35, 38, 40, 42, 44, 46, 48, 50, 52, 54, 58];
const saltoM = [25, 28, 30, 32, 34, 36, 38, 40, 42, 44, 48];
const agilH = [13, 12.6, 12.2, 11.9, 11.6, 11.4, 11, 10.6, 10.2, 9.8, 9.4];
const agilM = [14, 13.6, 13.3, 13, 12.7, 12.5, 12.1, 11.7, 11.3, 10.9, 10.5];
const t1600H = [420, 410, 400, 390, 380, 370, 360, 350, 340, 330, 310];
const t1600M = [500, 485, 470, 455, 440, 425, 410, 395, 380, 365, 340];
const farmerH = [90, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40];
const farmerM = [100, 95, 90, 85, 80, 75, 70, 65, 60, 55, 50];
const abdom1H = [20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70];
const abdom1M = [15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65];

let sql = `-- Generado automáticamente. Ejecutar DESPUÉS de seed.sql base
USE mydb;

UPDATE oposiciones SET incluida_gratis = 1;

INSERT INTO oposiciones (id_oposicion, nombre, incluida_gratis) VALUES
(3, 'Bomberos - Escala Básica', 1),
(4, 'Policía Local', 1),
(5, 'Instituciones Penitenciarias (AII)', 1),
(6, 'Ejército de Tierra - Tropa', 1)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), incluida_gratis = 1;

INSERT INTO ejercicios (id_ejercicio, nombre, video_url, instrucciones_tecnicas) VALUES
(61, 'Salto vertical con contramovimiento', 'https://www.youtube.com/watch?v=52GwhOvlpj8', 'Flexión rápida de cadera y rodillas, brazos coordinados. Salto vertical máximo. Aterrizaje suave con rodillas semiflexionadas.'),
(62, 'Farmer walk 40 metros', 'https://www.youtube.com/watch?v=Fkzk_RqlYig', 'Camina 40 m con kettlebells o mancuernas pesadas. Torso erguido, core activo. Ritmo constante sin perder técnica.'),
(63, 'Step mill / escalera 15 min', 'https://www.youtube.com/watch?v=dQqApCGd5Ss', 'Subida continua en step o máquina de escaleras. Simula prueba de bomberos. Mantén cadencia estable.'),
(64, 'Abdominales 1 minuto', 'https://www.youtube.com/watch?v=1919eTCoESo', 'Máximo número de repeticiones válidas en 60 s. Codos a rodillas, lumbar pegada al suelo.'),
(65, 'Abdominales 2 minutos', 'https://www.youtube.com/watch?v=1919eTCoESo', 'Test de resistencia abdominal oficial en muchas convocatorias militares. Ritmo sostenible los primeros 60 s.'),
(66, 'Flexiones 2 minutos', 'https://www.youtube.com/watch?v=IODxDxX7oi4', 'Flexiones estrictas en 120 s. Pecho al suelo, cuerpo alineado. Prioriza técnica para que cuenten todas.'),
(67, 'Carrera 1600 metros', 'https://www.youtube.com/watch?v=5UQbZpJb0lM', '4 vueltas de pista. Ritmo objetivo según baremo. Controla el primer km para no quemarte.'),
(68, 'Sprint 50 metros', 'https://www.youtube.com/watch?v=5UQbZpJb0lM', 'Aceleración máxima en 50 m. Salida en tres tiempos. Recuperación completa entre series.'),
(69, 'Remo con kettlebell', 'https://www.youtube.com/watch?v=YSxHifyI6s8', 'Potencia desde cadera. Refuerza espalda y agarre para pruebas de arrastre y suspensión.'),
(70, 'Prensa de pierna unilateral', 'https://www.youtube.com/watch?v=dQqApCGd5Ss', 'Fuerza específica para subida de escaleras y zancadas con carga.'),
(71, 'Crawl inverso / bear crawl', 'https://www.youtube.com/watch?v=nmwgirgXLYM', 'Desplazamiento en cuadrupedia. Core y hombros para circuitos de agilidad.'),
(72, 'Lanzamiento balón medicinal', 'https://www.youtube.com/watch?v=YSxHifyI6s8', 'Potencia explosiva de tren superior. 3-5 series de 6-8 repeticiones.')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO pruebas_oficiales (id_pruebas_oficiales, nombre_prueba, descripcion, trucos, oposiciones_id_oposicion, mejor_si_es_menor) VALUES
(8, 'Carrera 1.000 metros',
 'Prueba aeróbica en pista. Distancia oficial 1000 m cronometrados. Baremos orientativos según convocatorias de bomberos en España.',
 'Entrena series de 400 m a ritmo de examen. Rodajes de 30-40 min para base.', 3, 1),
(9, 'Salto vertical',
 'Salto vertical con contramovimiento (cm). Se mide la diferencia entre alcance de pie y alcance en salto. Prueba habitual en bomberos.',
 'Trabaja sentadilla explosiva y saltos al cajón. 2-3 series de 5 saltos máximos con descanso completo.', 3, 0),
(10, 'Dominadas / Suspensión en barra',
 'Hombres: dominadas prono, extensión completa. Mujeres: suspensión isométrica con barbilla sobre la barra.',
 'Progresiones con bandas y negativas. Grip work 2 veces por semana.', 3, 0),
(11, 'Desplazamiento con carga (40 m)',
 'Farmer walk o transporte de kettlebells 20 kg (h) / 12 kg (m) en 40 metros. Cronometrado.',
 'Practica con el peso oficial de tu convocatoria. Core y agarre son clave.', 3, 1),

(12, 'Circuito de agilidad',
 'Recorrido con vallas, slalom y cambios de dirección. Dos intentos, mejor tiempo (segundos).',
 'Memoriza el circuito. Escalera de coordinación 3 veces por semana.', 4, 1),
(13, 'Dominadas / Suspensión en barra',
 'Igual criterio que Policía Nacional en la mayoría de ayuntamientos.',
 'Dominadas estrictas 4 días por semana con volumen progresivo.', 4, 0),
(14, 'Carrera 1.000 metros',
 'Carrera en pista 1000 m. Baremos similares a PN en muchas convocatorias locales.',
 'Series 400 m + 200 m. Un rodaje largo semanal.', 4, 1),

(15, 'Carrera 1.600 metros',
 'Resistencia en pista (4x400 m). Prueba frecuente en oposiciones penitenciarias.',
 'Progresión 1200-1600 m a ritmo controlado.', 5, 1),
(16, 'Flexiones en 1 minuto',
 'Máximo de flexiones válidas en 60 segundos. Cuerpo recto, pecho cerca del suelo.',
 'EMOM flexiones + trabajo de tríceps.', 5, 0),
(17, 'Abdominales en 1 minuto',
 'Repeticiones válidas en 60 s. Codos a rodillas.',
 'Haz test cada 2 semanas para medir progreso.', 5, 0),
(18, 'Salto vertical',
 'Salto vertical con contramovimiento en centímetros.',
 'Pliometría 2 veces por semana sin sobrecargar tendones.', 5, 0),

(19, 'Carrera 2.000 metros',
 'Prueba de resistencia militar. Objetivo: completar en el menor tiempo posible según baremo.',
 'Series 800-1000 m. Rodaje 45-60 min los fines de semana.', 6, 1),
(20, 'Flexiones en 2 minutos',
 'Flexiones continuas en 120 s según protocolo de instrucción militar.',
 'Volumen alto con técnica perfecta. Descansa 48 h entre sesiones intensas.', 6, 0),
(21, 'Abdominales en 2 minutos',
 'Repeticiones en 120 s. Ritmo constante, sin perder forma en el minuto final.',
 'Combina series largas y bloques de 30 s.', 6, 0),
(22, 'Salto vertical',
 'Salto vertical (cm) con técnica reglamentaria.',
 'Fuerza máxima en sentadilla + saltos.', 6, 0)
ON DUPLICATE KEY UPDATE nombre_prueba = VALUES(nombre_prueba);

`;

const baremoBlocks = [
  ['-- Bomberos', 8, baremosTiempo(8, 'HOMBRE', t1000H), baremosTiempo(8, 'MUJER', t1000M)],
  [null, 9, baremosSalto(9, 'HOMBRE', saltoH), baremosSalto(9, 'MUJER', saltoM)],
  [null, 10, baremosReps(10, 'HOMBRE', domH), baremosReps(10, 'MUJER', domM)],
  [null, 11, baremosTiempo(11, 'HOMBRE', farmerH), baremosTiempo(11, 'MUJER', farmerM)],
  ['-- Policía Local', 12, baremosTiempo(12, 'HOMBRE', agilH), baremosTiempo(12, 'MUJER', agilM)],
  [null, 13, baremosReps(13, 'HOMBRE', domH), baremosReps(13, 'MUJER', domM)],
  [null, 14, baremosTiempo(14, 'HOMBRE', t1000H), baremosTiempo(14, 'MUJER', t1000M)],
  ['-- Penitenciarias', 15, baremosTiempo(15, 'HOMBRE', t1600H), baremosTiempo(15, 'MUJER', t1600M)],
  [null, 16, baremosReps(16, 'HOMBRE', flexH), baremosReps(16, 'MUJER', flexM)],
  [null, 17, baremosReps(17, 'HOMBRE', abdom1H), baremosReps(17, 'MUJER', abdom1M)],
  [null, 18, baremosSalto(18, 'HOMBRE', saltoH), baremosSalto(18, 'MUJER', saltoM)],
  ['-- Ejército', 19, baremosTiempo(19, 'HOMBRE', t2000H), baremosTiempo(19, 'MUJER', t2000M)],
  [null, 20, baremosReps(20, 'HOMBRE', flexH.map((v) => v + 5)), baremosReps(20, 'MUJER', flexM.map((v) => v + 3))],
  [null, 21, baremosReps(21, 'HOMBRE', abdom1H.map((v) => v + 10)), baremosReps(21, 'MUJER', abdom1M.map((v) => v + 8))],
  [null, 22, baremosSalto(22, 'HOMBRE', saltoH), baremosSalto(22, 'MUJER', saltoM)]
];

for (const [comment, id, h, m] of baremoBlocks) {
  if (comment) sql += `\n${comment}\n`;
  sql += `INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES\n${h};\n`;
  sql += `INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES\n${m};\n`;
}

const reqMap = {
  8: { h: [219, 204, 174], m: [270, 249, 204] },
  9: { h: [40, 46, 54], m: [30, 36, 44] },
  10: { h: [6, 10, 16], m: [44, 57, 88] },
  11: { h: [80, 65, 40], m: [90, 75, 50] },
  12: { h: [12.2, 11.4, 9.8], m: [13.3, 12.5, 10.9] },
  13: { h: [6, 10, 16], m: [44, 57, 88] },
  14: { h: [219, 204, 174], m: [270, 249, 204] },
  15: { h: [400, 370, 310], m: [470, 425, 340] },
  16: { h: [13, 18, 35], m: [9, 14, 27] },
  17: { h: [30, 45, 65], m: [25, 40, 60] },
  18: { h: [40, 46, 54], m: [30, 36, 44] },
  19: { h: [585, 565, 420], m: [706, 674, 530] },
  20: { h: [15, 23, 45], m: [12, 20, 33] },
  21: { h: [40, 55, 80], m: [33, 48, 73] },
  22: { h: [40, 46, 54], m: [30, 36, 44] }
};

sql += `\nDELETE FROM requisitos_nivel WHERE pruebas_oficiales_id_pruebas_oficiales BETWEEN 8 AND 22;\n`;
for (const [id, v] of Object.entries(reqMap)) {
  sql += `INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, ${v.h[0]}, ${id}), ('HOMBRE', 2, ${v.h[1]}, ${id}), ('HOMBRE', 3, ${v.h[2]}, ${id}),
('MUJER', 1, ${v.m[0]}, ${id}), ('MUJER', 2, ${v.m[1]}, ${id}), ('MUJER', 3, ${v.m[2]}, ${id});\n`;
}

// Rutinas BASICO + INTERMEDIO + AVANZADO por oposición (plantilla por enfoque)
function rutinasOpo(opoId, startRutinaId) {
  let id = startRutinaId;
  let out = '';
  const niveles = ['BASICO', 'INTERMEDIO', 'AVANZADO'];
  const generos = ['HOMBRE', 'MUJER'];
  const enfoques = ['RESISTENCIA', 'FUERZA', 'VELOCIDAD'];
  const detalles = {
    RESISTENCIA: '(3, {id}, 1, 1, 0), (4, {id}, 1, 4, 120), (18, {id}, 1, 1, 0), (7, {id}, 4, 3, 60), (11, {id}, 30, 3, 45)',
    FUERZA: '(1, {id}, 8, 4, 90), (6, {id}, 15, 4, 60), (12, {id}, 15, 4, 60), (16, {id}, 12, 3, 60), (19, {id}, 10, 4, 90)',
    VELOCIDAD: '(5, {id}, 1, 6, 90), (7, {id}, 6, 4, 45), (17, {id}, 1, 4, 30), (13, {id}, 10, 4, 45), (28, {id}, 1, 6, 60)'
  };
  const detallesBomberos = {
    RESISTENCIA: '(3, {id}, 1, 1, 0), (67, {id}, 1, 4, 120), (62, {id}, 1, 3, 90), (63, {id}, 1, 1, 0), (11, {id}, 40, 3, 45)',
    FUERZA: '(1, {id}, 6, 4, 90), (61, {id}, 8, 4, 90), (62, {id}, 1, 4, 120), (69, {id}, 12, 3, 60), (16, {id}, 12, 3, 60)',
    VELOCIDAD: '(5, {id}, 1, 6, 90), (68, {id}, 1, 8, 60), (7, {id}, 8, 4, 45), (13, {id}, 8, 3, 60), (17, {id}, 1, 4, 30)'
  };
  const det = opoId === 3 ? detallesBomberos : detalles;
  for (const nivel of niveles) {
    for (const genero of generos) {
      for (const enf of enfoques) {
        out += `INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (${id}, '${nivel}', '${genero}', '${enf}', ${opoId});\n`;
        const reps = nivel === 'BASICO' ? 1 : nivel === 'INTERMEDIO' ? 1.2 : 1.4;
        const d = det[enf].replace(/\{id\}/g, id);
        out += `INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES ${d};\n`;
        id++;
      }
    }
  }
  return { sql: out, nextId: id };
}

let rid = 37;
for (const opo of [3, 4, 5, 6]) {
  sql += `\n-- Rutinas oposición ${opo}\n`;
  const r = rutinasOpo(opo, rid);
  sql += r.sql;
  rid = r.nextId;
}

sql += `
INSERT INTO noticias (titulo, contenido, fecha_publicacion, oposiciones_id_oposicion) VALUES
('OpoFit: nuevas oposiciones con prueba física', 'Ya puedes preparar Bomberos, Policía Local, Penitenciarias y Ejército con pruebas y baremos adaptados.', NOW(), 3),
('Convocatorias Bomberos 2026', 'Revisa el peso oficial del farmer walk y la altura del salto en la convocatoria de tu comunidad autónoma.', NOW(), 3),
('Policía Local: pruebas físicas', 'Circuito, dominadas/suspensión y 1000 m son las pruebas más habituales en ayuntamientos grandes.', NOW(), 4),
('Ejército: preparación pruebas oficiales', 'Trabaja el 2000 m y los test de flexiones y abdominales con cronómetro para simular el día del examen.', NOW(), 6);
`;

const outPath = path.join(__dirname, '../../BBDD/seed_nuevas_oposiciones.sql');
fs.writeFileSync(outPath, sql, 'utf8');
console.log('Escrito:', outPath);
