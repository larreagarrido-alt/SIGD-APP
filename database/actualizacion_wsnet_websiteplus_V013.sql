-- Actualizacion de base WebSite+ existente; requiere V011. No usar en asistencia.
USE `wsnet_websiteplus`;
-- Independiente de V012. No altera la clave ni las filas de V011.
CREATE TABLE IF NOT EXISTS wp_claves_faciales (
 id_clave BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
 id_usuario INT(11) NOT NULL,
 id_acceso_android INT UNSIGNED NOT NULL,
 id_sesion_registro BIGINT UNSIGNED NOT NULL,
 clave_publica TEXT NOT NULL,
 huella_clave CHAR(64) NOT NULL,
 credencial_hmac CHAR(64) NOT NULL,
 estado ENUM('PENDIENTE','ACTIVO','REVOCADO') NOT NULL DEFAULT 'PENDIENTE',
 creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 confirmado_en DATETIME NULL,
 revocado_en DATETIME NULL,
 activo_usuario INT(11) AS (CASE WHEN estado='ACTIVO' THEN id_usuario END) STORED,
 PRIMARY KEY(id_clave), UNIQUE KEY uk_face_key(huella_clave), UNIQUE KEY uk_face_user(activo_usuario),
 CONSTRAINT fk_face_android FOREIGN KEY(id_acceso_android) REFERENCES wp_accesos_android(id_acceso)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
INSERT IGNORE INTO wp_migraciones(version,descripcion) VALUES('V013','Clave facial separada, autorizada por V011; no habilita sesiones faciales');

UPDATE wp_migraciones SET checksum='3765c7c9b82a7998c3e832f29090f9c6243785b2901e70620b11228f0c9de4e2' WHERE version='V013';
