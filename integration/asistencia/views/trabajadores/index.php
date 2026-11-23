<?php require_once 'views/layout/head.php'; ?>
<script src="https://cdn.jsdelivr.net/npm/face-api.js@0.22.2/dist/face-api.min.js" integrity="sha384-gzn2n++arkvyhdNLmUf1s6F5NZ8iAbZ7FhIt+Zw7Jlf1n/vNTmZ3+cYr7S4ogyco" crossorigin="anonymous"></script>

<!-- MÓDULO 2: Navegación por Pestañas Internas (Sidebar intacto) -->
<div class="module-tabs">
    <a href="index.php?page=trabajadores" class="module-tab active"><i class='bx bx-list-ul'></i> General</a>
    <a href="index.php?page=trabajadores&action=horarios" class="module-tab"><i class='bx bx-time'></i> Horarios</a>
    <a href="index.php?page=trabajadores&action=evaluacion" class="module-tab"><i class='bx bx-line-chart'></i> Evaluación</a>
</div>

<div class="flex-between">
    <h2>Directorio de Colaboradores y Practicantes</h2>
    <?php if($_SESSION['rol'] == 'Administrador'): ?>
    <div>
        <button type="button" onclick="abrirModalCargo()" class="btn btn-outline-cargo" style="margin-right: 0.5rem; padding: 0.5rem 1rem; border-radius: 6px;">
            <i class='bx bx-briefcase-alt-2'></i> Nuevo Cargo
        </button>
        <button type="button" onclick="abrirModalTrabajador()" class="btn btn-primary" style="padding: 0.5rem 1rem; border-radius: 6px;">
            <i class='bx bx-user-plus'></i> Nuevo Trabajador
        </button>
    </div>
    <?php endif; ?>
</div>

<?php if(isset($_GET['success'])): ?>
    <div style="background: #D1FAE5; color: #065F46; padding: 1rem; border-radius: var(--radius-md); margin-bottom: 1.5rem; display: flex; align-items: center; gap: 0.5rem;">
        <i class='bx bx-check-circle'></i> Operación realizada con éxito.
    </div>
<?php endif; ?>
<?php if(isset($_GET['error'])): ?>
    <div style="background: #FEE2E2; color: #991B1B; padding: 1rem; border-radius: var(--radius-md); margin-bottom: 1.5rem; display: flex; align-items: center; gap: 0.5rem;">
        <i class='bx bx-x-circle'></i> <?php echo htmlspecialchars($_GET['error']); ?>
    </div>
<?php endif; ?>

<?php
// Extraer listas únicas para los filtros
$cargosUnicos = [];
$agenciasUnicas = [];
foreach($trabajadores as $t) {
    $cargo = $t['cargo_nombre'] ?? 'Sin Cargo';
    $agencia = $t['agencia_nombre'] ?? 'Sin Agencia';
    if(!in_array($cargo, $cargosUnicos)) $cargosUnicos[] = $cargo;
    if(!in_array($agencia, $agenciasUnicas)) $agenciasUnicas[] = $agencia;
}
sort($cargosUnicos);
sort($agenciasUnicas);
?>

<!-- Barra de Búsqueda y Filtros -->
<div class="filter-toolbar">
    <div class="filter-search-box">
        <i class='bx bx-search search-icon'></i>
        <input type="text" id="searchInput" placeholder="Buscar por DNI, nombre, código, cargo..." autocomplete="off">
        <button type="button" class="btn-clear-search" id="clearSearch" title="Limpiar búsqueda"><i class='bx bx-x'></i></button>
    </div>

    <div class="filter-select-group">
        <select class="filter-select" data-filter-col="cargo">
            <option value="all">Todos los Cargos</option>
            <?php foreach($cargosUnicos as $c): ?>
                <option value="<?php echo htmlspecialchars($c); ?>"><?php echo htmlspecialchars($c); ?></option>
            <?php endforeach; ?>
        </select>

        <select class="filter-select" data-filter-col="agencia">
            <option value="all">Todas las Agencias</option>
            <?php foreach($agenciasUnicas as $a): ?>
                <option value="<?php echo htmlspecialchars($a); ?>"><?php echo htmlspecialchars($a); ?></option>
            <?php endforeach; ?>
        </select>

        <select class="filter-select" data-filter-col="tipo">
            <option value="all">Todos los Tipos</option>
            <option value="empleado">Colaborador</option>
            <option value="practicante">Practicante</option>
        </select>

        <select class="filter-select" data-filter-col="estado">
            <option value="all">Todos los Estados</option>
            <option value="activo">Activo</option>
            <option value="inactivo">Inactivo</option>
            <option value="reserva">En Reserva</option>
        </select>
    </div>
