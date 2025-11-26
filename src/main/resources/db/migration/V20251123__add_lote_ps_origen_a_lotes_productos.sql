ALTER TABLE lotes_productos
    ADD COLUMN lote_ps_origen_id BIGINT NULL;

ALTER TABLE lotes_productos
    ADD CONSTRAINT fk_lotes_productos_ps_origen FOREIGN KEY (lote_ps_origen_id) REFERENCES lotes_productos(id);

CREATE INDEX idx_lotes_productos_ps_origen ON lotes_productos(lote_ps_origen_id);
