-- Metadatos de convocatoria y limpieza de datos incorrectos (ejecutar tras migration_v2)
USE `mydb`;

ALTER TABLE `oposiciones`
  ADD COLUMN `convocatoria_ref` VARCHAR(500) NULL,
  ADD COLUMN `notas_usuario` TEXT NULL;

ALTER TABLE `pruebas_oficiales`
  ADD COLUMN `convocatoria_ref` VARCHAR(500) NULL,
  ADD COLUMN `fuente_legal` VARCHAR(500) NULL,
  ADD COLUMN `tipo_baremo` ENUM('PUNTUACION','APTO_NO_APTO') NOT NULL DEFAULT 'PUNTUACION',
  ADD COLUMN `unidad_entrada` VARCHAR(10) NOT NULL DEFAULT 'reps' COMMENT 's = segundos, reps = repeticiones';

-- Eliminar datos generados incorrectamente (ids 3-6 y pruebas 8-22)
DELETE FROM `simulacro_pruebas` WHERE `simulacros_id_simulacro` IN (
  SELECT `id_simulacro` FROM `simulacros` WHERE `oposiciones_id_oposicion` IN (3, 4, 5, 6)
);
DELETE FROM `simulacros` WHERE `oposiciones_id_oposicion` IN (3, 4, 5, 6);
DELETE FROM `baremos_puntuacion` WHERE `pruebas_oficiales_id_pruebas_oficiales` BETWEEN 8 AND 30;
DELETE FROM `requisitos_nivel` WHERE `pruebas_oficiales_id_pruebas_oficiales` BETWEEN 8 AND 30;
DELETE FROM `marcas_perfil` WHERE `pruebas_oficiales_id_pruebas_oficiales` BETWEEN 8 AND 30;
DELETE FROM `pruebas_oficiales` WHERE `id_pruebas_oficiales` BETWEEN 8 AND 30;
DELETE FROM `detalle_rutina_opo` WHERE `rutinas_opo_id_rutina_opo` BETWEEN 37 AND 120;
DELETE FROM `rutinas_opo` WHERE `id_rutina_opo` BETWEEN 37 AND 120;
DELETE FROM `noticias` WHERE `oposiciones_id_oposicion` IN (3, 4, 5, 6);

-- Usuarios que tenían "Policía Local" (id 4) deben elegir otra oposición o Bomberos Ayto. Madrid
