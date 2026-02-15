insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'QC_READ', 'QC', 'READ', 'Lectura general del modulo de calidad', b'1'
where not exists (select 1 from permisos where codigo = 'QC_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'QC_WRITE', 'QC', 'WRITE', 'Escritura general del modulo de calidad', b'1'
where not exists (select 1 from permisos where codigo = 'QC_WRITE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'QC_EXPORT', 'QC', 'EXPORT', 'Exportacion general del modulo de calidad', b'1'
where not exists (select 1 from permisos where codigo = 'QC_EXPORT');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'QC_WORKFLOW', 'QC', 'WORKFLOW', 'Gestion general de flujos operativos de calidad', b'1'
where not exists (select 1 from permisos where codigo = 'QC_WORKFLOW');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'QC_WORKFLOW_FINISH', 'QC', 'WORKFLOW_FINISH', 'Cierre de flujos operativos de calidad', b'1'
where not exists (select 1 from permisos where codigo = 'QC_WORKFLOW_FINISH');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'QC_DECIDE', 'QC', 'DECIDE', 'Decisiones de liberacion/cierre en calidad', b'1'
where not exists (select 1 from permisos where codigo = 'QC_DECIDE');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'QC_READ'
where r.codigo in ('ROL_JEFE_CALIDAD', 'ROL_ANALISTA_CALIDAD', 'ROL_MICROBIOLOGO')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in ('QC_WRITE', 'QC_EXPORT')
where r.codigo in ('ROL_JEFE_CALIDAD', 'ROL_ANALISTA_CALIDAD', 'ROL_MICROBIOLOGO')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'QC_WORKFLOW'
where r.codigo in ('ROL_JEFE_CALIDAD', 'ROL_MICROBIOLOGO')
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in ('QC_WORKFLOW_FINISH', 'QC_DECIDE')
where r.codigo = 'ROL_JEFE_CALIDAD'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'QC_READ',
    'QC_WRITE',
    'QC_EXPORT',
    'QC_WORKFLOW',
    'QC_WORKFLOW_FINISH',
    'QC_DECIDE'
)
where r.codigo = 'ROL_SUPER_ADMIN'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );