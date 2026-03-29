-- =====================================================================
-- DATOS SEMILLA (SEED) — OpoFit
-- Datos reales de las oposiciones de Policía Nacional (Escala Básica)
-- y Guardia Civil (Acceso libre) según BOE y convocatorias vigentes.
-- =====================================================================
-- Ejecutar DESPUÉS de haber creado el esquema con mydb.sql

USE `mydb`;

SET FOREIGN_KEY_CHECKS=0;

-- Limpiamos datos existentes para evitar duplicados
DELETE FROM `registro_resultados`;
DELETE FROM `detalle_rutina_opo`;
DELETE FROM `detalle_rutina_pers`;
DELETE FROM `historial_sesiones`;
DELETE FROM `rutinas_pers`;
DELETE FROM `rutinas_opo`;
DELETE FROM `baremos_puntuacion`;
DELETE FROM `requisitos_nivel`;
DELETE FROM `marcas_perfil`;
DELETE FROM `noticias`;
DELETE FROM `settings`;
DELETE FROM `usuarios`;
DELETE FROM `pruebas_oficiales`;
DELETE FROM `ejercicios`;
DELETE FROM `oposiciones`;

SET FOREIGN_KEY_CHECKS=1;

-- =====================================================================
-- 1. OPOSICIONES
-- =====================================================================
INSERT INTO `oposiciones` (`id_oposicion`, `nombre`) VALUES
(1, 'Policía Nacional - Escala Básica'),
(2, 'Guardia Civil - Acceso Libre');

-- =====================================================================
-- 2. PRUEBAS OFICIALES
-- Datos según BOE (convocatorias 2025-2026)
--
-- Policía Nacional: 3 pruebas físicas
--   1) Circuito de agilidad (tiempo en segundos, menor es mejor)
--   2) Dominadas - Hombres / Suspensión en barra - Mujeres
--   3) Carrera 1.000 metros (tiempo en segundos, menor es mejor)
--
-- Guardia Civil: 4 pruebas físicas
--   4) Carrera 2.000 metros (tiempo en segundos, menor es mejor)
--   5) Circuito de agilidad (tiempo en segundos, menor es mejor)
--   6) Flexiones de brazos (repeticiones, mayor es mejor)
--   7) Natación 50 metros (tiempo en segundos, menor es mejor)
-- =====================================================================
INSERT INTO `pruebas_oficiales` (`id_pruebas_oficiales`, `nombre_prueba`, `descripcion`, `trucos`, `oposiciones_id_oposicion`, `mejor_si_es_menor`) VALUES
-- Policía Nacional
(1, 'Circuito de agilidad',
 'Recorrido con obstáculos cronometrado. Incluye saltos, giros, volteretas y cambios de dirección. Se realizan dos intentos y se contabiliza el mejor tiempo.',
 'Practica la voltereta hacia adelante sobre colchoneta. Entrena los cambios de dirección con conos. El circuito se memoriza: repásalo mentalmente antes del intento.',
 1, 1),

(2, 'Dominadas / Suspensión en barra',
 'Hombres: dominadas con agarre prono (palmas hacia fuera), extensión completa de brazos y barbilla por encima de la barra en cada repetición. Mujeres: suspensión en barra con brazos flexionados y barbilla por encima de la barra el mayor tiempo posible.',
 'Hombres: entrena dominadas estrictas sin kipping; fortalece el agarre con ejercicios de grip. Mujeres: practica isométricos en la posición de barbilla arriba; fortalece bíceps y espalda.',
 1, 0),

(3, 'Carrera 1.000 metros',
 'Carrera de resistencia en pista de atletismo. No se permite el uso de clavos. Se cronometra el tiempo empleado en completar la distancia.',
 'Distribuye el esfuerzo: los primeros 400m a ritmo controlado y aprieta en los últimos 200m. Entrena series de 400m y 200m a ritmo superior al de competición.',
 1, 1),

-- Guardia Civil
(4, 'Carrera 2.000 metros',
 'Prueba de resistencia aeróbica. Carrera en pista de atletismo, cronometrada. Se debe completar la distancia dentro del tiempo máximo establecido según sexo y edad.',
 'Entrena con series largas (800m-1200m) y rodajes de 30-40 minutos. Controla el ritmo con un reloj GPS. El entrenamiento de fartlek mejora mucho los tiempos.',
 2, 1),

