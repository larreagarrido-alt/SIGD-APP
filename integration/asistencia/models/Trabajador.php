<?php
require_once 'config/conexion.php';

class Trabajador {
    private $conn;
    private $table_name = "trabajadores";

    public function __construct($connection = null) {
        $database = $connection === null ? new Conexion() : null;
        $this->conn = $connection ?? $database->getConexion();
        try {
            $col = $this->conn->query("SHOW COLUMNS FROM trabajadores LIKE 'estado'")->fetch(PDO::FETCH_ASSOC);
            if ($col && stripos((string)($col['Type'] ?? ''), 'Reserva') === false) {
                $this->conn->exec("ALTER TABLE trabajadores MODIFY COLUMN estado ENUM('Activo','Inactivo','Reserva') NOT NULL DEFAULT 'Activo'");
            }
            $this->conn->exec("UPDATE trabajadores SET estado = 'Inactivo' WHERE estado = '' OR estado IS NULL");
        } catch (\Throwable $e) {}
    }

    public function connection() { return $this->conn; }

    public function leerTodos() {
        $condicionAdmin = "WHERE t.id_trabajador != 1 AND COALESCE(t.codigo_trabajador, '') != 'WS-0001' AND (c.nombre IS NULL OR c.nombre != 'Gerente General')";
        
        $query = "SELECT t.*, c.nombre as cargo_nombre, a.nombre as agencia_nombre, co.nombre_completo as coordinador_nombre, CONCAT(s.nombres, ' ', s.apellidos) as supervisor_nombre,
                  asis.hora_entrada as asis_hora_entrada, asis.hora_salida as asis_hora_salida, asis.minutos_acumulados as minutos_acumulados
                  FROM " . $this->table_name . " t
                  LEFT JOIN cargos c ON t.id_cargo = c.id_cargo
                  LEFT JOIN agencias a ON t.id_agencia = a.id_agencia
                  LEFT JOIN coordinadores_academicos co ON t.id_coordinador = co.id_coordinador
                  LEFT JOIN trabajadores s ON t.id_supervisor = s.id_trabajador
                  LEFT JOIN (
                      SELECT a1.id_trabajador, a1.hora_entrada, a1.hora_salida,
                             SUM(TIMESTAMPDIFF(MINUTE, CONCAT(a2.fecha, ' ', a2.hora_entrada), CONCAT(a2.fecha, ' ', a2.hora_salida))) as minutos_acumulados
                      FROM asistencias a1
                      LEFT JOIN asistencias a2 ON a1.id_trabajador = a2.id_trabajador 
                                               AND a2.fecha = CURDATE() 
                                               AND a2.hora_salida IS NOT NULL
                                               AND a2.id_asistencia != a1.id_asistencia
                      WHERE a1.fecha = CURDATE()
                        AND a1.id_asistencia = (SELECT MAX(id_asistencia) FROM asistencias WHERE id_trabajador = a1.id_trabajador AND fecha = CURDATE())
                      GROUP BY a1.id_trabajador, a1.hora_entrada, a1.hora_salida
                  ) asis ON t.id_trabajador = asis.id_trabajador
                  $condicionAdmin
                  ORDER BY CASE WHEN t.id_trabajador = 1 THEN 0 ELSE 1 END ASC, CAST(SUBSTRING(t.codigo_trabajador, 4) AS UNSIGNED) ASC";
        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        return $stmt;
    }

