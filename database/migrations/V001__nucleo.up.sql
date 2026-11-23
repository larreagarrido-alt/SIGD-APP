USE `websiteplus`;
-- =====================================================================
-- WebSite+  |  V001  |  Nucleo: migraciones, configuracion, permisos,
--                       dispositivos, sesiones, desafios, auditoria, archivos
-- Motor objetivo: MariaDB 10.4+ (local XAMPP) / 10.6 (cPanel)
-- Reglas:
--   * Solo CREA tablas nuevas con prefijo wp_. NO altera tablas existentes.
--   * Idempotente: se puede ejecutar varias veces sin error ni duplicados.
--   * Tipos de FK identicos a los del sistema de asistencia: INT(11) con signo.
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Control de versiones del esquema WebSite+
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_migraciones` (
  `version`      VARCHAR(10)  NOT NULL,
  `descripcion`  VARCHAR(150) NOT NULL,
  `checksum`     CHAR(64)     NULL COMMENT 'SHA-256 del archivo .up.sql (lo registra bin/migrate.php)',
  `aplicada_en`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Versiones de esquema WebSite+ aplicadas';

-- ---------------------------------------------------------------------
-- Configuracion de negocio (clave/valor). Sin secretos: los secretos van en config.php.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_configuracion` (
  `clave`           VARCHAR(80)  NOT NULL,
  `valor`           VARCHAR(500) NOT NULL,
  `tipo`            ENUM('ENTERO','DECIMAL','BOOLEANO','TEXTO','LISTA') NOT NULL DEFAULT 'TEXTO',
  `descripcion`     VARCHAR(255) NULL,
  `actualizado_por` INT(11)      NULL,
  `actualizado_en`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`clave`),
  KEY `fk_wpcfg_usuario` (`actualizado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Permisos propios de WebSite+ (no se mezclan con `permisos` del sistema de asistencia,
-- que representan vistas web antiguas). Los ROLES si se reutilizan (`roles`).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_permisos` (
  `id_permiso`  INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `codigo`      VARCHAR(80)  NOT NULL COMMENT 'modulo.accion, ej. kpi.revisar',
  `modulo`      VARCHAR(40)  NOT NULL,
  `descripcion` VARCHAR(255) NOT NULL,
  `es_sensible` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = acceso a datos privados; se audita cada uso',
  PRIMARY KEY (`id_permiso`),
  UNIQUE KEY `uk_wpperm_codigo` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `wp_rol_permisos` (
  `id_rol`     INT(11)      NOT NULL,
  `id_permiso` INT UNSIGNED NOT NULL,
  `alcance`    ENUM('PROPIO','EQUIPO','AGENCIA','TODOS') NOT NULL DEFAULT 'PROPIO'
               COMMENT 'PROPIO=sus datos; EQUIPO=trabajadores que supervisa; AGENCIA=su agencia; TODOS=empresa',
  PRIMARY KEY (`id_rol`, `id_permiso`),
  KEY `idx_wprp_permiso` (`id_permiso`),
  KEY `fk_wprp_rol` (`id_rol`),
  CONSTRAINT `fk_wprp_permiso` FOREIGN KEY (`id_permiso`) REFERENCES `wp_permisos` (`id_permiso`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `wp_usuario_permisos` (
  `id_usuario`   INT(11)      NOT NULL,
  `id_permiso`   INT UNSIGNED NOT NULL,
  `efecto`       ENUM('PERMITIR','DENEGAR') NOT NULL DEFAULT 'PERMITIR',
  `alcance`      ENUM('PROPIO','EQUIPO','AGENCIA','TODOS') NOT NULL DEFAULT 'PROPIO',
  `motivo`       VARCHAR(255) NOT NULL,
  `otorgado_por` INT(11)      NULL,
  `otorgado_en`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_usuario`, `id_permiso`),
  KEY `idx_wpup_permiso` (`id_permiso`),
  KEY `fk_wpup_usuario` (`id_usuario`),
  CONSTRAINT `fk_wpup_permiso` FOREIGN KEY (`id_permiso`)   REFERENCES `wp_permisos` (`id_permiso`) ON DELETE CASCADE,
  KEY `fk_wpup_otorga` (`otorgado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Excepciones por usuario sobre los permisos de su rol';

-- ---------------------------------------------------------------------
-- Archivos subidos (evidencias, imagenes de premios, reportes). El binario vive FUERA de public_html.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_archivos` (
  `id_archivo`      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `uuid`            CHAR(36)        NOT NULL,
  `ruta_relativa`   VARCHAR(255)    NOT NULL COMMENT 'Relativa a storage/ (nunca URL publica)',
  `nombre_original` VARCHAR(255)    NOT NULL,
  `mime`            VARCHAR(100)    NOT NULL,
  `tamano_bytes`    INT UNSIGNED    NOT NULL,
  `sha256`          CHAR(64)        NOT NULL,
  `subido_por`      INT(11)         NULL,
  `creado_en`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `eliminado_en`    DATETIME        NULL COMMENT 'Borrado logico',
  PRIMARY KEY (`id_archivo`),
  UNIQUE KEY `uk_wparch_uuid` (`uuid`),
  KEY `idx_wparch_sha` (`sha256`),
  KEY `fk_wparch_usuario` (`subido_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Dispositivos autorizados (clave publica del Android Keystore; NUNCA huellas ni rostros)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_dispositivos` (
  `id_dispositivo`    INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_usuario`        INT(11)      NOT NULL,
  `device_uid`        VARCHAR(64)  NOT NULL COMMENT 'UUID generado por la app en la instalacion',
  `nombre`            VARCHAR(100) NOT NULL,
  `plataforma`        ENUM('ANDROID','IOS','WEB') NOT NULL DEFAULT 'ANDROID',
  `modelo`            VARCHAR(100) NULL,
  `clave_publica`     TEXT         NOT NULL COMMENT 'PEM SubjectPublicKeyInfo (EC P-256)',
  `huella_clave`      CHAR(64)     NOT NULL COMMENT 'SHA-256 del DER de la clave publica',
  `algoritmo`         ENUM('ES256') NOT NULL DEFAULT 'ES256',
  `estado`            ENUM('PENDIENTE','ACTIVO','REVOCADO','RECHAZADO') NOT NULL DEFAULT 'PENDIENTE',
  `push_token`        VARCHAR(255) NULL COMMENT 'Token FCM (futuro)',
  `aprobado_por`      INT(11)      NULL,
  `aprobado_en`       DATETIME     NULL,
  `revocado_por`      INT(11)      NULL,
  `revocado_en`       DATETIME     NULL,
  `motivo_revocacion` VARCHAR(255) NULL,
  `ultimo_uso_en`     DATETIME     NULL,
  `creado_en`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- Garantiza UN solo dispositivo ACTIVO por usuario (cambio de celular = revocar + aprobar otro).
  `activo_por_usuario` INT(11) AS (CASE WHEN `estado` = 'ACTIVO' THEN `id_usuario` END) STORED,
  PRIMARY KEY (`id_dispositivo`),
  UNIQUE KEY `uk_wpdisp_usuario_uid` (`id_usuario`, `device_uid`),
  UNIQUE KEY `uk_wpdisp_huella` (`huella_clave`),
  UNIQUE KEY `uk_wpdisp_un_activo` (`activo_por_usuario`),
  KEY `idx_wpdisp_estado` (`estado`, `creado_en`),
  KEY `fk_wpdisp_usuario` (`id_usuario`),
  KEY `fk_wpdisp_aprueba` (`aprobado_por`),
  KEY `fk_wpdisp_revoca` (`revocado_por`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Sesiones de la API (Android hoy, web manana). Solo se guarda el HASH de los tokens.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_sesiones` (
  `id_sesion`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_usuario`            INT(11)      NOT NULL,
  `id_dispositivo`        INT UNSIGNED NULL,
  `metodo`                ENUM('PASSWORD','BIOMETRIA') NOT NULL,
  `cliente`               ENUM('ANDROID','WEB','PRUEBAS') NOT NULL DEFAULT 'ANDROID',
  `access_hash`           CHAR(64)     NOT NULL,
  `access_expira_en`      DATETIME     NOT NULL,
  `refresh_hash`          CHAR(64)     NOT NULL,
  `refresh_anterior_hash` CHAR(64)     NULL COMMENT 'Para detectar reutilizacion de un refresh ya rotado',
  `refresh_expira_en`     DATETIME     NOT NULL,
  `huella_credencial`     CHAR(64)     NOT NULL COMMENT 'HMAC del hash de contrasena vigente al iniciar sesion',
  `ip`                    VARCHAR(45)  NOT NULL,
  `user_agent`            VARCHAR(255) NULL,
  `creada_en`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ultimo_uso_en`         DATETIME     NULL,
  `revocada_en`           DATETIME     NULL,
  `motivo_revocacion`     VARCHAR(40)  NULL,
  PRIMARY KEY (`id_sesion`),
  UNIQUE KEY `uk_wpses_access` (`access_hash`),
  UNIQUE KEY `uk_wpses_refresh` (`refresh_hash`),
  KEY `idx_wpses_refresh_ant` (`refresh_anterior_hash`),
  KEY `idx_wpses_usuario` (`id_usuario`, `revocada_en`),
  KEY `idx_wpses_expira` (`refresh_expira_en`),
  KEY `fk_wpses_usuario` (`id_usuario`),
  CONSTRAINT `fk_wpses_dispositivo` FOREIGN KEY (`id_dispositivo`) REFERENCES `wp_dispositivos` (`id_dispositivo`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Desafios criptograficos de un solo uso (login biometrico, registro de dispositivo, entrega KPI)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_desafios` (
  `id_desafio`      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_usuario`      INT(11)      NOT NULL,
  `id_dispositivo`  INT UNSIGNED NULL,
  `proposito`       ENUM('LOGIN','REGISTRO_DISPOSITIVO','ENTREGA_KPI') NOT NULL,
  `referencia_tipo` VARCHAR(30)  NULL COMMENT 'ej. kpi_evaluacion',
  `referencia_id`   BIGINT UNSIGNED NULL,
  `nonce`           CHAR(43)     NOT NULL COMMENT '32 bytes aleatorios en base64url',
  `hash_contenido`  CHAR(64)     NULL COMMENT 'SHA-256 del contenido que se firma (respuestas KPI)',
  `mensaje`         VARCHAR(600) NOT NULL COMMENT 'Texto EXACTO que el dispositivo debe firmar',
  `expira_en`       DATETIME     NOT NULL,
  `usado_en`        DATETIME     NULL,
  `resultado`       ENUM('PENDIENTE','VALIDO','FIRMA_INVALIDA','VENCIDO') NOT NULL DEFAULT 'PENDIENTE',
  `ip`              VARCHAR(45)  NOT NULL,
  `creado_en`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_desafio`),
  UNIQUE KEY `uk_wpdes_nonce` (`nonce`),
  KEY `idx_wpdes_usuario` (`id_usuario`, `proposito`, `creado_en`),
  KEY `idx_wpdes_expira` (`expira_en`),
  KEY `fk_wpdes_usuario` (`id_usuario`),
  CONSTRAINT `fk_wpdes_dispositivo` FOREIGN KEY (`id_dispositivo`) REFERENCES `wp_dispositivos` (`id_dispositivo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Intentos de inicio de sesion (limitador contra fuerza bruta). Retencion: 90 dias.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_intentos_login` (
  `id_intento` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(50) NOT NULL,
  `ip`         VARCHAR(45) NOT NULL,
  `exito`      TINYINT(1)  NOT NULL,
  `motivo`     VARCHAR(40) NULL,
  `creado_en`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_intento`),
  KEY `idx_wpint_usuario` (`username`, `creado_en`),
  KEY `idx_wpint_ip` (`ip`, `creado_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- Auditoria de WebSite+ (inmutable; sin FK para sobrevivir a bajas). Sin contrasenas, tokens
-- ni respuestas psicologicas.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wp_auditoria` (
  `id_auditoria`   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `fecha_hora`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `id_solicitud`   CHAR(16)     NULL COMMENT 'Correlaciona con la respuesta HTTP',
  `id_usuario`     INT(11)      NULL,
  `username`       VARCHAR(50)  NULL,
  `id_trabajador_afectado` INT(11) NULL,
  `ip`             VARCHAR(45)  NOT NULL,
  `modulo`         VARCHAR(40)  NOT NULL,
  `accion`         VARCHAR(60)  NOT NULL,
  `recurso`        VARCHAR(60)  NULL,
  `id_recurso`     BIGINT       NULL,
  `resultado`      ENUM('EXITO','FALLO','DENEGADO') NOT NULL DEFAULT 'EXITO',
  `detalles`       LONGTEXT     NULL CHECK (`detalles` IS NULL OR JSON_VALID(`detalles`)),
  PRIMARY KEY (`id_auditoria`),
  KEY `idx_wpaud_fecha` (`fecha_hora`),
  KEY `idx_wpaud_usuario` (`id_usuario`, `fecha_hora`),
  KEY `idx_wpaud_afectado` (`id_trabajador_afectado`, `fecha_hora`),
  KEY `idx_wpaud_accion` (`modulo`, `accion`, `fecha_hora`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `wp_migraciones` (`version`, `descripcion`) VALUES ('V001', 'Nucleo: acceso, dispositivos, sesiones, auditoria');
