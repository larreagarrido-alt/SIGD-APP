# Integración Android local de KPIWeb (12-11-2026)

## Actualización de verificación (12-11-2026)

Esta sección reemplaza los estados anteriores de bienestar, notificaciones, biometría y pruebas. La base continúa siendo `KPIWeb-integracion` en `jair-integracion`; `KPIWeb-master` se dejó intacto. Antes de integrar sus cambios se guardaron copias fuente de ambas carpetas en `C:\Users\Jair L.G\AppData\Local\Temp\kpi-premerge-20261112-155001`. No se hizo commit, push, merge ni cambio en producción.

La URL configurada por Android es `https://ws.net.pe/websiteplus-api/api/v1/`. La prueba HTTPS de solo lectura devolvió 200 para `/health` (build publicado `face-self-service-20261022-v014`) y 401 para `/auth/me` sin token. Esto confirma respuesta HTTP y protección básica, no conexión de todos los módulos ni persistencia de producción.

Sondeo HTTPS adicional **sin token y sin datos personales**: `/premios`, `/premios/mis-puntos`, `/kpi/mis-kpis` y `/dashboard/resumen` devolvieron 401; `/ranking`, `/bienestar/tests`, `/notificaciones` y `/auth/seguridad/dispositivos` devolvieron 404. Los últimos cuatro contratos existen y pasaron pruebas **solo en el backend local**. Publicarlos requiere comparar primero el `public/index.php` y el esquema actuales, y autorización expresa antes de cambiar archivos de producción.

El ZIP `websiteplus_backend.zip` contiene código `src` y configuración, pero no contiene el `public/index.php` publicado ni el enrutamiento del servidor. Se compararon 35 archivos `src` publicados con los locales (normalizando finales de línea): 28 iguales y 7 diferentes en autenticación, control de acceso, dashboard y KPI. Por eso no se presume que los endpoints nuevos estén publicados. El SQL `wsnet_bd_asistencia.sql` se examinó como esquema de `wsnet_websiteplus` sin ejecutar sus `INSERT` ni exponer datos; incluye 45 definiciones de tablas/vistas y no contiene `wp_kpi_registros_android`. V015 continúa siendo propuesta local.

| Función | Conexión local actual | Prueba efectiva | Pendiente externo |
| --- | --- | --- | --- |
| Cuentas y autenticación | Contraseña, huella y rostro conservados; `/auth/me` y navegación por permisos; mejoras de master integradas | Rutas Android unitarias y autenticación PHP sintética | Verificar permisos efectivos de cuentas existentes autorizadas y hardware físico |
| KPI y sincronización | `/kpi/mis-kpis`, `/kpi/registros/sync`, acuse idempotente y propiedad por trabajador | API PHP sobre MySQL aislado; Android unitarias | V015 frente a esquema actual; mapas de formularios por cargo; servidor publicado |
| Administración y personal | `/dashboard/resumen`, `/personal` y detalle | API PHP sintética | Roles/cargos reales y publicación |
| Notificaciones | `/notificaciones` y marcado de lectura propio en cliente y PHP | API sintética con prueba de propiedad | Publicación del controlador y prueba con cuenta autorizada |
| Bienestar | `/bienestar/tests`, `/bienestar/mis-respuestas`, `/bienestar/respuestas`; Android usa cuestionarios oficiales del servidor para cuentas autenticadas | API sintética: catálogo, sesión, guardado, propiedad, historial, duplicado | Cuestionarios oficiales y concesión de `bienestar.responder` según política; publicación; dispositivo físico. Los recorridos de muestra no se envían al servidor |
| Biometría en Configuración | Estado propio y revocación de huella/rostro tras contraseña reciente; borrado local solo después del acuse del servidor | API sintética con titular y otra cuenta; compilación Android | Publicación, cuenta titular autorizada y prueba en dispositivo con reinstalación |
| Puntos y premios | Endpoints PHP y lecturas Android existentes | API sintética con puntos y canje idempotente | Flujo Android completo de canje/ranking y publicación |
| Ranking cerrado | `GET /ranking?periodo_tipo=MENSUAL` y vista Android existente; usa `wp_ranking_historico` | API sintética: 401, ausencia de período, empresa, agencia y política desconocida | Permiso efectivo `ranking.ver`, datos de períodos cerrados y publicación del endpoint |

