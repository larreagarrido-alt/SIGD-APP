# Plan de publicación controlada tras el preflight de producción

**Estado:** preparación únicamente. Rama `jair-integracion`; candidato v6 sin modificar. No se ejecutó SQL ni se publicó PHP en cPanel durante esta revisión. Las cuentas, contraseñas, roles, permisos y sesiones del sistema de asistencia permanecen intactos.

## Estado confirmado y compatibilidad

| Componente | Resultado comunicado desde phpMyAdmin | Consecuencia |
| --- | --- | --- |
| KPI | `wp_kpi_definiciones`, `wp_kpi_asignaciones` y `wp_kpi_evaluaciones` tienen 0 filas | Ninguno de los 13 formularios posee asignación/evaluación oficial comprobable. El servidor debe rechazar una captura como `KPI_NOT_ASSIGNED`; no declarar KPI operativo ni crear códigos ficticios. |
| V015 | `wp_kpi_registros_android` no existe; V015 no registrada | La tabla nueva puede crearse si el tipo/índice de `wp_kpi_definiciones.id_kpi` y el motor InnoDB coinciden. El FK admite una tabla padre vacía. La escritura KPI seguirá bloqueada por falta de evaluaciones. |
| V016 | `huella_habilitada` no existe; `uk_wpandroid_usuario` y `uk_wpandroid_celular` existen; V016 no registrada | El `ALTER` previsto parte de los nombres esperados. Conserva `uk_wpandroid_celular`, agrega la bandera y permite varios accesos por usuario. No depende de V012 ni de V015. |
| V017 | `activo_acceso_android` no existe; `uk_face_user` existe; V017 no registrada | El `ALTER` previsto parte del índice esperado. Requiere la tabla de V013 y preserva las claves existentes. No depende de V012 ni de V015. |
| Sesiones | `wp_sesiones` existe en InnoDB; `metodo` es `enum('PASSWORD','BIOMETRIA','FACIAL') NOT NULL` | El esquema informado admite los tres métodos; validar su funcionamiento con datos sintéticos en staging. |
| Historial y rostro complementario | V014 es la última versión; V012 no figura y `wp_rostros_cuenta` no existe | **No ejecutar `backend/bin/migrate.php`, V012 ni todo el historial.** V013 declara su independencia de V012. |

El titular confirmó que `wp_rostros_cuenta` **no existe hoy**. La copia de estructura anterior no representa ese estado. `FaceReferenceService` usa primero el descriptor válido de `wsnet_bd_asistencia.trabajadores`; ese login facial y la clave facial asociada no necesitan V012, aunque sí requieren las demás condiciones de autorización y configuración. Para una cuenta sin descriptor válido, el estado facial anuncia `migration_v012_required=true`, Android muestra que hace falta actualizar el servidor y el inicio facial devuelve `FACE_ENROLLMENT_REQUIRED`. El alta complementaria devuelve `FACIAL_SCHEMA_REQUIRED` (503) sin V012. Incluso con V012, una captura queda `PENDIENTE_VALIDACION` y no se convierte automáticamente en referencia de login. V012 es una decisión independiente, fuera de v6; si se requiere ese flujo, diseñar y ensayar por separado la validación empresarial antes de proponer su publicación.

## Orden de ensayo antes de tocar producción

