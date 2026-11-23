# Verificación de cuentas existentes — pendiente de evidencia autorizada

La prueba local usa identidades sintéticas; no se conocen aquí los permisos efectivos de todos los titulares de producción. La cuenta que motivó el reporte confirmó un login exitoso y la pantalla «Cuenta y seguridad» con el APK anterior. El código anterior exigía `dashboard.admin` o `kpi.ver` para abrir un dashboard. El APK corregido abre el inicio personal para un trabajador `Activo` verificado por `/auth/me`, incluso si no existe aún una asignación KPI; el dashboard administrativo sigue requiriendo `dashboard.admin`. Cada endpoint conserva sus controles de permisos. El titular probó el APK corregido en su teléfono y confirmó que ya abre el dashboard personal. «Mis KPI» aún no muestra formularios; una consulta HTTP sin credenciales confirmó 404 en `/kpi/formularios/permitidos` del servidor publicado.

| Perfil laboral | Cargo real | Permisos efectivos en producción | Dashboard esperado | Dashboard obtenido en teléfono | Formulario aprobado |
|---|---|---|---|---|---|
| Administrador | Según `/auth/me` | Pendiente | Administrativo si tiene `dashboard.admin`; de lo contrario inicio personal si es trabajador activo | Pendiente | Según servidor |
| Gerencia | 1 Gerente General | Pendiente | Según `dashboard.admin` efectivo | Pendiente | `GERENCIA` propuesto; no aprobado |
| Jefatura | 2 Jefe de Agencia | Pendiente | Según `dashboard.admin` efectivo | Pendiente | Sin asignación automática |
| Supervisor | Según trabajador real | Pendiente | Según `dashboard.admin` efectivo; si no, inicio personal | Pendiente | Según servidor |
| Colaborador | 3, 4, 5, 13, 14, 16, 17 u otro cargo que corresponda al titular | Pendiente | Inicio personal, salvo permiso administrativo explícito | Pendiente | Solo mapeo aprobado |
| Practicante | 6, 7, 15 u otro cargo que corresponda al titular | Pendiente | Inicio personal, salvo permiso administrativo explícito | Pendiente | Solo mapeo aprobado |

La relación entre rol, cargo y tipo de trabajador es independiente; la tabla no afirma que todas las personas con un cargo tengan el mismo rol. Los 12 IDs reales y los 11 mapeos propuestos están en `PROPUESTA_CARGOS_FORMULARIOS_KPI_2026-11-14.md`. En laboratorio se comprobaron los 12 nombres exactos en `/auth/me` y los 11 mapeos propuestos, pero ello no prueba permisos reales de producción.

## Resultado real de la cuenta administrativa observada

La consulta de solo lectura que ejecutó el usuario en phpMyAdmin el 14-11-2026 devolvió una sola fila para Manuel: **rol 1 Administrador, cargo 1 Gerente General**. Las columnas `dashboard_admin_rol`, `dashboard_admin_excepcion`, `kpi_ver_rol` y `kpi_ver_excepcion` estaban vacías. Una segunda lectura confirmó que el código `dashboard.admin` existe con `id_permiso=19`, pero el rol Administrador tiene **cero** concesiones. El router Android prioriza `dashboard.admin`; la pantalla personal resultó de la ausencia de ese permiso efectivo, no de una prioridad equivocada ni de un permiso perdido al guardar la sesión.

El APK posterior evita presentar roles de gestión sin `dashboard.admin` como colaboradores: muestra un estado de autorización pendiente. La propuesta de concesión mínima para el rol Administrador está en `PROPUESTA_PERMISO_DASHBOARD_ADMIN_2026-11-14.sql`. Ese permiso con alcance `TODOS` expone métricas empresariales; requiere aprobación específica, respaldo restaurable y ensayo en staging. No se ha aplicado.

Prueba física posterior: el APK `KPIWeb-presentacion-20261114-roles-seguros-debug.apk` se instaló con ADB sin borrar datos. El titular abrió la cuenta administrativa y confirmó el aviso específico «el servidor no le concedió acceso al dashboard administrativo». ADB verificó `MainActivity` en primer plano y el texto del aviso, sin extraer credenciales. Así queda confirmado que la cuenta ya no se enruta al dashboard de colaborador. No se ha comprobado el dashboard administrativo con permiso concedido ni se han probado físicamente las otras cuentas.

