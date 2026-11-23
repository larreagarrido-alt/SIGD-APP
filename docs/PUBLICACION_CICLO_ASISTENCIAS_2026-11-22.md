# Publicación Asistencias ↔ KPIWeb, 22/11/2026

Usar **solo** `Descargas/KPIWeb_ASISTENCIAS_HISTORIAL_FINAL_20261122`. Los PHP de Asistencias parten de los dos archivos descargados de cPanel a `Descargas/respaldo`; los hashes de origen coincidieron con la copia revisada.

1. Exportar un respaldo actual de `wsnet_bd_asistencia` y conservar los PHP actuales de ambos sitios. El SQL crea una tabla nueva y registros iniciales; no altera las filas existentes de `trabajadores` o `usuarios`.
2. En phpMyAdmin seleccionar `wsnet_bd_asistencia` e importar **solo** `SQL/13_APLICAR_HISTORIAL_TIPO_ASISTENCIAS_20261122.sql`. Debe terminar sin error y mostrar el conteo `BASELINE`.
3. En el sitio web de **Asistencias** reemplazar `controllers/TrabajadorController.php` y `models/Trabajador.php` con los dos archivos de `PHP_ASISTENCIAS/`.
4. En la API de **KPIWeb** reemplazar `websiteplus_backend/src/Auth/KpiLaborCalendar.php`, `websiteplus_backend/src/Auth/LaborPolicy.php` y `websiteplus_backend/src/Controllers/KpiController.php` con los tres de `PHP_API/`.
5. Ejecutar `AUDITORIA_SOLO_LECTURA.sql` en phpMyAdmin para detectar cuentas con rol invertido o sin horario. Al volver de colaborador a practicante, confirmar el horario nuevo en la web de Asistencias; no se reutilizan automáticamente los días antiguos.

No hay cambio de APK ni migración de la base `wsnet_websiteplus`. El SQL debe aplicarse **antes** de los PHP porque la API y el editor consultan la tabla nueva. No crear un segundo trabajador o usuario para convertir una persona: se edita la ficha existente.

Los períodos anteriores al primer registro de `tipo_vigencias` carecen de fecha de conversión verificable. La API no les asigna puntos oficiales automáticamente. Para revertir código, restaurar los cinco PHP respaldados; conservar la tabla y sus registros hasta revisar cualquier conversión hecha desde la publicación.
