-- Los planes existentes quedan como borradores; ninguno se publica por accidente.
ALTER TABLE `wp_bienestar_planes`
  MODIFY COLUMN `dimension` ENUM('AUTOACEPTACION','RELACIONES_POSITIVAS','AUTONOMIA','DOMINIO_ENTORNO','PROPOSITO_VIDA','CRECIMIENTO_PERSONAL','GENERAL','CARGA_DE_TAREAS','COMUNICACION','APOYO_DEL_EQUIPO','AMBIENTE_Y_RECURSOS','TRABAJO_Y_DESCANSO','APRENDIZAJE_Y_RECONOCIMIENTO','CLARIDAD_DE_RESPONSABILIDADES') NOT NULL,
  ADD COLUMN `hora_inicio` TIME NULL AFTER `fecha_programada`,
  ADD COLUMN `hora_fin` TIME NULL AFTER `hora_inicio`,
  ADD COLUMN `estado_publicacion` ENUM('BORRADOR','PUBLICADA') NOT NULL DEFAULT 'BORRADOR' AFTER `estado`,
  ADD COLUMN `visibilidad` ENUM('SOLO_ADMIN','TODO_EQUIPO','AGENCIA_CAMPANA','COLABORADORES') NOT NULL DEFAULT 'SOLO_ADMIN' AFTER `estado_publicacion`,
  ADD COLUMN `alcance_resumen` VARCHAR(255) NULL AFTER `visibilidad`,
  ADD COLUMN `agencias_json` LONGTEXT NULL AFTER `alcance_resumen`,
  ADD COLUMN `campanas_json` LONGTEXT NULL AFTER `agencias_json`,
  ADD COLUMN `trabajadores_json` LONGTEXT NULL AFTER `campanas_json`,
  ADD KEY `idx_wpwell_plan_publicacion` (`estado_publicacion`,`fecha_programada`);

INSERT INTO `wp_migraciones` (`version`,`descripcion`)
VALUES ('V028','Publicacion y destinatarios de actividades de bienestar');
