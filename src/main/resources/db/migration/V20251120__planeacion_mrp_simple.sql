ALTER TABLE productos
    ADD COLUMN lead_time_compra_dias INT NULL,
    ADD COLUMN lead_time_produccion_dias INT NULL,
    ADD COLUMN stock_seguridad DECIMAL(19,6) NULL,
    ADD COLUMN stock_maximo_planeacion DECIMAL(19,6) NULL;

CREATE TABLE corridas_mrp (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fecha_ejecucion DATETIME NOT NULL,
    horizonte_desde DATE NULL,
    horizonte_hasta DATE NULL,
    usuario_ejecuto_id BIGINT NULL,
    resumen VARCHAR(1000) NULL
) ENGINE=InnoDB;

CREATE TABLE sugerencias_abastecimiento (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    corrida_mrp_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    cantidad_sugerida DECIMAL(19,6) NOT NULL,
    fecha_requerida DATE NOT NULL,
    fecha_sugerida_pedido DATE NULL,
    estado VARCHAR(30) NOT NULL,
    origen VARCHAR(30) NOT NULL,
    observaciones VARCHAR(1000) NULL,
    orden_compra_id BIGINT NULL,
    orden_produccion_id BIGINT NULL,
    CONSTRAINT fk_sug_corrida FOREIGN KEY (corrida_mrp_id) REFERENCES corridas_mrp (id),
    CONSTRAINT fk_sug_producto FOREIGN KEY (producto_id) REFERENCES productos (id)
) ENGINE=InnoDB;
