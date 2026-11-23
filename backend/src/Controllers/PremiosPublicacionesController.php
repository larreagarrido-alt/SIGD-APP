<?php
declare(strict_types=1);
namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\AuditService;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

/** Publication catalog. It never awards, spends or converts KPI points. */
final class PremiosPublicacionesController
{
    private const FIELDS = 'p.id_premio,p.titulo,p.descripcion,p.id_imagen,p.inicio_en,p.fin_en,p.estado_publicacion,
        p.eliminado_en,p.creado_en,p.actualizado_en,
        GREATEST(0,TIMESTAMPDIFF(SECOND,NOW(),p.fin_en)) AS segundos_restantes,
        CASE WHEN p.eliminado_en IS NOT NULL THEN "ELIMINADO"
             WHEN p.estado_publicacion="BORRADOR" THEN "BORRADOR"
             WHEN p.estado_publicacion="DESACTIVADO" THEN "DESACTIVADO"
             WHEN p.fin_en IS NOT NULL AND p.fin_en<NOW() THEN "VENCIDO"
             WHEN p.inicio_en IS NOT NULL AND p.inicio_en>NOW() THEN "PROGRAMADO"
             ELSE "ACTIVO" END AS estado_efectivo';

    public function adminList(Request $request, array $session): void
    {
        Authorization::require($session, 'premios.administrar');
        $rows = Database::getConnection()->query('SELECT '.self::FIELDS.' FROM wp_premios p WHERE p.eliminado_en IS NULL ORDER BY p.actualizado_en DESC,p.id_premio DESC')->fetchAll(PDO::FETCH_ASSOC);
        foreach ($rows as &$row) $row['imagen_url'] = $row['id_imagen'] ? '/websiteplus-api/api/v1/premios/'.$row['id_premio'].'/imagen' : null;
        unset($row);
        Response::success(['premios'=>$rows,'total'=>count($rows)]);
    }

    public function catalog(Request $request, array $session): void
    {
        Authorization::require($session, 'premios.ver', (int)($session['id_trabajador'] ?? 0));
        $rows = Database::getConnection()->query('SELECT '.self::FIELDS.' FROM wp_premios p WHERE p.eliminado_en IS NULL AND p.estado_publicacion="PUBLICADO" AND (p.inicio_en IS NULL OR p.inicio_en<=NOW()) AND (p.fin_en IS NULL OR p.fin_en>=NOW()) ORDER BY p.inicio_en DESC,p.id_premio DESC')->fetchAll(PDO::FETCH_ASSOC);
        foreach ($rows as &$row) $row['imagen_url'] = $row['id_imagen'] ? '/websiteplus-api/api/v1/premios/'.$row['id_premio'].'/imagen' : null;
        unset($row);
        Response::success(['premios'=>$rows,'total'=>count($rows)]);
    }

    public function detail(Request $request, array $session, int $id): void
    {
        $admin = Authorization::allows($session, 'premios.administrar');
        if (!$admin) Authorization::require($session, 'premios.ver', (int)($session['id_trabajador'] ?? 0));
        $st=Database::getConnection()->prepare('SELECT '.self::FIELDS.' FROM wp_premios p WHERE p.id_premio=? AND p.eliminado_en IS NULL');
        $st->execute([$id]); $row=$st->fetch(PDO::FETCH_ASSOC);
        if (!$row || (!$admin && $row['estado_efectivo'] !== 'ACTIVO')) Response::error('Premio no disponible',404,'NOT_FOUND');
        $row['imagen_url']=$row['id_imagen'] ? '/websiteplus-api/api/v1/premios/'.$id.'/imagen' : null;
        Response::success($row);
    }