    public function leerUno($id) {
        $query = "SELECT t.*, c.nombre as cargo_nombre, a.nombre as agencia_nombre, co.nombre_completo as coordinador_nombre, CONCAT(s.nombres, ' ', s.apellidos) as supervisor_nombre,
                  asis.hora_entrada as asis_hora_entrada, asis.hora_salida as asis_hora_salida, asis.minutos_acumulados as minutos_acumulados
                  FROM " . $this->table_name . " t
                  LEFT JOIN cargos c ON t.id_cargo = c.id_cargo
                  LEFT JOIN agencias a ON t.id_agencia = a.id_agencia
                  LEFT JOIN coordinadores_academicos co ON t.id_coordinador = co.id_coordinador
                  LEFT JOIN trabajadores s ON t.id_supervisor = s.id_trabajador
                  LEFT JOIN (
                      SELECT a1.id_trabajador, a1.hora_entrada, a1.hora_salida,
                             SUM(TIMESTAMPDIFF(MINUTE, CONCAT(a2.fecha, ' ', a2.hora_entrada), CONCAT(a2.fecha, ' ', a2.hora_salida))) as minutos_acumulados
                      FROM asistencias a1
                      LEFT JOIN asistencias a2 ON a1.id_trabajador = a2.id_trabajador 
                                               AND a2.fecha = CURDATE() 
                                               AND a2.hora_salida IS NOT NULL
                                               AND a2.id_asistencia != a1.id_asistencia
                      WHERE a1.fecha = CURDATE()
                        AND a1.id_asistencia = (SELECT MAX(id_asistencia) FROM asistencias WHERE id_trabajador = a1.id_trabajador AND fecha = CURDATE())
                      GROUP BY a1.id_trabajador, a1.hora_entrada, a1.hora_salida
                  ) asis ON t.id_trabajador = asis.id_trabajador
                  WHERE t.id_trabajador = ? LIMIT 0,1";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $id);
        $stmt->execute();
        return $stmt->fetch(PDO::FETCH_ASSOC);
    }
    
