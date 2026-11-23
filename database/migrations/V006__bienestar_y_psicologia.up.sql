USE `websiteplus`;
-- =====================================================================
-- WebSite+  |  V006  |  Test psicológico, bienestar y salud ocupacional
-- REGLAS ESTRICTAS DE PRIVACIDAD:
--   * Respuestas individuales protegidas (solo accesibles con permiso sensible 'bienestar.ver_privado').
--   * NUNCA afectan el puntaje de KPI ni el ranking.
--   * No se exponen a administradores generales sin justificación clínica/ocupacional.
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Plantillas de cuestionarios de bienestar (ej. Clima Laboral, Burnout, Estrés)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_bienestar_tests` (
  `id_test`        INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `codigo`         VARCHAR(50)  NOT NULL,
  `titulo`         VARCHAR(150) NOT NULL,
  `descripcion`    TEXT         NOT NULL,
  `version`        INT UNSIGNED NOT NULL DEFAULT 1,
  `tipo`           ENUM('CLIMA_LABORAL','ESTRES','SATISFACCION','SALUD_MENTAL','GENERAL') NOT NULL DEFAULT 'GENERAL',
  `periodicidad`   ENUM('MENSUAL','TRIMESTRAL','SEMESTRAL','LIBRE') NOT NULL DEFAULT 'MENSUAL',
  `activo`         TINYINT(1)   NOT NULL DEFAULT 1,
  `creado_por`     INT(11)      NULL,
  `creado_en`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_test`),
  UNIQUE KEY `uk_wpbtest_codigo_version` (`codigo`, `version`),
  KEY `idx_wpbtest_activo` (`activo`),
  KEY `fk_wpbtest_usuario` (`creado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Preguntas del test
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_bienestar_preguntas` (
  `id_pregunta`    INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_test`        INT UNSIGNED NOT NULL,
  `orden`          INT          NOT NULL DEFAULT 1,
  `enunciado`      TEXT         NOT NULL,
  `tipo_respuesta` ENUM('ESCALA_1_5','SI_NO','OPCION_MULTIPLE','TEXTO_LIBRE') NOT NULL DEFAULT 'ESCALA_1_5',
  `es_inversa`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = puntaje invertido para métricas de riesgo',
  PRIMARY KEY (`id_pregunta`),
  KEY `idx_wpbpreg_test` (`id_test`, `orden`),
  CONSTRAINT `fk_wpbpreg_test` FOREIGN KEY (`id_test`) REFERENCES `wp_bienestar_tests` (`id_test`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Opciones para preguntas de opción múltiple
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_bienestar_opciones` (
  `id_opcion`   INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_pregunta` INT UNSIGNED NOT NULL,
  `orden`       INT          NOT NULL DEFAULT 1,
  `texto`       VARCHAR(200) NOT NULL,
  `valor_peso`  DECIMAL(5,2) NOT NULL DEFAULT 1.00,
  PRIMARY KEY (`id_opcion`),
  KEY `idx_wpbopc_pregunta` (`id_pregunta`),
  CONSTRAINT `fk_wpbopc_pregunta` FOREIGN KEY (`id_pregunta`) REFERENCES `wp_bienestar_preguntas` (`id_pregunta`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Evaluaciones de bienestar aplicadas al trabajador
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_bienestar_evaluaciones` (
  `id_evaluacion_bienestar` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_test`                 INT UNSIGNED NOT NULL,
  `id_trabajador`           INT(11)      NOT NULL,
  `periodo_clave`           VARCHAR(20)  NOT NULL COMMENT 'ej. 2026-10',
  `estado`                  ENUM('PENDIENTE','EN_PROCESO','COMPLETADA','EXPIRADA') NOT NULL DEFAULT 'PENDIENTE',
  `fecha_completada`        DATETIME     NULL,
  -- Puntaje agregado e índice de riesgo general (para semáforo de RRHH/Salud Ocupacional)
  `puntaje_total`           DECIMAL(6,2) NULL,
  `nivel_alerta`            ENUM('OPTIMO','NORMAL','ATENCION','CRITICO') NULL,
  `recomendacion_sistema`   TEXT         NULL,
  `creado_en`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_evaluacion_bienestar`),
  UNIQUE KEY `uk_wpbeval_trab_test_periodo` (`id_trabajador`, `id_test`, `periodo_clave`),
  KEY `idx_wpbeval_test` (`id_test`),
  KEY `idx_wpbeval_alerta` (`nivel_alerta`),
  CONSTRAINT `fk_wpbeval_test`       FOREIGN KEY (`id_test`)       REFERENCES `wp_bienestar_tests` (`id_test`),
  KEY `fk_wpbeval_trabajador` (`id_trabajador`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Respuestas individuales PRIVADAS del trabajador
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_bienestar_respuestas` (
  `id_respuesta`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_evaluacion_bienestar` BIGINT UNSIGNED NOT NULL,
  `id_pregunta`             INT UNSIGNED NOT NULL,
  `valor_numerico`          DECIMAL(5,2) NULL,
  `id_opcion_seleccionada`  INT UNSIGNED NULL,
  `texto_respuesta`         TEXT         NULL,
  `respondido_en`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_respuesta`),
  UNIQUE KEY `uk_wpbresp_eval_preg` (`id_evaluacion_bienestar`, `id_pregunta`),
  KEY `idx_wpbresp_pregunta` (`id_pregunta`),
  CONSTRAINT `fk_wpbresp_eval`   FOREIGN KEY (`id_evaluacion_bienestar`) REFERENCES `wp_bienestar_evaluaciones` (`id_evaluacion_bienestar`) ON DELETE CASCADE,
  CONSTRAINT `fk_wpbresp_preg`   FOREIGN KEY (`id_pregunta`)             REFERENCES `wp_bienestar_preguntas` (`id_pregunta`),
  CONSTRAINT `fk_wpbresp_opcion` FOREIGN KEY (`id_opcion_seleccionada`) REFERENCES `wp_bienestar_opciones` (`id_opcion`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Respuestas confidenciales de salud y bienestar';

INSERT IGNORE INTO `wp_migraciones` (`version`, `descripcion`) VALUES ('V006', 'Bienestar: cuestionarios, preguntas, evaluaciones confidenciales y respuestas protegidas');
