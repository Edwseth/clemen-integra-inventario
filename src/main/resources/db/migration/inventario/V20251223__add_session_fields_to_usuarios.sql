-- V20251223 - session_version + ultima_actividad en usuarios (idempotente)

SET @schema := DATABASE();

-- session_version
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=@schema AND table_name='usuarios' AND column_name='session_version'),
    'SELECT "OK: usuarios.session_version ya existe" AS info;',
    'ALTER TABLE usuarios ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ultima_actividad
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=@schema AND table_name='usuarios' AND column_name='ultima_actividad'),
    'SELECT "OK: usuarios.ultima_actividad ya existe" AS info;',
    'ALTER TABLE usuarios ADD COLUMN ultima_actividad DATETIME(6) NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

