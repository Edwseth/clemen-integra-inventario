-- Persistencia de confirmación y motivo de override homeopático en orden_produccion
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='orden_produccion' AND column_name='confirmacion_homeopatico');
SET @sql := IF(@c=0,
 'ALTER TABLE orden_produccion ADD COLUMN confirmacion_homeopatico TINYINT(1) NOT NULL DEFAULT 0 AFTER unidad_medida_id',
 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='orden_produccion' AND column_name='motivo_override_homeopatico');
SET @sql := IF(@c=0,
 'ALTER TABLE orden_produccion ADD COLUMN motivo_override_homeopatico VARCHAR(500) NULL AFTER confirmacion_homeopatico',
 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Ajusta longitud por compatibilidad con validación de API
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE()
             AND table_name='orden_produccion'
             AND column_name='motivo_override_homeopatico'
             AND CHARACTER_MAXIMUM_LENGTH IS NOT NULL
             AND CHARACTER_MAXIMUM_LENGTH < 500);
SET @sql := IF(@c>0,
 'ALTER TABLE orden_produccion MODIFY COLUMN motivo_override_homeopatico VARCHAR(500) NULL',
 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
