# Publicar cronograma KPIWeb

Paquete: `C:\Users\Jair L.G\Downloads\KPIWeb_CRONOGRAMA_FINAL_20261121`.

## Antes de subir

Guardar una copia nueva de `wsnet_websiteplus` y descargar los PHP vigentes `public_html/websiteplus-api/index.php`, `websiteplus_backend/src/Controllers/KpiOfficialAdminController.php` y, si existe, `websiteplus_backend/src/Controllers/ScheduleController.php`. Conservarlos fuera de la carpeta de publicación. No borrar ni vaciar tablas. Los respaldos de noviembre 14 son anteriores a estos cambios.

## Archivos y destino

| Archivo del paquete | Destino |
|---|---|
| `SQL/01_APLICAR_V025_HORARIOS.sql` | Importar una vez en phpMyAdmin, base `wsnet_websiteplus` |
| `SQL/02_CARGAR_13_PLANTILLAS_12_CARGOS.sql` | Importar una vez en la misma base, después de 01 |
| `Controllers/ScheduleController.php` | `websiteplus_backend/src/Controllers/ScheduleController.php` |
| `Controllers/KpiOfficialAdminController.php` | Reemplazar el mismo nombre en `websiteplus_backend/src/Controllers/` |
| `public/index.php` | Reemplazar `public_html/websiteplus-api/index.php` |
| `APK/KPIWeb_cronograma_debug.apk` | Instalar sobre la app existente sin borrar sus datos, solo después de publicar la API |

No subir carpetas `.git`, `build`, `local.properties`, el ZIP original ni el SQL a `public_html`. No cambiar `config.php` ni `.htaccess`.

## Comprobación

En phpMyAdmin ejecutar solo:

```sql
SELECT COUNT(*) AS plantillas FROM wsnet_websiteplus.wp_horarios_plantillas;
SELECT COUNT(*) AS horarios_publicados FROM wsnet_websiteplus.wp_horarios_cargo WHERE publicado=1;
SELECT COUNT(*) AS v025 FROM wsnet_websiteplus.wp_migraciones WHERE version='V025';
```

Se esperan `13`, `12` y `1`. En el APK, una cuenta activa abre **Menú → Horario de actividades diarias** y ve el horario de su cargo. El Administrador abre **Menú → Editor de Horarios**, selecciona un cargo, guarda un cambio y comprueba que sigue al volver a abrir. Los turnos de cada trabajador se eligen desde Editor de Horarios → menú → «Asignar turno a un trabajador»; el ZIP no contenía esas asignaciones personales.

Un trabajador nuevo con un cargo ya existente hereda su horario por `id_cargo`. Las cinco plantillas sin cargo actual se guardan para el futuro. Si se crea un cargo cuyo nombre coincida exactamente con una plantilla, su horario estará disponible; si el nombre difiere, el Administrador puede vincularla desde el menú del editor. El estado inactivo de Asistencias bloquea la consulta del horario sin borrar sus datos.

Si falla un PHP nuevo, restaurar las copias PHP previas. Mantener las tablas y los datos mientras se diagnostica el error; no usar un `DROP TABLE` automático.
