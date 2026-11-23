# Publicación controlada de Bienestar — 14 de noviembre de 2026

## Estado y alcance

La captura de phpMyAdmin muestra que V020 amplió `wp_bienestar_tests.periodicidad` y registró la versión. V020 **no** crea preguntas ni concede acceso. El respaldo descargado `C:\Users\Jair L.G\Downloads\WellnessController.php` mide 8281 bytes y su SHA-256 es `246c0b3804fefd03ed4925d14f773d41f09b659ae09de68832c55c2aa99c869f`. El PHP candidato difiere de ese archivo en una sola línea: cálculo del período `SEMANAL`. Conservar el respaldo descargado fuera del paquete de publicación.

El candidato incorpora las 6 preguntas semanales y 10 mensuales, con 5 opciones cada una, del `KPI-Web(dani).zip`. No incluye Ryff, resultados clínicos, puntuación KPI ni lectura privada de respuestas por administración. El permiso propuesto permite responder **solo cuestionarios propios** a los roles existentes 1, 5 y 6. Se probó en MariaDB aislado: 692 comprobaciones completas y 111 con V012 ausente; ninguna prueba escribió en cPanel.

La consulta de producción aportada por el usuario confirmó V020=1 y cero cuestionarios, preguntas, opciones y concesiones de `bienestar.responder`. El respaldo actual descargado a las 12:43 (`wsnet_websiteplus.sql`, 436187 bytes, SHA-256 `8a265a1fb52fca7d13dbd1c9b83a6f1dab88ffd9df222fe3b7a06998678c44bc`) fue restaurado en MariaDB local: 40 tablas, 9 vistas y 19 versiones registradas. Sobre una **segunda restauración aislada** de ese mismo archivo, V021 y el grant pasaron con 2 cuestionarios, 16 preguntas, 80 opciones, 3 permisos `PROPIO` y 0 permisos privados. El dump no incluye los roles de la base de asistencia; la prueba añadió únicamente tres roles sintéticos al laboratorio para validar el grant. El archivo SQL original no se copió al proyecto ni al paquete.

## Antes de publicar

1. Conservar el archivo PHP descargado y comprobar que su hash sigue siendo el indicado. No confiar en `src.zip`: no contiene este controlador.
2. Exportar un respaldo **actual** y restaurable de `wsnet_websiteplus` después de V020, manteniéndolo fuera del proyecto y del APK. Verificar la restauración en una instancia aislada. No sobrescribir el respaldo anterior.
3. Ejecutar la consulta `SELECT` de `PROPUESTA_ACTIVACION_BIENESTAR_DANI_2026-11-14.md`: confirmar V020=1 y revisar los conteos de cuestionarios, preguntas, opciones y concesiones. Si ya hay datos o permisos, detenerse y comparar antes de importar.
4. Verificar por `information_schema.COLUMNS` que `periodicidad` incluye `SEMANAL` y que `wp_bienestar_tests`, `wp_bienestar_preguntas`, `wp_bienestar_opciones` y `wp_migraciones` tienen las columnas que usa V021. Probar V021 y el grant sobre copia de staging actual antes de producción.

## Orden de instalación

1. Reemplazar únicamente `websiteplus_backend/src/Controllers/WellnessController.php` con `websiteplus_backend/src/Controllers/WellnessController.php` del candidato. Mantener intactos los demás PHP y `public_html/websiteplus-api/index.php`.
2. Verificar `/health` y que `GET /bienestar/tests` sin token continúe devolviendo 401. Con una cuenta autorizada aún sin permiso, 403 sigue siendo esperado.
3. Importar **una sola vez** `database/migrations/V021__bienestar_dani_cuestionarios.up.sql` en `wsnet_websiteplus`. Debe registrar V021, 2 cuestionarios, 16 preguntas y 80 opciones nuevas. Si cualquiera de los códigos ya existe, detenerse para revisar; no usar `INSERT IGNORE` ni sobrescribir filas.
4. Importar `database/proposals/bienestar_responder_roles_1_5_6.sql`. Debe mostrar `bienestar.responder` con `PROPIO` para roles 1, 5 y 6; no concede `bienestar.ver_privado`. Respeta cualquier denegación individual existente.
5. Cerrar y volver a abrir Bienestar en el APK instalado. El catálogo debe mostrar evaluación semanal y mensual. Solo el titular autorizado debe responder; no crear respuestas ficticias en producción.

## Verificación posterior y reversión

Ejecutar consultas de solo lectura para contar V020/V021, los dos códigos de cuestionario, sus 16 preguntas, 80 opciones y los tres grants. Probar 401 sin token, 403 en un rol sin permiso, y `GET /bienestar/tests` con una cuenta autorizada. La escritura real se prueba solo con respuestas voluntarias de su titular; verificar acuse y su historial, nunca leer respuestas privadas desde otra cuenta. Confirmar que KPI, login y `/auth/me` continúan operativos.

Si el PHP causa un error, restaurar **solo** el `WellnessController.php` respaldado. Si el contenido debe dejar de ofrecerse, poner `activo=0` únicamente a los dos códigos nuevos después de respaldar y revisar cualquier respuesta ya recibida. No borrar cuestionarios, respuestas ni V020/V021 automáticamente. Si se revocan los grants, retirar solo los tres creados por esta operación tras confirmar que no existían antes. Mantener la estructura `SEMANAL` si ya hay registros que la usan.

Los dos SQL y el controlador se entregan por separado dentro del ZIP candidato; no incluyen `.env`, claves, exportaciones, respuestas ni respaldos.
