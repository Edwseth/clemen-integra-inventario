ALTER TABLE especificaciones_calidad
    MODIFY COLUMN valor_minimo DECIMAL(10,2) NOT NULL;
ALTER TABLE especificaciones_calidad
    MODIFY COLUMN valor_maximo DECIMAL(10,2) NOT NULL;
ALTER TABLE especificaciones_calidad
    ADD COLUMN unidad VARCHAR(45) NOT NULL;
