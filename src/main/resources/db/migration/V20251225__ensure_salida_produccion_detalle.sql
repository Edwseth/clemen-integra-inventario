-- Garantiza que el tipo de detalle SALIDA_PRODUCCION exista para registrar consumos reales.
INSERT INTO tipos_movimiento_detalle (descripcion)
SELECT 'SALIDA_PRODUCCION'
WHERE NOT EXISTS (
    SELECT 1 FROM tipos_movimiento_detalle WHERE descripcion = 'SALIDA_PRODUCCION'
);
