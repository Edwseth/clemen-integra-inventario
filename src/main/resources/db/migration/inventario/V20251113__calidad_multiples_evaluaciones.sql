-- Optimizar consultas por lote y orden cronológico
-- Crear índice solo si NO existe (MySQL no soporta CREATE INDEX IF NOT EXISTS)
SET @idx2 := 'idx_eval_lote_fecha';

SET @sql2 := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'evaluaciones_calidad'
        AND index_name = @idx2
    ),
    'SELECT "OK: index idx_eval_lote_fecha ya existe, se omite" AS info;',
    'CREATE INDEX idx_eval_lote_fecha ON evaluaciones_calidad (lotes_productos_id, fecha_evaluacion);'
  )
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
