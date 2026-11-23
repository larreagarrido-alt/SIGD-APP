<?php
declare(strict_types=1);
namespace WebSitePlus\Core;
final class AttendanceViews
{
    public static function statements(string $source): array
    {
        if (!preg_match('/^[a-zA-Z0-9_]+$/D',$source)) { throw new \InvalidArgumentException('Invalid source schema'); }
        $columns = [
            'usuarios'=>'id_usuario,username,password,id_rol,id_trabajador,debe_cambiar_password',
            'trabajadores'=>'id_trabajador,dni,nombres,apellidos,email,estado,tipo,id_cargo,id_agencia,id_supervisor,foto_perfil,fecha_inicio,fecha_fin_contrato',
            'roles'=>'id_rol,nombre', 'cargos'=>'id_cargo,nombre', 'agencias'=>'id_agencia,nombre',
            'permisos_laborales'=>'id_permiso_laboral,id_trabajador,tipo_permiso,fecha_inicio,fecha_fin,motivo,estado',
        ];
        $sql=[];
        foreach($columns as $table=>$projection) { $sql[]="CREATE OR REPLACE SQL SECURITY INVOKER VIEW `$table` AS SELECT $projection FROM `$source`.`$table`"; }
        return $sql;
    }
}
