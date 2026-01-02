-- Crear índice solo si NO existe (MySQL no soporta CREATE INDEX IF NOT EXISTS)
SET @idx := 'idx_proveedores_nombre';

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'proveedores'
        AND index_name = @idx
    ),
    'SELECT "OK: index idx_proveedores_nombre ya existe, se omite" AS info;',
    'CREATE INDEX idx_proveedores_nombre ON proveedores (proveedor);'
  )
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

