<?php
declare(strict_types=1);

namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\AuditService;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

final class PremiosController
{
    /**
     * GET /api/v1/premios
     * Catálogo de premios activos con disponibilidad.
     */
    public function catalogo(Request $request, array $session): void
    {
        Authorization::require($session, 'premios.ver', (int) $session['id_trabajador']);
        $db = Database::getConnection();

        $stmt = $db->query('
            SELECT p.*, a.uuid AS imagen_uuid, a.nombre_original AS imagen_nombre
            FROM wp_premios p
            LEFT JOIN wp_archivos a ON p.id_imagen = a.id_archivo
            WHERE p.activo = 1
              AND (p.stock_ilimitado = 1 OR p.stock_disponible > 0)
              AND (p.fecha_inicio IS NULL OR p.fecha_inicio <= CURRENT_DATE())
              AND (p.fecha_fin IS NULL OR p.fecha_fin >= CURRENT_DATE())
            ORDER BY p.puntos_requeridos ASC
        ');
        $premios = $stmt->fetchAll();

        Response::success([
            'total'   => count($premios),
            'premios' => $premios,
        ]);
    }

    /**
     * GET /api/v1/premios/mis-puntos
     * Saldo actual del trabajador e historial del libro mayor (ledger).
     */
    public function misPuntos(Request $request, array $session): void
    {
        Authorization::require($session, 'premios.ver', (int) $session['id_trabajador']);
        $workerId = $session['id_trabajador'];
        if (!$workerId) {
            Response::error('Usuario sin trabajador vinculado', 403, 'FORBIDDEN');
        }

        $db = Database::getConnection();

        // Saldo actual
        $stSal = $db->prepare('SELECT saldo_posterior FROM wp_puntos_ledger WHERE id_trabajador = ? ORDER BY id_movimiento DESC LIMIT 1');
        $stSal->execute([$workerId]);
        $saldoActual = (int) ($stSal->fetchColumn() ?: 0);

        // Movimientos
        $stMov = $db->prepare('
            SELECT id_movimiento, tipo_movimiento, puntos, saldo_anterior, saldo_posterior,
                   motivo, creado_en
            FROM wp_puntos_ledger
            WHERE id_trabajador = ?
            ORDER BY id_movimiento DESC
            LIMIT 50
        ');
        $stMov->execute([$workerId]);
        $movimientos = $stMov->fetchAll();

        // Canjes solicitados
        $stCanjes = $db->prepare('
            SELECT c.*, p.titulo AS premio_titulo
            FROM wp_canjes c
            JOIN wp_premios p ON c.id_premio = p.id_premio
            WHERE c.id_trabajador = ?
            ORDER BY c.fecha_solicitud DESC
        ');
        $stCanjes->execute([$workerId]);
        $canjes = $stCanjes->fetchAll();

        Response::success([
            'id_trabajador' => (int) $workerId,
            'saldo_actual'  => $saldoActual,
            'movimientos'   => $movimientos,
            'mis_canjes'    => $canjes,
        ]);
    }

    /**
     * POST /api/v1/premios/canjear
     * Canje atómico de premio con validación de saldo y stock.
     */
    public function solicitarCanje(Request $request, array $session): void
    {
        $workerId = (int) $session['id_trabajador'];
        $premioId = (int) $request->input('id_premio', 0);

        if ($workerId <= 0 || $premioId <= 0) {
            Response::error('id_premio es requerido', 422, 'VALIDATION_FAILED');
        }

        Authorization::require($session, 'premios.canjear', $workerId);
        $requestId = (string) $request->input('solicitud_id', '');
        if (!preg_match('/^[a-zA-Z0-9-]{16,64}$/D', $requestId)) { Response::error('solicitud_id estable de 16 a 64 caracteres requerido', 422); }
        $codigoCanje = 'CNJ-' . strtoupper(bin2hex(random_bytes(4)));

        $resultado = Database::transaction(function (PDO $pdo) use ($workerId, $premioId, $codigoCanje, $session, $requestId) {
            \WebSitePlus\Auth\PointsLock::acquire($pdo, $workerId);
            $existing=$pdo->prepare('SELECT id_canje, codigo_canje, id_premio FROM wp_canjes WHERE id_trabajador=? AND solicitud_id=?');
            $existing->execute([$workerId,$requestId]);
            if ($row=$existing->fetch()) {
                if ((int)$row['id_premio'] !== $premioId) { return ['ok'=>false,'error'=>'solicitud_id ya usado para otro premio']; }
                return ['ok'=>true,'canje_id'=>(int)$row['id_canje'],'codigo_canje'=>$row['codigo_canje'],'premio_titulo'=>'Canje existente'];
            }
            // 1. Bloquear y consultar premio
            $stPrem = $pdo->prepare('SELECT * FROM wp_premios WHERE id_premio = ? AND activo = 1 AND (fecha_inicio IS NULL OR fecha_inicio <= CURRENT_DATE()) AND (fecha_fin IS NULL OR fecha_fin >= CURRENT_DATE()) FOR UPDATE');
            $stPrem->execute([$premioId]);
            $premio = $stPrem->fetch();

            if (!$premio) {
                return ['ok' => false, 'error' => 'Premio no disponible o inactivo'];
            }
            if (!$premio['stock_ilimitado'] && (int) $premio['stock_disponible'] <= 0) {
                return ['ok' => false, 'error' => 'No hay stock disponible para este premio'];
            }

            if ((int) $premio['puntos_requeridos'] <= 0) { throw new \RuntimeException('Costo invalido'); }
            $puntosReq = (int) $premio['puntos_requeridos'];

            // 2. Bloquear y consultar saldo en wp_puntos_ledger
            $stSal = $pdo->prepare('SELECT saldo_posterior FROM wp_puntos_ledger WHERE id_trabajador = ? ORDER BY id_movimiento DESC LIMIT 1 FOR UPDATE');
            $stSal->execute([$workerId]);
            $saldoAnt = (int) ($stSal->fetchColumn() ?: 0);

            if ($saldoAnt < $puntosReq) {
                return ['ok' => false, 'error' => "Puntos insuficientes. Tienes {$saldoAnt} y necesitas {$puntosReq}."];
            }

            $saldoPost = $saldoAnt - $puntosReq;

            // 3. Descontar stock
            if (!$premio['stock_ilimitado']) {
                $updStock = $pdo->prepare('UPDATE wp_premios SET stock_disponible = stock_disponible - 1 WHERE id_premio = ?');
                $updStock->execute([$premioId]);
            }

            // 4. Registrar movimiento en ledger
            $stLed = $pdo->prepare('
                INSERT INTO wp_puntos_ledger (
                    id_trabajador, tipo_movimiento, puntos, saldo_anterior, saldo_posterior,
                    referencia_tipo, motivo, creado_en
                ) VALUES (?, "CANJE_PREMIO", ?, ?, ?, "canje_premio", ?, NOW())
            ');
            $stLed->execute([
                $workerId,
                -$puntosReq,
                $saldoAnt,
                $saldoPost,
                "Canje de {$premio['titulo']}",
            ]);
            $movId = (int) $pdo->lastInsertId();

            // 5. Registrar canje
            $stCanje = $pdo->prepare('
                INSERT INTO wp_canjes (
                    codigo_canje, id_trabajador, id_premio, puntos_gastados, id_movimiento,
                    estado, fecha_solicitud
                ) VALUES (?, ?, ?, ?, ?, "SOLICITADO", NOW())
            ');
            $stCanje->execute([
                $codigoCanje,
                $workerId,
                $premioId,
                $puntosReq,
                $movId,
            ]);
            $canjeId = (int) $pdo->lastInsertId();
            $saveRequest=$pdo->prepare('UPDATE wp_canjes SET solicitud_id=? WHERE id_canje=?');
            $saveRequest->execute([$requestId,$canjeId]);

            return [
                'ok'            => true,
                'canje_id'      => $canjeId,
                'codigo_canje'  => $codigoCanje,
                'nuevo_saldo'   => $saldoPost,
                'premio_titulo' => $premio['titulo'],
            ];
        });

        if (!$resultado['ok']) {
            Response::error($resultado['error'], 400, 'REDEMPTION_FAILED');
        }

        AuditService::log('premios', 'canje_solicitado', 'EXITO', (int) $session['id_usuario'], $session['username'], $workerId, 'wp_canjes', $resultado['canje_id'], ['premio' => $resultado['premio_titulo']]);

        Response::success($resultado, 'Canje solicitado exitosamente. Muestra este codigo en Administracion.');
    }
}
