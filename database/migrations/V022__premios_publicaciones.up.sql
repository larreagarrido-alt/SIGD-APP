-- Publicaciones de premios; los estados visibles se calculan con la hora del servidor.
-- Preserva las filas y fechas del catalogo anterior.
ALTER TABLE `wp_premios`
  ADD COLUMN `estado_publicacion` ENUM('BORRADOR','PUBLICADO','DESACTIVADO') NOT NULL DEFAULT 'BORRADOR' AFTER `activo`,
  ADD COLUMN `inicio_en` DATETIME NULL AFTER `estado_publicacion`,
  ADD COLUMN `fin_en` DATETIME NULL AFTER `inicio_en`,
  ADD COLUMN `eliminado_en` DATETIME NULL AFTER `fin_en`;

UPDATE `wp_premios`
SET `estado_publicacion` = IF(`activo` = 1, 'PUBLICADO', 'DESACTIVADO'),
    `inicio_en` = IF(`fecha_inicio` IS NULL, NULL, TIMESTAMP(`fecha_inicio`, '00:00:00')),
    `fin_en` = IF(`fecha_fin` IS NULL, NULL, TIMESTAMP(`fecha_fin`, '23:59:59'));

INSERT INTO `wp_migraciones` (`version`, `descripcion`)
VALUES ('V022', 'Estados y vigencia horaria de publicaciones de premios');
