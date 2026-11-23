# Endpoints para el equipo WebSite+

## Estado vigente: acceso facial V014 con PAD local

PHP: **213 comprobaciones locales aprobadas**; Android: **110 pruebas unitarias**. El recorrido adicional Motorola → cámara/PAD → firma → sesión PHP → perfil de cuenta sintética pasó al segundo intento; el primero fue falso rechazo. No está desplegado en cPanel. [Actualización V014](ACCESO_FACIAL_V014.md), [evidencia y límites](VALIDACION_ACCESO_FACIAL_V014.md).

POST `/auth/face/device/challenge` y `/auth/face/device/verify` mantienen sus rutas V013. Verify con `face_payload` V2 firmado puede devolver `session_issued=true`, `security_profile=LOCAL_PAD`, `auth` y `profile`; `liveness_verified=false` indica que no existe PAD de servidor. Un vector V013 simple sigue devolviendo diagnóstico sin sesión. No sustituir estas comprobaciones por un booleano cliente. V014 añade el método de sesión FACIAL, separado de BIOMETRIA fuerte; no necesita Python ni un nuevo endpoint público.

Todas las rutas siguientes usan POST y el prefijo `/api/v1`. Los cuerpos JSON usan `device_uid` de la instalación vinculada, nunca un usuario para seleccionar otra identidad:

| Ruta | Autorización / campos principales | Estado |
|---|---|---|
| `/auth/face/enrollment/status` | Bearer; `id_acceso`, `device_uid` | Consulta fuente ASISTENCIA primero; funciona sin V012 si hay referencia válida. No devuelve el descriptor. |
| `/auth/face/enrollment/challenge` | Bearer PASSWORD reciente y V011; identidad + `perfil_sha256` | Caso B: requiere V012; rechaza duplicar referencia válida de asistencia. |
| `/auth/face/enrollment/confirm` | Misma sesión; identidad + `challenge_id`, `firma_base64`, `perfil_json` | Guarda cifrado, PENDIENTE_VALIDACION, sin sesión. |
| `/auth/face/device/register` | Bearer PASSWORD reciente; `id_acceso`, `device_uid`, `clave_publica` EC P256 | V013: inicia autorización de clave facial separada. Devuelve `id_clave` y desafío, o `already_active`. |
| `/auth/face/device/confirm` | Mismo Bearer; `id_clave`, `device_uid`, `challenge_id`, `firma_base64` de V011 | Activa clave; una rotación revoca claves faciales anteriores sin cambiar V011. |
| `/auth/face/device/revoke-challenge` | Bearer PASSWORD reciente; `id_clave`, `device_uid` | Desafío de revocación facial. |
| `/auth/face/device/revoke` | Mismo Bearer; identidad facial + `challenge_id`, `firma_base64` de V011 | Revoca solo clave facial. API disponible; falta botón nativo específico. |
| `/auth/face/device/challenge` | Sin Bearer; `id_clave`, `device_uid` | Exige clave/cuenta/V011 activos y referencia autorizada; nonce LOGIN ligado a la clave. |
| `/auth/face/device/verify` | Sin Bearer; identidad facial + `challenge_id`, `nonce`, `firma_base64`, `face_payload` | Firma con clave facial sobre mensaje + salto de línea + `FACE_PAYLOAD_SHA256:` + SHA256 del JSON exacto. Compara solo esa cuenta. |

`device/verify` ante coincidencia devuelve `identity_match=true`, `device_signature_valid=true`, `liveness_verified=false`, `session_issued=false`, `status=PAD_REQUIRED`, `auth=null`. **No es un endpoint de login habilitado**. Falta PAD confiable y evaluado antes de emitir tokens. Las rutas antiguas `/auth/face/challenge` y `/auth/face/verify` permanecen por compatibilidad diagnóstica V011; tampoco habilitan ingreso facial. No usarlas para simular un login.

Los registros V012 complementarios permanecen pendientes; no hay endpoint que los convierta automáticamente en una referencia aprobada. Contraseña, refresh/logout y biometría V011 mantienen sus contratos indicados más abajo. Los recuentos históricos siguientes corresponden a V011, antes de las ampliaciones faciales.

Última comprobación: 16/10/2026. Hay **26 combinaciones método/ruta implementadas** en el código local y 86 comprobaciones PHP aprobadas. Las seis rutas nuevas de vinculación Android requieren V011 y desplegar el backend actualizado; no se ha ejecutado esa actualización en cPanel. Las pruebas usan trabajadores y claves sintéticos.

## Empezar

Jair debe proporcionar la URL del entorno de staging. La URL configurada en Android es `https://ws.net.pe/websiteplus-api/api/v1/`; esto no confirma que estén desplegadas las rutas nuevas.

