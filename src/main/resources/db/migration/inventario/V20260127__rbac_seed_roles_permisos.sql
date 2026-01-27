insert into roles (codigo, nombre, activo)
select 'ROL_MICROBIOLOGO', 'Microbiologo', b'1'
where not exists (select 1 from roles where codigo = 'ROL_MICROBIOLOGO');

insert into roles (codigo, nombre, activo)
select 'ROL_JEFE_CALIDAD', 'Jefe Calidad', b'1'
where not exists (select 1 from roles where codigo = 'ROL_JEFE_CALIDAD');

insert into roles (codigo, nombre, activo)
select 'ROL_ANALISTA_CALIDAD', 'Analista Calidad', b'1'
where not exists (select 1 from roles where codigo = 'ROL_ANALISTA_CALIDAD');

insert into roles (codigo, nombre, activo)
select 'ROL_JEFE_ALMACENES', 'Jefe Almacenes', b'1'
where not exists (select 1 from roles where codigo = 'ROL_JEFE_ALMACENES');

insert into roles (codigo, nombre, activo)
select 'ROL_ALMACENISTA', 'Almacenista', b'1'
where not exists (select 1 from roles where codigo = 'ROL_ALMACENISTA');

insert into roles (codigo, nombre, activo)
select 'ROL_JEFE_PRODUCCION', 'Jefe Produccion', b'1'
where not exists (select 1 from roles where codigo = 'ROL_JEFE_PRODUCCION');

insert into roles (codigo, nombre, activo)
select 'ROL_LIDER_HOMEOPATICOS', 'Lider Homeopaticos', b'1'
where not exists (select 1 from roles where codigo = 'ROL_LIDER_HOMEOPATICOS');

insert into roles (codigo, nombre, activo)
select 'ROL_LIDER_ALIMENTOS', 'Lider Alimentos', b'1'
where not exists (select 1 from roles where codigo = 'ROL_LIDER_ALIMENTOS');

insert into roles (codigo, nombre, activo)
select 'ROL_CONTADOR', 'Contador', b'1'
where not exists (select 1 from roles where codigo = 'ROL_CONTADOR');

insert into roles (codigo, nombre, activo)
select 'ROL_COMPRADOR', 'Comprador', b'1'
where not exists (select 1 from roles where codigo = 'ROL_COMPRADOR');

insert into roles (codigo, nombre, activo)
select 'ROL_PLANEADOR', 'Planeador', b'1'
where not exists (select 1 from roles where codigo = 'ROL_PLANEADOR');

insert into roles (codigo, nombre, activo)
select 'ROL_SUPER_ADMIN', 'Super Admin', b'1'
where not exists (select 1 from roles where codigo = 'ROL_SUPER_ADMIN');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_OP_READ', 'PROD', 'READ', 'Lectura de ordenes de produccion', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_OP_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_OP_CREATE', 'PROD', 'CREATE', 'Creacion de ordenes de produccion', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_OP_CREATE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_OP_EDIT', 'PROD', 'EDIT', 'Edicion de ordenes de produccion', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_OP_EDIT');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_OP_WORKFLOW_CANCEL', 'PROD', 'WORKFLOW_CANCEL', 'Cancelacion de ordenes de produccion', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_OP_WORKFLOW_CANCEL');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_OP_WORKFLOW_FINALIZE', 'PROD', 'WORKFLOW_FINALIZE', 'Finalizacion de ordenes de produccion', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_OP_WORKFLOW_FINALIZE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_ETAPA_CHECKLIST_READ', 'PROD', 'READ', 'Lectura de checklist de etapa', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_ETAPA_CHECKLIST_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_ETAPA_CHECKLIST_WRITE', 'PROD', 'WRITE', 'Actualizacion de checklist de etapa', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_ETAPA_CHECKLIST_WRITE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_BATCH_RECORD_READ', 'PROD', 'READ', 'Lectura de batch record', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_BATCH_RECORD_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_BATCH_RECORD_WRITE', 'PROD', 'WRITE', 'Actualizacion de batch record', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_BATCH_RECORD_WRITE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PROD_BATCH_RECORD_EXPORT', 'PROD', 'EXPORT', 'Exportacion de batch record', b'1'
where not exists (select 1 from permisos where codigo = 'PROD_BATCH_RECORD_EXPORT');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_PRODUCT_READ', 'INV', 'READ', 'Lectura de productos', b'1'
where not exists (select 1 from permisos where codigo = 'INV_PRODUCT_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_MOV_READ', 'INV', 'READ', 'Lectura de movimientos de inventario', b'1'
where not exists (select 1 from permisos where codigo = 'INV_MOV_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_REPORTES_EXPORT', 'INV', 'EXPORT', 'Exportacion de reportes de inventario', b'1'
where not exists (select 1 from permisos where codigo = 'INV_REPORTES_EXPORT');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_LOTES_READ', 'INV', 'READ', 'Lectura de lotes', b'1'
where not exists (select 1 from permisos where codigo = 'INV_LOTES_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'BOM_FORMULA_READ', 'BOM', 'READ', 'Lectura de formulas BOM', b'1'
where not exists (select 1 from permisos where codigo = 'BOM_FORMULA_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'QC_ALERTAS_READ', 'QC', 'READ', 'Lectura de alertas de calidad', b'1'
where not exists (select 1 from permisos where codigo = 'QC_ALERTAS_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'PO_PLAN_SEMANAL_READ', 'PO', 'READ', 'Lectura de plan semanal', b'1'
where not exists (select 1 from permisos where codigo = 'PO_PLAN_SEMANAL_READ');

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
    'INV_PRODUCT_READ',
    'INV_MOV_READ',
    'INV_REPORTES_EXPORT',
    'INV_LOTES_READ',
    'BOM_FORMULA_READ',
    'QC_ALERTAS_READ',
    'PO_PLAN_SEMANAL_READ'
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
    'PO_PLAN_SEMANAL_READ',
    'INV_PRODUCT_READ',
    'INV_LOTES_READ'
)
where r.codigo = 'ROL_PLANEADOR'
  and not exists (
    select 1 from roles_permisos rp
    where rp.rol_id = r.id and rp.permiso_id = p.id
  );
