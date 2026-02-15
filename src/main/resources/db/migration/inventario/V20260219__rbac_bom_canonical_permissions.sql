insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'BOM_READ', 'BOM', 'READ', 'Lectura general del modulo de BOM', b'1'
where not exists (select 1 from permisos where codigo = 'BOM_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'BOM_WRITE', 'BOM', 'WRITE', 'Escritura general del modulo de BOM', b'1'
where not exists (select 1 from permisos where codigo = 'BOM_WRITE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'BOM_EXPORT', 'BOM', 'EXPORT', 'Exportacion general del modulo de BOM', b'1'
where not exists (select 1 from permisos where codigo = 'BOM_EXPORT');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'BOM_DECIDE', 'BOM', 'DECIDE', 'Decisiones de aprobacion/cambio de estado en BOM', b'1'
where not exists (select 1 from permisos where codigo = 'BOM_DECIDE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'BOM_WORKFLOW', 'BOM', 'WORKFLOW', 'Gestion general de flujos operativos de BOM', b'1'
where not exists (select 1 from permisos where codigo = 'BOM_WORKFLOW');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'BOM_WORKFLOW_START', 'BOM', 'WORKFLOW_START', 'Inicio de flujos operativos de BOM', b'1'
where not exists (select 1 from permisos where codigo = 'BOM_WORKFLOW_START');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'BOM_WORKFLOW_FINISH', 'BOM', 'WORKFLOW_FINISH', 'Cierre de flujos operativos de BOM', b'1'
where not exists (select 1 from permisos where codigo = 'BOM_WORKFLOW_FINISH');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'BOM_READ'
where r.codigo in ('ROL_JEFE_PRODUCCION', 'ROL_PLANEADOR')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'BOM_READ',
    'BOM_WRITE',
    'BOM_EXPORT',
    'BOM_DECIDE',
    'BOM_WORKFLOW',
    'BOM_WORKFLOW_START',
    'BOM_WORKFLOW_FINISH'
)
where r.codigo = 'ROL_SUPER_ADMIN'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );