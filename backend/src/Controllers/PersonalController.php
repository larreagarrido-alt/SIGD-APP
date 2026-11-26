<?php
declare(strict_types=1);

namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\AuditService;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;
use WebSitePlus\Core\HttpException;

final class PersonalController
{
    /** Foto privada del trabajador, disponible solo para él o para Personal autorizado. */
    public function photo(Request $request, array $session, int $workerId): void
    {
        $own=(int)($session['id_trabajador']??0)===$workerId;
        if (!$own && !Authorization::allows($session,'empleados.ver',$workerId)) {
            Response::error('No autorizado para esta foto',403,'FORBIDDEN');
        }
        $worker=Database::getAttendanceConnection()->prepare(
            'SELECT 1 FROM trabajadores WHERE id_trabajador=? LIMIT 1');
        $worker->execute([$workerId]);
        if (!$worker->fetchColumn()) Response::error('Trabajador no encontrado',404,'NOT_FOUND');
        $path=dirname(__DIR__,2).'/storage/profile-photos/'.$workerId.'.jpg';
        $image=is_file($path) ? @getimagesize($path) : false;
        if (!is_file($path) || !is_readable($path) || filesize($path)>2097152
            || !is_array($image) || !in_array($image[2]??null,[IMAGETYPE_JPEG,IMAGETYPE_PNG],true)) {
            Response::error('Foto no disponible',404,'NOT_FOUND');
        }
        if ($request->getQuery('size')==='avatar' && function_exists('imagecreatefromstring')
            && function_exists('imagecreatetruecolor') && function_exists('imagecopyresampled')) {
            $source=@imagecreatefromstring((string)file_get_contents($path));
            if ($source!==false) {
                $width=imagesx($source); $height=imagesy($source);
                if ($width>0 && $height>0) {
                    $scale=min(1.0,192/max($width,$height));
                    $target=imagecreatetruecolor(max(1,(int)round($width*$scale)),max(1,(int)round($height*$scale)));
                    imagefill($target,0,0,imagecolorallocate($target,255,255,255));
                    imagecopyresampled($target,$source,0,0,0,0,imagesx($target),imagesy($target),$width,$height);
                    ob_start(); imagejpeg($target,null,78); $bytes=ob_get_clean();
                    imagedestroy($target); imagedestroy($source);
                    if (is_string($bytes) && $bytes!=='') {
                        header('Content-Type: image/jpeg');
                        header('Content-Length: '.strlen($bytes));
                        header('Cache-Control: private, max-age=300');
                        header('X-Content-Type-Options: nosniff');
                        echo $bytes;
                        return;
                    }
                } else imagedestroy($source);
            }
        }
        header('Content-Type: '.(($image[2]??null)===IMAGETYPE_PNG ? 'image/png' : 'image/jpeg'));
        header('Content-Length: '.filesize($path));
        header('Cache-Control: private, no-store');
        header('X-Content-Type-Options: nosniff');
        readfile($path);
    }