</div>

<div class="filter-status-info">
    <div class="filter-badge-counter" id="filterCounter">
        <i class='bx bx-filter-alt'></i> Mostrando registros...
    </div>
    <div style="font-size: 0.75rem; opacity: 0.7;">Atajo: Presiona <b>/</b> para buscar</div>
</div>

<div class="card">
    <div class="table-container table-responsive">
        <table class="table-compact">
            <thead>
                <tr>
                    <th>Código</th>
                    <th>DNI</th>
                    <th>Nombres y Apellidos</th>
                    <th>Cargo</th>
                    <th>Agencia</th>
                    <th style="text-align: center;">Tipo</th>
                    <th>Asignación</th>
                    <th>Antigüedad</th>
                    <th style="text-align: center;">Estado</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach($trabajadores as $t):
                    $dias = (time() - strtotime($t['fecha_inicio'])) / (60 * 60 * 24);
                    $antiguedad_label = '';
                    if($dias < 90) {
                        $antiguedad_label = 'Nuevo (< 3 meses)';
                    } elseif($dias > 365) {
                        $anios = floor($dias / 365);
                        $antiguedad_label = $anios > 1 ? "Antiguo ($anios años)" : "Antiguo (1 año)";
                    } else {
                        $meses = max(1, floor($dias / 30));
                        $antiguedad_label = "$meses meses";
                    }

                    $fotoValida = (!empty($t['foto_perfil']) && file_exists($t['foto_perfil'])) ? $t['foto_perfil'] : '';

                    $workerData = [
                        'id' => $t['id_trabajador'],
                        'dni' => $t['dni'],
                        'codigo' => !empty($t['codigo_trabajador']) ? $t['codigo_trabajador'] : 'ID #' . $t['id_trabajador'],
                        'nombres' => $t['nombres'],
                        'apellidos' => $t['apellidos'],
                        'cargo' => $t['cargo_nombre'] ?? 'Sin Cargo',
                        'agencia' => $t['agencia_nombre'] ?? 'Sin Agencia',
                        'tipo' => $t['tipo'] ?? 'Colaborador',
                        'estado' => $t['estado'] ?? 'Activo',
                        'telefono' => $t['telefono'] ?? '',
                        'email' => $t['email'] ?? '',
                        'fecha_inicio' => $t['fecha_inicio'] ?? '',
                        'fecha_inicio_formato' => !empty($t['fecha_inicio']) ? date('d/m/Y', strtotime($t['fecha_inicio'])) : '',
                        'antiguedad_label' => $antiguedad_label,
                        'foto' => $fotoValida,
                        'userRol' => $_SESSION['rol'] ?? '',
                        'asis_hora_entrada' => $t['asis_hora_entrada'] ?? null,
                        'asis_hora_salida' => $t['asis_hora_salida'] ?? null,
                        'minutos_acumulados' => $t['minutos_acumulados'] ?? 0
                    ];
                    $jsonWorker = htmlspecialchars(json_encode($workerData, JSON_UNESCAPED_UNICODE), ENT_QUOTES, 'UTF-8');
                ?>
                <tr data-col-cargo="<?php echo htmlspecialchars($t['cargo_nombre'] ?? 'Sin Cargo'); ?>"
                    data-col-agencia="<?php echo htmlspecialchars($t['agencia_nombre'] ?? 'Sin Agencia'); ?>"
                    data-col-tipo="<?php echo htmlspecialchars($t['tipo'] ?? 'Colaborador'); ?>"
                    data-col-estado="<?php echo htmlspecialchars($t['estado'] ?? 'Activo'); ?>">
                    <td style="font-weight: 500; font-family: monospace; color: var(--primary);"><?php echo htmlspecialchars($t['codigo_trabajador'] ?? ''); ?></td>
                    <td style="font-weight: 500;"><?php echo htmlspecialchars($t['dni']); ?></td>
                    <td>
                        <div class="worker-name-container">
                            <div class="worker-avatar-trigger" data-worker='<?php echo $jsonWorker; ?>' title="Pase el cursor para ver credencial del trabajador">
                                <?php if(!empty($fotoValida)): ?>
                                    <img src="<?php echo htmlspecialchars($fotoValida); ?>" alt="Foto" class="avatar-img-thumb" style="width: 32px; height: 32px;">
                                <?php else: ?>
                                    <div class="avatar-initials-thumb" style="width: 32px; height: 32px; font-size: 0.85rem;">
                                        <?php echo strtoupper(substr($t['nombres'], 0, 1) . substr($t['apellidos'], 0, 1)); ?>
                                    </div>
                                <?php endif; ?>
                            </div>
                            <div>
                                <a href="index.php?page=trabajadores&action=perfil&id=<?php echo $t['id_trabajador']; ?>" class="worker-name-link">
                                    <?php echo htmlspecialchars($t['nombres'] . ' ' . $t['apellidos']); ?>
                                </a>
                            </div>
                        </div>
                    </td>
                    <td><?php echo htmlspecialchars($t['cargo_nombre']); ?></td>
                    <td><?php echo htmlspecialchars($t['agencia_nombre']); ?></td>
                    <td style="text-align: center; vertical-align: middle;">
                        <?php if($t['tipo'] == 'Practicante'): ?>
                            <span class="badge badge-outline-cyan">Practicante</span>
                        <?php else: ?>
                            <span class="badge badge-outline-purple">Colaborador</span>
                        <?php endif; ?>
                    </td>
                    <td style="font-size: 0.85rem; line-height: 1.2;">
                        <?php if($t['tipo'] == 'Practicante' && !empty($t['coordinador_nombre'])): ?>
                            <div style="color: var(--gray); font-size: 0.75rem;">Monitor:</div>
                            <div style="font-weight: 500;"><?php echo htmlspecialchars($t['coordinador_nombre']); ?></div>
                        <?php elseif($t['tipo'] == 'Colaborador' && !empty($t['supervisor_nombre'])): ?>
                            <div style="color: var(--gray); font-size: 0.75rem;">Supervisor:</div>
                            <div style="font-weight: 500;"><?php echo htmlspecialchars($t['supervisor_nombre']); ?></div>
                        <?php else: ?>
                            <span style="color: var(--gray); font-style: italic;">Sin asignar</span>
                        <?php endif; ?>
                    </td>
                    <td>
                        <div style="font-size: 0.85rem; color: var(--text-heading);">
                            <?php echo date('d/m/Y', strtotime($t['fecha_inicio'])); ?>
                        </div>
                        <?php if($dias < 90): ?>
                            <span class="badge badge-outline-orange" style="font-size: 0.7rem; padding: 0.1rem 0.4rem;">Nuevo</span>
                        <?php elseif($dias > 365): ?>
                            <span class="badge badge-outline-gray" style="font-size: 0.7rem; padding: 0.1rem 0.4rem;">Antiguo</span>
                        <?php endif; ?>
                    </td>
                    <td style="text-align: center; vertical-align: middle;">
                        <?php
                        if (!isset($trabajadorModelInstance)) {
                            require_once 'models/Trabajador.php';
                            $trabajadorModelInstance = new Trabajador();
                        }
                        $esCritico = $trabajadorModelInstance->tieneAlertaDisciplinariaCritica($t['id_trabajador']);
                        ?>
                        <div style="display: inline-flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;">
                            <?php if($t['estado'] == 'Activo'): ?>
                                <span class="badge badge-outline-green" style="font-size: 0.72rem; padding: 2px 8px; border-radius: 20px; line-height: 1;">Activo</span>
                            <?php elseif($t['estado'] == 'Reserva'): ?>
                                <span class="badge badge-outline-yellow" style="font-size: 0.72rem; padding: 2px 8px; border-radius: 20px; line-height: 1;">Reserva</span>
                            <?php else: ?>
                                <span class="badge badge-outline-red" style="font-size: 0.72rem; padding: 2px 8px; border-radius: 20px; line-height: 1;">Inactivo</span>
                            <?php endif; ?>

                            <?php if($esCritico): ?>
                                <span class="badge badge-outline-red" style="display: inline-flex; align-items: center; gap: 3px; font-size: 0.65rem; padding: 2px 6px; border-radius: 12px; white-space: nowrap; line-height: 1;" title="Superó el límite máximo de 5 tardanzas en el mes">
                                    <i class='bx bx-error-circle' style="font-size: 0.75rem;"></i> Límite 5 Tard.
                                </span>
                            <?php endif; ?>
                        </div>
                    </td>
                    <td>
                        <div style="display: flex; gap: 0.25rem; align-items: center; justify-content: flex-start;">
                            <a href="index.php?page=trabajadores&action=perfil&id=<?php echo $t['id_trabajador']; ?>" class="btn btn-outline-purple" style="padding: 0.25rem 0.5rem;">
                                <i class='bx bx-show'></i> Ver Perfil
                            </a>

                            <?php if($_SESSION['rol'] == 'Administrador' || $_SESSION['rol'] == 'Jefe de Agencia'): ?>
                            <a href="index.php?page=trabajadores&action=informe&id=<?php echo $t['id_trabajador']; ?>" class="btn btn-outline-green" style="padding: 0.25rem 0.5rem;">
                                <i class='bx bx-file'></i> Informe
                            </a>
                            <?php endif; ?>

                            <?php if($_SESSION['rol'] == 'Administrador'): ?>
                            <a href="index.php?page=trabajadores&action=editar&id=<?php echo $t['id_trabajador']; ?>" class="btn btn-outline-orange" style="padding: 0.25rem 0.5rem;">
                                <i class='bx bx-edit'></i> Editar
                            </a>
                            <?php endif; ?>
                        </div>
                    </td>
                </tr>
                <?php endforeach; ?>

                <?php if(empty($trabajadores)): ?>
                <tr class="empty-table-row">
                    <td colspan="10" style="text-align: center; color: var(--gray); padding: 2rem;">No hay trabajadores registrados en la base de datos.</td>
                </tr>
                <?php endif; ?>

                <!-- Fila cuando los filtros no coinciden con nada -->
                <tr id="no-results-row" class="no-results-row" style="display: none;">
                    <td colspan="10">
                        <div class="no-results-box">
                            <i class='bx bx-search-alt'></i>
                            <h4>No se encontraron resultados</h4>
                            <p>No hay trabajadores que coincidan con los filtros y la búsqueda actual.</p>
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</div>

