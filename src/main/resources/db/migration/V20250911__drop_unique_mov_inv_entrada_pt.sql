-- Remove unique constraint to allow multiple PT entries per OP/product/lote
ALTER TABLE movimientos_inventario DROP INDEX uk_mov_inv_entrada_pt;

-- Optional non-unique index for performance
CREATE INDEX idx_mov_inv_entrada_pt ON movimientos_inventario (tipo_mov, motivos_movimiento_id, orden_produccion_id, productos_id, lotes_productos_id);
