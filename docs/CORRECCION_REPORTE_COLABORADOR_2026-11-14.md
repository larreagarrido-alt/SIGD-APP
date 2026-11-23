# Reporte KPI en colaboradores y practicantes

## Fallo observado

Al tocar «Reporte» desde un formulario, la aplicación mostraba una pantalla blanca y regresaba al inicio. El registro Android del teléfono mostró `IllegalArgumentException: No view found for id ... id/main` al crear `KpiReportsHubFragment`. Los formularios heredados abrían el reporte en `R.id.main`, pero `CollaboratorKpiIntegrationActivity` montaba el formulario en otro identificador.

## Corrección

- El contenedor de KPI de colaboradores y practicantes ahora usa `R.id.main`, el destino que comparten los 13 formularios. El usuario confirmó en el teléfono que «Reporte» abre después de instalar el APK corregido.
- El diálogo común de envío ya no muestra el oso, no afirma que se abonaron puntos ni que el servidor selló un reporte antes de recibir confirmación. Indica que la ficha quedó guardada en el dispositivo y que el estado del servidor debe consultarse en Reportes.
- Los dos métodos de sonido de éxito de KPI quedaron silenciosos. Se conservó el sonido de los recordatorios programados.
- El enlace de Community Manager dejó de usar un nombre fijo de ejemplo y toma la identidad autenticada.

`app:testDebugUnitTest` pasó 171 pruebas; `app:assembleDebug` compiló. El APK se instaló con `adb install -r` sin borrar datos. La ruta de Reporte se comprobó en el teléfono; la ausencia del sonido y del oso tras un nuevo envío aún debe comprobarse durante un envío legítimo, sin generar datos de prueba en producción.
