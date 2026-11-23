<?php
require_once 'models/Trabajador.php';

class TrabajadorController {
    private $horarioModel;

    // --- SEGURIDAD FASE 2: Helpers para subida de archivos (REMOVIDOS Y CENTRALIZADOS) ---
    // Se utilizará helpers/UploadHelper.php para todas las subidas
    // ---------------------------------------------------------

    // --- SEGURIDAD FASE 3: Validaciones estrictas de Backend ---
    private function validarDatosTrabajador($dni, $telefono, $fecha_nacimiento) {
        $errores = [];

        if (empty($dni) || !preg_match('/^[0-9A-Za-z.\-]{8,15}$/', $dni)) {
            $errores[] = "El DNI o Documento debe tener entre 8 y 15 caracteres (se permiten letras, puntos y guiones para extranjeros).";
        }

        if (!empty($telefono) && !preg_match('/^[0-9+\s\-]{9,20}$/', $telefono)) {
            $errores[] = "El número de celular debe ser válido (se permiten números, +, espacios y guiones).";
        }

        if (!empty($fecha_nacimiento)) {
            $hoy = date('Y-m-d');
            if ($fecha_nacimiento > $hoy) {
                $errores[] = "La fecha de nacimiento no puede estar en el futuro.";
            } else {
                $edad = date_diff(date_create($fecha_nacimiento), date_create($hoy))->y;
                if ($edad < 15 || $edad > 95) {
                    $errores[] = "La fecha de nacimiento arroja una edad inválida ($edad años).";
                }
            }
        }

        return $errores;
    }
    // -----------------------------------------------------------

    public function index() {
        
        $trabajador = new Trabajador();
        $stmt = $trabajador->leerTodos();
        $trabajadores = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Cargar datos para el modal de creación
        $cargos = $trabajador->getCargos();
        $agencias = $trabajador->getAgencias();
        require_once 'models/CoordinadorAcademico.php';
        $coordinadores = (new CoordinadorAcademico())->listar();
        $supervisores = $trabajador->getSupervisores();
        
        // Cargar trabajadores y practicantes (para asignar a nuevos supervisores/coordinadores si fuera necesario)
        $todos_los_trabajadores = $trabajadores; // Ya los tenemos de leerTodos()
        $empleados_disponibles = array_filter($todos_los_trabajadores, function($t) { return $t['tipo'] === 'Colaborador'; });
        $practicantes_disponibles = array_filter($todos_los_trabajadores, function($t) { return $t['tipo'] === 'Practicante'; });
        
        require_once 'views/trabajadores/index.php';
    }

    public function crear() {
        if(!isset($_SESSION['rol']) || !in_array($_SESSION['rol'], ['Administrador', 'Gerencia'])) {
            header("Location: index.php?page=dashboard&error=" . urlencode("Acceso denegado: Solo Administrador o Gerencia pueden crear trabajadores."));
            exit;
        }

        $trabajador = new Trabajador();
        
        if($_SERVER['REQUEST_METHOD'] == 'POST') {
            $initialPassword = (string)($_POST['password_inicial'] ?? '');
            if (strlen($initialPassword) < 12 || strlen($initialPassword) > 128 || $initialPassword === (string)($_POST['dni'] ?? '')) {
                die('La clave inicial debe tener entre 12 y 128 caracteres y ser distinta del DNI.');
            }
            // --- SEGURIDAD FASE 3: Validar datos antes de procesar ---
            $errores_backend = $this->validarDatosTrabajador($_POST['dni'] ?? '', $_POST['telefono'] ?? '', $_POST['fecha_nacimiento'] ?? '');
            if (!empty($errores_backend)) {
                die("Error de validación de seguridad: " . implode(" | ", $errores_backend));
            }
            // Subida Segura de Archivos FASE 2
            require_once 'helpers/UploadHelper.php';
            $cv_path = null;
            $contrato_path = null;
            $firma_path = null;
            $foto_path = null;

            try {
                if (isset($_FILES['cv_archivo']) && $_FILES['cv_archivo']['error'] != UPLOAD_ERR_NO_FILE) {
                    $cv_path = UploadHelper::handleUpload($_FILES['cv_archivo'], UploadHelper::TYPE_PRIVATE_DOC);
                }
                if (isset($_FILES['contrato_archivo']) && $_FILES['contrato_archivo']['error'] != UPLOAD_ERR_NO_FILE) {
                    $contrato_path = UploadHelper::handleUpload($_FILES['contrato_archivo'], UploadHelper::TYPE_PRIVATE_DOC);
                }
                if (isset($_FILES['firma']) && $_FILES['firma']['error'] != UPLOAD_ERR_NO_FILE) {
                    $firma_path = UploadHelper::handleUpload($_FILES['firma'], UploadHelper::TYPE_PRIVATE_DOC);
                }
                if (isset($_FILES['foto_archivo']) && $_FILES['foto_archivo']['error'] != UPLOAD_ERR_NO_FILE) {
                    $foto_path = UploadHelper::handleUpload($_FILES['foto_archivo'], UploadHelper::TYPE_PUBLIC_IMAGE);
                }
            } catch (Exception $e) {
                die('<script>alert(' . json_encode('Error al subir archivo: ' . $e->getMessage()) . ');history.back();</script>');
            }

            if (!$foto_path && !empty($_POST['foto_base64'])) {
                // Implementación rápida en línea para Base64 usando lógia segura
                $partes = explode(',', $_POST['foto_base64']);
                if (count($partes) == 2) {
                    $datos_foto = base64_decode($partes[1]);
                    $ext = 'jpg';
                    $targetDir = __DIR__ . '/../uploads/';
                    if (!is_dir($targetDir)) @mkdir($targetDir, 0750, true);
                    $nombre_archivo = bin2hex(random_bytes(16)) . '.' . $ext;
                    $ruta_final = $targetDir . $nombre_archivo;
                    if (file_put_contents($ruta_final, $datos_foto)) {
                        $foto_path = 'uploads/' . $nombre_archivo;
                    }
                }
            }

            // Auto-generar código de trabajador WS-XXXX
            require_once 'config/conexion.php';
            $db_code = new Conexion();
            $conn_code = $db_code->getConexion();
            $query_code = "SELECT MAX(CAST(SUBSTRING(codigo_trabajador, 4) AS UNSIGNED)) as last_num FROM trabajadores WHERE codigo_trabajador LIKE 'WS-%'";
            $row_code = $conn_code->query($query_code)->fetch(PDO::FETCH_ASSOC);
            $next_num = isset($row_code['last_num']) ? intval($row_code['last_num']) + 1 : 1;
            $codigo_auto = 'WS-' . str_pad($next_num, 4, '0', STR_PAD_LEFT);

            $codigo_final = !empty($_POST['codigo_trabajador']) ? $_POST['codigo_trabajador'] : $codigo_auto;

            $datos = [
                'dni' => $_POST['dni'],
                'codigo_trabajador' => $codigo_final,
                'nombres' => $_POST['nombres'],
                'apellidos' => $_POST['apellidos'],
                'fecha_nacimiento' => $_POST['fecha_nacimiento'],
                'direccion' => $_POST['direccion'] ?? null,
                'departamento' => $_POST['departamento'] ?? null,
                'provincia' => $_POST['provincia'] ?? null,
                'distrito' => $_POST['distrito'] ?? null,
                'telefono' => $_POST['telefono'] ?? null,
                'email' => $_POST['email'] ?? null,
                'id_cargo' => $_POST['id_cargo'],
                'sueldo_base' => isset($_POST['sueldo_base']) ? $_POST['sueldo_base'] : null,
                'id_agencia' => $_POST['id_agencia'],
                'id_coordinador' => isset($_POST['id_coordinador']) && $_POST['id_coordinador'] !== '' ? $_POST['id_coordinador'] : null,
                'id_supervisor' => isset($_POST['id_supervisor']) && $_POST['id_supervisor'] !== '' ? $_POST['id_supervisor'] : null,
                'tipo' => $_POST['tipo'],
                'fecha_inicio' => $_POST['fecha_inicio'],
                'fecha_fin_contrato' => !empty($_POST['fecha_fin_contrato']) ? $_POST['fecha_fin_contrato'] : null,
                'experiencia' => $_POST['experiencia'] ?? null,
                'estado' => $_POST['estado'] ?? 'Activo',
                'rostro_descriptor' => isset($_POST['rostro_descriptor']) ? $_POST['rostro_descriptor'] : null,
                'huella_descriptor' => isset($_POST['huella_descriptor']) ? $_POST['huella_descriptor'] : null,
                'foto_perfil' => $foto_path,
                'firma' => $firma_path,
                'cv_path' => $cv_path,
                'contrato_path' => $contrato_path
            ];
            
            try {
                $nuevo_id = $trabajador->crear($datos);
                if($nuevo_id) {
                    // Crear el usuario automáticamente
                    require_once 'models/Usuario.php';
                    $usuarioModel = new Usuario();

                    $username = $_POST['dni'];
                    $password = password_hash($initialPassword, PASSWORD_DEFAULT);
                    $id_rol = ($_POST['tipo'] === 'Practicante') ? 6 : 5; // Rol 6 para Practicantes, 5 para Trabajadores
                    
                    $usuarioModel->crear([
                        'username' => $username,
                        'password' => $password,
                        'id_rol' => $id_rol,
                        'id_trabajador' => $nuevo_id,
                        'debe_cambiar_password' => 1
                    ]);

                    // Si se asignaron trabajadores a este nuevo supervisor
                    if(isset($_POST['trabajadores_asignados']) && is_array($_POST['trabajadores_asignados']) && !empty($_POST['trabajadores_asignados'])) {
                        require_once 'config/conexion.php';
                        $db_asig = new Conexion();
                        $conn_asig = $db_asig->getConexion();
                        foreach($_POST['trabajadores_asignados'] as $id_asignado) {
                            $stmt_asig = $conn_asig->prepare("UPDATE trabajadores SET id_supervisor = :id_sup WHERE id_trabajador = :id_trab");
                            $stmt_asig->execute([
                                ':id_sup' => $nuevo_id,
                                ':id_trab' => $id_asignado
                            ]);
                        }
                    }

                    header("Location: index.php?page=trabajadores&success=1");
                    exit;
                } else {
                    $error = "Error al crear el trabajador";
                }
            } catch (Exception $e) {
                die("Error de Base de Datos al crear trabajador. Es posible que falte ejecutar el script de actualización de columnas. Detalles: " . $e->getMessage());
            }
        }
        // Si se accede por GET a crear (que ya no debería pasar por la interfaz), redirigir
        header("Location: index.php?page=trabajadores");
        exit;
    }

