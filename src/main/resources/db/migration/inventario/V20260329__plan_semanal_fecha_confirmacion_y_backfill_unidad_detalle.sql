ALTER TABLE plan_produccion_semanal
    ADD COLUMN fecha_confirmacion DATETIME NULL AFTER fecha_creacion;

UPDATE plan_produccion_detalle d
JOIN productos p ON p.id = d.producto_id
SET d.unidad_medida_id = p.unidades_medida_id
WHERE d.unidad_medida_id IS NULL
  AND p.unidades_medida_id IS NOT NULL;
