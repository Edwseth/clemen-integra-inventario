CREATE TABLE IF NOT EXISTS regularizacion_trazabilidad_operacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(80) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    orden_produccion_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    creado_por_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    resultado_json JSON NULL,
    CONSTRAINT uk_regularizacion_trazabilidad_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_reg_trazabilidad_operacion_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuarios (id)
) ENGINE=InnoDB;

CREATE INDEX idx_reg_trazabilidad_op_orden_prod ON regularizacion_trazabilidad_operacion (orden_produccion_id);
CREATE INDEX idx_reg_trazabilidad_op_producto ON regularizacion_trazabilidad_operacion (producto_id);
