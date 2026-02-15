insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_READ', 'INV', 'READ', 'Lectura general del modulo de inventario', b'1'
where not exists (select 1 from permisos where codigo = 'INV_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_WRITE', 'INV', 'WRITE', 'Escritura general del modulo de inventario', b'1'
where not exists (select 1 from permisos where codigo = 'INV_WRITE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_EXPORT', 'INV', 'EXPORT', 'Exportacion general del modulo de inventario', b'1'
where not exists (select 1 from permisos where codigo = 'INV_EXPORT');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_DECIDE', 'INV', 'DECIDE', 'Aprobacion y rechazo de solicitudes de inventario', b'1'
where not exists (select 1 from permisos where codigo = 'INV_DECIDE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_WORKFLOW_START', 'INV', 'WORKFLOW_START', 'Inicio de flujos operativos de inventario', b'1'
where not exists (select 1 from permisos where codigo = 'INV_WORKFLOW_START');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_WORKFLOW_FINISH', 'INV', 'WORKFLOW_FINISH', 'Cierre/aplicacion de flujos operativos de inventario', b'1'
where not exists (select 1 from permisos where codigo = 'INV_WORKFLOW_FINISH');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_WORKFLOW', 'INV', 'WORKFLOW', 'Gestion general de flujos operativos de inventario', b'1'
where not exists (select 1 from permisos where codigo = 'INV_WORKFLOW');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'INV_READ'
where r.codigo in ('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_CONTADOR', 'ROL_PLANEADOR', 'ROL_COMPRADOR')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'INV_WRITE'
where r.codigo in ('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_CONTADOR', 'ROL_COMPRADOR')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'INV_EXPORT'
where r.codigo in ('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_CONTADOR', 'ROL_PLANEADOR', 'ROL_COMPRADOR')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'INV_DECIDE'
where r.codigo in ('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_CONTADOR')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in ('INV_WORKFLOW_START', 'INV_WORKFLOW_FINISH', 'INV_WORKFLOW')
where r.codigo in ('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_CONTADOR')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'INV_READ',
    'INV_WRITE',
    'INV_EXPORT',
    'INV_DECIDE',
    'INV_WORKFLOW_START',
    'INV_WORKFLOW_FINISH',
    'INV_WORKFLOW'
)
where r.codigo = 'ROL_SUPER_ADMIN'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );
