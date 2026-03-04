create table oc_servicio_ejecuciones (
    id bigint not null auto_increment,
    orden_compra_id integer not null,
    orden_compra_detalle_id bigint not null,
    cantidad_ejecutada decimal(19,3) not null,
    fecha_ejecucion datetime(6) not null default now(6),
    observaciones varchar(500),
    usuario_id bigint not null,
    created_at datetime(6) not null default now(6),
    primary key (id),
    constraint fk_oc_serv_ejec_orden foreign key (orden_compra_id) references ordenes_compra (id),
    constraint fk_oc_serv_ejec_detalle foreign key (orden_compra_detalle_id) references orden_compra_detalle (id),
    constraint fk_oc_serv_ejec_usuario foreign key (usuario_id) references usuarios (id)
) engine=InnoDB;

create index idx_oc_serv_ejec_orden on oc_servicio_ejecuciones (orden_compra_id);
create index idx_oc_serv_ejec_detalle on oc_servicio_ejecuciones (orden_compra_detalle_id);
