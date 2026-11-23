# Publicación controlada KPIWeb — preparación local del 14-11-2026

**Estado observado:** el APK local compila y el laboratorio sintético pasa. La API HTTPS pública responde `200` en `/health` y `401` sin token en `/auth/me`; al 14-11-2026 devuelve `404` para `/kpi/formularios/permitidos` y `/kpi/formularios` (además de las rutas nuevas comprobadas anteriormente). El titular confirmó en el teléfono que el APK corregido abre el dashboard personal, pero «Mis KPI» no muestra formularios. **El APK nuevo aún no tiene un backend público compatible.** Ningún archivo o SQL se publicó durante esta preparación.

El candidato vigente para la corrección de navegación es `kpiweb-cpanel-release-candidate-20261114-integral-nav-v2.zip` (SHA-256 `A1E2019A2844575BDE82333C7B17B4AD94AEA029B5F001C6EAD815D0BB8A6D78`). Conserva el ZIP integral anterior y el biométrico v6 intactos. Añade `TokenService.php` al paquete porque `/auth/me` devuelve ahora `id_cargo` e `id_agencia`; `AuthController.php` y el router también deben copiarse juntos. El manifiesto verificó 25 archivos de despliegue (15 PHP y 10 SQL). Las copias publicadas `src.zip` y `websiteplus-api.zip` se usaron como línea base privada, sobre la que se superpuso **exactamente el contenido del ZIP v2**. El laboratorio no es staging de cPanel ni prueba restauración de respaldo empresarial.

La consulta de producción del usuario confirmó que el rol 1 `Administrador` no tiene `dashboard.admin`, aunque el permiso existe. Esto es independiente de V015–V019: instalar PHP y migraciones por sí solo **no abrirá el dashboard administrativo** para ese rol. Revisar y aprobar separadamente `docs/PROPUESTA_PERMISO_DASHBOARD_ADMIN_2026-11-14.sql`, probarlo en staging y verificar que `/auth/me` incluya `dashboard.admin` antes de instalar el APK final. No asignar ese permiso a Gerencia, Jefatura ni Supervisión sin definir el alcance de datos de sus dashboards.

Pruebas del 14-11-2026: 15 validaciones previas sin base, 659 comprobaciones PHP/HTTP/MySQL en el laboratorio completo y 108 en la variante con V012 y `wp_rostros_cuenta` ausentes, incluidas pruebas de permiso administrativo ausente y restaurado **solo en laboratorio**. El montaje exacto del ZIP v2 sobre las copias publicadas pasó previamente 656/105 comprobaciones; las tres pruebas nuevas no cambian los PHP del ZIP. `php -l` pasó en los 15 PHP. Android pasó 170/170 pruebas unitarias y `:app:assembleDebug`. El APK más reciente (`KPIWeb-presentacion-20261114-roles-seguros-debug.apk`, SHA-256 `5D4B3A3DAF246E8ECF305CFB840AF030EA6579A700C5505E22199E7E80978F8F`) se instaló mediante `adb install -r` sin borrar datos. La navegación administrativa después de la concesión aprobada aún no se ha probado físicamente.

La copia publicada `src.zip` aportada contiene 40 PHP. Comparada por SHA-256 con `backend/src`, coinciden 39; el único PHP existente diferente es `DashboardController.php`. El `websiteplus-api.zip` aportado tiene un `index.php` distinto que no declara las rutas nuevas. Su `.htaccess` conserva las reglas de Authorization y no necesita reemplazo. Estas copias son una referencia; antes de publicar se requiere una copia reciente del estado del servidor para detectar cambios hechos después de exportarlas.

## Preflight de solo lectura en phpMyAdmin

Ejecutar sobre el esquema correcto y conservar los resultados **en privado**, sin volcar filas personales al repositorio:

