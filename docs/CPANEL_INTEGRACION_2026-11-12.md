# Compatibilidad local con los archivos publicados de cPanel

Fecha de revisión: 12-11-2026. Rama de trabajo: `jair-integracion` en `KPIWeb-integracion`. No se publicó, importó ni modificó nada en cPanel. Los ZIP y la exportación original permanecen fuera del repositorio.

## Comparación del backend publicado

Fuentes: `Downloads/src.zip` (`websiteplus_backend/src`) y `Downloads/websiteplus-api.zip` (`public_html/websiteplus-api`). Se comparó cada PHP normalizando solamente los finales de línea. De 35 archivos publicados bajo `src`, 29 coinciden con el código local y 6 presentan estas diferencias:

| Archivo de `src` | Diferencia local conservada | Motivo |
| --- | --- | --- |
| `Auth/AndroidAccessService.php` | Límites de desafíos por usuario y tope independiente por IP | Una oficina con IP compartida no bloquea a todos por la actividad de una cuenta |
| `Auth/FaceCredentialService.php` | Límite de desafíos faciales por dispositivo/usuario y tope de IP | Misma protección para redes compartidas |
| `Auth/RateLimiter.php` | Límite de fallos por cuenta y tope de abuso por IP | Conserva protección sin bloqueo cruzado de cuentas |
| `Controllers/AuthController.php` | `/auth/me` consulta datos laborales actuales, incluidos `cargo_nombre`, agencia y trabajador | Android enruta por identidad y permisos efectivos actuales |
| `Controllers/DashboardController.php` | Semana ISO (`o-\WW`) en lugar del año calendario | Evita períodos erróneos alrededor de Año Nuevo |
| `Controllers/KpiController.php` | Semana ISO y `kpi_codigo`/`kpi_version` en la respuesta | Enlace de capturas Android a definición oficial |

`Auth/TokenService.php` coincide con el publicado y conserva la consulta de cargo, rol, agencia, estado laboral, huella de contraseña y validez de sesión. Los servicios de asistencia, autorización y los controladores de actividades, personal y premios coinciden. La copia publicada de `.htaccess` coincide con la local y preserva la transmisión de `Authorization` a PHP-FPM/CGI. El diff de `index.php` publicado frente al local contiene solo rutas añadidas: las rutas anteriores permanecen.

Los controladores locales nuevos bajo `src/Controllers` son `KpiSyncController.php`, `NotificationsController.php`, `WellnessController.php`, `SecurityDevicesController.php` y `RankingController.php`. Los primeros cuatro grupos de rutas, más ranking, aún devolvieron 404 en la API publicada durante un sondeo HTTPS sin token. Las rutas publicadas `/auth/me`, `/kpi/mis-kpis`, `/premios`, `/premios/mis-puntos` y `/dashboard/resumen` devolvieron 401 sin sesión; `/health` devolvió 200. Esto no verifica acceso de titulares ni persistencia en producción.

## Esquema y límites de datos

La estructura de la nueva exportación `wsnet_websiteplus.sql` es idéntica a la estructura recibida antes: 36 tablas reales, 88 ALTER de claves/índices/autoincrementos y 9 vistas. Las vistas de usuarios, trabajadores, roles, cargos, agencias y permisos laborales consultan `wsnet_bd_asistencia`; el backend conserva esta relación. La exportación completa **no se ejecutó ni se añadió al proyecto**. La copia de solo estructura queda en `Downloads/wsnet_websiteplus_structure_only_20261112.sql`, sin filas ni definidores.

`wp_kpi_registros_android` no figura en la exportación. V015 es la única migración local nueva requerida para `/kpi/registros/sync`; su `id_kpi INT UNSIGNED` coincide con `wp_kpi_definiciones.id_kpi`, y `wp_migraciones` admite el registro de versión. Debe compararse con un `SHOW CREATE TABLE` actual en staging antes de aplicarse. La sincronización local valida trabajador, código KPI, período, meta y peso contra la evaluación oficial; guarda meta/peso del servidor y acepta reintentos por `record_id` sin duplicar. Una captura sincronizada sigue siendo **captura cruda**, no una entrega KPI calificada ni puntos acreditados.

El cliente Android de asignaciones KPI descarta respuestas tardías de otra sesión, elimina su caché al recibir 403 y cierra la sesión ante 401. Las capturas pendientes no se marcan como sincronizadas sin el acuse exacto del servidor.

La exportación trae permisos individuales existentes, pero no filas de concesiones por rol, cuestionarios de bienestar, asignaciones/evaluaciones KPI, premios, ranking o notificaciones. Puede ser una instantánea anterior o parcial respecto al estado actual. No se crearán cuentas, permisos, cuestionarios, KPI ni premios por suposición. Los formularios por cargo con códigos locales requieren un mapa oficial `cargo → codigo/id_kpi → período → campos → evaluación asignada`; sin esa asignación, el servidor rechaza la captura y Android conserva el pendiente.

## Matriz de estado

