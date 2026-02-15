insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_CONTEOS_CLOSE', 'INV', 'WORKFLOW_FINISH', 'Cerrar conteos ciclicos', b'1'
where not exists (select 1 from permisos where codigo = 'INV_CONTEOS_CLOSE');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'INV_CONTEOS_CLOSE'
where r.codigo = 'ROL_CONTADOR'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

delete rp
from roles_permisos rp
join roles r on r.id = rp.rol_id
join permisos p on p.id = rp.permiso_id
where p.codigo = 'INV_CONTEOS_CLOSE'
  and r.codigo <> 'ROL_CONTADOR';