```sql
SELECT DATABASE() AS esquema_actual;
SELECT version, aplicada_en FROM wsnet_websiteplus.wp_migraciones
 WHERE version IN ('V012','V014','V015','V016','V017','V018','V019') ORDER BY version;
SELECT table_name, engine FROM information_schema.tables
 WHERE table_schema='wsnet_websiteplus' AND table_name IN
 ('wp_sesiones','wp_accesos_android','wp_claves_faciales','wp_rostros_cuenta',
  'wp_kpi_registros_android','wp_kpi_formulario_cargos','wp_kpi_formularios_detalle','wp_kpi_borradores_admin');
SELECT table_name,column_name,column_type,is_nullable,extra FROM information_schema.columns
 WHERE table_schema='wsnet_websiteplus' AND table_name IN
 ('wp_sesiones','wp_accesos_android','wp_claves_faciales','wp_kpi_definiciones',
  'wp_kpi_registros_android','wp_kpi_formulario_cargos','wp_kpi_formularios_detalle','wp_kpi_borradores_admin')
 AND column_name IN ('metodo','huella_habilitada','activo_acceso_android','id_kpi','id_cargo','revision','payload_json');
SELECT table_name,index_name,non_unique,seq_in_index,column_name FROM information_schema.statistics
 WHERE table_schema='wsnet_websiteplus' AND table_name IN ('wp_accesos_android','wp_claves_faciales','wp_kpi_formularios_detalle')
 ORDER BY table_name,index_name,seq_in_index;
SELECT id_cargo,nombre FROM wsnet_bd_asistencia.cargos ORDER BY id_cargo;
SELECT 'definiciones' AS tabla,COUNT(*) AS total FROM wsnet_websiteplus.wp_kpi_definiciones
 UNION ALL SELECT 'asignaciones',COUNT(*) FROM wsnet_websiteplus.wp_kpi_asignaciones
 UNION ALL SELECT 'evaluaciones',COUNT(*) FROM wsnet_websiteplus.wp_kpi_evaluaciones;
```

Si los índices/columnas, versiones o los conteos difieren del estado confirmado, **detener** la instalación y reconciliar el paquete. No ejecutar V012 ni `backend/bin/migrate.php` sobre producción. `wp_rostros_cuenta` debe seguir ausente salvo decisión específica posterior.

## Respaldos y ensayo obligatorio

1. Exportar **fuera del proyecto** la base completa `wsnet_websiteplus` y tomar respaldo de los PHP publicados bajo `websiteplus_backend/src/` y `public_html/websiteplus-api/index.php`, incluida `.htaccess` como referencia. Mantenerlos privados y registrar SHA-256, fecha y tamaño. No incluir secretos, `.env`, usuarios ni datos reales en el ZIP candidato.
2. Verificar la restauración del respaldo en una base de laboratorio **aislada y autorizada**; comprobar tablas e índices. La suite local usa solo identidades sintéticas y no demuestra la restaurabilidad de un backup de la empresa. Sin esa comprobación, no iniciar cambios de producción.
   Una opción viable es un subdominio y una base separada de cPanel con control de acceso y copia anonimizada del esquema, aprobados por la empresa. El laboratorio MariaDB local ya existe para pruebas sintéticas, pero no sustituye esa validación de publicación.
3. En staging que reproduzca V014 con V012 y `wp_rostros_cuenta` ausentes, ensayar manualmente **V015 → V016 → V017 → V018 → V019**, verificando tabla/índice/columna y `wp_migraciones` después de cada archivo. El ensayo local de esa variante pasó, pero debe repetirse con el esquema empresarial recién exportado.
4. Probar en staging la correspondencia propuesta de `docs/PROPUESTA_CARGOS_FORMULARIOS_KPI_2026-11-14.md`. Los 11 pares usan IDs reales comunicados por el usuario, pero **no están aprobados para producción**. El cargo 2 permanece sin formulario. El generador `backend/bin/generate_approved_form_mappings.php` acepta únicamente las filas que la empresa apruebe, aunque no cubran los 13 tipos. Los cinco tipos sin cargo equivalente permanecen en el APK, sin mapeo ni entrega autorizada. No usar el fixture sintético en producción.
5. Probar en staging cuentas y permisos autorizados: contraseña, huella, rostro con descriptor válido, ausencia de descriptor, dos dispositivos, 401/403, formularios de los 13 tipos, reportes anidados, reintento/idempotencia, recuperación tras reinstalación y dashboards. V019 conserva **borradores privados**, no publica KPI oficiales.

