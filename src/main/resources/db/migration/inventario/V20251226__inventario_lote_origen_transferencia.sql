-- V20251226 - lotes_productos.lote_origen_id + FK (idempotente)

SET @schema := DATABASE();

-- Columna
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=@schema AND table_name='lotes_productos' AND column_name='lote_origen_id'),
    'SELECT "OK: lotes_productos.lote_origen_id ya existe" AS info;',
    'ALTER TABLE lotes_productos ADD COLUMN lote_origen_id BIGINT NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- FK
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.table_constraints
      WHERE table_schema=@schema AND table_name='lotes_productos'
        AND constraint_name='fk_lotes_productos_lote_origen'
        AND constraint_type='FOREIGN KEY'
    ),
    'SELECT "OK: FK fk_lotes_productos_lote_origen ya existe" AS info;',
    'ALTER TABLE lotes_productos
       ADD CONSTRAINT fk_lotes_productos_lote_origen
       FOREIGN KEY (lote_origen_id) REFERENCES lotes_productos (id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

