-- Asegura columnas homeopáticas en orden_produccion para compatibilidad entre ambientes

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'orden_produccion'
      AND column_name = 'confirmacion_homeopatico'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE orden_produccion ADD COLUMN confirmacion_homeopatico TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'orden_produccion'
      AND column_name = 'motivo_override_homeopatico'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE orden_produccion ADD COLUMN motivo_override_homeopatico VARCHAR(500) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
