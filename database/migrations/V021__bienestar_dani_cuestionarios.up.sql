-- Questions and visible answer labels are copied from Dani's Android module.
-- No clinical interpretation, KPI score, account, role or permission is created here.
-- Review exact wording with WEBSITE before any production import.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO wp_bienestar_tests (codigo,titulo,descripcion,version,tipo,periodicidad,activo)
VALUES ('DANI_SEMANAL','Evaluación semanal','Percepción semanal de bienestar laboral; sin diagnóstico clínico',1,'GENERAL','SEMANAL',1);
SET @dani_weekly_id = LAST_INSERT_ID();
INSERT INTO wp_bienestar_preguntas (id_test,orden,enunciado,tipo_respuesta) VALUES
(@dani_weekly_id,1,'¿Cómo describirías tu estado de ánimo general durante esta semana?','OPCION_MULTIPLE'),
(@dani_weekly_id,2,'¿Qué nivel de motivación sentiste frente a tus actividades de la semana?','OPCION_MULTIPLE'),
(@dani_weekly_id,3,'¿Cómo percibiste el equilibrio y manejo de tu carga laboral esta semana?','OPCION_MULTIPLE'),
(@dani_weekly_id,4,'¿Cómo te sentiste respecto a tu energía y nivel de descanso frente al cansancio?','OPCION_MULTIPLE'),
(@dani_weekly_id,5,'¿Qué tan respaldado/a te sentiste por tu equipo y coordinación esta semana?','OPCION_MULTIPLE'),
(@dani_weekly_id,6,'¿Cuál es tu grado de satisfacción general al cerrar esta semana?','OPCION_MULTIPLE');
INSERT INTO wp_bienestar_opciones (id_pregunta,orden,texto)
SELECT q.id_pregunta, o.orden, o.texto FROM wp_bienestar_preguntas q
JOIN (SELECT 1 AS orden,'Muy bien' AS texto UNION ALL SELECT 2,'Bien' UNION ALL SELECT 3,'Neutral'
      UNION ALL SELECT 4,'Mal' UNION ALL SELECT 5,'Muy mal') o
WHERE q.id_test=@dani_weekly_id;

INSERT INTO wp_bienestar_tests (codigo,titulo,descripcion,version,tipo,periodicidad,activo)
VALUES ('DANI_MENSUAL','Evaluación mensual','Percepción mensual de bienestar laboral; sin diagnóstico clínico',1,'GENERAL','MENSUAL',1);
SET @dani_monthly_id = LAST_INSERT_ID();
INSERT INTO wp_bienestar_preguntas (id_test,orden,enunciado,tipo_respuesta) VALUES
(@dani_monthly_id,1,'¿Cómo te sentiste con tu experiencia de trabajo este mes?','OPCION_MULTIPLE'),
(@dani_monthly_id,2,'¿Cómo te sentiste con la cantidad de tareas que realizaste este mes?','OPCION_MULTIPLE'),
(@dani_monthly_id,3,'¿Cómo te sentiste con la claridad de tus responsabilidades este mes?','OPCION_MULTIPLE'),
(@dani_monthly_id,4,'¿Cómo te sentiste con la comunicación con tus compañeros este mes?','OPCION_MULTIPLE'),
(@dani_monthly_id,5,'¿Cómo te sentiste con el apoyo del equipo cuando lo necesitaste?','OPCION_MULTIPLE'),
(@dani_monthly_id,6,'¿Cómo te sentiste con el tiempo disponible para descansar fuera del trabajo?','OPCION_MULTIPLE'),
(@dani_monthly_id,7,'¿Cómo te sentiste con los recursos disponibles para tus tareas?','OPCION_MULTIPLE'),
(@dani_monthly_id,8,'¿Cómo te sentiste con las oportunidades de aprender este mes?','OPCION_MULTIPLE'),
(@dani_monthly_id,9,'¿Cómo te sentiste con el reconocimiento de tu esfuerzo este mes?','OPCION_MULTIPLE'),
(@dani_monthly_id,10,'En general, ¿cómo describirías tu experiencia laboral de este mes?','OPCION_MULTIPLE');
INSERT INTO wp_bienestar_opciones (id_pregunta,orden,texto)
SELECT q.id_pregunta, o.orden, o.texto FROM wp_bienestar_preguntas q
JOIN (SELECT 1 AS orden,'Muy mal' AS texto UNION ALL SELECT 2,'Mal' UNION ALL SELECT 3,'Regular'
      UNION ALL SELECT 4,'Bien' UNION ALL SELECT 5,'Muy bien') o
WHERE q.id_test=@dani_monthly_id;

INSERT INTO wp_migraciones (version,descripcion)
VALUES ('V021','Cuestionarios de bienestar semanal y mensual del modulo Android de Dani');
COMMIT;
