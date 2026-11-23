# Bienestar de `KPI-Web(dani).zip` — 14 de noviembre de 2026

## Comparación de archivos

Se compararon por SHA-256 los 731 archivos de `app/src` del ZIP con esta carpeta: 448 son idénticos, 283 difieren por cambios de integración y ninguno falta. Los 11 archivos de Bienestar de `app/src/main` están presentes. Diez son idénticos; el único modificado es `CollaboratorWellnessActivity.java`, porque la sesión real abre `OfficialWellnessScreen` para consultar la API. No se extrajeron ni copiaron los objetos `.git` incluidos en el ZIP.

## Funcionalidad que aporta el ZIP

`collaborator_activity_wellness.xml` define la pantalla oscura multifase de Dani. La actividad contiene una evaluación semanal local de seis preguntas, una mensual local de diez y un flujo Ryff expresamente rotulado como ejemplo visual. Incluye progreso, revisión, resultado, evolución y recomendaciones locales. Los archivos de catálogo, reglas, análisis, personalización, repositorio y sus pruebas siguen en el proyecto.

El propio código original declara que estos resultados locales no se envían al servidor ni afectan KPI, premios o ranking. Por ello, mostrar esa pantalla como cuestionario oficial en una sesión real sería engañoso. La integración Android usa `GET /bienestar/tests`, `GET /bienestar/mis-respuestas` y el envío autenticado de respuestas; el backend exige `bienestar.responder` y asocia cada respuesta al `id_trabajador` de la sesión. La pantalla de sesión real ahora conserva el encabezado y la navegación de Dani, usando el cuerpo para estados y formularios recibidos de la API.

## Bloqueo comprobado en producción

La consulta de solo lectura aportada por el usuario devolvió 0 cuestionarios activos, 0 preguntas y 0 opciones. La captura del teléfono muestra HTTP 403 traducido a «Tu cuenta no tiene permiso para responder cuestionarios». Los permisos previamente aprobados para roles 1, 5 y 6 no incluyeron `bienestar.responder`. El ZIP no aporta un catálogo SQL empresarial aprobado ni una concesión de permisos para ese módulo.

Una segunda captura del teléfono confirma que el encabezado oscuro y la navegación de Dani ya aparecen en la sesión real; el servidor sigue denegando el cuestionario. Esto es un bloqueo de autorización y datos, no un fallo de carga visual. No debe sustituirse la respuesta 403 por las respuestas locales de demostración.

La evaluación mensual del ZIP trae diez preguntas y cinco opciones de percepción. La semanal trae seis preguntas, pero el esquema `V006` y `WellnessController` no admiten periodicidad `SEMANAL`; solo `MENSUAL`, `TRIMESTRAL`, `SEMESTRAL` y `LIBRE`. El flujo Ryff está identificado en el código como ejemplo visual. Para publicar la evaluación semanal se requiere una extensión de esquema y backend, además de la aprobación del cuestionario; etiquetarla como `LIBRE` alteraría la regla de repetición y no sería equivalente.

Se preparó `V020__bienestar_semanal.up.sql`, que amplía solo el `ENUM` de periodicidad, y el controlador local calcula la clave con año y semana ISO. No crea cuestionarios ni concede permisos. En dos laboratorios aislados con datos sintéticos pasaron 109 comprobaciones con V012 ausente y 665 con V012 presente. La suite completa incluye guardado semanal, prevención de duplicados y propiedad del historial. También pasaron las 171 pruebas unitarias Android y `assembleDebug`; el APK se instaló sin borrar datos. La captura posterior del usuario muestra que V020 fue importada en phpMyAdmin: `ALTER TABLE` se ejecutó y se insertó la versión V020. Todavía falta comprobar por SELECT el estado final del esquema. El PHP actualizado no se ha confirmado publicado.

La reversión de V020 requiere primero una consulta de solo lectura para saber si existen filas `SEMANAL`. Si existen, el `ENUM` no debe contraerse ni deben eliminarse esos cuestionarios/respuestas: se conserva el esquema y se revierte únicamente el PHP o la visibilidad del módulo según el incidente. Si no existen, una contracción del `ENUM` puede prepararse y probarse en staging antes de autorizarla. No hay reversión automática que borre información confidencial.

Antes de aplicar V020 a una copia reciente del esquema real, comprobar exclusivamente con lectura:

```sql
SELECT COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'wsnet_websiteplus'
  AND TABLE_NAME = 'wp_bienestar_tests'
  AND COLUMN_NAME = 'periodicidad';

SELECT periodicidad, COUNT(*) AS cuestionarios
FROM wsnet_websiteplus.wp_bienestar_tests
GROUP BY periodicidad;
```

La publicación necesita decisiones separadas: cuáles de las seis preguntas semanales y diez mensuales del ZIP son cuestionarios empresariales aprobados, sus opciones y periodicidades, y qué roles reciben `bienestar.responder` con alcance propio. El flujo Ryff de ejemplo no debe publicarse como prueba clínica. No se ha aplicado un `INSERT` de contenido de cuestionarios en producción.

Para habilitar Bienestar real se necesita aprobar el cuestionario, las opciones y su periodicidad, revisar privacidad de resultados y autorizar el permiso por rol con alcance propio. Esas decisiones deben comprobarse primero en staging. No se copiaron respuestas locales a MySQL ni se publicaron preguntas o permisos de demostración.

## Firmas de Programación

`kpi_fragment_form.xml` incluía dos nombres dibujados de ejemplo en la sección de firmas. Se retiraron de la interfaz. En una sesión real, el nombre obtenido de la sesión identifica al responsable pero ya no se almacena automáticamente como una firma; el campo de firma queda vacío hasta una acción de firma válida. La firma del supervisor también queda pendiente. Los dos reportes ya recibidos en producción no se alteraron.
