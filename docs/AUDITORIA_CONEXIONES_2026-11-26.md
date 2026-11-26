# Revisión de conexiones de SIGD — 26/11/2026

## Lo corregido

| Pantalla o flujo | Causa observada | Corrección |
| --- | --- | --- |
| Inicio del colaborador: KPI entregados | Contaba solo evaluaciones oficiales aprobadas, aunque el servidor ya había recibido formularios | El servidor informa los tipos de formulario entregados en la semana actual; la app los muestra como recibidos y distingue la aprobación posterior |
| Inicio del colaborador: Mis asistencias | La tarjeta usaba el perfil de muestra y no consultaba Asistencias | Consulta `GET /asistencias/mias` para el mes actual y presenta días con marcación; muestra un estado de error si la consulta falla |
| Próxima entrega y turnos pendientes | Se tomaban únicamente asignaciones oficiales, sin la jornada exigible | El servidor calcula turnos pendientes y la próxima fecha a partir del horario vigente, permisos y entregas de Asistencias |
| Bienestar semanal | La última respuesta guardada aparecía como completada aunque perteneciera a una semana anterior | La tarjeta compara la clave de período ISO de la evaluación con la semana actual de Lima; la respuesta anterior sigue en el historial |
| Aviso KPI al administrador | El servidor guardaba la notificación, pero la app no generaba aviso del sistema | La app consulta avisos nuevos cada 30 segundos mientras SIGD está visible y muestra notificación con vibración si Android lo permite |
| KPI de Programador Junior enviado sin aparecer en Inicio | Android guarda fechas con espacios, por ejemplo `25 / 11 / 2026`, y el servidor solo aceptaba `25/11/2026` | Ambos lectores del servidor aceptan el formato real: conteo de entregas y medición oficial. Las entregas previas de la semana se vuelven a contar al consultar. Los avisos antiguos no se generan retroactivamente |
| Avisos para revisores autorizados | El servidor elegía destinatarios por rol 1 aunque la revisión se controla con `kpi.revisar` | Solo envía el aviso a otras cuentas activas que tienen permiso efectivo de revisión sobre el trabajador |
| Avisos con SIGD abierta | La consulta se detenía al salir del Inicio del Administrador | Se consulta mientras cualquier pantalla de SIGD permanezca visible; al tocar el aviso abre la bandeja correspondiente a la cuenta |
| Recordatorios de turnos | El aviso de las 14:00 y 18:40 miraba si había turno, incluso si ya estaba entregado | Consulta los turnos pendientes de hoy en el servidor antes de notificar |
| Primeros puestos del resumen administrativo | Incluía personal sin puntos y podía incluir la ficha de Gerencia; además tomaba el mes de aprobación, aunque el Ranking usa el mes evaluado | Usa el mes, el porcentaje ponderado, la integridad del reporte y el orden del Ranking; excluye la ficha de Gerencia |

## Comprobaciones realizadas

- Compilación de Android y pruebas unitarias: `:app:assembleDebug :app:testDebugUnitTest`, correctas.
- Pruebas del backend con base aislada y datos sintéticos: 837 comprobaciones correctas. Incluyen destinatarios permitidos y denegados, además del turno de hoy ya entregado. No se conectaron a producción.
- Caso específico del KPI de Programador Junior con fecha `25 / 11 / 2026`: la medición devuelve las dos tareas y el lector reconoce el turno completado.
- Sintaxis PHP de los dos controladores modificados: correcta.
- La API pública respondió en `/api/v1/health` y las rutas protegidas comprobadas rechazaron solicitudes sin sesión con HTTP 401. Esto confirma que las rutas están disponibles, pero no verifica datos de una cuenta real ni que cPanel tenga la versión nueva.

## Publicación y verificación pendientes

1. El usuario confirmó la subida de `KpiFormCompletion.php` y `KpiFormMeasurement.php`; las tres pantallas afectadas ya se comprobaron con una cuenta real. Para los cambios de esta segunda revisión falta reemplazar **solo** `websiteplus_backend/src/Controllers/KpiFormsController.php` y `websiteplus_backend/src/Controllers/DashboardController.php` con el paquete nuevo. No se requiere SQL.
2. El APK de esta revisión se instaló con `adb install -r` en el teléfono conectado, sin borrar sus datos. El usuario ya había confirmado con la versión anterior que el KPI entregado se cuenta, Mis asistencias muestra el dato y la evaluación semanal anterior figura disponible en la semana actual. Los avisos nuevos de esta revisión aún necesitan comprobación con una entrega real.
3. Instalar el APK nuevo también en el teléfono del administrador. Con una entrega KPI real y autorizada, comprobar que aparece en su bandeja y que el teléfono vibra mientras SIGD esté abierta y tenga habilitadas las notificaciones del sistema. No se debe crear una entrega ficticia en producción solo para esta prueba.

## Límite conocido de las notificaciones

La notificación de KPI **no llega de inmediato con la app cerrada o en segundo plano**. El servidor guarda el aviso, pero el proyecto todavía no tiene un servicio push ni las credenciales de un proyecto Firebase. La vibración también depende del permiso de Android y de la configuración del canal en el teléfono. Esta parte requiere configurar el servicio push y probarlo en el teléfono del administrador antes de considerarla terminada.

El análisis Android Lint encontró 512 errores de traducciones faltantes para inglés y portugués, además de avisos de diseño. No son fallos de conexión ni impiden la compilación, pero no se considera que Lint esté limpio. Las pruebas aisladas tampoco sustituyen una prueba autenticada de todos los roles contra cPanel.

No se han borrado los KPI ni las evaluaciones de prueba de producción. Tampoco se ha probado el flujo completo con credenciales reales en el servidor.
