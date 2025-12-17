-- Índices para soportar consultas paginadas y filtros recientes
CREATE INDEX IF NOT EXISTS idx_lotes_estado_producto ON lotes_productos (estado, productos_id);
CREATE INDEX IF NOT EXISTS idx_evaluaciones_fecha ON evaluaciones_calidad (fecha_evaluacion);