La app tiene configurada `https://ws.net.pe/websiteplus-api/api/v1/`. Para pruebas usar staging con usuarios sintéticos. El despliegue de las rutas nuevas en ese dominio todavía debe realizarse y comprobarse; no se presenta el código local como ya instalado.

Para cada solicitud protegida:

```http
Content-Type: application/json
Authorization: Bearer ACCESS_TOKEN_RECIBIDO
```

Enviar un objeto JSON. Si no hay campos, usar `{}`, no `[]`. Los IDs de trabajador se toman de la sesión en operaciones propias: no enviar otro id para intentar cambiar de usuario. Las listas se encuentran bajo `data`.

## Disponibles: autenticación

| Método y ruta | Qué hace | Cuerpo / notas |
|---|---|---|
| GET `/health` | Comprueba PHP/router | Sin login; no comprueba la conexión de bases |
| POST `/auth/login` | Inicia sesión con la cuenta de asistencia | username, password |
| POST `/auth/refresh` | Renueva sesión y rota ambos tokens | refresh_token; guardar los nuevos y reemplazar los anteriores |
| GET `/auth/me` | Devuelve perfil y permisos actuales | Bearer |
| POST `/auth/logout` | Revoca sesión en servidor | Bearer y `{}`; luego borrar sesión local |
| POST `/auth/desafio` | Prepara firma de una entrega propia | Bearer, dispositivo autorizado y campos descritos abajo |
| POST `/auth/biometria/registro` | Solicita vincular cuenta y celular | Bearer PASSWORD reciente; device_uid, clave_publica, nombre |
| POST `/auth/biometria/confirmar` | Comprueba la clave y activa el vínculo | El mismo Bearer; id_acceso, device_uid, challenge_id, firma_base64 |
| POST `/auth/biometria/desafio` | Prepara un login desde el celular vinculado | id_acceso, device_uid |
| POST `/auth/biometria/login` | Crea sesión con prueba de clave | id_acceso, device_uid, challenge_id, firma_base64 |
| POST `/auth/biometria/revocar` | Desvincula el equipo actual | Bearer BIOMETRIA reciente de ese vínculo; id_acceso |
| POST `/auth/biometria/recuperar` | Recuperación de un celular perdido | Bearer PASSWORD reciente, permiso especial TODOS; id_acceso, motivo |

El registro rechaza otra vinculación activa con 409, incluso con contraseña correcta. No guarda imágenes de dedos. Contrato, límites y actualización: [cuenta vinculada al celular](acceso_huella_celular.md).

Ejemplo de login:

```json
{"username":"USUARIO_DE_PRUEBA","password":"CONTRASENA_DE_PRUEBA"}
```

Leer `data.auth.access_token`, `data.auth.refresh_token`, `data.user`, `data.worker` y `data.permissions`. El login no incluye permisos por defecto: Jair debe asignarlos según los roles verificados. El permiso de una pantalla se decide por su código, no por comparar números de rol.

`/auth/me` devuelve user/worker/permissions; no genera un nuevo bloque auth. Si devuelve 401, cerrar el acceso local o renovar según corresponda. No se debe entrar al dashboard únicamente porque exista un token guardado.

## Disponibles: compañero KPI

| Método y ruta | Función | Datos principales |
|---|---|---|
| GET `/kpi/mis-kpis?periodo=2026-W42` | Indicadores del trabajador | Permiso kpi.ver |
| GET `/kpi/evaluaciones/{id}` | Detalle, criterios y evidencias | Propiedad o alcance de kpi.ver |
| POST `/kpi/evaluaciones/{id}/borrador` | Guarda avance | valor_reportado, comentarios_entrega |
| POST `/kpi/evaluaciones/{id}/entregar` | Confirma entrega | valor_reportado, comentarios_entrega, id_desafio, firma_base64; cabecera X-Device-Uid |
| GET `/actividades` | Consulta actividades | fecha o desde/hasta, estado; por defecto solo propias sin permiso global |
| POST `/actividades` | Registra una actividad propia | titulo, descripcion; opcionales fecha_actividad, hora_inicio, hora_fin, horas_dedicadas, id_evaluacion_kpi propio |

Borrador numérico:

```json
{"valor_reportado":10,"comentarios_entrega":"Avance de prueba"}
```

Las entregas firmadas **están implementadas en PHP y probadas con claves sintéticas**, pero el compañero no puede completar todavía el flujo real del teléfono: falta enrolamiento/aprobación y validación de la política biométrica de las claves Android. Un booleano `biometria=true` no sirve. No crear un bypass para desarrollar la pantalla.

Para preparar `/auth/desafio` enviar proposito=ENTREGA_KPI, device_uid autorizado, referencia_tipo=kpi_evaluacion, referencia_id propia y hash_contenido SHA-256. El digest y la codificación exacta de firma están en `contratos_api.md`. Nunca enviar huellas o rostros.