<!-- Script del Hovercard inicializado para esta vista -->
<script src="assets/js/worker-hovercard.js?v=<?php echo time(); ?>"></script>
<script src="assets/js/table-filter.js?v=<?php echo time(); ?>"></script>
<script>
    if (typeof initWorkerHovercards === 'function') {
        initWorkerHovercards();
    }

    // Inicializar el filtro de tabla
    if (typeof initTableFilter === 'function') {
        initTableFilter({
            searchInput: '#searchInput',
            clearBtn: '#clearSearch',
            table: '.table-container table',
            counterEl: '#filterCounter',
            noResultsRow: '#no-results-row',
            filterSelects: '.filter-select'
        });
    }
</script>


<!-- Modal Cargo -->
<div class="modal-overlay" id="modalCargo" style="align-items: flex-start; padding-top: 5vh;">
    <div class="modal-container" style="max-width: 1100px; height: auto; max-height: 90vh;">
        <div class="modal-header">
            <h2 style="margin:0;"><i class='bx bx-briefcase'></i> Gestionar Cargos</h2>
            <button type="button" onclick="cerrarModalCargo()" style="background:none; border:none; font-size:1.5rem; cursor:pointer; color: var(--text-heading, inherit);"><i class='bx bx-x'></i></button>
        </div>
        <div class="modal-body" style="flex-direction: row; flex-wrap: wrap;">
            <!-- Columna Izquierda: Creación -->
            <div class="modal-col-left" style="flex: 1; min-width: 300px; border-right: 1px solid var(--border);">
                <h3 style="margin-top:0; margin-bottom:1rem; font-size:1.1rem;">Nuevo Cargo</h3>
                <form action="index.php?page=trabajadores&action=crear_cargo" method="POST">
    <input type="hidden" name="csrf_token" value="<?php echo CsrfHelper::generateToken(); ?>">

                    <div class="form-group">
                        <label class="form-label">Nombre del Cargo</label>
                        <input type="text" name="nombre_cargo" class="form-control" required placeholder="Ej. Analista de Datos">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Sueldo:</label>
                        <input type="number" step="0.01" name="sueldo_base" class="form-control" required placeholder="0.00">
                    </div>
                    <button type="submit" class="btn btn-primary" style="width: 100%;"><i class='bx bx-save'></i> Guardar Cargo</button>
                </form>
            </div>
            <!-- Columna Derecha: Listado -->
            <div class="modal-col-right" style="flex: 2; min-width: 400px; background: transparent;">
                <h3 style="margin-top:0; margin-bottom:1rem; font-size:1.1rem; color: var(--text-heading);">Cargos Existentes</h3>
                <div class="table-container" style="max-height: 50vh; overflow-y: auto;">
                    <table>
                        <thead>
                            <tr>
                                <th style="width:60px; text-align:center;">N°</th>
                                <th style="width: auto; min-width: 220px;">Cargo</th>
                                <th style="width: 150px;">Sueldo</th>
                                <th style="width:100px; text-align:center;">Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php foreach($cargos as $c): ?>
                            <tr>
                                <td style="text-align:center;">
                                    <span class="badge info"><?php echo htmlspecialchars($c['num_trabajadores'] ?? 0); ?></span>
                                </td>
                                <td>
                                    <form id="form_cargo_<?php echo $c['id_cargo']; ?>" action="index.php?page=trabajadores&action=editar_cargo" method="POST" style="display:none;">
                                        <input type="hidden" name="id_cargo" value="<?php echo $c['id_cargo']; ?>">
                                    </form>
                                    <input form="form_cargo_<?php echo $c['id_cargo']; ?>" type="text" name="nombre_cargo" value="<?php echo htmlspecialchars($c['nombre']); ?>" style="border:none; background:transparent; border-bottom:1px solid transparent; width:100%; min-width: 200px; outline:none; padding:0.25rem; font-family:inherit; font-size:inherit; font-weight:bold; color:var(--text-heading, inherit); transition:0.2s;" onfocus="this.style.borderBottom='1px solid var(--primary)'" onblur="this.style.borderBottom='1px solid transparent'" required>
                                </td>
                                <td>
                                    <div style="display:flex; align-items:center; width: 100%;">
                                        <span style="color:var(--text-muted); margin-right:4px;">S/</span>
                                        <input form="form_cargo_<?php echo $c['id_cargo']; ?>" type="number" step="0.01" name="sueldo_base" value="<?php echo htmlspecialchars($c['sueldo_base'] ?? '0.00'); ?>" style="width: 100%; min-width: 70px; border:none; background:transparent; border-bottom:1px solid transparent; outline:none; padding:0.25rem; font-family:inherit; font-size:inherit; font-weight:bold; color:var(--text-heading, inherit); transition:0.2s;" onfocus="this.style.borderBottom='1px solid var(--primary)'" onblur="this.style.borderBottom='1px solid transparent'" required>
                                    </div>
                                </td>
                                <td style="text-align:center; display:flex; gap:0.25rem; justify-content:center;">
                                    <button form="form_cargo_<?php echo $c['id_cargo']; ?>" type="submit" class="btn btn-light" style="padding:0.25rem 0.5rem; color:var(--primary);" title="Actualizar">
                                        ☑️
                                    </button>
                                    <a href="index.php?page=trabajadores&action=eliminar_cargo&id=<?php echo $c['id_cargo']; ?>" class="btn btn-light" style="padding:0.25rem 0.5rem; color:var(--danger);" onclick="return confirm('¿Está seguro de eliminar este cargo? Solo debe borrarse si no tiene trabajadores asignados.');" title="Eliminar">
                                        <i class='bx bx-trash'></i>
                                    </a>
                                </td>
                            </tr>
                            <?php endforeach; ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
