-- Horarios compartidos por cargo y turnos individuales. No carga horarios de ejemplo ni concede permisos.
SET NAMES utf8mb4;

CREATE TABLE `wp_horarios_plantillas` (
  `id_plantilla` VARCHAR(60) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `perfil_json` LONGTEXT NOT NULL,
  `actualizado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_plantilla`),
  CONSTRAINT `ck_wphorario_plantilla_json` CHECK (JSON_VALID(`perfil_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `wp_horarios_cargo` (
  `id_cargo` INT NOT NULL,
  `perfil_json` LONGTEXT NOT NULL,
  `revision` BIGINT UNSIGNED NOT NULL DEFAULT 1,
  `publicado` TINYINT(1) NOT NULL DEFAULT 0,
  `actualizado_por` INT NULL,
  `actualizado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_cargo`),
  CONSTRAINT `ck_wphorario_json` CHECK (JSON_VALID(`perfil_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `wp_horarios_turnos` (
  `id_trabajador` INT NOT NULL,
  `turno` ENUM('MANANA','TARDE','COMPLETO') NOT NULL,
  `hora_entrada` TIME NOT NULL,
  `hora_salida` TIME NOT NULL,
  `actualizado_por` INT NOT NULL,
  `actualizado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_trabajador`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `wp_horarios_alertas` (
  `id_alerta` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_cargo` INT NOT NULL,
  `revision` BIGINT UNSIGNED NOT NULL,
  `estado` ENUM('PENDIENTE','ATENDIDA') NOT NULL DEFAULT 'PENDIENTE',
  `creada_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `atendida_por` INT NULL,
  `atendida_en` DATETIME NULL,
  PRIMARY KEY (`id_alerta`),
  UNIQUE KEY `uk_wphorario_alerta_revision` (`id_cargo`,`revision`),
  KEY `idx_wphorario_alerta_estado` (`estado`,`creada_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `wp_migraciones` (`version`,`descripcion`)
VALUES ('V025','Plantillas y horarios compartidos por cargo y turnos por trabajador');