1. Conservar v6, su SHA-256 y una copia de los ZIP publicados. Preparar staging de la empresa con esquema equivalente al **estado actual** y datos sintéticos; nunca importar usuarios, hashes, sesiones, perfiles faciales ni exportaciones con datos reales.
2. En staging, reproducir **la ausencia física de `wp_rostros_cuenta` y de V012 en el historial**, V014 como última versión, `wp_sesiones` InnoDB con los tres valores de `metodo`, índices biométricos originales y tablas KPI oficiales vacías. Usar solo identidades y descriptores faciales sintéticos. Ensayar login/password, huella y rostro con descriptor válido, y comprobar el estado/error esperado sin descriptor. Confirmar que las vistas de `wsnet_bd_asistencia` son de solo lectura y que `Authorization` llega por `.htaccess`.
3. Sin ejecutar V012, validar el respaldo y ejecutar cada migración aprobada por separado: **V015 → V016 → V017**. Comprobar columnas, índices y `wp_migraciones` después de cada paso; V012 debe seguir ausente. MySQL/MariaDB confirma DDL por separado; si falla una sentencia posterior, detenerse y revisar el estado real, sin repetir el archivo completo.
4. Sobre ese esquema, instalar exactamente los ocho controladores de v6 y `Auth/AndroidAccessService.php`, `Auth/FaceCredentialService.php` y `Auth/RateLimiter.php`. Instalar `public_html/websiteplus-api/index.php` **al final**. Mantener `.htaccess` y configuración publicados salvo que una comparación autorizada demuestre una necesidad concreta. No mezclar servicios Auth antiguos con rutas/controladores de v6.
5. Ejecutar suite HTTP/SQL de v6 en esta variante **sin V012**: 401/403, titular y no titular, contraseña, refresh/logout, huella, rostro con descriptor sintético válido, recuperación, dos dispositivos y sesiones afectadas. Verificar que sin descriptor el estado declara V012 requerida, el login facial se rechaza y el alta complementaria devuelve 503 sin crear filas. Con KPI oficiales vacíos, comprobar rechazo `KPI_NOT_ASSIGNED` y conservación de pendientes en Android; no probar escrituras KPI reales ni simular asignaciones oficiales en producción.
6. Probar APK en teléfonos según [lista física](PRUEBAS_FISICAS_BIOMETRIA_2026-11-12.md). Obtener decisión empresarial sobre las [13 correspondencias KPI](KPI_FORMULARIOS_PUBLICACION_2026-11-12.md) antes de declarar los módulos KPI operativos.

## Ventana de publicación, solo tras autorización

1. Respaldar la base completa `wsnet_websiteplus` por canal privado; verificar restaurabilidad y anotar hora/checksum. Respaldar los archivos publicados que se sustituirían bajo `websiteplus_backend/src/Controllers/`, `websiteplus_backend/src/Auth/` y `public_html/websiteplus-api/index.php`, más una copia de `.htaccess` y del APK anterior. No incluir `.env`, exportaciones reales ni secretos en el ZIP candidato o Git.
2. Repetir las consultas de solo lectura inmediatamente antes de publicar. Si cambió cualquier índice, columna, versión o contenido KPI relevante, detener la instalación y volver a comparar. Congelar tráfico/escrituras de las rutas nuevas durante la copia de PHP.
3. Aplicar únicamente las migraciones aprobadas en el mismo orden ensayado; verificar cada resultado antes de copiar PHP. Copiar el conjunto completo de servicios/controladores y router en el orden anterior. No crear usuarios ni cambiar contraseñas o permisos.
4. Comprobar `/health`, rutas anteriores, login y `/auth/me` con cuentas existentes **solo con autorización de sus titulares**; esas llamadas crean/actualizan sesiones, por lo que no son consultas puramente de lectura. Reservar pruebas de escritura y biometría para staging o una autorización específica.

## Reversión sin pérdida de vínculos

Ante un fallo, detener el APK nuevo y retirar primero las rutas nuevas restaurando el router respaldado. Conservar V015/V016/V017 y sus filas durante la reversión urgente. Mantener los tres servicios Auth compatibles con el esquema nuevo: un `AndroidAccessService` antiguo ignoraría `huella_habilitada=0`, y un `FaceCredentialService` antiguo podría revocar todas las claves faciales de la cuenta. No borrar ni modificar sesiones, claves faciales o accesos Android para forzar índices únicos anteriores.

Una reversión de esquema es un trabajo posterior y separado: V017 antes de V016, solo si sus guardas comprueban que no hay múltiples rostros/accesos activos ni huellas deshabilitadas; V015 se conserva para no perder capturas. Nunca ejecutar un `down` a ciegas sobre producción. V012 no tiene `down` preparado y no se revierte automáticamente.

## Condiciones para autorizar

**Hoy no se puede declarar lista la publicación completa de KPIWeb:** falta ensayar en staging empresarial la variante confirmada **sin V012 ni `wp_rostros_cuenta`**, probar en teléfonos y recibir las definiciones/asignaciones/evaluaciones KPI oficiales. V6 pasó 583 pruebas PHP/HTTP/MySQL en laboratorio aislado con datos sintéticos, pero ese resultado no demuestra la variante exacta de producción sin V012. El registro facial complementario seguirá limitado mientras falte esa tabla y una vía autorizada de validación.