    public function save(Request $request, array $session, ?int $id = null): void
    {
        Authorization::require($session, 'premios.administrar');
        $title=trim((string)$request->input('titulo',''));
        $description=trim((string)$request->input('descripcion',''));
        $state=(string)$request->input('estado_publicacion','BORRADOR');
        $start=self::date($request->input('inicio_en'));
        $end=self::date($request->input('fin_en'));
        if (mb_strlen($title)<3 || mb_strlen($title)>150 || mb_strlen($description)>10000 || $description===''
            || !in_array($state,['BORRADOR','PUBLICADO','DESACTIVADO'],true)) Response::error('Datos del premio invalidos',422,'VALIDATION_FAILED');
        if (($start===null)!==($end===null) || ($start!==null && $end<=$start)) Response::error('Vigencia invalida',422,'VALIDATION_FAILED');
        if ($state==='PUBLICADO' && ($start===null || $end===null)) Response::error('Inicio y vencimiento requeridos para publicar',422,'VALIDATION_FAILED');
        if ($state==='PUBLICADO' && $id===null) Response::error('Guarda el borrador y su imagen antes de publicar',422,'IMAGE_REQUIRED');
        $db=Database::getConnection();
        $result=Database::transaction(function(PDO $pdo) use($id,$title,$description,$state,$start,$end,$session): int {
            if ($id===null) {
                $st=$pdo->prepare('INSERT INTO wp_premios (titulo,descripcion,puntos_requeridos,stock_disponible,stock_ilimitado,activo,estado_publicacion,inicio_en,fin_en,fecha_inicio,fecha_fin,creado_por) VALUES (?,?,0,0,1,0,?,?,?,?,?,?)');
                $st->execute([$title,$description,$state,$start,$end,$start?substr($start,0,10):null,$end?substr($end,0,10):null,$session['id_usuario']]);
                return (int)$pdo->lastInsertId();
            }
            $lock=$pdo->prepare('SELECT id_imagen,eliminado_en FROM wp_premios WHERE id_premio=? FOR UPDATE');
            $lock->execute([$id]); $existing=$lock->fetch(PDO::FETCH_ASSOC);
            if (!$existing || $existing['eliminado_en']!==null) Response::error('Premio no encontrado',404,'NOT_FOUND');
            if ($state==='PUBLICADO' && !$existing['id_imagen']) Response::error('Sube una imagen antes de publicar',422,'IMAGE_REQUIRED');
            $st=$pdo->prepare('UPDATE wp_premios SET titulo=?,descripcion=?,estado_publicacion=?,activo=0,inicio_en=?,fin_en=?,fecha_inicio=?,fecha_fin=? WHERE id_premio=?');
            $st->execute([$title,$description,$state,$start,$end,$start?substr($start,0,10):null,$end?substr($end,0,10):null,$id]);
            return $id;
        });
        AuditService::log('premios','publicacion_guardada','EXITO',(int)$session['id_usuario'],$session['username']??null,null,'wp_premios',$result,['estado'=>$state]);
        Response::success(['id_premio'=>$result], 'Premio guardado');
    }

    public function remove(Request $request, array $session, int $id): void
    {
        Authorization::require($session, 'premios.administrar');
        $db=Database::getConnection();
        $st=$db->prepare('UPDATE wp_premios SET eliminado_en=NOW(),estado_publicacion="DESACTIVADO",activo=0 WHERE id_premio=? AND eliminado_en IS NULL');
        $st->execute([$id]);
        if ($st->rowCount()!==1) Response::error('Premio no encontrado',404,'NOT_FOUND');
        AuditService::log('premios','publicacion_eliminada','EXITO',(int)$session['id_usuario'],$session['username']??null,null,'wp_premios',$id);
        Response::success(['id_premio'=>$id]);
    }

