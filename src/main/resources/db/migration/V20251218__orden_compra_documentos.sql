-- V20251218 - orden_compra_documentos (idempotente)

SET @schema := DATABASE();

-- A) Tabla
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = @schema AND table_name = 'orden_compra_documentos'
    ),
    'SELECT "OK: tabla orden_compra_documentos ya existe" AS info;',
    'CREATE TABLE orden_compra_documentos (
        id BIGINT NOT NULL AUTO_INCREMENT,
        orden_compra_id BIGINT NOT NULL,
        tipo_documento VARCHAR(20) NOT NULL,
        nombre_visible VARCHAR(255) NOT NULL,
        nombre_archivo VARCHAR(255) NOT NULL,
        content_type VARCHAR(100),
        size BIGINT,
        storage_path VARCHAR(500) NOT NULL,
        creado_por_id BIGINT NOT NULL,
        fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (id)
     ) ENGINE=InnoDB;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- B) FK orden_compra_id -> ordenes_compra
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.table_constraints
      WHERE table_schema = @schema
        AND table_name = 'orden_compra_documentos'
        AND constraint_name = 'fk_oc_documento_oc'
        AND constraint_type = 'FOREIGN KEY'
    ),
    'SELECT "OK: FK fk_oc_documento_oc ya existe" AS info;',
    'ALTER TABLE orden_compra_documentos
       ADD CONSTRAINT fk_oc_documento_oc
       FOREIGN KEY (orden_compra_id) REFERENCES ordenes_compra (id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- C) FK creado_por_id -> usuarios
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.table_constraints
      WHERE table_schema = @schema
        AND table_name = 'orden_compra_documentos'
        AND constraint_name = 'fk_oc_documento_usuario'
        AND constraint_type = 'FOREIGN KEY'
    ),
    'SELECT "OK: FK fk_oc_documento_usuario ya existe" AS info;',
    'ALTER TABLE orden_compra_documentos
       ADD CONSTRAINT fk_oc_documento_usuario
       FOREIGN KEY (creado_por_id) REFERENCES usuarios (id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- D) Índices
SET @idx := 'idx_oc_documentos_oc';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema AND table_name = 'orden_compra_documentos' AND index_name = @idx
    ),
    'SELECT "OK: idx_oc_documentos_oc ya existe" AS info;',
    'CREATE INDEX idx_oc_documentos_oc ON orden_compra_documentos (orden_compra_id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := 'idx_oc_documentos_tipo';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema AND table_name = 'orden_compra_documentos' AND index_name = @idx
    ),
    'SELECT "OK: idx_oc_documentos_tipo ya existe" AS info;',
    'CREATE INDEX idx_oc_documentos_tipo ON orden_compra_documentos (tipo_documento);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