Pruebas actuales: `backend/tests/run_employees.ps1` con MariaDB local aislado: **549 comprobaciones HTTP/SQL** más **15 validaciones**, sin conexión a producción. `:app:testDebugUnitTest` pasó **147 pruebas**, 0 fallos; `:app:assembleDebug` generó `app/build/outputs/apk/debug/app-debug.apk`. No se ensayaron pantallas en teléfono ni se creó cuenta alguna en cPanel. Las dos identidades sintéticas existen solo en el laboratorio local temporal.

Para cerrar la verificación real se necesita: copia del `public/index.php` publicado y reglas `.htaccess`/rewrite; confirmación de que la exportación entregada corresponde al estado **actual** de cPanel; mapeo de KPI por cargo y cuestionarios de bienestar oficiales; entorno de pruebas autorizado o titulares que autoricen explícitamente consultas con sus cuentas existentes. No enviar contraseñas en documentos ni ejecutar escrituras en producción. Las cuentas reales, sus permisos y su vínculo a trabajador/cargo/rol/agencia deben permanecer tal como están.

### Revisión de la exportación completa recibida

El archivo original está fuera del proyecto, en `Downloads`, y se preservó sin cambios. Se generó una copia de estructura solamente en `Downloads\wsnet_websiteplus_structure_only_20261112.sql`, también fuera del proyecto: **36 tablas, 88 sentencias ALTER de claves/índices/autoincrementos y 9 vistas**, sin `INSERT`, `UPDATE`, `DELETE` ni `DEFINER`. No se ejecutó ni importó la exportación. La copia es referencia de análisis, no migración para aplicar automáticamente.

Hallazgos estructurales: las seis identidades laborales (`usuarios`, `trabajadores`, `roles`, `cargos`, `agencias`, `permisos_laborales`) son vistas de la base de asistencia; WebSite+ tiene tablas propias de sesiones, permisos, credenciales, KPI, bienestar, puntos, premios y notificaciones. Los campos usados por los controladores locales de notificaciones, bienestar y seguridad biométrica están presentes. `wp_kpi_definiciones.id_kpi` es `INT UNSIGNED`, compatible con la FK propuesta por V015; `wp_migraciones` admite la versión y el checksum. La tabla `wp_kpi_registros_android` no está en la exportación, por lo que `/kpi/registros/sync` local requiere V015 antes de poder funcionar en ese esquema. No se aplicó V015.

La exportación incluye filas de permisos de usuario y sesiones existentes, pero no filas exportadas de `wp_rol_permisos`, cuestionarios de bienestar, asignaciones/evaluaciones KPI, premios, ranking ni notificaciones. Esto **no demuestra** que hoy sigan vacías en cPanel: requiere confirmar fecha y cobertura de la exportación con consultas de solo lectura autorizadas. Tampoco se deben crear concesiones de rol, cuestionarios o asignaciones por suposición. La autorización Android/PHP continúa basada en los permisos efectivos de `/auth/me` y en el trabajador vinculado a la sesión existente.

## Alcance y fuentes

Base: `jair-integracion`. Se compararon por separado `KPI-Web(dani).zip` (administración, colaborador, cuenta, Configuración y KPI común) y `KPI-Web.zip` (formularios KPI por cargo y KPI común). Los ZIP originales no se alteraron. Se copiaron fuentes Android y recursos, sin directorios de compilación ni ajustes locales de Android Studio. Se conservó el enrutador de esta rama porque exige permisos del perfil obtenido con `/auth/me`; el enrutador del ZIP de Dani incluía una ruta alternativa por número de rol.

