# Revisión de conexiones de SIGD — 26/11/2026

## Lo corregido

| Pantalla o flujo | Causa observada | Corrección |
| --- | --- | --- |
| Inicio del colaborador: KPI entregados | Contaba solo evaluaciones oficiales aprobadas, aunque el servidor ya había recibido formularios | El servidor informa los tipos de formulario entregados en la semana actual; la app los muestra como recibidos y distingue la aprobación posterior |
| Inicio del colaborador: Mis asistencias | La tarjeta usaba el perfil de muestra y no consultaba Asistencias | Consulta `GET /asistencias/mias` para el mes actual y presenta días con marcación; muestra un estado de error si la consulta falla |
| Próxima entrega y turnos pendientes | Se tomaban únicamente asignaciones oficiales, sin la jornada exigible | El servidor calcula turnos pendientes y la próxima fecha a partir del horario vigente, permisos y entregas de Asistencias |
| Bienestar semanal | La última respuesta guardada aparecía como completada aunque perteneciera a una semana anterior | La tarjeta compara la clave de período ISO de la evaluación con la semana actual de Lima; la respuesta anterior sigue en el historial |
| Aviso KPI al administrador | El servidor guardaba la notificación, pero la app no generaba aviso del sistema | La pantalla principal de Administrador consulta avisos nuevos cada 30 segundos mientras está abierta y muestra notificación con vibración si Android lo permite |
| KPI de Programador Junior enviado sin aparecer en Inicio | Android guarda fechas con espacios, por ejemplo `25 / 11 / 2026`, y el servidor solo aceptaba `25/11/2026` | Ambos lectores del servidor aceptan el formato real: conteo de entregas y medición oficial. Las entregas previas de la semana se vuelven a contar al consultar. Los avisos antiguos no se generan retroactivamente |

## Comprobaciones realizadas

- Compilación de Android y pruebas unitarias: `:app:assembleDebug :app:testDebugUnitTest`, correctas.
- Pruebas del backend con base aislada y datos sintéticos: 834 comprobaciones correctas. No se conectaron a producción.
- Caso específico del KPI de Programador Junior con fecha `25 / 11 / 2026`: la medición devuelve las dos tareas y el lector reconoce el turno completado.
- Sintaxis PHP de `KpiFormsController.php`: correcta.
- La API pública respondió en `/api/v1/health` y las rutas protegidas comprobadas rechazaron solicitudes sin sesión con HTTP 401. Esto confirma que las rutas están disponibles, pero no verifica datos de una cuenta real ni que cPanel tenga la versión nueva.

## Publicación y verificación pendientes

1. El usuario confirmó la subida de `websiteplus_backend/src/Controllers/KpiFormsController.php`, `websiteplus_backend/src/Kpi/KpiFormCompletion.php` y `websiteplus_backend/src/Kpi/KpiFormMeasurement.php` a cPanel. Falta comprobar sus resultados con una cuenta real. No se requirió SQL nuevo.
2. El APK se instaló con `adb install -r` en el teléfono conectado. El usuario confirmó en su cuenta que el KPI entregado se cuenta, Mis asistencias muestra el dato y la evaluación semanal anterior figura disponible en la semana actual.
3. Con una entrega KPI real y autorizada, comprobar que aparece en la bandeja del administrador y que su teléfono vibra mientras mantiene abierta la pantalla principal y tiene habilitadas las notificaciones del sistema.

## Límite conocido de las notificaciones

La notificación de KPI **no llega de inmediato con la app cerrada o en segundo plano**. El servidor guarda el aviso, pero el proyecto todavía no tiene un servicio push ni las credenciales de un proyecto Firebase. La vibración también depende del permiso de Android y de la configuración del canal en el teléfono. Esta parte requiere configurar el servicio push y probarlo en el teléfono del administrador antes de considerarla terminada.

No se han borrado los KPI ni las evaluaciones de prueba de producción. Tampoco se ha probado el flujo completo con credenciales reales en el servidor.
