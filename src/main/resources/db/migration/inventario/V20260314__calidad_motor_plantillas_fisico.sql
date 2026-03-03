CREATE TABLE plantillas_analisis (
    id BIGINT NOT NULL AUTO_INCREMENT,
    producto_id INT NOT NULL,
    tipo_analisis VARCHAR(20) NOT NULL,
    nombre VARCHAR(255) NULL,
    version INT NOT NULL,
    vigente BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    created_by_id INT NULL,
    updated_at DATETIME(6) NULL,
    updated_by_id INT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_plantillas_analisis_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_plantillas_analisis_created_by FOREIGN KEY (created_by_id) REFERENCES usuarios (id),
    CONSTRAINT fk_plantillas_analisis_updated_by FOREIGN KEY (updated_by_id) REFERENCES usuarios (id),
    CONSTRAINT uk_plantillas_analisis_producto_tipo_version UNIQUE (producto_id, tipo_analisis, version)
);

CREATE INDEX idx_plantillas_analisis_producto_tipo_vigente
    ON plantillas_analisis (producto_id, tipo_analisis, vigente);

CREATE TABLE plantilla_campos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    plantilla_id BIGINT NOT NULL,
    codigo VARCHAR(80) NOT NULL,
    label VARCHAR(255) NOT NULL,
    tipo_campo VARCHAR(30) NOT NULL,
    requerido BOOLEAN NOT NULL DEFAULT TRUE,
    orden INT NOT NULL,
    config_json TEXT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    created_by_id BIGINT NULL,
    updated_at DATETIME(6) NULL,
    updated_by_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_plantilla_campos_plantilla FOREIGN KEY (plantilla_id) REFERENCES plantillas_analisis (id) ON DELETE CASCADE,
    CONSTRAINT fk_plantilla_campos_created_by FOREIGN KEY (created_by_id) REFERENCES usuarios (id),
    CONSTRAINT fk_plantilla_campos_updated_by FOREIGN KEY (updated_by_id) REFERENCES usuarios (id),
    CONSTRAINT uk_plantilla_campos_plantilla_codigo UNIQUE (plantilla_id, codigo)
);

CREATE INDEX idx_plantilla_campos_plantilla_orden
    ON plantilla_campos (plantilla_id, orden);
