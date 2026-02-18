insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PO_PLAN_SEMANAL_WRITE', 'PO', 'WRITE', 'Creación/edición de plan semanal', b'1'
where not exists (select 1 from permisos where codigo = 'PO_PLAN_SEMANAL_WRITE');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'PO_PLAN_SEMANAL_READ'
where r.codigo = 'ROL_JEFE_PRODUCCION'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'PO_PLAN_SEMANAL_WRITE'
where r.codigo in ('ROL_PLANEADOR', 'ROL_SUPER_ADMIN')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

delete rp
from roles_permisos rp
join roles r on r.id = rp.rol_id
join permisos p on p.id = rp.permiso_id
where r.codigo = 'ROL_JEFE_PRODUCCION'
  and p.codigo = 'PO_PLAN_SEMANAL_WRITE';
