insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PO_READ', 'PO', 'READ', 'Lectura general del modulo de planeacion', b'1'
where not exists (select 1 from permisos where codigo = 'PO_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PO_WRITE', 'PO', 'WRITE', 'Escritura general del modulo de planeacion', b'1'
where not exists (select 1 from permisos where codigo = 'PO_WRITE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PO_EXPORT', 'PO', 'EXPORT', 'Exportacion general del modulo de planeacion', b'1'
where not exists (select 1 from permisos where codigo = 'PO_EXPORT');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PO_DECIDE', 'PO', 'DECIDE', 'Decisiones de aprobacion/cierre en planeacion', b'1'
where not exists (select 1 from permisos where codigo = 'PO_DECIDE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PO_WORKFLOW', 'PO', 'WORKFLOW', 'Gestion general de flujos operativos de planeacion', b'1'
where not exists (select 1 from permisos where codigo = 'PO_WORKFLOW');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PO_WORKFLOW_START', 'PO', 'WORKFLOW_START', 'Inicio de flujos operativos de planeacion', b'1'
where not exists (select 1 from permisos where codigo = 'PO_WORKFLOW_START');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PO_WORKFLOW_FINISH', 'PO', 'WORKFLOW_FINISH', 'Cierre de flujos operativos de planeacion', b'1'
where not exists (select 1 from permisos where codigo = 'PO_WORKFLOW_FINISH');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'PO_READ'
where r.codigo in ('ROL_PLANEADOR', 'ROL_JEFE_PRODUCCION')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in ('PO_WRITE', 'PO_EXPORT', 'PO_WORKFLOW', 'PO_WORKFLOW_START', 'PO_WORKFLOW_FINISH', 'PO_DECIDE')
where r.codigo = 'ROL_PLANEADOR'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'PO_READ',
    'PO_WRITE',
    'PO_EXPORT',
    'PO_DECIDE',
    'PO_WORKFLOW',
    'PO_WORKFLOW_START',
    'PO_WORKFLOW_FINISH'
)
where r.codigo = 'ROL_SUPER_ADMIN'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );