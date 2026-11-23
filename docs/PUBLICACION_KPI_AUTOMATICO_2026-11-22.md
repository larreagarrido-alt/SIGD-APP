# Cálculo KPI desde los formularios — 22/11/2026

## Qué se corrigió

El administrador ya no escribe `valor_verificado`. La API cuenta los campos de producción del reporte confirmado, ignora porcentajes y puntos enviados por el teléfono, compara la cantidad con la meta numérica aprobada y muestra el porcentaje y los puntos antes de aprobar. La aprobación conserva su función: confirmar que el reporte es válido; solo entonces el servidor acredita 20, 17, 14, 12 o 10 puntos. Si se observa, acredita cero. Los turnos exigibles de Asistencias ajustan la meta.

El cálculo reconoce los 13 tipos de formulario. Cada indicador oficial usa un código de unidad que corresponde a un campo medible, por ejemplo `VIDEOS`, `PIEZAS`, `PUBLICACIONES`, `TAREAS`, `ASUNTOS` o `ACTIVIDADES`. En Gerencia, `ASUNTOS` cuenta asuntos registrados; la ficha aún no aporta datos independientes para validar todos los porcentajes de las cinco dimensiones de la imagen. No se usa el porcentaje escrito por el usuario como prueba de cumplimiento.

El cargo actual en Asistencias permite crear automáticamente la asignación de un indicador ya aprobado cuando el trabajador activo envía su primer reporte. Un cambio de cargo que no coincida con el indicador detiene la revisión para evitar puntuar con una regla anterior.

## Publicación

Paquete: `C:\Users\Jair L.G\Downloads\KPIWeb_KPI_AUTOMATICO_20261122`.

`CPANEL_SUBIR_Y_EXTRAER.zip` contiene la estructura completa `public_html/websiteplus-api` y `websiteplus_backend/src`. En cPanel se sube al directorio principal de la cuenta, donde ya están `public_html` y `websiteplus_backend`, y se extrae allí **después de respaldar los PHP actuales**. Comprobar que los archivos quedaron en esas rutas; no debe aparecer una carpeta adicional llamada `CPANEL`.

- Si el paquete `KPIWeb_INTEGRADO_DANI_20261122` ya está publicado, subir **solo** `CPANEL/websiteplus_backend/src/Controllers/KpiOfficialAdminController.php` a la misma ruta del servidor y añadir `CPANEL/websiteplus_backend/src/Kpi/KpiFormMeasurement.php`. Instalar `APP/SIGD-kpi-automatico-debug.apk` sobre la versión anterior.
- Si el paquete anterior aún no está publicado, respaldar los PHP actuales, subir el contenido completo de `CPANEL` respetando las rutas y aplicar `SQL/V028__bienestar_actividades_publicadas.up.sql` **solo si V028 no figura ya en `wp_migraciones`**. Instalar el APK de este paquete. No importar de nuevo V023–V027.
- No hay migración SQL nueva para este cálculo. No borrar todavía los 10 reportes KPI ni las tres evaluaciones privadas de prueba hasta terminar la verificación final y guardar el respaldo actualizado.

## Lo que aún falta para puntos y ranking reales

La última consulta de la base real mostró **0 indicadores aprobados y 0 evaluaciones aprobadas**. Ningún APK o PHP puede generar una puntuación oficial con una meta de servidor inexistente. El administrador debe registrar y aprobar metas numéricas por cargo y unidad, con período y peso que correspondan a las fichas definitivas. El editor visual de plantillas no crea esas reglas en `wp_kpi_definiciones`. La comparación entre cargos y el cierre mensual siguen requiriendo su aprobación explícita; antes del cierre el ranking mensual estará vacío.

Los colaboradores freelance 15 y 21 carecen de turnos fijos. La regla actual de Asistencias impide calificar un período sin turnos exigibles; hace falta definir en producción un período y una meta por entregables para ellos. Asignarles turnos ficticios o puntos automáticos por un porcentaje local sería incorrecto.

## Pruebas

Se comprobó `2 / 4 = 50 %`, clasificación Regular y 12 puntos tras aprobar, exclusión de días no confirmados, lectura de los 13 formatos, y rechazo del porcentaje que el teléfono declara por sí solo. Android compiló y ejecutó sus pruebas unitarias. La integración PHP/MariaDB aislada pasó 814 comprobaciones, incluido el flujo de puntuación, asignación automática y cierre. No se modificó la base de producción.
