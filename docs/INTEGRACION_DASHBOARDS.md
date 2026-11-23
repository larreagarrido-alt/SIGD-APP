# Integrar dashboards con autenticación

## Estado de `jair-integracion` — 24 de octubre de 2026

La rama actual conserva el flujo central ya descrito abajo. `DashboardRouter.destinations()` está vacío porque `features/kpi` y `features/admin` solo contienen sus README; no hay Activities de esos módulos que registrar. Los tres caminos de acceso en `LoginActivity` (contraseña, huella y rostro) terminan en `AccountNavigation.afterAuthentication()`, que abre `MainActivity`; esta entrada consulta `/auth/me` antes de seleccionar un destino. `ApiService` ya declara `/auth/me`, login, refresh, logout y las dos consultas de dashboard/KPI; las demás rutas de negocio corresponden a los servicios de los futuros módulos.

En esta revisión, `/auth/me` con 401 limpia la sesión activa y vuelve al login; 403 presenta acceso denegado sin abrir un módulo; un fallo de conexión permite reintentar. `AccountActivity` también comprueba `/auth/me` al abrirse o reanudarse y mantiene deshabilitada Seguridad facial hasta verificar la identidad. Cerrar sesión sigue disponible durante un fallo de red. No se cambiaron los mecanismos de login ni sus claves o vínculos biométricos.

Se agregaron pruebas locales para selección de destinos por permiso/rol, ausencia de módulos, rechazo de Cuenta como dashboard, identidad, 401, 403, permisos ausentes, fallo de red y respuestas tardías. El 24 de octubre, `:app:testDebugUnitTest :app:assembleDebug` terminó con **12 pruebas aprobadas, cero fallos, cero errores y APK debug generado**. El error previo `Unable to establish loopback connection` provenía de `Selector.open()`/sockets Unix del JDK al usar la carpeta temporal predeterminada de Windows; se reprodujo fuera de Gradle en tres JDK. Se resolvió para esa ejecución con `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Users\Public\codex-java-sockets`, carpeta corta creada fuera del proyecto. No se cambió la configuración Gradle ni el código Android para corregir el entorno. Los recuentos históricos indicados más abajo pertenecen a otra revisión.

Al recibir las dos ramas, revisar sus Activities, permiso efectivo requerido, servicio API y declaración `exported=false`; entonces registrar únicamente las Activities reales en `destinations()` con prioridad explícita. Cada módulo debe proteger sus pantallas al reanudarse y manejar 401, 403 y red usando el perfil vigente y la autorización del backend. Después ejecutar pruebas unitarias, compilación, lint y pruebas manuales de contraseña, huella y rostro con cuentas autorizadas en staging; comprobar también el retorno a Cuenta y el logout. No se debe interpretar el acceso a la Activity como autorización para consultar datos fuera del alcance concedido por la API.

La rama `jair-proyecto` conserva contraseña, huella V011 y rostro. Los tres guardan la sesión y llaman al mismo punto de entrada. No hay dashboards de KPI o administración inventados en esta entrega.

## Recorrido

`LoginActivity → AccountNavigation.afterAuthentication → MainActivity → GET /auth/me → DashboardRouter → Activity real del módulo`.

`MainActivity` es únicamente una entrada de navegación: obtiene identidad, rol y códigos de permiso actuales del backend y comprueba que el usuario coincida con el de la sesión. No decide por IDs de rol guardados ni por valores recibidos en un Intent. El perfil facial abreviado tampoco se usa como fuente de permisos: se consulta `/auth/me` después de cualquiera de los tres métodos.

Mientras no haya un dashboard registrado que corresponda al perfil, abre `AccountActivity`: **Cuenta y seguridad**, con un aviso de módulo principal todavía no conectado, Seguridad facial y Cerrar sesión. Esta pantalla no es el dashboard. El router impide registrarla como tal, incluidas sus subclases, así como Login, Seguridad facial o la propia entrada MainActivity.

Si el token devuelve 401 o la identidad no coincide, se limpia solamente la sesión activa y se vuelve al login, conservando los accesos biométricos vinculados. Ante fallo de red, 403 o perfil sin permisos válidos, no se elige un dashboard: se ofrece reintentar, entrar a Cuenta y seguridad o cerrar sesión. No se borra la vinculación por un fallo de conexión.

## Qué conecta cada compañero

| Responsable | Cambios al integrar |
|---|---|
| Jair — autenticación | Mantener login, biometría, sesión, entrada y Cuenta y seguridad. No definir el diseño de los dashboards. |
| Dashboard/KPI | Añadir la Activity real de su módulo y declararla en el manifest con `exported=false`. Registrar su destino y permiso real en `DashboardRouter.destinations()`. `kpi.ver` existe en el backend; usarlo solo si corresponde al contenido de esa entrada. |
| Administración | Añadir su Activity real y declararla con `exported=false`. Registrar su entrada con `dashboard.admin`, que existe en el backend. No conceder administración por tener solamente `auth.perfil` ni por suponer que cierto número de rol es administrador. |

El punto que se editará es `app/src/main/java/pe/net/ws/kpiweb/navigation/DashboardRouter.java`, método `destinations()`. Devolver una lista de `Destination` con las clases reales ya integradas. Ejemplo de una entrada, donde `actividadRealDelModulo` representa su `Class<? extends Activity>` existente:

```java
new DashboardRouter.Destination(
    actividadRealDelModulo,
    "dashboard.admin",
    java.util.Collections.emptySet()
)
```

