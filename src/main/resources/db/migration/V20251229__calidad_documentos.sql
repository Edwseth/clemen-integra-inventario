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

SET @idx_doc_tipo_estado = (
    SELECT IF(
            EXISTS(
                SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'documentos_calidad'
                  AND INDEX_NAME = 'idx_documentos_calidad_tipo_estado'
            ),
            'SELECT 1',
            'CREATE INDEX idx_documentos_calidad_tipo_estado ON documentos_calidad (tipo, estado)'
        )
);
PREPARE stmt FROM @idx_doc_tipo_estado;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_doc_codigo = (
    SELECT IF(
            EXISTS(
                SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'documentos_calidad'
                  AND INDEX_NAME = 'idx_documentos_calidad_codigo'
            ),
            'SELECT 1',
            'CREATE INDEX idx_documentos_calidad_codigo ON documentos_calidad (codigo)'
        )
);
PREPARE stmt FROM @idx_doc_codigo;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_doc_nombre = (
    SELECT IF(
            EXISTS(
                SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'documentos_calidad'
                  AND INDEX_NAME = 'idx_documentos_calidad_nombre'
            ),
            'SELECT 1',
            'CREATE INDEX idx_documentos_calidad_nombre ON documentos_calidad (nombre)'
        )
);
PREPARE stmt FROM @idx_doc_nombre;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_doc_lote = (
    SELECT IF(
            EXISTS(
                SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'documentos_calidad'
                  AND INDEX_NAME = 'idx_documentos_calidad_lote'
            ),
            'SELECT 1',
            'CREATE INDEX idx_documentos_calidad_lote ON documentos_calidad (lote_id)'
        )
);
PREPARE stmt FROM @idx_doc_lote;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

SET @idx_doc_version_doc = (
    SELECT IF(
            EXISTS(
                SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'documentos_calidad_versiones'
                  AND INDEX_NAME = 'idx_documentos_calidad_version_doc'
            ),
            'SELECT 1',
            'CREATE INDEX idx_documentos_calidad_version_doc ON documentos_calidad_versiones (documento_id)'
        )
);
PREPARE stmt FROM @idx_doc_version_doc;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @uk_doc_version = (
    SELECT IF(
            EXISTS(
                SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'documentos_calidad_versiones'
                  AND INDEX_NAME = 'uk_documentos_calidad_version'
            ),
            'SELECT 1',
            'CREATE UNIQUE INDEX uk_documentos_calidad_version ON documentos_calidad_versiones (documento_id, version)'
        )
);
PREPARE stmt FROM @uk_doc_version;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
