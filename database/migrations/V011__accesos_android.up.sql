USE `websiteplus`;
-- Credenciales para iniciar sesion desde un celular personal. NO guarda huellas.
-- Independiente de wp_dispositivos: este registro no autoriza firmas KPI.
CREATE TABLE IF NOT EXISTS wp_accesos_android (
  id_acceso INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_usuario INT(11) NOT NULL,
  device_uid VARCHAR(64) NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  clave_publica TEXT NOT NULL COMMENT 'EC P-256 SubjectPublicKeyInfo PEM',
  huella_clave CHAR(64) NOT NULL COMMENT 'SHA256 de la clave publica DER; no es una huella digital',
  credencial_hmac CHAR(64) NOT NULL,
  id_sesion_registro BIGINT UNSIGNED NOT NULL,
  estado ENUM('PENDIENTE','ACTIVO','REVOCADO') NOT NULL DEFAULT 'PENDIENTE',
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  verificado_en DATETIME NULL,
  ultimo_uso_en DATETIME NULL,
  revocado_en DATETIME NULL,
  activo_usuario INT(11) AS (CASE WHEN estado='ACTIVO' THEN id_usuario END) STORED,
  activo_celular VARCHAR(64) AS (CASE WHEN estado='ACTIVO' THEN device_uid END) STORED,
  PRIMARY KEY (id_acceso),
  UNIQUE KEY uk_wpandroid_usuario (activo_usuario),
  UNIQUE KEY uk_wpandroid_celular (activo_celular),
  UNIQUE KEY uk_wpandroid_clave (huella_clave),
  KEY idx_wpandroid_cuenta (id_usuario, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

ALTER TABLE wp_sesiones ADD COLUMN id_acceso_android INT UNSIGNED NULL,
  ADD KEY idx_wpses_android (id_acceso_android),
  ADD CONSTRAINT fk_wpses_android FOREIGN KEY (id_acceso_android) REFERENCES wp_accesos_android(id_acceso);

INSERT IGNORE INTO wp_permisos (codigo,modulo,descripcion,es_sensible)
VALUES ('auth.dispositivos.recuperar','auth','Revocar un celular perdido tras verificar la identidad del titular',1);
-- No concede este permiso automaticamente a ningun rol.
INSERT IGNORE INTO wp_migraciones (version,descripcion) VALUES ('V011','Acceso Android: una cuenta por celular y una clave activa por cuenta');
