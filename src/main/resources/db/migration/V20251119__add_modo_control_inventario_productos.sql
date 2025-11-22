ALTER TABLE productos
    ADD COLUMN modo_control_inventario VARCHAR(30) NOT NULL DEFAULT 'CONTROL_STOCK';

UPDATE productos
SET modo_control_inventario = 'CONTROL_STOCK'
WHERE modo_control_inventario IS NULL;

UPDATE productos
SET modo_control_inventario = 'SIN_CONTROL_STOCK'
WHERE codigo_sku = 'SM0001';
