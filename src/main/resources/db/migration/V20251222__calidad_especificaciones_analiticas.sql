CREATE TABLE IF NOT EXISTS especificaciones_fisico_quimicas (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    nombre_parametro VARCHAR(255) NOT NULL,
    unidad VARCHAR(100) NULL,
    limite_inferior DECIMAL(10, 2) NULL,
    limite_superior DECIMAL(10, 2) NULL,
    observaciones TEXT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NULL,
    created_by_id BIGINT NULL,
    updated_at DATETIME NULL,
    updated_by_id BIGINT NULL,
    CONSTRAINT fk_espec_fq_producto FOREIGN KEY (producto_id)
        REFERENCES productos (id),
    CONSTRAINT fk_espec_fq_creado_por FOREIGN KEY (created_by_id)
        REFERENCES usuarios (id),
    CONSTRAINT fk_espec_fq_actualizado_por FOREIGN KEY (updated_by_id)
        REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_espec_fq_producto_activo
    ON especificaciones_fisico_quimicas (producto_id, activo);

ALTER TABLE plantillas_analisis_micro
    ADD COLUMN IF NOT EXISTS producto_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS vigente TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fecha_vigencia_desde DATE NULL,
    ADD COLUMN IF NOT EXISTS fecha_vigencia_hasta DATE NULL,
    ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS updated_by_id BIGINT NULL;

ALTER TABLE plantillas_analisis_micro
    ADD CONSTRAINT fk_plantilla_micro_producto FOREIGN KEY (producto_id)
        REFERENCES productos (id);

ALTER TABLE plantillas_analisis_micro
    ADD CONSTRAINT fk_plantilla_micro_creado_por FOREIGN KEY (created_by_id)
        REFERENCES usuarios (id);

ALTER TABLE plantillas_analisis_micro
    ADD CONSTRAINT fk_plantilla_micro_actualizado_por FOREIGN KEY (updated_by_id)
        REFERENCES usuarios (id);

CREATE INDEX idx_plantilla_micro_producto_vigente
    ON plantillas_analisis_micro (producto_id, vigente);