(5, 'Circuito de agilidad (GC)',
 'Recorrido con obstáculos cronometrado similar al de Policía Nacional. Incluye saltos, pasos bajo vallas, slalom y cambios de dirección. Un intento.',
 'Practica circuitos con obstáculos variados. Trabaja la coordinación con ejercicios de escalera de agilidad. Memoriza el recorrido antes de la prueba.',
 2, 1),

(6, 'Flexiones de brazos',
 'Extensiones de brazos en el suelo (flexiones). El cuerpo debe mantenerse recto, con descenso hasta que el pecho toque el suelo o la barbilla sobrepase el plano de apoyo de las manos. Dos intentos permitidos.',
 'Entrena flexiones estrictas a diario con progresiones. Fortalece el core con planchas. No hagas las repeticiones rápido: mantén la técnica para que todas cuenten.',
 2, 0),

(7, 'Natación 50 metros',
 'Prueba de soltura en medio acuático. Estilo libre (crol), 50 metros en piscina. Se cronometra el tiempo. Salida desde dentro del agua.',
 'Mejora la técnica de crol, especialmente la respiración bilateral. Entrena series de 25m y 50m con descansos cortos. Practica las salidas desde dentro del agua.',
 2, 1);

-- =====================================================================
-- 3. BAREMOS DE PUNTUACIÓN — POLICÍA NACIONAL
-- Según tablas oficiales BOE (escala de 0 a 10 puntos)
-- 0 puntos = eliminado; mínimo 5 de media para aprobar
-- =====================================================================

-- ---- Circuito de agilidad (prueba 1) — Hombres (tiempo en segundos) ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(1, 'HOMBRE', 11.70, 0),
(1, 'HOMBRE', 11.40, 1),
(1, 'HOMBRE', 11.10, 2),
(1, 'HOMBRE', 10.80, 3),
(1, 'HOMBRE', 10.60, 4),
(1, 'HOMBRE', 10.20, 5),
(1, 'HOMBRE', 9.80, 6),
(1, 'HOMBRE', 9.40, 7),
(1, 'HOMBRE', 9.00, 8),
(1, 'HOMBRE', 8.60, 9),
(1, 'HOMBRE', 8.20, 10);

-- ---- Circuito de agilidad (prueba 1) — Mujeres ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(1, 'MUJER', 12.80, 0),
(1, 'MUJER', 12.50, 1),
(1, 'MUJER', 12.20, 2),
(1, 'MUJER', 11.90, 3),
(1, 'MUJER', 11.60, 4),
(1, 'MUJER', 11.30, 5),
(1, 'MUJER', 10.90, 6),
(1, 'MUJER', 10.50, 7),
(1, 'MUJER', 10.10, 8),
(1, 'MUJER', 9.70, 9),
(1, 'MUJER', 9.30, 10);

-- ---- Dominadas / Suspensión (prueba 2) — Hombres (repeticiones) ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(2, 'HOMBRE', 4.00, 0),
(2, 'HOMBRE', 5.00, 1),
(2, 'HOMBRE', 6.00, 2),
(2, 'HOMBRE', 7.00, 3),
(2, 'HOMBRE', 9.00, 4),
(2, 'HOMBRE', 10.00, 5),
(2, 'HOMBRE', 12.00, 6),
(2, 'HOMBRE', 13.00, 7),
(2, 'HOMBRE', 14.00, 8),
(2, 'HOMBRE', 16.00, 9),
(2, 'HOMBRE', 17.00, 10);

-- ---- Dominadas / Suspensión (prueba 2) — Mujeres (segundos de suspensión) ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(2, 'MUJER', 35.00, 0),
(2, 'MUJER', 40.00, 1),
(2, 'MUJER', 44.00, 2),
(2, 'MUJER', 48.00, 3),
(2, 'MUJER', 53.00, 4),
(2, 'MUJER', 57.00, 5),
(2, 'MUJER', 65.00, 6),
(2, 'MUJER', 72.00, 7),
(2, 'MUJER', 80.00, 8),
(2, 'MUJER', 88.00, 9),
(2, 'MUJER', 95.00, 10);

