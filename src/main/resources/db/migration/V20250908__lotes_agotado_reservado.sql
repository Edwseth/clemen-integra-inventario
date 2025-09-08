-- NUEVAS columnas para disponibilidad y rastro de agotamiento
ALTER TABLE lotes_productos
  ADD COLUMN agotado TINYINT(1) NOT NULL DEFAULT 0 AFTER stock_lote,
  ADD COLUMN stock_reservado DECIMAL(18,6) NOT NULL DEFAULT 0 AFTER agotado,
  ADD COLUMN fecha_agotado DATETIME NULL AFTER stock_reservado;

-- Índice para FEFO eficiente
CREATE INDEX idx_fefo_disponibles
  ON lotes_productos (producto_id, estado, fecha_vencimiento, agotado, stock_lote, stock_reservado);

-- Backfill: marcar agotados los lotes sin stock
UPDATE lotes_productos
SET agotado = 1, fecha_agotado = IFNULL(fecha_agotado, NOW())
WHERE stock_lote <= 0;
