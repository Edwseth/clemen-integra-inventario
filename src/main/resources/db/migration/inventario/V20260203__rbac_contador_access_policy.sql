insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_CONTEOS_READ', 'INV', 'READ', 'Lectura de conteos ciclicos', b'1'
where not exists (select 1 from permisos where codigo = 'INV_CONTEOS_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_UBICACIONES_READ', 'INV', 'READ', 'Lectura de ubicaciones fisicas', b'1'
where not exists (select 1 from permisos where codigo = 'INV_UBICACIONES_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_AJUSTES_READ', 'INV', 'READ', 'Lectura de ajustes de inventario', b'1'
where not exists (select 1 from permisos where codigo = 'INV_AJUSTES_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_AJUSTES_WRITE', 'INV', 'WRITE', 'Gestion de ajustes de inventario', b'1'
where not exists (select 1 from permisos where codigo = 'INV_AJUSTES_WRITE');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'INV_CONTEOS_READ',
    'INV_UBICACIONES_READ',
    'INV_AJUSTES_READ',
    'INV_AJUSTES_WRITE',
    'PROD_INDICADORES_READ',
    'PROD_INDICADORES_EXPORT',
    'PROD_ALERTAS_READ',
    'PO_PLAN_SEMANAL_READ'
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
  and p.codigo = 'INV_CONTEOS_WRITE';
