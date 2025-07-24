ALTER TABLE productos
    DROP COLUMN IF EXISTS requiere_inspeccion;
ALTER TABLE productos
    ADD COLUMN tipo_analisis VARCHAR(30) NOT NULL DEFAULT 'NINGUNO';