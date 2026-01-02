-- V20251216 - conteos ciclicos (idempotente)

-- A) conteos_ciclicos
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name = 'conteos_ciclicos'
    ),
    'SELECT "OK: tabla conteos_ciclicos ya existe" AS info;',
    'CREATE TABLE conteos_ciclicos (
        id BIGINT NOT NULL AUTO_INCREMENT,
        almacen_id INT NOT NULL,
        estado VARCHAR(20) NOT NULL,
        fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        creado_por_id BIGINT NOT NULL,
        aplicado_en DATETIME NULL,
        aplicado_por_id BIGINT NULL,
        idempotency_key_aplicar VARCHAR(64) NULL,
        CONSTRAINT pk_conteos_ciclicos PRIMARY KEY (id),
        CONSTRAINT fk_conteos_ciclicos_almacen FOREIGN KEY (almacen_id) REFERENCES almacenes (id),
        CONSTRAINT fk_conteos_ciclicos_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuarios (id),
        CONSTRAINT fk_conteos_ciclicos_aplicado_por FOREIGN KEY (aplicado_por_id) REFERENCES usuarios (id),
        CONSTRAINT uk_conteos_ciclicos_idempotency UNIQUE (idempotency_key_aplicar)
     ) ENGINE=InnoDB;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- B) conteos_ciclicos_detalle
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name = 'conteos_ciclicos_detalle'
    ),
    'SELECT "OK: tabla conteos_ciclicos_detalle ya existe" AS info;',
    'CREATE TABLE conteos_ciclicos_detalle (
        id BIGINT NOT NULL AUTO_INCREMENT,
        conteo_id BIGINT NOT NULL,
        producto_id INT NOT NULL,
        lote_producto_id BIGINT NULL,
        ubicacion_fisica_id BIGINT NULL,
        stock_sistema DECIMAL(10, 2) NOT NULL,
        conteo_fisico DECIMAL(10, 2) NOT NULL,
        diferencia DECIMAL(10, 2) NOT NULL,
        CONSTRAINT pk_conteos_ciclicos_detalle PRIMARY KEY (id),
        CONSTRAINT fk_conteos_ciclicos_detalle_conteo FOREIGN KEY (conteo_id) REFERENCES conteos_ciclicos (id),
        CONSTRAINT fk_conteos_ciclicos_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
        CONSTRAINT fk_conteos_ciclicos_detalle_lote FOREIGN KEY (lote_producto_id) REFERENCES lotes_productos (id),
        CONSTRAINT fk_conteos_ciclicos_detalle_ubicacion FOREIGN KEY (ubicacion_fisica_id) REFERENCES ubicaciones_fisicas (id)
     ) ENGINE=InnoDB;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- C) Índices (si no existen)
SET @schema := DATABASE();

SET @idx := 'idx_conteos_ciclicos_detalle_conteo';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema
        AND table_name = 'conteos_ciclicos_detalle'
        AND index_name = @idx
    ),
    'SELECT "OK: idx_conteos_ciclicos_detalle_conteo ya existe" AS info;',
    'CREATE INDEX idx_conteos_ciclicos_detalle_conteo ON conteos_ciclicos_detalle (conteo_id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := 'idx_conteos_ciclicos_detalle_producto';
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema
        AND table_name = 'conteos_ciclicos_detalle'
        AND index_name = @idx
    ),
    'SELECT "OK: idx_conteos_ciclicos_detalle_producto ya existe" AS info;',
    'CREATE INDEX idx_conteos_ciclicos_detalle_producto ON conteos_ciclicos_detalle (producto_id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