No se restauró el chatbot Websito IA. Los archivos históricos SQL que lo mencionan no deben ejecutarse en bloque.

## Matriz de conexión revisada

La columna «estado» describe **código local**. Ninguna fila equivale a una prueba contra MySQL de producción.

| Pantalla o función | Cliente y API local | Tablas previstas | Permiso o identidad | Estado comprobado |
| --- | --- | --- | --- | --- |
| Contraseña, huella, rostro, entrada | `LoginActivity`, `MainActivity`, `GET /auth/me` | `usuarios`, `trabajadores`, `roles`, `wp_sesiones`, credenciales biométricas | Sesión del titular; destino por permiso de `/auth/me` | Rutas y errores cubiertos por pruebas unitarias; sin ensayo con cuentas reales |
| Administración: resumen | `AdminDashboardActivity`, `GET /dashboard/resumen` | Vistas KPI y actividad del backend | `dashboard.admin` | Solicitud real y estados de error; datos de producción sin verificar |
| Administración: personal | `AdminPersonalActivity`, `GET /personal`; detalle `GET /personal/{id}` | `trabajadores`, `cargos`, `agencias` y relaciones | Permisos del controlador PHP | Repositorio activo `LivePersonalDataSource`; sin ensayo de base |
| Colaborador: inicio | `CollaboratorDashboardActivity`, `/auth/me`, `/kpi/mis-kpis`, `/premios/mis-puntos` | `wp_kpi_evaluaciones`, `wp_puntos_ledger` | `kpi.ver` y titular | Solicitudes reales; elementos secundarios heredados aún usan datos locales/de muestra |
| KPI asignados | `KpiRemoteRepository`, `GET /kpi/mis-kpis?periodo=YYYY-Www` | `wp_kpi_definiciones`, `wp_kpi_evaluaciones` | `kpi.ver`, trabajador autenticado | Contrato Java/PHP y semana ISO alineados; cache aislada por usuario |
| Captura KPI dinámica | `KpiCustomRepository`, `POST /kpi/registros/sync`, `GET /kpi/registros` | Propuesta `wp_kpi_registros_android` (V015) | `kpi.entregar`, trabajador de sesión | Envío al guardar y reintento manual desde historial; solo marca `SYNCED` ante acuse exacto; sin prueba MySQL |
| Formularios KPI por cargo | `saveRoleDailyEvaluation` y varios `StorageManager` locales | Evaluaciones KPI por determinar | Requiere código de KPI asignado por API | Parcial: formularios integrados y captura local; los códigos estáticos o locales no se hacen pasar por asignaciones del servidor |
| Puntos y premios | `/premios`, `/premios/mis-puntos`; partes de ranking usan `CollaboratorRewardsRankingDataSource` | `wp_premios`, `wp_puntos_ledger`, ranking | Sesión y reglas PHP | Lecturas API parciales; ranking y canje en Android pendientes de validación |
| Bienestar | `CollaboratorWellnessActivity`, preferencias locales | `wp_bienestar_*` | Debe ser titular y acceso autorizado | Cuestionario local; sin endpoint Android de envío/lectura verificado; no es diagnóstico |
| Notificaciones | Repositorio Android heredado; resumen administrativo parcial | `wp_notificaciones` | Por destinatario | Sin contrato Android completo comprobado |
| Perfil y Configuración | `AccountActivity`, `SettingsActivity`, `/auth/me` | `usuarios`, `trabajadores`, credenciales | Titular | Perfil de sesión; preferencias locales. Gestión integral de biometría pendiente |

## Roles y cargos

