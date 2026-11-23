# KPI y Asistencias: publicación del calendario real

Estado al 22/11/2026: implementación y pruebas locales terminadas; pendiente publicar y comprobar en producción. El respaldo original de Asistencias usado para revisar columnas fue `Descargas/respaldo/wsnet_bd_asistencia.sql`. No se copiaron datos personales al paquete.

## Qué cambia

- La API lee de Asistencias el horario de cada trabajador. Para practicantes usa `horario_practicantes` y su historial `horario_vigencias`; para los demás, `horarios_trabajadores`, `temporadas_horario` y `horario_detalles`.
- Vacaciones y permisos aprobados se aplican entre `fecha_inicio` y `fecha_fin`, incluidos ambos días. Un día libre no cancela una meta semanal completa.
- Los descansos por horas extra de `consumos_extras` quitan únicamente el turno cubierto. Los feriados no laborables y las ausencias justificadas y aprobadas tampoco se exigen. Una falta sin aprobar queda pendiente de revisión, sin calificación automática.
- Si un período no tiene ningún turno exigible, su KPI no se exige. Si hay algunos turnos, una meta numérica se ajusta: `meta efectiva = meta original × turnos exigibles / turnos programados`. Los niveles porcentuales siguen siendo los mismos para todos los cargos. La evaluación guarda la meta y el calendario aplicados al calificarla.
- El APK ya no inventa turnos ni marcaciones. Al abrir el formulario consulta el calendario de esa cuenta. Los avisos vuelven a consultarlo antes de mostrarse; sin confirmación del servidor no se emite aviso.

## Archivos de publicación

Están copiados juntos en `C:\Users\Jair L.G\Downloads\KPIWeb_ASISTENCIAS_20261122`.

1. En phpMyAdmin, base `wsnet_websiteplus`, importar `01_APLICAR_V027_META_TURNOS.sql` una sola vez. Solo agrega cuatro columnas para dejar constancia del ajuste y registra V027. No modifica Asistencias ni los KPI ya calificados.
2. En cPanel, carpeta `websiteplus_backend/src/Auth/`, reemplazar `LaborPolicy.php` y subir el nuevo `KpiLaborCalendar.php` desde `PHP/Auth/`.
3. En `websiteplus_backend/src/Controllers/`, reemplazar `KpiController.php` y `KpiFormsController.php` desde `PHP/Controllers/`.
4. Instalar `KPIWeb_ASISTENCIAS_20261122.apk` en el teléfono sin borrar los datos de la aplicación. El APK se entrega aparte; no se sube a cPanel.

Conservar antes una copia de esos tres PHP existentes y el respaldo SQL reciente. Subir los PHP después de V027, porque `KpiController.php` usa las columnas nuevas. No cambiar `public_html/websiteplus-api/index.php`, `.htaccess`, permisos ni tablas de Asistencias para esta actualización.

## Comprobación sin datos personales

- Un trabajador con horario variable ve solo sus turnos programados para la semana. Un día sin turno no genera aviso KPI.
- Una vacación aprobada de varias semanas muestra el período libre y no exige fichas ni crea incumplimientos.
- Un descanso aprobado solo de tarde deja el turno de mañana disponible.
- En una evaluación numérica de 10 actividades con 12 turnos programados y 10 exigibles, la meta congelada pasa a 8,33. Los umbrales de porcentaje no cambian.
- Los formularios ya enviados y las evaluaciones históricas se conservan.

## Pruebas realizadas

`backend/tests/integration.php`: 781 verificaciones con dos bases sintéticas aisladas, sin conexión a producción. `php -l` correcto en los cuatro PHP. `:app:assembleDebug --offline` correcto. Falta la comprobación con cuentas y permisos reales tras publicar; hasta entonces no se afirma que producción esté actualizada.
