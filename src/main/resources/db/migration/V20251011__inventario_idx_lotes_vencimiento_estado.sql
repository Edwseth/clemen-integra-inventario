CREATE INDEX IF NOT EXISTS idx_lote_vencimiento_estado
    ON lotes_productos(fecha_vencimiento, estado);
