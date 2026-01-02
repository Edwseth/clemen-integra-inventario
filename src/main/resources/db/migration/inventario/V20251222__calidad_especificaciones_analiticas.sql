-- V20251222 - especificaciones_fisico_quimicas + columnas/idx/FKs en plantillas_analisis_micro (idempotente)

SET @schema := DATABASE();

-- A) Tabla especificaciones_fisico_quimicas
CREATE TABLE IF NOT EXISTS especificaciones_fisico_quimicas (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  producto_id BIGINT NOT NULL,
  nombre_parametro VARCHAR(255) NOT NULL,
  unidad VARCHAR(100) NULL,
  limite_inferior DECIMAL(10, 2) NULL,
  limite_superior DECIMAL(10, 2) NULL,
  observaciones TEXT NULL,
  activo TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NULL,
  created_by_id BIGINT NULL,
  updated_at DATETIME NULL,
  updated_by_id BIGINT NULL,
  CONSTRAINT fk_espec_fq_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
  CONSTRAINT fk_espec_fq_creado_por FOREIGN KEY (created_by_id) REFERENCES usuarios (id),
  CONSTRAINT fk_espec_fq_actualizado_por FOREIGN KEY (updated_by_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Índice especificaciones
SET @idx := 'idx_espec_fq_producto_activo';
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=@schema AND table_name='especificaciones_fisico_quimicas' AND index_name=@idx),
    'SELECT "OK: idx_espec_fq_producto_activo ya existe" AS info;',
    'CREATE INDEX idx_espec_fq_producto_activo ON especificaciones_fisico_quimicas (producto_id, activo);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- B) Columnas en plantillas_analisis_micro
-- producto_id
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=@schema AND table_name='plantillas_analisis_micro' AND column_name='producto_id'),
    'SELECT "OK: plantillas_analisis_micro.producto_id ya existe" AS info;',
    'ALTER TABLE plantillas_analisis_micro ADD COLUMN producto_id BIGINT NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- vigente
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=@schema AND table_name='plantillas_analisis_micro' AND column_name='vigente'),
    'SELECT "OK: plantillas_analisis_micro.vigente ya existe" AS info;',
    'ALTER TABLE plantillas_analisis_micro ADD COLUMN vigente TINYINT(1) NOT NULL DEFAULT 0;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- fecha_vigencia_desde
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=@schema AND table_name='plantillas_analisis_micro' AND column_name='fecha_vigencia_desde'),
    'SELECT "OK: plantillas_analisis_micro.fecha_vigencia_desde ya existe" AS info;',
    'ALTER TABLE plantillas_analisis_micro ADD COLUMN fecha_vigencia_desde DATE NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- fecha_vigencia_hasta
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=@schema AND table_name='plantillas_analisis_micro' AND column_name='fecha_vigencia_hasta'),
    'SELECT "OK: plantillas_analisis_micro.fecha_vigencia_hasta ya existe" AS info;',
    'ALTER TABLE plantillas_analisis_micro ADD COLUMN fecha_vigencia_hasta DATE NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- updated_at
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=@schema AND table_name='plantillas_analisis_micro' AND column_name='updated_at'),
    'SELECT "OK: plantillas_analisis_micro.updated_at ya existe" AS info;',
    'ALTER TABLE plantillas_analisis_micro ADD COLUMN updated_at DATETIME NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- updated_by_id
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=@schema AND table_name='plantillas_analisis_micro' AND column_name='updated_by_id'),
    'SELECT "OK: plantillas_analisis_micro.updated_by_id ya existe" AS info;',
    'ALTER TABLE plantillas_analisis_micro ADD COLUMN updated_by_id BIGINT NULL;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- C) FKs en plantillas_analisis_micro (solo si no existen)
-- fk_plantilla_micro_producto
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.table_constraints
      WHERE table_schema=@schema AND table_name='plantillas_analisis_micro'
        AND constraint_name='fk_plantilla_micro_producto' AND constraint_type='FOREIGN KEY'
    ),
    'SELECT "OK: FK fk_plantilla_micro_producto ya existe" AS info;',
    'ALTER TABLE plantillas_analisis_micro
       ADD CONSTRAINT fk_plantilla_micro_producto FOREIGN KEY (producto_id) REFERENCES productos (id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- fk_plantilla_micro_creado_por (ojo: created_by_id debe existir ya en tu tabla; si no existe, esto fallará)
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.table_constraints
      WHERE table_schema=@schema AND table_name='plantillas_analisis_micro'
        AND constraint_name='fk_plantilla_micro_creado_por' AND constraint_type='FOREIGN KEY'
    ),
    'SELECT "OK: FK fk_plantilla_micro_creado_por ya existe" AS info;',
    'ALTER TABLE plantillas_analisis_micro
       ADD CONSTRAINT fk_plantilla_micro_creado_por FOREIGN KEY (created_by_id) REFERENCES usuarios (id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- fk_plantilla_micro_actualizado_por
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.table_constraints
      WHERE table_schema=@schema AND table_name='plantillas_analisis_micro'
        AND constraint_name='fk_plantilla_micro_actualizado_por' AND constraint_type='FOREIGN KEY'
    ),
    'SELECT "OK: FK fk_plantilla_micro_actualizado_por ya existe" AS info;',
    'ALTER TABLE plantillas_analisis_micro
       ADD CONSTRAINT fk_plantilla_micro_actualizado_por FOREIGN KEY (updated_by_id) REFERENCES usuarios (id);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- D) Índice plantilla_micro_producto_vigente
SET @idx := 'idx_plantilla_micro_producto_vigente';
SET @sql := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=@schema AND table_name='plantillas_analisis_micro' AND index_name=@idx),
    'SELECT "OK: idx_plantilla_micro_producto_vigente ya existe" AS info;',
    'CREATE INDEX idx_plantilla_micro_producto_vigente ON plantillas_analisis_micro (producto_id, vigente);'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
