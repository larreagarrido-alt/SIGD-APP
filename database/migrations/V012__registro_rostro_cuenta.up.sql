-- Perfil capturado por WebSite+. No modifica trabajadores ni activa sesiones faciales.
CREATE TABLE IF NOT EXISTS wp_rostros_cuenta (
  id_rostro BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_usuario INT(11) NOT NULL,
  id_acceso_android INT UNSIGNED NOT NULL,
  id_sesion_registro BIGINT UNSIGNED NOT NULL,
  modelo VARCHAR(60) NOT NULL,
  muestras TINYINT UNSIGNED NOT NULL,
  perfil_cifrado MEDIUMTEXT NOT NULL COMMENT 'AES-256-GCM; nunca imagen ni descriptor legible',
  iv_base64 VARCHAR(24) NOT NULL,
  tag_base64 VARCHAR(32) NOT NULL,
  estado ENUM('PENDIENTE_VALIDACION','REVOCADO') NOT NULL DEFAULT 'PENDIENTE_VALIDACION',
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_rostro),
  UNIQUE KEY uk_wprost_usuario (id_usuario),
  CONSTRAINT fk_wprost_android FOREIGN KEY (id_acceso_android) REFERENCES wp_accesos_android(id_acceso),
  CONSTRAINT fk_wprost_sesion FOREIGN KEY (id_sesion_registro) REFERENCES wp_sesiones(id_sesion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
INSERT IGNORE INTO wp_migraciones (version,descripcion) VALUES ('V012','Registro propio de rostro cifrado por cuenta; acceso facial bloqueado');