function abrirModalCargo() {
    document.getElementById('modalCargo').classList.add('active');
}

function cerrarModalCargo() {
    document.getElementById('modalCargo').classList.remove('active');
}
</script>

<!-- Modal Trabajador -->
<style>
.modal-overlay {
    position: fixed; top: 0; left: 0; width: 100%; height: 100%;
    background: rgba(0,0,0,0.5); z-index: 1000;
    display: none; justify-content: center; align-items: center;
}
.modal-overlay.active { display: flex; }
.modal-container {
    background: var(--bg-card); width: 95%; max-width: 1200px;
    height: 90vh; border-radius: var(--radius-lg);
    box-shadow: 0 10px 25px rgba(0,0,0,0.2);
    display: flex; flex-direction: column; overflow: hidden;
}
.modal-header {
    padding: 1.5rem; border-bottom: 1px solid var(--border);
    display: flex; justify-content: space-between; align-items: center;
}
.modal-body {
    display: flex; flex: 1; overflow: hidden;
}
.modal-col-left {
    flex: 2; padding: 1.5rem; overflow-y: auto; border-right: 1px solid var(--border);
}
.modal-col-right {
    flex: 1; padding: 1.5rem; overflow-y: auto; background: #f8fafc;
}
.assignment-list {
    display: flex; flex-direction: column; gap: 0.5rem;
}
.assignment-item {
    background: white; border: 1px solid var(--border);
    padding: 0.75rem; border-radius: var(--radius-md);
    display: flex; align-items: center; gap: 0.75rem; cursor: pointer;
    transition: all 0.2s;
}
.assignment-item:hover { border-color: var(--primary); }
.assignment-item input[type="radio"], .assignment-item input[type="checkbox"] {
    width: 1.2rem; height: 1.2rem;
}
.assignment-item.selected {
    border-color: var(--primary); background: #f0f9ff;
}
</style>