-- ---- Carrera 1.000m (prueba 3) — Hombres (tiempo en segundos) ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(3, 'HOMBRE', 229.00, 0),
(3, 'HOMBRE', 224.00, 1),
(3, 'HOMBRE', 219.00, 2),
(3, 'HOMBRE', 214.00, 3),
(3, 'HOMBRE', 209.00, 4),
(3, 'HOMBRE', 204.00, 5),
(3, 'HOMBRE', 199.00, 6),
(3, 'HOMBRE', 194.00, 7),
(3, 'HOMBRE', 189.00, 8),
(3, 'HOMBRE', 184.00, 9),
(3, 'HOMBRE', 174.00, 10);

-- ---- Carrera 1.000m (prueba 3) — Mujeres (tiempo en segundos) ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(3, 'MUJER', 286.00, 0),
(3, 'MUJER', 278.00, 1),
(3, 'MUJER', 270.00, 2),
(3, 'MUJER', 262.00, 3),
(3, 'MUJER', 254.00, 4),
(3, 'MUJER', 249.00, 5),
(3, 'MUJER', 241.00, 6),
(3, 'MUJER', 233.00, 7),
(3, 'MUJER', 225.00, 8),
(3, 'MUJER', 216.00, 9),
(3, 'MUJER', 204.00, 10);

-- =====================================================================
-- 4. BAREMOS DE PUNTUACIÓN — GUARDIA CIVIL
-- La Guardia Civil usa sistema apto/no apto (BOE), pero para la app
-- creamos una escala de 0 a 10 basada en rendimiento proporcional
-- respecto a las marcas mínimas oficiales.
-- =====================================================================

-- ---- Carrera 2.000m (prueba 4) — Hombres (tiempo en segundos) ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(4, 'HOMBRE', 625.00, 0),
(4, 'HOMBRE', 605.00, 1),
(4, 'HOMBRE', 585.00, 2),
(4, 'HOMBRE', 575.00, 3),
(4, 'HOMBRE', 570.00, 4),
(4, 'HOMBRE', 565.00, 5),
(4, 'HOMBRE', 540.00, 6),
(4, 'HOMBRE', 510.00, 7),
(4, 'HOMBRE', 480.00, 8),
(4, 'HOMBRE', 450.00, 9),
(4, 'HOMBRE', 420.00, 10);

-- ---- Carrera 2.000m (prueba 4) — Mujeres (tiempo en segundos) ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(4, 'MUJER', 734.00, 0),
(4, 'MUJER', 720.00, 1),
(4, 'MUJER', 706.00, 2),
(4, 'MUJER', 692.00, 3),
(4, 'MUJER', 680.00, 4),
(4, 'MUJER', 674.00, 5),
(4, 'MUJER', 650.00, 6),
(4, 'MUJER', 620.00, 7),
(4, 'MUJER', 590.00, 8),
(4, 'MUJER', 560.00, 9),
(4, 'MUJER', 530.00, 10);

-- ---- Circuito agilidad GC (prueba 5) — Hombres ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(5, 'HOMBRE', 13.00, 0),
(5, 'HOMBRE', 12.60, 1),
(5, 'HOMBRE', 12.20, 2),
(5, 'HOMBRE', 11.90, 3),
(5, 'HOMBRE', 11.60, 4),
(5, 'HOMBRE', 11.40, 5),
(5, 'HOMBRE', 11.00, 6),
(5, 'HOMBRE', 10.60, 7),
(5, 'HOMBRE', 10.20, 8),
(5, 'HOMBRE', 9.80, 9),
(5, 'HOMBRE', 9.40, 10);

-- ---- Circuito agilidad GC (prueba 5) — Mujeres ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(5, 'MUJER', 14.00, 0),
(5, 'MUJER', 13.60, 1),
(5, 'MUJER', 13.30, 2),
(5, 'MUJER', 13.00, 3),
(5, 'MUJER', 12.70, 4),
(5, 'MUJER', 12.50, 5),
(5, 'MUJER', 12.10, 6),
(5, 'MUJER', 11.70, 7),
(5, 'MUJER', 11.30, 8),
(5, 'MUJER', 10.90, 9),
(5, 'MUJER', 10.50, 10);

-- ---- Flexiones (prueba 6) — Hombres (repeticiones) ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(6, 'HOMBRE', 10.00, 0),
(6, 'HOMBRE', 12.00, 1),
(6, 'HOMBRE', 13.00, 2),
(6, 'HOMBRE', 15.00, 3),
(6, 'HOMBRE', 16.00, 4),
(6, 'HOMBRE', 18.00, 5),
(6, 'HOMBRE', 22.00, 6),
(6, 'HOMBRE', 26.00, 7),
(6, 'HOMBRE', 30.00, 8),
(6, 'HOMBRE', 35.00, 9),
(6, 'HOMBRE', 40.00, 10);

