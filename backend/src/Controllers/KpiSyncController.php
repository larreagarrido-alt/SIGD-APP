<?php
declare(strict_types=1);

namespace WebSitePlus\Controllers;

use DateTimeImmutable;
use PDO;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Auth\AuditService;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

/** Stores raw Android captures. It never awards points or treats a client biometric flag as proof. */
final class KpiSyncController
{
    public function sync(Request $request, array $session): void
    {
        $workerId = (int) ($session['id_trabajador'] ?? 0);
        if ($workerId <= 0) Response::error('Trabajador no vinculado', 403, 'FORBIDDEN');
        Authorization::require($session, 'kpi.entregar', $workerId);
        $records = $request->getBody()['records'] ?? null;
        if (!is_array($records) || !array_is_list($records) || count($records) < 1 || count($records) > 50) {
            Response::error('Se requieren entre 1 y 50 registros', 422, 'VALIDATION_FAILED');
        }
        $normalized = [];
        foreach ($records as $record) {
            $row = self::normalize($record, $workerId);
            if (isset($normalized[$row['record_id']])) Response::error('ID duplicado en el lote', 422, 'VALIDATION_FAILED');
            $normalized[$row['record_id']] = $row;
        }

        $inserted = Database::transaction(function (PDO $db) use ($normalized, $workerId, $session): int {
            $existing = $db->prepare('SELECT payload_sha256 FROM wp_kpi_registros_android WHERE id_trabajador=? AND record_id=? FOR UPDATE');
            $assigned = $db->prepare('SELECT e.id_kpi, e.periodo_clave, e.meta_snapshot, e.peso_snapshot FROM wp_kpi_evaluaciones e JOIN wp_kpi_definiciones d ON d.id_kpi=e.id_kpi WHERE e.id_trabajador=? AND d.codigo=? AND e.periodo_clave IN (?,?,?) AND e.estado NOT IN ("Cancelado","No exigible") ORDER BY e.id_evaluacion DESC LIMIT 1');
            $insert = $db->prepare('INSERT INTO wp_kpi_registros_android (id_trabajador,id_usuario,record_id,id_kpi,periodo_clave,fecha,valor_real,meta_usada,peso_usado,payload_sha256) VALUES (?,?,?,?,?,?,?,?,?,?)');
            $count = 0;
            foreach ($normalized as $row) {
                $existing->execute([$workerId, $row['record_id']]);
                $previous = $existing->fetchColumn();
                if ($previous !== false) {
                    if (!hash_equals((string) $previous, $row['hash'])) {
                        throw new HttpException('El ID ya existe con otro contenido', 409, 'RECORD_CONFLICT');
                    }
                    continue;
                }
                $date = $row['date_object'];
                $keys = [$date->format('Y-m-d'), $date->format('o-\WW'), $date->format('Y-m')];
                $assigned->execute([$workerId, $row['kpi_code'], ...$keys]);
                $evaluation = $assigned->fetch();
                if (!$evaluation) {
                    throw new HttpException('KPI no asignado para la fecha del registro', 422, 'KPI_NOT_ASSIGNED');
                }
                $officialTarget = number_format((float) $evaluation['meta_snapshot'], 2, '.', '');
                $officialWeight = number_format((float) $evaluation['peso_snapshot'], 2, '.', '');
                $displayWeight = (float) $officialWeight > 0 && (float) $officialWeight <= 1
                    ? number_format((float) $officialWeight * 100, 2, '.', '') : $officialWeight;
                if ($row['target'] !== $officialTarget
                    || ($row['weight'] !== $officialWeight && $row['weight'] !== $displayWeight)) {
                    throw new HttpException('La meta o peso no coincide con la asignacion oficial', 422, 'KPI_SNAPSHOT_MISMATCH');
                }
                $insert->execute([$workerId, (int) $session['id_usuario'], $row['record_id'],
                    (int) $evaluation['id_kpi'], $evaluation['periodo_clave'], $row['date'],
                    $row['value'], $officialTarget, $officialWeight, $row['hash']]);
                $count++;
            }
            return $count;
        });
        AuditService::log('kpi', 'registros_android_sync', 'EXITO', (int) $session['id_usuario'],
            $session['username'] ?? null, $workerId, 'wp_kpi_registros_android', null,
            ['cantidad' => $inserted]);
        Response::success(['id_trabajador' => $workerId, 'accepted_ids' => array_keys($normalized),
            'inserted' => $inserted], 'Registros confirmados');
    }

    public function ownRecords(Request $request, array $session): void
    {
        $workerId = (int) ($session['id_trabajador'] ?? 0);
        if ($workerId <= 0) Response::error('Trabajador no vinculado', 403, 'FORBIDDEN');
        Authorization::require($session, 'kpi.ver', $workerId);
        $db = Database::getConnection();
        $query = $db->prepare('SELECT record_id, id_kpi, periodo_clave, fecha, valor_real, meta_usada, peso_usado, recibido_en FROM wp_kpi_registros_android WHERE id_trabajador=? ORDER BY recibido_en DESC LIMIT 100');
        $query->execute([$workerId]);
        Response::success(['id_trabajador' => $workerId, 'registros' => $query->fetchAll()]);
    }

    private static function normalize(mixed $record, int $workerId): array
    {
        if (!is_array($record)) Response::error('Registro inválido', 422, 'VALIDATION_FAILED');
        $id = $record['recordId'] ?? null;
        $code = $record['kpiId'] ?? null;
        $employee = (string) ($record['employeeId'] ?? '');
        if (!is_string($id) || preg_match('/^[A-Za-z0-9_-]{1,80}$/D', $id) !== 1
            || !is_string($code) || preg_match('/^[A-Za-z0-9_-]{1,50}$/D', $code) !== 1
            || $employee !== (string) $workerId) {
            Response::error('ID de registro, KPI o trabajador inválido', 422, 'VALIDATION_FAILED');
        }
        $rawDate = $record['date'] ?? null;
        $date = is_string($rawDate) ? DateTimeImmutable::createFromFormat('!Y-m-d', $rawDate) : false;
        if (!$date || $date->format('Y-m-d') !== $rawDate) {
            $date = is_string($rawDate) ? DateTimeImmutable::createFromFormat('!d/m/Y', $rawDate) : false;
            if (!$date || $date->format('d/m/Y') !== $rawDate) Response::error('Fecha inválida', 422, 'VALIDATION_FAILED');
        }
        $value = self::decimal($record['actualValue'] ?? null, 9999999999);
        $target = self::decimal($record['targetValueUsed'] ?? null, 9999999999);
        $weight = self::decimal($record['weightUsed'] ?? null, 100);
        $normalized = [$id, $code, $date->format('Y-m-d'), $value, $target, $weight];
        return ['record_id' => $id, 'kpi_code' => $code, 'date_object' => $date,
            'date' => $normalized[2], 'value' => $value, 'target' => $target, 'weight' => $weight,
            'hash' => hash('sha256', json_encode($normalized, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES))];
    }

    private static function decimal(mixed $value, float $max): string
    {
        if ((!is_int($value) && !is_float($value)) || !is_finite((float) $value)
            || $value < 0 || $value > $max) Response::error('Valor numérico inválido', 422, 'VALIDATION_FAILED');
        return number_format((float) $value, 2, '.', '');
    }
}
