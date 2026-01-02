-- V20251213 - version en plantillas_analisis_micro + tipo_incidente en no_conformidad (idempotente)

-- A) plantillas_analisis_micro.version
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'plantillas_analisis_micro'
        AND column_name = 'version'
    ),
    'SELECT "OK: plantillas_analisis_micro.version ya existe" AS info;',
    'ALTER TABLE plantillas_analisis_micro ADD COLUMN version INT NOT NULL DEFAULT 1;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- B) no_conformidad.tipo_incidente
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'no_conformidad'
        AND column_name = 'tipo_incidente'
    ),
    'SELECT "OK: no_conformidad.tipo_incidente ya existe" AS info;',
    'ALTER TABLE no_conformidad ADD COLUMN tipo_incidente VARCHAR(30) NOT NULL DEFAULT ''NO_CONFORMIDAD'';'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- C) Opcional: normalizar valores existentes
UPDATE no_conformidad
SET tipo_incidente = 'NO_CONFORMIDAD'
WHERE tipo_incidente IS NULL OR tipo_incidente = '';

-- D) Nota técnica:
-- Evito ENUM aquí para no romper por diferencias de engines/migraciones previas.
-- Si realmente quieres ENUM, debe ser otro ALTER que verifique el tipo actual y convierta.

