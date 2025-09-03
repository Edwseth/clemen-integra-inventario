CREATE TABLE IF NOT EXISTS etapa_plantilla (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  producto_id INT NOT NULL,
  nombre VARCHAR(150) NOT NULL,
  secuencia INT NOT NULL,
  activo TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_etapa_plantilla_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
  INDEX idx_etapa_plantilla_producto (producto_id),
  INDEX idx_etapa_plantilla_prod_seq (producto_id, secuencia)
);

ALTER TABLE etapa_produccion
    ADD COLUMN usuario_id BIGINT NULL,
    ADD COLUMN usuario_nombre VARCHAR(100) NULL;
