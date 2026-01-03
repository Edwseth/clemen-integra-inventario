-- V20251230 - Planeación: asegurar columnas de auditoría en plan_produccion_detalle (idempotente)

SET @schema := DATABASE();

-- A) Validar existencia de tabla
SET @tabla_plan_detalle_existe := (
  SELECT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @schema
      AND table_name = 'plan_produccion_detalle'
  )
);

SET @sql := IF(@tabla_plan_detalle_existe = 1,
    'SELECT "OK: tabla plan_produccion_detalle existe" AS info;',
    'SELECT "SKIP: tabla plan_produccion_detalle no existe" AS info;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- creado_por_id
SET @sql := (
  SELECT IF(
    @tabla_plan_detalle_existe = 0,
    'SELECT "SKIP: tabla plan_produccion_detalle no existe" AS info;',
    IF(
      EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = @schema
          AND table_name = 'plan_produccion_detalle'
          AND column_name = 'creado_por_id'
      ),
      'SELECT "OK: columna creado_por_id ya existe" AS info;',
      'ALTER TABLE plan_produccion_detalle ADD COLUMN creado_por_id BIGINT NULL;'
    )
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- fecha_creacion
SET @sql := (
  SELECT IF(
    @tabla_plan_detalle_existe = 0,
    'SELECT "SKIP: tabla plan_produccion_detalle no existe" AS info;',
    IF(
      EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = @schema
          AND table_name = 'plan_produccion_detalle'
          AND column_name = 'fecha_creacion'
      ),
      'SELECT "OK: columna fecha_creacion ya existe" AS info;',
      'ALTER TABLE plan_produccion_detalle ADD COLUMN fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;'
    )
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- observacion
SET @sql := (
  SELECT IF(
    @tabla_plan_detalle_existe = 0,
    'SELECT "SKIP: tabla plan_produccion_detalle no existe" AS info;',
    IF(
      EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = @schema
          AND table_name = 'plan_produccion_detalle'
          AND column_name = 'observacion'
      ),
      'SELECT "OK: columna observacion ya existe" AS info;',
      'ALTER TABLE plan_produccion_detalle ADD COLUMN observacion VARCHAR(500) NULL;'
    )
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- origen_demanda
SET @sql := (
  SELECT IF(
    @tabla_plan_detalle_existe = 0,
    'SELECT "SKIP: tabla plan_produccion_detalle no existe" AS info;',
    IF(
      EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = @schema
          AND table_name = 'plan_produccion_detalle'
          AND column_name = 'origen_demanda'
      ),
      'SELECT "OK: columna origen_demanda ya existe" AS info;',
      'ALTER TABLE plan_produccion_detalle ADD COLUMN origen_demanda VARCHAR(100) NULL;'
    )
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- FK opcional a usuarios
SET @usuarios_existe := (
  SELECT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @schema
      AND table_name = 'usuarios'
  )
);

SET @fk_existe := (
  SELECT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE table_schema = @schema
      AND table_name = 'plan_produccion_detalle'
      AND constraint_name = 'fk_plan_det_creado_por'
      AND constraint_type = 'FOREIGN KEY'
  )
);

SET @sql := (
  SELECT IF(
    @tabla_plan_detalle_existe = 0,
    'SELECT "SKIP: tabla plan_produccion_detalle no existe" AS info;',
    IF(
      @usuarios_existe = 1 AND @fk_existe = 0,
      'ALTER TABLE plan_produccion_detalle ADD CONSTRAINT fk_plan_det_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuarios (id);',
      'SELECT "OK: FK fk_plan_det_creado_por ya existe o tabla usuarios no existe" AS info;'
    )
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
