# Contrato de empleados para integración

Base: `/api/v1`. Todas las rutas siguientes requieren `Authorization: Bearer <ACCESS_TOKEN_DE_PRUEBA>` de la sesión existente. No guardar contraseñas en ejemplos ni utilizar tokens reales en documentación. Usar HTTPS fuera del laboratorio.

## Rutas disponibles

| Método y ruta | Permisos efectivos | Resultado |
|---|---|---|
| GET `/personal` | `empleados.ver`, alcance PROPIO/EQUIPO/AGENCIA/TODOS | Listado filtrado y paginado |
| GET `/personal/{id_trabajador}` | `empleados.ver` sobre ese trabajador | Datos vigentes, periodos, eventos y permisos |
| POST `/personal/{id_trabajador}/transicion` | **Ambos** `empleados.ver` y `empleados.administrar` sobre ese trabajador | Transición laboral explícita |

Usar `id_trabajador` como identidad laboral; no confundirlo con `id_usuario`. La denegación individual prevalece sobre el rol. PROPIO permite al trabajador asociado; EQUIPO incluye el propio y reportes directos; AGENCIA incluye propio y misma agencia; TODOS permite todos. Una cuenta sin trabajador no obtiene un equipo por tener supervisor `0`. Tener permiso de lectura nunca concede transiciones.

Los roles/permisos son los devueltos por el backend; no deducirlos del texto del rol, tipo de trabajador o nombre de pantalla. Las pantallas de KPI/administración pertenecen a sus responsables; este contrato no impone un dashboard.

## Listado

Filtros opcionales:

| Parámetro | Valores |
|---|---|
| `tipo` | `Practicante` o `Colaborador` |
| `estado` | `Activo` o `Inactivo` del origen; no usar `Desvinculado` en este filtro |
| `id_cargo` | Entero positivo hasta 2147483647 |
| `q` | Nombre completo o DNI, máximo 150 caracteres; `0` también es búsqueda válida. `%` y `_` conservan el significado de comodines LIKE |
| `limit` | 1–100; predeterminado 100 |
| `offset` | 0–100000; predeterminado 0 |

Los filtros se combinan dentro del alcance autorizado. `data.total` sigue siendo el número de **filas de esta página**, no un conteo global. Se añade `data.paginacion` sin retirar campos anteriores. Si `hay_mas=true`, solicitar `offset+limit`. No existe aislamiento de una fotografía entre páginas si asistencia cambia durante la navegación.

```http
GET /api/v1/personal?tipo=Practicante&estado=Activo&limit=20&offset=0
Authorization: Bearer <ACCESS_TOKEN_DE_PRUEBA>
```

Ejemplo sintético abreviado (los objetos conservan los campos adicionales de la vista):

```json
{
  "status": "success",
  "code": 200,
  "message": "Operacion exitosa",
  "data": {
    "total": 1,
    "empleados": [{"id_trabajador": 104, "nombre_completo": "Persona Sintetica", "tipo_trabajador": "Practicante", "estado_asistencia": "Activo", "situacion_websiteplus": "VIGENTE", "condicion_operativa": "OPERATIVO"}],
    "paginacion": {"limit": 20, "offset": 0, "hay_mas": false}
  }
}
```

La vista incluye `dni`, cargo/agencia (ID y nombre), cuenta (`id_usuario`, `username`, `rol_nombre`) y `permiso_hoy_tipo`. Puede haber valores null. Si asistencia tiene varias cuentas por trabajador, la vista puede devolver varias filas del mismo trabajador: conservar compatibilidad y agrupar por `id_trabajador` en una interfaz centrada en personas; no usar `total` como cantidad de personas únicas. No se devuelven contraseñas ni descriptores biométricos.

## Detalle y ausencias autorizadas

```http
GET /api/v1/personal/104
Authorization: Bearer <ACCESS_TOKEN_DE_PRUEBA>
```

`data` contiene `empleado`, `periodos_laborales`, `eventos_transicion` y `permisos_laborales`. Este último contiene los **20 registros más recientes** de asistencia, incluidos aprobados, pendientes y rechazados: ID, tipo, inicio, fin, motivo y estado. No es un historial ilimitado. No redistribuir motivos fuera del alcance autorizado.

Vacaciones, permisos y ausencias justificadas se muestran según los registros del origen. No se deducen ausencias injustificadas de que falte una marcación ni se crea un intervalo de vacaciones inexistente. La condición operativa prioriza DESVINCULADO, después INACTIVO, después permiso/vacación aprobada vigente y finalmente OPERATIVO. Un permiso aprobado no convierte una cuenta inactiva en activa y no revoca por sí solo una cuenta activa.

