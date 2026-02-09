insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'CONTROL_DOCUMENTAL_WRITE', 'DOC', 'WRITE', 'Creacion y carga de versiones en control documental', b'1'
where not exists (select 1 from permisos where codigo = 'CONTROL_DOCUMENTAL_WRITE');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'PO_PLAN_SEMANAL_READ',
    'CONTROL_DOCUMENTAL_WRITE'
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
  and (
      p.codigo in ('QC_RETENCIONES_READ', 'QC_RETENCIONES_WRITE', 'QC_NO_CONFORMIDADES_READ', 'QC_NO_CONFORMIDADES_WRITE',
                   'QC_VIDA_UTIL_READ', 'QC_VIDA_UTIL_WRITE')
      or p.codigo like 'QC_RETENCION%'
      or p.codigo like 'QC_NO_CONFORMIDAD%'
      or p.codigo like 'QC_VIDA_UTIL%'
  );
