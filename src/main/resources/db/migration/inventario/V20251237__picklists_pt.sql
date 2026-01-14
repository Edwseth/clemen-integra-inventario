-- V20251237 - Picklists PT (cabecera, líneas y asignaciones)
CREATE TABLE IF NOT EXISTS picklists_pt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(50) NOT NULL,
    cliente_nombre VARCHAR(255) NOT NULL,
    min_vida_util_dias INT NULL,
    estado ENUM('BORRADOR','GENERADO','CONFIRMADO','EJECUTADO','CANCELADO') NOT NULL,
    almacen_pt_id INT NOT NULL,
    tipo_movimiento_detalle_id INT NOT NULL,
    doc_referencia VARCHAR(100) NULL,
    observaciones VARCHAR(500) NULL,
    creado_por BIGINT NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    confirmado_por BIGINT NULL,
    fecha_confirmacion DATETIME(6) NULL,
    ejecutado_por BIGINT NULL,
    fecha_ejecucion DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_picklists_pt_codigo (codigo),
    KEY idx_picklist_estado (estado),
    CONSTRAINT fk_picklists_pt_creado_por FOREIGN KEY (creado_por) REFERENCES usuarios (id),
    CONSTRAINT fk_picklists_pt_confirmado_por FOREIGN KEY (confirmado_por) REFERENCES usuarios (id),
    CONSTRAINT fk_picklists_pt_ejecutado_por FOREIGN KEY (ejecutado_por) REFERENCES usuarios (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS picklists_pt_lineas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    picklist_id BIGINT NOT NULL,
    producto_id INT NOT NULL,
    cantidad DECIMAL(18,6) NOT NULL,
    modo_asignacion ENUM('AUTO_FEFO','MANUAL_LOTE') NOT NULL,
    lote_producto_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_picklist_lineas_picklist (picklist_id),
    CONSTRAINT fk_picklists_pt_lineas_picklist FOREIGN KEY (picklist_id) REFERENCES picklists_pt (id),
    CONSTRAINT fk_picklists_pt_lineas_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_picklists_pt_lineas_lote FOREIGN KEY (lote_producto_id) REFERENCES lotes_productos (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS picklists_pt_asignaciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    picklist_id BIGINT NOT NULL,
    producto_id INT NOT NULL,
    lote_producto_id BIGINT NOT NULL,
    cantidad_asignada DECIMAL(18,6) NOT NULL,
    fecha_vencimiento DATETIME(6) NULL,
    almacen_id INT NOT NULL,
    orden SMALLINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_picklist_asig_picklist (picklist_id),
    CONSTRAINT fk_picklists_pt_asig_picklist FOREIGN KEY (picklist_id) REFERENCES picklists_pt (id),
    CONSTRAINT fk_picklists_pt_asig_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_picklists_pt_asig_lote FOREIGN KEY (lote_producto_id) REFERENCES lotes_productos (id),
    CONSTRAINT fk_picklists_pt_asig_almacen FOREIGN KEY (almacen_id) REFERENCES almacenes (id)
) ENGINE=InnoDB;
