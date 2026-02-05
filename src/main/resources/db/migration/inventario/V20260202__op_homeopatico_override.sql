CREATE TABLE op_homeopatico_override (
    id BIGINT NOT NULL AUTO_INCREMENT,
    orden_produccion_id BIGINT NOT NULL,
    producto_id INT NOT NULL,
    semanas_vigencia INT NOT NULL,
    cantidad_solicitada DECIMAL(18,3) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    usuario_id BIGINT NULL,
    fecha DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_op_homeopatico_override_orden
        FOREIGN KEY (orden_produccion_id) REFERENCES orden_produccion (id),
    CONSTRAINT fk_op_homeopatico_override_producto
        FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_op_homeopatico_override_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_op_homeopatico_override_orden
    ON op_homeopatico_override (orden_produccion_id);

CREATE INDEX idx_op_homeopatico_override_producto
    ON op_homeopatico_override (producto_id);
