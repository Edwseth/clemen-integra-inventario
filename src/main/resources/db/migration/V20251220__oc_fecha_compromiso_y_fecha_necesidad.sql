ALTER TABLE ordenes_compra
    ADD COLUMN fecha_compromiso_entrega DATE NULL;

ALTER TABLE orden_compra_detalle
    ADD COLUMN fecha_necesidad DATE NULL;

CREATE INDEX idx_oc_estado_fecha ON ordenes_compra (estado, fecha_compromiso_entrega);
CREATE INDEX idx_ocd_oc ON orden_compra_detalle (ordenes_compra_id);
