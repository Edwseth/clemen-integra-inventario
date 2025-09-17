CREATE TABLE IF NOT EXISTS reservas_lote (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lote_id BIGINT NOT NULL,
    solicitud_movimiento_detalle_id BIGINT NOT NULL,
    cantidad_reservada DECIMAL(18,6) NOT NULL,
    cantidad_consumida DECIMAL(18,6) NOT NULL DEFAULT 0,
    estado ENUM('ACTIVA','CONSUMIDA','CANCELADA') NOT NULL DEFAULT 'ACTIVA',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservas_lote_lote FOREIGN KEY (lote_id) REFERENCES lotes_productos (id),
    CONSTRAINT fk_reservas_lote_detalle FOREIGN KEY (solicitud_movimiento_detalle_id) REFERENCES solicitudes_movimiento_detalle (id),
    CONSTRAINT ck_reservas_lote_consumo CHECK (cantidad_consumida <= cantidad_reservada)
);

CREATE INDEX IF NOT EXISTS idx_reservas_lote_lote_id ON reservas_lote (lote_id);
CREATE INDEX IF NOT EXISTS idx_reservas_lote_detalle_id ON reservas_lote (solicitud_movimiento_detalle_id);
CREATE INDEX IF NOT EXISTS idx_reservas_lote_lote_estado ON reservas_lote (lote_id, estado);

INSERT INTO reservas_lote (lote_id, solicitud_movimiento_detalle_id, cantidad_reservada, cantidad_consumida, estado, created_at, updated_at)
SELECT
    d.lote_id,
    d.id,
    COALESCE(d.cantidad, 0),
    LEAST(GREATEST(COALESCE(d.cantidad_atendida, 0), 0), COALESCE(d.cantidad, 0)),
    CASE
        WHEN LEAST(GREATEST(COALESCE(d.cantidad_atendida, 0), 0), COALESCE(d.cantidad, 0)) >= COALESCE(d.cantidad, 0)
            THEN 'CONSUMIDA'
        ELSE 'ACTIVA'
    END,
    NOW(),
    NOW()
FROM solicitudes_movimiento_detalle d
JOIN solicitudes_movimiento s ON s.id = d.solicitud_movimiento_id
LEFT JOIN reservas_lote rl ON rl.solicitud_movimiento_detalle_id = d.id
WHERE rl.id IS NULL
  AND s.estado IN ('PENDIENTE','AUTORIZADA','PARCIAL')
  AND d.lote_id IS NOT NULL
  AND COALESCE(d.cantidad, 0) > 0
  AND (COALESCE(d.cantidad, 0) - LEAST(GREATEST(COALESCE(d.cantidad_atendida, 0), 0), COALESCE(d.cantidad, 0))) > 0;

UPDATE lotes_productos lp
LEFT JOIN (
    SELECT
        lote_id,
        SUM(CASE
                WHEN estado <> 'CANCELADA'
                    THEN GREATEST(cantidad_reservada - cantidad_consumida, 0)
                ELSE 0
            END) AS reservado_pendiente
    FROM reservas_lote
    GROUP BY lote_id
) agg ON agg.lote_id = lp.id
SET lp.stock_reservado = ROUND(COALESCE(agg.reservado_pendiente, 0), 6);
