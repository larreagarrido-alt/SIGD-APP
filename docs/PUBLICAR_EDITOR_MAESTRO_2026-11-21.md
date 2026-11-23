# Publicar Editor Maestro compartido

Paquete en `Descargas/KPIWeb_EDITOR_MAESTRO_FINAL_20261121`. No sustituir los PHP de Horarios ni Bienestar.

## Respaldo

Exportar una copia nueva de `wsnet_websiteplus` justo antes de importar V026 y descargar de cPanel el `public_html/websiteplus-api/index.php` vigente antes de reemplazarlo. El `index.php` de este paquete parte del publicado para Horarios y solo agrega las rutas de plantillas. Mantener la copia del archivo anterior para revertirlo si fuese necesario.

## Publicación

1. En phpMyAdmin, base `wsnet_websiteplus`, importar `SQL/01_APLICAR_V026_PLANTILLAS_KPI.sql` una vez.
2. Subir `Controllers/KpiTemplateController.php` a `websiteplus_backend/src/Controllers/`.
3. Reemplazar `public_html/websiteplus-api/index.php` con `public/index.php` del paquete.
4. Instalar `APK/KPIWeb_editor_maestro_debug.apk` sobre la app existente, sin desinstalarla ni borrar sus datos. El APK no se sube a cPanel.

Este APK también permite ampliar con pellizco o con `− / 100% / +` las fichas KPI recibidas y el Centro de Reportes. Antes de enviar una ficha comprueba que el tipo corresponda al cargo real de Asistencias. Una ficha de otro cargo queda conservada localmente y no se envía como si fuera un KPI válido.

## Comprobación

En phpMyAdmin ejecutar:

```sql
SELECT COUNT(*) AS v026 FROM wsnet_websiteplus.wp_migraciones WHERE version='V026';
SELECT COUNT(*) AS estado FROM wsnet_websiteplus.wp_kpi_plantillas_estado WHERE id=1;
```

Ambos valores deben ser 1. En el Administrador, abrir Editor Maestro, modificar un texto sin datos personales, guardar y volver a abrir; el valor debe seguir. Una segunda cuenta autorizada debe ver el cambio en un formulario nuevo del cargo correspondiente. Las semanas históricas y los reportes confirmados no se reescriben.

Si falla la API, restaurar el `index.php` anterior y retirar el controlador nuevo; conservar las tablas y el respaldo SQL para revisar el problema sin borrar reportes.
