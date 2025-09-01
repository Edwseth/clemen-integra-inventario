ALTER TABLE productos ADD COLUMN IF NOT EXISTS rendimiento_unidad DECIMAL(19,6) NULL;
UPDATE productos SET rendimiento_unidad = 0 WHERE rendimiento_unidad IS NULL;
UPDATE productos
SET rendimiento_unidad = 4.000000
WHERE LOWER(nombre) LIKE LOWER('%jarabe vitamina c%250%')
  AND (rendimiento_unidad IS NULL OR rendimiento_unidad = 0);
