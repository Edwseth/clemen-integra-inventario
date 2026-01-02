-- Compatibilidad: si los campos ya existen (entornos que aplicaron la migración previa)
-- no se intenta recrearlos.

SET @add_codigo_ubicacion_interna = (
    SELECT IF(
            EXISTS(
                SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'lotes_productos'
                  AND COLUMN_NAME = 'codigo_ubicacion_interna'
            ),
            'SELECT 1',
            'ALTER TABLE lotes_productos ADD COLUMN codigo_ubicacion_interna VARCHAR(50) NULL'
        )
);
PREPARE stmt FROM @add_codigo_ubicacion_interna;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_descripcion_ubicacion_interna = (
    SELECT IF(
            EXISTS(
                SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'lotes_productos'
                  AND COLUMN_NAME = 'descripcion_ubicacion_interna'
            ),
            'SELECT 1',
            'ALTER TABLE lotes_productos ADD COLUMN descripcion_ubicacion_interna VARCHAR(255) NULL'
        )
);
PREPARE stmt FROM @add_descripcion_ubicacion_interna;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
