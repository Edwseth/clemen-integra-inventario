CREATE TABLE controles_proceso_produccion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    etapa VARCHAR(100),
    parametro VARCHAR(150),
    valor_medido VARCHAR(255),
    unidad VARCHAR(50),
    cumple BIT,
    observaciones TEXT,
    fecha_registro DATETIME(6),
    orden_produccion_id BIGINT NOT NULL,
    evaluado_por_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cpp_orden FOREIGN KEY (orden_produccion_id) REFERENCES orden_produccion (id),
    CONSTRAINT fk_cpp_usuario FOREIGN KEY (evaluado_por_id) REFERENCES usuarios (id)
) ENGINE=InnoDB;

CREATE TABLE controles_empaque_lote (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parametro VARCHAR(150),
    valor_medido VARCHAR(255),
    unidad VARCHAR(50),
    cumple BIT,
    observaciones TEXT,
    fecha_registro DATETIME(6),
    orden_produccion_id BIGINT NOT NULL,
    lote_producto_id BIGINT,
    evaluado_por_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cel_orden FOREIGN KEY (orden_produccion_id) REFERENCES orden_produccion (id),
    CONSTRAINT fk_cel_lote FOREIGN KEY (lote_producto_id) REFERENCES lotes_productos (id),
    CONSTRAINT fk_cel_usuario FOREIGN KEY (evaluado_por_id) REFERENCES usuarios (id)
) ENGINE=InnoDB;

CREATE TABLE observaciones_proceso (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tipo VARCHAR(100),
    descripcion TEXT,
    fecha_registro DATETIME(6),
    orden_produccion_id BIGINT NOT NULL,
    registrado_por_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_op_orden FOREIGN KEY (orden_produccion_id) REFERENCES orden_produccion (id),
    CONSTRAINT fk_op_usuario FOREIGN KEY (registrado_por_id) REFERENCES usuarios (id)
) ENGINE=InnoDB;
