-- V20251123 - add lote ps origen a lotes_productos (idempotente)

-- 1) Columna lote_ps_origen_id (solo si no existe)
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'lotes_productos'
        AND column_name = 'lote_ps_origen_id'
    ),
    'SELECT "OK: columna lote_ps_origen_id ya existe" AS info;',
    'ALTER TABLE lotes_productos ADD COLUMN lote_ps_origen_id BIGINT NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) FK (solo si no existe)
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.table_constraints
      WHERE table_schema = DATABASE()
        AND table_name = 'lotes_productos'
        AND constraint_name = 'fk_lotes_productos_ps_origen'
        AND constraint_type = 'FOREIGN KEY'
    ),
    'SELECT "OK: FK fk_lotes_productos_ps_origen ya existe" AS info;',
    'ALTER TABLE lotes_productos
       ADD CONSTRAINT fk_lotes_productos_ps_origen
       FOREIGN KEY (lote_ps_origen_id) REFERENCES lotes_productos(id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Índice (solo si no existe)
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'lotes_productos'
        AND index_name = 'idx_lotes_productos_ps_origen'
    ),
    'SELECT "OK: index idx_lotes_productos_ps_origen ya existe" AS info;',
    'CREATE INDEX idx_lotes_productos_ps_origen ON lotes_productos(lote_ps_origen_id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