    public function perfil() {
        $id = isset($_GET['id']) ? $_GET['id'] : die('ID de trabajador no especificado');
        
        // SEGURIDAD FASE 2: Prevención de IDOR - Restringir vista de perfiles ajenos
        if ($_SESSION['trabajador_id'] != $id && (!isset($_SESSION['rol']) || (!in_array($_SESSION['rol'], ['Administrador', 'Gerencia']) && strpos($_SESSION['rol'], 'Jefe') === false))) {
            die('Acceso denegado: No tienes permisos para ver el perfil de este trabajador.');
        }
        $trabajadorModel = new Trabajador();
        $trabajador = $trabajadorModel->leerUno($id);
        
        if(!$trabajador) {
            echo "Trabajador no encontrado";
            exit;
        }

        // Consultar si el trabajador alcanzó el límite crítico de 5 tardanzas
        require_once 'models/AlertaDisciplinaria.php';
        $alertaModel = new AlertaDisciplinaria();
        $tieneAlertaCritica = $alertaModel->tieneAlertaCritica($id);
        $tardanzasMes = $alertaModel->contarTardanzasMes($id);
        $faltasMes = $alertaModel->contarFaltasMes($id);

        // Aquí también podríamos cargar sus contratos, documentos, etc.
        require_once 'views/trabajadores/perfil.php';
    }

    public function mi_perfil() {
        if(!isset($_SESSION['trabajador_id'])) {
            echo "Sesión no válida para esta acción.";
            exit;
        }

        $id = $_SESSION['trabajador_id'];
        $trabajadorModel = new Trabajador();

        if($_SERVER['REQUEST_METHOD'] == 'POST') {
            // --- SEGURIDAD FASE 3: Validar datos antes de procesar ---
            $errores_backend = $this->validarDatosTrabajador($_POST['dni'] ?? '', $_POST['telefono'] ?? '', $_POST['fecha_nacimiento'] ?? '');
            if (!empty($errores_backend)) {
                die("Error de validación de seguridad: " . implode(" | ", $errores_backend));
            }

            $trabajador_antiguo = $trabajadorModel->leerUno($id);
            
            // Subida Segura FASE 2
            require_once 'helpers/UploadHelper.php';
            
            try {
                $cv_path = (isset($_FILES['cv_archivo']) && $_FILES['cv_archivo']['error'] != UPLOAD_ERR_NO_FILE) ? UploadHelper::handleUpload($_FILES['cv_archivo'], UploadHelper::TYPE_PRIVATE_DOC) : $trabajador_antiguo['cv_path'];
                $firma_path = (isset($_FILES['firma']) && $_FILES['firma']['error'] != UPLOAD_ERR_NO_FILE) ? UploadHelper::handleUpload($_FILES['firma'], UploadHelper::TYPE_PRIVATE_DOC) : $trabajador_antiguo['firma'];
            } catch (Exception $e) {
                die('<script>alert(' . json_encode('Error al subir archivo: ' . $e->getMessage()) . ');history.back();</script>');
            }
            
            $foto_path = $trabajador_antiguo['foto_perfil'];
            if(!empty($_POST['foto_base64'])) {
                // Implementación rápida en línea para Base64 usando lógia segura
                $partes = explode(',', $_POST['foto_base64']);
                if (count($partes) == 2) {
                    $datos_foto = base64_decode($partes[1]);
                    $ext = 'jpg';
                    $targetDir = __DIR__ . '/../uploads/';
                    if (!is_dir($targetDir)) @mkdir($targetDir, 0750, true);
                    $nombre_archivo = bin2hex(random_bytes(16)) . '.' . $ext;
                    $ruta_final = $targetDir . $nombre_archivo;
                    if (file_put_contents($ruta_final, $datos_foto)) {
                        $foto_path = 'uploads/' . $nombre_archivo;
                    }
                }
            }

            // Actualizar credenciales si se proporcionan
            require_once 'config/conexion.php';
            $db = new Conexion();
            $conn = $db->getConexion();
            
            if(!empty($_POST['username'])) {
                $stmt = $conn->prepare("UPDATE usuarios SET username = :user WHERE id_trabajador = :id");
                $stmt->execute([':user' => $_POST['username'], ':id' => $id]);
            }
            if(!empty($_POST['password'])) {
                $stmt = $conn->prepare("UPDATE usuarios SET password = :pwd, debe_cambiar_password = 0 WHERE id_trabajador = :id");
                $stmt->execute([':pwd' => password_hash($_POST['password'], PASSWORD_DEFAULT), ':id' => $id]);
            }

            $datos = [
                'direccion' => $_POST['direccion'],
                'telefono' => $_POST['telefono'],
                'email' => $_POST['email'],
                'rostro_descriptor' => isset($_POST['rostro_descriptor']) && !empty($_POST['rostro_descriptor']) ? $_POST['rostro_descriptor'] : $trabajador_antiguo['rostro_descriptor'],
                'huella_descriptor' => isset($_POST['huella_descriptor']) && !empty($_POST['huella_descriptor']) ? $_POST['huella_descriptor'] : $trabajador_antiguo['huella_descriptor'],
                'foto_perfil' => $foto_path,
                'firma' => $firma_path,
                'cv_path' => $cv_path,
                // Valores modificables por el usuario
                'dni' => $_POST['dni'],
                'nombres' => $_POST['nombres'],
                'apellidos' => $_POST['apellidos'],
                'fecha_nacimiento' => isset($_POST['fecha_nacimiento']) ? $_POST['fecha_nacimiento'] : $trabajador_antiguo['fecha_nacimiento'],
                'tipo' => isset($_POST['tipo']) ? $_POST['tipo'] : $trabajador_antiguo['tipo'],
                'fecha_inicio' => isset($_POST['fecha_inicio']) ? $_POST['fecha_inicio'] : $trabajador_antiguo['fecha_inicio'],
                'fecha_fin_contrato' => isset($_POST['fecha_fin_contrato']) ? $_POST['fecha_fin_contrato'] : $trabajador_antiguo['fecha_fin_contrato'],
                // Conservar los valores inmodificables para que el update no los borre (excepto si es Admin)
                'codigo_trabajador' => (isset($_SESSION['rol']) && $_SESSION['rol'] === 'Administrador' && isset($_POST['codigo_trabajador'])) ? $_POST['codigo_trabajador'] : $trabajador_antiguo['codigo_trabajador'],
                'id_cargo' => $trabajador_antiguo['id_cargo'],
                'sueldo_base' => isset($_POST['sueldo_base']) ? $_POST['sueldo_base'] : $trabajador_antiguo['sueldo_base'],
                'id_agencia' => $trabajador_antiguo['id_agencia'],
                'estado' => $trabajador_antiguo['estado'],
                'experiencia' => $trabajador_antiguo['experiencia']
            ];
            
            if($trabajadorModel->actualizar($id, $datos)) {
                // NUEVO: Actualizar la sesión si es el usuario actual
                if (isset($_SESSION['trabajador_id']) && $_SESSION['trabajador_id'] == $id) {
                    $_SESSION['foto_perfil'] = $foto_path;
                    if (!empty($_POST['username'])) {
                        $_SESSION['username'] = $_POST['username'];
                    }
                }
                header("Location: index.php?page=mi_perfil&success=1");
                exit;
            } else {
                $error = "Error al actualizar su perfil.";
            }
        }
        
        $trabajador = $trabajadorModel->leerUno($id);
        
        // Fetch username
        require_once 'config/conexion.php';
        $db = new Conexion();
        $conn = $db->getConexion();
        $stmt = $conn->prepare("SELECT username FROM usuarios WHERE id_trabajador = :id");
        $stmt->execute([':id' => $id]);
        $usuarioData = $stmt->fetch(PDO::FETCH_ASSOC);
        $trabajador['username'] = $usuarioData ? $usuarioData['username'] : '';
        // Fetch Llamados de Atención
        require_once 'models/LlamadoAtencion.php';
        $llamadoModel = new LlamadoAtencion();
        $llamadosAtencion = $llamadoModel->obtenerPorTrabajador($id);

        // Fetch Nivel Actual
        require_once 'models/Asistencia.php';
        $asistenciaModel = new Asistencia();
        $mesActual = date('m');
        $anioActual = date('Y');
        $nivelTrabajador = $asistenciaModel->obtenerNivelTardanzasMes($id, $mesActual, $anioActual);

        require_once 'views/trabajadores/mi_perfil.php';
    }

