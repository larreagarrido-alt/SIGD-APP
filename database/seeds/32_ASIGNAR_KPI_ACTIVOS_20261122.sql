-- Ejecutar una vez en wsnet_websiteplus después de las metas oficiales.
-- Solo asigna metas ya aprobadas a trabajadores activos con cargo mapeado.
-- No crea cuentas, no cambia Asistencias y no toca reportes ni puntos.
INSERT IGNORE INTO wp_kpi_asignaciones
    (id_trabajador,id_kpi,fecha_inicio,asignado_por)
SELECT t.id_trabajador,d.id_kpi,CURDATE(),NULL
FROM wsnet_bd_asistencia.trabajadores AS t
JOIN wp_kpi_formulario_cargos AS m ON m.id_cargo=t.id_cargo
    AND m.aprobado_en IS NOT NULL
JOIN wp_kpi_definiciones AS d ON d.id_cargo_objetivo=t.id_cargo
    AND d.codigo=CONCAT('SIGD-OFICIAL-CARGO-',t.id_cargo)
    AND d.tipo_metrica='PORCENTAJE' AND d.unidad_medida='CUMPLIMIENTO'
    AND d.activo=1 AND d.regla_aprobada_en IS NOT NULL
WHERE t.estado='Activo'
    AND NOT EXISTS (
        SELECT 1 FROM wp_kpi_asignaciones AS a
        WHERE a.id_trabajador=t.id_trabajador AND a.id_kpi=d.id_kpi
          AND a.activo=1 AND a.fecha_inicio<=CURDATE()
          AND (a.fecha_fin IS NULL OR a.fecha_fin>=CURDATE())
    );

SELECT COUNT(*) AS asignaciones_activas
FROM wp_kpi_asignaciones
WHERE activo=1 AND fecha_inicio<=CURDATE()
  AND (fecha_fin IS NULL OR fecha_fin>=CURDATE());
