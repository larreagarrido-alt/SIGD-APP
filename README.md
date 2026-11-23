# SIGD · Sistema Integral de Gestión del Desempeño

Aplicación Android para registrar actividades, consultar reportes KPI y acompañar el bienestar del equipo. Comparte las cuentas, cargos y estados laborales del sistema web de Asistencias mediante una API PHP. El administrador revisa los reportes recibidos y decide si son válidos para el ranking.

> **Estado:** el proyecto compila y pasó pruebas locales con datos sintéticos. La [revisión funcional](docs/REVISION_PRE_GITHUB_2026-11-22.md) documenta pendientes antes de considerarlo listo en producción. Subir el código a GitHub no publica la API en cPanel ni instala el APK.

## Qué resuelve

SIGD reúne formularios por cargo, resultados, cronogramas y bienestar en una sola app. No exige crear una segunda cuenta por trabajador. Consulta Asistencias para saber su cargo, estado laboral y jornadas exigibles; así una vacación, un cambio de cargo o un día sin turno pueden reflejarse en la obligación KPI.

| Módulo | Trabajador | Administrador |
| --- | --- | --- |
| Cuenta y personal | Entra con su cuenta de Asistencias y consulta su perfil. | Consulta trabajadores según sus permisos. |
| KPI y reportes | Completa el formulario de su cargo y envía actividades. | Ve la ficha y el porcentaje calculado; aprueba o rechaza su validez. |
| Horarios | Consulta el cronograma de actividades de su cargo. | Edita y publica los cronogramas. |
| Bienestar | Responde cuestionarios habilitados y consulta su historial. | Gestiona campañas y consultas permitidas. |
| Ranking y premios | Consulta resultados y premios disponibles. | Consulta evaluaciones aprobadas y administra premios. |
| Avisos | Recibe recordatorios de jornadas KPI pendientes. | Consulta avisos administrativos. |

El **cronograma de actividades de SIGD** y las **marcaciones del sistema de Asistencias** son cosas distintas.

## Arquitectura

```text
App Android SIGD
       │ HTTPS + sesión
       ▼
API PHP de SIGD ─────► wsnet_websiteplus
       │                Formularios, evaluaciones, horarios,
       │                bienestar, puntos y premios
       ▼
wsnet_bd_asistencia
Cuentas, trabajadores, cargos, estado laboral y jornadas exigibles
```

La app no crea otra cuenta. El backend lee el estado laboral actual de Asistencias. Si el cargo o el estado cambia, la app debe consultar el servidor de nuevo. Las bases tienen funciones distintas y se configuran por separado.

### Flujo de un KPI

1. El trabajador abre el formulario permitido para su cargo y registra sus resultados.
2. La API recibe y conserva la ficha. **Recibido** no significa **aprobado**.
3. El servidor calcula el porcentaje desde cantidades registradas, metas por cargo y turnos exigibles. Los niveles porcentuales son comunes a todos los cargos.
4. El administrador revisa la ficha y aprueba o rechaza su validez; no inventa un porcentaje manual.
5. Solo una evaluación aprobada acredita puntos y entra al ranking. El mes nuevo se consulta desde cero.

El menú **Metas del cálculo oficial** permite cambiar las cantidades que usa el servidor para las fichas nuevas. Los textos de la lámina siguen siendo editables por separado; el administrador debe mantener ambos valores coherentes. Cada ficha recibida conserva la revisión numérica con la que será evaluada. Véase la [guía de publicación de esta corrección](docs/PUBLICAR_CORRECCIONES_METAS_Y_ALTAS_2026-11-22.md).

## Carpetas principales

