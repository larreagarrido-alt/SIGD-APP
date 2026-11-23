-- Solo lectura. Ejecutar en wsnet_websiteplus después de las dos importaciones.
SELECT
  (SELECT COUNT(*) FROM wp_migraciones WHERE version='V029') AS v029,
  (SELECT COUNT(*) FROM wp_migraciones WHERE version='V030') AS v030,
  (SELECT COUNT(*) FROM wp_kpi_definiciones
    WHERE codigo LIKE 'SIGD-OFICIAL-CARGO-%' AND tipo_metrica='PORCENTAJE'
      AND unidad_medida='CUMPLIMIENTO' AND meta_esperada=100
      AND activo=1 AND regla_aprobada_en IS NOT NULL) AS metas_oficiales;
