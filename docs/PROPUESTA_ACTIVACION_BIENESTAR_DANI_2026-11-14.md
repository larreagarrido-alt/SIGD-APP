# Propuesta de activación de Bienestar de Dani

**Estado:** contenido del ZIP para revisión empresarial. No es una orden de importación ni una aprobación clínica.

## Lo que ya existe

La app consulta `GET /bienestar/tests` con la sesión del trabajador. `POST /bienestar/respuestas` guarda respuestas privadas para ese `id_trabajador`, y `GET /bienestar/mis-respuestas` devuelve su historial. V020, importada por el usuario según la captura de phpMyAdmin, solo permite `SEMANAL` en la columna. Para esa periodicidad también debe publicarse el `WellnessController.php` local que calcula el período ISO. La migración por sí sola no puede quitar el 403.

## Cuestionarios encontrados en el ZIP

| Flujo original | Preguntas | Periodicidad técnica | Estado |
| --- | ---: | --- | --- |
| Evaluación semanal | 6 | `SEMANAL` con V020 y PHP actualizado | Requiere aprobación de contenido y privacidad |
| Evaluación mensual | 10 | `MENSUAL` ya admitida | Requiere aprobación de contenido y privacidad |
| Ryff | 3 ejemplos visuales | Ninguna | No publicar como cuestionario oficial |

Preguntas semanales, en el orden del ZIP:

1. ¿Cómo describirías tu estado de ánimo general durante esta semana?
2. ¿Qué nivel de motivación sentiste frente a tus actividades de la semana?
3. ¿Cómo percibiste el equilibrio y manejo de tu carga laboral esta semana?
4. ¿Cómo te sentiste respecto a tu energía y nivel de descanso frente al cansancio?
5. ¿Qué tan respaldado/a te sentiste por tu equipo y coordinación esta semana?
6. ¿Cuál es tu grado de satisfacción general al cerrar esta semana?

Opciones semanales del ZIP: **Muy bien (5), Bien (4), Neutral (3), Mal (2), Muy mal (1)**.

Preguntas mensuales, en el orden del ZIP:

1. ¿Cómo te sentiste con tu experiencia de trabajo este mes?
2. ¿Cómo te sentiste con la cantidad de tareas que realizaste este mes?
3. ¿Cómo te sentiste con la claridad de tus responsabilidades este mes?
4. ¿Cómo te sentiste con la comunicación con tus compañeros este mes?
5. ¿Cómo te sentiste con el apoyo del equipo cuando lo necesitaste?
6. ¿Cómo te sentiste con el tiempo disponible para descansar fuera del trabajo?
7. ¿Cómo te sentiste con los recursos disponibles para tus tareas?
8. ¿Cómo te sentiste con las oportunidades de aprender este mes?
9. ¿Cómo te sentiste con el reconocimiento de tu esfuerzo este mes?
10. En general, ¿cómo describirías tu experiencia laboral de este mes?

Opciones mensuales del ZIP: **Muy mal (5), Mal (4), Regular (3), Bien (2), Muy bien (1)**. La dirección numérica es inversa a la semanal. No se debe usar ese valor para un resultado clínico ni un puntaje KPI sin una regla aprobada.

## Decisiones pendientes antes de insertar datos reales

1. La empresa confirma qué preguntas, textos y opciones se publican, sus nombres y códigos definitivos, periodicidad y vigencia.
2. Define qué roles existentes reciben `bienestar.responder` con alcance `PROPIO`. No se concede `bienestar.ver_privado` a administración general.
3. Revisa quién puede ver resultados individuales, conservación y privacidad. El backend actual solo guarda respuestas y muestra al titular su historial; no reproduce la evaluación local ni las recomendaciones de Dani desde MySQL.
4. Se prueba en staging una cuenta autorizada y otra sin permiso: catálogo, respuesta, acuse, lectura tras reinstalación, duplicado y separación entre trabajadores.

Con la autorización del equipo comunicada después, se preparó localmente V021 con estas preguntas y un SQL separado para `bienestar.responder` `PROPIO` en roles 1, 5 y 6. Se probaron sobre MariaDB aislado. Aún no se han importado V021 ni los permisos a producción; el procedimiento está en `GUIA_ACTIVACION_BIENESTAR_2026-11-14.md`.

## Comprobación actual de solo lectura

```sql
SELECT
  (SELECT COUNT(*) FROM wsnet_websiteplus.wp_migraciones WHERE version = 'V020') AS v020,
  (SELECT COUNT(*) FROM wsnet_websiteplus.wp_bienestar_tests WHERE activo = 1) AS cuestionarios_activos,
  (SELECT COUNT(*) FROM wsnet_websiteplus.wp_bienestar_preguntas) AS preguntas,
  (SELECT COUNT(*) FROM wsnet_websiteplus.wp_bienestar_opciones) AS opciones,
  (SELECT COUNT(*)
   FROM wsnet_websiteplus.wp_rol_permisos rp
   JOIN wsnet_websiteplus.wp_permisos p ON p.id_permiso = rp.id_permiso
   WHERE p.codigo = 'bienestar.responder') AS concesiones_responder;
```
