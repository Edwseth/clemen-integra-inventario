insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'ADMIN_RBAC_READ', 'ADMIN', 'READ', 'Lectura de administracion RBAC', b'1'
where not exists (select 1 from permisos where codigo = 'ADMIN_RBAC_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'ADMIN_RBAC_WRITE', 'ADMIN', 'WRITE', 'Escritura de administracion RBAC', b'1'
where not exists (select 1 from permisos where codigo = 'ADMIN_RBAC_WRITE');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in ('ADMIN_RBAC_READ', 'ADMIN_RBAC_WRITE')
where r.codigo = 'ROL_SUPER_ADMIN'
  and not exists (
    select 1
    from roles_permisos rp
    where rp.rol_id = r.id
      and rp.permiso_id = p.id
  );
