-- Bienestar organizacional. No inserta preguntas Ryff, puntuaciones ni permisos de rol.
-- Las respuestas siguen en las tablas privadas de V006; las campañas las vinculan sin copiarlas.
SET NAMES utf8mb4;

ALTER TABLE `wp_bienestar_preguntas`
  ADD COLUMN `dimension_ryff` ENUM('AUTOACEPTACION','RELACIONES_POSITIVAS','AUTONOMIA','DOMINIO_ENTORNO','PROPOSITO_VIDA','CRECIMIENTO_PERSONAL') NULL AFTER `es_inversa`;

CREATE TABLE `wp_bienestar_ryff_instrumentos` (
  `id_test` INT UNSIGNED NOT NULL,
  `version_instrumento` VARCHAR(80) NOT NULL,
  `escala_min` TINYINT UNSIGNED NOT NULL,
  `escala_max` TINYINT UNSIGNED NOT NULL,
  `procedencia` VARCHAR(255) NOT NULL,
  `validado_por` VARCHAR(150) NOT NULL,
  `uso_autorizado_por` VARCHAR(150) NOT NULL,
  `validado_en` DATETIME NOT NULL,
  `creado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_test`),
  CONSTRAINT `fk_wpwell_ryff_test` FOREIGN KEY (`id_test`) REFERENCES `wp_bienestar_tests` (`id_test`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `wp_bienestar_campanas` (
  `id_campana` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_test` INT UNSIGNED NULL,
  `titulo` VARCHAR(150) NOT NULL,
  `descripcion` TEXT NOT NULL,
  `audiencia` ENUM('COLABORADOR','PRACTICANTE','AMBOS') NOT NULL,
  `id_agencia` INT NULL,
  `inicio_en` DATETIME NOT NULL,
  `fin_en` DATETIME NOT NULL,
  `estado` ENUM('BORRADOR','PUBLICADA','DESACTIVADA','CERRADA') NOT NULL DEFAULT 'BORRADOR',
  `creado_por` INT NOT NULL,
  `creado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_campana`),
  KEY `idx_wpwell_campana_vigencia` (`estado`,`inicio_en`,`fin_en`),
  CONSTRAINT `fk_wpwell_campana_test` FOREIGN KEY (`id_test`) REFERENCES `wp_bienestar_tests` (`id_test`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `wp_bienestar_participaciones` (
  `id_campana` BIGINT UNSIGNED NOT NULL,
  `id_evaluacion_bienestar` BIGINT UNSIGNED NOT NULL,
  `id_trabajador` INT NOT NULL,
  `consentimiento_version` VARCHAR(40) NOT NULL,
  `consentido_en` DATETIME NOT NULL,
  `revision_privada_consentida_en` DATETIME NULL,
  `revision_privada_finalidad` VARCHAR(500) NULL,
  `retirado_en` DATETIME NULL,
  PRIMARY KEY (`id_campana`,`id_trabajador`),
  UNIQUE KEY `uk_wpwell_part_eval` (`id_evaluacion_bienestar`),
  CONSTRAINT `fk_wpwell_part_campana` FOREIGN KEY (`id_campana`) REFERENCES `wp_bienestar_campanas` (`id_campana`),
  CONSTRAINT `fk_wpwell_part_eval` FOREIGN KEY (`id_evaluacion_bienestar`) REFERENCES `wp_bienestar_evaluaciones` (`id_evaluacion_bienestar`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `wp_bienestar_accesos_privados` (
  `id_evaluacion_bienestar` BIGINT UNSIGNED NOT NULL,
  `id_usuario_autorizado` INT NOT NULL,
  `finalidad` VARCHAR(500) NOT NULL,
  `autorizado_por` INT NOT NULL,
  `autorizado_en` DATETIME NOT NULL,
  `vence_en` DATETIME NOT NULL,
  `revocado_en` DATETIME NULL,
  `observaciones_privadas` TEXT NULL,
  `observado_en` DATETIME NULL,
  PRIMARY KEY (`id_evaluacion_bienestar`,`id_usuario_autorizado`),
  CONSTRAINT `fk_wpwell_priv_eval` FOREIGN KEY (`id_evaluacion_bienestar`) REFERENCES `wp_bienestar_evaluaciones` (`id_evaluacion_bienestar`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `wp_bienestar_planes` (
  `id_plan` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(150) NOT NULL,
  `objetivo` TEXT NOT NULL,
  `dimension` ENUM('AUTOACEPTACION','RELACIONES_POSITIVAS','AUTONOMIA','DOMINIO_ENTORNO','PROPOSITO_VIDA','CRECIMIENTO_PERSONAL','GENERAL') NOT NULL,
  `responsable` VARCHAR(150) NOT NULL,
  `fecha_programada` DATE NOT NULL,
  `estado` ENUM('PENDIENTE','EN_PROCESO','COMPLETADA') NOT NULL DEFAULT 'PENDIENTE',
  `observaciones` TEXT NULL,
  `creado_por` INT NOT NULL,
  `creado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_plan`),
  KEY `idx_wpwell_plan_fecha` (`fecha_programada`,`estado`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `wp_permisos` (`codigo`,`modulo`,`descripcion`,`es_sensible`) VALUES
('bienestar.administrar','bienestar','Administrar campañas Ryff y planes de bienestar',1);
INSERT INTO `wp_migraciones` (`version`,`descripcion`) VALUES
('V024','Campañas Ryff, consentimientos, acceso privado y planes organizacionales');
