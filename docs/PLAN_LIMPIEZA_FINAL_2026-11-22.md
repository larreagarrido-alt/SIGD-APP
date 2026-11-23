# Limpieza final de datos de prueba

## Alcance confirmado

El usuario confirmó que los 10 reportes de `wp_kpi_formularios_detalle`, las evaluaciones de Bienestar 7, 8 y 9 con 26 respuestas y el único premio del catálogo son pruebas. Se retirarán al cerrar la integración y después de un respaldo nuevo de `wsnet_websiteplus`. Todavía no se ha ejecutado ninguna eliminación.

Se conservan cuentas, trabajadores, Asistencias, horarios, plantillas, cuestionarios, permisos, migraciones y configuraciones. El inventario compartido mostró cero vínculos KPI, evaluaciones oficiales, puntos, cierres, canjes, campañas, planes, actividades y notificaciones. Esos conteos se deben volver a comprobar en el momento del cierre.

## Orden para la entrega

1. Terminar la integración del diseño y publicar el conjunto final de APK y PHP.
2. Pausar envíos durante el corte. Exportar un respaldo actual de la base y comprobar que el archivo existe y tiene contenido.
3. Ejecutar `database/verification/AUDITORIA_PRELIMPIEZA_FINAL_20261122.sql`. Revisar que los conteos e IDs coincidan con el alcance autorizado. Si hay registros nuevos, detener la limpieza y distinguir cuáles son reales.
4. Preparar una eliminación transaccional limitada a los IDs identificados. Primero retirar dependencias vinculadas, si las hay. Las respuestas de Bienestar tienen borrado en cascada al eliminar las evaluaciones. La imagen de un premio necesita revisión separada: eliminar el premio no elimina el archivo de `wp_archivos` ni el archivo físico.
5. Verificar cero reportes de prueba, cero evaluaciones 7–9, cero respuestas de esas evaluaciones y cero premios de prueba; conservar el respaldo fuera del proyecto.
6. En teléfonos usados para las pruebas, evitar que el buzón local vuelva a enviar reportes borrados. Revisar los borradores locales antes de limpiar los datos de la app; limpiar datos también elimina la sesión y el registro biométrico local.

No ejecutar un `TRUNCATE` general ni borrar registros nuevos que no hayan sido identificados como pruebas.
