-- Permitir múltiples evaluaciones del mismo tipo por lote
ALTER TABLE evaluaciones_calidad DROP INDEX uk_lote_tipo_evaluacion;

-- Optimizar consultas por lote y orden cronológico
CREATE INDEX idx_eval_lote_fecha ON evaluaciones_calidad (lotes_productos_id, fecha_evaluacion DESC);