-- ---- Flexiones (prueba 6) — Mujeres (repeticiones) ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(6, 'MUJER', 7.00, 0),
(6, 'MUJER', 8.00, 1),
(6, 'MUJER', 9.00, 2),
(6, 'MUJER', 11.00, 3),
(6, 'MUJER', 12.00, 4),
(6, 'MUJER', 14.00, 5),
(6, 'MUJER', 17.00, 6),
(6, 'MUJER', 20.00, 7),
(6, 'MUJER', 23.00, 8),
(6, 'MUJER', 27.00, 9),
(6, 'MUJER', 30.00, 10);

-- ---- Natación 50m (prueba 7) — Hombres (tiempo en segundos) ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(7, 'HOMBRE', 80.00, 0),
(7, 'HOMBRE', 78.00, 1),
(7, 'HOMBRE', 76.00, 2),
(7, 'HOMBRE', 74.00, 3),
(7, 'HOMBRE', 72.00, 4),
(7, 'HOMBRE', 70.00, 5),
(7, 'HOMBRE', 65.00, 6),
(7, 'HOMBRE', 58.00, 7),
(7, 'HOMBRE', 50.00, 8),
(7, 'HOMBRE', 42.00, 9),
(7, 'HOMBRE', 35.00, 10);

-- ---- Natación 50m (prueba 7) — Mujeres (tiempo en segundos) ----
INSERT INTO `baremos_puntuacion` (`pruebas_oficiales_id_pruebas_oficiales`, `genero`, `marca_valor`, `nota`) VALUES
(7, 'MUJER', 85.00, 0),
(7, 'MUJER', 83.00, 1),
(7, 'MUJER', 81.00, 2),
(7, 'MUJER', 79.00, 3),
(7, 'MUJER', 77.00, 4),
(7, 'MUJER', 75.00, 5),
(7, 'MUJER', 70.00, 6),
(7, 'MUJER', 63.00, 7),
(7, 'MUJER', 55.00, 8),
(7, 'MUJER', 47.00, 9),
(7, 'MUJER', 40.00, 10);

-- =====================================================================
-- 5. REQUISITOS DE NIVEL (umbrales para BASICO / INTERMEDIO / AVANZADO)
-- nivel_exigencia: 1 = BASICO, 2 = INTERMEDIO, 3 = AVANZADO
-- valor_objetivo = marca que el usuario debe alcanzar para ese nivel
-- =====================================================================

-- Policía Nacional — Circuito agilidad (prueba 1)
INSERT INTO `requisitos_nivel` (`genero`, `nivel_exigencia`, `valor_objetivo`, `pruebas_oficiales_id_pruebas_oficiales`) VALUES
('HOMBRE', 1, 11.40, 1), ('HOMBRE', 2, 10.20, 1), ('HOMBRE', 3, 8.60, 1),
('MUJER',  1, 12.50, 1), ('MUJER',  2, 11.30, 1), ('MUJER',  3, 9.70, 1);

-- Policía Nacional — Dominadas/Suspensión (prueba 2)
INSERT INTO `requisitos_nivel` (`genero`, `nivel_exigencia`, `valor_objetivo`, `pruebas_oficiales_id_pruebas_oficiales`) VALUES
('HOMBRE', 1, 6.00, 2),  ('HOMBRE', 2, 10.00, 2), ('HOMBRE', 3, 16.00, 2),
('MUJER',  1, 44.00, 2), ('MUJER',  2, 57.00, 2), ('MUJER',  3, 88.00, 2);

-- Policía Nacional — Carrera 1.000m (prueba 3)
INSERT INTO `requisitos_nivel` (`genero`, `nivel_exigencia`, `valor_objetivo`, `pruebas_oficiales_id_pruebas_oficiales`) VALUES
('HOMBRE', 1, 219.00, 3), ('HOMBRE', 2, 204.00, 3), ('HOMBRE', 3, 184.00, 3),
('MUJER',  1, 270.00, 3), ('MUJER',  2, 249.00, 3), ('MUJER',  3, 216.00, 3);

