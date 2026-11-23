# Propuesta cargo–formulario KPI para aprobación

Fuente: consulta de solo lectura que el usuario realizó el 14-11-2026 en `wsnet_bd_asistencia.cargos` y `wsnet_websiteplus.cargos`. Ambos catálogos muestran los mismos 12 registros. Los identificadores y nombres de la tabla siguiente son reales; la correspondencia funcional **todavía es una propuesta empresarial**, no una asignación oficial. No modifica cargos, usuarios ni permisos.

| ID cargo | Nombre real | Formulario propuesto | Estado |
|---:|---|---|---|
| 1 | Gerente General | `GERENCIA` | Aprobar alcance, metas y permisos |
| 2 | Jefe de Agencia | Sin asignar | Revisar funciones y permisos efectivos; no heredar `GERENCIA` |
| 3 | Creativa/Diseño | `DISENADOR` | Aprobar metas propias |
| 4 | Programador Web | `PROGRAMACION` | Aprobar metas propias |
| 5 | Ejecutivo Comercial | `VENTAS` | Aprobar metas propias |
| 6 | Programador Junior | `PROGRAMACION` | Aprobar metas distintas de cargo 4 |
| 7 | Diseñador Junior | `DISENADOR_JUNIOR` | Aprobar metas propias |
| 13 | Community Manager | `COMMUNITY` | Aprobar metas propias |
| 14 | Diseñador Senior | `DISENADOR_SENIOR` | Aprobar metas propias |
| 15 | Community Manager Junior | `COMMUNITY` | Aprobar metas distintas de cargo 13 |
| 16 | Community Manager Senior | `COMMUNITY` | Aprobar metas distintas de cargos 13 y 15 |
| 17 | Creativo / Videógrafo | `VIDEO` | Aprobar metas propias |

`ADMINISTRACION`, `ASISTENCIA_ADMIN`, `CONTABILIDAD`, `FOTOGRAFIA` y `TRAFFICKER` son los cinco formularios conservados sin cargo equivalente explícito. Mantenerlos sin correspondencia hasta que la empresa determine el cargo real o autorice uno existente. No se crean cargos.

El acceso Android oficial consulta `/kpi/formularios/permitidos`: el backend toma `id_cargo` e `id_trabajador` de la sesión, comprueba `kpi.ver` y `kpi.entregar`, y retorna únicamente correspondencias con `aprobado_en` definido. Los nombres se muestran como identidad, no se usan para conceder permisos. El enrutador heredado basado en cadenas de cargo permanece en el código original, pero este acceso autenticado monta el fragmento a partir del tipo devuelto por el servidor. Así, `Programador Junior` y `Programador Web` comparten pantalla `PROGRAMACION` solo si ambas filas se aprueban; las metas oficiales deben configurarse por cargo.

El laboratorio aislado probó los 11 pares con esos IDs, el cargo 2 sin formulario, la ausencia de mapeo y la denegación 403 cuando se retira el permiso efectivo. Esto no valida que una persona real de cada cargo tenga hoy los permisos requeridos. Para decidir el cargo 2 y publicar las filas, la empresa debe revisar resultados autorizados de `/auth/me` para sus titulares, las funciones del puesto, el alcance de agencia, códigos KPI, pesos, metas y períodos. Las tablas oficiales de definiciones, asignaciones y evaluaciones estaban vacías en la última lectura comunicada; no se introducen valores ficticios.

Tras aprobar filas concretas, rellenar una copia privada de `docs/APROBACION_FORMULARIOS_KPI.example.json` con referencia de aprobación e ID del usuario que aprueba. El generador local `backend/bin/generate_approved_form_mappings.php` emite SQL para **solo esas filas**, con validación de existencia del cargo y del aprobador en la base destino. Ejecutarlo primero en staging autorizado, verificar el resultado y solicitar autorización separada para producción. No poner el JSON aprobado en Git ni en el ZIP de despliegue.
