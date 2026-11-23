-- Permit the weekly questionnaire already present in Dani's Android module.
-- Schema only: no questions, answers, permissions, accounts or sessions are changed.
ALTER TABLE `wp_bienestar_tests`
  MODIFY COLUMN `periodicidad` ENUM('MENSUAL','TRIMESTRAL','SEMESTRAL','LIBRE','SEMANAL')
  NOT NULL DEFAULT 'MENSUAL';

INSERT INTO `wp_migraciones` (`version`, `descripcion`)
VALUES ('V020', 'Periodicidad semanal para cuestionarios de bienestar');