    public function editar() {
        // SEGURIDAD FASE 2: Prevención de IDOR - Solo roles autorizados
        if(!isset($_SESSION['rol']) || (!in_array($_SESSION['rol'], ['Administrador', 'Gerencia']) && strpos($_SESSION['rol'], 'Jefe') === false)) {
            die('Acceso denegado: No tienes permisos para editar perfiles.');
        }

        $id = isset($_GET['id']) ? $_GET['id'] : die('ID de trabajador no especificado');
        $trabajadorModel = new Trabajador();

        if($_SERVER['REQUEST_METHOD'] == 'POST') {
            // --- SEGURIDAD FASE 3: Validar datos antes de procesar ---
            $errores_backend = $this->validarDatosTrabajador($_POST['dni'] ?? '', $_POST['telefono'] ?? '', $_POST['fecha_nacimiento'] ?? '');
            if (!empty($errores_backend)) {
                die("Error de validación de seguridad: " . implode(" | ", $errores_backend));
            }

            $trabajador_antiguo = $trabajadorModel->leerUno($id);
            
            // Subida Segura FASE 2
            require_once 'helpers/UploadHelper.php';
            
            try {
                $cv_path = (isset($_FILES['cv_archivo']) && $_FILES['cv_archivo']['error'] != UPLOAD_ERR_NO_FILE) ? UploadHelper::handleUpload($_FILES['cv_archivo'], UploadHelper::TYPE_PRIVATE_DOC) : $trabajador_antiguo['cv_path'];
                $contrato_path = (isset($_FILES['contrato_archivo']) && $_FILES['contrato_archivo']['error'] != UPLOAD_ERR_NO_FILE) ? UploadHelper::handleUpload($_FILES['contrato_archivo'], UploadHelper::TYPE_PRIVATE_DOC) : $trabajador_antiguo['contrato_path'];
                $firma_path = (isset($_FILES['firma']) && $_FILES['firma']['error'] != UPLOAD_ERR_NO_FILE) ? UploadHelper::handleUpload($_FILES['firma'], UploadHelper::TYPE_PRIVATE_DOC) : $trabajador_antiguo['firma'];
                $copia_dni_path = (isset($_FILES['copia_dni_archivo']) && $_FILES['copia_dni_archivo']['error'] != UPLOAD_ERR_NO_FILE) ? UploadHelper::handleUpload($_FILES['copia_dni_archivo'], UploadHelper::TYPE_PRIVATE_DOC) : ($trabajador_antiguo['copia_dni_path'] ?? null);
                $certificados_path = (isset($_FILES['certificados_archivo']) && $_FILES['certificados_archivo']['error'] != UPLOAD_ERR_NO_FILE) ? UploadHelper::handleUpload($_FILES['certificados_archivo'], UploadHelper::TYPE_PRIVATE_DOC) : ($trabajador_antiguo['certificados_path'] ?? null);
                $constancias_medicas_path = (isset($_FILES['constancias_medicas_archivo']) && $_FILES['constancias_medicas_archivo']['error'] != UPLOAD_ERR_NO_FILE) ? UploadHelper::handleUpload($_FILES['constancias_medicas_archivo'], UploadHelper::TYPE_PRIVATE_DOC) : ($trabajador_antiguo['constancias_medicas_path'] ?? null);
                
                $foto_path = $trabajador_antiguo['foto_perfil'];
                if(isset($_FILES['foto_archivo']) && $_FILES['foto_archivo']['error'] == 0) {
                    $foto_path = UploadHelper::handleUpload($_FILES['foto_archivo'], UploadHelper::TYPE_PUBLIC_IMAGE);
                }
            } catch (Exception $e) {
                die('<script>alert(' . json_encode('Error al subir archivo: ' . $e->getMessage()) . ');history.back();</script>');
            }

            // Eliminación de documentos marcados por el usuario (botón "Eliminar")
            // y registro de archivos viejos para borrarlos del disco tras guardar con éxito
            $archivos_a_borrar = [];
            $mapa_docs = [
                'cv_path'                  => 'cv_path',
                'contrato_path'            => 'contrato_path',
                'firma'                    => 'firma_path',
                'copia_dni_path'           => 'copia_dni_path',
                'certificados_path'        => 'certificados_path',
                'constancias_medicas_path' => 'constancias_medicas_path',
            ];
            foreach ($mapa_docs as $campo_bd => $var) {
                $antiguo = $trabajador_antiguo[$campo_bd] ?? null;
                $subioNuevo = ($$var !== $antiguo);
                if (!$subioNuevo && !empty($_POST['eliminar_' . $campo_bd]) && $_POST['eliminar_' . $campo_bd] === '1') {
                    $$var = null;
                }
                if (!empty($antiguo) && $$var !== $antiguo) {
                    $archivos_a_borrar[] = $antiguo;
                }
            }

            if(!isset($_FILES['foto_archivo']) || $_FILES['foto_archivo']['error'] != 0) {
                if(!empty($_POST['foto_base64'])) {
                    // Implementación rápida en línea para Base64 usando lógia segura
                    $partes = explode(',', $_POST['foto_base64']);
                    if (count($partes) == 2) {
                        $datos_foto = base64_decode($partes[1]);
                        $ext = 'jpg';
                        $targetDir = __DIR__ . '/../uploads/';
                        if (!is_dir($targetDir)) @mkdir($targetDir, 0750, true);
                        $nombre_archivo = bin2hex(random_bytes(16)) . '.' . $ext;
                        $ruta_final = $targetDir . $nombre_archivo;
                        if (file_put_contents($ruta_final, $datos_foto)) {
                            $foto_path = 'uploads/' . $nombre_archivo;
                        }
                    }
                }
            }

            $datos = [
                'codigo_trabajador' => $_POST['codigo_trabajador'],
                'dni' => $_POST['dni'],
                'nombres' => $_POST['nombres'],
                'apellidos' => $_POST['apellidos'],
                'direccion' => $_POST['direccion'] ?? null,
                'departamento' => $_POST['departamento'] ?? null,
                'provincia' => $_POST['provincia'] ?? null,
                'distrito' => $_POST['distrito'] ?? null,
                'telefono' => $_POST['telefono'] ?? null,
                'email' => $_POST['email'] ?? null,
                'id_cargo' => $_POST['id_cargo'],
                'sueldo_base' => isset($_POST['sueldo_base']) ? $_POST['sueldo_base'] : $trabajador_antiguo['sueldo_base'],
                'id_agencia' => isset($_POST['id_agencia']) ? $_POST['id_agencia'] : $trabajador_antiguo['id_agencia'],
                'id_coordinador' => isset($_POST['id_coordinador']) ? ($_POST['id_coordinador'] !== '' ? $_POST['id_coordinador'] : null) : $trabajador_antiguo['id_coordinador'],
                'id_supervisor' => isset($_POST['id_supervisor']) ? ($_POST['id_supervisor'] !== '' ? $_POST['id_supervisor'] : null) : $trabajador_antiguo['id_supervisor'],
                'tipo' => isset($_POST['tipo']) ? $_POST['tipo'] : $trabajador_antiguo['tipo'],
                'fecha_nacimiento' => isset($_POST['fecha_nacimiento']) ? $_POST['fecha_nacimiento'] : $trabajador_antiguo['fecha_nacimiento'],
                'fecha_inicio' => isset($_POST['fecha_inicio']) ? $_POST['fecha_inicio'] : $trabajador_antiguo['fecha_inicio'],
                'fecha_fin_contrato' => isset($_POST['fecha_fin_contrato']) ? $_POST['fecha_fin_contrato'] : $trabajador_antiguo['fecha_fin_contrato'],
                'experiencia' => isset($_POST['experiencia']) ? $_POST['experiencia'] : $trabajador_antiguo['experiencia'],
                'estado' => isset($_POST['estado']) ? $_POST['estado'] : $trabajador_antiguo['estado'],
                'rostro_descriptor' => isset($_POST['rostro_descriptor']) && !empty($_POST['rostro_descriptor']) ? $_POST['rostro_descriptor'] : $trabajador_antiguo['rostro_descriptor'],
                'huella_descriptor' => isset($_POST['huella_descriptor']) && !empty($_POST['huella_descriptor']) ? $_POST['huella_descriptor'] : $trabajador_antiguo['huella_descriptor'],
                'foto_perfil' => $foto_path,
                'firma' => $firma_path,
                'cv_path' => $cv_path,
                'contrato_path' => $contrato_path,
                // Nuevos campos Ficha de Colaborador
                'estado_civil' => $_POST['estado_civil'] ?? ($trabajador_antiguo['estado_civil'] ?? 'Soltero'),
                'nacionalidad' => $_POST['nacionalidad'] ?? ($trabajador_antiguo['nacionalidad'] ?? 'Peruana'),
                'copia_dni_path' => $copia_dni_path,
                'certificados_path' => $certificados_path,
                'constancias_medicas_path' => $constancias_medicas_path,
                'formacion_academica' => $_POST['formacion_academica'] ?? ($trabajador_antiguo['formacion_academica'] ?? null),
                'habilidades' => $_POST['habilidades'] ?? ($trabajador_antiguo['habilidades'] ?? null),
                'herramientas' => $_POST['herramientas'] ?? ($trabajador_antiguo['herramientas'] ?? null),
                'contacto_emergencia_nombre' => $_POST['contacto_emergencia_nombre'] ?? ($trabajador_antiguo['contacto_emergencia_nombre'] ?? null),
                'contacto_emergencia_telefono' => $_POST['contacto_emergencia_telefono'] ?? ($trabajador_antiguo['contacto_emergencia_telefono'] ?? null),
                'contacto_emergencia_parentesco' => $_POST['contacto_emergencia_parentesco'] ?? ($trabajador_antiguo['contacto_emergencia_parentesco'] ?? null)
            ];

            // Actualizar credenciales si se proporcionan
            require_once 'config/conexion.php';
            $db = new Conexion();
            $conn = $db->getConexion();
            
            if(!empty($_POST['username'])) {
                $stmt = $conn->prepare("UPDATE usuarios SET username = :user WHERE id_trabajador = :id");
                $stmt->execute([':user' => $_POST['username'], ':id' => $id]);
            }
            if(!empty($_POST['password'])) {
                $stmt = $conn->prepare("UPDATE usuarios SET password = :pwd, debe_cambiar_password = 0 WHERE id_trabajador = :id");
                $stmt->execute([':pwd' => password_hash($_POST['password'], PASSWORD_DEFAULT), ':id' => $id]);
            }
            if(isset($_POST['tipo'])) {
                $nuevo_rol = ($_POST['tipo'] === 'Practicante') ? 6 : 5;
                $stmt = $conn->prepare("UPDATE usuarios SET id_rol = :rol WHERE id_trabajador = :id");
                $stmt->execute([':rol' => $nuevo_rol, ':id' => $id]);
            }
            
            try {
                if($trabajadorModel->actualizar($id, $datos)) {
                    // Borrar del disco los documentos reemplazados o eliminados
                    $baseSegura = realpath(__DIR__ . '/../uploads_private');
                    foreach ($archivos_a_borrar as $rutaVieja) {
                        $abs = realpath(__DIR__ . '/../' . $rutaVieja);
                        if ($abs && $baseSegura && strpos($abs, $baseSegura) === 0 && is_file($abs)) {
                            @unlink($abs);
                        }
                    }
                    // NUEVO: Actualizar la sesión si el administrador se edita a sí mismo
                    if (isset($_SESSION['trabajador_id']) && $_SESSION['trabajador_id'] == $id) {
                        $_SESSION['foto_perfil'] = $foto_path;
                        if (!empty($_POST['username'])) {
                            $_SESSION['username'] = $_POST['username'];
                        }
                    }
                    header("Location: index.php?page=trabajadores");
                    exit;
                } else {
                    $error = "Error al actualizar.";
                }
            } catch (Exception $e) {
                die("Error de Base de Datos al actualizar trabajador. Detalles: " . $e->getMessage());
            }
        }
        
        $trabajador = $trabajadorModel->leerUno($id);
        
        // Fetch username
        require_once 'config/conexion.php';
        $db = new Conexion();
        $conn = $db->getConexion();
        $stmt = $conn->prepare("SELECT username FROM usuarios WHERE id_trabajador = :id");
        $stmt->execute([':id' => $id]);
        $usuarioData = $stmt->fetch(PDO::FETCH_ASSOC);
        $trabajador['username'] = $usuarioData ? $usuarioData['username'] : '';

        $cargos = $trabajadorModel->getCargos();
        $agencias = $trabajadorModel->getAgencias();
        require_once 'models/CoordinadorAcademico.php';
        $coordinadores = (new CoordinadorAcademico())->listar();
        $supervisores = $trabajadorModel->getSupervisores();
        
        // Cargar historial de contratos
        require_once 'config/conexion.php';
        $db = new Conexion();
        $conn = $db->getConexion();
        $stmt_c = $conn->prepare("SELECT * FROM contratos WHERE id_trabajador = ? ORDER BY fecha_inicio DESC");
        $stmt_c->execute([$id]);
        $contratos = $stmt_c->fetchAll(PDO::FETCH_ASSOC);
        
        require_once 'views/trabajadores/editar.php';
    }

