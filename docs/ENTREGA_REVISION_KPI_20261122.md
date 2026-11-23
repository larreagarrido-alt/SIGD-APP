# Revisión de reportes KPI — 22/11/2026

## Cambios listos

- «Reportes recibidos» abre la ficha guardada en el servidor. Programación conserva su lámina original; los demás cargos muestran una ficha SIGD con todas las secciones y datos recibidos, con zoom.
- El administrador ve el porcentaje calculado, pulsa «Calcular y decidir» y después puede **Aprobar** u **Observar**. La observación exige un motivo y no acredita puntos.
- El editor ya no pide el ID del trabajador. Los KPI aprobados se asignan desde el cargo vigente en Asistencias al abrir «Mis KPI». El SQL adjunto asigna los trabajadores activos que ya existían.
- Gerencia conserva sus KPI y queda fuera del ranking del equipo. Volvió la flecha para salir de su formulario.
- Las cuentas, cargos y estados laborales se leen de Asistencias. Las 14 fotos proporcionadas en `Descargas/Fotosapp` se sirven por una ruta privada de la API que exige una sesión válida; no van dentro del APK ni al repositorio.
- Diez fotos tienen contenido PNG aunque el archivo se llame `.jpg`; el controlador detecta el formato real al responder.

## Archivos para producción

Conservar antes copia de los PHP actuales y subir estos cuatro archivos:

1. `backend/src/Controllers/KpiOfficialAdminController.php` → `websiteplus_backend/src/Controllers/KpiOfficialAdminController.php`
2. `backend/src/Controllers/KpiFormsController.php` → `websiteplus_backend/src/Controllers/KpiFormsController.php`
3. `backend/src/Controllers/PersonalController.php` → `websiteplus_backend/src/Controllers/PersonalController.php`
4. `backend/public/index.php` → `public_html/websiteplus-api/index.php`

En cPanel, crear `websiteplus_backend/storage/profile-photos/`, subir allí `FOTOS_PRIVADAS_SUBIR_Y_EXTRAER.zip` y extraerlo en esa misma carpeta. Deben quedar 14 archivos `1.jpg`, `2.jpg`, etc., directamente dentro de `profile-photos/`. Borrar el ZIP del servidor después de extraerlo. Esa carpeta es privada y queda fuera de `public_html`; no ponerla en `public_html` ni subirla a GitHub.

`database/seeds/32_ASIGNAR_KPI_ACTIVOS_20261122.sql` ya fue importado por el dueño en **wsnet_websiteplus**. No volver a importarlo para esta actualización de fotos.

Instalar `app-debug.apk` sobre la aplicación actual, sin desinstalarla.

## Comprobaciones realizadas

- Compilación de Android y pruebas unitarias: correctas.
- Pruebas de servidor con bases sintéticas: 823 correctas. No se conectaron a producción.
- APK instalado en el teléfono conectado, sin borrar datos.

## Comprobaciones pendientes en producción

- Abrir un reporte real de cada tipo de formulario y revisar su ficha y porcentaje.
- Aprobar u observar un reporte real solo cuando corresponda; no usar los reportes de prueba para una aprobación oficial.
- Comprobar en el APK instalado que la foto del propio perfil y las tarjetas de Personal aparecen tras subir los 14 JPG. Los nuevos trabajadores necesitarán que su foto de Asistencias sea accesible al backend; aún falta conocer la carpeta real de `assets/uploads` en cPanel para automatizar esa parte. Mientras no haya imagen, la app muestra iniciales.
- Completar la limpieza de los reportes y evaluaciones que el dueño identificó como pruebas, después del respaldo acordado.

GitHub queda para el final, por indicación del dueño.
