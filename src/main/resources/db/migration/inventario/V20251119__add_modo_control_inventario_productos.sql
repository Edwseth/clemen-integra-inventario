-- V20251119 - productos modo_control_inventario (idempotente)

-- 1) Columna
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'productos'
        AND column_name = 'modo_control_inventario'
    ),
    'SELECT "OK: columna modo_control_inventario ya existe" AS info;',
    'ALTER TABLE productos
       ADD COLUMN modo_control_inventario VARCHAR(30) NOT NULL DEFAULT ''CONTROL_STOCK'';'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Normalización (seguro repetir)
UPDATE productos
SET modo_control_inventario = 'CONTROL_STOCK'
WHERE modo_control_inventario IS NULL;

UPDATE productos
SET modo_control_inventario = 'SIN_CONTROL_STOCK'
WHERE codigo_sku = 'SM0001';
