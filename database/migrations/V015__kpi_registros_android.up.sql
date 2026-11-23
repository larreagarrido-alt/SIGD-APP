-- Local integration proposal. Apply only after comparing the target schema and taking a backup.
-- Fail if a table with this name already exists; never silently accept a different schema.
CREATE TABLE `wp_kpi_registros_android` (
  `id_registro` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_trabajador` INT NOT NULL,
  `id_usuario` INT NOT NULL,
  `record_id` VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `id_kpi` INT UNSIGNED NOT NULL,
  `periodo_clave` VARCHAR(20) NOT NULL,
  `fecha` DATE NOT NULL,
  `valor_real` DECIMAL(12,2) NOT NULL,
  `meta_usada` DECIMAL(12,2) NOT NULL,
  `peso_usado` DECIMAL(5,2) NOT NULL,
  `payload_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `recibido_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_registro`),
  UNIQUE KEY `uk_wpkpireg_worker_record` (`id_trabajador`, `record_id`),
  KEY `idx_wpkpireg_worker_date` (`id_trabajador`, `fecha`),
  KEY `idx_wpkpireg_kpi` (`id_kpi`),
  CONSTRAINT `fk_wpkpireg_kpi` FOREIGN KEY (`id_kpi`) REFERENCES `wp_kpi_definiciones` (`id_kpi`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `wp_migraciones` (`version`, `descripcion`)
VALUES ('V015', 'Capturas KPI Android pendientes de evaluación oficial');
