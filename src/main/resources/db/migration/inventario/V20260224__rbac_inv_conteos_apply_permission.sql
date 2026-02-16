-- Refuerzo RBAC: permiso explicito para aplicar conteos ciclicos
insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_CONTEOS_APPLY', 'INV', 'WORKFLOW_FINISH', 'Aplicar conteos ciclicos', b'1'
where not exists (select 1 from permisos where codigo = 'INV_CONTEOS_APPLY');

-- Asignacion recomendada: CONTADOR
insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'INV_CONTEOS_APPLY'
where r.codigo = 'ROL_CONTADOR'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

-- Seguridad operacional: super admin mantiene acceso
insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'INV_CONTEOS_APPLY'
where r.codigo = 'ROL_SUPER_ADMIN'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

-- Asignacion opcional segun negocio (no aplicada automaticamente): ROL_JEFE_ALMACENES
-- insert into roles_permisos (rol_id, permiso_id)
-- select r.id, p.id
-- from roles r
-- join permisos p on p.codigo = 'INV_CONTEOS_APPLY'
-- where r.codigo = 'ROL_JEFE_ALMACENES'
--   and not exists (
--     select 1 from roles_permisos rp
--     where rp.rol_id = r.id and rp.permiso_id = p.id
--   );