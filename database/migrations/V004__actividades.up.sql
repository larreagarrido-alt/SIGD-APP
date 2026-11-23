USE `websiteplus`;
-- =====================================================================
-- WebSite+  |  V004  |  Actividades diarias/semanales e historial
-- Distingue actividades laborales reales de registros de asistencia o evaluaciones KPI.
-- Permite asociar opcionalmente una actividad a un KPI y adjuntar evidencia.
-- =====================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `wp_actividades` (
  `id_actividad`       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_trabajador`      INT(11)         NOT NULL,
  `id_evaluacion_kpi`  BIGINT UNSIGNED NULL COMMENT 'Vinculación opcional con una entrega de KPI',
  `id_archivo`         BIGINT UNSIGNED NULL COMMENT 'Evidencia adjunta (captura, PDF, ticket)',
  `titulo`             VARCHAR(150)    NOT NULL,
  `descripcion`        TEXT            NOT NULL,
  `fecha_actividad`    DATE            NOT NULL,
  `hora_inicio`        TIME            NULL,
  `hora_fin`           TIME            NULL,
  `horas_dedicadas`    DECIMAL(4,2)    NULL,
  `estado`             ENUM('Borrador','Registrada','Revisada','Observada','Cancelada') NOT NULL DEFAULT 'Registrada',
  `observacion_supervisor` TEXT        NULL,
  `revisado_por`       INT(11)         NULL,
  `revisado_en`        DATETIME        NULL,
  `creado_en`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_actividad`),
  KEY `idx_wpact_trab_fecha` (`id_trabajador`, `fecha_actividad`),
  KEY `idx_wpact_estado` (`estado`),
  KEY `idx_wpact_eval_kpi` (`id_evaluacion_kpi`),
  KEY `idx_wpact_archivo` (`id_archivo`),
  KEY `fk_wpact_trabajador` (`id_trabajador`),
  CONSTRAINT `fk_wpact_kpi_eval`   FOREIGN KEY (`id_evaluacion_kpi`) REFERENCES `wp_kpi_evaluaciones` (`id_evaluacion`) ON DELETE SET NULL,
  CONSTRAINT `fk_wpact_archivo`    FOREIGN KEY (`id_archivo`)        REFERENCES `wp_archivos` (`id_archivo`) ON DELETE SET NULL,
  KEY `fk_wpact_supervisor` (`revisado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `wp_actividad_historial` (
  `id_historial`    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_actividad`    BIGINT UNSIGNED NOT NULL,
  `titulo_anterior` VARCHAR(150)    NOT NULL,
  `desc_anterior`   TEXT            NOT NULL,
  `modificado_por`  INT(11)         NULL,
  `motivo_cambio`   VARCHAR(255)    NULL,
  `modificado_en`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_historial`),
  KEY `idx_wpacth_actividad` (`id_actividad`),
  CONSTRAINT `fk_wpacth_actividad` FOREIGN KEY (`id_actividad`)   REFERENCES `wp_actividades` (`id_actividad`) ON DELETE CASCADE,
  KEY `fk_wpacth_usuario` (`modificado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `wp_migraciones` (`version`, `descripcion`) VALUES ('V004', 'Actividades laborales e historial de modificaciones');