## Instalación de PHP y SQL, solo en ventana aprobada

Aplicar primero cada migración **up** comprobada, una por una. `V015` crea `wp_kpi_registros_android`; `V016` ajusta huella por dispositivo; `V017` ajusta clave facial por dispositivo; `V018` crea detalle completo y la tabla de correspondencias; `V019` crea borradores administrativos. MariaDB confirma DDL por sentencia: ante fallo, inspeccionar el estado actual antes de reintentar. Los `down` del paquete son referencia de reversión manual y **no se ejecutan automáticamente**.

Copiar después el conjunto PHP coherente del candidato a las rutas indicadas en `MANIFEST_SHA256.txt`: controladores y servicios Auth bajo `websiteplus_backend/src/`, y `public_html/websiteplus-api/index.php` **al final**. No copiar `.htaccess` ni configuración sin una diferencia comprobada. Comprobar SHA-256 de cada archivo copiado y `php -l` antes de habilitar tráfico. El router nuevo debe quedar alineado con V015–V019. Nunca publicar el router antes de sus dependencias.

## Humo posterior y reversión segura

- Sin credenciales: `/health` 200; `/auth/me`, `/kpi/formularios/permitidos`, `/kpi/formularios`, `/kpi/configuracion/borradores` deben responder 401; ninguna de las rutas nuevas debe seguir en 404.
- Con autorización del titular: `/auth/login` y `/auth/me`, `cargo_nombre`, `id_trabajador`, permisos efectivos y formulario aprobado; una captura de prueba únicamente en staging o con consentimiento de escritura separado. Login crea sesión, por lo que no es consulta puramente de lectura.
- Si falla PHP, restaurar **todo** el conjunto PHP previo, incluido el router, desde el respaldo verificado. Mantener las tablas y registros migrados para no perder reportes, sesiones ni vinculaciones. No ejecutar a ciegas los `down` ni restaurar servicios Auth antiguos que puedan interpretar de manera insegura `huella_habilitada=0` o varias claves faciales.
- Si falla una migración, detener PHP nuevo y revisar el esquema/índices antes de cualquier reversión. Una restauración de base sobre producción puede borrar registros nuevos; requiere un procedimiento y ventana específicos.

## APK y demostración

El APK debug se genera en `app/build/outputs/apk/debug/app-debug.apk`. Comprobar `adb devices`; si aparece un teléfono propio autorizado, instalar con `adb install -r` y verificar login, cargo, KPI aprobado, guardado confirmado, lectura tras reinicio, bienestar, notificaciones, administración y Configuración. Si no hay teléfono conectado, copiar el APK al dispositivo e instalarlo desde Archivos. El APK apunta a `https://ws.net.pe/websiteplus-api/api/v1/`; instalarlo **antes** del backend nuevo solo mostrará estados de servidor no disponible para las rutas nuevas. Sin teléfono conectado y sin staging empresarial autorizado, esta verificación física queda pendiente.

Para la presentación, mostrar solo métricas confirmadas. Si las definiciones, asignaciones y evaluaciones oficiales continúan vacías, explicar que los reportes completos pueden guardarse tras aprobar correspondencias, pero no existen todavía puntuaciones ni puntos KPI oficiales. Sin una correspondencia aprobada, la pantalla muestra que el cargo está pendiente y el servidor rechaza la entrega.