En eventos `origen=SINCRONIZACION`, mostrar «Observado el…»: `fecha_efectiva_confirmada=false`, `observado_en=creado_en`. El campo heredado `fecha_efectiva` es el día de observación, **no la fecha histórica efectiva acreditada**. En eventos API, `fecha_efectiva_confirmada=true`. Nunca utilizar fechas observadas para calcular contratos, pausas o nómina.

## Transiciones

```http
POST /api/v1/personal/104/transicion
Authorization: Bearer <ACCESS_TOKEN_DE_PRUEBA>
Content-Type: application/json

{"tipo_evento":"DESVINCULACION","motivo":"Cierre autorizado de la relacion laboral"}
```

`tipo_evento` y `motivo` deben ser cadenas. Motivo: 1–500 caracteres después de recortar espacios. `fecha_efectiva` es opcional: omitirla usa hoy en America/Lima; si se envía, debe ser cadena ISO `YYYY-MM-DD` del día actual. Null, arreglos, números y booleanos no sustituyen texto válido.

| Evento | Condiciones y efecto |
|---|---|
| DESVINCULACION | VIGENTE → DESVINCULADO; cerrar el periodo abierto si existe; revocar todas las sesiones de todas las cuentas del trabajador |
| REINGRESO | DESVINCULADO y asistencia Activo; abrir nuevo periodo con tipo/cargo/agencia actuales; no restaurar tokens antiguos |
| REACTIVACION | VIGENTE, asistencia Activo y última observación Inactivo; registrar el retorno sin inventar un periodo. Si la sincronización ya registró el retorno, responder 409, no duplicarlo |

Una inactividad observada no equivale a desvinculación. Un cambio de cargo/tipo/agencia se observa automáticamente, sin reescribir periodos históricos. No hay endpoint de empleados que cambie estado, usuario, contraseña, vacaciones, cargo o agencia en asistencia.

Éxito 200 conserva el sobre JSON anterior y devuelve:

```json
{"id_evento":501,"id_trabajador":104,"situacion":"DESVINCULADO","id_periodo":32}
```

Este objeto está dentro de `data`. `id_periodo` puede ser null. Ante timeout **no repetir ciegamente** una transición: volver a consultar detalle/historial y conciliar el resultado; el endpoint no acepta una clave de idempotencia. Una repetición confirmada devuelve 409 y no añade otra transición.

## Errores y efectos de lectura

| HTTP | `error_code` | Tratamiento |
|---|---|---|
| 401 | `UNAUTHORIZED` / `INVALID_TOKEN` | Token ausente/mal formado o sesión vencida, revocada o laboralmente restringida; usar el flujo existente de sesión |
| 403 | `FORBIDDEN` | Permiso/alcance insuficiente; no reintentar con otro ID |
| 404 | `NOT_FOUND` | Trabajador inexistente dentro de una consulta autorizada; para un ID fuera del alcance puede prevalecer 403 |
| 409 | `LABOR_STATE_CONFLICT` | Estado incompatible o transición ya aplicada |
| 409 | `LABOR_PERIOD_CONFLICT` | Periodo abierto o cronología inconsistente; requiere revisión |
| 422 | `VALIDATION_FAILED` | Filtros/cuerpo/fecha inválidos. Un parámetro con forma de arreglo puede usar el código genérico `ERROR_422` del Request existente |
| 500 | `INTERNAL_SERVER_ERROR` | Error saneado; no se expone SQL, rutas ni credenciales |

Ejemplo: `{"status":"error","code":409,"error_code":"LABOR_STATE_CONFLICT","message":"..."}`. No depender de comparar literalmente el mensaje traducible.

Las consultas autorizadas observan cambios y pueden escribir **únicamente** fotografía/eventos y revocaciones en WebSite+. Una lista solo observa los IDs devueltos; no todos los trabajadores ni la fila adicional usada para detectar otra página. Cada trabajador tiene su propia transacción: si una lista falla más adelante, las observaciones anteriores confirmadas permanecen. No se escriben datos originales de asistencia. Las lecturas repetidas sin cambios no duplican eventos.

Las observaciones no son vigilancia continua. Un ciclo Inactivo→Activo completamente ocurrido entre observaciones no puede reconstruirse sin historial de asistencia. La programación opcional del observador requiere aprobación; no se ha instalado ninguna tarea en cPanel.
