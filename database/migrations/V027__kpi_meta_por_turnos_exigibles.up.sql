-- Freeze the attendance adjustment used when an official KPI is graded.
ALTER TABLE `wp_kpi_evaluaciones`
  ADD COLUMN `meta_efectiva_snapshot` DECIMAL(10,2) NULL AFTER `meta_snapshot`,
  ADD COLUMN `turnos_programados_snapshot` SMALLINT UNSIGNED NULL AFTER `meta_efectiva_snapshot`,
  ADD COLUMN `turnos_exigibles_snapshot` SMALLINT UNSIGNED NULL AFTER `turnos_programados_snapshot`,
  ADD COLUMN `calendario_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL AFTER `turnos_exigibles_snapshot`;

INSERT INTO `wp_migraciones` (`version`,`descripcion`)
VALUES ('V027','Meta KPI ajustada a turnos exigibles de Asistencias');
