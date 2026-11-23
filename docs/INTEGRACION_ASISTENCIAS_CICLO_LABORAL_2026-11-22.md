# Integración con Asistencias: ciclo de una misma cuenta

Asistencias es el origen de `trabajadores`, `usuarios`, cargo, tipo, estado, horarios, vacaciones, permisos, faltas, justificaciones y descansos por horas extra. KPIWeb los consulta; no crea una segunda cuenta ni modifica esas tablas. Los reportes KPI ya confirmados conservan el cargo con que se enviaron.

| Cambio en Asistencias | Comportamiento de KPIWeb |
| --- | --- |
| Alta de trabajador con cuenta | La cuenta existente inicia sesión en la app; el cargo actual determina el formulario. |
| Practicante → colaborador | La app lee el nuevo tipo, cargo y rol al consultar el perfil. KPI usa la asignación vigente de `horarios_trabajadores` y respeta `dias_asistencia`. |
| Colaborador → practicante | La misma cuenta conserva su identidad e historial. KPI vuelve a leer `horario_practicantes`; no toma un horario antiguo de colaborador si falta un día de prácticas. |
| Cambio de horario o días | La siguiente consulta KPI toma el horario actual de Asistencias. Los descansos, permisos y vacaciones aprobados no cuentan como turnos exigibles. |
| Inactivo → activo | Inactivo impide el acceso; la reactivación permite volver a entrar con la misma cuenta. |

Para convertir una cuenta, el operador de Asistencias debe actualizar **en la misma operación de negocio** `trabajadores.tipo`, el cargo si cambia, el rol de `usuarios` cuando corresponda (5 colaborador / 6 practicante), y el horario laboral vigente del nuevo tipo. El editor corregido guarda tipo, cargo, rol e historial de vigencia en una transacción. Al volver a practicante se debe revisar expresamente `horario_practicantes`; un horario antiguo puede seguir almacenado aunque los días de clase hayan cambiado. El ZIP de horarios de KPIWeb contiene actividades por cargo, no sustituye los turnos personales de Asistencias.

Al revisar el código local de Asistencias del 19/11 encontramos que `mi_perfil` aceptaba `tipo` enviado por POST aunque el selector visual estuviera deshabilitado; el editor administrativo actualizaba `usuarios.id_rol` y `trabajadores.tipo` usando conexiones distintas y podía cambiar a rol 5 una cuenta administrativa. Se prepararon correcciones para conservar el tipo en el perfil propio y actualizar tipo y rol juntos en el editor. El 22/11 se compararon los dos PHP descargados de cPanel en `Descargas/respaldo` con la copia local y sus hashes SHA-256 coincidieron exactamente.

El respaldo recibido no contiene fechas efectivas históricas de cada conversión de tipo. La nueva tabla `tipo_vigencias` empieza con el tipo conocido en la fecha de publicación en Lima y registra los cambios futuros del editor administrativo. El calendario usa el tipo registrado para **cada día** y no califica un período anterior al primer registro. `wp_eventos_laborales` conserva solo la fecha en que KPIWeb observó un cambio antiguo, que puede ser posterior a su fecha real. Para evaluar períodos anteriores al nuevo historial se necesita una revisión documentada de Asistencias; el sistema no inventa las fechas.

La auditoría de solo lectura está en `database/proposals/auditoria_integracion_asistencias_20261122.sql`. En el respaldo local del 21/11 hay cuatro asignaciones con `dias_asistencia`; no había cuentas de roles 5/6 invertidos. Esta observación no afirma el estado actual de producción.

Verificación sintética final: `backend/tests/integration.php` pasó 793 comprobaciones en una base aislada el 22/11/2026; incluyó ida y vuelta de tipo laboral, historial por fecha, horarios específicos, vacaciones y reactivación. Los cinco PHP pasaron comprobación de sintaxis y el SQL de instalación se ejecutó en una base aislada. Falta la prueba con datos actuales de producción después de publicar.
