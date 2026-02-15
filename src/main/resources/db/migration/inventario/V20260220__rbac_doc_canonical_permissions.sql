insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'DOC_READ', 'DOC', 'READ', 'Lectura general del modulo de control documental', b'1'
where not exists (select 1 from permisos where codigo = 'DOC_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'DOC_WRITE', 'DOC', 'WRITE', 'Creacion y carga de versiones en control documental', b'1'
where not exists (select 1 from permisos where codigo = 'DOC_WRITE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'DOC_EXPORT', 'DOC', 'EXPORT', 'Descarga e impresion de documentos en control documental', b'1'
where not exists (select 1 from permisos where codigo = 'DOC_EXPORT');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'DOC_DELETE', 'DOC', 'DELETE', 'Eliminacion de documentos en control documental', b'1'
where not exists (select 1 from permisos where codigo = 'DOC_DELETE');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in ('DOC_READ', 'DOC_WRITE', 'DOC_EXPORT', 'DOC_DELETE')
where r.codigo = 'ROL_SUPER_ADMIN'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles_permisos rp_cd
join roles r on r.id = rp_cd.rol_id
join permisos p_cd on p_cd.id = rp_cd.permiso_id and p_cd.codigo = 'CONTROL_DOCUMENTAL_WRITE'
join permisos p on p.codigo in ('DOC_READ', 'DOC_WRITE')
where not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'DOC_READ'
where r.codigo in (
    'ROL_JEFE_CALIDAD',
    'ROL_ANALISTA_CALIDAD',
    'ROL_MICROBIOLOGO',
    'ROL_JEFE_ALMACENES',
    'ROL_ALMACENISTA',
    'ROL_JEFE_PRODUCCION',
    'ROL_LIDER_HOMEOPATICOS',
    'ROL_LIDER_ALIMENTOS',
    'ROL_CONTADOR',
    'ROL_COMPRADOR',
    'ROL_PLANEADOR'
)
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in ('DOC_EXPORT', 'DOC_DELETE')
where r.codigo = 'ROL_JEFE_CALIDAD'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );