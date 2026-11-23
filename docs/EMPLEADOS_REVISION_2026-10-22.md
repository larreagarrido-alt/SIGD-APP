# Empleados e integración laboral

Revisión e implementación inicial en `jair-empleados`, 22 de octubre de 2026. El Word proporcionado se consultó como referencia y no se modificó. No se accedió a cPanel ni se volvieron a contar trabajadores reales; los 7 practicantes, 6 colaboradores y el administrador sin tipo son contexto proporcionado por el usuario, no mediciones del laboratorio.

## Auditoría final de la responsabilidad de empleados

Continuación de los commits locales `fe9cc7a` y `a4ecb6a`. Este apartado refleja el resultado final; los apartados posteriores conservan la evidencia de las dos etapas previas.

### Cobertura y alcance terminado

| Responsabilidad backend | Resultado |
|---|---|
| Listar, buscar y consultar trabajadores | Implementado, con filtros y paginación acotada; detalle e historial disponibles |
| Practicantes, colaboradores, activos e inactivos | Lectura del origen y restricciones de sesión compartidas; el tipo no concede administración |
| Vacaciones, permisos y ausencias autorizadas | Lectura de registros aprobados/pendientes/rechazados de asistencia, sin crear intervalos ni calcular ausencias por falta de marcación |
| Inactivación/reactivación observada | Fotografía comparada, eventos sin duplicación y revocación; no supone una pausa contractual con fechas acreditadas |
| Desvinculación y reingreso | Transiciones explícitas autorizadas, periodos e historial preservados; todas las cuentas y sesiones del trabajador restringidas |
| Cambios de tipo, cargo y agencia | Observación e historial; datos vigentes vienen del origen, sin reescribir periodos históricos |
| Permisos y alcances | PROPIO/EQUIPO/AGENCIA/TODOS, denegaciones individuales, lectura separada de administración |
| Integración y sincronización | Asistencia SELECT-only, transacciones por trabajador, bloqueo común y CLI opcional seguro |
| Integración con los compañeros | Contrato exacto en `EMPLEADOS_API.md`; no se han creado pantallas administrativas ni funciones KPI |

### Fallos reales corregidos en esta auditoría

1. **EQUIPO sin trabajador asociado:** el listado comparaba `id_supervisor=0`, mientras el detalle ya exigía una identidad de trabajador válida. Un registro del origen con supervisor `0` podía aparecer en la lista de esa cuenta. Se exige trabajador positivo para la rama EQUIPO; caso reproducido con datos sintéticos y lista/detalle coherentes.
2. **Tipos de JSON convertidos a texto:** el controlador convertía números/booleanos a cadenas antes de validar, permitiendo interpretar `motivo=123` como un motivo textual. Ahora los tres campos se validan como cadenas antes de tocar datos. Una fecha explícita null no se reemplaza silenciosamente por hoy. Precisión: los arreglos ya eran rechazados por `Request`; no se afirma que se guardaran arreglos antes. La validación del endpoint ahora proporciona el mismo `VALIDATION_FAILED` para esas formas incorrectas.
3. **Búsqueda `0` ignorada:** `empty('0')` desactivaba el filtro. Ahora la cadena cero se busca como cualquier otro texto.
4. **Listado limitado sin continuación y orden ambiguo:** conservar 100 filas por defecto, añadir `limit/offset` y `paginacion.hay_mas`, con desempate por IDs. Se consulta una fila adicional para saber si hay más, sin COUNT global. Solo se observan los IDs de las filas devueltas, nunca la adicional. `total` conserva su significado anterior (filas de página). No se promete una fotografía estable si el origen cambia entre solicitudes.
5. **Historial con empates:** añadir IDs como último desempate de eventos, periodos y permisos para que varias operaciones del mismo día/segundo no cambien arbitrariamente de orden.
6. **Entrega de pruebas incompleta:** varias dependencias necesarias estaban sin seguimiento; además había rutas OpenSSL de XAMPP y dependencia del esquema completo de asistencia/propuestas SQL con un identificador real. Se preparó una selección explícita de fuentes, seis DDL mínimos sin datos, plantillas sintéticas y configuración OpenSSL portable. No se incorporaron datos reales ni propuestas de permisos sobre cuentas reales.

También se acotaron filtros inválidos, longitud de búsqueda y paginación con 422 antes de sincronizar. No se modificaron `Authorization.php`, `TokenService.php`, servicios de autenticación, observador/transiciones, `LaborPolicy.php`, migraciones ni módulos de compañeros en esta auditoría final. Sus pruebas anteriores se conservaron.

### Pruebas finales y reproducibilidad comprobada

- **506 comprobaciones de integración aprobadas**: reproducen las 480 anteriores y añaden 26 casos de auditoría final.
- **15 pruebas de validación aprobadas**, sin conexión a bases.
- PHP CLI **8.2.12**, MariaDB **10.4.32**, Windows. Comprobación sintáctica de los PHP seleccionados y `git diff --check` sin errores de espacios.
- La ejecución definitiva se hizo desde una **copia nueva con solo los 89 archivos de `backend/tests/repro_manifest.json`**, sin config privado ni almacenamiento previo. El runner inicializó una instancia MariaDB nueva, verificó su datadir, ejecutó las pruebas y detuvo esa misma instancia. No se reutilizó el laboratorio antiguo ni un servidor empresarial.
- Evidencia local privada: `entregables/empleados-repro-verificado-e16e02a509f842cb9672ec375a636f52/backend/storage/repro-a2befbb49b2e44db8a73311c22f04a5f/integration.log`. Ese almacenamiento está ignorado por Git y **no forma parte de la entrega**.
- La primera prueba desde copia limpia detectó dos casos SQL faltantes en el manifiesto; quedaron incluidos y el preflight ahora comprueba también las inclusiones estáticas de pruebas. La ejecución definitiva terminó con `RESULT: 506 passed` y código de salida cero.
- Verificados: denegación y alcances; trabajador sin tipo/asociación; todas las cuentas restringidas; access/refresh; datos de asistencia y KPI intactos; observación repetida; concurrencia de inicialización, actualización y transición; lectura fresca tras bloqueo; rollback de eventos/fotografía/revocación; JSON inválido; filtros y paginación; ausencia justificada y orden del historial.
- La regresión biométrica usa claves y vectores sintéticos, no imágenes ni personas del origen. No demuestra una validación física nueva del PAD ni una instalación nueva en Motorola. No se genera APK por este cambio backend.

