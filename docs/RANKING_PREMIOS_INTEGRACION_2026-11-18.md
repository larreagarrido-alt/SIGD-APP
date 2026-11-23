# Ranking y premios: estado de integración local

Rama `jair-integracion`. No se ejecutaron cambios en cPanel, MySQL de producción ni cuentas reales durante esta integración. No se creó commit, push ni merge.

## Código preparado

- `V022__premios_publicaciones.up.sql` amplía el catálogo existente sin borrar premios. La publicación efectiva se calcula con `NOW()` de MySQL: borrador, programado, activo, vencido o desactivado. La imagen se guarda fuera de `public_html`, con MIME real, dimensiones, límite de 5 MB y nombre generado por el servidor. Los premios nuevos no habilitan canje ni descuentan puntos.
- `V023__kpi_revision_y_cierre.up.sql` conserva las tablas oficiales KPI existentes. Agrega vínculo auditable del formulario con su evaluación, aprobación explícita de una política de comparación entre cargos y un cierre único por mes. Inserta el código `ranking.cerrar` en el catálogo de permisos, sin concederlo a ningún rol.
- `KpiOfficialAdminController.php` configura indicadores con meta, peso, puntos base, unidad, cargo, período y responsable explícitos; asigna trabajadores; revisa reportes; exige evaluación aprobada antes del cierre. `KpiController.php` impide aprobar reportes modificados o meses cerrados.
- Android agrega gestión de premios, configuración y revisión KPI desde el ranking, y consulta meses cerrados. La vista de trabajadores vuelve a consultar el catálogo activo y no usa puntos para mostrar publicaciones.

## Regla de cálculo que requiere decisión de WEBSITE

El cierre disponible usa el promedio de los porcentajes aprobados ponderado por `peso_snapshot`. Ordena por porcentaje descendente, luego puntos confirmados descendentes; un empate exacto en ambos valores comparte puesto. El ID del trabajador solo estabiliza el orden visual. El responsable con `ranking.cerrar` debe aprobar expresamente por mes la comparación entre cargos y justificarla. Sin esa aprobación, el servidor rechaza el cierre. La aplicación no inserta metas, pesos, puntos base ni posiciones por defecto.

El último conteo compartido por el usuario fue: 10 formularios recibidos, 0 KPI oficiales, 0 evaluaciones aprobadas y 0 ranking. Es un dato de aquella consulta, no una lectura actual de producción. Esos 10 formularios no se convierten automáticamente en evaluaciones.

## Permisos pendientes de aprobación y aplicación

Para operar los nuevos flujos en producción hacen falta decisiones explícitas sobre `kpi.administrar` (configurar y asignar), `kpi.revisar` (evaluar a trabajadores del alcance autorizado), `ranking.cerrar` (aprobar política y cerrar), `premios.administrar` (publicar) y `premios.ver` (catálogo de colaboradores y practicantes). La migración crea solo el código nuevo `ranking.cerrar`; no otorga permisos. Los permisos existentes deben conservar su alcance y las denegaciones por usuario.

También faltan los indicadores y reglas reales aprobados por WEBSITE: meta, unidad, peso, puntos base, período, responsable y correspondencia exacta con cada cargo, además de la aprobación de la fórmula para comparar cargos distintos. No se debe elegir esos valores a partir de las pantallas de ejemplo del ZIP.

## Publicación controlada pendiente

1. Verificar un respaldo SQL y de los PHP actuales con restauración aislada comprobada. Confirmar el esquema objetivo y las versiones ya aplicadas.
2. Aplicar únicamente V022 y V023 que falten, en ese orden, primero en staging equivalente. No volver a importar V015–V021 si ya están presentes.
3. Subir `PremiosPublicacionesController.php`, `KpiOfficialAdminController.php`, `RankingController.php`, `KpiController.php` y `public/index.php` juntos; conservar copia verificable de los archivos reemplazados. Verificar que `backend/storage/premios` está fuera del directorio público y que PHP puede escribir allí.
4. Aprobar y conceder los permisos mínimos correspondientes, luego registrar los indicadores y política reales mediante las pantallas administrativas. No habilitar cierre si quedan reportes modificados o evaluaciones pendientes.
5. Compilar, instalar y comprobar el APK nuevo con una cuenta administrativa y cuentas autorizadas de colaborador y practicante. Verificar creación, imagen, vigencia y lectura de premios; revisión, aprobación, cierre e historial del ranking. Probar cambio de cuenta, sesión vencida y falta de conexión.

## Pruebas locales

Las migraciones y la API superaron 748 comprobaciones en MariaDB aislado con datos sintéticos; `-NoV012` superó 112 en la variante sin V012 y `wp_rostros_cuenta`. Android compiló y pasó `:app:testDebugUnitTest`. Estas pruebas no sustituyen una prueba física y de permisos en staging equivalente ni confirman que el despliegue ya esté en producción.
