ALTER TABLE lotes_productos
    ADD COLUMN costo_unitario_material DECIMAL(19,6) NULL,
    ADD COLUMN costo_total_material_ingresado DECIMAL(19,6) NULL;

ALTER TABLE movimientos_inventario
    ADD COLUMN costo_unitario_aplicado DECIMAL(19,6) NULL,
    ADD COLUMN costo_total_aplicado DECIMAL(19,6) NULL;

ALTER TABLE recepciones_oc
    ADD COLUMN gastos_adicionales_total DECIMAL(19,6) NULL DEFAULT 0,
    ADD COLUMN criterio_prorrateo VARCHAR(20) NULL DEFAULT 'VALOR';