    /**
     * GET /api/v1/personal
     * Lista trabajadores con filtros (tipo, estado, cargo, busqueda) usando la vista optimizada.
     */
    public function index(Request $request, array $session): void
    {
        $scope=Authorization::permissions($session)['empleados.ver']??null;
        if(!in_array($scope,['TODOS','PROPIO','EQUIPO','AGENCIA'],true))Response::error('No autorizado para este recurso',403,'FORBIDDEN');
        $db = Database::getConnection();

        $tipo = $request->getQuery('tipo');         // 'Colaborador', 'Practicante'
        $estado = $request->getQuery('estado');     // Legacy callers may ask for Activo only.
        $cargoId = $request->getQuery('id_cargo');
        $search = $request->getQuery('q');
        $limit=$request->getQuery('limit','100');
        $offset=$request->getQuery('offset','0');
        if(($tipo!==null&&!in_array($tipo,['Practicante','Colaborador'],true))
            ||($estado!==null&&$estado!=='Activo')
            ||($cargoId!==null&&(!ctype_digit((string)$cargoId)||(int)$cargoId<1||(int)$cargoId>2147483647))
            ||($search!==null&&mb_strlen((string)$search)>150)
            ||!ctype_digit((string)$limit)||(int)$limit<1||(int)$limit>100
            ||!ctype_digit((string)$offset)||(int)$offset>100000){
            throw new HttpException('Filtros o paginacion invalidos',422,'VALIDATION_FAILED');
        }
        $limit=(int)$limit;$offset=(int)$offset;

        $sql = 'SELECT *, (SELECT t.foto_perfil FROM trabajadores t WHERE t.id_trabajador=vw_wp_empleados_estado.id_trabajador LIMIT 1) AS foto_perfil FROM vw_wp_empleados_estado WHERE estado_asistencia="Activo" AND situacion_websiteplus<>"DESVINCULADO"';
        $params = [];
        if($scope!=='TODOS'){
            $own=(int)($session['id_trabajador']??0);
            if($scope==='EQUIPO'&&$own>0){
                $sql.=' AND (id_trabajador=? OR id_trabajador IN (SELECT id_trabajador FROM trabajadores WHERE id_supervisor=?))';$params=[$own,$own];
            }elseif($scope==='AGENCIA'&&!empty($session['id_agencia'])){
                $sql.=' AND (id_trabajador=? OR id_agencia=?)';$params=[$own,(int)$session['id_agencia']];
            }else{$sql.=' AND id_trabajador=?';$params=[$own];}
        }

        if (!empty($tipo)) {
            $sql .= ' AND tipo_trabajador = ?';
            $params[] = $tipo;
        }
        if (!empty($cargoId)) {
            $sql .= ' AND id_cargo = ?';
            $params[] = (int) $cargoId;
        }
        if ($search!==null&&$search!=='') {
            $sql .= ' AND (nombre_completo LIKE ? OR dni LIKE ?)';
            $params[] = "%{$search}%";
            $params[] = "%{$search}%";
        }

        $sql .= ' ORDER BY nombre_completo ASC,id_trabajador ASC,id_usuario ASC LIMIT '.($limit+1).' OFFSET '.$offset;

        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        $workers = $stmt->fetchAll();
        $hasMore=count($workers)>$limit;
        $workers=array_slice($workers,0,$limit);
        // Observe only authorized, filtered workers returned by this request (at most 100).
        foreach(array_unique(array_column($workers,'id_trabajador')) as $id){
            \WebSitePlus\Employees\LaborObservationService::observe((int)$id);
        }

        Response::success([
            'total'     => count($workers),
            'empleados' => $workers,
            'paginacion'=> ['limit'=>$limit,'offset'=>$offset,'hay_mas'=>$hasMore],
        ]);
    }

