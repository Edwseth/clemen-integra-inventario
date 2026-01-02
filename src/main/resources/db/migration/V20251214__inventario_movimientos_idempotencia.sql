-- V20251214 - idempotency en movimientos_inventario (idempotente)

-- A) Dropear índices únicos errados si existen (por nombre)
SET @schema := DATABASE();

SET @idx := 'uk_movimientos_inventario_codigo_lote_producto';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema
        AND table_name = 'movimientos_inventario'
        AND index_name = @idx
    ),
    CONCAT('ALTER TABLE movimientos_inventario DROP INDEX ', @idx, ';'),
    'SELECT "OK: uk_movimientos_inventario_codigo_lote_producto no existe" AS info;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := 'uk_mov_inv_codigo_lote_producto';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema
        AND table_name = 'movimientos_inventario'
        AND index_name = @idx
    ),
    CONCAT('ALTER TABLE movimientos_inventario DROP INDEX ', @idx, ';'),
    'SELECT "OK: uk_mov_inv_codigo_lote_producto no existe" AS info;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := 'uk_codigo_lote_productos';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema
        AND table_name = 'movimientos_inventario'
        AND index_name = @idx
    ),
    CONCAT('ALTER TABLE movimientos_inventario DROP INDEX ', @idx, ';'),
    'SELECT "OK: uk_codigo_lote_productos no existe" AS info;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := 'movimientos_inventario_codigo_lote_producto_idx';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema
        AND table_name = 'movimientos_inventario'
        AND index_name = @idx
    ),
    CONCAT('ALTER TABLE movimientos_inventario DROP INDEX ', @idx, ';'),
    'SELECT "OK: movimientos_inventario_codigo_lote_producto_idx no existe" AS info;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- B) Columna idempotency_key si no existe
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = @schema
        AND table_name = 'movimientos_inventario'
        AND column_name = 'idempotency_key'
    ),
    'SELECT "OK: columna idempotency_key ya existe" AS info;',
    'ALTER TABLE movimientos_inventario ADD COLUMN idempotency_key VARCHAR(64) NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- C) Índice único uk_movimientos_inventario_idempotency si no existe
SET @idx := 'uk_movimientos_inventario_idempotency';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema
        AND table_name = 'movimientos_inventario'
        AND index_name = @idx
    ),
    'SELECT "OK: index uk_movimientos_inventario_idempotency ya existe" AS info;',
    'CREATE UNIQUE INDEX uk_movimientos_inventario_idempotency ON movimientos_inventario (idempotency_key);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

