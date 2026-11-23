USE `websiteplus`;
-- =====================================================================
-- WebSite+  |  V005  |  Gamificación, ledger de puntos, premios, canjes y ranking
-- Reglas:
--   * Libro mayor (ledger) inmutable de puntos: cada cambio es una fila con saldo previo y posterior.
--   * Prevención de saldos negativos y duplicidad de canjes.
--   * Ranking histórico congelado por periodo para auditoría y reconocimientos.
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Catálogo de premios canjeables
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_premios` (
  `id_premio`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `titulo`             VARCHAR(150) NOT NULL,
  `descripcion`        TEXT         NOT NULL,
  `puntos_requeridos`  INT UNSIGNED NOT NULL,
  `stock_disponible`   INT          NOT NULL DEFAULT 0,
  `stock_ilimitado`    TINYINT(1)   NOT NULL DEFAULT 0,
  `id_imagen`          BIGINT UNSIGNED NULL COMMENT 'Foto del premio en wp_archivos',
  `activo`             TINYINT(1)   NOT NULL DEFAULT 1,
  `fecha_inicio`       DATE         NULL,
  `fecha_fin`          DATE         NULL,
  `creado_por`         INT(11)      NULL,
  `creado_en`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_premio`),
  KEY `idx_wpprem_activo` (`activo`, `puntos_requeridos`),
  CONSTRAINT `fk_wpprem_imagen`  FOREIGN KEY (`id_imagen`)   REFERENCES `wp_archivos` (`id_archivo`) ON DELETE SET NULL,
  KEY `fk_wpprem_usuario` (`creado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Libro mayor de puntos (Ledger). Inmutable: solo INSERT.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_puntos_ledger` (
  `id_movimiento`    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_trabajador`    INT(11)         NOT NULL,
  `tipo_movimiento`  ENUM('GANADO_KPI','BONO_ADMIN','CANJE_PREMIO','AJUSTE_MANUAL','EXPIRACION') NOT NULL,
  `puntos`           INT             NOT NULL COMMENT 'Positivo para créditos, negativo para débitos',
  `saldo_anterior`   INT             NOT NULL DEFAULT 0,
  `saldo_posterior`  INT             NOT NULL DEFAULT 0,
  `referencia_tipo`  VARCHAR(40)     NULL COMMENT 'kpi_evaluacion, canje_premio, admin_ajuste',
  `referencia_id`    BIGINT UNSIGNED NULL,
  `motivo`           VARCHAR(255)    NOT NULL,
  `autorizado_por`   INT(11)         NULL COMMENT 'Usuario admin/supervisor que autorizó o NULL si fue automático',
  `creado_en`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_movimiento`),
  KEY `idx_wppun_trabajador` (`id_trabajador`, `creado_en`),
  KEY `idx_wppun_ref` (`referencia_tipo`, `referencia_id`),
  CONSTRAINT `chk_wppun_saldo_no_negativo` CHECK (`saldo_posterior` >= 0),
  KEY `fk_wppun_trabajador` (`id_trabajador`),
  KEY `fk_wppun_autoriza` (`autorizado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Canjes solicitados y procesados
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_canjes` (
  `id_canje`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `codigo_canje`     VARCHAR(30)     NOT NULL COMMENT 'Código único para validación física o entrega',
  `id_trabajador`    INT(11)         NOT NULL,
  `id_premio`        INT UNSIGNED    NOT NULL,
  `puntos_gastados`  INT UNSIGNED    NOT NULL,
  `id_movimiento`    BIGINT UNSIGNED NULL COMMENT 'Movimiento en wp_puntos_ledger que dedujo los puntos',
  `estado`           ENUM('SOLICITADO','APROBADO','ENTREGADO','RECHAZADO','CANCELADO') NOT NULL DEFAULT 'SOLICITADO',
  `motivo_rechazo`   VARCHAR(255)    NULL,
  `fecha_solicitud`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_resolucion` DATETIME        NULL,
  `resuelto_por`     INT(11)         NULL,
  PRIMARY KEY (`id_canje`),
  UNIQUE KEY `uk_wpcanje_codigo` (`codigo_canje`),
  KEY `idx_wpcanje_trabajador` (`id_trabajador`, `estado`),
  KEY `idx_wpcanje_premio` (`id_premio`),
  KEY `idx_wpcanje_estado` (`estado`, `fecha_solicitud`),
  KEY `fk_wpcanje_trabajador` (`id_trabajador`),
  CONSTRAINT `fk_wpcanje_premio`     FOREIGN KEY (`id_premio`)     REFERENCES `wp_premios` (`id_premio`),
  CONSTRAINT `fk_wpcanje_movimiento` FOREIGN KEY (`id_movimiento`) REFERENCES `wp_puntos_ledger` (`id_movimiento`) ON DELETE SET NULL,
  KEY `fk_wpcanje_resuelve` (`resuelto_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Snapshots de ranking histórico por periodo (congelados al cierre)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_ranking_historico` (
  `id_ranking`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `periodo_tipo`            ENUM('MENSUAL','TRIMESTRAL','ANUAL') NOT NULL,
  `periodo_clave`           VARCHAR(20)  NOT NULL COMMENT 'ej. 2026-10, 2026-Q4',
  `id_trabajador`           INT(11)      NOT NULL,
  `puntos_kpi_periodo`      INT UNSIGNED NOT NULL DEFAULT 0,
  `porcentaje_cumplimiento` DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  `kpis_asignados`          INT UNSIGNED NOT NULL DEFAULT 0,
  `kpis_aprobados`          INT UNSIGNED NOT NULL DEFAULT 0,
  `puesto`                  INT UNSIGNED NOT NULL,
  `medalla`                 ENUM('ORO','PLATA','BRONCE','MENCION','NINGUNA') NOT NULL DEFAULT 'NINGUNA',
  `cerrado_por`             INT(11)      NULL,
  `cerrado_en`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_ranking`),
  UNIQUE KEY `uk_wprank_periodo_trab` (`periodo_tipo`, `periodo_clave`, `id_trabajador`),
  KEY `idx_wprank_periodo_puesto` (`periodo_tipo`, `periodo_clave`, `puesto`),
  KEY `fk_wprank_trabajador` (`id_trabajador`),
  KEY `fk_wprank_cierra` (`cerrado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `wp_migraciones` (`version`, `descripcion`) VALUES ('V005', 'Gamificacion: premios, libro mayor de puntos, canjes y ranking');
