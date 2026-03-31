create table if not exists ajustes_valorizacion_lote (
    id bigint not null auto_increment,
    lote_id bigint not null,
    codigo_lote varchar(80) not null,
    producto_id int not null,
    almacen_id int not null,
    costo_unitario_anterior decimal(19,6) not null,
    costo_unitario_nuevo decimal(19,6) not null,
    costo_total_anterior decimal(19,6) not null,
    costo_total_nuevo decimal(19,6) not null,
    total_ingresado_anterior decimal(19,6) not null,
    total_ingresado_nuevo decimal(19,6) not null,
    motivo varchar(120) not null,
    observacion varchar(500) not null,
    documento_soporte varchar(255) not null,
    idempotency_key varchar(64) not null,
    payload_fingerprint varchar(64) not null,
    usuario_id bigint not null,
    fecha_ajuste datetime not null,
    primary key (id),
    unique key uk_ajuste_val_lote_idem (idempotency_key),
    constraint fk_aj_val_lote_lote foreign key (lote_id) references lotes_productos (id),
    constraint fk_aj_val_lote_producto foreign key (producto_id) references productos (id),
    constraint fk_aj_val_lote_almacen foreign key (almacen_id) references almacenes (id),
    constraint fk_aj_val_lote_usuario foreign key (usuario_id) references usuarios (id)
);

create index idx_aj_val_lote_lote_fecha on ajustes_valorizacion_lote (lote_id, fecha_ajuste desc);

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_COSTEO_AJUSTE_READ', 'INV', 'READ', 'Lectura de elegibilidad y auditoria de ajuste de valorizacion por lote', b'1'
where not exists (select 1 from permisos where codigo = 'INV_COSTEO_AJUSTE_READ');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_COSTEO_AJUSTE_WRITE', 'INV', 'WRITE', 'Ejecucion de ajuste de valorizacion por lote', b'1'
where not exists (select 1 from permisos where codigo = 'INV_COSTEO_AJUSTE_WRITE');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'INV_COSTEO_AJUSTE_OVERRIDE', 'INV', 'DECIDE', 'Override sobre lotes con costo positivo en ajuste de valorizacion', b'1'
where not exists (select 1 from permisos where codigo = 'INV_COSTEO_AJUSTE_OVERRIDE');

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'INV_COSTEO_AJUSTE_READ'
where r.nombre in ('Contador', 'Jefe Almacenes')
  and not exists (
    select 1 from roles_permisos rp where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'INV_COSTEO_AJUSTE_WRITE'
where r.nombre in ('Contador')
  and not exists (
    select 1 from roles_permisos rp where rp.rol_id = r.id and rp.permiso_id = p.id
  );

insert into roles_permisos (rol_id, permiso_id)
select r.id, p.id
from roles r
join permisos p on p.codigo = 'INV_COSTEO_AJUSTE_OVERRIDE'
where r.nombre in ('Super Admin')
  and not exists (
    select 1 from roles_permisos rp where rp.rol_id = r.id and rp.permiso_id = p.id
  );
