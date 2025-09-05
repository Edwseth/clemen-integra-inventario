ALTER TABLE etapa_produccion
    DROP INDEX nombre_etapa;

ALTER TABLE etapa_produccion
    ADD CONSTRAINT uk_orden_nombre_etapa UNIQUE (orden_produccion_id, nombre_etapa);