-- Guardia Civil — Carrera 2.000m (prueba 4)
INSERT INTO `requisitos_nivel` (`genero`, `nivel_exigencia`, `valor_objetivo`, `pruebas_oficiales_id_pruebas_oficiales`) VALUES
('HOMBRE', 1, 585.00, 4), ('HOMBRE', 2, 565.00, 4), ('HOMBRE', 3, 450.00, 4),
('MUJER',  1, 706.00, 4), ('MUJER',  2, 674.00, 4), ('MUJER',  3, 560.00, 4);

-- Guardia Civil — Circuito agilidad (prueba 5)
INSERT INTO `requisitos_nivel` (`genero`, `nivel_exigencia`, `valor_objetivo`, `pruebas_oficiales_id_pruebas_oficiales`) VALUES
('HOMBRE', 1, 12.20, 5), ('HOMBRE', 2, 11.40, 5), ('HOMBRE', 3, 9.80, 5),
('MUJER',  1, 13.30, 5), ('MUJER',  2, 12.50, 5), ('MUJER',  3, 10.90, 5);

-- Guardia Civil — Flexiones (prueba 6)
INSERT INTO `requisitos_nivel` (`genero`, `nivel_exigencia`, `valor_objetivo`, `pruebas_oficiales_id_pruebas_oficiales`) VALUES
('HOMBRE', 1, 13.00, 6), ('HOMBRE', 2, 18.00, 6), ('HOMBRE', 3, 35.00, 6),
('MUJER',  1, 9.00, 6),  ('MUJER',  2, 14.00, 6), ('MUJER',  3, 27.00, 6);

-- Guardia Civil — Natación 50m (prueba 7)
INSERT INTO `requisitos_nivel` (`genero`, `nivel_exigencia`, `valor_objetivo`, `pruebas_oficiales_id_pruebas_oficiales`) VALUES
('HOMBRE', 1, 76.00, 7), ('HOMBRE', 2, 70.00, 7), ('HOMBRE', 3, 42.00, 7),
('MUJER',  1, 81.00, 7), ('MUJER',  2, 75.00, 7), ('MUJER',  3, 47.00, 7);

-- =====================================================================
-- 6. EJERCICIOS (catálogo de ejercicios para las rutinas)
-- =====================================================================
INSERT INTO `ejercicios` (`id_ejercicio`, `nombre`, `video_url`, `instrucciones_tecnicas`) VALUES
(1,  'Dominadas estrictas',       NULL, 'Agarre prono, separación de manos a la anchura de hombros. Extensión completa de brazos abajo, barbilla por encima de la barra arriba. Sin balanceo ni kipping.'),
(2,  'Dominadas asistidas',       NULL, 'Igual que las dominadas estrictas pero con banda elástica en los pies para reducir el peso efectivo. Ideal para progresar hacia dominadas sin asistencia.'),
(3,  'Carrera continua 30 min',   NULL, 'Correr a ritmo constante durante 30 minutos. Ritmo conversacional, sin llegar a la fatiga. Mejora la base aeróbica para las pruebas de carrera.'),
(4,  'Series 400m',               NULL, 'Correr 400 metros al ritmo objetivo de la prueba de 1000m. Descanso de 1:30-2:00 entre series. Hacer entre 4 y 8 repeticiones.'),
(5,  'Series 200m',               NULL, 'Correr 200 metros a ritmo rápido (por debajo del ritmo de competición). Descanso de 1:00-1:30 entre series. Hacer entre 6 y 10 repeticiones.'),
(6,  'Flexiones de brazos',       NULL, 'Posición de plancha con manos a la anchura de hombros. Bajar el pecho hasta tocar el suelo manteniendo el cuerpo recto. Subir extendiendo los brazos completamente.'),
(7,  'Circuito de conos',         NULL, 'Colocar 6-8 conos en zigzag separados 2 metros. Recorrer ida y vuelta lo más rápido posible. Trabajar los cambios de dirección y la coordinación.'),
(8,  'Natación crol 50m',         NULL, 'Nadar 50 metros estilo crol cronometrado. Practicar la salida desde dentro del agua. Respiración bilateral cada 3 brazadas.'),
(9,  'Series natación 25m',       NULL, 'Nadar 25 metros a máxima velocidad. Descanso de 30-45 segundos entre series. Hacer entre 8 y 12 repeticiones. Mejora la velocidad en el agua.'),
(10, 'Suspensión en barra',       NULL, 'Colgarse de la barra con agarre prono y brazos flexionados, barbilla por encima de la barra. Mantener la posición el mayor tiempo posible sin balanceo.'),
(11, 'Plancha abdominal',         NULL, 'Posición de plancha sobre antebrazos. Mantener el cuerpo recto (sin hundir la cadera) durante el tiempo indicado. Fortalece el core y mejora la estabilidad.'),
(12, 'Sentadillas',               NULL, 'De pie, pies a la anchura de hombros. Bajar flexionando rodillas hasta que los muslos queden paralelos al suelo. Subir empujando con los talones. Espalda recta.'),
(13, 'Burpees',                   NULL, 'Desde de pie, bajar a posición de flexión, hacer una flexión, recoger pies hacia las manos y saltar con brazos arriba. Ejercicio completo de fuerza y resistencia.'),
(14, 'Zancadas',                  NULL, 'Dar un paso largo hacia adelante flexionando ambas rodillas a 90°. La rodilla trasera casi toca el suelo. Alternar piernas. Fortalece cuádriceps y glúteos.'),
(15, 'Fartlek 20 min',            NULL, 'Carrera continua alternando ritmos: 2 minutos a ritmo rápido, 2 minutos a ritmo suave. Mejora la capacidad de cambiar de ritmo y la resistencia anaeróbica.'),
(16, 'Remo invertido',            NULL, 'Colgarse bajo una barra baja con el cuerpo recto. Tirar del pecho hacia la barra apretando escápulas. Fortalece la espalda, complemento perfecto para dominadas.'),
(17, 'Escalera de agilidad',      NULL, 'Realizar diferentes patrones de pisada a máxima velocidad por una escalera de agilidad en el suelo. Mejora la coordinación y la velocidad de pies.'),
(18, 'Carrera continua 45 min',   NULL, 'Rodaje largo a ritmo suave-moderado. Desarrolla la resistencia aeróbica de base, fundamental para las pruebas de 1000m y 2000m.'),
(19, 'Press banca',               NULL, 'Tumbado en banco plano, bajar la barra al pecho y empujar hasta extensión completa de brazos. Agarre a la anchura de hombros. Fortalece pectoral y tríceps.'),
(20, 'Fondos en paralelas',       NULL, 'Apoyado en barras paralelas, flexionar brazos hasta que el codo forme 90° y empujar hasta extensión completa. Trabaja pectoral, tríceps y deltoides anterior.');