## Disponibles: compañero administración

| Método y ruta | Función | Permiso / cuerpo |
|---|---|---|
| GET `/dashboard/resumen?periodo=2026-W42` | Resumen de personal, KPI, actividades y ranking | dashboard.admin con TODOS |
| GET `/personal` | Lista personal | empleados.ver con TODOS; filtros tipo, estado, id_cargo, q |
| GET `/personal/{id}` | Detalle e historial | empleados.ver y alcance |
| POST `/personal/{id}/transicion` | Desvincula o registra reingreso/reactivación | empleados.administrar y empleados.ver; tipo_evento, motivo, fecha_efectiva actual |
| POST `/kpi/evaluaciones/{id}/calificar` | Aprueba u observa entrega | kpi.revisar; estado Aprobado/Observado y observaciones_supervisor |
| GET `/premios` | Catálogo disponible | premios.ver |
| GET `/premios/mis-puntos` | Puntos y canjes del trabajador conectado | premios.ver; no consulta puntos ajenos |
| POST `/premios/canjear` | Solicita un premio para el trabajador conectado | premios.canjear; id_premio, solicitud_id |

El canje es una operación del trabajador aunque se liste aquí por pertenecer al módulo de premios. `solicitud_id` debe mantenerse idéntico al reintentar la misma acción; usar un UUID. Una acción nueva lleva otro UUID. El servidor calcula puntos y verifica saldo/stock.

Dashboard devuelve `data.periodo_consultado`, `personal`, `kpi`, `actividades`, `top_ranking`. KPI usa el periodo solicitado; actividades contiene hoy/semana actual y ranking usa el mes actual. No existe un filtro universal HOY/SEMANA que cambie todas las métricas: la pantalla debe reflejar esa diferencia.

Reingreso/reactivación exige que asistencia ya tenga estado Activo. No hay endpoint que cambie estado, contraseña, cargo o contrato en la base empresarial.

## Pendientes: no consumir como si ya existieran

| Funcionalidad | Estado actual |
|---|---|
| Crear/versionar definiciones y asignar/generar KPI | Tablas existentes, endpoints de administración pendientes |
| Subir/descargar evidencias y reportes PDF/CSV | Metadatos/tablas existentes, endpoints pendientes |
| Dispositivos para firmas KPI | Registro/aprobación de `wp_dispositivos` pendiente; el registro de login Android es independiente y no lo autoriza |
| Recuperación administrativa de celulares | API implementada con permiso especial; pantalla del módulo administración pendiente |
| Bienestar/test psicológico y agregados privados | Tablas existentes; endpoints y privacidad integral pendientes |
| Chatbot IA | Tablas/configuración existentes; endpoint/proveedor no integrado |
| Notificaciones y push | Tabla existente; endpoints y envío pendientes |
| CRUD y aprobación/entrega administrativa de premios | Catálogo/canje disponibles; gestión administrativa pendiente |
| Endpoint independiente de ranking/histórico | Ranking parcial en dashboard; sin endpoint separado |
| Listados de personal por agencia/equipo | Control de detalle existente; lista global exige TODOS |
| Métricas KPI inversas, texto y rúbricas completas | Calificación implementada solo para métrica numérica creciente |

Los períodos con ausencia aprobada se excluyen de incumplimiento según la política provisional documentada: solapamiento parcial excluye todo el periodo; personalizados requieren revisión. No presentar esa política como prorrateo terminado.

## Errores que deben manejar las pantallas

- 400/422: datos inválidos o acción no permitida en el estado actual.
- 401: token inválido/revocado/vencido; también firma inválida en entrega.
- 403: falta permiso, cuenta suspendida o dispositivo no autorizado.
- 404: recurso inexistente o fuera de las operaciones propias.
- 409: conflicto de estado al calificar.
- 429: demasiados intentos de login.
- 500: error del servidor; mostrar mensaje breve y reportar, sin registrar contraseñas/tokens.

## Qué falta en Android

ApiService.java declara login, perfil, refresh, listado KPI, dashboard y rutas V011 de registro/desafío/firma de huella. FaceDeviceApi declara el acceso facial independiente V014. Los demás métodos REST deben declararse todavía. Para evitar conflictos, agregar KpiApiService y AdminApiService en los módulos respectivos utilizando el mismo cliente/configuración, y coordinar cualquier cambio a core, manifest, Gradle o MainActivity.

Revisar la integración de las pantallas de cada compañero con los endpoints antes de declarar su módulo terminado. El recorrido facial local con sesión/perfil se probó, pero no sustituye la validación manual de contraseña, huella y rostro después de actualizar cPanel. No cambiar la URL compartida ni subir config.php privado para resolver conflictos.
