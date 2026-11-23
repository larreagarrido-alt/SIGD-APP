# Inventario de formularios KPI para la publicación

> Actualización posterior: este inventario describe el primer avance. La recuperación paginada, el navegador de reportes y los borradores administrativos V019 se detallan en [MATRIZ_APROBACION_EMPRESA_KPI_2026-11-12.md](MATRIZ_APROBACION_EMPRESA_KPI_2026-11-12.md). No publicar todavía un paquete parcial V018: faltan las pruebas físicas de los 13 recorridos y la conciliación de V018/V019 con staging.

Revisión local del 12-11-2026. Los códigos de esta tabla proceden de Android; **no son definiciones oficiales de MySQL**. El titular confirmó por consultas de solo lectura en phpMyAdmin que `wp_kpi_definiciones`, `wp_kpi_asignaciones` y `wp_kpi_evaluaciones` tienen **0 filas actualmente en producción**. Los cargos se resuelven desde `cargo_nombre` de `/auth/me` mediante `KpiNavigationHelper`; la exportación contiene la vista `cargos`, pero no las filas fuente de `wsnet_bd_asistencia`. Por tanto, ningún cruce cargo↔formulario↔KPI puede certificarse todavía como oficial.

**Comprobación del material disponible:** la copia de estructura confirma que existen las tres tablas y sus relaciones (`wp_kpi_asignaciones.id_kpi` y `wp_kpi_evaluaciones.id_kpi` apuntan a definiciones; las evaluaciones también apuntan a una asignación). La exportación completa recibida contiene **cero sentencias `INSERT INTO`** para esas tres tablas; el recuento actual de phpMyAdmin confirma las tres en cero. Las [consultas de solo lectura](CONSULTAS_SOLO_LECTURA_CPANEL_2026-11-12.sql) permiten volver a comprobarlas antes de publicar sin mostrar identificadores de trabajadores.

| Formulario Android | Cargo usado por el router o formulario | Código local al guardar | Persistencia actual | Configuración oficial pendiente |
| --- | --- | --- | --- | --- |
| `KpiSalesContainerFragment` | Asesor de Ventas / comercial | `KPI-VENTAS-01` | Registro local `PENDING` y datos del formulario en almacenamiento local | Confirmar `cargos.nombre`, definición con este `codigo` o mapa aprobado, meta, peso, período y evaluación por trabajador |
| `KpiDesignerFormFragment` | Diseñador Gráfico y variantes | Código generado como `KPI-` + tres letras del cargo + `-01`; no es estable para todas las variantes | `KpiDesignerMemoryRepository` y captura local `PENDING` | Fijar código por cargo/modalidad con una definición oficial; no usar la abreviatura generada para producción |
| `KpiJuniorContainerFragment` | Diseñador Gráfico Junior | `KPI-JR-DISENO-M` o `KPI-JR-DISENO-T` según turno | Memoria local y captura `PENDING` | Definir si son dos KPI oficiales por turno o uno con dimensión de turno; meta, peso y período por turno |
| `KpiSeniorContainerFragment` | Diseñador Gráfico Senior | `KPI-SR-DISENO` | `SharedPreferences` y captura `PENDING` | Definición oficial y vínculo entre producción/supervisión, meta y período |
| `KpiGeneralManagerContainerFragment` | Gerente General | `KPI-GG-DIRECCION` | `SharedPreferences` y captura `PENDING` | Definición oficial, unidad de medida de porcentaje, meta y permiso del cargo |
| `KpiFormFragment` | Programador / Programador Junior | `KPI-PROG-01` | `SharedPreferences` y captura `PENDING` | Confirmar si este código local coincide con un KPI oficial; meta del formulario y evaluación por fecha |
| `KpiAdministratorContainerFragment` | Administrador | Ninguno en el guardado actual | Solo `KpiAdministratorStorageManager` local | Definición(es), código(s), campos resumibles y regla de evaluación antes de conectar escritura |
| `KpiAdminAssistantContainerFragment` | Asistente Administrativo | Ninguno | Solo `KpiAdminAssistantStorageManager` local | Igual: definición, código, meta, peso, período y asociación por trabajador |
| `KpiAccountantContainerFragment` | Contador | Ninguno | Solo `KpiAccountantStorageManager` local | Igual; acordar métrica oficial de contabilidad |
| `KpiCommunityContainerFragment` | Community Manager | Ninguno | Solo `KpiCommunityStorageManager` local | Igual; acordar métrica oficial de publicaciones/interacciones |
| `KpiPhotographerContainerFragment` | Fotógrafo | Ninguno | Solo `KpiPhotographerStorageManager` local | Igual; acordar métrica oficial de sesiones/entregables |
| `KpiTraffickerContainerFragment` | Trafficker Digital | Ninguno | Solo `KpiTraffickerStorageManager` local | Igual; acordar métrica oficial de campañas/resultados |
| `KpiVideographerContainerFragment` | Videógrafo | Ninguno | Solo `KpiVideographerStorageManager` local | Igual; acordar métrica oficial de videos/entregables |