-- =====================================================================
-- 7. RUTINAS DE OPOSICIÓN
-- Una rutina por cada combinación: oposición × nivel × género
-- =====================================================================
INSERT INTO `rutinas_opo` (`id_rutina_opo`, `nivel`, `genero`, `enfoque_tipo`, `oposiciones_id_oposicion`) VALUES
-- Policía Nacional
(1,  'BASICO',      'HOMBRE', 'RESISTENCIA', 1),
(2,  'INTERMEDIO',  'HOMBRE', 'FUERZA',      1),
(3,  'AVANZADO',    'HOMBRE', 'VELOCIDAD',   1),
(4,  'BASICO',      'MUJER',  'RESISTENCIA', 1),
(5,  'INTERMEDIO',  'MUJER',  'FUERZA',      1),
(6,  'AVANZADO',    'MUJER',  'VELOCIDAD',   1),
-- Guardia Civil
(7,  'BASICO',      'HOMBRE', 'RESISTENCIA', 2),
(8,  'INTERMEDIO',  'HOMBRE', 'FUERZA',      2),
(9,  'AVANZADO',    'HOMBRE', 'VELOCIDAD',   2),
(10, 'BASICO',      'MUJER',  'RESISTENCIA', 2),
(11, 'INTERMEDIO',  'MUJER',  'FUERZA',      2),
(12, 'AVANZADO',    'MUJER',  'VELOCIDAD',   2);

-- =====================================================================
-- 8. DETALLE DE RUTINAS (ejercicios asignados a cada rutina)
-- =====================================================================

-- ---- Policía Nacional — BASICO HOMBRE (rutina 1) ----
INSERT INTO `detalle_rutina_opo` (`ejercicios_id_ejercicio`, `rutinas_opo_id_rutina_opo`, `repeticiones`, `series`, `descanso`) VALUES
(2,  1, 8,  3, 90),   -- Dominadas asistidas
(6,  1, 15, 3, 60),   -- Flexiones
(3,  1, 1,  1, 0),    -- Carrera continua 30 min
(7,  1, 4,  3, 60),   -- Circuito de conos
(11, 1, 30, 3, 45);   -- Plancha abdominal (30 seg)

