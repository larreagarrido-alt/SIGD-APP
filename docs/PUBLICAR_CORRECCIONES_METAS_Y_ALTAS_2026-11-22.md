# Publicar metas versionadas y claves iniciales

Estos cambios del repositorio no se activan solo por subirlos a GitHub. Haz un respaldo actual de ambas bases y de los PHP que se reemplazarán.

## SIGD en cPanel

1. En `wsnet_websiteplus`, aplica **una sola vez** `database/migrations/V031__kpi_metas_versionadas.up.sql`. Comprueba que `wp_migraciones` tenga V031 antes de continuar. No borres evaluaciones ni reportes.
2. Reemplaza estos PHP conservando su ruta:
   - `websiteplus_backend/src/Kpi/KpiOfficialGoals.php` (nuevo).
   - `websiteplus_backend/src/Kpi/KpiOfficialScore.php`.
   - `websiteplus_backend/src/Controllers/KpiTemplateController.php`.
   - `websiteplus_backend/src/Controllers/KpiFormsController.php`.
   - `websiteplus_backend/src/Controllers/KpiOfficialAdminController.php`.
   - `websiteplus_backend/src/Controllers/AuthController.php`.
3. Instala el APK compilado de este mismo commit. En el Editor de reportes KPI, el menú **Metas del cálculo oficial** permite cambiar cantidades numéricas y guardarlas en el servidor. Los textos de la lámina se editan aparte; comprueba que expresen la misma meta antes de publicar. Cada ficha nueva queda ligada a la revisión de metas activa cuando se recibió.

## Web de Asistencias

Sube a la raíz de la web de Asistencias, respetando rutas y después de respaldar los originales:

- `integration/asistencia/controllers/TrabajadorController.php` → `controllers/TrabajadorController.php`.
- `integration/asistencia/views/trabajadores/index.php` → `views/trabajadores/index.php`.

Al crear un trabajador, Administración ahora debe darle una **clave inicial única** de 12 a 128 caracteres y entregársela por un canal privado. En su primer ingreso, la web de Asistencias obliga a cambiarla. La app SIGD no admite esa clave provisional; el trabajador entra a la app después del cambio en la web. Esto afecta solo a nuevas altas hechas con los PHP actualizados; no cambia contraseñas existentes.

## Comprobación final

Con cuentas autorizadas y sin crear reportes falsos en producción, comprueba: alta de un trabajador de prueba controlado, primer cambio de clave en Asistencias, ingreso a SIGD, asignación del formulario por cargo, edición de una meta numérica, envío de una ficha real y revisión del porcentaje. El cálculo y los puntos de fichas antiguas conservan su revisión o la meta predeterminada. No hagas la limpieza de datos de prueba hasta distinguirlos de registros reales nuevos.
