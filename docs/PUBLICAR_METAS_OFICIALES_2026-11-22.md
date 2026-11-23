# Publicar metas KPI oficiales — 22/11/2026

## Archivos para cPanel

Desde `C:\Users\Jair L.G\Downloads\KPIWeb_METAS_OFICIALES_20261122\CPANEL\websiteplus_backend\src\` subir **estos seis PHP** a las mismas rutas de `websiteplus_backend/src` en cPanel, después de descargar una copia de los seis archivos actuales:

1. `Auth/KpiLaborCalendar.php`
2. `Controllers/KpiOfficialAdminController.php`
3. `Controllers/KpiController.php`
4. `Kpi/KpiFormCompletion.php`
5. `Kpi/KpiFormMeasurement.php`
6. `Kpi/KpiOfficialScore.php` (nuevo)

No reemplazar `public_html/websiteplus-api/index.php` para este cambio. Instalar el APK adjunto sobre el existente, sin borrar datos, para retirar el campo de ID del indicador en la revisión.

## Base de datos

En phpMyAdmin seleccionar **`wsnet_websiteplus`**, comprobar que existe un respaldo reciente, e importar en este orden:

1. `SQL/01_V029__kpi_varios_reportes_por_evaluacion.up.sql` — permite que Ventas vincule las fichas de cada día con una sola evaluación semanal. Ejecutar una sola vez; debe registrarse `V029` en `wp_migraciones`.
2. `SQL/02_APLICAR_METAS_KPI_OFICIALES_20261122.sql` — crea las 11 reglas automáticas de los 11 pares cargo–formulario ya aprobados. Registra `V030`. No borra reportes ni respuestas.

Ejecutar luego `SQL/03_VERIFICAR_METAS_KPI_OFICIALES.sql` (solo lectura). Debe mostrar `v029=1`, `v030=1` y `metas_oficiales=11`. Si algún número difiere, detenerse y revisar el resultado de la importación antes de aprobar un KPI.

## Flujo que queda activo

El teléfono envía cantidades y la API calcula el porcentaje desde la meta del cargo. Las vacaciones, permisos, cambios de horario y de tipo laboral usan los turnos exigibles de Asistencias. El administrador abre el reporte y decide aprobarlo u observarlo; al aprobar se acreditan 20/17/14/12/10 puntos según 90/80/60/40 %. Ventas se revisa una vez terminada la semana. El KPI de Gerencia se evalúa, pero no participa en el ranking. El ranking mensual se publica al cerrar el mes con su política de comparación aprobada.

## Límites que requieren decisión

El editor de cronograma cambia la ficha visual, pero no cambia todavía la fórmula del servidor. Para cambiar una meta oficial hay que actualizar ambos y comprobar el cálculo. Los trabajadores freelance sin turnos registrados siguen bloqueados para calificación, porque Asistencias no da un denominador verificable. El cargo Jefe de Agencia tiene horario de Diseñador Senior, pero carece de mapeo KPI aprobado. La publicación y una prueba real de extremo a extremo aún están pendientes; las pruebas locales no equivalen a verificación de producción.
