-- DDL only for six integration fixtures. No production data, users or secrets.

CREATE TABLE `agencias` (
  `id_agencia` int(11) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `departamento` varchar(100) DEFAULT '',
  `provincia` varchar(100) DEFAULT '',
  `distrito` varchar(100) DEFAULT '',
  `direccion` varchar(255) DEFAULT NULL,
  `codigo` varchar(20) DEFAULT NULL,
  `latitud` decimal(10,8) DEFAULT NULL,
  `longitud` decimal(11,8) DEFAULT NULL,
  `radio_permitido` int(11) NOT NULL DEFAULT 100
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `cargos` (
  `id_cargo` int(11) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `sueldo_base` decimal(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `roles` (
  `id_rol` int(11) NOT NULL,
  `nombre` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `trabajadores` (
  `id_trabajador` int(11) NOT NULL,
  `dni` varchar(15) NOT NULL,
  `nombres` varchar(100) NOT NULL,
  `apellidos` varchar(100) NOT NULL,
  `fecha_nacimiento` date NOT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `id_cargo` int(11) NOT NULL,
  `sueldo_base` decimal(10,2) DEFAULT NULL,
  `id_agencia` int(11) NOT NULL,
  `tipo` enum('Colaborador','Practicante') NOT NULL DEFAULT 'Colaborador',
  `fecha_inicio` date NOT NULL,
  `fecha_fin_contrato` date DEFAULT NULL,
  `experiencia` text DEFAULT NULL,
  `estado` enum('Activo','Inactivo') NOT NULL DEFAULT 'Activo',
  `rostro_descriptor` text DEFAULT NULL,
  `foto_perfil` varchar(255) DEFAULT NULL,
  `codigo_trabajador` varchar(20) DEFAULT NULL,
  `firma` varchar(255) DEFAULT NULL,
  `cv_path` varchar(255) DEFAULT NULL,
  `contrato_path` varchar(255) DEFAULT NULL,
  `huella_descriptor` text DEFAULT NULL,
  `id_coordinador` int(11) DEFAULT NULL,
  `id_supervisor` int(11) DEFAULT NULL,
  `es_supervisor` tinyint(1) NOT NULL DEFAULT 0,
  `departamento` varchar(100) DEFAULT NULL,
  `provincia` varchar(100) DEFAULT NULL,
  `distrito` varchar(100) DEFAULT NULL,
  `estado_civil` varchar(30) DEFAULT 'Soltero',
  `nacionalidad` varchar(50) DEFAULT 'Peruana',
  `copia_dni_path` varchar(255) DEFAULT NULL,
  `certificados_path` varchar(255) DEFAULT NULL,
  `constancias_medicas_path` varchar(255) DEFAULT NULL,
  `formacion_academica` varchar(255) DEFAULT NULL,
  `habilidades` text DEFAULT NULL,
  `herramientas` text DEFAULT NULL,
  `contacto_emergencia_nombre` varchar(150) DEFAULT NULL,
  `contacto_emergencia_telefono` varchar(30) DEFAULT NULL,
  `contacto_emergencia_parentesco` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `usuarios` (
  `id_usuario` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `id_rol` int(11) NOT NULL,
  `id_trabajador` int(11) DEFAULT NULL,
  `reset_token` varchar(64) DEFAULT NULL,
  `reset_expires` datetime DEFAULT NULL,
  `firma_reporte` varchar(255) DEFAULT NULL,
  `cargo_reporte` varchar(255) DEFAULT NULL,
  `debe_cambiar_password` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `permisos_laborales` (
  `id_permiso_laboral` int(11) NOT NULL,
  `id_trabajador` int(11) NOT NULL,
  `tipo_permiso` varchar(100) NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date NOT NULL,
  `motivo` text DEFAULT NULL,
  `estado` varchar(50) DEFAULT 'Pendiente',
  `procesado_asistencia` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `horario_practicantes` (
  `id_trabajador` int NOT NULL, `dia` varchar(20) NOT NULL,
  `asiste` tinyint(1) DEFAULT 1, `asiste_manana` tinyint(1) DEFAULT 1,
  `asiste_tarde` tinyint(1) DEFAULT 0, `hora_inicio` time DEFAULT NULL,
  `hora_fin` time DEFAULT NULL, `hora_inicio_tarde` time DEFAULT NULL,
  `hora_fin_tarde` time DEFAULT NULL
);
CREATE TABLE `horario_vigencias` (
  `id_trabajador` int NOT NULL, `vigente_desde` date NOT NULL,
  `horario_json` longtext NOT NULL
);
CREATE TABLE `temporadas_horario` (
  `id_temporada` int NOT NULL, `fecha_inicio` date NOT NULL, `fecha_fin` date NOT NULL
);
CREATE TABLE `horarios_trabajadores` (
  `id_horario_trabajador` int NOT NULL, `id_trabajador` int NOT NULL,
  `id_temporada` int NOT NULL, `id_horario` int NOT NULL,
  `dias_asistencia` varchar(150) DEFAULT NULL
);
CREATE TABLE `tipo_vigencias` (
  `id_trabajador` int NOT NULL, `vigente_desde` date NOT NULL,
  `tipo` varchar(20) NOT NULL, `id_cargo` int DEFAULT NULL,
  `origen` varchar(20) NOT NULL, `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_trabajador`,`vigente_desde`)
);
CREATE TABLE `horario_detalles` (
  `id_horario_detalle` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `id_horario` int NOT NULL, `dia_semana` varchar(20) NOT NULL,
  `hora_entrada` time DEFAULT NULL, `hora_salida` time DEFAULT NULL,
  `inicio_refrigerio` time DEFAULT NULL, `fin_refrigerio` time DEFAULT NULL,
  `es_laborable` tinyint(1) DEFAULT 1
);
CREATE TABLE `consumos_extras` (
  `id_trabajador` int NOT NULL, `tipo_consumo` varchar(20) NOT NULL,
  `fecha_permiso` date DEFAULT NULL, `detalles` varchar(255) DEFAULT NULL
);
CREATE TABLE `feriados` (
  `fecha` date NOT NULL, `estado` tinyint(1) DEFAULT 1,
  `excluir_evaluacion` tinyint(1) DEFAULT 1, `tipo_jornada` varchar(20) NOT NULL
);
CREATE TABLE `faltas` (`id_trabajador` int NOT NULL, `fecha_falta` date NOT NULL);
CREATE TABLE `asistencias` (
  `id_asistencia` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `id_trabajador` int NOT NULL, `fecha` date NOT NULL,
  `hora_entrada` time NULL, `hora_salida` time NULL,
  `estado_asistencia` varchar(50) NOT NULL DEFAULT 'Puntual'
);
CREATE TABLE `justificaciones` (
  `id_trabajador` int NOT NULL, `fecha_referencia` date NOT NULL,
  `tipo_justificacion` varchar(20) NOT NULL, `estado_aprobacion` varchar(20) NOT NULL
);
