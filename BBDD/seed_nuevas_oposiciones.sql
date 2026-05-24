-- Generado automáticamente. Ejecutar DESPUÉS de seed.sql base
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


-- Bomberos
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(8, 'HOMBRE', 229, 0),
(8, 'HOMBRE', 224, 1),
(8, 'HOMBRE', 219, 2),
(8, 'HOMBRE', 214, 3),
(8, 'HOMBRE', 209, 4),
(8, 'HOMBRE', 204, 5),
(8, 'HOMBRE', 199, 6),
(8, 'HOMBRE', 194, 7),
(8, 'HOMBRE', 189, 8),
(8, 'HOMBRE', 184, 9),
(8, 'HOMBRE', 174, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(8, 'MUJER', 286, 0),
(8, 'MUJER', 278, 1),
(8, 'MUJER', 270, 2),
(8, 'MUJER', 262, 3),
(8, 'MUJER', 254, 4),
(8, 'MUJER', 249, 5),
(8, 'MUJER', 241, 6),
(8, 'MUJER', 233, 7),
(8, 'MUJER', 225, 8),
(8, 'MUJER', 216, 9),
(8, 'MUJER', 204, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(9, 'HOMBRE', 35, 0),
(9, 'HOMBRE', 38, 1),
(9, 'HOMBRE', 40, 2),
(9, 'HOMBRE', 42, 3),
(9, 'HOMBRE', 44, 4),
(9, 'HOMBRE', 46, 5),
(9, 'HOMBRE', 48, 6),
(9, 'HOMBRE', 50, 7),
(9, 'HOMBRE', 52, 8),
(9, 'HOMBRE', 54, 9),
(9, 'HOMBRE', 58, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(9, 'MUJER', 25, 0),
(9, 'MUJER', 28, 1),
(9, 'MUJER', 30, 2),
(9, 'MUJER', 32, 3),
(9, 'MUJER', 34, 4),
(9, 'MUJER', 36, 5),
(9, 'MUJER', 38, 6),
(9, 'MUJER', 40, 7),
(9, 'MUJER', 42, 8),
(9, 'MUJER', 44, 9),
(9, 'MUJER', 48, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(10, 'HOMBRE', 4, 0),
(10, 'HOMBRE', 5, 1),
(10, 'HOMBRE', 6, 2),
(10, 'HOMBRE', 7, 3),
(10, 'HOMBRE', 9, 4),
(10, 'HOMBRE', 10, 5),
(10, 'HOMBRE', 12, 6),
(10, 'HOMBRE', 13, 7),
(10, 'HOMBRE', 14, 8),
(10, 'HOMBRE', 16, 9),
(10, 'HOMBRE', 17, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(10, 'MUJER', 35, 0),
(10, 'MUJER', 40, 1),
(10, 'MUJER', 44, 2),
(10, 'MUJER', 48, 3),
(10, 'MUJER', 53, 4),
(10, 'MUJER', 57, 5),
(10, 'MUJER', 65, 6),
(10, 'MUJER', 72, 7),
(10, 'MUJER', 80, 8),
(10, 'MUJER', 88, 9),
(10, 'MUJER', 95, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(11, 'HOMBRE', 90, 0),
(11, 'HOMBRE', 85, 1),
(11, 'HOMBRE', 80, 2),
(11, 'HOMBRE', 75, 3),
(11, 'HOMBRE', 70, 4),
(11, 'HOMBRE', 65, 5),
(11, 'HOMBRE', 60, 6),
(11, 'HOMBRE', 55, 7),
(11, 'HOMBRE', 50, 8),
(11, 'HOMBRE', 45, 9),
(11, 'HOMBRE', 40, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(11, 'MUJER', 100, 0),
(11, 'MUJER', 95, 1),
(11, 'MUJER', 90, 2),
(11, 'MUJER', 85, 3),
(11, 'MUJER', 80, 4),
(11, 'MUJER', 75, 5),
(11, 'MUJER', 70, 6),
(11, 'MUJER', 65, 7),
(11, 'MUJER', 60, 8),
(11, 'MUJER', 55, 9),
(11, 'MUJER', 50, 10);

-- Policía Local
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(12, 'HOMBRE', 13, 0),
(12, 'HOMBRE', 12.6, 1),
(12, 'HOMBRE', 12.2, 2),
(12, 'HOMBRE', 11.9, 3),
(12, 'HOMBRE', 11.6, 4),
(12, 'HOMBRE', 11.4, 5),
(12, 'HOMBRE', 11, 6),
(12, 'HOMBRE', 10.6, 7),
(12, 'HOMBRE', 10.2, 8),
(12, 'HOMBRE', 9.8, 9),
(12, 'HOMBRE', 9.4, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(12, 'MUJER', 14, 0),
(12, 'MUJER', 13.6, 1),
(12, 'MUJER', 13.3, 2),
(12, 'MUJER', 13, 3),
(12, 'MUJER', 12.7, 4),
(12, 'MUJER', 12.5, 5),
(12, 'MUJER', 12.1, 6),
(12, 'MUJER', 11.7, 7),
(12, 'MUJER', 11.3, 8),
(12, 'MUJER', 10.9, 9),
(12, 'MUJER', 10.5, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(13, 'HOMBRE', 4, 0),
(13, 'HOMBRE', 5, 1),
(13, 'HOMBRE', 6, 2),
(13, 'HOMBRE', 7, 3),
(13, 'HOMBRE', 9, 4),
(13, 'HOMBRE', 10, 5),
(13, 'HOMBRE', 12, 6),
(13, 'HOMBRE', 13, 7),
(13, 'HOMBRE', 14, 8),
(13, 'HOMBRE', 16, 9),
(13, 'HOMBRE', 17, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(13, 'MUJER', 35, 0),
(13, 'MUJER', 40, 1),
(13, 'MUJER', 44, 2),
(13, 'MUJER', 48, 3),
(13, 'MUJER', 53, 4),
(13, 'MUJER', 57, 5),
(13, 'MUJER', 65, 6),
(13, 'MUJER', 72, 7),
(13, 'MUJER', 80, 8),
(13, 'MUJER', 88, 9),
(13, 'MUJER', 95, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(14, 'HOMBRE', 229, 0),
(14, 'HOMBRE', 224, 1),
(14, 'HOMBRE', 219, 2),
(14, 'HOMBRE', 214, 3),
(14, 'HOMBRE', 209, 4),
(14, 'HOMBRE', 204, 5),
(14, 'HOMBRE', 199, 6),
(14, 'HOMBRE', 194, 7),
(14, 'HOMBRE', 189, 8),
(14, 'HOMBRE', 184, 9),
(14, 'HOMBRE', 174, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(14, 'MUJER', 286, 0),
(14, 'MUJER', 278, 1),
(14, 'MUJER', 270, 2),
(14, 'MUJER', 262, 3),
(14, 'MUJER', 254, 4),
(14, 'MUJER', 249, 5),
(14, 'MUJER', 241, 6),
(14, 'MUJER', 233, 7),
(14, 'MUJER', 225, 8),
(14, 'MUJER', 216, 9),
(14, 'MUJER', 204, 10);

-- Penitenciarias
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(15, 'HOMBRE', 420, 0),
(15, 'HOMBRE', 410, 1),
(15, 'HOMBRE', 400, 2),
(15, 'HOMBRE', 390, 3),
(15, 'HOMBRE', 380, 4),
(15, 'HOMBRE', 370, 5),
(15, 'HOMBRE', 360, 6),
(15, 'HOMBRE', 350, 7),
(15, 'HOMBRE', 340, 8),
(15, 'HOMBRE', 330, 9),
(15, 'HOMBRE', 310, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(15, 'MUJER', 500, 0),
(15, 'MUJER', 485, 1),
(15, 'MUJER', 470, 2),
(15, 'MUJER', 455, 3),
(15, 'MUJER', 440, 4),
(15, 'MUJER', 425, 5),
(15, 'MUJER', 410, 6),
(15, 'MUJER', 395, 7),
(15, 'MUJER', 380, 8),
(15, 'MUJER', 365, 9),
(15, 'MUJER', 340, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(16, 'HOMBRE', 10, 0),
(16, 'HOMBRE', 12, 1),
(16, 'HOMBRE', 13, 2),
(16, 'HOMBRE', 15, 3),
(16, 'HOMBRE', 16, 4),
(16, 'HOMBRE', 18, 5),
(16, 'HOMBRE', 22, 6),
(16, 'HOMBRE', 26, 7),
(16, 'HOMBRE', 30, 8),
(16, 'HOMBRE', 35, 9),
(16, 'HOMBRE', 40, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(16, 'MUJER', 7, 0),
(16, 'MUJER', 8, 1),
(16, 'MUJER', 9, 2),
(16, 'MUJER', 11, 3),
(16, 'MUJER', 12, 4),
(16, 'MUJER', 14, 5),
(16, 'MUJER', 17, 6),
(16, 'MUJER', 20, 7),
(16, 'MUJER', 23, 8),
(16, 'MUJER', 27, 9),
(16, 'MUJER', 30, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(17, 'HOMBRE', 20, 0),
(17, 'HOMBRE', 25, 1),
(17, 'HOMBRE', 30, 2),
(17, 'HOMBRE', 35, 3),
(17, 'HOMBRE', 40, 4),
(17, 'HOMBRE', 45, 5),
(17, 'HOMBRE', 50, 6),
(17, 'HOMBRE', 55, 7),
(17, 'HOMBRE', 60, 8),
(17, 'HOMBRE', 65, 9),
(17, 'HOMBRE', 70, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(17, 'MUJER', 15, 0),
(17, 'MUJER', 20, 1),
(17, 'MUJER', 25, 2),
(17, 'MUJER', 30, 3),
(17, 'MUJER', 35, 4),
(17, 'MUJER', 40, 5),
(17, 'MUJER', 45, 6),
(17, 'MUJER', 50, 7),
(17, 'MUJER', 55, 8),
(17, 'MUJER', 60, 9),
(17, 'MUJER', 65, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(18, 'HOMBRE', 35, 0),
(18, 'HOMBRE', 38, 1),
(18, 'HOMBRE', 40, 2),
(18, 'HOMBRE', 42, 3),
(18, 'HOMBRE', 44, 4),
(18, 'HOMBRE', 46, 5),
(18, 'HOMBRE', 48, 6),
(18, 'HOMBRE', 50, 7),
(18, 'HOMBRE', 52, 8),
(18, 'HOMBRE', 54, 9),
(18, 'HOMBRE', 58, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(18, 'MUJER', 25, 0),
(18, 'MUJER', 28, 1),
(18, 'MUJER', 30, 2),
(18, 'MUJER', 32, 3),
(18, 'MUJER', 34, 4),
(18, 'MUJER', 36, 5),
(18, 'MUJER', 38, 6),
(18, 'MUJER', 40, 7),
(18, 'MUJER', 42, 8),
(18, 'MUJER', 44, 9),
(18, 'MUJER', 48, 10);

-- Ejército
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(19, 'HOMBRE', 625, 0),
(19, 'HOMBRE', 605, 1),
(19, 'HOMBRE', 585, 2),
(19, 'HOMBRE', 575, 3),
(19, 'HOMBRE', 570, 4),
(19, 'HOMBRE', 565, 5),
(19, 'HOMBRE', 540, 6),
(19, 'HOMBRE', 510, 7),
(19, 'HOMBRE', 480, 8),
(19, 'HOMBRE', 450, 9),
(19, 'HOMBRE', 420, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(19, 'MUJER', 734, 0),
(19, 'MUJER', 720, 1),
(19, 'MUJER', 706, 2),
(19, 'MUJER', 692, 3),
(19, 'MUJER', 680, 4),
(19, 'MUJER', 674, 5),
(19, 'MUJER', 650, 6),
(19, 'MUJER', 620, 7),
(19, 'MUJER', 590, 8),
(19, 'MUJER', 560, 9),
(19, 'MUJER', 530, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(20, 'HOMBRE', 15, 0),
(20, 'HOMBRE', 17, 1),
(20, 'HOMBRE', 18, 2),
(20, 'HOMBRE', 20, 3),
(20, 'HOMBRE', 21, 4),
(20, 'HOMBRE', 23, 5),
(20, 'HOMBRE', 27, 6),
(20, 'HOMBRE', 31, 7),
(20, 'HOMBRE', 35, 8),
(20, 'HOMBRE', 40, 9),
(20, 'HOMBRE', 45, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(20, 'MUJER', 10, 0),
(20, 'MUJER', 11, 1),
(20, 'MUJER', 12, 2),
(20, 'MUJER', 14, 3),
(20, 'MUJER', 15, 4),
(20, 'MUJER', 17, 5),
(20, 'MUJER', 20, 6),
(20, 'MUJER', 23, 7),
(20, 'MUJER', 26, 8),
(20, 'MUJER', 30, 9),
(20, 'MUJER', 33, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(21, 'HOMBRE', 30, 0),
(21, 'HOMBRE', 35, 1),
(21, 'HOMBRE', 40, 2),
(21, 'HOMBRE', 45, 3),
(21, 'HOMBRE', 50, 4),
(21, 'HOMBRE', 55, 5),
(21, 'HOMBRE', 60, 6),
(21, 'HOMBRE', 65, 7),
(21, 'HOMBRE', 70, 8),
(21, 'HOMBRE', 75, 9),
(21, 'HOMBRE', 80, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(21, 'MUJER', 23, 0),
(21, 'MUJER', 28, 1),
(21, 'MUJER', 33, 2),
(21, 'MUJER', 38, 3),
(21, 'MUJER', 43, 4),
(21, 'MUJER', 48, 5),
(21, 'MUJER', 53, 6),
(21, 'MUJER', 58, 7),
(21, 'MUJER', 63, 8),
(21, 'MUJER', 68, 9),
(21, 'MUJER', 73, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(22, 'HOMBRE', 35, 0),
(22, 'HOMBRE', 38, 1),
(22, 'HOMBRE', 40, 2),
(22, 'HOMBRE', 42, 3),
(22, 'HOMBRE', 44, 4),
(22, 'HOMBRE', 46, 5),
(22, 'HOMBRE', 48, 6),
(22, 'HOMBRE', 50, 7),
(22, 'HOMBRE', 52, 8),
(22, 'HOMBRE', 54, 9),
(22, 'HOMBRE', 58, 10);
INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota) VALUES
(22, 'MUJER', 25, 0),
(22, 'MUJER', 28, 1),
(22, 'MUJER', 30, 2),
(22, 'MUJER', 32, 3),
(22, 'MUJER', 34, 4),
(22, 'MUJER', 36, 5),
(22, 'MUJER', 38, 6),
(22, 'MUJER', 40, 7),
(22, 'MUJER', 42, 8),
(22, 'MUJER', 44, 9),
(22, 'MUJER', 48, 10);

DELETE FROM requisitos_nivel WHERE pruebas_oficiales_id_pruebas_oficiales BETWEEN 8 AND 22;
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 219, 8), ('HOMBRE', 2, 204, 8), ('HOMBRE', 3, 174, 8),
('MUJER', 1, 270, 8), ('MUJER', 2, 249, 8), ('MUJER', 3, 204, 8);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 40, 9), ('HOMBRE', 2, 46, 9), ('HOMBRE', 3, 54, 9),
('MUJER', 1, 30, 9), ('MUJER', 2, 36, 9), ('MUJER', 3, 44, 9);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 6, 10), ('HOMBRE', 2, 10, 10), ('HOMBRE', 3, 16, 10),
('MUJER', 1, 44, 10), ('MUJER', 2, 57, 10), ('MUJER', 3, 88, 10);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 80, 11), ('HOMBRE', 2, 65, 11), ('HOMBRE', 3, 40, 11),
('MUJER', 1, 90, 11), ('MUJER', 2, 75, 11), ('MUJER', 3, 50, 11);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 12.2, 12), ('HOMBRE', 2, 11.4, 12), ('HOMBRE', 3, 9.8, 12),
('MUJER', 1, 13.3, 12), ('MUJER', 2, 12.5, 12), ('MUJER', 3, 10.9, 12);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 6, 13), ('HOMBRE', 2, 10, 13), ('HOMBRE', 3, 16, 13),
('MUJER', 1, 44, 13), ('MUJER', 2, 57, 13), ('MUJER', 3, 88, 13);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 219, 14), ('HOMBRE', 2, 204, 14), ('HOMBRE', 3, 174, 14),
('MUJER', 1, 270, 14), ('MUJER', 2, 249, 14), ('MUJER', 3, 204, 14);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 400, 15), ('HOMBRE', 2, 370, 15), ('HOMBRE', 3, 310, 15),
('MUJER', 1, 470, 15), ('MUJER', 2, 425, 15), ('MUJER', 3, 340, 15);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 13, 16), ('HOMBRE', 2, 18, 16), ('HOMBRE', 3, 35, 16),
('MUJER', 1, 9, 16), ('MUJER', 2, 14, 16), ('MUJER', 3, 27, 16);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 30, 17), ('HOMBRE', 2, 45, 17), ('HOMBRE', 3, 65, 17),
('MUJER', 1, 25, 17), ('MUJER', 2, 40, 17), ('MUJER', 3, 60, 17);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 40, 18), ('HOMBRE', 2, 46, 18), ('HOMBRE', 3, 54, 18),
('MUJER', 1, 30, 18), ('MUJER', 2, 36, 18), ('MUJER', 3, 44, 18);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 585, 19), ('HOMBRE', 2, 565, 19), ('HOMBRE', 3, 420, 19),
('MUJER', 1, 706, 19), ('MUJER', 2, 674, 19), ('MUJER', 3, 530, 19);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 15, 20), ('HOMBRE', 2, 23, 20), ('HOMBRE', 3, 45, 20),
('MUJER', 1, 12, 20), ('MUJER', 2, 20, 20), ('MUJER', 3, 33, 20);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 40, 21), ('HOMBRE', 2, 55, 21), ('HOMBRE', 3, 80, 21),
('MUJER', 1, 33, 21), ('MUJER', 2, 48, 21), ('MUJER', 3, 73, 21);
INSERT INTO requisitos_nivel (genero, nivel_exigencia, valor_objetivo, pruebas_oficiales_id_pruebas_oficiales) VALUES
('HOMBRE', 1, 40, 22), ('HOMBRE', 2, 46, 22), ('HOMBRE', 3, 54, 22),
('MUJER', 1, 30, 22), ('MUJER', 2, 36, 22), ('MUJER', 3, 44, 22);

-- Rutinas oposición 3
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (37, 'BASICO', 'HOMBRE', 'RESISTENCIA', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 37, 1, 1, 0), (67, 37, 1, 4, 120), (62, 37, 1, 3, 90), (63, 37, 1, 1, 0), (11, 37, 40, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (38, 'BASICO', 'HOMBRE', 'FUERZA', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 38, 6, 4, 90), (61, 38, 8, 4, 90), (62, 38, 1, 4, 120), (69, 38, 12, 3, 60), (16, 38, 12, 3, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (39, 'BASICO', 'HOMBRE', 'VELOCIDAD', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 39, 1, 6, 90), (68, 39, 1, 8, 60), (7, 39, 8, 4, 45), (13, 39, 8, 3, 60), (17, 39, 1, 4, 30);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (40, 'BASICO', 'MUJER', 'RESISTENCIA', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 40, 1, 1, 0), (67, 40, 1, 4, 120), (62, 40, 1, 3, 90), (63, 40, 1, 1, 0), (11, 40, 40, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (41, 'BASICO', 'MUJER', 'FUERZA', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 41, 6, 4, 90), (61, 41, 8, 4, 90), (62, 41, 1, 4, 120), (69, 41, 12, 3, 60), (16, 41, 12, 3, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (42, 'BASICO', 'MUJER', 'VELOCIDAD', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 42, 1, 6, 90), (68, 42, 1, 8, 60), (7, 42, 8, 4, 45), (13, 42, 8, 3, 60), (17, 42, 1, 4, 30);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (43, 'INTERMEDIO', 'HOMBRE', 'RESISTENCIA', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 43, 1, 1, 0), (67, 43, 1, 4, 120), (62, 43, 1, 3, 90), (63, 43, 1, 1, 0), (11, 43, 40, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (44, 'INTERMEDIO', 'HOMBRE', 'FUERZA', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 44, 6, 4, 90), (61, 44, 8, 4, 90), (62, 44, 1, 4, 120), (69, 44, 12, 3, 60), (16, 44, 12, 3, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (45, 'INTERMEDIO', 'HOMBRE', 'VELOCIDAD', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 45, 1, 6, 90), (68, 45, 1, 8, 60), (7, 45, 8, 4, 45), (13, 45, 8, 3, 60), (17, 45, 1, 4, 30);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (46, 'INTERMEDIO', 'MUJER', 'RESISTENCIA', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 46, 1, 1, 0), (67, 46, 1, 4, 120), (62, 46, 1, 3, 90), (63, 46, 1, 1, 0), (11, 46, 40, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (47, 'INTERMEDIO', 'MUJER', 'FUERZA', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 47, 6, 4, 90), (61, 47, 8, 4, 90), (62, 47, 1, 4, 120), (69, 47, 12, 3, 60), (16, 47, 12, 3, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (48, 'INTERMEDIO', 'MUJER', 'VELOCIDAD', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 48, 1, 6, 90), (68, 48, 1, 8, 60), (7, 48, 8, 4, 45), (13, 48, 8, 3, 60), (17, 48, 1, 4, 30);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (49, 'AVANZADO', 'HOMBRE', 'RESISTENCIA', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 49, 1, 1, 0), (67, 49, 1, 4, 120), (62, 49, 1, 3, 90), (63, 49, 1, 1, 0), (11, 49, 40, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (50, 'AVANZADO', 'HOMBRE', 'FUERZA', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 50, 6, 4, 90), (61, 50, 8, 4, 90), (62, 50, 1, 4, 120), (69, 50, 12, 3, 60), (16, 50, 12, 3, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (51, 'AVANZADO', 'HOMBRE', 'VELOCIDAD', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 51, 1, 6, 90), (68, 51, 1, 8, 60), (7, 51, 8, 4, 45), (13, 51, 8, 3, 60), (17, 51, 1, 4, 30);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (52, 'AVANZADO', 'MUJER', 'RESISTENCIA', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 52, 1, 1, 0), (67, 52, 1, 4, 120), (62, 52, 1, 3, 90), (63, 52, 1, 1, 0), (11, 52, 40, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (53, 'AVANZADO', 'MUJER', 'FUERZA', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 53, 6, 4, 90), (61, 53, 8, 4, 90), (62, 53, 1, 4, 120), (69, 53, 12, 3, 60), (16, 53, 12, 3, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (54, 'AVANZADO', 'MUJER', 'VELOCIDAD', 3);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 54, 1, 6, 90), (68, 54, 1, 8, 60), (7, 54, 8, 4, 45), (13, 54, 8, 3, 60), (17, 54, 1, 4, 30);

-- Rutinas oposición 4
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (55, 'BASICO', 'HOMBRE', 'RESISTENCIA', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 55, 1, 1, 0), (4, 55, 1, 4, 120), (18, 55, 1, 1, 0), (7, 55, 4, 3, 60), (11, 55, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (56, 'BASICO', 'HOMBRE', 'FUERZA', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 56, 8, 4, 90), (6, 56, 15, 4, 60), (12, 56, 15, 4, 60), (16, 56, 12, 3, 60), (19, 56, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (57, 'BASICO', 'HOMBRE', 'VELOCIDAD', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 57, 1, 6, 90), (7, 57, 6, 4, 45), (17, 57, 1, 4, 30), (13, 57, 10, 4, 45), (28, 57, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (58, 'BASICO', 'MUJER', 'RESISTENCIA', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 58, 1, 1, 0), (4, 58, 1, 4, 120), (18, 58, 1, 1, 0), (7, 58, 4, 3, 60), (11, 58, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (59, 'BASICO', 'MUJER', 'FUERZA', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 59, 8, 4, 90), (6, 59, 15, 4, 60), (12, 59, 15, 4, 60), (16, 59, 12, 3, 60), (19, 59, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (60, 'BASICO', 'MUJER', 'VELOCIDAD', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 60, 1, 6, 90), (7, 60, 6, 4, 45), (17, 60, 1, 4, 30), (13, 60, 10, 4, 45), (28, 60, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (61, 'INTERMEDIO', 'HOMBRE', 'RESISTENCIA', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 61, 1, 1, 0), (4, 61, 1, 4, 120), (18, 61, 1, 1, 0), (7, 61, 4, 3, 60), (11, 61, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (62, 'INTERMEDIO', 'HOMBRE', 'FUERZA', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 62, 8, 4, 90), (6, 62, 15, 4, 60), (12, 62, 15, 4, 60), (16, 62, 12, 3, 60), (19, 62, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (63, 'INTERMEDIO', 'HOMBRE', 'VELOCIDAD', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 63, 1, 6, 90), (7, 63, 6, 4, 45), (17, 63, 1, 4, 30), (13, 63, 10, 4, 45), (28, 63, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (64, 'INTERMEDIO', 'MUJER', 'RESISTENCIA', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 64, 1, 1, 0), (4, 64, 1, 4, 120), (18, 64, 1, 1, 0), (7, 64, 4, 3, 60), (11, 64, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (65, 'INTERMEDIO', 'MUJER', 'FUERZA', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 65, 8, 4, 90), (6, 65, 15, 4, 60), (12, 65, 15, 4, 60), (16, 65, 12, 3, 60), (19, 65, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (66, 'INTERMEDIO', 'MUJER', 'VELOCIDAD', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 66, 1, 6, 90), (7, 66, 6, 4, 45), (17, 66, 1, 4, 30), (13, 66, 10, 4, 45), (28, 66, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (67, 'AVANZADO', 'HOMBRE', 'RESISTENCIA', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 67, 1, 1, 0), (4, 67, 1, 4, 120), (18, 67, 1, 1, 0), (7, 67, 4, 3, 60), (11, 67, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (68, 'AVANZADO', 'HOMBRE', 'FUERZA', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 68, 8, 4, 90), (6, 68, 15, 4, 60), (12, 68, 15, 4, 60), (16, 68, 12, 3, 60), (19, 68, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (69, 'AVANZADO', 'HOMBRE', 'VELOCIDAD', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 69, 1, 6, 90), (7, 69, 6, 4, 45), (17, 69, 1, 4, 30), (13, 69, 10, 4, 45), (28, 69, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (70, 'AVANZADO', 'MUJER', 'RESISTENCIA', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 70, 1, 1, 0), (4, 70, 1, 4, 120), (18, 70, 1, 1, 0), (7, 70, 4, 3, 60), (11, 70, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (71, 'AVANZADO', 'MUJER', 'FUERZA', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 71, 8, 4, 90), (6, 71, 15, 4, 60), (12, 71, 15, 4, 60), (16, 71, 12, 3, 60), (19, 71, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (72, 'AVANZADO', 'MUJER', 'VELOCIDAD', 4);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 72, 1, 6, 90), (7, 72, 6, 4, 45), (17, 72, 1, 4, 30), (13, 72, 10, 4, 45), (28, 72, 1, 6, 60);

-- Rutinas oposición 5
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (73, 'BASICO', 'HOMBRE', 'RESISTENCIA', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 73, 1, 1, 0), (4, 73, 1, 4, 120), (18, 73, 1, 1, 0), (7, 73, 4, 3, 60), (11, 73, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (74, 'BASICO', 'HOMBRE', 'FUERZA', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 74, 8, 4, 90), (6, 74, 15, 4, 60), (12, 74, 15, 4, 60), (16, 74, 12, 3, 60), (19, 74, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (75, 'BASICO', 'HOMBRE', 'VELOCIDAD', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 75, 1, 6, 90), (7, 75, 6, 4, 45), (17, 75, 1, 4, 30), (13, 75, 10, 4, 45), (28, 75, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (76, 'BASICO', 'MUJER', 'RESISTENCIA', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 76, 1, 1, 0), (4, 76, 1, 4, 120), (18, 76, 1, 1, 0), (7, 76, 4, 3, 60), (11, 76, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (77, 'BASICO', 'MUJER', 'FUERZA', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 77, 8, 4, 90), (6, 77, 15, 4, 60), (12, 77, 15, 4, 60), (16, 77, 12, 3, 60), (19, 77, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (78, 'BASICO', 'MUJER', 'VELOCIDAD', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 78, 1, 6, 90), (7, 78, 6, 4, 45), (17, 78, 1, 4, 30), (13, 78, 10, 4, 45), (28, 78, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (79, 'INTERMEDIO', 'HOMBRE', 'RESISTENCIA', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 79, 1, 1, 0), (4, 79, 1, 4, 120), (18, 79, 1, 1, 0), (7, 79, 4, 3, 60), (11, 79, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (80, 'INTERMEDIO', 'HOMBRE', 'FUERZA', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 80, 8, 4, 90), (6, 80, 15, 4, 60), (12, 80, 15, 4, 60), (16, 80, 12, 3, 60), (19, 80, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (81, 'INTERMEDIO', 'HOMBRE', 'VELOCIDAD', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 81, 1, 6, 90), (7, 81, 6, 4, 45), (17, 81, 1, 4, 30), (13, 81, 10, 4, 45), (28, 81, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (82, 'INTERMEDIO', 'MUJER', 'RESISTENCIA', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 82, 1, 1, 0), (4, 82, 1, 4, 120), (18, 82, 1, 1, 0), (7, 82, 4, 3, 60), (11, 82, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (83, 'INTERMEDIO', 'MUJER', 'FUERZA', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 83, 8, 4, 90), (6, 83, 15, 4, 60), (12, 83, 15, 4, 60), (16, 83, 12, 3, 60), (19, 83, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (84, 'INTERMEDIO', 'MUJER', 'VELOCIDAD', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 84, 1, 6, 90), (7, 84, 6, 4, 45), (17, 84, 1, 4, 30), (13, 84, 10, 4, 45), (28, 84, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (85, 'AVANZADO', 'HOMBRE', 'RESISTENCIA', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 85, 1, 1, 0), (4, 85, 1, 4, 120), (18, 85, 1, 1, 0), (7, 85, 4, 3, 60), (11, 85, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (86, 'AVANZADO', 'HOMBRE', 'FUERZA', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 86, 8, 4, 90), (6, 86, 15, 4, 60), (12, 86, 15, 4, 60), (16, 86, 12, 3, 60), (19, 86, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (87, 'AVANZADO', 'HOMBRE', 'VELOCIDAD', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 87, 1, 6, 90), (7, 87, 6, 4, 45), (17, 87, 1, 4, 30), (13, 87, 10, 4, 45), (28, 87, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (88, 'AVANZADO', 'MUJER', 'RESISTENCIA', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 88, 1, 1, 0), (4, 88, 1, 4, 120), (18, 88, 1, 1, 0), (7, 88, 4, 3, 60), (11, 88, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (89, 'AVANZADO', 'MUJER', 'FUERZA', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 89, 8, 4, 90), (6, 89, 15, 4, 60), (12, 89, 15, 4, 60), (16, 89, 12, 3, 60), (19, 89, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (90, 'AVANZADO', 'MUJER', 'VELOCIDAD', 5);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 90, 1, 6, 90), (7, 90, 6, 4, 45), (17, 90, 1, 4, 30), (13, 90, 10, 4, 45), (28, 90, 1, 6, 60);

-- Rutinas oposición 6
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (91, 'BASICO', 'HOMBRE', 'RESISTENCIA', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 91, 1, 1, 0), (4, 91, 1, 4, 120), (18, 91, 1, 1, 0), (7, 91, 4, 3, 60), (11, 91, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (92, 'BASICO', 'HOMBRE', 'FUERZA', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 92, 8, 4, 90), (6, 92, 15, 4, 60), (12, 92, 15, 4, 60), (16, 92, 12, 3, 60), (19, 92, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (93, 'BASICO', 'HOMBRE', 'VELOCIDAD', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 93, 1, 6, 90), (7, 93, 6, 4, 45), (17, 93, 1, 4, 30), (13, 93, 10, 4, 45), (28, 93, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (94, 'BASICO', 'MUJER', 'RESISTENCIA', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 94, 1, 1, 0), (4, 94, 1, 4, 120), (18, 94, 1, 1, 0), (7, 94, 4, 3, 60), (11, 94, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (95, 'BASICO', 'MUJER', 'FUERZA', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 95, 8, 4, 90), (6, 95, 15, 4, 60), (12, 95, 15, 4, 60), (16, 95, 12, 3, 60), (19, 95, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (96, 'BASICO', 'MUJER', 'VELOCIDAD', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 96, 1, 6, 90), (7, 96, 6, 4, 45), (17, 96, 1, 4, 30), (13, 96, 10, 4, 45), (28, 96, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (97, 'INTERMEDIO', 'HOMBRE', 'RESISTENCIA', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 97, 1, 1, 0), (4, 97, 1, 4, 120), (18, 97, 1, 1, 0), (7, 97, 4, 3, 60), (11, 97, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (98, 'INTERMEDIO', 'HOMBRE', 'FUERZA', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 98, 8, 4, 90), (6, 98, 15, 4, 60), (12, 98, 15, 4, 60), (16, 98, 12, 3, 60), (19, 98, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (99, 'INTERMEDIO', 'HOMBRE', 'VELOCIDAD', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 99, 1, 6, 90), (7, 99, 6, 4, 45), (17, 99, 1, 4, 30), (13, 99, 10, 4, 45), (28, 99, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (100, 'INTERMEDIO', 'MUJER', 'RESISTENCIA', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 100, 1, 1, 0), (4, 100, 1, 4, 120), (18, 100, 1, 1, 0), (7, 100, 4, 3, 60), (11, 100, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (101, 'INTERMEDIO', 'MUJER', 'FUERZA', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 101, 8, 4, 90), (6, 101, 15, 4, 60), (12, 101, 15, 4, 60), (16, 101, 12, 3, 60), (19, 101, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (102, 'INTERMEDIO', 'MUJER', 'VELOCIDAD', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 102, 1, 6, 90), (7, 102, 6, 4, 45), (17, 102, 1, 4, 30), (13, 102, 10, 4, 45), (28, 102, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (103, 'AVANZADO', 'HOMBRE', 'RESISTENCIA', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 103, 1, 1, 0), (4, 103, 1, 4, 120), (18, 103, 1, 1, 0), (7, 103, 4, 3, 60), (11, 103, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (104, 'AVANZADO', 'HOMBRE', 'FUERZA', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 104, 8, 4, 90), (6, 104, 15, 4, 60), (12, 104, 15, 4, 60), (16, 104, 12, 3, 60), (19, 104, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (105, 'AVANZADO', 'HOMBRE', 'VELOCIDAD', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 105, 1, 6, 90), (7, 105, 6, 4, 45), (17, 105, 1, 4, 30), (13, 105, 10, 4, 45), (28, 105, 1, 6, 60);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (106, 'AVANZADO', 'MUJER', 'RESISTENCIA', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (3, 106, 1, 1, 0), (4, 106, 1, 4, 120), (18, 106, 1, 1, 0), (7, 106, 4, 3, 60), (11, 106, 30, 3, 45);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (107, 'AVANZADO', 'MUJER', 'FUERZA', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (1, 107, 8, 4, 90), (6, 107, 15, 4, 60), (12, 107, 15, 4, 60), (16, 107, 12, 3, 60), (19, 107, 10, 4, 90);
INSERT INTO rutinas_opo (id_rutina_opo, nivel, genero, enfoque_tipo, oposiciones_id_oposicion) VALUES (108, 'AVANZADO', 'MUJER', 'VELOCIDAD', 6);
INSERT INTO detalle_rutina_opo (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso) VALUES (5, 108, 1, 6, 90), (7, 108, 6, 4, 45), (17, 108, 1, 4, 30), (13, 108, 10, 4, 45), (28, 108, 1, 6, 60);

INSERT INTO noticias (titulo, contenido, fecha_publicacion, oposiciones_id_oposicion) VALUES
('OpoFit: nuevas oposiciones con prueba física', 'Ya puedes preparar Bomberos, Policía Local, Penitenciarias y Ejército con pruebas y baremos adaptados.', NOW(), 3),
('Convocatorias Bomberos 2026', 'Revisa el peso oficial del farmer walk y la altura del salto en la convocatoria de tu comunidad autónoma.', NOW(), 3),
('Policía Local: pruebas físicas', 'Circuito, dominadas/suspensión y 1000 m son las pruebas más habituales en ayuntamientos grandes.', NOW(), 4),
('Ejército: preparación pruebas oficiales', 'Trabaja el 2000 m y los test de flexiones y abdominales con cronómetro para simular el día del examen.', NOW(), 6);
