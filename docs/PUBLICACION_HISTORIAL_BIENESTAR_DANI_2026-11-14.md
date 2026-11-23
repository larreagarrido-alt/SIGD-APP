# Publicación controlada: historial visual de Bienestar

## Estado verificado

- La APK instalada conserva las pantallas de Dani. En el teléfono se comprobó la vista «Mis evaluaciones» y el avance pregunta por pregunta.
- El guardado y el historial básico de Bienestar ya funcionan en producción con V020/V021 y los permisos existentes. No se requiere otra migración ni cambiar cuentas, cargos o permisos.
- La ruta privada `GET /bienestar/mis-respuestas/{id}` fue publicada por el usuario. La consulta externa sin sesión devuelve 401, como corresponde. El usuario confirmó que su evaluación semanal abre una tarjeta con respuestas recuperadas del servidor.
- El servidor comprueba `bienestar.responder`, el trabajador de la sesión y el propietario de la evaluación. Responde 404 para evaluaciones ajenas y `Cache-Control: no-store`.
- Pruebas de laboratorio aislado: 695 comprobaciones PHP/HTTP/MySQL; 112 con V012 y `wp_rostros_cuenta` ausentes. Android: 172 pruebas sin fallos y compilación APK correcta. La apertura automática del resultado mensual tras guardar y la confirmación KPI con tono se incluyeron en el último APK; requieren comprobación física con un registro real, sin generar datos ficticios.

## Archivos exactos

Origen en `Descargas/KPIWeb_BIENESTAR_HISTORIAL_20261114`:

1. `WellnessController.php` → `websiteplus_backend/src/Controllers/WellnessController.php`.
2. `index.php` → `public_html/websiteplus-api/index.php`.
3. `KPIWeb_Bienestar_Dani_20261114.apk` → instalar en el teléfono con actualización, sin borrar datos.

El `index.php` solo añade la ruta privada anterior respecto del paquete PHP publicado. `WellnessController.php` conserva la corrección del HTTP 500 por `DELETE` denegado e incorpora solo la lectura privada de detalle. No subir `.htaccess`, migraciones ni SQL.

## Secuencia segura

1. Descargar desde cPanel **los dos archivos PHP actuales** a una carpeta de respaldo fechada, y comprobar que tienen contenido. Conservar además el respaldo SQL existente.
2. Subir `WellnessController.php` y luego `index.php` a las rutas exactas. Comprobar que ambos quedaron con los tamaños y hash del paquete local. No mezclar con el `index.php` del paquete de publicación anterior.
3. Abrir la APK instalada con la cuenta dueña de una evaluación. En Bienestar, abrir «Mi evolución», seleccionar la evaluación mensual guardada y comprobar que muestra su resultado y sugerencias. No compartir capturas con respuestas personales.
4. Comprobar con otra cuenta autorizada que ve únicamente su propio historial. El backend debe rechazar la consulta de un ID ajeno.
5. Si aparece un fallo, reponer **ambos** PHP descargados en el paso 1. No revertir V020/V021 ni borrar respuestas, sesiones o cuentas. Mantener la APK sin borrar datos mientras se diagnostica.

El módulo Ryff sigue condicionado a una invitación y un cuestionario validado; no se presentan resultados de demostración como resultados personales. Las sugerencias mensuales son orientación local calculada desde respuestas propias recuperadas del servidor; no son diagnóstico ni puntuaciones KPI.
