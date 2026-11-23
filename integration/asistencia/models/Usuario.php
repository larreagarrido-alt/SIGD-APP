<?php
require_once 'config/conexion.php';

class Usuario {
    private $conn;
    private $table_name = "usuarios";

    public $id_usuario;
    public $username;
    public $password;
    public $id_rol;
    public $id_trabajador;
    public $rol_nombre;
    public $foto_perfil;
    public $debe_cambiar_password;

    public function __construct() {
        $database = new Conexion();
        $this->conn = $database->getConexion();
    }

    public function login($username, $password) {
        $query = "SELECT u.*, r.nombre as rol_nombre, t.foto_perfil
                  FROM " . $this->table_name . " u
                  INNER JOIN roles r ON u.id_rol = r.id_rol
                  LEFT JOIN trabajadores t ON u.id_trabajador = t.id_trabajador
                  WHERE u.username = :username
                    AND (u.id_trabajador IS NULL OR (t.id_trabajador IS NOT NULL AND t.estado <> 'Inactivo')) LIMIT 0,1";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':username', $username);
        $stmt->execute();

        if ($stmt->rowCount() > 0) {
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            if (password_verify($password, $row['password'])) {
                $this->id_usuario = $row['id_usuario'];
                $this->username = $row['username'];
                $this->id_rol = $row['id_rol'];
                $this->id_trabajador = $row['id_trabajador'];
                $this->rol_nombre = $row['rol_nombre'];
                $this->foto_perfil = $row['foto_perfil'];
                $this->debe_cambiar_password = isset($row['debe_cambiar_password']) ? $row['debe_cambiar_password'] : 0;
                return true;
            }
        }
        return false;
    }

    public function getByTrabajadorId($id_trabajador) {
        $query = "SELECT u.*, r.nombre as rol_nombre
                  FROM " . $this->table_name . " u
                  INNER JOIN roles r ON u.id_rol = r.id_rol
                  WHERE u.id_trabajador = :id LIMIT 0,1";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':id', $id_trabajador);
        $stmt->execute();
        if($stmt->rowCount() > 0) {
            return $stmt->fetch(PDO::FETCH_ASSOC);
        }
        return false;
    }

    public function crear($datos) {
        $query = "INSERT INTO " . $this->table_name . " (username, password, id_rol, id_trabajador, debe_cambiar_password) VALUES (:username, :password, :id_rol, :id_trabajador, :debe_cambiar_password)";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':username', $datos['username']);
        $stmt->bindParam(':password', $datos['password']);
        $stmt->bindParam(':id_rol', $datos['id_rol']);
        $stmt->bindParam(':id_trabajador', $datos['id_trabajador']);
        $debeCambiarPassword = (int)($datos['debe_cambiar_password'] ?? 0);
        $stmt->bindParam(':debe_cambiar_password', $debeCambiarPassword, PDO::PARAM_INT);

        if($stmt->execute()) {
            return true;
        }
        return false;
    }

    public function obtenerUsuarios() {
        $query = "SELECT u.id_usuario, u.username, r.nombre as rol_nombre, t.nombres, t.apellidos
                  FROM " . $this->table_name . " u
                  INNER JOIN roles r ON u.id_rol = r.id_rol
                  LEFT JOIN trabajadores t ON u.id_trabajador = t.id_trabajador
                  ORDER BY u.username ASC";
        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
}
