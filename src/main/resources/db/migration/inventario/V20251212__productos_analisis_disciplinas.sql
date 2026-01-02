-- V20251212 - banderas independientes por disciplina de análisis (idempotente)

-- A) Agregar columnas si no existen
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'productos'
        AND column_name = 'requiere_analisis_fisico'
    ),
    'SELECT "OK: requiere_analisis_fisico ya existe" AS info;',
    'ALTER TABLE productos ADD COLUMN requiere_analisis_fisico TINYINT(1) NOT NULL DEFAULT 0;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'productos'
        AND column_name = 'requiere_analisis_quimico'
    ),
    'SELECT "OK: requiere_analisis_quimico ya existe" AS info;',
    'ALTER TABLE productos ADD COLUMN requiere_analisis_quimico TINYINT(1) NOT NULL DEFAULT 0;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'productos'
        AND column_name = 'requiere_analisis_microbiologico'
    ),
    'SELECT "OK: requiere_analisis_microbiologico ya existe" AS info;',
    'ALTER TABLE productos ADD COLUMN requiere_analisis_microbiologico TINYINT(1) NOT NULL DEFAULT 0;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- B) Poblar banderas (idempotente por reglas: recalcula siempre)
-- Material de Empaque (ME): FISICO + MICRO, excepto etiquetas ET%
UPDATE productos p
JOIN categorias_producto c ON p.categorias_producto_id = c.id
SET p.requiere_analisis_fisico = 1,
    p.requiere_analisis_quimico = 0,
    p.requiere_analisis_microbiologico = 1
WHERE c.nombre = 'Material de Empaque'
  AND (p.codigo_sku IS NULL OR p.codigo_sku NOT LIKE 'ET%');

-- Etiquetas ET%: solo FISICO
UPDATE productos p
JOIN categorias_producto c ON p.categorias_producto_id = c.id
SET p.requiere_analisis_fisico = 1,
    p.requiere_analisis_quimico = 0,
    p.requiere_analisis_microbiologico = 0
WHERE c.nombre = 'Material de Empaque'
  AND p.codigo_sku LIKE 'ET%';

-- Materia Prima / PT / PS / SM: QUIMICO + MICRO
UPDATE productos p
JOIN categorias_producto c ON p.categorias_producto_id = c.id
SET p.requiere_analisis_fisico = 0,
    p.requiere_analisis_quimico = 1,
    p.requiere_analisis_microbiologico = 1
WHERE c.nombre IN ('Materia Prima', 'Producto Terminado', 'Producto Semielaborado', 'SM');
