ALTER TABLE orden_produccion
    ADD COLUMN batch_record_estado VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    ADD COLUMN batch_record_revisado_por_id BIGINT NULL,
    ADD COLUMN batch_record_fecha_revision DATETIME NULL,
    ADD COLUMN batch_record_observaciones_calidad TEXT NULL;

ALTER TABLE orden_produccion
    ADD CONSTRAINT fk_op_batch_record_revisado_por FOREIGN KEY (batch_record_revisado_por_id) REFERENCES usuarios (id);