    /**
     * GET /api/v1/personal/{id}
     * Detalle completo del empleado: datos actuales, periodos laborales e historial de transiciones.
     */
    public function show(Request $request, array $session, int $workerId): void
    {
        Authorization::require($session, 'empleados.ver', $workerId ?? null);
        $db = Database::getConnection();

        $stmt = $db->prepare('SELECT *,
            (SELECT t.foto_perfil FROM trabajadores t WHERE t.id_trabajador=vw_wp_empleados_estado.id_trabajador LIMIT 1) AS foto_perfil,
            (SELECT p.fecha_inicio FROM wp_periodos_laborales p
                WHERE p.id_trabajador=vw_wp_empleados_estado.id_trabajador AND p.fecha_fin IS NULL
                ORDER BY p.fecha_inicio DESC,p.id_periodo DESC LIMIT 1) AS fecha_inicio
            FROM vw_wp_empleados_estado WHERE id_trabajador = ? LIMIT 1');
        $stmt->execute([$workerId]);
        $worker = $stmt->fetch();

        if (!$worker) {
            Response::error('Trabajador no encontrado', 404, 'NOT_FOUND');
        }
        \WebSitePlus\Employees\LaborObservationService::observe($workerId);

        // Periodos laborales históricos
        $stPer = $db->prepare('
            SELECT p.*, c.nombre AS cargo_nombre, a.nombre AS agencia_nombre
            FROM wp_periodos_laborales p
            LEFT JOIN cargos c ON p.id_cargo = c.id_cargo
            LEFT JOIN agencias a ON p.id_agencia = a.id_agencia
            WHERE p.id_trabajador = ?
            ORDER BY p.fecha_inicio DESC,p.id_periodo DESC
        ');
        $stPer->execute([$workerId]);
        $periods = $stPer->fetchAll();

        // Eventos y transiciones
        $stEv = $db->prepare('
            SELECT e.*, u.username AS responsable_username
            FROM wp_eventos_laborales e
            LEFT JOIN usuarios u ON e.id_usuario_responsable = u.id_usuario
            WHERE e.id_trabajador = ?
            ORDER BY e.fecha_efectiva DESC, e.creado_en DESC,e.id_evento DESC
        ');
        $stEv->execute([$workerId]);
        $events = $stEv->fetchAll();
        foreach($events as &$event){
            // V002 requires a DATE. For source observations it is the observation day,
            // not evidence of the actual historical effective day. Preserve legacy fields.
            $observed=$event['origen']==='SINCRONIZACION';
            $event['fecha_efectiva_confirmada']=!$observed;
            $event['observado_en']=$observed?$event['creado_en']:null;
        }
        unset($event);

        // Permisos y vacaciones históricas en el sistema de asistencia
        $stPl = Database::getAttendanceConnection()->prepare('
            SELECT id_permiso_laboral, tipo_permiso, fecha_inicio, fecha_fin, motivo, estado
            FROM permisos_laborales
            WHERE id_trabajador = ?
            ORDER BY fecha_inicio DESC,id_permiso_laboral DESC
            LIMIT 20
        ');
        $stPl->execute([$workerId]);
        $leaves = $stPl->fetchAll();

        $month=date('Y-m');
        $monthStart=$month.'-01 00:00:00';
        $monthEnd=date('Y-m-d 00:00:00',strtotime($monthStart.' +1 month'));
        $indicators=$db->prepare('SELECT d.id_kpi,d.nombre,d.descripcion,d.periodicidad,d.meta_esperada,d.unidad_medida,
            (SELECT ROUND(SUM(e.porcentaje_cumplimiento*e.peso_snapshot)/NULLIF(SUM(e.peso_snapshot),0),2)
             FROM wp_kpi_evaluaciones e WHERE e.id_kpi=d.id_kpi AND e.id_trabajador=?
               AND e.estado="Aprobado" AND e.fecha_limite>=? AND e.fecha_limite<?
               AND e.peso_snapshot>0 AND e.porcentaje_cumplimiento IS NOT NULL) AS porcentaje_actual
            FROM wp_kpi_definiciones d WHERE d.id_cargo_objetivo=? AND d.activo=1
              AND d.regla_aprobada_en IS NOT NULL ORDER BY d.id_kpi');
        $indicators->execute([$workerId,$monthStart,$monthEnd,(int)$worker['id_cargo']]);
        $officialIndicators=$indicators->fetchAll(PDO::FETCH_ASSOC);
        $approved=$db->prepare('SELECT COUNT(*) AS evaluaciones_aprobadas,
            ROUND(SUM(e.porcentaje_cumplimiento*e.peso_snapshot)/NULLIF(SUM(e.peso_snapshot),0),2) AS porcentaje,
            COALESCE(SUM(e.puntos_otorgados),0) AS puntos
            FROM wp_kpi_evaluaciones e WHERE e.id_trabajador=? AND e.estado="Aprobado"
              AND e.fecha_limite>=? AND e.fecha_limite<? AND e.peso_snapshot>0
              AND e.porcentaje_cumplimiento IS NOT NULL AND e.puntos_otorgados IS NOT NULL
              AND EXISTS (SELECT 1 FROM wp_kpi_reporte_evaluaciones l
                  JOIN wp_kpi_formularios_detalle f ON f.id_detalle=l.id_detalle
                  WHERE l.id_evaluacion=e.id_evaluacion AND l.payload_sha256=f.payload_sha256)
              AND NOT EXISTS (SELECT 1 FROM wp_kpi_reporte_evaluaciones l
                  JOIN wp_kpi_formularios_detalle f ON f.id_detalle=l.id_detalle
                  WHERE l.id_evaluacion=e.id_evaluacion AND l.payload_sha256<>f.payload_sha256)');
        $approved->execute([$workerId,$monthStart,$monthEnd]);
        $result=$approved->fetch(PDO::FETCH_ASSOC);
        $received=$db->prepare('SELECT COUNT(*) FROM wp_kpi_formularios_detalle f
            WHERE f.id_trabajador=? AND f.actualizado_en>=? AND f.actualizado_en<?');
        $received->execute([$workerId,$monthStart,$monthEnd]);
        $receivedCount=(int)$received->fetchColumn();
        $awaiting=$db->prepare('SELECT COUNT(*) FROM wp_kpi_formularios_detalle f
            WHERE f.id_trabajador=? AND f.actualizado_en>=? AND f.actualizado_en<?
              AND NOT EXISTS (SELECT 1 FROM wp_kpi_reporte_evaluaciones l WHERE l.id_detalle=f.id_detalle)');
        $awaiting->execute([$workerId,$monthStart,$monthEnd]);
        $history=$db->prepare('SELECT DATE_FORMAT(e.fecha_limite,"%Y-%m") AS periodo,
            ROUND(SUM(e.porcentaje_cumplimiento*e.peso_snapshot)/NULLIF(SUM(e.peso_snapshot),0),2) AS porcentaje
            FROM wp_kpi_evaluaciones e WHERE e.id_trabajador=? AND e.estado="Aprobado"
              AND e.peso_snapshot>0 AND e.porcentaje_cumplimiento IS NOT NULL
              AND EXISTS (SELECT 1 FROM wp_kpi_reporte_evaluaciones l
                  JOIN wp_kpi_formularios_detalle f ON f.id_detalle=l.id_detalle
                  WHERE l.id_evaluacion=e.id_evaluacion AND l.payload_sha256=f.payload_sha256)
            GROUP BY DATE_FORMAT(e.fecha_limite,"%Y-%m") ORDER BY periodo DESC LIMIT 6');
        $history->execute([$workerId]);

        Response::success([
            'empleado'           => $worker,
            'periodos_laborales' => $periods,
            'eventos_transicion' => $events,
            'permisos_laborales' => $leaves,
            'kpi_resumen' => ['periodo'=>$month,'indicadores_asignados'=>count($officialIndicators),
                'formularios_recibidos'=>$receivedCount,
                'sin_revisar'=>(int)$awaiting->fetchColumn(),
                'evaluaciones_aprobadas'=>(int)($result['evaluaciones_aprobadas']??0),
                'porcentaje'=>$result['porcentaje']??null,'puntos'=>(int)($result['puntos']??0)],
            'kpi_indicadores'=>$officialIndicators,
            'kpi_historial'=>$history->fetchAll(PDO::FETCH_ASSOC),
        ]);
    }

    /**
     * POST /api/v1/personal/{id}/transicion
     * Registra DESVINCULACION, REINGRESO o una REACTIVACION ya observada en asistencia.
     * Solo para usuarios con permiso 'empleados.administrar'.
     */
    public function registrarTransicion(Request $request, array $session, int $workerId): void
    {
        Authorization::require($session, 'empleados.administrar', $workerId);
        Authorization::require($session, 'empleados.ver', $workerId);
        $body=$request->getBody();
        $event=$body['tipo_evento']??'';
        $reason=$body['motivo']??'';
        $date=array_key_exists('fecha_efectiva',$body)?$body['fecha_efectiva']:date('Y-m-d');
        if(!is_string($event)||!is_string($reason)||!is_string($date)){
            throw new HttpException('Evento, motivo y fecha deben ser cadenas de texto',422,'VALIDATION_FAILED');
        }
        $result=\WebSitePlus\Employees\LaborTransitionService::apply($workerId,$event,$reason,$date,(int)$session['id_usuario']);
        AuditService::log('empleados',$event,'EXITO',(int)$session['id_usuario'],$session['username'],$workerId,
            'wp_situacion_laboral',$workerId,['motivo'=>trim($reason),'id_evento'=>$result['id_evento']],$request->getIp());
        Response::success($result,"Transicion laboral registrada exitosamente: {$event}");
    }
}
