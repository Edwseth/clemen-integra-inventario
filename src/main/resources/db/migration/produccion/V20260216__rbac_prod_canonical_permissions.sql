insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_READ', 'PROD', 'READ', 'Lectura general del modulo de produccion', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_WRITE', 'PROD', 'WRITE', 'Escritura general del modulo de produccion', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_WRITE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_EXPORT', 'PROD', 'EXPORT', 'Exportacion general del modulo de produccion', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_EXPORT');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_DECIDE', 'PROD', 'DECIDE', 'Aprobacion/rechazo de decisiones operativas de produccion', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_DECIDE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_WORKFLOW_START', 'PROD', 'WORKFLOW_START', 'Inicio de flujos operativos de produccion', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_WORKFLOW_START');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_WORKFLOW_FINISH', 'PROD', 'WORKFLOW_FINISH', 'Cierre de flujos operativos de produccion', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_WORKFLOW_FINISH');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_WORKFLOW', 'PROD', 'WORKFLOW', 'Gestion general de flujos operativos de produccion', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_WORKFLOW');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'PROD_READ'
where r.codigo in ('ROL_JEFE_PRODUCCION', 'ROL_PLANEADOR')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in ('PROD_WRITE', 'PROD_EXPORT')
where r.codigo in ('ROL_JEFE_PRODUCCION', 'ROL_PLANEADOR')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in ('PROD_DECIDE', 'PROD_WORKFLOW_START', 'PROD_WORKFLOW_FINISH', 'PROD_WORKFLOW')
where r.codigo = 'ROL_JEFE_PRODUCCION'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'PROD_READ',
    'PROD_WRITE',
    'PROD_EXPORT',
    'PROD_DECIDE',
    'PROD_WORKFLOW_START',
    'PROD_WORKFLOW_FINISH',
    'PROD_WORKFLOW'
)
where r.codigo = 'ROL_SUPER_ADMIN'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );
