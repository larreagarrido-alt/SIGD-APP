USE `websiteplus`;
-- =====================================================================
-- WebSite+  |  V003  |  KPI dinámicos, criterios, asignaciones, evaluaciones,
--                       entregas, revisiones y firmas de entrega
-- Reglas:
--   * Indicadores dinámicos y versionados (los cambios no alteran evaluaciones pasadas).
--   * Cálculo de puntaje en servidor.
--   * Prevención de entregas duplicadas (UNIQUE por asignación y periodo).
--   * Integración con biometría (desafío criptográfico firmado).
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Categorías de indicadores (ej. Productividad, Calidad, Innovación, Puntualidad)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_kpi_categorias` (
  `id_categoria` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `nombre`       VARCHAR(100) NOT NULL,
  `descripcion`  VARCHAR(255) NULL,
  `icono`        VARCHAR(50)  NULL COMMENT 'Nombre del icono para la app Android',
  `orden`        INT NOT NULL DEFAULT 0,
  `activo`       TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_categoria`),
  UNIQUE KEY `uk_wpkpicat_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Definición de indicadores (plantilla dinámica versionada)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_kpi_definiciones` (
  `id_kpi`            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `codigo`            VARCHAR(50)  NOT NULL COMMENT 'Código legible, ej. KPI-PROD-01',
  `id_categoria`      INT UNSIGNED NOT NULL,
  `nombre`            VARCHAR(150) NOT NULL,
  `descripcion`       TEXT         NOT NULL,
  `version`           INT UNSIGNED NOT NULL DEFAULT 1,
  `tipo_metrica`      ENUM('NUMERICO','PORCENTAJE','BOOLEANO','ESCALA_1_5','TEXTO') NOT NULL DEFAULT 'NUMERICO',
  `unidad_medida`     VARCHAR(30)  NULL COMMENT 'ej. Tareas, %, Horas, Documentos',
  `meta_esperada`     DECIMAL(10,2) NOT NULL DEFAULT 100.00,
  `peso`              DECIMAL(5,2)  NOT NULL DEFAULT 1.00 COMMENT 'Ponderación en la puntuación global',
  `puntos_base`       INT UNSIGNED  NOT NULL DEFAULT 100 COMMENT 'Puntos máximos a otorgar al 100% de cumplimiento',
  `periodicidad`      ENUM('DIARIO','SEMANAL','MENSUAL','PERSONALIZADA') NOT NULL DEFAULT 'SEMANAL',
  `requiere_evidencia` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = adjuntar captura o documento en la entrega',
  `requiere_firma`    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = confirmar con desafío biométrico',
  `aplica_a`          ENUM('TODOS','CARGO','INDIVIDUAL') NOT NULL DEFAULT 'CARGO',
  `id_cargo_objetivo` INT(11)      NULL COMMENT 'Si aplica_a = CARGO',
  `activo`            TINYINT(1)   NOT NULL DEFAULT 1,
  `creado_por`        INT(11)      NULL,
  `creado_en`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_kpi`),
  UNIQUE KEY `uk_wpkpi_codigo_version` (`codigo`, `version`),
  KEY `idx_wpkpi_cat` (`id_categoria`),
  KEY `idx_wpkpi_cargo` (`id_cargo_objetivo`),
  KEY `idx_wpkpi_activo` (`activo`, `periodicidad`),
  CONSTRAINT `fk_wpkpi_categoria` FOREIGN KEY (`id_categoria`)      REFERENCES `wp_kpi_categorias` (`id_categoria`),
  KEY `fk_wpkpi_cargo` (`id_cargo_objetivo`),
  KEY `fk_wpkpi_usuario` (`creado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Criterios de evaluación dinámica / rúbrica por indicador
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_kpi_criterios` (
  `id_criterio`      INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_kpi`           INT UNSIGNED NOT NULL,
  `rango_min`        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `rango_max`        DECIMAL(10,2) NOT NULL DEFAULT 100.00,
  `porcentaje_puntos` DECIMAL(5,2) NOT NULL DEFAULT 100.00 COMMENT '% de puntos_base obtenidos',
  `etiqueta`         VARCHAR(50)  NOT NULL COMMENT 'ej. Deficiente, Aceptable, Excelente',
  `descripcion`      VARCHAR(255) NULL,
  PRIMARY KEY (`id_criterio`),
  KEY `idx_wpkpicrit_kpi` (`id_kpi`),
  CONSTRAINT `fk_wpkpicrit_kpi` FOREIGN KEY (`id_kpi`) REFERENCES `wp_kpi_definiciones` (`id_kpi`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Asignación de KPI a empleados individuales (matriz de exigibilidad)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_kpi_asignaciones` (
  `id_asignacion` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_trabajador` INT(11)      NOT NULL,
  `id_kpi`        INT UNSIGNED NOT NULL,
  `fecha_inicio`  DATE         NOT NULL,
  `fecha_fin`     DATE         NULL COMMENT 'NULL = asignación vigente continua',
  `activo`        TINYINT(1)   NOT NULL DEFAULT 1,
  `asignado_por`  INT(11)      NULL,
  `asignado_en`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_asignacion`),
  UNIQUE KEY `uk_wpkpiasig_trab_kpi_inicio` (`id_trabajador`, `id_kpi`, `fecha_inicio`),
  KEY `idx_wpkpiasig_trabajador` (`id_trabajador`, `activo`),
  KEY `idx_wpkpiasig_kpi` (`id_kpi`),
  KEY `fk_wpkpiasig_trabajador` (`id_trabajador`),
  CONSTRAINT `fk_wpkpiasig_kpi`        FOREIGN KEY (`id_kpi`)        REFERENCES `wp_kpi_definiciones` (`id_kpi`),
  KEY `fk_wpkpiasig_usuario` (`asignado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Evaluaciones / Instancias de entrega de KPI en un periodo concreto
-- Estados estrictos:
--   'Pendiente' -> 'En progreso' -> 'Entregado' -> 'En revisión' -> 'Aprobado' / 'Observado'
--   'Vencido' (automático si vence sin entrega)
--   'No exigible' (automático o justificado si estuvo de vacaciones/permiso aprobado)
--   'Cancelado' (por supervisor justificado)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_kpi_evaluaciones` (
  `id_evaluacion`       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_asignacion`       INT UNSIGNED NOT NULL,
  `id_trabajador`       INT(11)      NOT NULL,
  `id_kpi`              INT UNSIGNED NOT NULL,
  `periodo_tipo`        ENUM('DIARIO','SEMANAL','MENSUAL','PERSONALIZADO') NOT NULL,
  `periodo_clave`       VARCHAR(20)  NOT NULL COMMENT 'ej. 2026-W42, 2026-10, 2026-10-15',
  `fecha_limite`        DATETIME     NOT NULL,
  `estado`              ENUM('Pendiente','En progreso','Entregado','En revisión','Aprobado','Observado','Vencido','No exigible','Cancelado')
                        NOT NULL DEFAULT 'Pendiente',
  `motivo_no_exigible`  VARCHAR(255) NULL COMMENT 'ej. Permiso laboral aprobado #11, Vacaciones',
  -- Snapshot de la plantilla al momento de generar la evaluación (inmutable ante cambios de la definición)
  `meta_snapshot`       DECIMAL(10,2) NOT NULL,
  `peso_snapshot`       DECIMAL(5,2)  NOT NULL,
  `puntos_base_snapshot` INT UNSIGNED NOT NULL,
  -- Valores entregados
  `valor_reportado`     DECIMAL(10,2) NULL,
  `comentarios_entrega` TEXT          NULL,
  `fecha_primer_borrador` DATETIME    NULL,
  `fecha_entrega`       DATETIME      NULL,
  -- Calificación del servidor y supervisor
  `porcentaje_cumplimiento` DECIMAL(5,2) NULL,
  `puntos_otorgados`    INT UNSIGNED  NULL,
  `calificado_por`      INT(11)       NULL,
  `fecha_calificacion`  DATETIME      NULL,
  `observaciones_supervisor` TEXT     NULL,
  `creado_en`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_evaluacion`),
  -- PREVIENE ENTREGAS DUPLICADAS: Un solo registro por trabajador, KPI y periodo
  UNIQUE KEY `uk_wpkpieval_trab_kpi_periodo` (`id_trabajador`, `id_kpi`, `periodo_clave`),
  KEY `idx_wpkpieval_estado_fecha` (`estado`, `fecha_limite`),
  KEY `idx_wpkpieval_trabajador` (`id_trabajador`, `periodo_clave`),
  KEY `idx_wpkpieval_asignacion` (`id_asignacion`),
  CONSTRAINT `fk_wpkpieval_asig`       FOREIGN KEY (`id_asignacion`) REFERENCES `wp_kpi_asignaciones` (`id_asignacion`),
  KEY `fk_wpkpieval_trabajador` (`id_trabajador`),
  CONSTRAINT `fk_wpkpieval_kpi`        FOREIGN KEY (`id_kpi`)        REFERENCES `wp_kpi_definiciones` (`id_kpi`),
  KEY `fk_wpkpieval_califica` (`calificado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Evidencias de entrega (fotos, capturas, reportes PDF adjuntos)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_kpi_evidencias` (
  `id_evidencia`   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_evaluacion`  BIGINT UNSIGNED NOT NULL,
  `id_archivo`     BIGINT UNSIGNED NOT NULL,
  `descripcion`    VARCHAR(255)    NULL,
  `subido_en`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_evidencia`),
  KEY `idx_wpkpievid_eval` (`id_evaluacion`),
  KEY `idx_wpkpievid_arch` (`id_archivo`),
  CONSTRAINT `fk_wpkpievid_eval` FOREIGN KEY (`id_evaluacion`) REFERENCES `wp_kpi_evaluaciones` (`id_evaluacion`) ON DELETE CASCADE,
  CONSTRAINT `fk_wpkpievid_arch` FOREIGN KEY (`id_archivo`)    REFERENCES `wp_archivos` (`id_archivo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Firmas biométricas de entrega de KPI (desafío criptográfico completado)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_kpi_firmas` (
  `id_firma`        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_evaluacion`   BIGINT UNSIGNED NOT NULL,
  `id_desafio`      BIGINT UNSIGNED NOT NULL,
  `id_dispositivo`  INT UNSIGNED    NOT NULL,
  `firma_base64`    TEXT            NOT NULL COMMENT 'Firma criptográfica ECDSA SHA-256',
  `sha256_entrega`  CHAR(64)        NOT NULL COMMENT 'Digest del contenido firmado en el momento de entrega',
  `verificada`      TINYINT(1)      NOT NULL DEFAULT 1,
  `firmado_en`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_firma`),
  UNIQUE KEY `uk_wpkpifirma_eval` (`id_evaluacion`),
  KEY `idx_wpkpifirma_desafio` (`id_desafio`),
  CONSTRAINT `fk_wpkpifirma_eval`    FOREIGN KEY (`id_evaluacion`)  REFERENCES `wp_kpi_evaluaciones` (`id_evaluacion`) ON DELETE CASCADE,
  CONSTRAINT `fk_wpkpifirma_desafio` FOREIGN KEY (`id_desafio`)     REFERENCES `wp_desafios` (`id_desafio`),
  CONSTRAINT `fk_wpkpifirma_disp`    FOREIGN KEY (`id_dispositivo`) REFERENCES `wp_dispositivos` (`id_dispositivo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Historial inmutable de revisiones y transiciones de estado de la entrega
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_kpi_revisiones` (
  `id_revision`     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_evaluacion`   BIGINT UNSIGNED NOT NULL,
  `estado_anterior` VARCHAR(30)     NOT NULL,
  `estado_nuevo`    VARCHAR(30)     NOT NULL,
  `motivo`          VARCHAR(255)    NULL,
  `observacion`     TEXT            NULL,
  `id_usuario`      INT(11)         NULL,
  `creado_en`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_revision`),
  KEY `idx_wpkpirev_eval` (`id_evaluacion`, `creado_en`),
  CONSTRAINT `fk_wpkpirev_eval` FOREIGN KEY (`id_evaluacion`) REFERENCES `wp_kpi_evaluaciones` (`id_evaluacion`) ON DELETE CASCADE,
  KEY `fk_wpkpirev_user` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `wp_migraciones` (`version`, `descripcion`) VALUES ('V003', 'KPI: categorias, definiciones, criterios, asignaciones, evaluaciones, evidencias, firmas');
