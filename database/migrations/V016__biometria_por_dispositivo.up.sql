-- Guardian access remains active for facial login when fingerprint login is disabled.
-- Existing rows keep their current login behavior (DEFAULT 1).
-- Allows more than one active Android access per account; device_uid remains unique.
ALTER TABLE `wp_accesos_android`
  ADD COLUMN `huella_habilitada` TINYINT(1) NOT NULL DEFAULT 1 AFTER `estado`,
  DROP INDEX `uk_wpandroid_usuario`,
  ADD KEY `idx_wpandroid_active_user` (`activo_usuario`);

INSERT INTO `wp_migraciones` (`version`, `descripcion`)
VALUES ('V016', 'Control de huella por dispositivo sin revocar clave facial');
