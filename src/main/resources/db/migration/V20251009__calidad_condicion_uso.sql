CREATE TABLE condiciones_uso (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lote_id BIGINT NOT NULL,
    tipo ENUM('REEVALUACION_FECHA','DISTRIBUCION_CONDICIONADA','LIMITACION_USO','OTRO') NOT NULL,
    parametro_fecha DATETIME NULL,
    descripcion TEXT NULL,
    estado ENUM('ACTIVA','LEVANTADA') NOT NULL DEFAULT 'ACTIVA',
    creado_por BIGINT NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_por BIGINT NULL,
    actualizado_en DATETIME NULL,
    CONSTRAINT fk_condicion_uso_lote FOREIGN KEY (lote_id) REFERENCES lotes_productos (id)
) ENGINE=InnoDB;

CREATE INDEX idx_condicion_uso_lote_estado ON condiciones_uso (lote_id, estado);