## Lectura autorizada en phpMyAdmin

Esta consulta muestra **solo metadatos de acceso** y debe revisarse privadamente. No publicar el resultado ni introducirlo en Git. No intenta iniciar sesión ni cambia datos. Permite detectar qué roles carecen de `dashboard.admin` y `kpi.ver`, y si hay anulaciones por usuario. Verificar previamente que el esquema tenga los nombres de columna indicados.

```sql
SELECT u.id_usuario, u.username, u.id_rol, r.nombre AS rol_nombre,
       t.id_trabajador, t.estado AS estado_trabajador, t.tipo AS tipo_trabajador,
       t.id_cargo, c.nombre AS cargo_nombre, t.id_agencia,
       COALESCE(rp_admin.alcance, '') AS dashboard_admin_rol,
       COALESCE(up_admin.efecto, '') AS dashboard_admin_excepcion,
       COALESCE(rp_kpi.alcance, '') AS kpi_ver_rol,
       COALESCE(up_kpi.efecto, '') AS kpi_ver_excepcion
FROM wsnet_bd_asistencia.usuarios u
JOIN wsnet_bd_asistencia.roles r ON r.id_rol=u.id_rol
LEFT JOIN wsnet_bd_asistencia.trabajadores t ON t.id_trabajador=u.id_trabajador
LEFT JOIN wsnet_bd_asistencia.cargos c ON c.id_cargo=t.id_cargo
LEFT JOIN wsnet_websiteplus.wp_permisos p_admin ON p_admin.codigo='dashboard.admin'
LEFT JOIN wsnet_websiteplus.wp_rol_permisos rp_admin ON rp_admin.id_rol=u.id_rol AND rp_admin.id_permiso=p_admin.id_permiso
LEFT JOIN wsnet_websiteplus.wp_usuario_permisos up_admin ON up_admin.id_usuario=u.id_usuario AND up_admin.id_permiso=p_admin.id_permiso
LEFT JOIN wsnet_websiteplus.wp_permisos p_kpi ON p_kpi.codigo='kpi.ver'
LEFT JOIN wsnet_websiteplus.wp_rol_permisos rp_kpi ON rp_kpi.id_rol=u.id_rol AND rp_kpi.id_permiso=p_kpi.id_permiso
LEFT JOIN wsnet_websiteplus.wp_usuario_permisos up_kpi ON up_kpi.id_usuario=u.id_usuario AND up_kpi.id_permiso=p_kpi.id_permiso
ORDER BY u.id_rol, t.id_cargo, u.id_usuario;
```

Interpretar una excepción `DENEGAR` como ausencia del permiso aunque el rol tenga alcance. Esta consulta no decide cambios de permisos: una corrección de permisos legítimos requiere aprobación específica de la empresa. La prueba de los seis perfiles reales requiere consentimiento de sus titulares y una sesión supervisada en el teléfono; no se prueban automáticamente sus contraseñas.

Para auditar los seis roles **sin mostrar cuentas personales**, ejecutar adicionalmente en phpMyAdmin:

```sql
SELECT r.id_rol,r.nombre AS rol_nombre,p.codigo,rp.alcance,
       COUNT(DISTINCT u.id_usuario) AS cuentas_con_este_rol
FROM wsnet_bd_asistencia.roles AS r
LEFT JOIN wsnet_bd_asistencia.usuarios AS u ON u.id_rol=r.id_rol
LEFT JOIN wsnet_websiteplus.wp_rol_permisos AS rp ON rp.id_rol=r.id_rol
LEFT JOIN wsnet_websiteplus.wp_permisos AS p ON p.id_permiso=rp.id_permiso
GROUP BY r.id_rol,r.nombre,p.codigo,rp.alcance
ORDER BY r.id_rol,p.codigo;
```

Una fila sin `codigo` indica que ese rol no tiene ninguna concesión por rol. No inferir autorización individual: `wp_usuario_permisos` puede permitir o denegar permisos para cuentas concretas. Jefatura y Supervisión necesitan además una decisión sobre alcance; el resumen administrativo actual devuelve métricas de toda la empresa y requiere `dashboard.admin` con `TODOS`.
