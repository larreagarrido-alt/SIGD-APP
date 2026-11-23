USE `websiteplus`;
-- =====================================================================
-- WebSite+  |  V002  |  Relacion laboral: periodos, situacion complementaria, eventos
-- La fuente OFICIAL sigue siendo `trabajadores` (estado Activo/Inactivo, tipo, cargo).
-- Estas tablas solo COMPLEMENTAN: historial de periodos, desvinculacion definitiva y
-- registro de transiciones. Nunca se escribe en `trabajadores`.
-- =====================================================================
SET NAMES utf8mb4;

-- Periodos laborales: una persona (mismo id_trabajador) puede tener varios periodos:
-- practicas 2026-I, pausa, practicas 2026-II, colaborador 2027...
CREATE TABLE IF NOT EXISTS `wp_periodos_laborales` (
  `id_periodo`      INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_trabajador`   INT(11)      NOT NULL,
  `tipo`            ENUM('Practicante','Colaborador','Sin tipo') NOT NULL
                    COMMENT '"Sin tipo" cubre registros antiguos con tipo vacio',
  `id_cargo`        INT(11)      NULL,
  `id_agencia`      INT(11)      NULL,
  `fecha_inicio`    DATE         NOT NULL,
  `fecha_fin`       DATE         NULL COMMENT 'NULL = periodo abierto (vigente)',
  `motivo_inicio`   ENUM('INGRESO','REINGRESO','CONVERSION_TIPO','CAMBIO_CARGO','CARGA_INICIAL') NOT NULL,
  `motivo_fin`      ENUM('INACTIVACION','DESVINCULACION','CONVERSION_TIPO','CAMBIO_CARGO','FIN_PRACTICAS','FIN_CONTRATO','CARGA_INICIAL') NULL,
  `registrado_por`  INT(11)      NULL COMMENT 'NULL = proceso automatico de sincronizacion',
  `creado_en`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- Solo UN periodo abierto por trabajador (los NULL no chocan en indices UNIQUE).
  `abierto_por_trabajador` INT(11) AS (CASE WHEN `fecha_fin` IS NULL THEN `id_trabajador` END) STORED,
  PRIMARY KEY (`id_periodo`),
  UNIQUE KEY `uk_wpper_un_abierto` (`abierto_por_trabajador`),
  KEY `idx_wpper_trabajador` (`id_trabajador`, `fecha_inicio`),
  KEY `idx_wpper_fechas` (`fecha_inicio`, `fecha_fin`),
  CONSTRAINT `chk_wpper_fechas` CHECK (`fecha_fin` IS NULL OR `fecha_fin` >= `fecha_inicio`),
  KEY `fk_wpper_trabajador` (`id_trabajador`),
  KEY `fk_wpper_cargo` (`id_cargo`),
  KEY `fk_wpper_agencia` (`id_agencia`),
  KEY `fk_wpper_usuario` (`registrado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Situacion complementaria (1 fila por trabajador). Distingue "inactivo temporal" de
-- "desvinculado definitivo", algo que el sistema antiguo no puede expresar.
-- Ademas guarda la ultima "foto" vista de trabajadores para detectar cambios hechos
-- desde el sistema de asistencia (sincronizacion).
CREATE TABLE IF NOT EXISTS `wp_situacion_laboral` (
  `id_trabajador`        INT(11)      NOT NULL,
  `situacion`            ENUM('VIGENTE','DESVINCULADO') NOT NULL DEFAULT 'VIGENTE',
  `desvinculado_en`      DATE         NULL,
  `motivo_situacion`     VARCHAR(500) NULL,
  `ultimo_estado_visto`  ENUM('Activo','Inactivo') NOT NULL,
  `ultimo_tipo_visto`    VARCHAR(20)  NOT NULL,
  `ultimo_cargo_visto`   INT(11)      NULL,
  `ultima_agencia_vista` INT(11)      NULL,
  `sincronizado_en`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_por`      INT(11)      NULL,
  `actualizado_en`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_trabajador`),
  KEY `idx_wpsit_situacion` (`situacion`),
  KEY `fk_wpsit_trabajador` (`id_trabajador`),
  KEY `fk_wpsit_usuario` (`actualizado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Bitacora de transiciones laborales (fecha, motivo, responsable). Solo INSERT.
CREATE TABLE IF NOT EXISTS `wp_eventos_laborales` (
  `id_evento`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_trabajador`          INT(11)      NOT NULL,
  `tipo_evento`            ENUM('CARGA_INICIAL','INACTIVACION','REACTIVACION','REINGRESO','DESVINCULACION',
                                'CONVERSION_TIPO','CAMBIO_CARGO','CAMBIO_AGENCIA') NOT NULL,
  `valor_anterior`         VARCHAR(100) NULL,
  `valor_nuevo`            VARCHAR(100) NULL,
  `fecha_efectiva`         DATE         NOT NULL,
  `motivo`                 VARCHAR(500) NULL,
  `origen`                 ENUM('API','SINCRONIZACION') NOT NULL,
  `id_periodo`             INT UNSIGNED NULL,
  `id_usuario_responsable` INT(11)      NULL,
  `creado_en`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_evento`),
  KEY `idx_wpevl_trabajador` (`id_trabajador`, `creado_en`),
  KEY `idx_wpevl_tipo` (`tipo_evento`, `fecha_efectiva`),
  KEY `fk_wpevl_trabajador` (`id_trabajador`),
  CONSTRAINT `fk_wpevl_periodo`    FOREIGN KEY (`id_periodo`)             REFERENCES `wp_periodos_laborales` (`id_periodo`),
  KEY `fk_wpevl_usuario` (`id_usuario_responsable`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `wp_migraciones` (`version`, `descripcion`) VALUES ('V002', 'Relacion laboral: periodos, situacion, eventos');
