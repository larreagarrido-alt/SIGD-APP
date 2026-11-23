SET NAMES utf8mb4;

-- The existing official evaluation tables remain the single source of scores.
ALTER TABLE `wp_kpi_definiciones`
  ADD COLUMN `responsable_revision` INT NULL,
  ADD COLUMN `regla_aprobada_por` INT NULL,
  ADD COLUMN `regla_aprobada_en` DATETIME NULL;

CREATE TABLE `wp_kpi_reporte_evaluaciones` (
  `id_detalle` BIGINT UNSIGNED NOT NULL,
  `id_evaluacion` BIGINT UNSIGNED NOT NULL,
  `payload_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `fecha_evaluada` DATE NOT NULL,
  `vinculado_por` INT NOT NULL,
  `vinculado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_detalle`,`id_evaluacion`),
  UNIQUE KEY `uk_wpkpirep_eval` (`id_evaluacion`),
  KEY `idx_wpkpirep_detalle` (`id_detalle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `wp_ranking_politicas` (
  `id_politica` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `periodo_clave` CHAR(7) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `formula` ENUM('PROMEDIO_PONDERADO') NOT NULL,
  `comparacion_cargos_aprobada` TINYINT(1) NOT NULL DEFAULT 0,
  `justificacion` TEXT NOT NULL,
  `aprobado_por` INT NOT NULL,
  `aprobado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_politica`),
  UNIQUE KEY `uk_wprankpol_periodo` (`periodo_clave`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `wp_ranking_cierres` (
  `id_cierre` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `periodo_clave` CHAR(7) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `id_politica` BIGINT UNSIGNED NOT NULL,
  `evaluaciones_aprobadas` INT UNSIGNED NOT NULL,
  `participantes` INT UNSIGNED NOT NULL,
  `origen_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `cerrado_por` INT NOT NULL,
  `cerrado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_cierre`),
  UNIQUE KEY `uk_wprankcierre_periodo` (`periodo_clave`),
  KEY `idx_wprankcierre_politica` (`id_politica`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `wp_permisos` (`codigo`,`modulo`,`descripcion`,`es_sensible`)
VALUES ('ranking.cerrar','ranking','Aprobar reglas y cerrar ranking mensual',1);
INSERT INTO `wp_migraciones` (`version`,`descripcion`)
VALUES ('V023','Vinculo de reportes KPI, aprobacion de reglas y cierres mensuales');