| Ruta | Contenido |
| --- | --- |
| [`app/`](app/) | App Android en Java y XML. |
| [`backend/public/index.php`](backend/public/index.php) | Entrada y rutas de la API PHP. |
| [`backend/src/`](backend/src/) | Autenticación, permisos y lógica del sistema. |
| [`backend/config/config.example.php`](backend/config/config.example.php) | Configuración de ejemplo sin secretos. |
| [`database/migrations/`](database/migrations/) | Cambios versionados de la base SIGD. |
| [`database/seeds/`](database/seeds/) | Plantillas y metas para publicación controlada. |
| [`integration/asistencia/`](integration/asistencia/) | Cambios de integración para la web de Asistencias. |
| [`docs/`](docs/) | Contratos, guías y auditorías. |

## Abrir y compilar

Requisitos: Android Studio, JDK 11 o compatible con el proyecto, Android SDK 37 y conexión para obtener dependencias Gradle la primera vez.

1. Clona el repositorio y abre **la carpeta raíz** en Android Studio, donde está `settings.gradle.kts`. No abras solo `app/`.
2. Espera Gradle Sync. Android Studio creará `local.properties` con la ruta del SDK de tu equipo.
3. Compila y ejecuta las pruebas unitarias en PowerShell:

   ```powershell
   .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
   ```

4. Ejecuta la configuración `app` en un emulador o teléfono. El APK de `app/build/outputs/apk/debug/` sirve para desarrollo, no es una entrega firmada.

La URL de la API está en [`ApiClient.java`](app/src/main/java/pe/net/ws/kpiweb/core/network/ApiClient.java). Si pruebas contra un servidor local, cambia esa dirección para tu entorno. No uses datos reales de producción en las pruebas.

## API y pruebas

Para un entorno PHP local, copia [`config.example.php`](backend/config/config.example.php) a `backend/config/config.php` y completa las conexiones y secretos locales. `config.php` está excluido de Git. La base SIGD y la base de Asistencias deben apuntar a sus esquemas respectivos.

La suite de integración usa una instancia MariaDB **sintética y aislada**. Ejemplo en Windows con XAMPP instalado en `C:\xampp`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File backend/tests/run_employees.ps1 `
  -PhpExe C:/xampp/php/php.exe -MariaDbBin C:/xampp/mysql/bin
```

La última revisión local obtuvo compilación y pruebas Android correctas, más **828 comprobaciones de integración** correctas. La suite no accedió a producción. Consulta [requisitos y cobertura](docs/EMPLEADOS_PRUEBAS.md) antes de ejecutarla.

## Publicación y seguridad

GitHub almacena el **código fuente**. Publicar una versión funcional también requiere migraciones pendientes, PHP actualizados en cPanel, configuración privada del servidor y un APK de esa misma versión. Haz primero un respaldo actual y comprueba al final acceso, envío, revisión y ranking con cuentas autorizadas.

- [Revisión funcional y pendientes](docs/REVISION_PRE_GITHUB_2026-11-22.md)
- [Guía de publicación en cPanel](docs/GUIA_PUBLICACION_INTEGRAL_KPI_2026-11-14.md)
- [Integración con Asistencias](docs/INTEGRACION_ASISTENCIAS_CICLO_LABORAL_2026-11-22.md)
- [Limpieza controlada de pruebas](docs/PLAN_LIMPIEZA_FINAL_2026-11-22.md)
- [Índice completo de documentación](docs/INDICE.md)

No subas `backend/config/config.php`, respaldos SQL reales, fotos de trabajadores, `storage/`, APK generados, claves privadas ni archivos de configuración local de Android Studio.

## Límites conocidos

- Ryff contiene solo una demostración de tres preguntas; no hay un cuestionario validado para puntuar personas.
- El archivado del ranking anterior se activa al consultar el ranking, no exactamente a medianoche.
- Las altas nuevas requieren una clave inicial única y un cambio obligatorio en la web de Asistencias; esta corrección debe publicarse también allí.
- La limpieza de los datos de prueba identificados por el equipo sigue pendiente.

Consulta la [revisión previa a GitHub](docs/REVISION_PRE_GITHUB_2026-11-22.md) para el alcance y la evidencia de cada punto.
