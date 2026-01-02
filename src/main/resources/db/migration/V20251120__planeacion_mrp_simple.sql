-- V20251120 - MRP: columnas productos + tablas corridas_mrp / sugerencias_abastecimiento (idempotente)

-- ==== A) Columnas en productos ====

-- lead_time_compra_dias
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'productos'
        AND column_name = 'lead_time_compra_dias'
    ),
    'SELECT "OK: columna lead_time_compra_dias ya existe" AS info;',
    'ALTER TABLE productos ADD COLUMN lead_time_compra_dias INT NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- lead_time_produccion_dias
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'productos'
        AND column_name = 'lead_time_produccion_dias'
    ),
    'SELECT "OK: columna lead_time_produccion_dias ya existe" AS info;',
    'ALTER TABLE productos ADD COLUMN lead_time_produccion_dias INT NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- stock_seguridad
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'productos'
        AND column_name = 'stock_seguridad'
    ),
    'SELECT "OK: columna stock_seguridad ya existe" AS info;',
    'ALTER TABLE productos ADD COLUMN stock_seguridad DECIMAL(19,6) NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- stock_maximo_planeacion
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'productos'
        AND column_name = 'stock_maximo_planeacion'
    ),
    'SELECT "OK: columna stock_maximo_planeacion ya existe" AS info;',
    'ALTER TABLE productos ADD COLUMN stock_maximo_planeacion DECIMAL(19,6) NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==== B) Tabla corridas_mrp ====
-- ==== B) Tabla corridas_mrp (asegurar columna usuario_ejecuto_id antes de FK) ====

-- 1) Crear tabla si NO existe (incluye usuario_ejecuto_id)
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name = 'corridas_mrp'
    ),
    'SELECT "OK: tabla corridas_mrp ya existe" AS info;',
    'CREATE TABLE corridas_mrp (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        fecha_ejecucion DATETIME NOT NULL,
        horizonte_desde DATE NULL,
        horizonte_hasta DATE NULL,
        usuario_ejecuto_id BIGINT NULL,
        resumen VARCHAR(1000) NULL
     ) ENGINE=InnoDB;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Si la tabla YA existía, asegurar que la columna exista
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'corridas_mrp'
        AND column_name = 'usuario_ejecuto_id'
    ),
    'SELECT "OK: columna usuario_ejecuto_id ya existe en corridas_mrp" AS info;',
    'ALTER TABLE corridas_mrp ADD COLUMN usuario_ejecuto_id BIGINT NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Crear FK solo si NO existe (y ahora sí existe la columna)
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.table_constraints
      WHERE table_schema = DATABASE()
        AND table_name = 'corridas_mrp'
        AND constraint_name = 'fk_corridas_mrp_usuario'
        AND constraint_type = 'FOREIGN KEY'
    ),
    'SELECT "OK: FK fk_corridas_mrp_usuario ya existe" AS info;',
    'ALTER TABLE corridas_mrp
       ADD CONSTRAINT fk_corridas_mrp_usuario
       FOREIGN KEY (usuario_ejecuto_id) REFERENCES usuarios (id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==== C) Tabla sugerencias_abastecimiento ====
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name = 'sugerencias_abastecimiento'
    ),
    'SELECT "OK: tabla sugerencias_abastecimiento ya existe" AS info;',
    'CREATE TABLE sugerencias_abastecimiento (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        corrida_mrp_id BIGINT NOT NULL,
        producto_id BIGINT NOT NULL,
        tipo VARCHAR(30) NOT NULL,
        cantidad_sugerida DECIMAL(19,6) NOT NULL,
        fecha_requerida DATE NOT NULL,
        fecha_sugerida_pedido DATE NULL,
        estado VARCHAR(30) NOT NULL,
        origen VARCHAR(30) NOT NULL,
        observaciones VARCHAR(1000) NULL,
        orden_compra_id BIGINT NULL,
        orden_produccion_id BIGINT NULL,
        CONSTRAINT fk_sug_corrida FOREIGN KEY (corrida_mrp_id) REFERENCES corridas_mrp (id),
        CONSTRAINT fk_sug_producto FOREIGN KEY (producto_id) REFERENCES productos (id)
     ) ENGINE=InnoDB;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


