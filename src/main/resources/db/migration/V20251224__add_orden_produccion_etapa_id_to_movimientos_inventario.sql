alter table movimientos_inventario
    add column orden_produccion_etapa_id bigint null;

alter table movimientos_inventario
    add constraint fk_movimientos_inventario_orden_produccion_etapa
        foreign key (orden_produccion_etapa_id) references etapa_produccion (id);

create index idx_mov_op_etapa
    on movimientos_inventario (orden_produccion_id, orden_produccion_etapa_id);