| Módulo | Contrato local y prueba aislada | Estado publicado o pendiente |
| --- | --- | --- |
| Contraseña, huella, rostro y `/auth/me` | Auth y sesión anteriores; enrutamiento Android probado | Rutas anteriores publicadas; sin prueba en teléfono ni con titular autorizado |
| Configuración biométrica | Estado y revocación propia tras contraseña reciente; recuperación tras reinstalación sin clave antigua | Controlador/rutas locales; 404 publicado; falta prueba física de registro/reinstalación |
| KPI oficial | Lectura, borrador, entrega firmada y calificación existentes | Rutas anteriores publicadas; asignaciones reales no verificadas |
| Capturas de formularios KPI | Sincronización idempotente y lectura propia; meta/peso oficiales | Ruta 404 y tabla V015 ausente en exportación; códigos de formularios pendientes de mapeo |
| Personal y administración | Resumen, personal y transiciones laborales existentes | Rutas anteriores publicadas; formularios de configuración KPI administrativos siguen locales |
| Bienestar | Cuestionarios oficiales, guardado por trabajador e historial propios | Ruta 404; no hay cuestionarios en la exportación; permiso efectivo por verificar |
| Notificaciones | Lista y marcado propios | Ruta 404; sin filas de notificaciones en la exportación |
| Puntos y premios | Saldo, movimientos, catálogo y canje en PHP; Android consulta saldo/catálogo | Rutas anteriores publicadas; el diseño Android de reconocimientos no ofrece canje; no hay datos exportados de catálogo |
| Ranking | Lectura de períodos cerrados, permiso `ranking.ver`, visibilidad por empresa/agencia | Ruta 404; no hay snapshots exportados |

## Resultado de pruebas

`backend/tests/run_employees.ps1` usó MariaDB local en un puerto aislado, con dos identidades sintéticas y sin conexión a producción: **549 comprobaciones HTTP/SQL y 15 validaciones**. Incluye propiedad de registros, sesiones, 401/403/404, biometría, notificaciones, bienestar, ranking, KPI, cargo/permiso efectivo de `/auth/me` y reintento de captura. El lint de **41 archivos PHP** no encontró errores. `:app:testDebugUnitTest` pasó **147 pruebas**, cero fallos; `:app:assembleDebug` compiló el APK debug. Falta ensayo en dispositivo físico y con cuentas existentes autorizadas.

## Archivos a actualizar posteriormente en cPanel

Tras revisar la copia publicada final y aprobar el despliegue, comparar y sustituir **solo** estos destinos:

1. `websiteplus_backend/src/Auth/AndroidAccessService.php`, `FaceCredentialService.php`, `RateLimiter.php`.
2. `websiteplus_backend/src/Controllers/AuthController.php`, `DashboardController.php`, `KpiController.php`.
3. Añadir `websiteplus_backend/src/Controllers/KpiSyncController.php`, `NotificationsController.php`, `WellnessController.php`, `SecurityDevicesController.php`, `RankingController.php`.
4. `public_html/websiteplus-api/index.php`, conservando todas las rutas publicadas y añadiendo las locales.
5. **Conservar** `public_html/websiteplus-api/.htaccess` publicado: ya coincide y transmite `Authorization`.
6. Aplicar V015 **solo si** staging confirma compatibilidad y se decide habilitar sincronización de capturas. No copiar el SQL exportado con datos, `backend/config/config.php` ni credenciales al APK o Git.

## Despliegue y reversión propuestos, aún sin ejecutar

1. Obtener respaldo íntegro y verificable de `websiteplus_backend/src`, `public_html/websiteplus-api/index.php`, `.htaccess` y estructura/datos de la base por un canal privado. Registrar checksum y versión. Preparar staging con identidades anonimizadas; nunca clonar contraseñas o biometría real allí.
2. Comparar de nuevo los archivos publicados justo antes del cambio y validar `SHOW CREATE TABLE` de dependencias de V015, permisos efectivos y versiones PHP/MariaDB. Confirmar la política de cuestionarios, KPI y ranking.
3. En staging, aplicar V015 si procede, publicar los PHP nuevos y el `index.php` reconciliado, conservar `.htaccess`, ejecutar PHP lint, pruebas HTTP de 401/403/404, lectura propia y escrituras sintéticas autorizadas. Compilar e instalar el APK en un dispositivo de ensayo para contraseña, huella, rostro, revocación y reinstalación.
4. Con aprobación posterior, repetir respaldo y publicación en una ventana acordada. Publicar backend compatible antes de distribuir APK. Validar `/health`, rutas protegidas sin token y flujos con titulares que hayan autorizado expresamente su uso; no escribir datos reales como prueba sin autorización específica.
5. Si falla, restaurar **los archivos PHP respaldados** y mantener el APK anterior. Si V015 contiene registros, no borrar la tabla: detener el endpoint, exportar sus datos de forma privada y decidir una reversión de esquema revisada. Restaurar base completa solo mediante procedimiento de recuperación acordado, preservando sesiones y cuentas existentes.
