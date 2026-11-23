USE `websiteplus`;
-- =====================================================================
-- WebSite+  |  V009  |  Catálogo de permisos, roles predeterminados y categorías base
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Catálogo de permisos WebSite+
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `wp_permisos` (`codigo`, `modulo`, `descripcion`, `es_sensible`) VALUES
('auth.perfil',              'auth',       'Consultar y actualizar su propio perfil', 0),
('auth.dispositivos',        'auth',       'Gestionar sus dispositivos biometricos autorizados', 0),
('empleados.ver',            'empleados',  'Consultar lista de empleados y estados', 0),
('empleados.administrar',    'empleados',  'Registrar reingresos, pausas o desvinculaciones', 1),
('kpi.ver',                  'kpi',        'Consultar indicadores asignados', 0),
('kpi.entregar',             'kpi',        'Registrar borradores y entregas con firma biometrica', 0),
('kpi.revisar',              'kpi',        'Calificar y observar entregas de KPI', 0),
('kpi.administrar',          'kpi',        'Crear y versionar definiciones y asignaciones de KPI', 1),
('actividad.crear',          'actividad',  'Registrar actividades diarias laborales', 0),
('actividad.ver',            'actividad',  'Ver actividades laborales del equipo', 0),
('actividad.revisar',        'actividad',  'Revisar y observar actividades de supervisados', 0),
('ranking.ver',              'ranking',    'Consultar ranking de desempeno del periodo', 0),
('premios.ver',              'premios',    'Ver catalogo de premios disponibles y saldo de puntos', 0),
('premios.canjear',          'premios',    'Solicitar canje de premios con puntos acumulados', 0),
('premios.administrar',      'premios',    'Crear premios y aprobar o entregar canjes', 1),
('bienestar.responder',      'bienestar',  'Responder cuestionarios de clima y bienestar', 0),
('bienestar.ver_agregado',   'bienestar',  'Consultar metricas de bienestar agregadas y semaforo', 0),
('bienestar.ver_privado',    'bienestar',  'Ver respuestas individuales confidenciales (Salud/Psicologia)', 1),
('dashboard.admin',          'dashboard',  'Acceso al dashboard general de administracion', 0),
('reportes.exportar',        'reportes',   'Generar y descargar reportes PDF y CSV', 0),
('chatbot.usar',             'chatbot',    'Acceso al asistente virtual de la empresa', 0);

-- ---------------------------------------------------------------------
-- Asignación de permisos por rol estándar
-- Roles existentes en bd_asistencia:
--   1: Administrador, 2: Gerencia, 3: Jefe de Agencia, 4: Supervisor, 5: Colaborador, 6: Practicante
-- ---------------------------------------------------------------------

-- ROL 1: Administrador (acceso total con alcance TODOS)
-- Role grants must be configured after verifying actual roles in attendance.

-- ROL 2: Gerencia (acceso a dashboard, reportes, ranking, kpi con alcance TODOS)
-- Role grants must be configured after verifying actual roles in attendance.

-- ROL 3: Jefe de Agencia (alcance AGENCIA)
-- Role grants must be configured after verifying actual roles in attendance.

-- ROL 4: Supervisor (alcance EQUIPO)
-- Role grants must be configured after verifying actual roles in attendance.

-- ROL 5: Colaborador (alcance PROPIO)
-- Role grants must be configured after verifying actual roles in attendance.

-- ROL 6: Practicante (alcance PROPIO)
-- Role grants must be configured after verifying actual roles in attendance.

-- ---------------------------------------------------------------------
-- Categorías iniciales de KPI
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `wp_kpi_categorias` (`id_categoria`, `nombre`, `descripcion`, `icono`, `orden`) VALUES
(1, 'Productividad', 'Metas operativas, tareas y avance de proyectos', 'ic_productivity', 1),
(2, 'Calidad y Servicio', 'Precision de entregables, satisfaccion de clientes y diseno', 'ic_quality', 2),
(3, 'Innovacion y Aprendizaje', 'Cursos aprobados, propuestas de mejora e iniciativas', 'ic_innovation', 3),
(4, 'Compromiso y Asistencia', 'Puntualidad, asistencia a reuniones y participacion activa', 'ic_engagement', 4);

-- ---------------------------------------------------------------------
-- Configuración por defecto en wp_configuracion
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `wp_configuracion` (`clave`, `valor`, `tipo`, `descripcion`) VALUES
('jwt_access_ttl_minutos', '60', 'ENTERO', 'Duracion del token de acceso para la app Android'),
('jwt_refresh_ttl_dias', '30', 'ENTERO', 'Duracion del token de refresco'),
('desafio_ttl_segundos', '180', 'ENTERO', 'Ventana de tiempo para responder un desafio biometrico (3 minutos)'),
('max_intentos_login', '5', 'ENTERO', 'Intentos fallidos antes de bloquear temporalmente'),
('bloqueo_minutos', '15', 'ENTERO', 'Tiempo de bloqueo tras exceder intentos'),
('periodo_activo_kpi', 'SEMANAL', 'TEXTO', 'Periodicidad por defecto para evaluaciones activas'),
('ranking_visibilidad', 'EMPRESA', 'TEXTO', 'Alcance visible del ranking para colaboradores (EMPRESA o AGENCIA)');

INSERT IGNORE INTO `wp_migraciones` (`version`, `descripcion`) VALUES ('V009', 'Catalogos base: permisos, roles predeterminados, categorias de KPI y configuracion');
