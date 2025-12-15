-- Ajustes de idempotencia para movimientos de inventario
-- 1) Remover índice único errado sobre codigo_lote/producto si existe
ALTER TABLE movimientos_inventario
    DROP INDEX IF EXISTS uk_movimientos_inventario_codigo_lote_producto;
ALTER TABLE movimientos_inventario
    DROP INDEX IF EXISTS uk_mov_inv_codigo_lote_producto;
ALTER TABLE movimientos_inventario
    DROP INDEX IF EXISTS uk_codigo_lote_productos;
ALTER TABLE movimientos_inventario
    DROP INDEX IF EXISTS movimientos_inventario_codigo_lote_producto_idx;

-- 2) Agregar clave de idempotencia y su índice único (permite múltiples NULL)
ALTER TABLE movimientos_inventario
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64) NULL AFTER codigo_recepcion;

CREATE UNIQUE INDEX IF NOT EXISTS uk_movimientos_inventario_idempotency
    ON movimientos_inventario (idempotency_key);
