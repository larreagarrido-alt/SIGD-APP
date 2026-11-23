-- SOLO actualizacion V011 a V014 de WebSite+. No instala V012 ni toca asistencia.
-- Respalda antes de importar. DDL no es una transaccion reversible.
USE `wsnet_websiteplus`;
SET @wsp_face_upgrade_ready = 0;
SET @wsp_face_upgrade_ready = (DATABASE()='wsnet_websiteplus' AND EXISTS (SELECT 1 FROM wp_migraciones WHERE version='V011') AND NOT EXISTS (SELECT 1 FROM wp_migraciones WHERE CAST(SUBSTRING(version,2) AS UNSIGNED)>14));
SELECT @wsp_face_upgrade_ready AS preparado_para_actualizar;

-- V013
SET @wsp_face_upgrade_sql = IF(@wsp_face_upgrade_ready=1,'CREATE TABLE IF NOT EXISTS wp_claves_faciales (
 id_clave BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
 id_usuario INT(11) NOT NULL,
 id_acceso_android INT UNSIGNED NOT NULL,
 id_sesion_registro BIGINT UNSIGNED NOT NULL,
 clave_publica TEXT NOT NULL,
 huella_clave CHAR(64) NOT NULL,
 credencial_hmac CHAR(64) NOT NULL,
 estado ENUM(''PENDIENTE'',''ACTIVO'',''REVOCADO'') NOT NULL DEFAULT ''PENDIENTE'',
 creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 confirmado_en DATETIME NULL,
 revocado_en DATETIME NULL,
 activo_usuario INT(11) AS (CASE WHEN estado=''ACTIVO'' THEN id_usuario END) STORED,
 PRIMARY KEY(id_clave), UNIQUE KEY uk_face_key(huella_clave), UNIQUE KEY uk_face_user(activo_usuario),
 CONSTRAINT fk_face_android FOREIGN KEY(id_acceso_android) REFERENCES wp_accesos_android(id_acceso)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci','SELECT ''CANCELADO requiere V011 y ninguna version posterior a V014'' AS resultado');
PREPARE wsp_face_upgrade_stmt FROM @wsp_face_upgrade_sql;
EXECUTE wsp_face_upgrade_stmt;
DEALLOCATE PREPARE wsp_face_upgrade_stmt;
SET @wsp_face_upgrade_sql = IF(@wsp_face_upgrade_ready=1,'INSERT IGNORE INTO wp_migraciones(version,descripcion) VALUES(''V013'',''Clave facial separada, autorizada por V011; no habilita sesiones faciales'')','SELECT ''CANCELADO requiere V011 y ninguna version posterior a V014'' AS resultado');
PREPARE wsp_face_upgrade_stmt FROM @wsp_face_upgrade_sql;
EXECUTE wsp_face_upgrade_stmt;
DEALLOCATE PREPARE wsp_face_upgrade_stmt;
SET @wsp_face_upgrade_sql = IF(@wsp_face_upgrade_ready=1,'UPDATE wp_migraciones SET checksum=''3765c7c9b82a7998c3e832f29090f9c6243785b2901e70620b11228f0c9de4e2'' WHERE version=''V013''','SELECT ''CANCELADO requiere V011 y ninguna version posterior a V014'' AS resultado');
PREPARE wsp_face_upgrade_stmt FROM @wsp_face_upgrade_sql;
EXECUTE wsp_face_upgrade_stmt;
DEALLOCATE PREPARE wsp_face_upgrade_stmt;

-- V014
SET @wsp_face_upgrade_sql = IF(@wsp_face_upgrade_ready=1,'ALTER TABLE wp_sesiones MODIFY COLUMN metodo ENUM(''PASSWORD'',''BIOMETRIA'',''FACIAL'') NOT NULL','SELECT ''CANCELADO requiere V011 y ninguna version posterior a V014'' AS resultado');
PREPARE wsp_face_upgrade_stmt FROM @wsp_face_upgrade_sql;
EXECUTE wsp_face_upgrade_stmt;
DEALLOCATE PREPARE wsp_face_upgrade_stmt;
SET @wsp_face_upgrade_sql = IF(@wsp_face_upgrade_ready=1,'INSERT IGNORE INTO wp_migraciones(version,descripcion) VALUES(''V014'',''Sesiones FACIAL con identidad 1:1 y PAD local firmado; no PAD de servidor'')','SELECT ''CANCELADO requiere V011 y ninguna version posterior a V014'' AS resultado');
PREPARE wsp_face_upgrade_stmt FROM @wsp_face_upgrade_sql;
EXECUTE wsp_face_upgrade_stmt;
DEALLOCATE PREPARE wsp_face_upgrade_stmt;
SET @wsp_face_upgrade_sql = IF(@wsp_face_upgrade_ready=1,'UPDATE wp_migraciones SET checksum=''f510d1912ac76f34ca0e60c64ea799d57065cbd3cd045227cf124148156acf2a'' WHERE version=''V014''','SELECT ''CANCELADO requiere V011 y ninguna version posterior a V014'' AS resultado');
PREPARE wsp_face_upgrade_stmt FROM @wsp_face_upgrade_sql;
EXECUTE wsp_face_upgrade_stmt;
DEALLOCATE PREPARE wsp_face_upgrade_stmt;

SELECT IF(@wsp_face_upgrade_ready=1,'Actualizacion V013 y V014 aplicada; comprobar PHP y APK','CANCELADO sin cambios') AS resultado;
SET @wsp_face_upgrade_ready=NULL, @wsp_face_upgrade_sql=NULL;
