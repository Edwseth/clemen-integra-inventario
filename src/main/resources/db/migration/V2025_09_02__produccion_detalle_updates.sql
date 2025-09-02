ALTER TABLE cierres_produccion
    ADD COLUMN usuario_id BIGINT,
    ADD COLUMN usuario_nombre VARCHAR(100),
    ADD INDEX idx_cierres_produccion_fecha (fecha_cierre);

ALTER TABLE movimientos_inventario
    ADD COLUMN orden_produccion_id BIGINT NULL,
    ADD INDEX idx_mov_inv_orden (orden_produccion_id),
    ADD CONSTRAINT fk_mov_inv_orden FOREIGN KEY (orden_produccion_id) REFERENCES orden_produccion(id);

ALTER TABLE etapa_produccion
    ADD COLUMN estado VARCHAR(32) NOT NULL DEFAULT 'PENDIENTE',
    ADD COLUMN fecha_inicio DATETIME NULL,
    ADD COLUMN fecha_fin DATETIME NULL;