    public function getTrabajadoresRegulares() {
        $query = "SELECT t.*, c.nombre as cargo_nombre, a.nombre as agencia_nombre, CONCAT(s.nombres, ' ', s.apellidos) as supervisor_nombre 
                  FROM " . $this->table_name . " t
                  LEFT JOIN cargos c ON t.id_cargo = c.id_cargo
                  LEFT JOIN agencias a ON t.id_agencia = a.id_agencia
                  LEFT JOIN trabajadores s ON t.id_supervisor = s.id_trabajador
                  WHERE t.tipo != 'Practicante' AND t.id_trabajador != 1 AND COALESCE(t.codigo_trabajador, '') != 'WS-0001' AND (c.nombre IS NULL OR c.nombre != 'Gerente General')
                  ORDER BY CASE WHEN t.id_trabajador = 1 THEN 0 ELSE 1 END ASC, CAST(SUBSTRING(t.codigo_trabajador, 4) AS UNSIGNED) ASC";
        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function getPracticantes() {
        $query = "SELECT t.*, c.nombre as cargo_nombre, a.nombre as agencia_nombre, co.nombre_completo as coordinador_nombre 
                  FROM " . $this->table_name . " t
                  LEFT JOIN cargos c ON t.id_cargo = c.id_cargo
                  LEFT JOIN agencias a ON t.id_agencia = a.id_agencia
                  LEFT JOIN coordinadores_academicos co ON t.id_coordinador = co.id_coordinador
                  WHERE t.tipo = 'Practicante' AND t.id_trabajador != 1 AND COALESCE(t.codigo_trabajador, '') != 'WS-0001'
                  ORDER BY CASE WHEN t.id_trabajador = 1 THEN 0 ELSE 1 END ASC, CAST(SUBSTRING(t.codigo_trabajador, 4) AS UNSIGNED) ASC";
        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function getSupervisores() {
        $query = "SELECT t.id_trabajador, t.nombres, t.apellidos, c.nombre as cargo_nombre 
                  FROM " . $this->table_name . " t
                  LEFT JOIN cargos c ON t.id_cargo = c.id_cargo
                  WHERE t.estado = 'Activo' AND (t.es_supervisor = 1 OR t.id_trabajador = 1)
                  ORDER BY CASE WHEN t.id_trabajador = 1 THEN 0 ELSE 1 END ASC, CAST(SUBSTRING(t.codigo_trabajador, 4) AS UNSIGNED) ASC";
        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function getSupervisoresConConteo() {
        $query = "SELECT s.id_trabajador, s.nombres, s.apellidos, s.id_cargo, c.nombre as cargo_nombre, s.email, s.telefono, s.estado,
                  (SELECT COUNT(*) FROM " . $this->table_name . " t WHERE t.id_supervisor = s.id_trabajador AND t.estado = 'Activo') as num_trabajadores,
                  (SELECT GROUP_CONCAT(CONCAT(t.codigo_trabajador, ' - ', SUBSTRING_INDEX(t.nombres, ' ', 1), ' ', SUBSTRING_INDEX(t.apellidos, ' ', 1)) SEPARATOR ', ') FROM " . $this->table_name . " t WHERE t.id_supervisor = s.id_trabajador AND t.estado = 'Activo') as lista_trabajadores
                  FROM " . $this->table_name . " s
                  LEFT JOIN cargos c ON s.id_cargo = c.id_cargo
                  WHERE s.estado = 'Activo' AND ((s.tipo = 'Colaborador' AND s.es_supervisor = 1) OR s.id_trabajador = 1)
                  ORDER BY CASE WHEN s.id_trabajador = 1 THEN 0 ELSE 1 END ASC, CAST(SUBSTRING(s.codigo_trabajador, 4) AS UNSIGNED) ASC";
        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function getPosiblesSupervisores() {
        $query = "SELECT t.id_trabajador, t.nombres, t.apellidos, c.nombre as cargo_nombre 
                  FROM " . $this->table_name . " t
                  LEFT JOIN cargos c ON t.id_cargo = c.id_cargo
                  WHERE t.tipo = 'Colaborador' AND t.estado = 'Activo' AND t.es_supervisor = 0 AND t.id_trabajador != 1
                  ORDER BY CASE WHEN t.id_trabajador = 1 THEN 0 ELSE 1 END ASC, CAST(SUBSTRING(t.codigo_trabajador, 4) AS UNSIGNED) ASC";
        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function hacerSupervisor($id_trabajador) {
        $query = "UPDATE " . $this->table_name . " SET es_supervisor = 1 WHERE id_trabajador = :id";
        $stmt = $this->conn->prepare($query);
        return $stmt->execute([':id' => $id_trabajador]);
    }

    public function quitarSupervisor($id_trabajador) {
        $query2 = "UPDATE " . $this->table_name . " SET id_supervisor = NULL WHERE id_supervisor = :id";
        $stmt2 = $this->conn->prepare($query2);
        $stmt2->execute([':id' => $id_trabajador]);

        $query = "UPDATE " . $this->table_name . " SET es_supervisor = 0 WHERE id_trabajador = :id";
        $stmt = $this->conn->prepare($query);
        return $stmt->execute([':id' => $id_trabajador]);
    }

    public function getTrabajadoresPorSupervisor($id_supervisor) {
        $query = "SELECT t.*, c.nombre as cargo_nombre, a.nombre as agencia_nombre 
                  FROM " . $this->table_name . " t
                  LEFT JOIN cargos c ON t.id_cargo = c.id_cargo
                  LEFT JOIN agencias a ON t.id_agencia = a.id_agencia
                  WHERE t.id_supervisor = ? AND t.id_trabajador != 1 AND COALESCE(t.codigo_trabajador, '') != 'WS-0001'
                  ORDER BY t.estado ASC, CASE WHEN t.id_trabajador = 1 THEN 0 ELSE 1 END ASC, CAST(SUBSTRING(t.codigo_trabajador, 4) AS UNSIGNED) ASC";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $id_supervisor);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function getCargos() {
        $query = "SELECT c.*, (SELECT COUNT(*) FROM " . $this->table_name . " t WHERE t.id_cargo = c.id_cargo) as num_trabajadores FROM cargos c ORDER BY c.nombre ASC";
        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function getAgencias() {
        $query = "SELECT * FROM agencias ORDER BY nombre ASC";
        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function getHistorialAsistencias($id_trabajador) {
        $query = "
            SELECT fecha, hora_entrada, hora_salida, estado_asistencia 
            FROM asistencias 
            WHERE id_trabajador = :id 
            
            UNION ALL
            
            SELECT fecha_falta as fecha, NULL as hora_entrada, NULL as hora_salida, 'Falta' as estado_asistencia
            FROM faltas
            WHERE id_trabajador = :id
            
            ORDER BY fecha DESC, hora_entrada DESC
        ";
        $stmt = $this->conn->prepare($query);
        $stmt->execute([':id' => $id_trabajador]);
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function crear($datos) {
        $query = "INSERT INTO " . $this->table_name . " 
                  (dni, codigo_trabajador, nombres, apellidos, fecha_nacimiento, direccion, departamento, provincia, distrito, telefono, email, id_cargo, sueldo_base, id_agencia, id_coordinador, id_supervisor, tipo, fecha_inicio, fecha_fin_contrato, experiencia, estado, rostro_descriptor, huella_descriptor, foto_perfil, firma, cv_path, contrato_path)
                  VALUES (:dni, :codigo_trabajador, :nombres, :apellidos, :fecha_nacimiento, :direccion, :departamento, :provincia, :distrito, :telefono, :email, :id_cargo, :sueldo_base, :id_agencia, :id_coordinador, :id_supervisor, :tipo, :fecha_inicio, :fecha_fin_contrato, :experiencia, :estado, :rostro, :huella, :foto, :firma, :cv, :contrato)";
                  
        $stmt = $this->conn->prepare($query);

        $stmt->bindParam(":dni", $datos['dni']);
        $stmt->bindParam(":codigo_trabajador", $datos['codigo_trabajador']);
        $stmt->bindParam(":nombres", $datos['nombres']);
        $stmt->bindParam(":apellidos", $datos['apellidos']);
        $stmt->bindParam(":fecha_nacimiento", $datos['fecha_nacimiento']);
        $stmt->bindParam(":direccion", $datos['direccion']);
        $stmt->bindValue(":departamento", !empty($datos['departamento']) ? $datos['departamento'] : null);
        $stmt->bindValue(":provincia", !empty($datos['provincia']) ? $datos['provincia'] : null);
        $stmt->bindValue(":distrito", !empty($datos['distrito']) ? $datos['distrito'] : null);
        $stmt->bindParam(":telefono", $datos['telefono']);
        $stmt->bindParam(":email", $datos['email']);
        $stmt->bindParam(":id_cargo", $datos['id_cargo']);
        $stmt->bindParam(":sueldo_base", $datos['sueldo_base']);
        $stmt->bindParam(":id_agencia", $datos['id_agencia']);
        $stmt->bindValue(":id_coordinador", !empty($datos['id_coordinador']) ? $datos['id_coordinador'] : null, PDO::PARAM_INT);
        $stmt->bindValue(":id_supervisor", !empty($datos['id_supervisor']) ? $datos['id_supervisor'] : null, PDO::PARAM_INT);
        $stmt->bindParam(":tipo", $datos['tipo']);
        $stmt->bindValue(":fecha_inicio", !empty($datos['fecha_inicio']) ? $datos['fecha_inicio'] : null);
        $stmt->bindValue(":fecha_fin_contrato", !empty($datos['fecha_fin_contrato']) ? $datos['fecha_fin_contrato'] : null);
        $stmt->bindParam(":experiencia", $datos['experiencia']);
        $stmt->bindParam(":estado", $datos['estado']);
        $stmt->bindParam(":rostro", $datos['rostro_descriptor']);
        $stmt->bindParam(":huella", $datos['huella_descriptor']);
        $stmt->bindParam(":foto", $datos['foto_perfil']);
        $stmt->bindParam(":firma", $datos['firma']);
        $stmt->bindParam(":cv", $datos['cv_path']);
        $stmt->bindParam(":contrato", $datos['contrato_path']);

        if($stmt->execute()) {
            return $this->conn->lastInsertId();
        }
        return false;
    }

    public function actualizar($id, $datos) {
        $query = "UPDATE " . $this->table_name . " SET 
                  codigo_trabajador = :codigo_trabajador, dni = :dni, nombres = :nombres, apellidos = :apellidos, 
                  fecha_nacimiento = :fecha_nacimiento, direccion = :direccion, 
                  departamento = :departamento, provincia = :provincia, distrito = :distrito, 
                  telefono = :telefono, email = :email, id_cargo = :id_cargo, sueldo_base = :sueldo_base,
                  id_agencia = :id_agencia, id_coordinador = :id_coordinador, id_supervisor = :id_supervisor, tipo = :tipo, fecha_inicio = :fecha_inicio, fecha_fin_contrato = :fecha_fin_contrato, 
                  experiencia = :experiencia, estado = :estado,
                  rostro_descriptor = :rostro, huella_descriptor = :huella, foto_perfil = :foto, firma = :firma, cv_path = :cv, contrato_path = :contrato,
                  estado_civil = :estado_civil, nacionalidad = :nacionalidad,
                  copia_dni_path = :copia_dni, certificados_path = :certificados, constancias_medicas_path = :constancias_medicas,
                  formacion_academica = :formacion_academica, habilidades = :habilidades, herramientas = :herramientas,
                  contacto_emergencia_nombre = :contacto_emergencia_nombre, contacto_emergencia_telefono = :contacto_emergencia_telefono, contacto_emergencia_parentesco = :contacto_emergencia_parentesco
                  WHERE id_trabajador = :id";
                  
        $stmt = $this->conn->prepare($query);

        $stmt->bindParam(":codigo_trabajador", $datos['codigo_trabajador']);
        $stmt->bindParam(":dni", $datos['dni']);
        $stmt->bindParam(":nombres", $datos['nombres']);
        $stmt->bindParam(":apellidos", $datos['apellidos']);
        $stmt->bindParam(":direccion", $datos['direccion']);
        $stmt->bindValue(":departamento", !empty($datos['departamento']) ? $datos['departamento'] : null);
        $stmt->bindValue(":provincia", !empty($datos['provincia']) ? $datos['provincia'] : null);
        $stmt->bindValue(":distrito", !empty($datos['distrito']) ? $datos['distrito'] : null);
        $stmt->bindParam(":telefono", $datos['telefono']);
        $stmt->bindParam(":email", $datos['email']);
        $stmt->bindParam(":id_cargo", $datos['id_cargo']);
        $stmt->bindParam(":sueldo_base", $datos['sueldo_base']);
        $stmt->bindParam(":id_agencia", $datos['id_agencia']);
        $stmt->bindValue(":id_coordinador", !empty($datos['id_coordinador']) ? $datos['id_coordinador'] : null, PDO::PARAM_INT);
        $stmt->bindValue(":id_supervisor", !empty($datos['id_supervisor']) ? $datos['id_supervisor'] : null, PDO::PARAM_INT);
        $stmt->bindParam(":tipo", $datos['tipo']);
        $stmt->bindValue(":fecha_fin_contrato", !empty($datos['fecha_fin_contrato']) ? $datos['fecha_fin_contrato'] : null);
        $stmt->bindValue(":fecha_nacimiento", !empty($datos['fecha_nacimiento']) ? $datos['fecha_nacimiento'] : null);
        $stmt->bindValue(":fecha_inicio", !empty($datos['fecha_inicio']) ? $datos['fecha_inicio'] : null);
        $stmt->bindParam(":experiencia", $datos['experiencia']);
        $stmt->bindParam(":estado", $datos['estado']);
        $stmt->bindParam(":rostro", $datos['rostro_descriptor']);
        $stmt->bindParam(":huella", $datos['huella_descriptor']);
        $stmt->bindParam(":foto", $datos['foto_perfil']);
        $stmt->bindParam(":firma", $datos['firma']);
        $stmt->bindParam(":cv", $datos['cv_path']);
        $stmt->bindParam(":contrato", $datos['contrato_path']);

        // Nuevos campos de Ficha de Colaborador
        $stmt->bindValue(":estado_civil", !empty($datos['estado_civil']) ? $datos['estado_civil'] : 'Soltero');
        $stmt->bindValue(":nacionalidad", !empty($datos['nacionalidad']) ? $datos['nacionalidad'] : 'Peruana');
        $stmt->bindValue(":copia_dni", !empty($datos['copia_dni_path']) ? $datos['copia_dni_path'] : null);
        $stmt->bindValue(":certificados", !empty($datos['certificados_path']) ? $datos['certificados_path'] : null);
        $stmt->bindValue(":constancias_medicas", !empty($datos['constancias_medicas_path']) ? $datos['constancias_medicas_path'] : null);
        $stmt->bindValue(":formacion_academica", !empty($datos['formacion_academica']) ? $datos['formacion_academica'] : null);
        $stmt->bindValue(":habilidades", !empty($datos['habilidades']) ? $datos['habilidades'] : null);
        $stmt->bindValue(":herramientas", !empty($datos['herramientas']) ? $datos['herramientas'] : null);
        $stmt->bindValue(":contacto_emergencia_nombre", !empty($datos['contacto_emergencia_nombre']) ? $datos['contacto_emergencia_nombre'] : null);
        $stmt->bindValue(":contacto_emergencia_telefono", !empty($datos['contacto_emergencia_telefono']) ? $datos['contacto_emergencia_telefono'] : null);
        $stmt->bindValue(":contacto_emergencia_parentesco", !empty($datos['contacto_emergencia_parentesco']) ? $datos['contacto_emergencia_parentesco'] : null);

        $stmt->bindParam(":id", $id);

        if($stmt->execute()) {
            return true;
        }
        return false;
    }

    public function noRenovar($id) {
        // En lugar de desactivarlo ya, lo marcamos para no renovar. El cron/dashboard lo desactivará cuando llegue su fecha.
        $query = "UPDATE " . $this->table_name . " SET experiencia = CONCAT(COALESCE(experiencia, ''), '\n[NO RENOVAR]') WHERE id_trabajador = ?";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $id);
        if($stmt->execute()) {
            return true;
        }
        return false;
    }

    public function renovarContrato($id, $nueva_fecha) {
        $query = "UPDATE " . $this->table_name . " SET fecha_fin_contrato = ?, estado = 'Activo' WHERE id_trabajador = ?";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $nueva_fecha);
        $stmt->bindParam(2, $id);
        if($stmt->execute()) {
            return true;
        }
        return false;
    }

    public function tieneAlertaDisciplinariaCritica($id) {
        $mes = date('m');
        $anio = date('Y');
        $stmt = $this->conn->prepare("SELECT COUNT(*) FROM asistencias 
                                     WHERE id_trabajador = :id 
                                     AND MONTH(fecha) = :mes AND YEAR(fecha) = :anio 
                                     AND estado_asistencia = 'Tardanza'");
        $stmt->execute([':id' => $id, ':mes' => (int)$mes, ':anio' => (int)$anio]);
        $tardanzas = (int)$stmt->fetchColumn();
        return ($tardanzas >= 5);
    }
    public function crearCargo($nombre, $sueldo_base) {
        $query = "INSERT INTO cargos (nombre, sueldo_base) VALUES (:nombre, :sueldo_base)";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(":nombre", $nombre);
        $stmt->bindParam(":sueldo_base", $sueldo_base);
        return $stmt->execute();
    }

    public function actualizarCargo($id, $nombre, $sueldo_base) {
        $query = "UPDATE cargos SET nombre = :nombre, sueldo_base = :sueldo_base WHERE id_cargo = :id";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(":nombre", $nombre);
        $stmt->bindParam(":sueldo_base", $sueldo_base);
        $stmt->bindParam(":id", $id);
        return $stmt->execute();
    }

    public function eliminarCargo($id) {
        $query = "DELETE FROM cargos WHERE id_cargo = :id";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(":id", $id);
        return $stmt->execute();
    }
}
