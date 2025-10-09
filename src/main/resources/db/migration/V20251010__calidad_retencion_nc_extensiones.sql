ALTER TABLE retencion_lote
    ADD COLUMN motivo ENUM('NO_CONFORMIDAD','OTRO') NOT NULL DEFAULT 'OTRO' AFTER causa,
    ADD COLUMN no_conformidad_id BIGINT NULL AFTER motivo,
    ADD CONSTRAINT fk_retencion_no_conformidad FOREIGN KEY (no_conformidad_id) REFERENCES no_conformidad (id);

CREATE INDEX idx_retencion_lote_estado_motivo ON retencion_lote (lote_id, estado, motivo);

ALTER TABLE no_conformidad
    ADD COLUMN estado ENUM('ABIERTA','CERRADA') NOT NULL DEFAULT 'ABIERTA' AFTER severidad,
    ADD COLUMN lote_id BIGINT NULL AFTER estado,
    ADD COLUMN producto_id BIGINT NULL AFTER lote_id,
    ADD COLUMN evaluacion_id BIGINT NULL AFTER producto_id,
    ADD COLUMN creado_por BIGINT NULL AFTER usuario_reporta_id,
    ADD COLUMN actualizado_por BIGINT NULL AFTER creado_por,
    ADD COLUMN actualizado_en DATETIME(6) NULL AFTER actualizado_por;

ALTER TABLE no_conformidad
    ADD CONSTRAINT fk_nc_lote FOREIGN KEY (lote_id) REFERENCES lotes_productos (id),
    ADD CONSTRAINT fk_nc_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    ADD CONSTRAINT fk_nc_evaluacion FOREIGN KEY (evaluacion_id) REFERENCES evaluaciones_calidad (id);

CREATE INDEX idx_nc_lote_estado ON no_conformidad (lote_id, estado);
