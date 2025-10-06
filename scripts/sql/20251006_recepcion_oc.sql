-- Script de migración para recepciones de orden de compra

CREATE TABLE IF NOT EXISTS recepciones_oc (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(32) NOT NULL,
    fecha_recepcion DATE NOT NULL,
    orden_compra_id INT NOT NULL,
    almacen_destino_id INT NOT NULL,
    proveedor_id INT NULL,
    usuario_id BIGINT NOT NULL,
    observaciones VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recepciones_oc_codigo (codigo),
    UNIQUE KEY uk_recepciones_oc_orden_fecha (orden_compra_id, fecha_recepcion),
    KEY idx_recepciones_oc_orden_compra (orden_compra_id),
    KEY idx_recepciones_oc_almacen_destino (almacen_destino_id),
    KEY idx_recepciones_oc_proveedor (proveedor_id),
    KEY idx_recepciones_oc_usuario (usuario_id),
    CONSTRAINT fk_recepciones_oc_orden_compra FOREIGN KEY (orden_compra_id) REFERENCES ordenes_compra (id),
    CONSTRAINT fk_recepciones_oc_almacen_destino FOREIGN KEY (almacen_destino_id) REFERENCES almacenes (id),
    CONSTRAINT fk_recepciones_oc_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_recepciones_oc_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recepciones_oc_detalles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    recepcion_oc_id BIGINT UNSIGNED NOT NULL,
    orden_compra_detalle_id BIGINT NOT NULL,
    productos_id INT NOT NULL,
    lotes_productos_id BIGINT NULL,
    cantidad_recibida DECIMAL(18,6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_recepciones_oc_detalles_recepcion (recepcion_oc_id),
    KEY idx_recepciones_oc_detalles_orden_compra_detalle (orden_compra_detalle_id),
    KEY idx_recepciones_oc_detalles_producto (productos_id),
    KEY idx_recepciones_oc_detalles_lote (lotes_productos_id),
    CONSTRAINT fk_recepciones_oc_detalles_recepcion FOREIGN KEY (recepcion_oc_id) REFERENCES recepciones_oc (id),
    CONSTRAINT fk_recepciones_oc_detalles_orden_compra_detalle FOREIGN KEY (orden_compra_detalle_id) REFERENCES orden_compra_detalle (id),
    CONSTRAINT fk_recepciones_oc_detalles_producto FOREIGN KEY (productos_id) REFERENCES productos (id),
    CONSTRAINT fk_recepciones_oc_detalles_lote FOREIGN KEY (lotes_productos_id) REFERENCES lotes_productos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS secuencias_recepcion (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    anio INT NOT NULL,
    prefijo VARCHAR(16) NOT NULL,
    secuencia_actual BIGINT UNSIGNED NOT NULL,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_secuencias_recepcion_anio_prefijo (anio, prefijo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE movimientos_inventario
    ADD COLUMN recepcion_oc_id BIGINT UNSIGNED NULL,
    ADD COLUMN codigo_recepcion VARCHAR(32) NULL,
    ADD KEY idx_movimientos_inventario_recepcion_oc (recepcion_oc_id),
    ADD KEY idx_movimientos_inventario_codigo_recepcion (codigo_recepcion),
    ADD CONSTRAINT fk_movimientos_inventario_recepcion_oc FOREIGN KEY (recepcion_oc_id) REFERENCES recepciones_oc (id);