-- ---- Policía Nacional — INTERMEDIO HOMBRE (rutina 2) ----
INSERT INTO `detalle_rutina_opo` (`ejercicios_id_ejercicio`, `rutinas_opo_id_rutina_opo`, `repeticiones`, `series`, `descanso`) VALUES
(1,  2, 8,  4, 90),   -- Dominadas estrictas
(4,  2, 1,  6, 120),  -- Series 400m
(7,  2, 6,  4, 45),   -- Circuito de conos
(12, 2, 15, 4, 60),   -- Sentadillas
(16, 2, 12, 3, 60);   -- Remo invertido

-- ---- Policía Nacional — AVANZADO HOMBRE (rutina 3) ----
INSERT INTO `detalle_rutina_opo` (`ejercicios_id_ejercicio`, `rutinas_opo_id_rutina_opo`, `repeticiones`, `series`, `descanso`) VALUES
(1,  3, 12, 5, 90),   -- Dominadas estrictas
(5,  3, 1,  8, 90),   -- Series 200m
(7,  3, 8,  5, 30),   -- Circuito de conos
(13, 3, 10, 4, 45),   -- Burpees
(17, 3, 1,  6, 30);   -- Escalera agilidad

-- ---- Policía Nacional — BASICO MUJER (rutina 4) ----
INSERT INTO `detalle_rutina_opo` (`ejercicios_id_ejercicio`, `rutinas_opo_id_rutina_opo`, `repeticiones`, `series`, `descanso`) VALUES
(10, 4, 20, 3, 90),   -- Suspensión en barra (20 seg)
(6,  4, 10, 3, 60),   -- Flexiones
(3,  4, 1,  1, 0),    -- Carrera continua 30 min
(7,  4, 4,  3, 60),   -- Circuito de conos
(11, 4, 20, 3, 45);   -- Plancha abdominal (20 seg)

-- ---- Policía Nacional — INTERMEDIO MUJER (rutina 5) ----
INSERT INTO `detalle_rutina_opo` (`ejercicios_id_ejercicio`, `rutinas_opo_id_rutina_opo`, `repeticiones`, `series`, `descanso`) VALUES
(10, 5, 35, 4, 90),   -- Suspensión en barra (35 seg)
(4,  5, 1,  5, 120),  -- Series 400m
(7,  5, 6,  4, 45),   -- Circuito de conos
(14, 5, 12, 4, 60),   -- Zancadas
(16, 5, 10, 3, 60);   -- Remo invertido

-- ---- Policía Nacional — AVANZADO MUJER (rutina 6) ----
INSERT INTO `detalle_rutina_opo` (`ejercicios_id_ejercicio`, `rutinas_opo_id_rutina_opo`, `repeticiones`, `series`, `descanso`) VALUES
(10, 6, 50, 5, 90),   -- Suspensión en barra (50 seg)
(5,  6, 1,  8, 90),   -- Series 200m
(7,  6, 8,  5, 30),   -- Circuito de conos
(13, 6, 8,  4, 45),   -- Burpees
(17, 6, 1,  6, 30);   -- Escalera agilidad

-- ---- Guardia Civil — BASICO HOMBRE (rutina 7) ----
INSERT INTO `detalle_rutina_opo` (`ejercicios_id_ejercicio`, `rutinas_opo_id_rutina_opo`, `repeticiones`, `series`, `descanso`) VALUES
(6,  7, 12, 3, 60),   -- Flexiones
(3,  7, 1,  1, 0),    -- Carrera continua 30 min
(7,  7, 4,  3, 60),   -- Circuito de conos
(8,  7, 1,  4, 60),   -- Natación crol 50m
(11, 7, 30, 3, 45);   -- Plancha abdominal

-- ---- Guardia Civil — INTERMEDIO HOMBRE (rutina 8) ----
INSERT INTO `detalle_rutina_opo` (`ejercicios_id_ejercicio`, `rutinas_opo_id_rutina_opo`, `repeticiones`, `series`, `descanso`) VALUES
(6,  8, 20, 4, 60),   -- Flexiones
(15, 8, 1,  1, 0),    -- Fartlek 20 min
(7,  8, 6,  4, 45),   -- Circuito de conos
(9,  8, 1,  10, 45),  -- Series natación 25m
(19, 8, 10, 4, 90);   -- Press banca

