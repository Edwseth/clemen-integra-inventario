-- V20251220 - fechas compromiso/necesidad + índices (idempotente)

SET @schema := DATABASE();

-- A) ordenes_compra.fecha_compromiso_entrega
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = @schema AND table_name = 'ordenes_compra' AND column_name = 'fecha_compromiso_entrega'
    ),
    'SELECT "OK: ordenes_compra.fecha_compromiso_entrega ya existe" AS info;',
    'ALTER TABLE ordenes_compra ADD COLUMN fecha_compromiso_entrega DATE NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- B) orden_compra_detalle.fecha_necesidad
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = @schema AND table_name = 'orden_compra_detalle' AND column_name = 'fecha_necesidad'
    ),
    'SELECT "OK: orden_compra_detalle.fecha_necesidad ya existe" AS info;',
    'ALTER TABLE orden_compra_detalle ADD COLUMN fecha_necesidad DATE NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- C) idx_oc_estado_fecha
SET @idx := 'idx_oc_estado_fecha';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema AND table_name = 'ordenes_compra' AND index_name = @idx
    ),
    'SELECT "OK: idx_oc_estado_fecha ya existe" AS info;',
    'CREATE INDEX idx_oc_estado_fecha ON ordenes_compra (estado, fecha_compromiso_entrega);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- D) idx_ocd_oc
SET @idx := 'idx_ocd_oc';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema AND table_name = 'orden_compra_detalle' AND index_name = @idx
    ),
    'SELECT "OK: idx_ocd_oc ya existe" AS info;',
    'CREATE INDEX idx_ocd_oc ON orden_compra_detalle (ordenes_compra_id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
