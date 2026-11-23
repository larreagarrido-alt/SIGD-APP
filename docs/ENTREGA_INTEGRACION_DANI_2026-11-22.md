# Integración Android de Daniela — 22/11/2026

## Qué quedó integrado

- Se incorporaron las pantallas, recursos, colores y pruebas nuevas del ZIP de Daniela. Se mantuvieron el acceso real a API, la sesión, el calendario KPI de Asistencias, la sincronización de formularios y el visor avanzado de reportes del proyecto actual.
- La comparación final deja 24 archivos distintos del ZIP: son fusiones deliberadas de rutas, red, asistencia y editor KPI. En particular, se conservó el editor maestro más nuevo del proyecto actual, porque el del ZIP elimina controles de zoom, guardado y selección que ya funcionan.
- **Mi horario** abre el cronograma de actividades. **Mi asistencia** consulta las marcaciones del trabajador en Asistencias. Son módulos separados.
- El formulario de actividades de Bienestar ahora tiene soporte de servidor para borrador/publicación, destinatarios y listado privado para colaboradores. Los planes anteriores quedan como borradores al aplicar V028.
- Se incorporó la regla local de puntos aprobada: 20, 17, 14, 12 o 10 según el porcentaje después de la aprobación administrativa. No se acreditan puntos por solo enviar una ficha.
- El Centro de gestión abre Gestión KPI, Editor de fichas KPI y Editor de horarios. La pestaña Reportes del administrador abre las fichas recibidas desde MySQL; Personal queda para perfiles. El editor de fichas guarda plantillas compartidas en el servidor, mientras Gestión KPI configura indicadores oficiales para puntuación.
- Se quitó del Centro de gestión la tarjeta repetida «Centro de reportes». La pestaña inferior Reportes muestra únicamente las fichas recibidas; Gestión KPI conserva la configuración, revisión y cierre. La diferencia entre porcentajes visibles del APK y puntos oficiales del servidor está en `docs/AUDITORIA_REPORTES_Y_CALCULO_KPI_2026-11-22.md`.
- El cronograma conserva sus pestañas de actividades, metas KPI y evaluación. Para colaboradores y practicantes, la vista diaria filtra la plantilla del cargo por los turnos exigibles que informa Asistencias esta semana. En un día libre, permiso o vacaciones no muestra actividades como si fueran obligatorias. El cronograma y las marcaciones de Mi asistencia siguen siendo pantallas separadas.
- Las 13 plantillas del ZIP tienen metas visibles. La pestaña Evaluación ahora aparece en todos los cargos con los niveles aprobados de 90/80/60/40 %, en lugar de mostrar solo la escala antigua de Gerencia. Esta tabla visual no crea definiciones oficiales ni acredita puntos por sí misma.

## Comprobaciones hechas

- Android: `:app:testDebugUnitTest :app:assembleDebug` correcto; 244 pruebas unitarias, sin fallos.
- PHP/MariaDB local: 811 comprobaciones sintéticas correctas, incluido Bienestar publicado para su destinatario. No se usó la base real.
- El APK inició en emulador y mostró el login nuevo. No se hicieron pruebas con cuentas reales en esta revisión.
- Android Lint aún marca 511 traducciones incompletas y avisos anteriores del proyecto; el APK sí compila. El equipo debe revisar el contenido en idiomas distintos del español.

## Para publicar en cPanel

El paquete está en `Descargas/KPIWeb_INTEGRADO_DANI_20261122`.

