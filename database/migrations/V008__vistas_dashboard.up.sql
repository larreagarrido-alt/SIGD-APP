USE `websiteplus`;
-- =====================================================================
-- WebSite+  |  V008  |  Vistas analíticas para Dashboard y Reportes
-- Optimiza consultas frecuentes del administrador y la amiga sin tocar tablas físicas.
-- Detecta automáticamente quién está de vacaciones o con permiso aprobado hoy.
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Vista 1: Estado laboral completo y actual de cada trabajador
-- Detecta si hoy tiene permiso/vacaciones aprobadas en `permisos_laborales`.
-- ---------------------------------------------------------------------
CREATE OR REPLACE SQL SECURITY INVOKER VIEW `vw_wp_empleados_estado` AS
SELECT 
  t.`id_trabajador`,
  t.`dni`,
  CONCAT(t.`nombres`, ' ', t.`apellidos`) AS `nombre_completo`,
  t.`tipo` AS `tipo_trabajador`,
  t.`estado` AS `estado_asistencia`,
  COALESCE(s.`situacion`, 'VIGENTE') AS `situacion_websiteplus`,
  c.`id_cargo`,
  c.`nombre` AS `cargo_nombre`,
  a.`id_agencia`,
  a.`nombre` AS `agencia_nombre`,
  u.`id_usuario`,
  u.`username`,
  r.`nombre` AS `rol_nombre`,
  -- Indicador dinámico de permiso/vacaciones HOY
  (
    SELECT pl.`tipo_permiso`
    FROM `permisos_laborales` pl
    WHERE pl.`id_trabajador` = t.`id_trabajador`
      AND pl.`estado` = 'Aprobado'
      AND CURRENT_DATE() BETWEEN pl.`fecha_inicio` AND pl.`fecha_fin`
    LIMIT 1
  ) AS `permiso_hoy_tipo`,
  CASE 
    WHEN COALESCE(s.`situacion`, 'VIGENTE') = 'DESVINCULADO' THEN 'DESVINCULADO'
    WHEN t.`estado` = 'Inactivo' THEN 'INACTIVO'
    WHEN (
      SELECT 1 FROM `permisos_laborales` pl
      WHERE pl.`id_trabajador` = t.`id_trabajador`
        AND pl.`estado` = 'Aprobado'
        AND CURRENT_DATE() BETWEEN pl.`fecha_inicio` AND pl.`fecha_fin`
      LIMIT 1
    ) IS NOT NULL THEN 'DE_VACACIONES_O_PERMISO'
    ELSE 'OPERATIVO'
  END AS `condicion_operativa`
FROM `trabajadores` t
LEFT JOIN `cargos` c ON t.`id_cargo` = c.`id_cargo`
LEFT JOIN `agencias` a ON t.`id_agencia` = a.`id_agencia`
LEFT JOIN `usuarios` u ON u.`id_trabajador` = t.`id_trabajador`
LEFT JOIN `roles` r ON u.`id_rol` = r.`id_rol`
LEFT JOIN `wp_situacion_laboral` s ON s.`id_trabajador` = t.`id_trabajador`;

-- ---------------------------------------------------------------------
-- Vista 2: Resumen de cumplimiento de KPI por trabajador y periodo
-- ---------------------------------------------------------------------
CREATE OR REPLACE SQL SECURITY INVOKER VIEW `vw_wp_kpi_resumen_periodo` AS
SELECT 
  e.`id_trabajador`,
  e.`periodo_clave`,
  COUNT(*) AS `total_kpis`,
  SUM(CASE WHEN e.`estado` = 'Aprobado' THEN 1 ELSE 0 END) AS `aprobados`,
  SUM(CASE WHEN e.`estado` = 'Entregado' THEN 1 ELSE 0 END) AS `entregados`,
  SUM(CASE WHEN e.`estado` = 'Pendiente' THEN 1 ELSE 0 END) AS `pendientes`,
  SUM(CASE WHEN e.`estado` = 'Vencido' THEN 1 ELSE 0 END) AS `vencidos`,
  SUM(CASE WHEN e.`estado` = 'Observado' THEN 1 ELSE 0 END) AS `observados`,
  SUM(CASE WHEN e.`estado` = 'No exigible' THEN 1 ELSE 0 END) AS `no_exigibles`,
  -- Solo se computa el promedio de cumplimiento sobre los exigibles (excluyendo 'No exigible' y 'Cancelado')
  ROUND(
    AVG(CASE WHEN e.`estado` NOT IN ('No exigible', 'Cancelado') THEN COALESCE(e.`porcentaje_cumplimiento`, 0.00) ELSE NULL END),
    2
  ) AS `promedio_cumplimiento`,
  COALESCE(SUM(e.`puntos_otorgados`), 0) AS `puntos_obtenidos`
FROM `wp_kpi_evaluaciones` e
GROUP BY e.`id_trabajador`, e.`periodo_clave`;

-- ---------------------------------------------------------------------
-- Vista 3: Saldo de puntos actual de cada trabajador
-- ---------------------------------------------------------------------
CREATE OR REPLACE SQL SECURITY INVOKER VIEW `vw_wp_saldo_puntos` AS
SELECT 
  t.`id_trabajador`,
  CONCAT(t.`nombres`, ' ', t.`apellidos`) AS `nombre_completo`,
  COALESCE(
    (
      SELECT l.`saldo_posterior`
      FROM `wp_puntos_ledger` l
      WHERE l.`id_trabajador` = t.`id_trabajador`
      ORDER BY l.`id_movimiento` DESC
      LIMIT 1
    ),
    0
  ) AS `saldo_puntos_actual`
FROM `trabajadores` t;

INSERT IGNORE INTO `wp_migraciones` (`version`, `descripcion`) VALUES ('V008', 'Vistas analiticas: empleados completos, resumen kpi y saldos');
