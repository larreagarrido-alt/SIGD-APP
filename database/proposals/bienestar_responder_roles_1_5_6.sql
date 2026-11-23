-- Review with WEBSITE before production use. Grants answer-own-questionnaire only.
-- Does not grant bienestar.ver_privado, change individual exceptions or alter accounts.
INSERT INTO wsnet_websiteplus.wp_rol_permisos (id_rol,id_permiso,alcance)
SELECT r.id_rol,p.id_permiso,'PROPIO'
FROM (
  SELECT 1 AS id_rol,'Administrador' AS nombre
  UNION ALL SELECT 5,'Colaborador'
  UNION ALL SELECT 6,'Practicante'
) proposed
JOIN wsnet_bd_asistencia.roles r ON r.id_rol=proposed.id_rol AND r.nombre=proposed.nombre
JOIN wsnet_websiteplus.wp_permisos p ON p.codigo='bienestar.responder'
WHERE NOT EXISTS (
  SELECT 1 FROM wsnet_websiteplus.wp_rol_permisos current_grant
  WHERE current_grant.id_rol=r.id_rol AND current_grant.id_permiso=p.id_permiso
);

SELECT rp.id_rol,p.codigo,rp.alcance
FROM wsnet_websiteplus.wp_rol_permisos rp
JOIN wsnet_websiteplus.wp_permisos p ON p.id_permiso=rp.id_permiso
WHERE p.codigo='bienestar.responder' AND rp.id_rol IN (1,5,6)
ORDER BY rp.id_rol;
