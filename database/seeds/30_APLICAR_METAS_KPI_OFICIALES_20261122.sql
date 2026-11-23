-- Metas oficiales SIGD autorizadas el 22/11/2026.
-- Se calculan en el servidor desde cantidades del formulario y turnos exigibles de Asistencias.
-- Requiere las migraciones V003, V009, V023 y V027. No altera reportes ni puntos existentes.
SET NAMES utf8mb4;

INSERT INTO wp_kpi_definiciones
  (codigo,id_categoria,nombre,descripcion,tipo_metrica,unidad_medida,meta_esperada,
   peso,puntos_base,periodicidad,aplica_a,id_cargo_objetivo,responsable_revision,
   activo,creado_por,regla_aprobada_por,regla_aprobada_en)
SELECT CONCAT('SIGD-OFICIAL-CARGO-',c.id_cargo),1,
       CONCAT('Cumplimiento oficial - ',c.tipo_formulario),
       'Porcentaje recalculado en el servidor desde las metas del cronograma y el formulario confirmado.',
       'PORCENTAJE','CUMPLIMIENTO',100.00,1.00,20,c.periodicidad,'CARGO',c.id_cargo,
       a.id_usuario,1,a.id_usuario,a.id_usuario,NOW()
FROM (
  SELECT 1 id_cargo,'GERENCIA' tipo_formulario,'SEMANAL' periodicidad UNION ALL
  SELECT 3,'DISENADOR','SEMANAL' UNION ALL
  SELECT 4,'PROGRAMACION','DIARIO' UNION ALL
  SELECT 5,'VENTAS','SEMANAL' UNION ALL
  SELECT 6,'PROGRAMACION','DIARIO' UNION ALL
  SELECT 7,'DISENADOR_JUNIOR','DIARIO' UNION ALL
  SELECT 13,'COMMUNITY','SEMANAL' UNION ALL
  SELECT 14,'DISENADOR_SENIOR','SEMANAL' UNION ALL
  SELECT 15,'COMMUNITY','SEMANAL' UNION ALL
  SELECT 16,'COMMUNITY','SEMANAL' UNION ALL
  SELECT 17,'VIDEO','SEMANAL'
) c
JOIN (SELECT MIN(id_usuario) id_usuario FROM usuarios WHERE id_rol=1) a ON a.id_usuario IS NOT NULL
JOIN cargos cargo ON cargo.id_cargo=c.id_cargo
WHERE NOT EXISTS (
  SELECT 1 FROM wp_kpi_definiciones old
  WHERE old.id_cargo_objetivo=c.id_cargo AND old.activo=1 AND old.regla_aprobada_en IS NOT NULL
);

INSERT INTO wp_migraciones (version,descripcion)
SELECT 'V030','Metas oficiales automaticas por cargo y periodos exigibles'
WHERE NOT EXISTS (SELECT 1 FROM wp_migraciones WHERE version='V030');

