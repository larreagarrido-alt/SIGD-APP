# Integración del cronograma — 21/11/2026

## Fuente y correspondencia

Se revisaron `KPI-Web (cronograma).zip` y `GUIA_INTEGRACION_COMPLETA.md`. El ZIP contiene 13 plantillas Android, pero guardaba los horarios en `SharedPreferences` y no traía las rutas PHP ni SQL descritas en la guía. Se incorporaron sus clases y recursos sin sustituir el login, los formularios KPI ni Bienestar de esta app.

Asistencias tiene 12 cargos actuales. El usuario aclaró que Jefe de Agencia usa Diseñador Senior; Programador Web usa Programador Junior; y los tres cargos Community Manager comparten Community Manager. Confirmó también el uso final de las seis plantillas que el código del ZIP llamaba «propuestas». Las otras correspondencias siguen las de formularios KPI aprobadas previamente:

| Cargo en Asistencias | Plantilla del ZIP |
|---|---|
| 1 Gerente General | Gerente General |
| 2 Jefe de Agencia | Diseñador Senior |
| 3 Creativa/Diseño | Diseñador Gráfico |
| 4 Programador Web | Programador Junior |
| 5 Ejecutivo Comercial | Asesor de Ventas |
| 6 Programador Junior | Programador Junior |
| 7 Diseñador Junior | Diseñador Junior |
| 13 Community Manager | Community Manager |
| 14 Diseñador Senior | Diseñador Senior |
| 15 Community Manager Junior | Community Manager |
| 16 Community Manager Senior | Community Manager |
| 17 Creativo / Videógrafo | Videógrafo |

Las 13 plantillas se conservan en `wp_horarios_plantillas`; las cinco sin cargo actual —Administrador, Asistente Administrativo, Contador, Fotógrafo y Trafficker Digital— quedan disponibles para cargos futuros. Un trabajador nuevo con un cargo existente recibirá el horario de ese cargo. Un cargo futuro cuyo nombre coincida exactamente con una plantilla podrá verla automáticamente; si no coincide, el administrador podrá elegir la plantilla en el editor. No se crean cargos en Asistencias.

## Implementado localmente

- `V025__horarios_compartidos.up.sql` crea las tablas de plantillas, horarios por cargo, turnos por trabajador y alertas. `S025__horarios_cargos_confirmados.sql` carga 13 plantillas y publica 12 horarios.
- `ScheduleController.php` lee el cargo y el estado activo desde Asistencias. Devuelve el horario del cargo, permite al administrador editarlo, asignar una plantilla a un cargo nuevo y establecer turno de mañana, tarde o completo. El “Licenciado” usa el rol Administrador existente.
- El APK lee horarios del servidor y no muestra plantillas locales como si fueran asignaciones vigentes. El editor envía cada cambio, conserva la revisión para detectar conflictos y permite atender alertas confirmadas por el servidor.
- Al aprobar un indicador KPI oficial, el backend abre una alerta para revisar el horario del cargo. Los reportes KPI existentes siguen usando `wp_kpi_registros_android` y `wp_kpi_formularios_detalle`; no se crean reportes paralelos.
- `/horarios/mi-horario` incluye `turno`, `hora_entrada` y `hora_salida` cuando el administrador los haya asignado; el APK muestra ese turno junto al horario. Las horas autorizadas son mañana 08:00–13:30 y tarde 15:30–18:30. El ZIP no indica el turno individual de cada trabajador, por lo que no se inventaron esas asignaciones. Una adaptación local de `/auth/me` también existe en el árbol de trabajo, pero no forma parte del paquete de publicación: la ruta de horarios ya devuelve esos campos.

## Verificación y publicación

En MariaDB local aislado, V025 y la semilla crearon 13 plantillas y 12 horarios publicados. Se comprobaron las filas de Jefe de Agencia, Programador Web y Community Manager Junior/Senior. Además, se restauró el respaldo de `wsnet_websiteplus` descargado el 21/11 en otra base local aislada. Sus vistas requerían seis tablas externas de Asistencias, para las cuales se crearon estructuras vacías solo en el entorno local. Sobre esa copia real, V025 y la semilla se aplicaron sin errores y dieron los conteos 1/13/12. Los PHP modificados pasaron comprobación de sintaxis. Android compiló y pasó `:app:testDebugUnitTest --offline`.

Se preparó `C:\Users\Jair L.G\Downloads\KPIWeb_CRONOGRAMA_FINAL_20261121` con los dos SQL, tres PHP, APK y guía de publicación. Los hashes de esos archivos coinciden con el árbol local. Los dos PHP actuales descargados a `Descargas\respaldo` el 21/11 se compararon: `index.php` solo incorpora las rutas de horarios y `KpiOfficialAdminController.php` solo cambia la aprobación para generar el aviso. El respaldo SQL nuevo quedó en `Descargas\respaldo\wsnet_websiteplus.sql`. El usuario confirmó la subida inicial de PHP y la importación SQL; phpMyAdmin mostró los conteos correctos. El APK se instaló con `adb install -r` sin borrar datos. Falta probar con cuentas reales la lectura de horarios, el guardado desde administración y la asignación de turnos. La compilación y la base local no sustituyen esa prueba. No subir archivos originales del ZIP sobre los PHP actuales de cPanel.

Tras importar V025 y la semilla, phpMyAdmin confirmó `13/12/1`. La primera prueba física mostró un fallo de lectura: PHP convertía el mapa JSON de días (`items`) en una lista al llamar `json_decode(..., true)`. Android esperaba el objeto original. Se corrigió `ScheduleController.php` para preservar el objeto al responder, asignar plantillas y guardar cambios. La versión corregida pasó lint de PHP y una prueba de ida y vuelta PHP → Gson con un perfil real de la semilla. El usuario subió el PHP corregido y se observó en el teléfono el horario de Programador Junior cargado con actividades desde el servidor. Se corrigieron el espacio de la barra de estado y el encaje al ancho del Editor. El usuario confirmó que el Editor abre y se ve bien con Administrador. Una llamada real a `ScheduleController::save` sobre la copia local del respaldo avanzó de revisión 1 a 2, conservó `slots[0].items` como objeto JSON y generó una alerta. No se guardaron cambios de prueba en producción.
