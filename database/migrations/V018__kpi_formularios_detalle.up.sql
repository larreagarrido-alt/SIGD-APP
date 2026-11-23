-- Staging proposal: complete form snapshots, independent of official KPI scoring.
-- Check the target schema and take a verified backup before any authorized deployment.
CREATE TABLE `wp_kpi_formulario_cargos` (
  `id_cargo` INT NOT NULL,
  `tipo_formulario` VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `aprobado_por` INT NULL,
  `aprobado_en` DATETIME NULL,
  PRIMARY KEY (`id_cargo`,`tipo_formulario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `wp_kpi_formularios_detalle` (
  `id_detalle` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_trabajador` INT NOT NULL,
  `id_usuario` INT NOT NULL,
  `id_cargo` INT NOT NULL,
  `tipo_formulario` VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `registro_uid` VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `revision` BIGINT UNSIGNED NOT NULL,
  `payload_json` LONGTEXT NOT NULL,
  `payload_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `creado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_detalle`),
  UNIQUE KEY `uk_wpkpiform_worker_type_uid` (`id_trabajador`,`tipo_formulario`,`registro_uid`),
  KEY `idx_wpkpiform_worker_time` (`id_trabajador`,`actualizado_en`),
  CONSTRAINT `ck_wpkpiform_json` CHECK (JSON_VALID(`payload_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `wp_migraciones` (`version`,`descripcion`)
VALUES ('V018','Detalle completo de formularios KPI Android');