<div class="modal-overlay" id="modalTrabajador" style="align-items: flex-start; padding-top: 5vh;">
    <div class="modal-container">
        <div class="modal-header">
            <h2 style="margin:0;"><i class='bx bx-user-plus'></i> Registrar Nuevo Trabajador</h2>
            <button type="button" class="btn-clear-search" onclick="cerrarModalTrabajador()" style="background:none; border:none; font-size:1.5rem; cursor:pointer; color: var(--text-heading, inherit);"><i class='bx bx-x'></i></button>
        </div>
        <?php
            // Calcular siguiente código
            require_once 'config/conexion.php';
            $db_code = new Conexion();
            $conn_code = $db_code->getConexion();
            $query_code = "SELECT MAX(CAST(SUBSTRING(codigo_trabajador, 4) AS UNSIGNED)) as last_num FROM trabajadores WHERE codigo_trabajador LIKE 'WS-%'";
            $row_code = $conn_code->query($query_code)->fetch(PDO::FETCH_ASSOC);
            $next_num = isset($row_code['last_num']) ? intval($row_code['last_num']) + 1 : 1;
            $codigo_auto = 'WS-' . str_pad($next_num, 4, '0', STR_PAD_LEFT);
        ?>
        <form action="index.php?page=trabajadores&action=crear" method="POST" enctype="multipart/form-data" style="display:flex; flex-direction:column; flex:1; overflow:hidden;" id="formCrearTrabajador">
    <input type="hidden" name="csrf_token" value="<?php echo CsrfHelper::generateToken(); ?>">

            <div class="modal-body">
                <!-- Formulario Principal Única Columna -->
                <div class="modal-col-left" style="flex: 1; border-right: none;">
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem;">
                        <div class="form-group">
                            <label class="form-label">DNI</label>
                            <input type="text" name="dni" class="form-control" required maxlength="15" pattern="[A-Za-z0-9.\-]{8,15}" title="Debe contener entre 8 y 15 caracteres (letras, números, puntos y guiones)" oninput="this.value = this.value.replace(/[^a-zA-Z0-9.\-]/g, '')">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Clave inicial para la cuenta</label>
                            <input type="password" name="password_inicial" class="form-control" required minlength="12" maxlength="128" autocomplete="new-password" placeholder="Mínimo 12 caracteres">
                            <small>Entrégala de forma privada. El trabajador deberá cambiarla al ingresar por primera vez en Asistencias.</small>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Código de Trabajador</label>
                            <input type="text" name="codigo_trabajador" class="form-control" value="<?php echo $codigo_auto; ?>">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Nombres</label>
                            <input type="text" name="nombres" class="form-control" required pattern="[a-zA-ZñÑáéíóúÁÉÍÓÚ\s]+" title="Solo se permiten letras" oninput="this.value = this.value.replace(/[^a-zA-ZñÑáéíóúÁÉÍÓÚ\s]/g, '')">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Apellidos</label>
                            <input type="text" name="apellidos" class="form-control" required pattern="[a-zA-ZñÑáéíóúÁÉÍÓÚ\s]+" title="Solo se permiten letras" oninput="this.value = this.value.replace(/[^a-zA-ZñÑáéíóúÁÉÍÓÚ\s]/g, '')">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Fecha de Nacimiento</label>
                            <input type="date" name="fecha_nacimiento" class="form-control" required max="<?php echo date('Y-m-d', strtotime('-15 years')); ?>">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Celular (Teléfono)</label>
                            <input type="text" name="telefono" class="form-control" maxlength="20" pattern="[0-9+\s\-]{9,20}" title="Debe contener entre 9 y 20 caracteres (números, +, espacios y guiones)" oninput="this.value = this.value.replace(/[^0-9+\s\-]/g, '')">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Email</label>
                            <input type="email" name="email" class="form-control">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Dirección</label>
                            <input type="text" name="direccion" class="form-control">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Departamento</label>
                            <input type="text" name="departamento" class="form-control">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Provincia</label>
                            <input type="text" name="provincia" class="form-control">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Distrito</label>
                            <input type="text" name="distrito" class="form-control">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Tipo de Contrato/Relación</label>
                            <select name="tipo" id="tipo_contrato_modal" class="form-control" required>
                                <option value="Colaborador">Colaborador</option>
                                <option value="Practicante">Practicante Pre/Pro</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Cargo</label>
                            <select name="id_cargo" id="cargo_modal" class="form-control" required>
                                <option value="">Seleccione un cargo...</option>
                                <?php foreach($cargos as $c): ?>
                                    <option value="<?php echo $c['id_cargo']; ?>" data-nombre="<?php echo htmlspecialchars($c['nombre']); ?>" data-sueldo="<?php echo htmlspecialchars($c['sueldo_base'] ?? 0); ?>">
                                        <?php echo htmlspecialchars($c['nombre']); ?>
                                    </option>
                                <?php endforeach; ?>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Agencia</label>
                            <select name="id_agencia" class="form-control" required>
                                <option value="">Seleccione una agencia...</option>
                                <?php foreach($agencias as $a): ?>
                                    <option value="<?php echo $a['id_agencia']; ?>"><?php echo $a['nombre']; ?></option>
                                <?php endforeach; ?>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Sueldo Base (S/)</label>
                            <input type="number" step="0.01" name="sueldo_base" id="sueldo_base_modal" class="form-control" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Fecha de Inicio Laboral</label>
                            <input type="date" name="fecha_inicio" class="form-control" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Fin de Contrato</label>
                            <input type="date" name="fecha_fin_contrato" id="fecha_fin_contrato_modal" class="form-control">
                        </div>

                        <div class="form-group" style="grid-column: span 2; border: 1px solid var(--border); padding: 1.5rem; border-radius: 8px; background: #F8FAFC;">
                            <label class="form-label" style="color: var(--primary); font-size: 1.1rem;"><i class='bx bx-camera'></i> Foto de Perfil / Evidencia</label>
                            <div style="margin-bottom: 1rem; border-bottom: 1px solid #ddd; padding-bottom: 1rem;">
                                <label style="font-weight: 500;">Opción 1: Subir Archivo</label>
                                <input type="file" name="foto_archivo" class="form-control" accept="image/*" style="margin-top: 0.5rem;">
                            </div>
                            <label style="font-weight: 500;">Opción 2: Capturar con Cámara</label>
                            <div style="display: flex; gap: 1rem; align-items: start; margin-top: 1rem;">
                                <div style="flex: 1;">
                                    <button type="button" id="btn-iniciar-camara" class="btn btn-primary" style="margin-bottom: 0.5rem;">
                                        <i class='bx bx-video'></i> Encender Cámara
                                    </button>
                                    <button type="button" id="btn-tomar-foto" class="btn btn-primary" style="display: none;">
                                        <i class='bx bx-camera'></i> Capturar Foto
                                    </button>
                                    <div id="rostro-status" style="margin-top: 1rem; font-size: 0.9rem; font-weight: 500;"></div>
                                    <input type="hidden" name="foto_base64" id="foto_base64">
                                    <input type="hidden" name="rostro_descriptor" id="rostro_descriptor">
                                </div>
                                <div style="width: 240px; height: 180px; background: #000; border-radius: 8px; overflow: hidden; position: relative;">
                                    <video id="video-registro" width="240" height="180" autoplay muted style="display:none; object-fit: cover;"></video>
                                    <canvas id="canvas-overlay" width="240" height="180" style="position: absolute; top: 0; left: 0; display:none;"></canvas>
                                    <img id="foto-preview" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; display: none;">
                                    <div id="camera-placeholder" style="position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; color: #333;">
                                        <i class='bx bx-camera-off' style="font-size: 3rem;"></i>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Inputs ocultos para enviar siempre id_coordinador o id_supervisor según corresponda -->
                        <input type="hidden" name="id_supervisor" id="hidden_id_supervisor" value="">
                        <input type="hidden" name="id_coordinador" id="hidden_id_coordinador" value="">
                    </div>
                </div>
            </div>
            <div style="padding: 1.5rem; border-top: 1px solid var(--border); display: flex; justify-content: flex-end; gap: 1rem;">
                <button type="button" class="btn btn-light" onclick="cerrarModalTrabajador()">Cancelar</button>
                <button type="submit" class="btn btn-primary"><i class='bx bx-save'></i> Guardar Trabajador</button>
            </div>
        </form>
    </div>
