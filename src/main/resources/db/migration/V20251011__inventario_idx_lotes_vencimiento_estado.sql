-- V20251011__inventario_idx_lotes_vencimiento_estado.sql

-- MySQL no soporta CREATE INDEX IF NOT EXISTS
-- Entonces lo hacemos idempotente consultando information_schema.statistics

SET @idx_name := 'idx_lote_vencimiento_estado';
SET @tbl := 'lotes_productos';

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = @tbl
        AND index_name = @idx_name
    ),
    'SELECT "OK: index ya existe" AS info;',
    'CREATE INDEX idx_lote_vencimiento_estado ON lotes_productos(fecha_vencimiento, estado);'
  )
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
