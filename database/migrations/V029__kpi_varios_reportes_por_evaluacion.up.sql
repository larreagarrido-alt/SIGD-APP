-- Weekly evaluations such as Sales link every daily report used in the calculation.
SET NAMES utf8mb4;
ALTER TABLE wp_kpi_reporte_evaluaciones
  DROP INDEX uk_wpkpirep_eval,
  ADD KEY idx_wpkpirep_eval (id_evaluacion);
INSERT INTO wp_migraciones (version,descripcion)
VALUES ('V029','Varios reportes diarios vinculados a una evaluacion semanal');
