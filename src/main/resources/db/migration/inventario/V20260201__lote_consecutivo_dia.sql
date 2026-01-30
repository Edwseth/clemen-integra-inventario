CREATE TABLE IF NOT EXISTS lote_consecutivo_dia (
    fecha DATE NOT NULL,
    ultimo_valor INT NOT NULL,
    PRIMARY KEY (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
