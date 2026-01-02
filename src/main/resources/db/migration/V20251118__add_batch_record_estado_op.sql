-- V20251118 - orden_produccion batch record campos + FK (idempotente)

-- 1) batch_record_estado
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'orden_produccion'
        AND column_name = 'batch_record_estado'
    ),
    'SELECT "OK: columna batch_record_estado ya existe" AS info;',
    'ALTER TABLE orden_produccion
       ADD COLUMN batch_record_estado VARCHAR(30) NOT NULL DEFAULT ''BORRADOR'';'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) batch_record_revisado_por_id
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'orden_produccion'
        AND column_name = 'batch_record_revisado_por_id'
    ),
    'SELECT "OK: columna batch_record_revisado_por_id ya existe" AS info;',
    'ALTER TABLE orden_produccion
       ADD COLUMN batch_record_revisado_por_id BIGINT NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) batch_record_fecha_revision
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'orden_produccion'
        AND column_name = 'batch_record_fecha_revision'
    ),
    'SELECT "OK: columna batch_record_fecha_revision ya existe" AS info;',
    'ALTER TABLE orden_produccion
       ADD COLUMN batch_record_fecha_revision DATETIME NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) batch_record_observaciones_calidad
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'orden_produccion'
        AND column_name = 'batch_record_observaciones_calidad'
    ),
    'SELECT "OK: columna batch_record_observaciones_calidad ya existe" AS info;',
    'ALTER TABLE orden_produccion
       ADD COLUMN batch_record_observaciones_calidad TEXT NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) FK fk_op_batch_record_revisado_por
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.table_constraints tc
      WHERE tc.table_schema = DATABASE()
        AND tc.table_name = 'orden_produccion'
        AND tc.constraint_name = 'fk_op_batch_record_revisado_por'
        AND tc.constraint_type = 'FOREIGN KEY'
    ),
    'SELECT "OK: FK fk_op_batch_record_revisado_por ya existe" AS info;',
    'ALTER TABLE orden_produccion
       ADD CONSTRAINT fk_op_batch_record_revisado_por
         FOREIGN KEY (batch_record_revisado_por_id) REFERENCES usuarios (id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
