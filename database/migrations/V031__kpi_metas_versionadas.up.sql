-- Las metas numéricas oficiales se guardan por revisión y se fijan al recibir cada ficha.
-- La plantilla visual continúa siendo editable, pero los cambios de puntuación requieren
-- el campo estructurado validado por el servidor.
CREATE TABLE wp_kpi_metas_versiones (
  revision BIGINT UNSIGNED NOT NULL,
  metas_json LONGTEXT NOT NULL,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  creado_por INT NULL,
  PRIMARY KEY (revision),
  CONSTRAINT ck_wpkpimetas_json CHECK (JSON_VALID(metas_json))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO wp_kpi_metas_versiones (revision,metas_json)
VALUES (0,'{}');

-- La plantilla visual puede tener revisiones previas. Esas revisiones no
-- contenían metas numéricas oficiales, por lo que conservan los valores base.
INSERT IGNORE INTO wp_kpi_metas_versiones (revision,metas_json,creado_por)
SELECT revision,'{}',actualizado_por FROM wp_kpi_plantillas_estado WHERE id=1;

ALTER TABLE wp_kpi_formularios_detalle
  ADD COLUMN meta_revision BIGINT UNSIGNED NULL AFTER revision;

INSERT INTO wp_migraciones (version,descripcion)
VALUES ('V031','Metas KPI numericas versionadas por ficha recibida');