1. Exportar un respaldo **actual** de `wsnet_websiteplus` y conservarlo fuera de `public_html`.
2. En phpMyAdmin de `wsnet_websiteplus`, importar solo `SQL/V028__bienestar_actividades_publicadas.up.sql`. No aplicar las migraciones antiguas otra vez.
3. En el Administrador de archivos, guardar copia de los PHP actuales y sustituir los archivos de `CPANEL/websiteplus_backend/src` en `websiteplus_backend/src`, conservando sus subcarpetas. Esto incluye `Controllers/ScheduleController.php`, que agrega el calendario personal al cronograma. Sustituir `CPANEL/public_html/websiteplus-api/index.php` en `public_html/websiteplus-api/index.php`. No subir `.env`, bases SQL de respaldo ni la carpeta `FUENTE` a la web. Si ya publicaste el paquete anterior de esta misma carpeta, para estas correcciones reemplaza `Controllers/ScheduleController.php`, `Controllers/KpiFormsController.php`, `Controllers/PersonalController.php` y añade `Kpi/KpiFormCompletion.php`. No hay SQL ni ruta pública nuevos en esas cuatro correcciones.
4. Instalar `APP/KPIWeb-integrado-20261122-debug.apk` sobre la app anterior. La firma es de desarrollo; para distribución formal hace falta firmar la variante de lanzamiento con la clave del equipo.
5. Con cuentas autorizadas, verificar: inicio de sesión, Mi horario (un practicante con días libres debe ver solo sus turnos exigibles), Mi asistencia por separado, Mis KPI, Centro de reportes del administrador, Editor de horarios, Editor de fichas KPI, Bienestar y publicación/lectura de una actividad real. No crear datos de prueba en producción.

`SQL/VERIFICAR_PERMISOS_ENTREGA_20261122.sql` es de solo lectura. Si el administrador figura con `SIN_CONCESION` para `bienestar.administrar` o `bienestar.ver_agregado`, su Centro de Bienestar seguirá denegando el acceso. No se agregaron permisos de salud automáticamente.

## Lo que todavía requiere cierre funcional

- La auditoría real del 22/11 mostró **0 indicadores KPI aprobados y 0 asignaciones activas**. El ZIP de Daniela aporta interfaz, pero no inserta esas definiciones oficiales en MySQL. El módulo de revisión aún solicita un valor verificado; por tanto, el cálculo automático de todos los cargos y el ranking con puntos no pueden declararse terminados. Los 10 formularios existentes son pruebas pendientes de la limpieza final acordada.
- Las metas de las 13 fichas no forman todavía un catálogo de servidor coherente para todos los cargos. Los freelance 15 y 21 necesitan una regla de entregables sin penalización por días sin turno. No se deben otorgar puntos usando un porcentaje solo calculado en Android.
- La antigua pantalla azul de selección de cargos era una entrada de prueba heredada. Ahora conduce a «Mis KPI» autenticado. La etiqueta del APK es SIGD y el icono tiene margen para no recortarse. Personal recibe la foto actual de Asistencias cuando `foto_perfil` contiene una URL HTTPS pública válida; si falta o no es accesible, muestra iniciales.
- «Mis KPI» abre directamente la ficha autorizada; se retiró la pantalla intermedia «Abrir mis KPI» y la flecha duplicada de la cabecera de las fichas integradas. Se conserva el botón Atrás del teléfono y la navegación inferior.
- Cuando el servidor detecta un nuevo día o turno completado en un formulario KPI, crea un aviso persistente para las cuentas del rol Administrador. Reintentos idénticos y cambios de borrador no crean avisos duplicados. Esta publicación no añade push de servidor: el administrador los verá al abrir Notificaciones. El recordatorio de las 8 p. m. consulta ahora los días completados del servidor y los turnos exigibles de Asistencias. Si falla esa consulta, no acusa al trabajador de un KPI pendiente.
- El instrumento Ryff sigue sin versión validada en producción (auditoría: 0). La pantalla visual no equivale a una evaluación clínica aprobada.
- La limpieza de los 10 reportes, 3 evaluaciones privadas de Bienestar y 1 premio de prueba se hará después de respaldar y validar la integración publicada. No se borró nada en esta integración.

La carpeta `FUENTE` contiene un proyecto Android Studio completo y limpio de cachés, configuraciones locales y datos privados para entregarlo al compañero.