No copiar un nombre de Activity ficticio ni crear una pantalla vacía para satisfacer el registro. El conjunto vacío significa cualquier rol que tenga ese permiso efectivo. Si el módulo también exige ciertos roles, poner sus IDs confirmados por el backend en el conjunto: se exigirán permiso **y** rol. La primera ruta autorizada gana; colocar primero las entradas específicas (por ejemplo administración) y después las generales. Una lista vacía es válida mientras el equipo aún no suba sus módulos.

El router organiza la interfaz; la autorización de datos sigue siendo responsabilidad del backend en cada endpoint, incluido el alcance PROPIO/EQUIPO/AGENCIA/TODOS. No convertir una ruta visible en permiso para consultar cuentas ajenas. Los dashboards deben volver a comprobar la sesión/permisos al reanudarse según su ciclo de vida; pueden reutilizar `SessionEntryCoordinator` para obtener un perfil vigente y gestionar 401.

## Cuenta, seguridad y logout desde cualquier dashboard

En su menú o perfil, abrir la pantalla común:

```java
AccountNavigation.openAccount(this);
```

Para cerrar sesión desde una Activity que extienda AppCompatActivity:

```java
AccountNavigation.signOut(this);
```

Se conserva el cierre de sesión del módulo de autenticación: revocar la sesión actual en la API, limpiar la sesión local y conservar la vinculación por defecto; también se mantiene la opción explícita de olvidar el acceso del teléfono y su flujo existente. Un error de conexión no impide cerrar la sesión local. No sustituirlo por borrar todas las preferencias o claves AndroidKeyStore.

No cambiar el launcher: sigue siendo `LoginActivity`. No conectar dashboards directamente a los callbacks de contraseña, huella o rostro. Conectar sus entradas únicamente en el router evita tres navegaciones diferentes y conflictos al integrar ramas.

## Validación de esta separación

Resultado del 22 de octubre de 2026: **155 pruebas unitarias aprobadas, cero fallos/errores** (134 existentes y 21 nuevas de navegación); `assembleDebug`, `assembleDebugAndroidTest` y `lintDebug` aprobados, sin errores de lint. Se conservan avisos de lint. Registro local: `backend/storage/face-evaluation/navigation-final-20261022.log`. Comparación de hashes e inventario: los dos archivos existentes de `features/kpi` y `features/admin` no cambiaron.

Se compilan APK normal y de pruebas y se ejecutan unitarias/lint. Las nuevas pruebas comprueban elección por permisos actuales, restricciones de rol, prioridad, ausencia de módulos, rechazo de pantallas de cuenta como dashboard, identidad distinta, 401, red y permisos ausentes. Se conservan las pruebas de autenticación existentes. Las pruebas de persistencia de logout ahora abren `AccountActivity`, donde está esa acción; los anfitriones debug de pruebas faciales también utilizan Cuenta, no un dashboard ficticio.

No se ejecutan estos anfitriones contra producción ni se instala en el teléfono para esta tarea. La prueba física facial del paquete anterior sigue siendo histórica, no una prueba física de esta nueva navegación. Al integrar los módulos, comprobar los tres métodos de acceso con cuentas autorizadas, el dashboard correcto por permisos y el menú Cuenta/Seguridad/Cerrar sesión.

No se cambiaron PHP, SQL, cPanel, KPI o administración por esta separación. No se realizó commit, push ni merge. Los APK/ZIP sellados de entregas anteriores no se reescriben automáticamente; al publicar una nueva entrega habrá que generar su paquete desde esta revisión.

## Archivos de esta revisión

Modificados:

- `app/src/main/java/pe/net/ws/kpiweb/MainActivity.java`: entrada que valida perfil y delega la navegación.
- `app/src/main/res/layout/activity_main.xml`: espera/reintento, sin checklist ni dashboard provisional.
- `app/src/main/java/pe/net/ws/kpiweb/features/auth/LoginActivity.java`: delegar únicamente la salida común tras autenticar; no cambiar los tres mecanismos de acceso.
- `app/src/main/AndroidManifest.xml`: declarar Cuenta como Activity interna; conservar launcher.
- `app/src/main/java/pe/net/ws/kpiweb/core/session/SessionManager.java`: añadir lectura del ID de usuario guardado para comparar con `/auth/me`, sin cambiar guardado/borrado de sesión.
- `README.md`: enlace a esta guía.

Nuevos:

- `app/src/main/java/pe/net/ws/kpiweb/AccountActivity.java`, `app/src/main/res/layout/activity_account.xml`, `app/src/main/res/values/navigation_strings.xml`: cuenta/seguridad y textos de entrada.
- `app/src/main/java/pe/net/ws/kpiweb/navigation/DashboardRouter.java`: único registro de dashboards reales, según permiso y rol opcional.
- `app/src/main/java/pe/net/ws/kpiweb/navigation/SessionEntryCoordinator.java`: comprobación del perfil, errores y cancelación de respuestas tardías.
- `app/src/main/java/pe/net/ws/kpiweb/navigation/AccountNavigation.java`: salida del login, menú Cuenta y logout compartidos.
- `app/src/test/java/pe/net/ws/kpiweb/navigation/DashboardRouterTest.java` y `SessionEntryCoordinatorTest.java`: 21 pruebas de entrada/rutas.
- Esta guía.

Ajustados exclusivamente para pruebas: `BiometricSessionPersistenceTest.java` y `FaceNativeHttpInstrumentedTest.java` bajo `app/src/androidTest/java/pe/net/ws/kpiweb/features/auth/`; `app/src/debug/AndroidManifest.xml`; sustituir el anfitrión debug `FaceSelfServiceDashboardHarnessActivity.java` por `FaceAccountHarnessActivity.java` en `app/src/debug/java/pe/net/ws/kpiweb/features/auth/`. El cambio evita que una prueba siga tratando Cuenta como dashboard o abra accidentalmente la nueva entrada contra el servidor real.
