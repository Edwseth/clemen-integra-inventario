insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_CONTEOS_START', 'INV', 'WORKFLOW_START', 'Iniciar conteos ciclicos', b'1'
where not exists (select 1 from permisos where codigo = 'INV_CONTEOS_START');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_CONTEOS_APPLY', 'INV', 'WORKFLOW_FINISH', 'Aplicar conteos ciclicos', b'1'
where not exists (select 1 from permisos where codigo = 'INV_CONTEOS_APPLY');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'INV_CONTEOS_START'
where r.codigo in ('ROL_JEFE_ALMACENES', 'ROL_SUPER_ADMIN')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in ('INV_CONTEOS_APPLY', 'INV_CONTEOS_CLOSE')
where r.codigo in ('ROL_CONTADOR', 'ROL_SUPER_ADMIN')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

delete rp
from roles_permisos rp
join roles r on r.id = rp.rol_id
join permisos p on p.id = rp.permiso_id
where p.codigo in ('INV_CONTEOS_APPLY', 'INV_CONTEOS_CLOSE')
  and r.codigo not in ('ROL_CONTADOR', 'ROL_SUPER_ADMIN');

delete rp
from roles_permisos rp
join roles r on r.id = rp.rol_id
join permisos p on p.id = rp.permiso_id
where p.codigo = 'INV_CONTEOS_START'
  and r.codigo not in ('ROL_JEFE_ALMACENES', 'ROL_SUPER_ADMIN');
