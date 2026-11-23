# SIGD

Aplicación Android para registrar actividades, consultar KPI y responder cuestionarios de Bienestar. El personal administrativo consulta trabajadores, reportes y resultados según los permisos de su cuenta. La app se comunica con una API PHP y usa MariaDB.

## Cómo se conectan los sistemas

```text
Android SIGD → API SIGD → wsnet_websiteplus
                            ↘ wsnet_bd_asistencia (cuentas y trabajadores)
```

Las cuentas, los cargos y el estado laboral vienen del sistema web de Asistencias. KPIWeb no crea una segunda cuenta para cada trabajador. En una prueba local, una cuenta nueva de Asistencias pudo ingresar a la app; al pasarla a **Inactivo**, se bloqueó el acceso, y al volver a **Activo**, pudo ingresar con la misma cuenta. Para ver un formulario KPI, el cargo también debe tener una correspondencia aprobada.

La URL configurada en Android está en [`ApiClient.java`](app/src/main/java/pe/net/ws/kpiweb/core/network/ApiClient.java). La API recibe las rutas en [`backend/public/index.php`](backend/public/index.php). Una respuesta correcta de `/health` solo confirma que la API responde; por sí sola no comprueba el acceso a las bases de datos.

## Partes principales

| Parte | Qué permite hacer |
| --- | --- |
| Acceso y cuenta | Iniciar sesión, consultar el perfil y administrar los métodos de acceso disponibles. |
| Mis KPI | Abrir el formulario correspondiente al cargo y enviar una ficha completa al servidor. |
| Reportes administrativos | Ver las fichas confirmadas por trabajador y abrirlas con su formato visual. |
| Bienestar | Responder las evaluaciones semanal y mensual, consultar el historial propio y ver las sugerencias disponibles. |
| Personal y avisos | Consultar trabajadores y resúmenes permitidos por el servidor. |
| Ranking y premios | Mostrar el resultado del mes actual a partir de KPI aprobados, consultar meses anteriores y ver premios disponibles. |

Un formulario **enviado** no equivale a una evaluación KPI **aprobada**. La API calcula el porcentaje frente a la meta oficial del cargo; el administrador decide si el reporte es válido. Solo las evaluaciones aprobadas aportan puntos y ranking. El mes actual comienza en cero y los meses anteriores se conservan como historial cuando están listos para el cierre.

La sección Ryff del ZIP de Dani contiene tres preguntas de muestra. No es un cuestionario completo para aplicar o puntuar. El panel de Bienestar organizacional y el flujo de campañas están preparados en el código local; su publicación y uso requieren las preguntas completas, los permisos y las decisiones de privacidad correspondientes.

## Estructura del proyecto

| Ruta | Contenido |
| --- | --- |
| `app/` | Aplicación Android en Java y XML. |
| `backend/public/` | Entrada pública y rutas de la API. |
| `backend/src/` | Autenticación, permisos y controladores PHP. |
| `backend/config/config.example.php` | Ejemplo de configuración sin credenciales. |
| `database/migrations/` | Cambios de estructura de la base de datos, por versión. |
| `docs/` | Revisiones, contratos y guías de publicación. |

## Abrir y probar en local

1. Abre la carpeta raíz en Android Studio y sincroniza Gradle. El proyecto usa Java 11, Android SDK 37 y Gradle Wrapper.
2. Compila y ejecuta las pruebas de Android:

   ```powershell
   .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
   ```

3. Para la API local, copia `backend/config/config.example.php` como `backend/config/config.php` y completa únicamente los datos de tu entorno. Ese archivo privado no se debe subir al repositorio.
4. Prueba los cambios de base y PHP con datos inventados en un MariaDB aislado. La guía de pruebas está en [`docs/EMPLEADOS_PRUEBAS.md`](docs/EMPLEADOS_PRUEBAS.md).

El APK de `app/build/outputs/apk/debug/` es para desarrollo; no es una entrega firmada para distribución.

## Publicación y estado conocido

La publicación requiere comprobar primero un respaldo restaurable, revisar qué migraciones ya existen, aplicar solo las que faltan, subir los PHP que correspondan y probar el recorrido en un teléfono con cuentas autorizadas. No se deben volver a importar migraciones ya aplicadas ni copiar `config.php` local a cPanel. La guía detallada está en [`docs/GUIA_PUBLICACION_INTEGRAL_KPI_2026-11-14.md`](docs/GUIA_PUBLICACION_INTEGRAL_KPI_2026-11-14.md).

Según las capturas compartidas hasta el 22/11/2026, el acceso, los formularios por cargo, la consulta de reportes y las evaluaciones de Bienestar funcionaron en el teléfono. Las pruebas locales de Android y de la API con base sintética pasan; esto no verifica el estado actual de cPanel ni la navegación visual en cada dispositivo. El ranking puede estar vacío si no hay evaluaciones aprobadas. Antes de presentar puntos o premios como operativos, comprueba la publicación del servidor, el APK instalado y el recorrido con cuentas autorizadas.

Para el estado y los límites de cada parte, consulta [`docs/RANKING_PREMIOS_INTEGRACION_2026-11-18.md`](docs/RANKING_PREMIOS_INTEGRACION_2026-11-18.md), [`docs/BIENESTAR_RYFF_IMPLEMENTACION_2026-11-18.md`](docs/BIENESTAR_RYFF_IMPLEMENTACION_2026-11-18.md) y [`docs/AUDITORIA_METAS_KPI_ZIPS_2026-11-18.md`](docs/AUDITORIA_METAS_KPI_ZIPS_2026-11-18.md).

La revisión previa a GitHub, con lo comprobado y lo que sigue pendiente, está en [`docs/REVISION_PRE_GITHUB_2026-11-22.md`](docs/REVISION_PRE_GITHUB_2026-11-22.md).
