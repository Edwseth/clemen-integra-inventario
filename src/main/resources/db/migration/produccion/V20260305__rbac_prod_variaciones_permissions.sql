insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_VARIACIONES_READ', 'PROD', 'READ', 'Lectura del informe de variaciones por regularizacion de trazabilidad', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_VARIACIONES_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_VARIACIONES_EXPORT', 'PROD', 'EXPORT', 'Exportacion del informe de variaciones por regularizacion de trazabilidad', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_VARIACIONES_EXPORT');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in ('PROD_VARIACIONES_READ', 'PROD_VARIACIONES_EXPORT')
where r.codigo = 'ROL_SUPER_ADMIN'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );
