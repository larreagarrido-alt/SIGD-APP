# Revisión del proyecto SIGD antes de publicarlo

Fecha: 22 de noviembre de 2026. Esta revisión cubre el código local y pruebas con datos sintéticos. No certifica el estado actual de cPanel ni los teléfonos del equipo.

## Comprobado

- Android compila y sus pruebas unitarias pasan con `:app:testDebugUnitTest :app:assembleDebug --offline`.
- La API pasa los casos de cálculo KPI. La suite de integración local comprueba cuentas, cambios de cargo, días exigibles de Asistencias, formularios, aprobación, ranking, horarios, Bienestar y fotos con datos sintéticos.
- Los reportes recibidos se calculan desde sus cantidades y metas por cargo. El administrador acepta o rechaza el reporte; solo los aceptados suman puntos. El ranking mensual consulta el mes actual sin arrastrar puntos anteriores.
- Los recordatorios KPI se programan al entrar al panel y al abrir la ficha; se restauran después de reiniciar el teléfono o actualizar la app. Antes de avisar, la app consulta las obligaciones del servidor y evita avisos en jornadas no exigibles.
- El código de producción no contiene el respaldo SQL, credenciales de cPanel, fotos de trabajadores ni bases de pruebas. Esos archivos quedan fuera del repositorio.

## Pendiente de resolver o verificar

1. **Edición de metas y puntuación oficial.** El editor de reportes guarda cambios visuales en `wp_kpi_plantillas_estado`, mientras `KpiOfficialScore` usa metas numéricas definidas en PHP. Cambiar una meta en la lámina no garantiza que cambie el porcentaje oficial ni los puntos. Se necesita una única fuente de metas versionada, con validación y prueba para los 13 formularios, antes de presentar esa edición como operativa.
2. **Publicación real.** Esta revisión no puede confirmar que los PHP, migraciones y APK más recientes estén desplegados en cPanel y en cada móvil. Las pruebas locales no sustituyen una prueba de envío, revisión y ranking en producción con una cuenta autorizada.
3. **Ryff.** El material recibido contiene una muestra de tres preguntas. Falta el cuestionario autorizado completo para habilitar puntuación o interpretación real.
4. **Datos de prueba.** La última auditoría aportada por el equipo mostró 10 reportes KPI, 3 evaluaciones de Bienestar con 26 respuestas y un premio de prueba. La limpieza estaba prevista para el final; no se ejecutó aquí ni se incluye una base real en GitHub.
5. **Cierre mensual.** El ranking del nuevo mes empieza en cero por su consulta. El archivado del mes anterior se ejecuta cuando se consulta el ranking; no hay un proceso programado que lo cierre exactamente a medianoche.
6. **Alta de cuentas en Asistencias.** El controlador de integración web todavía crea cuentas nuevas con la misma clave inicial `123456` y `debe_cambiar_password=0`. Hay que cambiar ese flujo por una clave única y un cambio obligatorio antes de usarlo para nuevas altas reales. No se probó aquí un cambio en la web de producción.

Por estos puntos, el proyecto se puede compartir como código y compilar, pero no sería honesto llamarlo «100 % verificado en producción» todavía.
