# Pruebas reproducibles de empleados

Estas pruebas crean bases, cuentas y claves **sintéticas** en una instancia MariaDB nueva, local y descartable. No utilizar XAMPP/MySQL de producción, cPanel ni una copia de datos reales. No requieren Android, modelos faciales, fotos, vídeos ni el Motorola. La regresión biométrica comprueba el protocolo con claves y vectores sintéticos, no certifica PAD físico.

## Requisitos y ejecución en Windows

- PHP CLI 8.1 o posterior con `pdo_mysql`, `mbstring`, `openssl`, `allow_url_fopen=On` y `proc_open` disponible. Usar una versión compatible con la del hosting para la validación previa al despliegue.
- MariaDB para Windows (validado con 10.4.32 de XAMPP) con `mysql_install_db.exe`, `mysqld.exe`, `mysql.exe` y `mysqladmin.exe` en su carpeta bin. No se distribuyen binarios ni se instalan servicios.
- PowerShell y puerto local **13317 libre**. Espacio libre para inicializar una instancia nueva; sus datos no forman parte del repositorio.

Desde la raíz del proyecto:

```powershell
& C:/xampp/php/php.exe backend/tests/check_repro.php
powershell -NoProfile -ExecutionPolicy Bypass -File backend/tests/run_employees.ps1 -PhpExe C:/xampp/php/php.exe -MariaDbBin C:/xampp/mysql/bin
```

Reemplazar esas rutas por las de cada equipo. La opción ExecutionPolicy solo afecta ese proceso. El primer comando revisa dependencias, extensiones y generación EC, sin conectarse a una base. El segundo:

1. Rechaza un puerto ocupado; no reutiliza ni detiene servidores existentes.
2. Inicializa `backend/storage/mysql-repro-<UUID>`, enlazado exclusivamente a 127.0.0.1:13317, sin servicio Windows. Root sin contraseña pertenece solo a esta instancia sintética temporal; no se utiliza una contraseña empresarial.
3. Ejecuta las 15 pruebas de validación y la suite de integración. Genera contraseñas/secreto/claves de prueba en ejecución; los textos `Synthetic-only…` son credenciales ficticias para fixtures, no cuentas reales.
4. Verifica `@@datadir` antes de crear fixtures y antes de detener el servidor propio. Crea esquemas aleatorios `audit…`, `export…`, `sql…`, `upgrade…` y `empty…`, sin tocar esquemas ajenos.
5. Detiene únicamente la instancia cuya ruta coincide y restaura las variables de entorno. Conserva los resultados para inspección; no borra archivos ni bases anteriores.

Los resultados y configuraciones efímeras están bajo `backend/storage/repro-<UUID>` y `backend/storage/tests-*`, ignorados por Git. **No compartir esas carpetas**: contienen claves/tokens/configuraciones sintéticas. El resultado final de integración se imprime sin secretos. Si hay fallo, se conserva `integration.log` privado. Los errores inyectados producen 500 deliberadamente; solo un `RESULT: ... passed` final y código de salida cero confirman toda la suite.

El script crea una instancia nueva por ejecución; no depende de `mysql-integral-20261017`, una configuración privada existente o una ruta OpenSSL de XAMPP. La configuración OpenSSL mínima de fixtures no contiene claves. No ejecutar el runner en un servidor empresarial.

## Dependencias revisadas y entrega en Git

`backend/tests/repro_manifest.json` enumera los archivos exactos necesarios. Incluye el runtime PHP/migraciones ya existentes, el ejemplo de configuración sin secretos, dos utilidades de migración/exportación necesarias para las regresiones, los casos de prueba, cuatro scripts SQL incrementales DDL y fixtures saneados. Los seis DDL de asistencia están en `backend/tests/fixtures/attendance_schema.sql`; no se necesita el esquema completo de asistencia ni un dump.

Las pruebas del permiso individual usan plantillas con `synthetic_websiteplus_test` y `fixture-grant-u12`; no dependen de propuestas SQL con nombres de trabajadores reales. Los scripts SQL incrementales no se aplican en cPanel: la suite redirige cada importación a su esquema local aleatorio. La prueba de concurrencia restringe sus procesos a configuraciones bajo storage/tests y al servidor propio.

No usar `git add .`: hay archivos locales ajenos pendientes. Cuando decidas preparar el commit manual, revisar primero esta selección explícita:

```powershell
$files = (Get-Content backend/tests/repro_manifest.json -Raw | ConvertFrom-Json).files
git status --short -- $files
git diff -- backend/src/Controllers/PersonalController.php .gitignore docs
# Solo tras revisar, incluir los archivos de la lista:
git add -- $files
git diff --cached --stat
git diff --cached --check
```

Aquí se muestran instrucciones; Codex no ha ejecutado `git add`, commit, push ni merge. Antes de confirmar, comprobar también cualquier archivo que ya estuviera preparado con anterioridad. El manifiesto incluye código de módulos existentes solo porque la suite de regresión los utiliza: no se han desarrollado ni cambiado esos módulos.

No incluir `.idea`, Gradle local, APK, dumps, backups, ZIP, `config.php`, `storage`, claves privadas, archivos de cámara o pruebas nativas privadas. No añadir todas las carpetas bin/tests/tools: hay utilidades y pruebas ajenas que no son necesarias para esta entrega.

## Cobertura y límites

La suite reproduce las 480 comprobaciones anteriores y añade los casos de auditoría final: permisos sin trabajador asociado, JSON con tipos incorrectos, filtros, búsqueda `0`, paginación, orden de historial y ausencia justificada leída del origen. Se comprueban contraseña, V011 y protocolo facial; access/refresh restringidos; todas las cuentas del trabajador; observaciones sin duplicación; concurrencia real con procesos separados y rollback ante fallos inyectados. El runtime solo dispone de SELECT sobre asistencia; el administrador del laboratorio prepara las fixtures sintéticas.

Un resultado local satisfactorio no confirma la versión PHP, los permisos SQL ni OPcache de cPanel. Esa validación de despliegue sigue siendo un paso autorizado aparte, sin ensayar bajas sobre empleados reales.
