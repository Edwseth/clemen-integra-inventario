insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_REPORTES_EXPORT', 'INV', 'EXPORT', 'Exportacion de reportes de inventario', b'1'
where not exists (select 1 from permisos where codigo = 'INV_REPORTES_EXPORT');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'INV_REPORTES_EXPORT'
)
where r.codigo = 'ROL_CONTADOR'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

delete rp
from roles_permisos rp
join roles r on r.id = rp.rol_id
join permisos p on p.id = rp.permiso_id
where r.codigo = 'ROL_CONTADOR'
  and p.codigo in (
    'PO_PLAN_SEMANAL_READ',
    'PO_PLAN_SEMANAL_WRITE',
    'PO_MRP_READ',
    'PO_MRP_WRITE',
    'PO_OC_CREATE',
    'PO_OC_EDIT',
    'PO_OC_WRITE'
  );
