ALTER TABLE lotes_productos
    ADD COLUMN lote_origen_id BIGINT NULL;

ALTER TABLE lotes_productos
    ADD CONSTRAINT fk_lotes_productos_lote_origen
        FOREIGN KEY (lote_origen_id) REFERENCES lotes_productos (id);
