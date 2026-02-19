insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_MOVIMIENTOS_READ', 'INV', 'READ', 'Lectura de pantalla Movimientos', b'1'
where not exists (select 1 from permisos where codigo = 'INV_MOVIMIENTOS_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_SOLICITUDES_READ', 'INV', 'READ', 'Lectura de pantalla Solicitudes', b'1'
where not exists (select 1 from permisos where codigo = 'INV_SOLICITUDES_READ');

insert into roles_permisos (rol_id, permiso_id)
select rp.rol_id, p_new.id
from roles_permisos rp
join permisos p_old on p_old.id = rp.permiso_id and p_old.codigo = 'INV_READ'
join roles r on r.id = rp.rol_id
join permisos p_new on p_new.codigo = 'INV_MOVIMIENTOS_READ'
where r.codigo <> 'ROL_CONTADOR'
  and not exists (
    select 1
    from roles_permisos x
    where x.rol_id = rp.rol_id and x.permiso_id = p_new.id
  );

insert into roles_permisos (rol_id, permiso_id)
select rp.rol_id, p_new.id
from roles_permisos rp
join permisos p_old on p_old.id = rp.permiso_id and p_old.codigo = 'INV_READ'
join roles r on r.id = rp.rol_id
join permisos p_new on p_new.codigo = 'INV_SOLICITUDES_READ'
where r.codigo <> 'ROL_CONTADOR'
  and not exists (
    select 1
    from roles_permisos x
    where x.rol_id = rp.rol_id and x.permiso_id = p_new.id
  );

delete rp
from roles_permisos rp
join roles r on r.id = rp.rol_id
join permisos p on p.id = rp.permiso_id
where r.codigo = 'ROL_CONTADOR'
  and p.codigo in ('INV_MOVIMIENTOS_READ', 'INV_SOLICITUDES_READ');