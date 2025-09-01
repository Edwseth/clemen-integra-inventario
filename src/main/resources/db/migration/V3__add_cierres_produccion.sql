CREATE TABLE cierres_produccion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    orden_produccion_id BIGINT NOT NULL,
    cantidad DECIMAL(10,2) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    cerrada_incompleta BIT,
    turno VARCHAR(50),
    observacion VARCHAR(255),
    fecha_cierre DATETIME NOT NULL,
    CONSTRAINT fk_cierre_orden FOREIGN KEY (orden_produccion_id) REFERENCES orden_produccion(id)
);

ALTER TABLE orden_produccion ADD COLUMN cantidad_producida_acumulada DECIMAL(10,2);
ALTER TABLE orden_produccion ADD COLUMN fecha_ultimo_cierre DATETIME;
ALTER TABLE orden_produccion ADD COLUMN version BIGINT;