Para reproducir, seguir [EMPLEADOS_PRUEBAS.md](EMPLEADOS_PRUEBAS.md). El comando principal desde la raíz es:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File backend/tests/run_employees.ps1 -PhpExe C:/xampp/php/php.exe -MariaDbBin C:/xampp/mysql/bin
```

Adaptar los ejecutables de cada equipo. El runner rechaza un puerto 13317 ocupado, no instala servicios ni usa configuraciones MySQL privadas, limita el servidor a loopback y no borra datos anteriores. Conserva las configuraciones efímeras bajo storage, ignoradas. El comando productivo opcional `sync_employees.php` sigue separado de este runner: no se ejecutó en cPanel ni se programó una tarea.

### Archivos de la auditoría final

- Modificado: `backend/src/Controllers/PersonalController.php`.
- Modificados: `.gitignore` (permitir SQL de fixtures), este informe y exclusivamente las referencias de empleados de `docs/endpoints_equipo.md`.
- Nuevos: `docs/EMPLEADOS_API.md`, `docs/EMPLEADOS_PRUEBAS.md`.
- Nuevos: `backend/tests/employee_final_cases.php`, `backend/tests/test_support.php`, `backend/tests/check_repro.php`, `backend/tests/run_employees.ps1`, `backend/tests/repro_manifest.json` y los cuatro archivos de `backend/tests/fixtures/`.
- Existentes sin seguimiento ajustados: `backend/tests/integration.php`, `android_access_cases.php`, `face_auth_cases.php`, `face_permission_grant_review_cases.php`.
- El manifiesto también enumera dependencias **existentes, revisadas y necesarias**: los demás casos empleados/autenticación, `backend/bin/migrate.php`, `export_sql.php`, cuatro SQL incrementales y runtime/migraciones ya presentes. Enumerarlos no significa que se hayan desarrollado nuevamente. Las demás herramientas, pruebas nativas, almacenamiento y documentación ajena quedan fuera.

No se ejecutó `git add`. Al acabar la revisión, **32 archivos seleccionados seguían sin seguimiento**: deben revisarse e incluirse en el commit manual antes de descargar la rama en otro equipo. Los dos commits anteriores solos no contienen una entrega de pruebas completa. Las instrucciones de selección están en la guía; no usar `git add .` con los archivos locales ajenos presentes.

### Decisiones de negocio y límites, separados de errores técnicos

- Permisos parciales y permisos cancelados: la política KPI existente sigue siendo la anterior. Sus responsables deben acordar prorrateo/restauración; no se cambia esa lógica desde empleados.
- Pausas contractuales y fechas históricas: se requiere evidencia y aprobación. Registrar observaciones presentes no acredita fechas efectivas antiguas.
- Frecuencia de observación: decidir si basta el uso de la API o se autoriza una ejecución periódica. Sin observación o historial externo no puede reconstruirse un ciclo de inactividad completo ocurrido entre dos lecturas.
- Ausencias sin registro autorizado: no deducirlas de marcaciones ausentes ni aplicar penalizaciones sin reglas del sistema de asistencia. El detalle ya muestra las ausencias justificadas que ese origen proporciona.
- La vista heredada puede devolver varias cuentas del mismo trabajador y el detalle devuelve los últimos 20 permisos. Son límites documentados, no una promesa de personas únicas ni de historial ilimitado de asistencia. Los periodos y eventos complementarios se conservan.

**Veredicto:** la responsabilidad backend de `jair-empleados`, dentro del alcance documentado, queda **técnicamente terminada en el árbol de trabajo y validada localmente**. No falta desarrollar pantallas de compañeros ni rehacer autenticación. Para compartir faltan la selección/revisión y commit manual de los archivos preparados; para desplegar falta validación autorizada de PHP/permisos/OPcache en cPanel. No se afirma que producción esté validada ni que las decisiones de negocio pendientes estén resueltas. No hay SQL nuevo que importar por esta auditoría.

No hubo commit, push, merge, PR, despliegue ni cambios en bases reales. Se conservaron README, IDE, Gradle y archivos locales previos.

## Continuación desde `fe9cc7a`: observación de asistencia

Se añadió `Employees/LaborObservationService.php`, reutilizando las tablas V002 existentes. No necesita migración ni importación SQL. La conexión de asistencia solo hace SELECT; cada trabajador se procesa en una transacción de WebSite+.

### Funcionalidad implementada

- Primera lectura: crear la fotografía complementaria y un evento CARGA_INICIAL con estado, tipo, cargo y agencia observados. No deducir el inicio histórico del contrato ni crear un periodo artificial con la fecha de hoy.
- Comparar cada lectura con la última fotografía. Registrar INACTIVACION, REACTIVACION, CONVERSION_TIPO, CAMBIO_CARGO o CAMBIO_AGENCIA únicamente cuando cambia el campo correspondiente. Practicantes y colaboradores usan el mismo mecanismo.
- Conservar todos los periodos y sus valores originales al observar cambios externos. Sin fecha efectiva acreditada no se cierran/dividen periodos ni se modifica retrospectivamente su tipo, cargo o agencia. El dato vigente se sigue leyendo de asistencia.
- Conservar la situación explícita VIGENTE/DESVINCULADO, su motivo, fecha y responsable. Inactivo se trata como restricción observada; **no demuestra una pausa contractual ni una desvinculación**. Un retorno Activo se registra si antes se observó Inactivo, pero nunca hace REINGRESO ni elimina DESVINCULADO.
- Revocar en la misma transacción todas las sesiones de todas las cuentas del trabajador si se observa Inactivo o DESVINCULADO. Un retorno no restaura access ni refresh. La comprobación compartida de tokens invoca el observador al detectar la restricción, tanto desde access como desde refresh; no se reconstruyen los métodos de autenticación.
- Mantener permisos y vacaciones como lectura viva del origen. No crear copias o intervalos de vacaciones ni cambiar la política KPI existente.

### Fechas e historial: distinción obligatoria

V002 exige una `fecha_efectiva` no nula. En los eventos nuevos con `origen=SINCRONIZACION`, ese campo contiene **el día en que se observó el cambio**, nunca una fecha histórica inferida. El motivo lo declara expresamente: la fecha efectiva y el motivo originales no están disponibles. `creado_en` conserva la hora de observación.

Para que la aplicación pueda mostrar esa diferencia sin interpretar mal el campo heredado, el detalle conserva todos los campos anteriores y añade a cada evento:

- `fecha_efectiva_confirmada=false` y `observado_en=creado_en` para SINCRONIZACION.
- `fecha_efectiva_confirmada=true` y `observado_en=null` para transiciones API con fecha explícita validada.

Quien integre el historial debe mostrar «Observado el…» para los primeros. No debe utilizar su día observado para liquidaciones, nómina, fechas de contrato o cálculos de duración de pausas. Si se requieren fechas efectivas históricas, hace falta una fuente acreditada y un flujo de aprobación; esta implementación no las inventa.

### Ejecución, concurrencia y límites

1. `GET /personal`: observar únicamente los trabajadores que ya pasaron permisos y filtros y están en la respuesta (máximo 100 filas; deduplicar IDs para observarlos una sola vez). Se conservan los alcances PROPIO/EQUIPO/AGENCIA/TODOS y la estructura `{total, empleados}`.
2. `GET /personal/{id}`: comprobar primero el alcance; luego observar y devolver el historial actualizado. No sincronizar otros trabajadores mediante una lectura no autorizada.
3. `POST /personal/{id}/transicion`: reutilizar el mismo bloqueo por trabajador. Una transición válida también registra otros cambios de asistencia antes de avanzar la fotografía, para no perder evidencia. La REACTIVACION manual conserva su evento API y no duplica el cambio automático de estado. Si ya se observó el retorno automáticamente, volver a solicitarlo devuelve el conflicto 409 existente.
4. Access/refresh de un trabajador restringido: registrar la observación y revocar también las sesiones de sus otras cuentas. El administrador sin trabajador asociado conserva su comportamiento anterior.
5. Se preparó `backend/bin/sync_employees.php` como opción CLI manual o programable. No se instaló ninguna tarea ni se ejecutó en el hosting. Procesa todos los IDs por lotes de 100, con transacciones independientes por trabajador; ante un error termina con código 1 y mensaje genérico, sin exponer secretos. Los trabajadores previamente confirmados quedan confirmados. Repetirlo es seguro.

El bloqueo es una fila de `wp_situacion_laboral` con `FOR UPDATE`; no se bloquean ni escriben filas de asistencia. Se vuelve a consultar asistencia **después de obtener el bloqueo**, mediante su conexión de lectura, para evitar reproducir una fotografía anterior mientras otra petición termina. Eventos, fotografía y revocación se confirman juntos; cualquier error revierte la operación del trabajador. No se modifican ni eliminan eventos anteriores.

Si no cambia ningún dato y no hay sesiones nuevas que revocar, la repetición deja idénticos eventos, fotografía y timestamps. No se actualiza un timestamp de «comprobado» en cada lectura. No existe vigilancia permanente: sin un acceso, consulta o ejecución CLI no hay nueva observación. Un cambio Inactivo→Activo ocurrido completamente entre dos observaciones **no puede reconstruirse** sin historial del origen. Tampoco puede revocarse retroactivamente una petición ya completada.

### Archivos de esta continuación

- Nuevo `backend/src/Employees/LaborObservationService.php`.
- Modificado `backend/src/Employees/LaborTransitionService.php`: bloqueo común y conservación de cambios observados durante transiciones explícitas.
- Modificado `backend/src/Controllers/PersonalController.php`: observación tras autorización y metadatos de fechas del historial.
- Modificado `backend/src/Auth/TokenService.php`: observación/revocación de todas las cuentas al detectar una restricción laboral; validación de contraseña, dispositivos y política facial conservada.
- Nuevo `backend/bin/sync_employees.php` (opcional, solo CLI).
- Nuevo `backend/tests/employee_observation_cases.php`; ampliados `backend/tests/integration.php` y `backend/tests/labor_transition_process.php`.
- Este informe. Los archivos de pruebas anteriores que ya estaban sin seguimiento en Git se conservaron; revisar su inclusión al preparar un commit manual.

### Actualización manual y reversión

No se ha desplegado nada. Tras autorizar y completar la validación final, respaldar los tres PHP modificados y copiar esos archivos más `src/Employees/LaborObservationService.php` a la estructura privada del backend. No modificar `public/index.php`, el `config.php` privado ni ninguna opción facial. No subir pruebas, almacenamiento del laboratorio o configuraciones locales. El CLI solo es necesario si se decide usarlo; no es un endpoint público.

Para una ejecución CLI futura, si el hosting la permite y se autoriza, el comando es:

```text
php /opt/wsnet/websiteplus_backend/bin/sync_employees.php /opt/wsnet/websiteplus_backend/config/config.php
```

La ruta y el ejecutable PHP deben verificarse en el hosting antes de usarlo. Tener cPanel no garantiza acceso a CLI; no se ha solicitado crear Terminal ni una tarea programada. La observación mediante los endpoints y tokens no depende de ese comando. Aprobar antes la frecuencia y el responsable de una ejecución periódica si se necesita cobertura independiente del uso de la app.

Después del despliegue, comprobar health y contraseña/huella con una cuenta autorizada; verificar listado y detalle para cada alcance; confirmar un segundo GET sin cambios de historial. Las pruebas de inactivación y reingreso se realizan en laboratorio, no alterando trabajadores reales para ensayar.

Para revertir código, restaurar los tres PHP respaldados como conjunto. El nuevo servicio y CLI pueden quedarse sin invocarse; no es necesario borrarlos. Conservar eventos y periodos ya registrados. La reversión de código **no restaura sesiones revocadas**: iniciar sesión otra vez cuando corresponda. No restaurar una base antigua sobre producción para deshacer observaciones.

### Validación final de esta continuación

- **480 comprobaciones de integración HTTP/MySQL aprobadas**: las 420 anteriores y 60 adicionales de observación. Registro local privado: `backend/storage/face-evaluation/employee-observation-20261022.log`.
- **15 pruebas de validación sin base de datos aprobadas**. Sintaxis PHP correcta en los ocho archivos PHP nuevos/modificados de esta continuación; `git diff --check` sin errores de espacios.
- Primera observación concurrente: cuatro procesos inicializan una sola fila y un solo evento, sin periodos artificiales. Observación concurrente de cuatro campos: cada cambio aparece exactamente una vez.
- Una prueba mantiene el bloqueo mientras cambia el origen: comprueba que el observador espera realmente y usa la lectura actual después del bloqueo, sin reproducir una inactividad ya superada antes de observarla.
- Carrera mixta de dos observaciones y dos desvinculaciones: una sola desvinculación, un conflicto esperado y un solo cambio de agencia; ninguna evidencia perdida.
- Fallos inyectados al insertar un evento y al revocar sesiones: rollback de fotografía, eventos y revocación; error HTTP saneado. Reintento después del rollback conocido produce un solo evento.
- Activo→Inactivo→Activo, cambio de tipo/cargo/agencia, dos cuentas del mismo trabajador, tokens antiguos, vacaciones aprobadas, permisos, desvinculación explícita y reingreso, alcances y filtros, CLI repetido y datos fuente/KPI intactos.
- La regresión incluye contraseña, protocolo V011 y flujo facial con cuentas y firmas sintéticas. No equivale a una nueva prueba física de biometría o PAD en el Motorola. No se modificó Android ni se generó otra APK para este cambio exclusivamente PHP.

**Estado:** la parte backend implementada está lista para una validación final en un entorno de pruebas del equipo. No está desplegada ni validada en cPanel. Antes de cerrar el módulo empresarial quedan las decisiones de permisos parciales/cancelados, fechas efectivas históricas cuando se requieran y frecuencia de observación. La ausencia de esos acuerdos no se resuelve inventando pausas ni modificando los cálculos de KPI.

## Lo que ya funcionaba

- `/personal`, `/personal/{id}` y `/personal/{id}/transicion` existen y utilizan las vistas de asistencia y tablas complementarias de WebSite+.
- `TokenService.validateAccessToken()` y `rotateRefreshToken()` ya llaman a `sessionAllowed()`. Ambas rechazan trabajador inactivo, situación distinta de VIGENTE, contraseña cambiada y dispositivos revocados. No había una omisión de esa llamada en el refresh actual: no se reescribió el servicio.
- Las cuentas sin trabajador asociado conservan la política de acceso existente. El tipo Practicante/Colaborador/Sin tipo no sustituye al estado laboral ni convierte automáticamente a alguien en administrador.
- La vista de empleados calcula permisos/vacaciones vigentes leyendo asistencia. `LaborPolicy.syncLeaves()` ya consulta permisos aprobados y está conectado a solicitudes KPI/dashboard; no es un sincronizador general del historial de empleados.

## Fallos corregidos

| Fallo comprobable en el código anterior | Corrección |
|---|---|
| El listado pedía `empleados.ver` con trabajador null. Solo TODOS podía superarlo; PROPIO/EQUIPO/AGENCIA recibían 403. | Filtrar el listado por el alcance efectivo: propio trabajador, reportes directos, agencia o todos. Una denegación individual sigue prevaleciendo. Los filtros de búsqueda se aplican después del alcance. |
| Se permitía repetir desvinculación/reingreso y añadir eventos contradictorios. No se bloqueaba una fila común por trabajador. | Serializar las transiciones mediante la fila complementaria y rechazar cambios que no corresponden al estado con 409 `LABOR_STATE_CONFLICT`. |
| El evento guardaba `trabajadores.estado` como estado anterior incluso si el cambio era de DESVINCULADO a VIGENTE. | Guardar la situación complementaria real anterior/nueva. La reactivación observada usa Inactivo/Activo para describir el cambio de asistencia, sin escribirlo. |
| La desvinculación no cerraba el periodo abierto ni el reingreso abría uno nuevo. | Cerrar únicamente el periodo abierto, enlazarlo al evento y abrir un nuevo periodo de reingreso con el tipo/cargo/agencia actuales. Los periodos ya cerrados permanecen intactos. |
| Se revocaba solo el primer `id_usuario` obtenido por `fetchColumn()`. Una segunda cuenta podía conservar sesiones y recuperarlas tras el reingreso si no las utilizaba durante la baja. | Revocar en la misma transacción todas las sesiones, de todos los métodos, de todos los usuarios asociados al trabajador. Se prueba la revocación inmediata antes de consultar esos tokens. |
| El motivo vacío por espacios o demasiado largo podía llegar a MySQL. | Validar motivo de 1–500 caracteres, evento permitido y fecha ISO real del día actual. |

El controlador delega la operación en `Employees/LaborTransitionService.php`. La situación, periodos, evento y revocación se confirman juntos o se revierten juntos. Los eventos anteriores no se actualizan ni borran. No se cambian contraseñas, claves, vínculos de huella/rostro o identidades laborales.

## Contrato conservado y comportamiento

- **DESVINCULACION:** requiere situación VIGENTE. Marca DESVINCULADO en WebSite+, cierra el periodo abierto si existe y revoca sesiones de todas las cuentas vinculadas. No cambia el Activo/Inactivo de asistencia. Repetirla produce 409.
- **REINGRESO:** requiere situación DESVINCULADO y trabajador ya Activo en asistencia. No fuerza la reactivación del sistema externo. Abre un periodo nuevo, sin restaurar tokens anteriores. Si el desvinculado conserva un periodo abierto inconsistente, se rechaza con `LABOR_PERIOD_CONFLICT` para revisión.
- **REACTIVACION:** solo registra un retorno ya observado: situación VIGENTE, asistencia Activo y última observación complementaria Inactivo. No inventa una fecha de pausa, no escribe asistencia y no crea otro periodo si la relación continúa. Sin evidencia previa devuelve 409; no se usa como sinónimo de reingreso de un desvinculado.
- Se conservan cambios solo del día actual. No se implementaron cambios retroactivos, programados, conversión de tipo ni suspensión manual de asistencia.
- `/personal/{id}` conserva periodos, eventos y permisos históricos; la consulta de permisos usa expresamente la conexión de asistencia destinada a lectura.
- La respuesta exitosa de transición conserva HTTP 200 y añade `data.id_evento`, `id_trabajador`, `situacion` e `id_periodo`. Permiso de lectura no concede autorización para transiciones: siguen exigiéndose `empleados.administrar` y `empleados.ver` con el alcance correspondiente.
- Un cambio por la API de WebSite+ revoca las sesiones inmediatamente al confirmar la transacción. La continuación añade observación en consultas autorizadas de empleados y CLI opcional; un access/refresh restringido también persiste la observación y revoca las sesiones de todas las cuentas del trabajador. Una petición ya ejecutándose no puede deshacerse. No se añadió vigilancia continua del sistema externo.

## Pruebas de la implementación inicial (antes de esta continuación)

Laboratorio MySQL local en puerto 13317 y esquemas aleatorios `audit…_websiteplus_test` / `audit…_asistencia_test`, verificando la ruta real de datos antes de crear fixtures. El usuario de ejecución dispone de SELECT en asistencia y DML únicamente en WebSite+; el administrador del laboratorio prepara los datos sintéticos. No se modificaron bases reales.

- **420 comprobaciones de integración HTTP/MySQL aprobadas**, incluidas 67 nuevas de empleados sobre las 353 previas de autenticación/backend. Registro privado: `backend/storage/face-evaluation/employee-labor-final-20261022.log`.
- **15 pruebas de validación sin base de datos aprobadas:** eventos admitidos, motivo vacío/largo, 500 caracteres Unicode y fechas inválidas/pasadas/futuras.
- Listado/detalle con PROPIO, EQUIPO, AGENCIA, TODOS y denegación explícita; lectura sin permiso para administrar transiciones.
- Refresh válido con access vencido y trabajador activo, también con permiso aprobado. Para PASSWORD, BIOMETRIA y FACIAL se prueba refresh directo tras inactivación o desvinculación, sin ejecutar antes la validación de access, y que reactivar no resucite esos tokens.
- Registro y firmas V011/facial de laboratorio usan los endpoints existentes y claves sintéticas. Son pruebas reales del protocolo y de sesiones MySQL, no pruebas físicas nuevas de huella, rostro o PAD.
- Desvinculación de dos cuentas del mismo trabajador; todos sus tokens quedan revocados antes de usarlos. Una cuenta secundaria no se consulta durante la baja y su refresh también sigue rechazado después del reingreso.
- Periodos cerrados preservados, un solo periodo abierto, cronología inválida rechazada, tipo histórico anterior preservado al reingresar con tipo actual distinto.
- Cambio externo de un permiso aprobado a rechazado reflejado en el detalle y condición operativa. Los flags `procesado_asistencia` no se modifican al leer.
- Error inyectado al guardar el evento: se revierten situación y cierre de periodo y se conserva la sesión válida. Cuatro procesos PHP concurrentes: un éxito, tres conflictos y un solo evento añadido.
- Las operaciones de empleados dejan idénticas las evaluaciones y revisiones KPI del laboratorio.

Comandos usados, con el servidor propio del laboratorio previamente iniciado:

```powershell
$env:WSP_TEST_PORT = '13317'
$env:WSP_TEST_DATA_DIR = (Resolve-Path 'backend/storage/mysql-integral-20261017').Path
$env:WSP_TEST_FACE_LOGIN_OPT_IN = '0'
& C:/xampp/php/php.exe backend/tests/labor_transition_validation_unit.php
& C:/xampp/php/php.exe backend/tests/integration.php
```

El helper `labor_transition_process.php` solo admite configuración y bases de pruebas dentro del almacenamiento local; no se despliega como endpoint. No se imprimen tokens, firmas ni descriptores. No se compila ni reinstala Android por esta revisión exclusivamente PHP.

## Decisiones pendientes que requieren aprobación

1. **Permisos/vacaciones parciales y KPI:** actualmente `LaborPolicy.syncLeaves()` marca todo el periodo solapado como No exigible, incluso si el permiso cubre solo una parte. No inventé prorrateo, descuentos o una exención alternativa y no modifiqué esa función ni los controladores KPI/dashboard. El responsable de KPI y la empresa deben aprobar cómo tratar un día, parte del mes, varios permisos y periodos personalizados.
2. **Permisos cancelados o rechazados posteriormente:** la vista de empleados se actualiza al leer, pero la sincronización KPI existente no restaura automáticamente una evaluación que ya quedó No exigible. Aprobar el procedimiento con su responsable antes de automatizarlo.
3. **Pausas y observaciones de asistencia:** el mecanismo de observación ya está implementado. Sigue pendiente acreditar fechas y motivos de pausas contractuales y aprobar la frecuencia de observación si se necesita seguimiento sin solicitudes. Una inactividad aislada no identifica su causa y una secuencia no observada no puede recuperarse automáticamente.
4. **Periodos iniciales e importados:** no se rellenan retrospectivamente periodos faltantes ni se corrigen fechas inconsistentes sin revisión. Las fechas de periodos tienen granularidad de día; no calculan fracciones de jornada, nómina ni efectos de un retorno en el mismo día.
5. La validación de despliegue con datos reales y permisos efectivos del servidor sigue pendiente de autorización. No requiere migraciones nuevas para estas correcciones; no se preparó ni ejecutó SQL de producción.

## Archivos de esta revisión

- Modificado `backend/src/Controllers/PersonalController.php`.
- Nuevo `backend/src/Employees/LaborTransitionService.php`.
- Modificado `backend/tests/integration.php` para incluir los nuevos casos.
- Nuevos `backend/tests/employee_labor_cases.php`, `backend/tests/labor_transition_validation_unit.php` y `backend/tests/labor_transition_process.php`.
- Este informe.

En la revisión inicial `TokenService.php` no necesitó cambios; en esta continuación recibe únicamente la observación/revocación laboral compartida descrita arriba. `LaborPolicy.php`, los flujos de login, huella, rostro, navegación, KPI, administración y las migraciones conservan su código. Los cambios locales preexistentes en README, IDE y Gradle se conservaron. No se eliminó ningún archivo, no se cambió producción y no se realizó commit, push ni merge.
