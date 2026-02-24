SET @schema := DATABASE();
SET @idx := 'idx_mov_fecha_lote_producto';

SET @sql := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema
          AND table_name = 'movimientos_inventario'
          AND index_name = @idx
    ),
    'SELECT "OK: idx_mov_fecha_lote_producto ya existe" AS info;',
    'CREATE INDEX idx_mov_fecha_lote_producto ON movimientos_inventario (fecha_ingreso, lotes_productos_id, productos_id);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
