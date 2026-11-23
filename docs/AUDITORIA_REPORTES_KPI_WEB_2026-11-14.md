# Auditoría de reportes de `KPI-Web.zip` — 14 de noviembre de 2026

## Alcance comprobado

Se comparó el contenido de `app/src/main` del ZIP original con la integración local, byte por byte. De 271 archivos KPI Java, XML e imágenes relevantes, 243 son idénticos, 28 tienen cambios de integración y ninguno falta. Entre los modificados, `KpiReportsHubFragment.java` conserva el renderizador original; recibió solo tres líneas adicionales para intentar recuperar formularios del servidor al volver a la pantalla. Las 14 plantillas `kpi_view_official_*_sheet.xml` están presentes. No se copiaron los archivos `app/build` del ZIP.

El envío y la lectura de reportes completos por API ya existen. Una consulta realizada por el usuario en producción devolvió dos filas en `wp_kpi_formularios_detalle`. Una captura del teléfono comprobó que el administrador pudo abrir el JSON de un reporte de Programación desde el servidor. La captura no demuestra aún que la plantilla visual original se alimente del servidor.

## Correspondencia de formularios y plantillas originales

| Tipo de formulario | Plantilla del ZIP | Fuente que usa actualmente `KpiReportsHubFragment` |
| --- | --- | --- |
| `ADMINISTRACION` | `kpi_view_official_administrator_sheet.xml` | `KpiAdministratorStorageManager` |
| `ASISTENCIA_ADMIN` | `kpi_view_official_admin_assistant_sheet.xml` | `KpiAdminAssistantStorageManager` |
| `CONTABILIDAD` | `kpi_view_official_accountant_sheet.xml` | `KpiAccountantStorageManager` |
| `FOTOGRAFIA` | `kpi_view_official_photographer_sheet.xml` | `KpiPhotographerStorageManager` |
| `VIDEO` | `kpi_view_official_videographer_sheet.xml` | `KpiVideographerStorageManager` |
| `TRAFFICKER` | `kpi_view_official_trafficker_sheet.xml` | `KpiTraffickerStorageManager` |
| `GERENCIA` | `kpi_view_official_general_manager_sheet.xml` | `KpiGeneralManagerStorageManager` |
| `PROGRAMACION` | `kpi_view_official_programmer_sheet.xml` | preferencias locales del formulario |
| `DISENADOR_JUNIOR` | `kpi_view_official_junior_designer_sheet.xml` | `KpiJuniorMemoryRepository` |
| `COMMUNITY` | `kpi_view_official_community_sheet.xml` | `KpiCommunityStorageManager` |
| `DISENADOR_SENIOR` | `kpi_view_official_senior_sheet.xml` | `KpiSeniorStorageManager` |
| `DISENADOR` | `kpi_view_official_designer_sheet.xml` | `KpiDesignerMemoryRepository` |
| `VENTAS` | `kpi_view_official_sales_sheet.xml` | `KpiSalesWeekManager`, `KpiRouteDbHelper`, `KpiSeniorStorageManager` |

La plantilla adicional `kpi_view_official_junior_sheet.xml` es una ficha genérica de Junior; no corresponde a un tipo adicional de los 13 formularios.

## Defecto de integración reproducido

Las 14 funciones `renderOfficial*Sheet()` leen repositorios o preferencias **locales** y ninguna consulta la API. El administrador abre reportes de otros trabajadores mediante `GET /kpi/formularios`, pero esa ruta muestra el JSON sin pasar por las plantillas del ZIP. Por tanto, conservar los XML y guardar el JSON no completa la presentación original del reporte.

No se puede resolver cargando temporalmente los datos de otro trabajador en las preferencias del administrador: mezclaría cuentas y podría alterar historiales locales. Los renderizadores deben recibir el snapshot autorizado desde la API como fuente de solo lectura. Cada plantilla tiene campos y cálculos propios y requiere una adaptación explícita. La de Programación, además, contiene fechas de octubre de 2026 y un supervisor de muestra en su ruta local; esos valores no deben aparecer en un reporte recuperado del servidor.

El inventario del hub reveló otras rutas de ejemplo: una lista fija de semanas de agosto y septiembre de 2026; un historial que agrega seis semanas prefijadas; porcentajes del 100 % y metas numéricas fijas en varios cargos; nombres y contactos de ejemplo en Ventas. El botón original «Enviar Reporte» no llama a PHP: cambia un estado local y muestra un éxito dirigido a una persona fija. Para las sesiones reales se desactivó esa confirmación ficticia y se enlazaron envío/historial con la consulta de reportes confirmados; la impresión local se bloqueó hasta que la ficha imprimible reciba datos del servidor. Estas protecciones no equivalen a completar la presentación original de todos los cargos.

Se inició una adaptación específica de Programación: el lector administrativo puede inflar `kpi_view_official_programmer_sheet.xml` con un snapshot leído desde MySQL y sustituir fecha, turno, actividades, resumen, criterios y firmas. Esta adaptación local compila, pero todavía requiere inspección visual en el teléfono y revisión campo por campo. Los otros doce tipos siguen en el detalle estructurado y no deben presentarse como fichas originales terminadas.

Una captura del teléfono mostró que el selector de trabajadores agregado para reportes era un diálogo blanco ajeno al diseño de Dani. Se retiró: el botón de Reportes ahora abre la lista oscura de Personal con búsqueda y tarjetas. La ficha de Programación se abre desde la lista de registros confirmados.

La primera variante de esa navegación consumía el toque completo de la tarjeta para abrir reportes y bloqueaba el perfil. Tras la prueba del usuario se corrigió: el toque de la tarjeta vuelve a abrir el perfil y un botón «Reportes» dentro de la tarjeta abre los formularios recibidos. El APK se recompiló, pasó 171/171 pruebas unitarias y se instaló sobre el anterior. Falta la comprobación física de ambas acciones.

## Verificación necesaria por cada tipo

1. Abrir el formulario original, guardar un registro sintético con campos simples y anidados en laboratorio.
2. Confirmar el acuse y leer el mismo snapshot por API; comparar `registro_uid`, revisión y contenido.
3. Construir la ficha del XML correspondiente únicamente con ese snapshot y la identidad autorizada. Mostrar guiones para valores ausentes, sin nombres, fechas, puntuaciones ni firmas inventadas.
4. Comprobar visualmente totales, actividades, observaciones, firmas, cambio de día/turno y exportación si la plantilla los ofrece.
5. Repetir desde otra instalación y desde la cuenta administradora con `kpi.ver` sobre el trabajador, sin copiar los datos a la cuenta administradora.

Los cálculos del ZIP pueden presentarse como resumen de captura. No equivalen a una evaluación KPI oficial mientras `wp_kpi_definiciones`, asignaciones y evaluaciones sigan vacías. No se modificarán datos de producción para completar esta adaptación.
