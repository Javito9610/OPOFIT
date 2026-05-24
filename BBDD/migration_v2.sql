-- Ejecutar en la BD (mydb). Si una columna ya existe, omite ese ALTER.
USE `mydb`;

ALTER TABLE `usuarios`
  ADD COLUMN `es_premium` TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN `premium_hasta` DATETIME NULL,
  ADD COLUMN `perfil_publico` TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN `fcm_token` VARCHAR(512) NULL,
  ADD COLUMN `es_admin` TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE `oposiciones`
  ADD COLUMN `incluida_gratis` TINYINT(1) NOT NULL DEFAULT 0;

UPDATE `oposiciones` SET `incluida_gratis` = 1;

CREATE TABLE IF NOT EXISTS `simulacros` (
  `id_simulacro` INT NOT NULL AUTO_INCREMENT,
  `fecha` DATETIME NOT NULL,
  `nota_media` DECIMAL(5,2) NULL,
  `usuarios_id_usuario` INT NOT NULL,
  `oposiciones_id_oposicion` INT NOT NULL,
  PRIMARY KEY (`id_simulacro`),
  INDEX `fk_sim_usuario_idx` (`usuarios_id_usuario`),
  INDEX `fk_sim_opo_idx` (`oposiciones_id_oposicion`),
  CONSTRAINT `fk_sim_usuario` FOREIGN KEY (`usuarios_id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `fk_sim_opo` FOREIGN KEY (`oposiciones_id_oposicion`) REFERENCES `oposiciones` (`id_oposicion`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `simulacro_pruebas` (
  `id_simulacro_prueba` INT NOT NULL AUTO_INCREMENT,
  `valor_registrado` DECIMAL(10,2) NOT NULL,
  `nota_obtenida` INT NULL,
  `simulacros_id_simulacro` INT NOT NULL,
  `pruebas_oficiales_id_pruebas_oficiales` INT NOT NULL,
  PRIMARY KEY (`id_simulacro_prueba`),
  CONSTRAINT `fk_sp_sim` FOREIGN KEY (`simulacros_id_simulacro`) REFERENCES `simulacros` (`id_simulacro`) ON DELETE CASCADE,
  CONSTRAINT `fk_sp_prueba` FOREIGN KEY (`pruebas_oficiales_id_pruebas_oficiales`) REFERENCES `pruebas_oficiales` (`id_pruebas_oficiales`)
) ENGINE=InnoDB;
