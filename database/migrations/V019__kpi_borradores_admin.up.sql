-- Local/staging only until approved. Administrative drafts never become official KPI assignments.
CREATE TABLE `wp_kpi_borradores_admin` (
  `id_usuario` INT NOT NULL,
  `borrador_uid` VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `revision` BIGINT UNSIGNED NOT NULL,
  `payload_json` LONGTEXT NOT NULL,
  `payload_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `actualizado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_usuario`,`borrador_uid`),
  CONSTRAINT `ck_wpkpiborrador_json` CHECK (JSON_VALID(`payload_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `wp_migraciones` (`version`,`descripcion`)
VALUES ('V019','Borradores administrativos KPI por cuenta');