</div>

<script>
// Datos inyectados desde PHP para el modal
const datosSupervisores = <?php echo json_encode($supervisores); ?>;
const datosCoordinadores = <?php echo json_encode($coordinadores); ?>;
const datosEmpleadosDisponibles = <?php echo json_encode(array_values(array_filter($empleados_disponibles, function($e) { return empty($e['id_supervisor']); }))); ?>;
const datosPracticantesDisponibles = <?php echo json_encode(array_values(array_filter($practicantes_disponibles, function($p) { return empty($p['id_coordinador']); }))); ?>;

function abrirModalTrabajador() {
    document.getElementById('modalTrabajador').classList.add('active');
}

function cerrarModalTrabajador() {
    document.getElementById('modalTrabajador').classList.remove('active');
    if(stream) {
        stream.getTracks().forEach(track => track.stop());
    }
}

document.getElementById('tipo_contrato_modal').addEventListener('change', function() {
    if(this.value === 'Practicante') {
        document.getElementById('fecha_fin_contrato_modal').value = '2026-12-06';
    }
});

document.getElementById('cargo_modal').addEventListener('change', function() {
    const selectedOption = this.options[this.selectedIndex];
    if(selectedOption && selectedOption.value) {
        const sueldo = selectedOption.getAttribute('data-sueldo');
        if(sueldo !== null && sueldo !== "") {
            document.getElementById('sueldo_base_modal').value = sueldo;
        }
    }
});

