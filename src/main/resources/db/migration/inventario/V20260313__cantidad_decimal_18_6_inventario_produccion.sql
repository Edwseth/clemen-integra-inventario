ALTER TABLE lotes_productos
    MODIFY COLUMN stock_lote DECIMAL(18,6) NOT NULL;

ALTER TABLE movimientos_inventario
    MODIFY COLUMN cantidad DECIMAL(18,6) NOT NULL;

ALTER TABLE cierres_produccion
    MODIFY COLUMN cantidad DECIMAL(18,6) NOT NULL;

ALTER TABLE ajustes_inventario
    MODIFY COLUMN cantidad DECIMAL(18,6) NOT NULL;

ALTER TABLE solicitudes_movimiento
    MODIFY COLUMN cantidad DECIMAL(18,6) NOT NULL;

ALTER TABLE orden_produccion
    MODIFY COLUMN cantidad_programada DECIMAL(18,6) NOT NULL,
    MODIFY COLUMN cantidad_producida DECIMAL(18,6) NOT NULL DEFAULT 0,
    MODIFY COLUMN cantidad_producida_acumulada DECIMAL(18,6) NOT NULL DEFAULT 0,
    MODIFY COLUMN porcentaje_cumplimiento DECIMAL(18,6) NULL;

ALTER TABLE regularizaciones_trazabilidad
    MODIFY COLUMN cantidad_programada DECIMAL(18,6) NOT NULL,
    MODIFY COLUMN cantidad_real DECIMAL(18,6) NOT NULL,
    MODIFY COLUMN diferencia DECIMAL(18,6) NOT NULL;

ALTER TABLE regularizacion_detalle
    MODIFY COLUMN cantidad DECIMAL(18,6) NOT NULL;
