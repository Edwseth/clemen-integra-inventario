ALTER TABLE vida_util_productos
    ADD COLUMN actualizado_por_id BIGINT NULL,
    ADD COLUMN fecha_actualizacion DATETIME NULL,
    ADD CONSTRAINT fk_vida_util_productos_usuario FOREIGN KEY (actualizado_por_id)
        REFERENCES usuarios (id);
