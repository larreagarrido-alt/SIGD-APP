USE `websiteplus`;
-- No new tables: idempotent redemptions and unique credits per evaluation.
ALTER TABLE wp_canjes ADD COLUMN IF NOT EXISTS solicitud_id VARCHAR(64) NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_canje_solicitud ON wp_canjes (id_trabajador, solicitud_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_puntos_referencia ON wp_puntos_ledger (tipo_movimiento, referencia_tipo, referencia_id);
-- An observed submission may be signed again; each challenge is usable once.
CREATE INDEX IF NOT EXISTS idx_firma_evaluacion ON wp_kpi_firmas (id_evaluacion);
DROP INDEX IF EXISTS uk_wpkpifirma_eval ON wp_kpi_firmas;
CREATE UNIQUE INDEX IF NOT EXISTS uk_firma_desafio ON wp_kpi_firmas (id_desafio);
INSERT IGNORE INTO wp_migraciones (version,descripcion) VALUES ('V010','Idempotencia canjes, creditos unicos y firmas por intento');
