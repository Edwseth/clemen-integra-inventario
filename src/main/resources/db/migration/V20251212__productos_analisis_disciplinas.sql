-- Agregar banderas independientes para las disciplinas de análisis de calidad
ALTER TABLE productos
    ADD COLUMN requiere_analisis_fisico TINYINT(1) NOT NULL DEFAULT 0 AFTER tipo_analisis_calidad,
    ADD COLUMN requiere_analisis_quimico TINYINT(1) NOT NULL DEFAULT 0 AFTER requiere_analisis_fisico,
    ADD COLUMN requiere_analisis_microbiologico TINYINT(1) NOT NULL DEFAULT 0 AFTER requiere_analisis_quimico;

-- Poblar las nuevas banderas con reglas iniciales. El campo tipo_analisis_calidad se mantiene como legado.
-- Material de Empaque (ME): por defecto FÍSICO + MICRO (1,0,1), excepto etiquetas (código SKU que inicia en 'ET').
UPDATE productos p
JOIN categorias_producto c ON p.categorias_producto_id = c.id
SET p.requiere_analisis_fisico = 1,
    p.requiere_analisis_quimico = 0,
    p.requiere_analisis_microbiologico = 1
WHERE c.nombre = 'Material de Empaque'
  AND (p.codigo_sku IS NULL OR p.codigo_sku NOT LIKE 'ET%');

-- Etiquetas: sólo FÍSICO.
UPDATE productos p
JOIN categorias_producto c ON p.categorias_producto_id = c.id
SET p.requiere_analisis_fisico = 1,
    p.requiere_analisis_quimico = 0,
    p.requiere_analisis_microbiologico = 0
WHERE c.nombre = 'Material de Empaque'
  AND p.codigo_sku LIKE 'ET%';

-- Materia Prima, Producto Terminado, Producto Semielaborado y SM => QUÍMICO + MICRO (0,1,1).
UPDATE productos p
JOIN categorias_producto c ON p.categorias_producto_id = c.id
SET p.requiere_analisis_fisico = 0,
    p.requiere_analisis_quimico = 1,
    p.requiere_analisis_microbiologico = 1
WHERE c.nombre IN ('Materia Prima', 'Producto Terminado', 'Producto Semielaborado', 'SM');

-- Espacio para excepciones adicionales por SKU específico (rellenar según defina el cliente).
-- UPDATE productos SET requiere_analisis_fisico = ?, requiere_analisis_quimico = ?, requiere_analisis_microbiologico = ?
-- WHERE codigo_sku = 'SM001';
