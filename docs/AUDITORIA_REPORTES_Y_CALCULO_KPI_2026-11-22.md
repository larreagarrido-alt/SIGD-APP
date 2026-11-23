# Reportes y cálculo KPI — revisión del 22/11/2026

## Accesos del administrador

- **Reportes** en la barra inferior: lista de formularios recibidos del servidor. Al tocar una fila abre la ficha visual. No ofrece configuración ni aprobación.
- **Gestión de KPI** en el Centro de gestión: indicadores oficiales, metas, asignaciones, revisión, aprobación y cierre. Conserva la lista de reportes para vincularlos a una evaluación.
- Se retiró la tarjeta **Centro de reportes** del Centro de gestión porque repetía el acceso de la barra inferior.
- **Editor de fichas KPI** cambia la plantilla visible por cargo. Sus textos y metas de pantalla no son filas de `wp_kpi_definiciones`.

## Lo que sí calcula el APK

El enrutador `CollaboratorKpiIntegrationActivity` abre 13 tipos autorizados: `VENTAS`, `DISENADOR`, `DISENADOR_JUNIOR`, `DISENADOR_SENIOR`, `GERENCIA`, `PROGRAMACION`, `ADMINISTRACION`, `ASISTENCIA_ADMIN`, `CONTABILIDAD`, `COMMUNITY`, `FOTOGRAFIA`, `TRAFFICKER` y `VIDEO`. Las fichas y modelos del módulo KPI calculan porcentajes o niveles **para mostrar el avance**. Las nueve imágenes aportadas corresponden a PROGRAMACION, TRAFFICKER, VIDEO, ADMINISTRACION, ASISTENCIA_ADMIN, COMMUNITY, CONTABILIDAD, FOTOGRAFIA y GERENCIA. Los otros cuatro tipos también están en el código y en el enrutador.

Estas imágenes muestran ejemplos de fichas completadas al 100 %, no resultados que se deban copiar literalmente al sistema. El cálculo esperado se hace con la meta del indicador y el resultado registrado: 2 videos realizados frente a una meta de 4 equivalen al 50 %. Para una evaluación oficial, el servidor debe obtener esos valores de los campos de la ficha y usar la meta vigente del cargo y período. Las imágenes no sustituyen los datos reales enviados por el trabajador.

## Lo que todavía no ocurre automáticamente en producción

`KpiFormsController::sync` confirma y guarda el JSON de la ficha. No crea una evaluación oficial. En `KpiOfficialAdminController::review` la app aún solicita `id_kpi`, fecha, `valor_verificado` y observación. `KpiController::calificar` calcula porcentaje y puntos en el servidor **después** de esa revisión y únicamente con una definición aprobada y una asignación vigente; ajusta la meta numérica por turnos exigibles de Asistencias. La última auditoría compartida de producción mostró 0 definiciones aprobadas, 0 asignaciones y 0 evaluaciones aprobadas. No se ha comprobado un cambio posterior.

Por tanto, el cálculo visual automático de las 13 fichas existe, pero la puntuación oficial automática de extremo a extremo todavía no está lista. El administrador debería confirmar la validez de la ficha; el valor, porcentaje y puntos deben salir de un cálculo del servidor. La escala porcentual común 90/80/60/40 y los puntos 20/17/14/12/10 fueron confirmados. Falta conectar cada indicador y meta vigente con los campos correspondientes de los 13 JSON y retirar `valor_verificado` de la revisión.
# Actualización del 22/11/2026: cálculo desde el reporte

La revisión administrativa ya no solicita `valor_verificado`. El servidor extrae cantidades de los campos confirmados de los 13 formularios, compara cada cantidad con la meta numérica oficial y presenta una vista previa del porcentaje y los puntos. Después de aprobar, la API vuelve a calcular con los turnos exigibles de Asistencias y acredita puntos. El detalle de publicación y límites está en `PUBLICACION_KPI_AUTOMATICO_2026-11-22.md`. Las referencias históricas a `valor_verificado` anteriores a esta nota describen el código previo a la corrección.

