# Abrir KPIWeb Integración en Android Studio

## Qué se corrigió

Se incorporaron del ZIP `KPI-Web (cronograma).zip` la versión completa de `KpiReportsEditorFragment.java`, `KpiOfficialTemplateEngine.java`, `KpiZoomableSheetLayout.java`, el diseño `kpi_fragment_reports_editor.xml` y los tres recursos que faltaban: `kpi_ic_menu_new_kpi.xml`, `kpi_ic_menu_restore.xml` y `kpi_ic_report_bar.xml`. Se conservaron los módulos actuales de horarios, autenticación, formularios KPI, Bienestar y conexión con el servidor.

La compilación `:app:assembleDebug` y las pruebas `:app:testDebugUnitTest` terminaron correctamente en el proyecto integrado.

## Cómo abrir el ZIP de fuentes

1. Extraer el ZIP completo en una carpeta local.
2. En Android Studio elegir **File → Open** y seleccionar la carpeta `KPIWeb-integracion` que contiene `settings.gradle.kts`. No abrir solamente `app`.
3. Esperar **Gradle Sync**. Android Studio creará su propio `local.properties` con la ruta del SDK de ese equipo. Si pide componentes del SDK o Gradle, instalar los que indique el IDE.
4. Ejecutar la configuración `app` o, desde la terminal de la carpeta raíz, `gradlew.bat :app:assembleDebug`.

El ZIP excluye `local.properties`, `.idea`, `.gradle`, carpetas `build`, datos de producción y `backend/config/config.php`. Esos archivos son propios de cada equipo o contienen configuración privada.

## Alcance del Editor Maestro

La guía del ZIP original marca el Editor Maestro como local. Esta integración añade `V026`, `KpiTemplateController.php` y las rutas `/kpi/plantillas` para guardar las plantillas visuales con revisión en el servidor. El editor carga la revisión antes de permitir cambios y solo muestra confirmación cuando el servidor responde. Los formularios nuevos consultan la plantilla al abrirse; las hojas de semanas históricas conservan su presentación. Los logos PNG elegidos en el editor se comparten con la plantilla. Esto no crea definiciones KPI oficiales, puntuaciones ni modifica reportes ya enviados.

Los reportes KPI recibidos y el Centro de Reportes también admiten zoom con dos dedos y botones. El envío comprueba los formularios autorizados por el cargo antes de sincronizar; conserva en el teléfono las fichas que no correspondan.

Para publicarlo se requiere importar V026, subir los dos PHP del paquete y luego instalar el APK nuevo. El ZIP de fuentes permite abrir y compilar el proyecto en otro equipo; no sustituye la publicación en cPanel.
