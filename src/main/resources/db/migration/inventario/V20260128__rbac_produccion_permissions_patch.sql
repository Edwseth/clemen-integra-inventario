insert into permisos (codigo, modulo, accion, descripcion, activo)
select v.codigo, v.modulo, v.accion, v.descripcion, v.activo
from (
    select 'PROD_OP_READ' as codigo, 'PROD' as modulo, 'READ' as accion, 'Lectura de ordenes de produccion' as descripcion, b'1' as activo
    union all
    select 'PROD_OP_CREATE', 'PROD', 'CREATE', 'Creacion de ordenes de produccion', b'1'
    union all
    select 'PROD_OP_EDIT', 'PROD', 'EDIT', 'Edicion de ordenes de produccion', b'1'
    union all
    select 'PROD_OP_WORKFLOW_CANCEL', 'PROD', 'WORKFLOW_CANCEL', 'Cancelacion de ordenes de produccion', b'1'
    union all
    select 'PROD_OP_WORKFLOW_FINALIZE', 'PROD', 'WORKFLOW_FINALIZE', 'Finalizacion de ordenes de produccion', b'1'
    union all
    select 'PROD_ETAPA_CHECKLIST_READ', 'PROD', 'READ', 'Lectura de checklist de etapa', b'1'
    union all
    select 'PROD_ETAPA_CHECKLIST_WRITE', 'PROD', 'WRITE', 'Actualizacion de checklist de etapa', b'1'
    union all
    select 'PROD_BATCH_RECORD_READ', 'PROD', 'READ', 'Lectura de batch record', b'1'
    union all
    select 'PROD_BATCH_RECORD_WRITE', 'PROD', 'WRITE', 'Actualizacion de batch record', b'1'
    union all
    select 'PROD_BATCH_RECORD_EXPORT', 'PROD', 'EXPORT', 'Exportacion de batch record', b'1'
    union all
    select 'PROD_BATCH_RECORD_DECIDE', 'PROD', 'DECIDE', 'Decision de calidad en batch record', b'1'
    union all
    select 'PROD_ETAPA_CLONE', 'PROD', 'CLONE', 'Clonar etapas de ordenes de produccion', b'1'
    union all
    select 'PROD_ETAPA_START', 'PROD', 'START', 'Iniciar etapa de produccion', b'1'
    union all
    select 'PROD_ETAPA_FINISH', 'PROD', 'FINISH', 'Finalizar etapa de produccion', b'1'
    union all
    select 'PROD_OP_EXPORT', 'PROD', 'EXPORT', 'Exportacion de ordenes de produccion', b'1'
    union all
    select 'PROD_REPORTS_READ', 'PROD', 'READ', 'Lectura de reportes de produccion', b'1'
    union all
    select 'PROD_INDICADORES_READ', 'PROD', 'READ', 'Lectura de indicadores de produccion', b'1'
    union all
    select 'PROD_INDICADORES_EXPORT', 'PROD', 'EXPORT', 'Exportacion de indicadores de produccion', b'1'
    union all
    select 'PROD_ALERTAS_READ', 'PROD', 'READ', 'Lectura de alertas de produccion', b'1'
) v
left join permisos p on p.codigo = v.codigo
where p.id is null;

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'PROD_OP_READ',
    'PROD_OP_CREATE',
    'PROD_OP_EDIT',
    'PROD_OP_WORKFLOW_CANCEL',
    'PROD_OP_WORKFLOW_FINALIZE',
    'PROD_ETAPA_CHECKLIST_READ',
    'PROD_ETAPA_CHECKLIST_WRITE',
    'PROD_BATCH_RECORD_READ',
    'PROD_BATCH_RECORD_WRITE',
    'PROD_BATCH_RECORD_EXPORT',
    'PROD_BATCH_RECORD_DECIDE',
    'PROD_ETAPA_CLONE',
    'PROD_ETAPA_START',
    'PROD_ETAPA_FINISH',
    'PROD_OP_EXPORT',
    'PROD_REPORTS_READ',
    'PROD_INDICADORES_READ',
    'PROD_INDICADORES_EXPORT',
    'PROD_ALERTAS_READ'
)
where r.codigo = 'ROL_SUPER_ADMIN'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'PROD_OP_READ',
    'PROD_OP_CREATE',
    'PROD_OP_EDIT',
    'PROD_OP_WORKFLOW_CANCEL',
    'PROD_OP_WORKFLOW_FINALIZE',
    'PROD_ETAPA_CHECKLIST_READ',
    'PROD_ETAPA_CHECKLIST_WRITE',
    'PROD_BATCH_RECORD_READ',
    'PROD_BATCH_RECORD_WRITE',
    'PROD_BATCH_RECORD_EXPORT',
    'PROD_ETAPA_CLONE',
    'PROD_ETAPA_START',
    'PROD_ETAPA_FINISH',
    'PROD_OP_EXPORT',
    'PROD_REPORTS_READ',
    'PROD_INDICADORES_READ',
    'PROD_INDICADORES_EXPORT',
    'PROD_ALERTAS_READ'
)
where r.codigo = 'ROL_JEFE_PRODUCCION'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'PROD_OP_READ',
    'PROD_OP_CREATE',
    'PROD_OP_EDIT',
    'PROD_OP_WORKFLOW_CANCEL',
    'PROD_OP_WORKFLOW_FINALIZE',
    'PROD_ETAPA_CHECKLIST_READ',
    'PROD_ETAPA_CHECKLIST_WRITE',
    'PROD_BATCH_RECORD_READ',
    'PROD_BATCH_RECORD_WRITE',
    'PROD_BATCH_RECORD_EXPORT',
    'PROD_ETAPA_CLONE',
    'PROD_ETAPA_START',
    'PROD_ETAPA_FINISH',
    'PROD_OP_EXPORT',
    'PROD_REPORTS_READ',
    'PROD_INDICADORES_READ',
    'PROD_INDICADORES_EXPORT',
    'PROD_ALERTAS_READ'
)
where r.codigo = 'ROL_PLANEADOR'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'PROD_OP_READ',
    'PROD_BATCH_RECORD_READ',
    'PROD_BATCH_RECORD_EXPORT',
    'PROD_BATCH_RECORD_DECIDE',
    'PROD_ALERTAS_READ'
)
where r.codigo = 'ROL_JEFE_CALIDAD'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'PROD_OP_READ',
    'PROD_BATCH_RECORD_READ',
    'PROD_BATCH_RECORD_EXPORT',
    'PROD_ALERTAS_READ'
)
where r.codigo = 'ROL_ANALISTA_CALIDAD'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo in (
    'PROD_OP_READ',
    'PROD_BATCH_RECORD_READ',
    'PROD_BATCH_RECORD_EXPORT',
    'PROD_ALERTAS_READ'
)
where r.codigo = 'ROL_MICROBIOLOGO'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );
