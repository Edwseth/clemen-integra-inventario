-- Índices para soportar consultas paginadas y filtros recientes

SET @create_idx_lotes_estado_producto = (
    SELECT IF(
            EXISTS(
                SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'lotes_productos'
                  AND INDEX_NAME = 'idx_lotes_estado_producto'
            ),
            'SELECT 1',
            'CREATE INDEX idx_lotes_estado_producto ON lotes_productos (estado, productos_id)'
        )
);
PREPARE stmt FROM @create_idx_lotes_estado_producto;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @create_idx_evaluaciones_fecha = (
    SELECT IF(
            EXISTS(
                SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'evaluaciones_calidad'
                  AND INDEX_NAME = 'idx_evaluaciones_fecha'
            ),
            'SELECT 1',
            'CREATE INDEX idx_evaluaciones_fecha ON evaluaciones_calidad (fecha_evaluacion)'
        )
);
PREPARE stmt FROM @create_idx_evaluaciones_fecha;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
