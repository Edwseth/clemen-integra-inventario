ALTER TABLE movimientos_inventario
    ADD COLUMN causa_devolucion_pt VARCHAR(30) NULL,
    ADD COLUMN condicion_producto_devuelto VARCHAR(20) NULL,
    ADD COLUMN cliente_nombre VARCHAR(255) NULL,
    ADD COLUMN lote_legacy BIT(1) NOT NULL DEFAULT b'0';
