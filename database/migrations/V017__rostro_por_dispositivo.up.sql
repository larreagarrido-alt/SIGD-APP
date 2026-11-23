-- Multiple Android devices may each hold one active facial key for the same account.
-- Keep the account lookup, and enforce one active face key per Android access.
ALTER TABLE `wp_claves_faciales`
  ADD COLUMN `activo_acceso_android` INT UNSIGNED
    AS (CASE WHEN `estado` = 'ACTIVO' THEN `id_acceso_android` END) STORED,
  DROP INDEX `uk_face_user`,
  ADD KEY `idx_face_active_user` (`activo_usuario`),
  ADD UNIQUE KEY `uk_face_active_android` (`activo_acceso_android`);

INSERT INTO `wp_migraciones` (`version`, `descripcion`)
VALUES ('V017', 'Clave facial activa por acceso Android');
