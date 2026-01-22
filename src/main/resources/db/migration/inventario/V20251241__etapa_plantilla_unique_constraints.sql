alter table etapa_plantilla
    add constraint uk_etapa_plantilla_producto_nombre unique (producto_id, nombre);
