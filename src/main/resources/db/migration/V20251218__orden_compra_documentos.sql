create table orden_compra_documentos (
    id bigint not null auto_increment,
    orden_compra_id bigint not null,
    tipo_documento varchar(20) not null,
    nombre_visible varchar(255) not null,
    nombre_archivo varchar(255) not null,
    content_type varchar(100),
    size bigint,
    storage_path varchar(500) not null,
    creado_por_id bigint not null,
    fecha_creacion datetime not null default current_timestamp,
    primary key (id)
) ENGINE=InnoDB;

alter table orden_compra_documentos
    add constraint fk_oc_documento_oc foreign key (orden_compra_id) references ordenes_compra (id);

alter table orden_compra_documentos
    add constraint fk_oc_documento_usuario foreign key (creado_por_id) references usuarios (id);

create index idx_oc_documentos_oc on orden_compra_documentos (orden_compra_id);
create index idx_oc_documentos_tipo on orden_compra_documentos (tipo_documento);