    public function despedir() {
        // VALIDACIÓN 1: Seguridad - Verificar sesión y permisos (Solo Administrador puede despedir)
        if (!isset($_SESSION['usuario_id']) || !isset($_SESSION['rol']) || $_SESSION['rol'] !== 'Administrador') {
            header("Location: index.php?page=dashboard&error=No tienes permisos para realizar esta acción.");
            exit;
        }

        if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !CsrfHelper::verifyToken()) {
            header("Location: index.php?page=trabajadores&error=" . urlencode("Solicitud de cese inválida."));
            exit;
        }

        if(isset($_GET['id'])) {
            $id = $_GET['id'];
            
            // VALIDACIÓN 2: Evitar inyecciones o datos basura - Validar que ID sea número
            if (!is_numeric($id) || $id <= 0) {
                header("Location: index.php?page=trabajadores&error=ID de trabajador inválido.");
                exit;
            }

            // VALIDACIÓN 3: Lógica - Evitar auto-eliminación
            if (isset($_SESSION['trabajador_id']) && $_SESSION['trabajador_id'] == $id) {
                header("Location: index.php?page=trabajadores&error=No puedes eliminarte a ti mismo por seguridad.");
                exit;
            }

            require_once 'config/conexion.php';
            $db = new Conexion();
            $conn = $db->getConexion();
            
            // Baja lógica: conservar perfil, credenciales, asistencias y documentos para consulta histórica.
            $stmt = $conn->prepare("UPDATE trabajadores SET estado = 'Inactivo', fecha_fin_contrato = CURDATE() WHERE id_trabajador = ?");
            if (!$stmt->execute([$id])) {
                header("Location: index.php?page=trabajadores&error=" . urlencode("No se pudo registrar el cese."));
                exit;
            }
            header("Location: index.php?page=trabajadores&success=1");
            exit;
        }
        header("Location: index.php?page=trabajadores");
        exit;
    }

    public function cesar() {
        if($_SERVER['REQUEST_METHOD'] == 'POST' && isset($_POST['id_trabajador'])) {
            // VALIDACIÓN: Seguridad - Verificar sesión y permisos
            if (!isset($_SESSION['usuario_id']) || !isset($_SESSION['rol']) || $_SESSION['rol'] !== 'Administrador') {
                header("Location: index.php?page=dashboard&error=No tienes permisos para realizar esta acción.");
                exit;
            }

            $id = $_POST['id_trabajador'];
            if (!is_numeric($id) || $id <= 0) {
                header("Location: index.php?page=trabajadores&error=ID inválido.");
                exit;
            }
            $motivo = $_POST['motivo_cese'];
            $fecha = $_POST['fecha_cese'];
            $obs = $_POST['observaciones_cese'];
            
            require_once 'models/Trabajador.php';
            $trabajadorModel = new Trabajador();
            
            require_once 'config/conexion.php';
            $db = new Conexion();
            $conn = $db->getConexion();
            
            // Actualizar estado e info en trabajadores
            $query = "UPDATE trabajadores SET estado = 'Inactivo', fecha_fin_contrato = ?, experiencia = CONCAT(COALESCE(experiencia, ''), '\n[CESE] Motivo: ', ?, ' - Fecha: ', ?, ' - Obs: ', ?) WHERE id_trabajador = ?";
            $conn->prepare($query)->execute([$fecha, $motivo, $fecha, $obs, $id]);
            
            header("Location: index.php?page=trabajadores&action=editar&id=" . $id . "&msg=Trabajador+Cesado&tipo=warning");
            exit;
        }
    }

    public function eliminar_acceso() {
        if($_SERVER['REQUEST_METHOD'] == 'POST' && isset($_POST['id_trabajador'])) {
            // VALIDACIÓN: Seguridad - Verificar sesión y permisos
            if (!isset($_SESSION['usuario_id']) || !isset($_SESSION['rol']) || $_SESSION['rol'] !== 'Administrador') {
                header("Location: index.php?page=dashboard&error=No tienes permisos para realizar esta acción.");
                exit;
            }

            $id = $_POST['id_trabajador'];
            if (!is_numeric($id) || $id <= 0) {
                header("Location: index.php?page=trabajadores&error=ID inválido.");
                exit;
            }
            
            require_once 'config/conexion.php';
            $db = new Conexion();
            $conn = $db->getConexion();
            
            // Revocar acceso mediante baja lógica. Se conserva la cuenta para auditoría histórica.
            $query_t = "UPDATE trabajadores SET estado = 'Inactivo' WHERE id_trabajador = ?";
            $conn->prepare($query_t)->execute([$id]);
            
            header("Location: index.php?page=trabajadores&msg=Acceso+Revocado&tipo=success");
            exit;
        }
    }

    public function renovar() {
        if(isset($_GET['id']) && isset($_POST['nueva_fecha'])) {
            // VALIDACIÓN: Seguridad - Verificar sesión y permisos
            if (!isset($_SESSION['usuario_id']) || !isset($_SESSION['rol']) || $_SESSION['rol'] !== 'Administrador') {
                header("Location: index.php?page=dashboard&error=No tienes permisos para realizar esta acción.");
                exit;
            }

            $id = $_GET['id'];
            if (!is_numeric($id) || $id <= 0) {
                header("Location: index.php?page=trabajadores&error=ID inválido.");
                exit;
            }

            $nueva_fecha = $_POST['nueva_fecha'];
            require_once 'models/Trabajador.php';
            $trabajadorModel = new Trabajador();
            $trabajadorModel->renovarContrato($id, $nueva_fecha);
            header("Location: " . $_SERVER['HTTP_REFERER']);
            exit;
        }
    }

    public function noRenovar() {
        if(isset($_GET['id'])) {
            $id = $_GET['id'];
            require_once 'config/conexion.php';
            $db = new Conexion();
            $conn = $db->getConexion();
            
            // Append [NO RENOVAR] to the experiencia field
            $query = "UPDATE trabajadores SET experiencia = CONCAT(COALESCE(experiencia, ''), '\n[NO RENOVAR]') WHERE id_trabajador = ?";
            $conn->prepare($query)->execute([$id]);
            
            header("Location: " . $_SERVER['HTTP_REFERER']);
            exit;
        }
    }



    public function informe() {

        if(isset($_GET['id'])) {
            $id = $_GET['id'];
            if (!isset($_SESSION['trabajador_id']) || ($_SESSION['trabajador_id'] != $id && (!isset($_SESSION['rol']) || (!in_array($_SESSION['rol'], ['Administrador', 'Gerencia']) && strpos($_SESSION['rol'], 'Jefe') === false)))) {
                die('Acceso denegado: No tienes permisos para ver la ficha de este trabajador.');
            }
            require_once 'models/Trabajador.php';
            $trabajadorModel = new Trabajador();
            $trabajador = $trabajadorModel->leerUno($id);

            if(!$trabajador) {
                echo "Trabajador no encontrado";
                exit;
            }

            // Obtener Horario Real según módulo de Horarios y Turnos (Temporada Invierno/Verano y Tipo)
            require_once 'models/HorarioModel.php';
            $hm = new HorarioModel();
            $modo_actual = $hm->getModoHorario();
            $is_verano = (trim(strtolower($modo_actual)) === 'verano');
            $temporadaNombre = $is_verano ? 'Temporada de Verano' : 'Temporada de Invierno';

            $es_practicante = (($trabajador['tipo'] ?? '') === 'Practicante');

            // Defaults del sistema según módulo Horarios y Turnos:
            if ($es_practicante) {
                $hora_inicio_default = $is_verano ? '09:30' : '08:00';
                $hora_fin_default = '13:30';
                $hora_inicio_tarde_default = '15:30';
                $hora_fin_tarde_default = '18:30';
                $asiste_tarde_default = 1;
            } else {
                $hora_inicio_default = $is_verano ? '09:30' : '08:30';
                $hora_fin_default = '13:30';
                $hora_inicio_tarde_default = '';
                $hora_fin_tarde_default = '';
                $asiste_tarde_default = 0;
            }

            // Consultar registros en horario_practicantes para este trabajador
            $horario_crudo = $hm->getHorarioByTrabajador($id);
            $dias_semana_orden = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
            $dias_laborables = [];
            $hora_manana_inicio = null;
            $hora_manana_fin = null;
            $tiene_tarde = false;
            $hora_tarde_inicio = null;
            $hora_tarde_fin = null;

            if (!empty($horario_crudo)) {
                foreach ($dias_semana_orden as $diaNom) {
                    foreach ($horario_crudo as $reg) {
                        $diaReg = str_replace('Miercoles', 'Miércoles', $reg['dia']);
                        if ($diaReg === $diaNom) {
                            $asiste_m = !empty($reg['asiste_manana']) || (!isset($reg['asiste_manana']) && !empty($reg['asiste']));
                            $asiste_t = !empty($reg['asiste_tarde']);
                            if ($asiste_m || $asiste_t) {
                                $dias_laborables[] = $diaNom;
                                if ($asiste_m) {
                                    if (!$hora_manana_inicio && !empty($reg['hora_inicio'])) $hora_manana_inicio = $reg['hora_inicio'];
                                    if (!$hora_manana_fin && !empty($reg['hora_fin'])) $hora_manana_fin = $reg['hora_fin'];
                                }
                                if ($asiste_t) {
                                    $tiene_tarde = true;
                                    if (!$hora_tarde_inicio && !empty($reg['hora_inicio_tarde'])) $hora_tarde_inicio = $reg['hora_inicio_tarde'];
                                    if (!$hora_tarde_fin && !empty($reg['hora_fin_tarde'])) $hora_tarde_fin = $reg['hora_fin_tarde'];
                                }
                            }
                            break;
                        }
                    }
                }
            }

            // Si no tiene registros personalizados, usar días laborables oficiales por defecto
            if (empty($dias_laborables)) {
                $dias_laborables = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'];
                $hora_manana_inicio = $hora_inicio_default;
                $hora_manana_fin = $hora_fin_default;
                if ($asiste_tarde_default) {
                    $tiene_tarde = true;
                    $hora_tarde_inicio = $hora_inicio_tarde_default;
                    $hora_tarde_fin = $hora_fin_tarde_default;
                }
            }

            // Formatear texto de días laborables
            if (count($dias_laborables) === 1) {
                $horarioDias = $dias_laborables[0];
            } elseif (count($dias_laborables) >= 2) {
                $horarioDias = $dias_laborables[0] . ' a ' . end($dias_laborables);
            } else {
                $horarioDias = "Lunes a Viernes";
            }

            // Formatear texto de horas
            $inicioFmt = $hora_manana_inicio ? $hora_manana_inicio : $hora_inicio_default;
            $finFmt = $hora_manana_fin ? $hora_manana_fin : $hora_fin_default;
            $horarioHoras = date("h:i a", strtotime($inicioFmt)) . ' – ' . date("h:i a", strtotime($finFmt));

            if ($tiene_tarde && $hora_tarde_inicio && $hora_tarde_fin) {
                $horarioHorasTarde = date("h:i a", strtotime($hora_tarde_inicio)) . ' – ' . date("h:i a", strtotime($hora_tarde_fin));
            } else {
                $horarioHorasTarde = null;
            }

            require_once 'views/trabajadores/ficha.php';
        } else {
            header("Location: index.php?page=trabajadores");
            exit;
        }
    }


    public function horarios() {
        require_once 'models/HorarioModel.php';
        $this->horarioModel = new HorarioModel();

        if (!isset($_SESSION['usuario_id']) || !isset($_SESSION['trabajador_id'])) {
            header("Location: index.php?page=login");
            exit();
        }

        $rol_usuario = $_SESSION['rol'] ?? '';
        $es_admin_o_jefe = ($rol_usuario === 'Administrador' || strpos($rol_usuario, 'Jefe') !== false);
        $es_admin = ($rol_usuario === 'Administrador');

        if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['modo_horario']) && $es_admin) {
            $modo = $_POST['modo_horario'] ?? 'Default';
            if ($this->horarioModel->setModoHorario($modo)) {
                echo json_encode(['success' => true, 'message' => 'Modo actualizado a ' . $modo]);
            } else {
                echo json_encode(['success' => false, 'message' => 'Error al actualizar el modo']);
            }
            exit();
        }

        $modo_actual = $this->horarioModel->getModoHorario();
        $is_verano = (trim(strtolower($modo_actual)) === 'verano');
        $id_trabajador_actual = $_SESSION['trabajador_id'];
        
        $tab_actual = $_GET['tab'] ?? 'trabajadores';
        $dias_semana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
        $trabajadores_list = [];
        $datos_admin = []; 

        if ($es_admin_o_jefe) {
            require_once 'models/Trabajador.php';
            $trabajadorModel = new Trabajador();
            
            if ($tab_actual === 'trabajadores') {
                $trabajadores_list = $trabajadorModel->getTrabajadoresRegulares();
            } else {
                $trabajadores_list = $trabajadorModel->getPracticantes();
            }
            
            // Remove the Jefe de Agencia from the list so they cannot modify their own schedule
            if (!$es_admin && $es_admin_o_jefe) {
                $trabajadores_list = array_filter($trabajadores_list, function($t) use ($id_trabajador_actual) {
                    return $t['id_trabajador'] != $id_trabajador_actual;
                });
                $trabajadores_list = array_values($trabajadores_list);
            }
            
            $id_seleccionado = isset($_GET['open_acc']) ? intval($_GET['open_acc']) : (count($trabajadores_list) > 0 ? $trabajadores_list[0]['id_trabajador'] : 0);
            
            $horarioTrabajador = null;
            if ($id_seleccionado > 0) {
                $infoTr = null;
                foreach($trabajadores_list as $t) {
                    if($t['id_trabajador'] == $id_seleccionado) { $infoTr = $t; break; }
                }

                // Si se solicitó un open_acc que pertenece a la otra pestaña (ej. Practicantes vs Colaboradores), cambiar automáticamente a su pestaña
                if (!$infoTr && isset($_GET['open_acc'])) {
                    $tab_alterno = ($tab_actual === 'trabajadores') ? 'practicantes' : 'trabajadores';
                    $lista_alterna = ($tab_alterno === 'trabajadores')
                        ? $trabajadorModel->getTrabajadoresRegulares()
                        : $trabajadorModel->getPracticantes();
                    if (!$es_admin && $es_admin_o_jefe) {
                        $lista_alterna = array_values(array_filter($lista_alterna, function($t) use ($id_trabajador_actual) {
                            return $t['id_trabajador'] != $id_trabajador_actual;
                        }));
                    }
                    foreach ($lista_alterna as $tAlt) {
                        if ($tAlt['id_trabajador'] == $id_seleccionado) {
                            $infoTr = $tAlt;
                            $tab_actual = $tab_alterno;
                            $trabajadores_list = $lista_alterna;
                            break;
                        }
                    }
                }
                
                if($infoTr) {
                    $es_practicante = ($infoTr['tipo'] === 'Practicante');
                    
                    // Defaults according to user rules
                    if ($es_practicante) {
                        $hora_inicio_default = '08:00';
                        $hora_fin_default = '13:30';
                        $hora_inicio_tarde = '15:30';
                        $hora_fin_tarde = '18:30';
                        $asiste_tarde_default = 1;
                        $total_horas = '30 horas';
                        $turno_nombre = 'Prácticas Estándar';
                    } else {
                        // Trabajadores
                        $hora_inicio_default = $is_verano ? '09:30' : '08:30';
                        $hora_fin_default = '13:30';
                        $hora_inicio_tarde = ''; // no tarde
                        $hora_fin_tarde = ''; // no tarde
                        $asiste_tarde_default = 0;
                        $total_horas = '25 horas';
                        $turno_nombre = 'Administrativo Estándar';
                    }

                    $horario_crudo = $this->horarioModel->getHorarioByTrabajador($id_seleccionado);
                    $horario_asociativo = [];
                    foreach ($dias_semana as $dia) {
                        $is_weekend = ($dia === 'Sábado' || $dia === 'Domingo');
                        $horario_asociativo[$dia] = [
                            'asiste' => $is_weekend ? 0 : 1, 
                            'asiste_manana' => $is_weekend ? 0 : 1, 
                            'asiste_tarde' => $is_weekend ? 0 : $asiste_tarde_default, 
                            'hora_inicio' => $hora_inicio_default, 
                            'hora_fin' => $hora_fin_default, 
                            'hora_inicio_tarde' => $hora_inicio_tarde, 
                            'hora_fin_tarde' => $hora_fin_tarde
                        ];
                    }
                    foreach ($horario_crudo as $h) {
                        if (isset($horario_asociativo[$h['dia']])) {
                            $horario_asociativo[$h['dia']] = [
                                'asiste' => $h['asiste'],
                                'asiste_manana' => isset($h['asiste_manana']) ? $h['asiste_manana'] : 1,
                                'asiste_tarde' => isset($h['asiste_tarde']) ? $h['asiste_tarde'] : $asiste_tarde_default,
                                'hora_inicio' => !empty($h['hora_inicio']) ? $h['hora_inicio'] : $hora_inicio_default,
                                'hora_fin' => !empty($h['hora_fin']) ? $h['hora_fin'] : $hora_fin_default,
                                'hora_inicio_tarde' => !empty($h['hora_inicio_tarde']) ? $h['hora_inicio_tarde'] : $hora_inicio_tarde,
                                'hora_fin_tarde' => !empty($h['hora_fin_tarde']) ? $h['hora_fin_tarde'] : $hora_fin_tarde
                            ];
                        }
                    }
                    
                    $minutos_totales = 0;
                    foreach ($horario_asociativo as $dia => $h) {
                        if ($h['asiste_manana'] && !empty($h['hora_inicio']) && !empty($h['hora_fin'])) {
                            $min = (strtotime($h['hora_fin']) - strtotime($h['hora_inicio'])) / 60;
                            if($min > 0) $minutos_totales += $min;
                        }
                        if ($h['asiste_tarde'] && !empty($h['hora_inicio_tarde']) && !empty($h['hora_fin_tarde'])) {
                            $min = (strtotime($h['hora_fin_tarde']) - strtotime($h['hora_inicio_tarde'])) / 60;
                            if($min > 0) $minutos_totales += $min;
                        }
                    }
                    $h_totales = floor($minutos_totales / 60);
                    $m_totales = $minutos_totales % 60;
                    $total_horas_calculadas = $h_totales . ' horas' . ($m_totales > 0 ? ' ' . $m_totales . ' min' : '');
                    
                    $horarioTrabajador = [
                        'tipo_trabajador' => $infoTr['tipo'],
                        'temporada_nombre' => $modo_actual,
                        'tolerancia_minutos' => 5,
                        'turno_nombre' => $turno_nombre,
                        'total_horas' => $total_horas_calculadas,
                        'estado_programacion' => 'Activo y Validado',
                        'dias_asistencia' => 'Lunes a Viernes',
                        'detalles' => []
                    ];
                    
                    foreach($dias_semana as $dia) {
                        $h = $horario_asociativo[$dia];
                        $horarioTrabajador['detalles'][] = [
                            'dia_semana' => $dia,
                            'es_laborable' => $h['asiste'],
                            'hora_entrada_fmt' => $h['asiste_manana'] ? $h['hora_inicio'] : ($h['asiste_tarde'] ? $h['hora_inicio_tarde'] : ''),
                            'hora_salida_fmt' => $h['asiste_tarde'] ? $h['hora_fin_tarde'] : ($h['asiste_manana'] ? $h['hora_fin'] : '')
                        ];
                    }
                }
            }
        } else {
            // No admin / jefe
            $id_seleccionado = $id_trabajador_actual;
            $infoTr = ['tipo' => $rol_usuario];
            $es_practicante = ($rol_usuario === 'Practicante');
            $tab_actual = $es_practicante ? 'practicantes' : 'trabajadores';
            
            if ($es_practicante) {
                $hora_inicio_default = '08:00';
                $hora_fin_default = '13:30';
                $hora_inicio_tarde = '15:30';
                $hora_fin_tarde = '18:30';
                $asiste_tarde_default = 1;
                $total_horas = '30 horas';
                $turno_nombre = 'Prácticas Estándar';
            } else {
                $hora_inicio_default = $is_verano ? '09:30' : '08:30';
                $hora_fin_default = '13:30';
                $hora_inicio_tarde = ''; 
                $hora_fin_tarde = ''; 
                $asiste_tarde_default = 0;
                $total_horas = '25 horas';
                $turno_nombre = 'Administrativo Estándar';
            }
            
            $horario_crudo = $this->horarioModel->getHorarioByTrabajador($id_trabajador_actual, date('Y-m-d'));
            
            $horario_asociativo = [];
            foreach ($dias_semana as $dia) {
                $is_weekend = ($dia === 'Sábado' || $dia === 'Domingo');
                $horario_asociativo[$dia] = [
                    'asiste' => $is_weekend ? 0 : 1, 
                    'asiste_manana' => $is_weekend ? 0 : 1, 
                    'asiste_tarde' => $is_weekend ? 0 : $asiste_tarde_default, 
                    'hora_inicio' => $hora_inicio_default, 
                    'hora_fin' => $hora_fin_default, 
                    'hora_inicio_tarde' => $hora_inicio_tarde, 
                    'hora_fin_tarde' => $hora_fin_tarde
                ];
            }
            foreach ($horario_crudo as $h) {
                if (isset($horario_asociativo[$h['dia']])) {
                    $horario_asociativo[$h['dia']] = [
                        'asiste' => $h['asiste'],
                        'asiste_manana' => isset($h['asiste_manana']) ? $h['asiste_manana'] : 1,
                        'asiste_tarde' => isset($h['asiste_tarde']) ? $h['asiste_tarde'] : $asiste_tarde_default,
                        'hora_inicio' => !empty($h['hora_inicio']) ? $h['hora_inicio'] : $hora_inicio_default,
                        'hora_fin' => !empty($h['hora_fin']) ? $h['hora_fin'] : $hora_fin_default,
                        'hora_inicio_tarde' => !empty($h['hora_inicio_tarde']) ? $h['hora_inicio_tarde'] : $hora_inicio_tarde,
                        'hora_fin_tarde' => !empty($h['hora_fin_tarde']) ? $h['hora_fin_tarde'] : $hora_fin_tarde
                    ];
                }
            }
            
            $minutos_totales = 0;
            foreach ($horario_asociativo as $dia => $h) {
                if ($h['asiste_manana'] && !empty($h['hora_inicio']) && !empty($h['hora_fin'])) {
                    $min = (strtotime($h['hora_fin']) - strtotime($h['hora_inicio'])) / 60;
                    if($min > 0) $minutos_totales += $min;
                }
                if ($h['asiste_tarde'] && !empty($h['hora_inicio_tarde']) && !empty($h['hora_fin_tarde'])) {
                    $min = (strtotime($h['hora_fin_tarde']) - strtotime($h['hora_inicio_tarde'])) / 60;
                    if($min > 0) $minutos_totales += $min;
                }
            }
            $h_totales = floor($minutos_totales / 60);
            $m_totales = $minutos_totales % 60;
            $total_horas_calculadas = $h_totales . ' horas' . ($m_totales > 0 ? ' ' . $m_totales . ' min' : '');
            
            $horarioTrabajador = [
                'tipo_trabajador' => $infoTr['tipo'],
                'temporada_nombre' => $modo_actual,
                'tolerancia_minutos' => 5,
                'turno_nombre' => $turno_nombre,
                'total_horas' => $total_horas_calculadas,
                'estado_programacion' => 'Activo y Validado',
                'dias_asistencia' => 'Lunes a Viernes',
                'detalles' => []
            ];
            
            foreach($dias_semana as $dia) {
                $h = $horario_asociativo[$dia];
                $horarioTrabajador['detalles'][] = [
                    'dia_semana' => $dia,
                    'es_laborable' => $h['asiste'],
                    'hora_entrada_fmt' => $h['asiste_manana'] ? $h['hora_inicio'] : ($h['asiste_tarde'] ? $h['hora_inicio_tarde'] : ''),
                    'hora_salida_fmt' => $h['asiste_tarde'] ? $h['hora_fin_tarde'] : ($h['asiste_manana'] ? $h['hora_fin'] : '')
                ];
            }
        }

        require_once 'views/trabajadores/horarios.php';
    }

    public function evaluacion() {
        // Si se pasa un ID directo, abrir el Dashboard Individual
        if (!empty($_GET['id']) || !empty($_GET['id_trabajador'])) {
            $this->evaluacion_detalle();
            return;
        }

        require_once 'models/Evaluacion.php';
        $evalModel = new Evaluacion();

        $filtroPeriodo = isset($_GET['periodo']) ? trim($_GET['periodo']) : 'hoy';
        $fecha_fin = date('Y-m-d');
        
        switch ($filtroPeriodo) {
            case 'hoy':
            default:
                $filtroPeriodo = 'hoy';
                $fecha_inicio = date('Y-m-d');
                $fecha_fin = date('Y-m-d');
                break;
            case 'esta_semana':
            case 'semana':
            case 'por_semana':
                $fecha_inicio = date('Y-m-d', strtotime('monday this week'));
                $fecha_fin = date('Y-m-d', strtotime('sunday this week'));
                break;
            case 'este_mes':
            case 'mes':
            case 'por_mes':
            case 'mes_actual':
                $fecha_inicio = date('Y-m-01');
                $fecha_fin = date('Y-m-t');
                break;
            case 'personalizado':
                $fecha_inicio = !empty($_GET['desde']) ? $_GET['desde'] : date('Y-m-d');
                $fecha_fin = !empty($_GET['hasta']) ? $_GET['hasta'] : date('Y-m-d');
                break;
            case 'semana_pasada':
                $fecha_inicio = date('Y-m-d', strtotime('monday last week'));
                $fecha_fin = date('Y-m-d', strtotime('sunday last week'));
                break;
            case 'mes_anterior':
            case 'mes_pasado':
                $fecha_inicio = date('Y-m-01', strtotime('first day of last month'));
                $fecha_fin = date('Y-m-t', strtotime('last day of last month'));
                break;
            case '7_dias':
            case '7':
                $fecha_inicio = date('Y-m-d', strtotime('-6 days'));
                break;
            case '30_dias':
            case '30':
                $fecha_inicio = date('Y-m-d', strtotime('-29 days'));
                break;
        }

        $filtros = [
            'area' => isset($_GET['area']) ? trim($_GET['area']) : 'Todos',
            'tipo' => isset($_GET['tipo_personal']) ? trim($_GET['tipo_personal']) : 'Todos',
            'busqueda' => isset($_GET['busqueda']) ? trim($_GET['busqueda']) : ''
        ];

        $resultado = $evalModel->evaluarTodos($fecha_inicio, $fecha_fin, $filtros);
        $kpi = $resultado['kpi'];
        $comparacion = $resultado['comparacion'];
        $lista = $resultado['lista'];
        $grafico_areas = $resultado['grafico_areas'];
        $grafico_evolucion = $resultado['grafico_evolucion'];
        $grafico_dona = $resultado['grafico_dona'];
        $acciones_rapidas = $resultado['acciones_rapidas'];
        $areas_disponibles = $resultado['areas_disponibles'];

        require_once 'views/trabajadores/evaluacion.php';
    }

    public function evaluacion_detalle() {
        require_once 'models/Evaluacion.php';
        $evalModel = new Evaluacion();

        $id_trabajador = !empty($_GET['id']) ? (int)$_GET['id'] : (!empty($_GET['id_trabajador']) ? (int)$_GET['id_trabajador'] : 0);
        if ($id_trabajador <= 0) {
            header("Location: index.php?page=trabajadores&action=evaluacion");
            exit;
        }

        $filtroPeriodo = isset($_GET['periodo']) ? trim($_GET['periodo']) : 'hoy';
        $fecha_fin = date('Y-m-d');

        switch ($filtroPeriodo) {
            case 'hoy':
            default:
                $filtroPeriodo = 'hoy';
                $fecha_inicio = date('Y-m-d');
                $fecha_fin = date('Y-m-d');
                break;
            case 'esta_semana':
            case 'semana':
            case 'por_semana':
                $fecha_inicio = date('Y-m-d', strtotime('monday this week'));
                $fecha_fin = date('Y-m-d', strtotime('sunday this week'));
                break;
            case 'este_mes':
            case 'mes':
            case 'por_mes':
            case 'mes_actual':
                $fecha_inicio = date('Y-m-01');
                $fecha_fin = date('Y-m-t');
                break;
            case 'personalizado':
                $fecha_inicio = !empty($_GET['desde']) ? $_GET['desde'] : date('Y-m-d');
                $fecha_fin = !empty($_GET['hasta']) ? $_GET['hasta'] : date('Y-m-d');
                break;
            case 'semana_pasada':
                $fecha_inicio = date('Y-m-d', strtotime('monday last week'));
                $fecha_fin = date('Y-m-d', strtotime('sunday last week'));
                break;
            case 'mes_anterior':
            case 'mes_pasado':
                $fecha_inicio = date('Y-m-01', strtotime('first day of last month'));
                $fecha_fin = date('Y-m-t', strtotime('last day of last month'));
                break;
            case '7_dias':
            case '7':
                $fecha_inicio = date('Y-m-d', strtotime('-6 days'));
                break;
            case '30_dias':
            case '30':
                $fecha_inicio = date('Y-m-d', strtotime('-29 days'));
                break;
        }

        try {
            $detalle = $evalModel->evaluarIndividual($id_trabajador, $fecha_inicio, $fecha_fin);
            if (!$detalle) {
                header("Location: index.php?page=trabajadores&action=evaluacion&error=Colaborador no encontrado");
                exit;
            }
            require_once 'views/trabajadores/evaluacion_detalle.php';
        } catch (\Throwable $e) {
            error_log("Error al cargar evaluacion_detalle: " . $e->getMessage());
            header("Location: index.php?page=trabajadores&action=evaluacion&error=Error al procesar evaluación individual");
            exit;
        }
    }

    public function guardar_observacion_evaluacion() {
        header('Content-Type: application/json');
        if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
            echo json_encode(['status' => 'error', 'msg' => 'Método no permitido']);
            exit;
        }

        $id_trabajador = isset($_POST['id_trabajador']) ? (int)$_POST['id_trabajador'] : 0;
        $observacion = isset($_POST['observacion']) ? trim($_POST['observacion']) : '';

        if ($id_trabajador <= 0 || empty($observacion)) {
            echo json_encode(['status' => 'error', 'msg' => 'Datos incompletos para guardar la observación.']);
            exit;
        }

        require_once 'models/Evaluacion.php';
        $evalModel = new Evaluacion();
        $ok = $evalModel->guardarObservacion($id_trabajador, $observacion);

        if ($ok) {
            echo json_encode([
                'status' => 'success',
                'msg' => 'Observación actualizada correctamente.',
                'fecha' => date('d M Y')
            ]);
        } else {
            echo json_encode(['status' => 'error', 'msg' => 'No se pudo guardar la observación en la base de datos.']);
        }
        exit;
    }

    public function imprimir_llamado() {
        error_reporting(E_ALL & ~E_DEPRECATED & ~E_USER_DEPRECATED);
        ini_set('display_errors', 0);

        if (!isset($_GET['id'])) {
            die("ID de llamado no proporcionado.");
        }
        $id_llamado = $_GET['id'];
        
        require_once 'models/LlamadoAtencion.php';
        $llamadoModel = new LlamadoAtencion();
        $llamado = $llamadoModel->obtenerPorId($id_llamado);
        
        if (!$llamado) {
            die("Documento disciplinario no encontrado.");
        }
        
        // Prevención IDOR
        if (isset($_SESSION['rol']) && !in_array($_SESSION['rol'], ['Administrador', 'Gerencia', 'Jefe'])) {
            if ($llamado['id_trabajador'] != $_SESSION['trabajador_id']) {
                die("Acceso denegado a este documento.");
            }
        }
        
        $nivel = $llamado['nivel'];
        $mensaje = $llamado['mensaje'];
        $nombrePracticante = $llamado['trab_nombres'] . ' ' . $llamado['trab_apellidos'];
        
        // --- GENERAR PDF ADJUNTO ---
        if (!class_exists('GeneradorPDF')) {
            require_once __DIR__ . '/../helpers/GeneradorPDF.php';
        }
        
        $tituloPDF = ($nivel >= 3) ? "AMONESTACIÓN Y EXPEDIENTE DISCIPLINARIO" : "NOTIFICACIÓN DISCIPLINARIA FORMAL";
        
        $pdf = new GeneradorPDF('P', 'mm', 'A4', $tituloPDF);
        $pdf->hide_header_details = true;
        $pdf->AddPage();
        
        // Título Principal
        $pdf->Ln(5);
        $pdf->SetFont('Arial', 'B', 14);
        $pdf->SetTextColor(30, 30, 30);
        $pdf->Cell(0, 8, utf8_decode($tituloPDF), 0, 1, 'C');
        $pdf->Ln(8);
        
        // Cuerpo Inicial
        $pdf->SetFont('Arial', '', 11);
        $pdf->SetTextColor(50, 50, 50);
        $pdf->MultiCell(0, 7, utf8_decode("Mediante el presente documento, la Gerencia de la empresa se dirige a usted para dejar constancia de la aplicación de medidas disciplinarias correspondientes al Nivel $nivel de incidencias de tardanza o faltas aplicadas, según el reglamento interno de asistencia."), 0, 'J');
        $pdf->Ln(5);
        
        // Resolución
        $pdf->SetFont('Arial', 'B', 11);
        $pdf->Cell(0, 7, utf8_decode("Resolución y Acción a Tomar:"), 0, 1, 'L');
        $pdf->SetFont('Arial', '', 11);
        $pdf->MultiCell(0, 7, utf8_decode("Estimado(a) $nombrePracticante,"), 0, 'L');
        $pdf->Ln(3);
        $pdf->MultiCell(0, 7, utf8_decode($mensaje), 0, 'J');
        $pdf->Ln(5);
        
        // Exhortación
        $pdf->MultiCell(0, 7, utf8_decode("Se exhorta al trabajador a cumplir estrictamente con los horarios establecidos para evitar futuras sanciones de mayor severidad contempladas en nuestro reglamento interno y legislación laboral vigente."), 0, 'J');
        
        // Tipo de Registro y Fecha
        $pdf->Ln(5);
        $pdf->SetFont('Arial', '', 11);
        $pdf->Cell(0, 7, utf8_decode("Tipo de Registro: Control mensual"), 0, 1, 'L');
        $pdf->Cell(0, 7, utf8_decode("Fecha de Emisión: " . date('d/m/Y', strtotime($llamado['fecha']))), 0, 1, 'L');
        
        // Firmas
        $cargoAdmin = !empty($llamado['cargo_reporte']) ? $llamado['cargo_reporte'] : 'Firma de Gerencia / RR.HH.';
        $firmaAdminStr = $llamado['gerente_firma'];
        $gerenteNombreStr = $llamado['gerente_nombres'] . ' ' . $llamado['gerente_apellidos'];
        
        $pdf->Ln(35);
        
        // Si existe la firma en los uploads
        if (!empty($firmaAdminStr) && file_exists(__DIR__ . '/../uploads/firmas/' . $firmaAdminStr)) {
            $y = $pdf->GetY() - 22;
            $pdf->Image(__DIR__ . '/../uploads/firmas/' . $firmaAdminStr, 40, $y, 40);
        }
        
        // Dos columnas para firmas
        $pdf->SetFont('Arial', 'B', 10);
        $pdf->Cell(95, 5, '___________________________________', 0, 0, 'C');
        $pdf->Cell(95, 5, '___________________________________', 0, 1, 'C');
        
        $pdf->Cell(95, 5, utf8_decode($gerenteNombreStr), 0, 0, 'C');
        $pdf->Cell(95, 5, utf8_decode($nombrePracticante), 0, 1, 'C');
        
        $pdf->SetFont('Arial', '', 9);
        $pdf->Cell(95, 5, utf8_decode($cargoAdmin), 0, 0, 'C');
        $pdf->Cell(95, 5, 'Firma del Trabajador', 0, 1, 'C');
        $pdf->Cell(95, 5, '(Emisor)', 0, 0, 'C');
        $pdf->Cell(95, 5, '(Recepcionado)', 0, 1, 'C');
        
        if (ob_get_length()) {
            ob_end_clean();
        }
        $pdf->Output('I', "Documento_Disciplinario_Nivel_$nivel.pdf");
        exit;
    }
    public function crear_cargo() {
        if(!isset($_SESSION['rol']) || !in_array($_SESSION['rol'], ['Administrador', 'Gerencia'])) {
            header("Location: index.php?page=trabajadores&error=Acceso Denegado");
            exit;
        }
        if($_SERVER['REQUEST_METHOD'] == 'POST' && !empty($_POST['nombre_cargo'])) {
            $sueldo_base = !empty($_POST['sueldo_base']) ? $_POST['sueldo_base'] : 0;
            require_once 'models/Trabajador.php';
            $trabajadorModel = new Trabajador();
            try {
                $trabajadorModel->crearCargo($_POST['nombre_cargo'], $sueldo_base);
                header("Location: index.php?page=trabajadores&success=1");
            } catch (Exception $e) {
                header("Location: index.php?page=trabajadores&error=" . urlencode("Error BD: " . $e->getMessage()));
            }
            exit;
        }
        header("Location: index.php?page=trabajadores");
        exit;
    }

    public function editar_cargo() {
        if(!isset($_SESSION['rol']) || !in_array($_SESSION['rol'], ['Administrador', 'Gerencia'])) {
            header("Location: index.php?page=trabajadores&error=Acceso Denegado");
            exit;
        }
        if($_SERVER['REQUEST_METHOD'] == 'POST' && isset($_POST['id_cargo']) && !empty($_POST['nombre_cargo'])) {
            $sueldo_base = !empty($_POST['sueldo_base']) ? $_POST['sueldo_base'] : 0;
            require_once 'models/Trabajador.php';
            $trabajadorModel = new Trabajador();
            try {
                $trabajadorModel->actualizarCargo($_POST['id_cargo'], $_POST['nombre_cargo'], $sueldo_base);
                header("Location: index.php?page=trabajadores&success=1");
            } catch (Exception $e) {
                header("Location: index.php?page=trabajadores&error=" . urlencode("Error BD: " . $e->getMessage()));
            }
            exit;
        }
        header("Location: index.php?page=trabajadores");
        exit;
    }

    public function eliminar_cargo() {
        if(!isset($_SESSION['rol']) || !in_array($_SESSION['rol'], ['Administrador', 'Gerencia'])) {
            header("Location: index.php?page=trabajadores&error=Acceso Denegado");
            exit;
        }
        if(isset($_GET['id'])) {
            require_once 'models/Trabajador.php';
            $trabajadorModel = new Trabajador();
            try {
                $trabajadorModel->eliminarCargo($_GET['id']);
                header("Location: index.php?page=trabajadores&success=1");
            } catch (Exception $e) {
                header("Location: index.php?page=trabajadores&error=" . urlencode("No se puede eliminar el cargo (posiblemente porque ya tiene trabajadores asignados). Detalle: " . $e->getMessage()));
            }
            exit;
        }
        header("Location: index.php?page=trabajadores");
        exit;
    }
}
?>
