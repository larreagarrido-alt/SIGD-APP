# Contrato de API revisado — 2026-10-15

Base configurada: `https://ws.net.pe/websiteplus-api/api/v1/`. El despliegue de las rutas nuevas sigue pendiente. HTTPS obligatorio en producción. JSON de entrada debe ser un objeto; arrays o JSON roto se rechazan. Respuestas: `status`, `code`, `message`, `data` en éxitos; `error_code` en errores. Códigos habituales: 400/422 validación, 401 sesión inválida, 403 autorización, 404 inexistente, 409 conflicto, 429 límite de login, 500 fallo interno sin detalles.

## Autenticación

- POST `/auth/login`: `username`, `password`; opcional cabecera `X-Device-Uid`. Credenciales de asistencia; no hay tabla de contraseñas propia. Devuelve `data.auth.access_token`, `refresh_token`, `expires_in`, `token_type`, usuario, trabajador y permisos efectivos.
- POST `/auth/refresh`: `refresh_token`; rota ambos tokens. Inactividad, desvinculación, cambio de contraseña o dispositivo revocado bloquean renovación. Se detecta reutilización del refresh inmediatamente anterior; no hay todavía historial de todas las generaciones.
- GET `/auth/me`: `Authorization: Bearer TOKEN`.
- POST `/auth/logout`: mismo Bearer, cuerpo `{}`; revoca sesión.
- POST `/auth/desafio`: ahora requiere Bearer y permiso `kpi.entregar`. Solo `proposito=ENTREGA_KPI`, `device_uid` activo, `referencia_tipo=kpi_evaluacion`, `referencia_id` propia y `hash_contenido` SHA-256 hexadecimal. `id_usuario` del cliente se ignora; se usa el de la sesión. No ofrece login biométrico ni enrolamiento automático.

Tokens opacos aleatorios de 256 bits, guardados solo como SHA-256. No son JWT. La cuenta inactiva se verifica en cada petición protegida. Reactivar permite nuevo login conservando ID e historial; no revive tokens revocados. Sincronización por consulta, no notificación instantánea de cambios mientras no hay solicitudes.

### Acceso desde un celular vinculado (V011)

Las seis rutas `/auth/biometria/{registro,confirmar,desafio,login,revocar,recuperar}` están descritas en [acceso_huella_celular.md](acceso_huella_celular.md). Solo una cuenta por instalación y un celular activo por cuenta. Cerrar sesión conserva el vínculo; otro teléfono recibe 409 hasta que el titular lo desvincule con una sesión firmada reciente o haya recuperación autorizada. El cliente firma con EC P-256 y el servidor crea una sesión nueva, en lugar de reutilizar un refresh antiguo. El registro se almacena en `wp_accesos_android` y no aprueba dispositivos de firma KPI. Key Attestation remoto sigue pendiente. Estas rutas nuevas necesitan actualizar PHP y aplicar V011 en el entorno de destino.

## Rutas de negocio

| Ruta | Autorización/comportamiento |
|---|---|
| GET `/personal` | empleados.ver, TODOS; listado limitado a 100 |
| GET `/personal/{id}` | empleados.ver y alcance permitido |
| POST `/personal/{id}/transicion` | empleados.administrar y empleados.ver; REINGRESO, REACTIVACION o DESVINCULACION; motivo y fecha actual; reingreso exige estado Activo en asistencia |
| GET `/kpi/mis-kpis?periodo=2026-W42` | kpi.ver propio |
| GET `/kpi/evaluaciones/{id}` | kpi.ver y propiedad/alcance |
| POST `/kpi/evaluaciones/{id}/borrador` | kpi.entregar propio; valor_reportado numérico no negativo, comentarios_entrega |
| POST `/kpi/evaluaciones/{id}/entregar` | kpi.entregar propio; valor, comentarios, id_desafio y firma_base64 cuando exige firma; X-Device-Uid activo |
| POST `/kpi/evaluaciones/{id}/calificar` | kpi.revisar y alcance; estado Aprobado/Observado, observaciones_supervisor; no permite autocalificar ni recalificar un aprobado |
| GET/POST `/actividades` | actividad.ver/crear; sin alcance TODOS se listan solo las propias; una referencia KPI debe ser propia |
| GET `/dashboard/resumen` | dashboard.admin, TODOS |
| GET `/premios`, `/premios/mis-puntos` | premios.ver |
| POST `/premios/canjear` | premios.canjear propio; id_premio y solicitud_id estable de 16–64 caracteres alfanuméricos/guiones; reintentar con el mismo ID no descuenta otra vez |

DENEGAR individual prevalece sobre el permiso del rol. No se interpreta `id_rol <= 4` como administrador. Sin asignación explícita de permisos, las rutas de negocio deniegan el acceso. EQUIPO usa id_supervisor, AGENCIA usa id_agencia; los listados globales restringidos todavía no filtran por esos ámbitos.

## Firma y exigibilidad

Para la entrega actual, el digest es SHA-256 del texto UTF-8 `EVAL:{id}:VAL:{valor}:COMS:{comentarios}`. Android debe conservar exactamente la representación del valor y comentarios entre el desafío y la entrega. Firma ECDSA/SHA-256 del `message` devuelto, DER codificado en base64. Se valida usuario, dispositivo, propósito, evaluación, digest, vigencia y consumo único. La firma y el cambio de estado se guardan en una transacción. Una firma válida prueba posesión de clave; todavía NO prueba por sí sola que hubo biometría del teléfono. Enrolamiento, attestation y aprobación de claves siguen pendientes. Nunca enviar huellas o imágenes faciales.

Permisos laborales aprobados que solapan periodos diarios/semanales/mensuales abiertos los marcan No exigible con historial. Política conservadora pendiente de acuerdo empresarial: un permiso parcial excluye provisionalmente el periodo completo; no hay prorrateo. Periodos personalizados y revocación posterior del permiso requieren revisión. No se reescriben aprobaciones históricas.

No hay endpoints terminados de bienestar, chatbot, notificaciones, subida de evidencias, definición/asignación KPI, enrolamiento de dispositivos para firmas KPI o exportación de reportes. Sus tablas no equivalen a módulos implementados. La calificación actual es numérica creciente; métricas de texto, inversas y rúbricas requieren completar reglas antes de usarlas.
