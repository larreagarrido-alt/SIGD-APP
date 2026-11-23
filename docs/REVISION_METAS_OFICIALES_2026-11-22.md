# Metas oficiales y evaluación automática — 22/11/2026

Los 13 formularios del cronograma tienen una regla de cálculo en `KpiOfficialScore`. El servidor toma las cantidades guardadas en el reporte confirmado, compara con la meta del cargo y calcula porcentaje y puntos. El porcentaje o los puntos enviados por el teléfono no sirven para calificar. El administrador abre el reporte, comprueba si es válido y lo aprueba u observa; no escribe un ID de indicador ni una cifra de producción.

La meta se ajusta a los turnos exigibles que confirma Asistencias. Un período sin turnos verificables o con una incidencia de calendario no se califica. Ventas suma los seis reportes diarios de la semana en una evaluación. Gerencia conserva su KPI propio, pero queda fuera del ranking. Los niveles comunes son 90/80/60/40 % y los puntos aprobados son 20/17/14/12/10.

La carga inicial de producción cubre los **11 pares cargo–formulario ya aprobados**. Las otras plantillas permanecen disponibles para cargos futuros; una regla se crea cuando exista un mapeo aprobado y el cargo activo coincida en Asistencias. La base real mostraba 0 definiciones antes de importar esta carga, por lo que esta función **aún no está publicada ni comprobada en producción**.

La meta automática está definida en código del servidor. Editar una cifra en la ficha visual del cronograma **no cambia por sí solo** la calificación oficial. Para cambiar metas hay que actualizar y publicar conjuntamente la regla del servidor y la ficha, con nueva prueba. Los colaboradores freelance sin turnos de Asistencias todavía requieren una regla verificable por entregables; actualmente se bloquea su calificación en lugar de inventar una jornada. Jefe de Agencia tiene horario de Diseñador Senior, pero no figura en los 11 mapeos KPI aprobados; no se le asigna una regla sin ese mapeo.

La prueba local aislada pasó 821 comprobaciones y Android compiló con sus pruebas unitarias. Falta aplicar V029 y la carga de metas, y comprobar el flujo con reportes reales en producción antes de afirmar que funciona al 100 %.
