# Cambio de horario y de cargo entre Asistencias y KPIWeb

Revisión del respaldo de Asistencias descargado el 21/11/2026 y del código actual. El respaldo es una copia; las cifras pueden haber cambiado en producción.

## Resultado

- Las 15 cuentas de colaborador o practicante del respaldo tienen `usuarios.id_rol` coherente con `trabajadores.tipo` (8 y 7 respectivamente).
- Seis colaboradores conservan filas en `horario_practicantes`. Esas filas podían imponerles un horario de una etapa anterior. El servidor corregido consulta `trabajadores.tipo` en cada solicitud y, para colaboradores, usa únicamente sus asignaciones vigentes en `horarios_trabajadores` y `temporadas_horario`.
- Tres colaboradores activos de esa copia no tienen asignación individual vigente: uno con cargo Community Manager y dos con cargo Creativo/Videógrafo. Sin una asignación real, KPIWeb no puede deducir sus días de trabajo. No se usan las filas antiguas de practicante ni una plantilla por cargo como sustituto.
- El formulario autorizado y su título se actualizan al abrir KPI según `trabajadores.id_cargo`, `trabajadores.tipo` y `cargos.nombre`. El rol de permisos sigue siendo `usuarios.id_rol`, que controla Asistencias; KPIWeb no lo cambia por su cuenta.
- Al cambiar de cargo, los reportes confirmados conservan el `id_cargo` con que se enviaron. El servidor rechaza una revisión que intentaría reescribir un reporte de otro cargo. El APK usa una identidad nueva para reportes posteriores al cambio, incluso cuando ambos cargos comparten el mismo tipo de formulario.
- Las vacaciones, permisos, feriados, faltas y descansos compensatorios se siguen tomando de Asistencias. Los niveles porcentuales son comunes; la meta numérica se ajusta por turnos exigibles al calificar.

## Consulta de la base actual

`00_AUDITORIA_HORARIOS_SOLO_LECTURA.sql` del paquete consulta sin escribir: cuentas cuyo rol no coincide con su tipo, cantidad de colaboradores con horario antiguo de practicante y los ID de colaboradores activos sin asignación vigente. Ejecutarla en `wsnet_bd_asistencia` permite saber si los tres horarios pendientes del respaldo ya se configuraron.

## Publicación

Paquete: `C:\Users\Jair L.G\Downloads\KPIWeb_CAMBIO_CARGO_20261122`.

- Reemplazar en cPanel `websiteplus_backend/src/Auth/KpiLaborCalendar.php`.
- Reemplazar `websiteplus_backend/src/Controllers/KpiFormsController.php`.
- Instalar `KPIWeb_CAMBIO_CARGO_20261122.apk` sin borrar datos. No se sube el APK a cPanel.

No se importa un SQL de actualización. Las tablas nuevas de la publicación anterior siguen iguales. Guardar copia de los dos PHP existentes antes de reemplazarlos. El paquete no contiene datos de trabajadores.

## Comprobación

Pruebas con bases sintéticas aisladas: cambio de tipo y cargo, selección del horario de colaborador aunque queden filas antiguas de practicante, separación de dos reportes del mismo formulario con cargos distintos, conservación del reporte antiguo y controles de permisos. Además, compilación del APK y sintaxis PHP correctas. Falta probar un cambio real de cargo después de publicarlo; el respaldo no acredita por sí solo el estado actual de producción.
