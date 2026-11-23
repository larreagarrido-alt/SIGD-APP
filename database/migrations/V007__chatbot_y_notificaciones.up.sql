USE `websiteplus`;
-- =====================================================================
-- WebSite+  |  V007  |  Chatbot con IA (estructura segura) y Notificaciones
-- Reglas:
--   * Secretos y API Keys de IA residen en el servidor (config.php), jamás en Android.
--   * Chatbot sin acceso a tablas de psicología ni contraseñas.
--   * Notificaciones persistentes con soporte para Push FCM futuro.
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Conversaciones del Chatbot
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_chatbot_conversaciones` (
  `id_conversacion` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_usuario`      INT(11)         NOT NULL,
  `titulo`          VARCHAR(150)    NOT NULL DEFAULT 'Nueva consulta',
  `contexto_modulo` ENUM('GENERAL','KPI','BIENESTAR_AYUDA','PREMIOS') NOT NULL DEFAULT 'GENERAL',
  `total_mensajes`  INT UNSIGNED    NOT NULL DEFAULT 0,
  `activo`          TINYINT(1)      NOT NULL DEFAULT 1,
  `creado_en`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_conversacion`),
  KEY `idx_wpchat_usuario` (`id_usuario`, `creado_en`),
  KEY `fk_wpchat_usuario` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Mensajes del Chatbot
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_chatbot_mensajes` (
  `id_mensaje`      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_conversacion` BIGINT UNSIGNED NOT NULL,
  `rol`             ENUM('USUARIO','ASISTENTE','SISTEMA') NOT NULL,
  `contenido`       TEXT            NOT NULL,
  `tokens_aprox`    INT UNSIGNED    NULL,
  `error_proveedor` VARCHAR(255)    NULL COMMENT 'Si la llamada a la IA fallo',
  `creado_en`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_mensaje`),
  KEY `idx_wpchatm_conv` (`id_conversacion`, `creado_en`),
  CONSTRAINT `fk_wpchatm_conv` FOREIGN KEY (`id_conversacion`) REFERENCES `wp_chatbot_conversaciones` (`id_conversacion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Cuotas y límites de uso del Chatbot (anti-abuso / control de costos)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_chatbot_cuotas` (
  `id_cuota`       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_usuario`     INT(11)         NOT NULL,
  `fecha`          DATE            NOT NULL,
  `mensajes_hoy`   INT UNSIGNED    NOT NULL DEFAULT 0,
  `tokens_hoy`     INT UNSIGNED    NOT NULL DEFAULT 0,
  `limite_alcanzado` TINYINT(1)    NOT NULL DEFAULT 0,
  PRIMARY KEY (`id_cuota`),
  UNIQUE KEY `uk_wpchatq_usuario_fecha` (`id_usuario`, `fecha`),
  KEY `fk_wpchatq_usuario` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Notificaciones en la App WebSite+ (separadas de las de asistencia antigua)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_notificaciones` (
  `id_notificacion` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_trabajador`   INT(11)         NOT NULL,
  `tipo`            ENUM('KPI_POR_VENCER','KPI_VENCIDO','KPI_REVISADO','PREMIO_DISPONIBLE','CANJE_ACTUALIZADO','AVISO_GENERAL') NOT NULL,
  `titulo`          VARCHAR(150)    NOT NULL,
  `mensaje`         VARCHAR(350)    NOT NULL,
  `referencia_tipo` VARCHAR(40)     NULL COMMENT 'kpi_evaluacion, canje, premio',
  `referencia_id`   BIGINT UNSIGNED NULL,
  `leida`           TINYINT(1)      NOT NULL DEFAULT 0,
  `fecha_leida`     DATETIME        NULL,
  `enviada_push`    TINYINT(1)      NOT NULL DEFAULT 0,
  `creado_en`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_notificacion`),
  KEY `idx_wpnot_trab_leida` (`id_trabajador`, `leida`, `creado_en`),
  KEY `idx_wpnot_tipo` (`tipo`),
  KEY `fk_wpnot_trabajador` (`id_trabajador`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `wp_migraciones` (`version`, `descripcion`) VALUES ('V007', 'Chatbot con IA, cuotas y sistema de notificaciones');
