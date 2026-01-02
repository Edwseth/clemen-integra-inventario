-- V20251219 - capa.fecha_limite (idempotente)

SET @schema := DATABASE();

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = @schema AND table_name = 'capa' AND column_name = 'fecha_limite'
    ),
    'SELECT "OK: columna capa.fecha_limite ya existe" AS info;',
    'ALTER TABLE capa ADD COLUMN fecha_limite DATETIME(6) NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

