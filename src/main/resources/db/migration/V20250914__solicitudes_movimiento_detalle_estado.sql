ALTER TABLE solicitudes_movimiento_detalle
    ADD COLUMN estado VARCHAR(20);

UPDATE solicitudes_movimiento_detalle
SET estado = 'PENDIENTE'
WHERE estado IS NULL;

ALTER TABLE solicitudes_movimiento_detalle
    ALTER COLUMN estado SET NOT NULL;
