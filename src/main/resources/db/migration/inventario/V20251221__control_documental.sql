-- V20251221 - documentos + documentos_versiones (idempotente)

SET @schema := DATABASE();

-- A) documentos (ya es IF NOT EXISTS, lo dejamos)
CREATE TABLE IF NOT EXISTS documentos (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(50) NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  tipo VARCHAR(30) NOT NULL,
  area VARCHAR(30) NOT NULL,
  estado VARCHAR(20) NOT NULL,
  descripcion TEXT NULL,
  fecha_creacion DATETIME NOT NULL,
  creado_por_id BIGINT NOT NULL,
  activo TINYINT NOT NULL DEFAULT 1,
  CONSTRAINT fk_documentos_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuarios(id),
  UNIQUE KEY uk_documentos_codigo (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Índices documentos
SET @idx := 'idx_documentos_tipo_area_estado';
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=@schema AND table_name='documentos' AND index_name=@idx),
    'SELECT "OK: idx_documentos_tipo_area_estado ya existe" AS info;',
    'CREATE INDEX idx_documentos_tipo_area_estado ON documentos (tipo, area, estado);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := 'idx_documentos_codigo';
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=@schema AND table_name='documentos' AND index_name=@idx),
    'SELECT "OK: idx_documentos_codigo ya existe" AS info;',
    'CREATE INDEX idx_documentos_codigo ON documentos (codigo);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := 'idx_documentos_nombre';
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=@schema AND table_name='documentos' AND index_name=@idx),
    'SELECT "OK: idx_documentos_nombre ya existe" AS info;',
    'CREATE INDEX idx_documentos_nombre ON documentos (nombre);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- B) documentos_versiones (ya es IF NOT EXISTS, lo dejamos)
CREATE TABLE IF NOT EXISTS documentos_versiones (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  documento_id BIGINT NOT NULL,
  numero_version INT NOT NULL,
  fecha_emision DATETIME NOT NULL,
  ruta_archivo VARCHAR(500) NOT NULL,
  nombre_archivo_original VARCHAR(255) NOT NULL,
  nombre_visible VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NULL,
  tamano_bytes BIGINT NULL,
  comentarios TEXT NULL,
  emitido_por_id BIGINT NOT NULL,
  vigente TINYINT NOT NULL DEFAULT 1,
  CONSTRAINT fk_documentos_version_documento FOREIGN KEY (documento_id) REFERENCES documentos(id),
  CONSTRAINT fk_documentos_version_emitido_por FOREIGN KEY (emitido_por_id) REFERENCES usuarios(id),
  UNIQUE KEY uk_documentos_version (documento_id, numero_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @idx := 'idx_documentos_version_documento';
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=@schema AND table_name='documentos_versiones' AND index_name=@idx),
    'SELECT "OK: idx_documentos_version_documento ya existe" AS info;',
    'CREATE INDEX idx_documentos_version_documento ON documentos_versiones (documento_id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
