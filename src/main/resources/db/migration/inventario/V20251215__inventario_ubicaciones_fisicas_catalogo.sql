-- V20251215 - ubicaciones_fisicas + lote -> ubicacion (idempotente)

-- A) Tabla ubicaciones_fisicas
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name = 'ubicaciones_fisicas'
    ),
    'SELECT "OK: tabla ubicaciones_fisicas ya existe" AS info;',
    'CREATE TABLE ubicaciones_fisicas (
        id BIGINT NOT NULL AUTO_INCREMENT,
        almacen_id INT NOT NULL,
        codigo VARCHAR(50) NOT NULL,
        descripcion VARCHAR(150),
        activo TINYINT NOT NULL DEFAULT 1,
        fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        usuario_id BIGINT,
        CONSTRAINT pk_ubicaciones_fisicas PRIMARY KEY (id),
        CONSTRAINT fk_ubicaciones_fisicas_almacen FOREIGN KEY (almacen_id) REFERENCES almacenes (id),
        CONSTRAINT fk_ubicaciones_fisicas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
        CONSTRAINT uk_ubicaciones_fisicas_codigo UNIQUE (almacen_id, codigo)
     ) ENGINE=InnoDB;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- B) Columna lotes_productos.ubicaciones_fisicas_id
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'lotes_productos'
        AND column_name = 'ubicaciones_fisicas_id'
    ),
    'SELECT "OK: columna ubicaciones_fisicas_id ya existe" AS info;',
    'ALTER TABLE lotes_productos ADD COLUMN ubicaciones_fisicas_id BIGINT NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- C) FK lotes_productos -> ubicaciones_fisicas (si no existe)
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.table_constraints
      WHERE table_schema = DATABASE()
        AND table_name = 'lotes_productos'
        AND constraint_name = 'fk_lotes_productos_ubicacion_fisica'
        AND constraint_type = 'FOREIGN KEY'
    ),
    'SELECT "OK: FK fk_lotes_productos_ubicacion_fisica ya existe" AS info;',
    'ALTER TABLE lotes_productos
       ADD CONSTRAINT fk_lotes_productos_ubicacion_fisica
       FOREIGN KEY (ubicaciones_fisicas_id) REFERENCES ubicaciones_fisicas(id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

