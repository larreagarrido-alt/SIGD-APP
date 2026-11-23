-- Plantillas visuales del Editor Maestro, compartidas entre dispositivos.
-- No altera definiciones, evaluaciones ni reportes KPI ya confirmados.
CREATE TABLE `wp_kpi_plantillas_estado` (
  `id` TINYINT UNSIGNED NOT NULL,
  `revision` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `estado_json` LONGTEXT NOT NULL,
  `actualizado_por` INT NULL,
  `actualizado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_wpkpiplantillas_json` CHECK (JSON_VALID(`estado_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `wp_kpi_plantillas_estado` (`id`,`revision`,`estado_json`)
VALUES (1,0,'{}');

CREATE TABLE `wp_kpi_plantillas_alertas` (
  `id_alerta` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `cargo_key` VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `revision` BIGINT UNSIGNED NOT NULL,
  `estado` ENUM('PENDIENTE','ATENDIDA') NOT NULL DEFAULT 'PENDIENTE',
  `creada_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `atendida_por` INT NULL,
  `atendida_en` DATETIME NULL,
  PRIMARY KEY (`id_alerta`),
  KEY `idx_wpkpiplantalert_estado` (`estado`,`creada_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `wp_migraciones` (`version`,`descripcion`)
VALUES ('V026','Plantillas visuales KPI compartidas y alertas del Editor Maestro');
