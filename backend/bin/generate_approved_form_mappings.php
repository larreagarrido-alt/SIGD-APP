<?php
declare(strict_types=1);

/** Emit SQL only after a human supplies approved, real cargo IDs. Never connects to MySQL. */
if (PHP_SAPI !== 'cli' || count($argv) !== 2) {
    fwrite(STDERR, "Usage: php generate_approved_form_mappings.php approved-mappings.json > approved-mappings.sql\n");
    exit(2);
}
$input = json_decode((string)file_get_contents($argv[1]), true, 512, JSON_THROW_ON_ERROR);
$types = ['VENTAS','DISENADOR','DISENADOR_JUNIOR','DISENADOR_SENIOR','GERENCIA',
    'PROGRAMACION','ADMINISTRACION','ASISTENCIA_ADMIN','CONTABILIDAD','COMMUNITY',
    'FOTOGRAFIA','TRAFFICKER','VIDEO'];
$approver = $input['approved_by_user_id'] ?? null;
$reference = $input['approval_reference'] ?? null;
$rows = $input['mappings'] ?? null;
if (($approver !== null && (!is_int($approver) || $approver < 1)) || !is_string($reference)
    || preg_match('/^[A-Za-z0-9_.-]{3,80}$/D', $reference) !== 1
    || !is_array($rows) || count($rows) < 1) {
    throw new RuntimeException('Approval reference and at least one real cargo mapping are required');
}
$seen = [];
foreach ($rows as $row) {
    $cargo = $row['id_cargo'] ?? null;
    $type = $row['tipo_formulario'] ?? null;
    if (!is_int($cargo) || $cargo < 1 || !is_string($type) || !in_array($type,$types,true))
        throw new RuntimeException('Invalid cargo or form type');
    $key = $cargo . ':' . $type;
    if (isset($seen[$key])) throw new RuntimeException('Repeated cargo/form mapping');
    $seen[$key] = true;
}
echo "-- Approval reference: $reference. Review IDs against the read-only preflight first.\n";
echo "-- aprobado_por is NULL when the business approver has no verified id_usuario.\n";
echo "-- Run only in authorized staging, then in an approved production window.\nSTART TRANSACTION;\n";
foreach ($rows as $row) {
    $cargo = $row['id_cargo'];
    $type = $row['tipo_formulario'];
    echo "INSERT INTO wp_kpi_formulario_cargos (id_cargo,tipo_formulario,aprobado_por,aprobado_en)\n";
    if ($approver === null) {
        echo " SELECT c.id_cargo,'$type',NULL,NOW() FROM cargos c\n";
        echo " WHERE c.id_cargo=$cargo\n";
    } else {
        echo " SELECT c.id_cargo,'$type',u.id_usuario,NOW() FROM cargos c CROSS JOIN usuarios u\n";
        echo " WHERE c.id_cargo=$cargo AND u.id_usuario=$approver\n";
    }
    echo " ON DUPLICATE KEY UPDATE aprobado_por=COALESCE(aprobado_por,VALUES(aprobado_por)), aprobado_en=COALESCE(aprobado_en,VALUES(aprobado_en));\n";
}
echo "COMMIT;\n";
echo "SELECT id_cargo,tipo_formulario,aprobado_en IS NOT NULL AS aprobado FROM wp_kpi_formulario_cargos ORDER BY id_cargo,tipo_formulario;\n";
