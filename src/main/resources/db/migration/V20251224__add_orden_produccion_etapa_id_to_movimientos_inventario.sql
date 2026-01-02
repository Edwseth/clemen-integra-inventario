-- V20251224 - movimientos_inventario.orden_produccion_etapa_id + FK + índice (idempotente)

SET @schema := DATABASE();

-- A) Columna
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=@schema AND table_name='movimientos_inventario' AND column_name='orden_produccion_etapa_id'),
    'SELECT "OK: movimientos_inventario.orden_produccion_etapa_id ya existe" AS info;',
    'ALTER TABLE movimientos_inventario ADD COLUMN orden_produccion_etapa_id BIGINT NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- B) FK
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.table_constraints
      WHERE table_schema=@schema AND table_name='movimientos_inventario'
        AND constraint_name='fk_movimientos_inventario_orden_produccion_etapa'
        AND constraint_type='FOREIGN KEY'
    ),
    'SELECT "OK: FK fk_movimientos_inventario_orden_produccion_etapa ya existe" AS info;',
    'ALTER TABLE movimientos_inventario
       ADD CONSTRAINT fk_movimientos_inventario_orden_produccion_etapa
       FOREIGN KEY (orden_produccion_etapa_id) REFERENCES etapa_produccion (id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- C) Índice
SET @idx := 'idx_mov_op_etapa';
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=@schema AND table_name='movimientos_inventario' AND index_name=@idx),
    'SELECT "OK: idx_mov_op_etapa ya existe" AS info;',
    'CREATE INDEX idx_mov_op_etapa ON movimientos_inventario (orden_produccion_id, orden_produccion_etapa_id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
