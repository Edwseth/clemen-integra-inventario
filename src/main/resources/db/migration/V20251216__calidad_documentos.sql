CREATE TABLE IF NOT EXISTS documentos_calidad (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tipo ENUM('SANITIZACION','CALIBRACION','OTRO') NOT NULL,
    codigo VARCHAR(60) NULL,
    nombre VARCHAR(255) NOT NULL,
    estado ENUM('VIGENTE','OBSOLETO') NOT NULL DEFAULT 'VIGENTE',
    lote_id BIGINT NULL,
    creado_por_id BIGINT NULL,
    fecha_creacion DATETIME NOT NULL,
    actualizado_por_id BIGINT NULL,
    fecha_actualizacion DATETIME NULL,
    CONSTRAINT fk_documento_calidad_lote FOREIGN KEY (lote_id) REFERENCES lotes_productos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_documentos_calidad_tipo_estado ON documentos_calidad (tipo, estado);
CREATE INDEX idx_documentos_calidad_codigo ON documentos_calidad (codigo);
CREATE INDEX idx_documentos_calidad_nombre ON documentos_calidad (nombre);
CREATE INDEX idx_documentos_calidad_lote ON documentos_calidad (lote_id);

CREATE TABLE IF NOT EXISTS documentos_calidad_versiones (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    version INT NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    nombre_visible VARCHAR(255) NULL,
    content_type VARCHAR(150) NULL,
    size_bytes BIGINT NULL,
    storage_path VARCHAR(512) NOT NULL,
    hash_opcional VARCHAR(128) NULL,
    creado_por_id BIGINT NULL,
    fecha_creacion DATETIME NOT NULL,
    CONSTRAINT fk_documento_calidad_version_documento FOREIGN KEY (documento_id) REFERENCES documentos_calidad(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_documentos_calidad_version_doc ON documentos_calidad_versiones (documento_id);
CREATE UNIQUE INDEX uk_documentos_calidad_version ON documentos_calidad_versiones (documento_id, version);