`KpiDynamicCaptureFragment` puede guardar capturas locales y pedir sincronización, pero sus definiciones deben venir de la API y corresponder a evaluaciones oficiales. `KpiAdminEditKpiFragment` y el gestor de KPI administrativos editan `KpiCustomRepository` local: sus cambios no son configuraciones persistidas en MySQL. Los códigos de ejemplo `KPI-COM-*`, `KPI-DIS-*`, `KPI-JUN-*`, `KPI-SEN-*` y `KPI-PROG-*` en métodos `seedDefault...` del repositorio no aparecen como registros oficiales en la exportación; esos métodos no deben utilizarse para crear asignaciones en producción.

## Regla para activar escritura por formulario

Para cada cargo se necesita una decisión aprobada con: nombre exacto del cargo en asistencia, `id_cargo`, `wp_kpi_definiciones.codigo`/`id_kpi`, categoría, versión, tipo de métrica, unidad, meta, peso, periodicidad, si exige evidencia/firma, fechas de asignación, trabajador destinatario y generación de `wp_kpi_evaluaciones`. Solo entonces puede validarse que el formulario entregue el valor correcto. El servidor rechaza un código no asignado y una meta/peso distintos de la evaluación; Android conserva el registro pendiente. Una captura cruda confirmada en V015 no sustituye el flujo oficial de borrador, firma, entrega y calificación de `wp_kpi_evaluaciones`.

**Consulta posterior de solo lectura**, con una cuenta autorizada y sin copiar resultados personales al informe: revisar `cargos`, `wp_kpi_definiciones`, `wp_kpi_asignaciones` y `wp_kpi_evaluaciones` mediante las vistas existentes y permisos adecuados. Si continúan vacías, primero debe aprobarse la configuración de KPI; no se inventarán filas ni concesiones de rol para hacer pasar las pantallas como conectadas.

## Propuesta de correspondencia para aprobación

La correspondencia propuesta es **por cargo visible en Android**, no una asignación oficial comprobada. Actualmente no hay definiciones, asignaciones ni evaluaciones KPI en producción, según el recuento comunicado desde phpMyAdmin. La empresa debe aprobar previamente el alta de cada definición, su `id_kpi`/`codigo` y asignación correspondiente; hasta entonces ninguna de las 13 pantallas puede activar entrega oficial.

| Pantalla | Cargo que debe confirmar asistencia | Definición oficial que debe señalar la empresa | Decisión específica pendiente |
| --- | --- | --- | --- |
| Ventas | Asesor de Ventas / comercial | Sin fila verificable; contrastar `KPI-VENTAS-01` local | Determinar ventas vs. visitas y unidad de medida |
| Diseñador | Diseñador Gráfico y variantes | Sin fila verificable; sustituir código generado | Precisar qué variantes comparten definición |
| Diseñador Junior | Diseñador Gráfico Junior | Sin fila verificable; contrastar los dos códigos locales de turno | Dos KPI por turno o uno con dimensión de turno |
| Diseñador Senior | Diseñador Gráfico Senior | Sin fila verificable; contrastar `KPI-SR-DISENO` | Producción individual y supervisión: una o dos definiciones |
| Gerencia General | Gerente General | Sin fila verificable; contrastar `KPI-GG-DIRECCION` | Fuente y fórmula del porcentaje mostrado |
| Programador | Programador / Programador Junior | Sin fila verificable; contrastar `KPI-PROG-01` | Una definición compartida o metas distintas por cargo |
| Administración | Administrador | Sin código local de entrega | Actividades que cuentan, unidad, meta y aprobador |
| Asistencia administrativa | Asistente Administrativo | Sin código local de entrega | Actividades que cuentan, unidad, meta y aprobador |
| Contabilidad | Contador | Sin código local de entrega | Indicador contable y fuente verificable |
| Community | Community Manager | Sin código local de entrega | Publicaciones e interacciones: una o dos definiciones |
| Fotografía | Fotógrafo | Sin código local de entrega | Sesiones y entregables: unidad y evidencia |
| Tráfico digital | Trafficker Digital | Sin código local de entrega | Campañas y resultados: métrica y fuente |
| Video | Videógrafo | Sin código local de entrega | Videos y entregables: unidad y evidencia |

Para aprobar cada fila, la empresa debe entregar el `cargos.id_cargo` y nombre exacto de asistencia, `wp_kpi_definiciones.id_kpi`/`codigo`/versión, fórmula y unidad del valor, meta, peso, período, asignación por `id_trabajador`, evaluación correspondiente y responsable de revisión. El cruce se validará por `/auth/me` y `/kpi/mis-kpis` con cuentas existentes autorizadas en staging; una coincidencia de texto del cargo por sí sola no concede permiso ni asignación.
