-- Diagnóstico RBAC: inconsistencias entre usuarios.rol (legacy) y usuarios_roles (join)

-- 1) Usuarios cuyo rol en usuarios_roles no coincide con usuarios.rol
select u.id as usuario_id,
       u.username,
       u.rol as rol_usuario,
       r.codigo as rol_join
from usuarios u
join usuarios_roles ur on ur.usuario_id = u.id
join roles r on r.id = ur.rol_id
where r.codigo <> u.rol;

-- 2) Usuarios con más de 1 rol en usuarios_roles (estado inválido para el diseño actual)
select ur.usuario_id,
       u.username,
       count(distinct ur.rol_id) as cantidad_roles
from usuarios_roles ur
join usuarios u on u.id = ur.usuario_id
group by ur.usuario_id, u.username
having count(distinct ur.rol_id) > 1;
