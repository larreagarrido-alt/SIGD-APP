# Bienestar organizacional — estado de integración

## Fuente revisada

`C:\Users\Jair L.G\Downloads\KPI-Web(dani).zip` contiene la pantalla de Dani. Su clase
`CollaboratorWellnessActivity` declara `RYFF_DEMO` con **tres** ítems. La propia clase indica que
una versión final de 18 ítems, su adaptación y puntuación requieren validación profesional.
Los cuestionarios semanales y mensuales de Dani siguen independientes y no se alteraron.

No se encontró en ese ZIP una versión Ryff completa, validada y autorizada para usarla como
instrumento oficial. No se importaron sus tres preguntas de ejemplo ni se generaron puntuaciones.

## Implementado localmente

- Pantalla Android «Bienestar organizacional», conectada al menú administrativo: resumen,
  participación mensual, dimensiones grupales, filtros de mes/campaña/agencia,
  comparación de participación por agencia, campañas, vista de cuestionario, plan de mejora
  y asistente con respuestas estructuradas. Muestra estado vacío cuando no hay datos suficientes.
- API PHP para estas funciones. `bienestar.ver_agregado` permite consultar resúmenes y usar
  orientación general. `bienestar.administrar` permite crear campañas y planes. No se concede
  ningún permiso por la migración V024.
- Campañas en borrador, programadas, activas, finalizadas o desactivadas según estado y fecha.
  Publicar exige un instrumento registrado y completo (mínimo 18 ítems, seis dimensiones y
  escala 1–5). Una campaña publicada sigue siendo voluntaria; solo la cuenta elegible puede
  abrirla y las respuestas exigen consentimiento explícito.
- Las respuestas permanecen en las tablas privadas de V006. V024 agrega referencias de
  participación y consentimiento, sin duplicarlas. Los promedios invierten ítems marcados
  `es_inversa` y se ocultan si hay menos de cinco participantes distintos.
- Revisión individual bloqueada por defecto: exige permiso separado, consentimiento específico
  de revisión privada, finalidad coincidente, autorización individual vigente y auditoría.
  El panel general no ofrece acceso a esta ruta.
- El asistente devuelve orientación general mediante reglas locales. No usa IA externa,
  no lee respuestas individuales y no da diagnósticos.

## Archivos para una futura publicación controlada

1. `database/migrations/V024__bienestar_campanas_ryff_y_plan.up.sql` en
   `wsnet_websiteplus`, **después** de verificar respaldo restaurable y esquema de destino.
2. `backend/src/Controllers/WellnessAdminController.php` y
   `backend/src/Controllers/WellnessController.php` en `websiteplus_backend/src/Controllers/`.
3. `backend/public/index.php` en `public_html/websiteplus-api/`.
4. APK Android firmado para distribución; el `app/build/outputs/apk/debug/app-debug.apk`
   generado ahora es solo de laboratorio y tiene firma de depuración.
5. `database/proposals/bienestar_admin_fase1_pendiente_aprobacion.sql` es una propuesta
   separada de permisos para el rol Administrador. **No se ejecutó**. Nunca concede
   `bienestar.ver_privado`.

No se modificó cPanel ni producción en esta integración. La aplicación instalada actualmente
no tendrá estas rutas hasta que se publique la API y se aplique V024 en una operación aprobada.

## Pendientes reales

- Documento del instrumento Ryff completo, versión/adaptación, ítems invertidos, escala,
  permiso de uso y validación profesional. Debe cargarse como test y registrarse en
  `wp_bienestar_ryff_instrumentos`. Sin eso la API rechaza publicar campañas.
- Política aprobada de retención, retiro de consentimiento y tratamiento de datos sensibles.
- Aprobación del permiso grupal/administrativo y prueba en staging con respaldo restaurable.
- Los gráficos longitudinales de dimensiones y comparaciones por área (más allá de agencia)
  requieren grupos suficientes y verificación adicional; el panel actual muestra participación
  de los últimos seis meses y las seis dimensiones del período.

## Pruebas

- `:app:assembleDebug --offline`: correcto.
- `:app:testDebugUnitTest --offline`: correcto.
- `backend/tests/run_employees.ps1` en MariaDB local aislada: 772 comprobaciones, sin datos
  reales ni acceso a producción. Incluye permisos 403, rechazo de demo, consentimiento,
  campaña futura, resultados ocultos para grupo pequeño y planes.
- Modo sin V012: 112 comprobaciones.