Los seis tipos solicitados (administración, gerencia, jefatura de agencia, supervisión, colaboración y prácticas) **no se infieren de IDs**. La entrada Android requiere el permiso devuelto por `/auth/me`: `dashboard.admin` abre el dashboard administrativo; `kpi.ver` abre el dashboard de colaborador; sin permiso se abre la cuenta con estado de módulo no disponible. Los cargos específicos dependen de `cargo_nombre` de la tabla `cargos`. Falta una matriz de cuentas reales anonimizadas y sus permisos efectivos para comprobar cada combinación. No se creó ninguna cuenta ni permiso de prueba en producción.

## Conflictos resueltos y límites

- Se registraron las Activities reales de ambos ZIP y se mantuvo el launcher de autenticación de `jair-integracion`.
- `/auth/me` local ahora consulta nombre, DNI, correo, cargo y agencia del trabajador. El contacto de la base de asistencia requiere su propio contrato y no se inventa.
- Gradle necesita en este equipo `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Users\Public\codex-java-sockets`; es un ajuste por comando para el error de loopback, sin borrar caches.
- Las clases KPI usan varios almacenes locales y datos de muestra. La V015 solo guarda capturas crudas asignadas; **no** calcula puntos ni certifica una entrega KPI.
- El endpoint de sincronización acepta lotes de 1 a 50, valida identidad del servidor y KPI asignado, y usa `record_id` para reintentos idempotentes. Los formularios con códigos KPI fijos deben mapearse a asignaciones oficiales antes de poder confirmar sus capturas.
- La revocación Android actual exige sesión biométrica reciente; `recuperar` exige contraseña reciente y permiso `auth.dispositivos.recuperar`. La revocación facial depende de la clave del dispositivo. No hay todavía un flujo seguro completo desde Configuración para desvincular ambos métodos o recuperar una instalación sin clave. No se añadieron botones que prometan esa operación sin contrato.
- No se conoce el esquema actual de cPanel. V015 es una propuesta local, no una migración aplicada.

## Pruebas locales

`./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain` con el JDK de Android Studio y el ajuste de sockets indicado: 147 pruebas, 0 fallos; APK debug generado. Las pruebas cubren rutas de contraseña, huella y rostro, permisos, errores de sesión y acuse de sincronización. PHP de XAMPP validó sintaxis de los controladores cambiados y `backend/public/index.php`. No se probó cámara, hardware biométrico, API HTTP en vivo ni MySQL.

## Antes de integrar con producción

1. Obtener un respaldo reciente **sin secretos en el repositorio** y un esquema exportado de las tablas citadas; comparar tipos, claves, índices, estados, permisos y versión de PHP/MySQL con V015 y los controladores locales.
2. Solicitar a los compañeros el mapa de cada formulario: cargo oficial, `codigo` KPI, `id_kpi`, período, campos, validación, lectura, escritura y permiso. Separar datos de muestra de datos compartidos.
3. Acordar el contrato de seguridad para gestión biométrica desde Configuración: estado por método, reautenticación con contraseña, revocación del titular, recuperación sin clave antigua, sesiones afectadas y auditoría. Implementar cliente y API local juntos y ensayar en base sintética.
4. Ejecutar pruebas de integración con una **copia** del esquema y cuentas sintéticas para las seis categorías, cargos, 401/403, duplicados, desconexión y cambio de cuenta. Comprobar persistencia tras reinicio y lectura restringida al titular.
5. Revisar el diff de ramas y resolver conflictos por archivo; repetir pruebas Android y PHP. No fusionar ni desplegar automáticamente.

## Despliegue posterior (manual)

Con aprobación y ventana de mantenimiento: exportar base y archivos actuales de cPanel, registrar checksum/versiones, comparar esquema, aplicar solo migraciones revisadas en staging, publicar backend compatible y luego APK, ejecutar pruebas de humo con cuentas de ensayo, vigilar errores. Si hay que revertir V015, exportar sus filas y ejecutar la migración `.down.sql` revisada: **renombra** la tabla para conservar registros. Restaurar archivos de backend desde el respaldo y revalidar sesiones. Ningún paso de despliegue se ejecutó en esta integración.