-- ---- Guardia Civil — AVANZADO HOMBRE (rutina 9) ----
INSERT INTO `detalle_rutina_opo` (`ejercicios_id_ejercicio`, `rutinas_opo_id_rutina_opo`, `repeticiones`, `series`, `descanso`) VALUES
(6,  9, 30, 5, 45),   -- Flexiones
(4,  9, 1,  8, 90),   -- Series 400m
(17, 9, 1,  6, 30),   -- Escalera agilidad
(9,  9, 1,  12, 30),  -- Series natación 25m
(13, 9, 12, 4, 45);   -- Burpees

-- ---- Guardia Civil — BASICO MUJER (rutina 10) ----
INSERT INTO `detalle_rutina_opo` (`ejercicios_id_ejercicio`, `rutinas_opo_id_rutina_opo`, `repeticiones`, `series`, `descanso`) VALUES
(6,  10, 8,  3, 60),  -- Flexiones
(3,  10, 1,  1, 0),   -- Carrera continua 30 min
(7,  10, 4,  3, 60),  -- Circuito de conos
(8,  10, 1,  4, 60),  -- Natación crol 50m
(11, 10, 20, 3, 45);  -- Plancha abdominal

-- ---- Guardia Civil — INTERMEDIO MUJER (rutina 11) ----
INSERT INTO `detalle_rutina_opo` (`ejercicios_id_ejercicio`, `rutinas_opo_id_rutina_opo`, `repeticiones`, `series`, `descanso`) VALUES
(6,  11, 15, 4, 60),  -- Flexiones
(15, 11, 1,  1, 0),   -- Fartlek 20 min
(7,  11, 6,  4, 45),  -- Circuito de conos
(9,  11, 1,  8, 45),  -- Series natación 25m
(14, 11, 12, 4, 60);  -- Zancadas

-- ---- Guardia Civil — AVANZADO MUJER (rutina 12) ----
INSERT INTO `detalle_rutina_opo` (`ejercicios_id_ejercicio`, `rutinas_opo_id_rutina_opo`, `repeticiones`, `series`, `descanso`) VALUES
(6,  12, 25, 5, 45),  -- Flexiones
(4,  12, 1,  6, 90),  -- Series 400m
(17, 12, 1,  6, 30),  -- Escalera agilidad
(9,  12, 1,  10, 30), -- Series natación 25m
(13, 12, 10, 4, 45);  -- Burpees

-- =====================================================================
-- 9. NOTICIAS (información relevante sobre las oposiciones)
-- =====================================================================
INSERT INTO `noticias` (`titulo`, `contenido`, `fecha_publicacion`, `oposiciones_id_oposicion`) VALUES
('Convocatoria Policía Nacional 2026 publicada en BOE',
 'Se ha publicado en el Boletín Oficial del Estado la nueva convocatoria de la Escala Básica de Policía Nacional con un total de 2.880 plazas. El plazo de solicitudes finaliza 20 días hábiles después de la publicación. Las pruebas físicas mantienen el mismo formato: circuito de agilidad, dominadas/suspensión y carrera de 1.000 metros.',
 '2026-01-15 09:00:00', 1),

('Modificación del circuito de agilidad CNP',
 'La Dirección General de la Policía ha confirmado que el circuito de agilidad mantiene las mismas características que en convocatorias anteriores. Se recomienda a los aspirantes practicar con el circuito oficial publicado en la web de la Policía Nacional. Se permiten dos intentos y se contabiliza el mejor tiempo.',
 '2026-02-01 10:00:00', 1),

('Convocatoria Guardia Civil 2026',
 'Publicada la convocatoria de acceso libre a la Guardia Civil con 2.091 plazas. Las pruebas físicas incluyen carrera de 2.000 metros, circuito de agilidad, flexiones de brazos y natación de 50 metros. El sistema de calificación es apto/no apto con marcas mínimas según sexo y edad.',
 '2026-01-20 09:00:00', 2),

('Consejos para la prueba de natación de Guardia Civil',
 'La prueba de natación de 50 metros es eliminatoria. Los aspirantes deben completar la distancia en estilo libre dentro del tiempo máximo establecido (70 segundos hombres, 75 segundos mujeres menores de 35 años). La salida se realiza desde dentro del agua. Se recomienda practicar la técnica de crol y las salidas desde la pared.',
 '2026-02-10 11:00:00', 2);
