# Personal y Reportes: revisión del 26 de noviembre de 2026

## Qué se corrigió

- Personal consulta el estado actual de Asistencias y muestra solo trabajadores activos. Al reactivar una cuenta allí, vuelve a aparecer en la app. El filtro «Inactivos» se retiró.
- La ficha de Personal usa datos del servidor: foto, fecha de inicio del vínculo vigente, indicadores oficiales, formularios recibidos, evaluaciones aprobadas e historial. Una ficha recibida todavía no se presenta como evaluación aprobada.
- Las fotos de perfil se piden en tamaño pequeño y la app conserva una copia temporal en memoria para que las listas carguen más rápido.
- Reportes destaca el nombre del trabajador, cargo, fecha de la ficha, estado de revisión y porcentaje. El estado de entrega se distingue del porcentaje calculado y del resultado aprobado.
- Programación informa cuántas tareas entregadas son del tipo exigido ese día y cuál es la meta aplicable. Una tarea de otro tipo no suma a esa meta. Las metas se ajustan a los turnos exigibles desde Asistencias.
- Si un reporte tiene más de una revisión, la lista muestra el estado y resultado de la revisión más reciente.

## Publicación

El cambio de Personal y Reportes requiere un APK nuevo y estos tres PHP del repositorio en sus mismas rutas de cPanel:

1. `websiteplus_backend/src/Controllers/PersonalController.php`
2. `websiteplus_backend/src/Controllers/KpiOfficialAdminController.php`
3. `websiteplus_backend/src/Kpi/KpiOfficialScore.php`

Si aún no se publicó el paquete anterior de conexiones, subir también `websiteplus_backend/src/Controllers/KpiFormsController.php` y `websiteplus_backend/src/Controllers/DashboardController.php`. El paquete de Descargas reúne los cinco archivos. Esta entrega no necesita SQL.

## Comprobación de entrega

La suite del backend pasó 840 comprobaciones con una base sintética y sin acceder a producción. El APK se compiló y pasaron sus pruebas unitarias. La comprobación final en la cuenta real debe hacerse tras subir los PHP: abrir Personal, una ficha individual y Reportes con una ficha ya existente. No hace falta enviar un KPI de prueba ni borrar registros.

La aprobación administrativa sigue siendo necesaria para convertir el cálculo en puntos oficiales. La app no debe mostrar un 0 % aprobado solo porque la ficha fue recibida.