// Integrar JS de la cámara (simplificado de crear.php)
const video = document.getElementById('video-registro');
const canvas = document.getElementById('canvas-overlay');
const imgPreview = document.getElementById('foto-preview');
const status = document.getElementById('rostro-status');
const btnCamara = document.getElementById('btn-iniciar-camara');
const btnTomar = document.getElementById('btn-tomar-foto');
const inputBase64 = document.getElementById('foto_base64');
const inputDescriptor = document.getElementById('rostro_descriptor');
const placeholder = document.getElementById('camera-placeholder');
let stream = null;

btnCamara.addEventListener('click', async () => {
    try {
        status.innerHTML = "Encendiendo cámara...";
        stream = await navigator.mediaDevices.getUserMedia({ video: true });
        video.srcObject = stream;
        video.style.display = 'block';
        placeholder.style.display = 'none';
        imgPreview.style.display = 'none';

        btnCamara.style.display = 'none';
        btnTomar.style.display = 'inline-block';
        status.innerHTML = "Cámara lista. Asegúrese de que el rostro sea visible y tome la foto.";
    } catch (error) {
        console.error(error);
        status.innerHTML = "<span style='color:red;'>Error al acceder a la cámara. Revise permisos.</span>";
    }
});

btnTomar.addEventListener('click', async () => {
    if (!stream) return;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    const fotoDataUrl = canvas.toDataURL('image/jpeg', 0.8);
    inputBase64.value = fotoDataUrl;
    imgPreview.src = fotoDataUrl;
    imgPreview.style.display = 'block';
    video.style.display = 'none';
    btnTomar.style.display = 'none';
    btnCamara.style.display = 'inline-block';
    btnCamara.innerHTML = "<i class='bx bx-refresh'></i> Volver a tomar foto";
    status.innerHTML = "<i class='bx bx-check-circle' style='color:#10b981;'></i> ¡Foto capturada!";
});
</script>

<?php require_once 'views/layout/footer.php'; ?>