    public function upload(Request $request, array $session, int $id): void
    {
        Authorization::require($session, 'premios.administrar');
        $file=$_FILES['imagen']??null;
        $max=(int)Config::get('premios.max_image_bytes',5242880);
        if (!is_array($file) || ($file['error']??UPLOAD_ERR_NO_FILE)!==UPLOAD_ERR_OK || ($file['size']??0)<1 || $file['size']>$max || !is_uploaded_file($file['tmp_name']??''))
            Response::error('Imagen ausente, demasiado grande o subida incompleta',422,'INVALID_IMAGE');
        $mime=(new \finfo(FILEINFO_MIME_TYPE))->file($file['tmp_name']);
        $extensions=['image/jpeg'=>'jpg','image/png'=>'png','image/webp'=>'webp'];
        if (!isset($extensions[$mime])) Response::error('Formato de imagen no permitido',422,'INVALID_IMAGE');
        $dimensions=@getimagesize($file['tmp_name']);
        if (!$dimensions || $dimensions[0]<1 || $dimensions[1]<1 || $dimensions[0]>5000 || $dimensions[1]>5000 || $dimensions[0]*$dimensions[1]>16000000 || $dimensions['mime']!==$mime)
            Response::error('Dimensiones o contenido invalidos',422,'INVALID_IMAGE');
        $db=Database::getConnection();
        $check=$db->prepare('SELECT id_premio FROM wp_premios WHERE id_premio=? AND eliminado_en IS NULL');
        $check->execute([$id]); if (!$check->fetchColumn()) Response::error('Premio no encontrado',404,'NOT_FOUND');
        $uuid=sprintf('%s-%s-4%s-%s-%s',bin2hex(random_bytes(4)),bin2hex(random_bytes(2)),substr(bin2hex(random_bytes(2)),1),bin2hex(random_bytes(2)),bin2hex(random_bytes(6)));
        $relative='premios/'.$uuid.'.'.$extensions[$mime];
        $root=dirname(__DIR__,2).'/storage/premios';
        if (!is_dir($root) && !mkdir($root,0700,true) && !is_dir($root)) Response::error('No se pudo preparar el almacenamiento',500);
        $path=$root.'/'.$uuid.'.'.$extensions[$mime];
        if (!move_uploaded_file($file['tmp_name'],$path)) Response::error('No se pudo guardar la imagen',500);
        chmod($path,0600);
        try {
            $imageId=Database::transaction(function(PDO $pdo) use($id,$uuid,$relative,$file,$mime,$path,$session): int {
                $lock=$pdo->prepare('SELECT id_premio FROM wp_premios WHERE id_premio=? AND eliminado_en IS NULL FOR UPDATE');
                $lock->execute([$id]); if (!$lock->fetchColumn()) Response::error('Premio no encontrado',404,'NOT_FOUND');
                $st=$pdo->prepare('INSERT INTO wp_archivos (uuid,ruta_relativa,nombre_original,mime,tamano_bytes,sha256,subido_por) VALUES (?,?,?,?,?,?,?)');
                $st->execute([$uuid,$relative,basename((string)$file['name']),$mime,(int)$file['size'],hash_file('sha256',$path),$session['id_usuario']]);
                $imageId=(int)$pdo->lastInsertId();
                $pdo->prepare('UPDATE wp_premios SET id_imagen=? WHERE id_premio=?')->execute([$imageId,$id]);
                return $imageId;
            });
        } catch (\Throwable $error) { @unlink($path); throw $error; }
        AuditService::log('premios','imagen_subida','EXITO',(int)$session['id_usuario'],$session['username']??null,null,'wp_premios',$id,['id_archivo'=>$imageId]);
        Response::success(['id_premio'=>$id,'id_imagen'=>$imageId,'imagen_url'=>'/websiteplus-api/api/v1/premios/'.$id.'/imagen']);
    }

    public function image(Request $request, array $session, int $id): void
    {
        $admin=Authorization::allows($session,'premios.administrar');
        if (!$admin) Authorization::require($session,'premios.ver',(int)($session['id_trabajador']??0));
        $st=Database::getConnection()->prepare('SELECT a.ruta_relativa,a.mime,a.tamano_bytes,a.sha256, p.eliminado_en,
            CASE WHEN p.estado_publicacion="PUBLICADO" AND (p.inicio_en IS NULL OR p.inicio_en<=NOW()) AND (p.fin_en IS NULL OR p.fin_en>=NOW()) THEN 1 ELSE 0 END AS visible
            FROM wp_premios p JOIN wp_archivos a ON a.id_archivo=p.id_imagen AND a.eliminado_en IS NULL WHERE p.id_premio=?');
        $st->execute([$id]);$row=$st->fetch(PDO::FETCH_ASSOC);
        if (!$row || $row['eliminado_en']!==null || (!$admin && !(int)$row['visible'])) Response::error('Imagen no disponible',404,'NOT_FOUND');
        if (!preg_match('#^premios/[a-f0-9-]{36}\.(jpg|png|webp)$#D',(string)$row['ruta_relativa'])) Response::error('Imagen no disponible',404,'NOT_FOUND');
        $path=dirname(__DIR__,2).'/storage/'.$row['ruta_relativa'];
        if (!is_file($path) || filesize($path)!==(int)$row['tamano_bytes']) Response::error('Imagen no disponible',404,'NOT_FOUND');
        header('Content-Type: '.$row['mime']);header('Content-Length: '.$row['tamano_bytes']);
        header('Cache-Control: private, no-store');header('X-Content-Type-Options: nosniff');
        readfile($path);exit;
    }

    private static function date(mixed $value): ?string
    {
        if ($value===null || $value==='') return null;
        if (!is_string($value) || !preg_match('/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/D',$value)) Response::error('Fecha invalida',422,'VALIDATION_FAILED');
        $date=\DateTimeImmutable::createFromFormat('!Y-m-d H:i:s',$value,new \DateTimeZone('America/Lima'));
        if (!$date || $date->format('Y-m-d H:i:s')!==$value) Response::error('Fecha invalida',422,'VALIDATION_FAILED');
        return $value;
    }
}
