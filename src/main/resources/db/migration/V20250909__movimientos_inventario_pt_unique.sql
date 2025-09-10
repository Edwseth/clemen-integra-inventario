-- Unique index to prevent duplicate PT entry movements
ALTER TABLE movimientos_inventario
    ADD CONSTRAINT uk_mov_inv_entrada_pt UNIQUE (tipo_mov, motivos_movimiento_id, orden_produccion_id, productos_id, lotes_productos_id);
