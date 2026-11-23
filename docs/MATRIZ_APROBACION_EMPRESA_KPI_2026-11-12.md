# Correspondencia empresarial pendiente para los 13 formularios

Esta matriz distingue el **tipo técnico existente** de la autorización oficial. En la estructura de producción comunicada, `wp_kpi_definiciones`, `wp_kpi_asignaciones` y `wp_kpi_evaluaciones` contienen cero filas. Por tanto, ningún `id_cargo`, código KPI, meta, peso o período puede darse por aprobado a partir de las pantallas Android. La tabla `wp_kpi_formulario_cargos` de V018 tampoco está publicada ni cargada.

| Formulario conservado | `tipo_formulario` usado por Android/PHP | Cargo que la empresa debe confirmar en `cargos` | Definición, código, meta, peso y período oficiales |
| --- | --- | --- | --- |
| Ventas | `VENTAS` | `id_cargo` y `cargo_nombre` por confirmar | Pendientes |
| Diseño gráfico | `DISENADOR` | Por confirmar | Pendientes |
| Diseño junior | `DISENADOR_JUNIOR` | Por confirmar | Pendientes |
| Diseño senior | `DISENADOR_SENIOR` | Por confirmar | Pendientes |
| Gerencia general | `GERENCIA` | Por confirmar | Pendientes |
| Programación | `PROGRAMACION` | Por confirmar | Pendientes |
| Administración | `ADMINISTRACION` | Por confirmar | Pendientes |
| Asistencia administrativa | `ASISTENCIA_ADMIN` | Por confirmar | Pendientes |
| Contabilidad | `CONTABILIDAD` | Por confirmar | Pendientes |
| Community manager | `COMMUNITY` | Por confirmar | Pendientes |
| Fotografía | `FOTOGRAFIA` | Por confirmar | Pendientes |
| Trafficker | `TRAFFICKER` | Por confirmar | Pendientes |
| Video | `VIDEO` | Por confirmar | Pendientes |

La empresa debe entregar por cada correspondencia: `id_cargo` de la tabla real `cargos`, nombre exacto, tipo de formulario permitido, responsable que aprueba y fecha de vigencia. Para convertir un reporte en evaluación oficial se requiere además el código KPI, categoría, fórmula homologada, meta, peso, periodicidad, reglas de evidencia/firma, alcance por trabajador y período, y aprobación de puntos. **No se deriva automáticamente una evaluación ni se conceden puntos de un JSON de formulario.**

El laboratorio sintético asignó los 13 tipos a un único `id_cargo=1` creado exclusivamente en su base temporal para comprobar autorización, lectura y escritura. Ese fixture no es una propuesta para producción ni identifica cargos reales. Sin correspondencia aprobada, el endpoint rechaza el envío con `FORM_NOT_ASSIGNED` y Android conserva el pendiente.

## Estado técnico local y límites

- V018 conserva el JSON completo por `id_trabajador`, tipo, UID y revisión; la lectura paginada y por UID permite reconstrucción y acuse exacto.
- V019 agrega borradores KPI privados por `id_usuario` para administradores con `kpi.administrar`; no crea definiciones, asignaciones ni evaluaciones oficiales. Un borrador no concede acceso a formularios ni modifica el alcance de otro administrador.
- Android recupera registros autorizados al ingresar a KPI y reconstruye los repositorios originales. Conserva capturas locales no confirmadas y aplica una revisión remota posterior cuando conoce la revisión previamente confirmada.
- La cola nueva se aísla por `id_usuario` **e** `id_trabajador`. Una cola previa que solo tenía `id_usuario` permanece en el teléfono sin borrarse ni enviarse bajo una identidad laboral que no puede probarse; su atribución requiere revisión del titular antes de migrarla.
- Los dashboards permiten abrir reportes confirmados del trabajador mediante la API y ver el contenido íntegro. El conteo de formularios continúa separado de las evaluaciones y puntos oficiales.
- El formulario comercial heredado `KpiSalesFormFragment` usa datos de ejemplo y no está en la ruta principal. Su botón ya no afirma un guardado inexistente; el guardado de Ventas sigue en `KpiSalesContainerFragment`.
- Falta comprobar los 13 recorridos en un teléfono y la visualización nativa de cada histórico tras reinstalación. La pantalla principal de Ventas solo modela la semana activa; el historial remoto completo sí está disponible en el navegador de reportes del servidor, pero no en un historial semanal nativo de Ventas.

## Validación aislada de este avance

- `:app:testDebugUnitTest`: 164 pruebas, cero fallos; incluye 13 pruebas parametrizadas de conservación de campos y objetos anidados de los modelos originales, y comprobaciones del acuse exacto.
- `:app:assembleDebug`: APK debug generado.
- Laboratorio PHP/HTTP/MySQL completo: 616 comprobaciones con esquema sintético y V012 presente.
- Laboratorio con la variante comunicada de producción: 62 comprobaciones con V012 y `wp_rostros_cuenta` ausentes; V015, V016, V017, V018 y V019 aplicadas solo en esa base aislada.
- Sin dispositivo conectado a ADB: no se ejecutaron pruebas físicas ni se tocó cPanel.

## Siguiente validación antes de cualquier paquete

1. Revisar en staging el esquema real exportado, las vistas de asistencia y los índices después de V015–V019; no ejecutar migraciones en producción.
2. Cargar correspondencias cargo–formulario sintéticas solo en staging para los 13 recorridos; obtener la decisión empresarial sobre cargos reales por separado.
3. Ejecutar en un teléfono: login, captura, acuse, consulta, reinicio, cambio de cuenta, cambio de dispositivo y reinstalación para los 13 tipos; verificar observaciones, actividades, firmas y cálculos en la UI original y en el navegador del servidor.
4. Probar sesiones vencidas, 403, falta de conexión, reintentos, conflictos de revisión y acceso a reportes de otro trabajador.
5. Mantener intacto el ZIP biométrico v6 y preparar un paquete KPI nuevo únicamente después de cerrar estas pruebas y verificar en conjunto las dependencias.
