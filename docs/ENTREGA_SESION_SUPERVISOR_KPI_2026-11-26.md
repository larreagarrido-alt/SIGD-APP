# Sesión, supervisor y cálculo KPI — 26 de noviembre de 2026

Se corrigió el cierre de sesión al vencer el token de acceso. El APK guarda el token de renovación cifrado con Android Keystore, solicita uno nuevo al servidor y repite la petición una vez. Tras instalar este APK, cada persona debe iniciar sesión una vez para que quede guardado su token de renovación. La inactivación en Asistencias, el cambio de contraseña y el cierre voluntario siguen invalidando la sesión.

En Programación, la vista previa ya compara tareas entregadas del tipo exigido para el día con la meta oficial de ese día. Las filas pendientes y las tareas de otro tipo no elevan el porcentaje. «Bugs reportados» es la suma informativa de los bugs anotados en cada actividad; no entra en el cálculo. El servidor conserva el cálculo final con los turnos exigibles de Asistencias y el administrador aprueba u observa la validez del reporte.

El nombre de supervisor se obtiene de `trabajadores.id_supervisor` en Asistencias. Si no hay supervisor asignado, la app lo indica. En Reportes del administrador se carga la foto del trabajador mediante la ruta privada que ya usa Personal.

## Archivos de cPanel

El paquete de Descargas reúne los cambios de esta revisión y los dos PHP de conexiones que aún no se habían publicado. Sustituir los archivos homónimos, conservando copia de los actuales:

- `websiteplus_backend/src/Controllers/AuthController.php`
- `websiteplus_backend/src/Controllers/DashboardController.php`
- `websiteplus_backend/src/Controllers/KpiFormsController.php`
- `websiteplus_backend/src/Controllers/KpiOfficialAdminController.php`
- `websiteplus_backend/src/Controllers/PersonalController.php`
- `websiteplus_backend/src/Kpi/KpiOfficialScore.php`

No se importa SQL. El APK se instala encima de la versión actual para conservar los datos del teléfono.

## Comprobación

El backend pasó 840 casos en una base sintética; Android compiló y pasó sus pruebas unitarias. Tras publicar los PHP, comprobar en una cuenta autorizada que el supervisor tiene nombre, Reportes muestra fotos y una ficha de Programación explica las tareas que cuentan. Para probar la renovación de sesión se requiere esperar al vencimiento del token; no se ha forzado una expiración en producción.
