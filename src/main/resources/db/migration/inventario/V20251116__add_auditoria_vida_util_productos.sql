-- vida_util_productos: auditoría de actualización + FK a usuarios
-- Idempotente (evita Duplicate column / Duplicate key / Can't create constraint)

-- 1) Columna actualizado_por_id
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'vida_util_productos'
        AND column_name = 'actualizado_por_id'
    ),
    'SELECT "OK: columna actualizado_por_id ya existe" AS info;',
    'ALTER TABLE vida_util_productos ADD COLUMN actualizado_por_id BIGINT NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Columna fecha_actualizacion
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'vida_util_productos'
        AND column_name = 'fecha_actualizacion'
    ),
    'SELECT "OK: columna fecha_actualizacion ya existe" AS info;',
    'ALTER TABLE vida_util_productos ADD COLUMN fecha_actualizacion DATETIME NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Índice para la FK (recomendado para evitar problemas y mejorar joins)
SET @idx := 'idx_vu_actualizado_por_id';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'vida_util_productos'
        AND index_name = @idx
    ),
    'SELECT "OK: index idx_vu_actualizado_por_id ya existe" AS info;',
    'CREATE INDEX idx_vu_actualizado_por_id ON vida_util_productos(actualizado_por_id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) FK (si ya existe, se omite)
SET @fk := 'fk_vida_util_productos_usuario';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.table_constraints
      WHERE constraint_schema = DATABASE()
        AND table_name = 'vida_util_productos'
        AND constraint_name = @fk
        AND constraint_type = 'FOREIGN KEY'
    ),
    'SELECT "OK: FK fk_vida_util_productos_usuario ya existe" AS info;',
    'ALTER TABLE vida_util_productos
       ADD CONSTRAINT fk_vida_util_productos_usuario
       FOREIGN KEY (actualizado_por_id) REFERENCES usuarios(id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

