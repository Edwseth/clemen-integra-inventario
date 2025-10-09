/* ===== 1) RETENCION_LOTE: columnas nuevas (compatibles) ===== */

-- motivo
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='retencion_lote' AND column_name='motivo');
SET @sql := IF(@c=0,
 'ALTER TABLE retencion_lote ADD COLUMN motivo ENUM(''NO_CONFORMIDAD'',''OTRO'') NOT NULL DEFAULT ''OTRO'' AFTER causa',
 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- no_conformidad_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='retencion_lote' AND column_name='no_conformidad_id');
SET @sql := IF(@c=0,
 'ALTER TABLE retencion_lote ADD COLUMN no_conformidad_id INT NULL AFTER motivo',
 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- índice idx_retencion_nc
SET @c := (SELECT COUNT(*) FROM information_schema.statistics
           WHERE table_schema=DATABASE() AND table_name='retencion_lote' AND index_name='idx_retencion_nc');
SET @sql := IF(@c=0,
 'CREATE INDEX idx_retencion_nc ON retencion_lote (no_conformidad_id)',
 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- índice compuesto idx_retencion_lote_estado_motivo
SET @c := (SELECT COUNT(*) FROM information_schema.statistics
           WHERE table_schema=DATABASE() AND table_name='retencion_lote' AND index_name='idx_retencion_lote_estado_motivo');
SET @sql := IF(@c=0,
 'CREATE INDEX idx_retencion_lote_estado_motivo ON retencion_lote (lote_id, estado, motivo)',
 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

/* ===== 2) NO_CONFORMIDAD: columnas base ===== */

-- estado
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='no_conformidad' AND column_name='estado');
SET @sql := IF(@c=0,
 'ALTER TABLE no_conformidad ADD COLUMN estado ENUM(''ABIERTA'',''CERRADA'') NOT NULL DEFAULT ''ABIERTA'' AFTER severidad',
 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- lote_id / producto_id / evaluacion_id / creado_por / actualizado_por / actualizado_en
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='no_conformidad' AND column_name='lote_id');
SET @sql := IF(@c=0, 'ALTER TABLE no_conformidad ADD COLUMN lote_id INT NULL AFTER estado', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='no_conformidad' AND column_name='producto_id');
SET @sql := IF(@c=0, 'ALTER TABLE no_conformidad ADD COLUMN producto_id INT NULL AFTER lote_id', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='no_conformidad' AND column_name='evaluacion_id');
SET @sql := IF(@c=0, 'ALTER TABLE no_conformidad ADD COLUMN evaluacion_id INT NULL AFTER producto_id', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='no_conformidad' AND column_name='creado_por');
SET @sql := IF(@c=0, 'ALTER TABLE no_conformidad ADD COLUMN creado_por INT NULL AFTER usuario_reporta_id', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='no_conformidad' AND column_name='actualizado_por');
SET @sql := IF(@c=0, 'ALTER TABLE no_conformidad ADD COLUMN actualizado_por INT NULL AFTER creado_por', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='no_conformidad' AND column_name='actualizado_en');
SET @sql := IF(@c=0, 'ALTER TABLE no_conformidad ADD COLUMN actualizado_en DATETIME(6) NULL AFTER actualizado_por', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

/* ===== 3) Alinear tipos HIJAS = PKs reales (int/bigint/unsigned) ===== */

-- no_conformidad.lote_id -> lotes_productos.id
SET @t := (SELECT COLUMN_TYPE FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='lotes_productos' AND column_name='id');
SET @sql := CONCAT('ALTER TABLE no_conformidad MODIFY COLUMN lote_id ', @t, ' NULL');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- no_conformidad.producto_id -> productos.id
SET @t := (SELECT COLUMN_TYPE FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='productos' AND column_name='id');
SET @sql := CONCAT('ALTER TABLE no_conformidad MODIFY COLUMN producto_id ', @t, ' NULL');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- no_conformidad.evaluacion_id -> evaluaciones_calidad.id
SET @t := (SELECT COLUMN_TYPE FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='evaluaciones_calidad' AND column_name='id');
SET @sql := CONCAT('ALTER TABLE no_conformidad MODIFY COLUMN evaluacion_id ', @t, ' NULL');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- retencion_lote.no_conformidad_id -> no_conformidad.id
SET @t := (SELECT COLUMN_TYPE FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name='no_conformidad' AND column_name='id');
SET @sql := CONCAT('ALTER TABLE retencion_lote MODIFY COLUMN no_conformidad_id ', @t, ' NULL');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

/* ===== 4) Índices en no_conformidad ===== */
SET @c := (SELECT COUNT(*) FROM information_schema.statistics
           WHERE table_schema=DATABASE() AND table_name='no_conformidad' AND index_name='idx_nc_lote_estado');
SET @sql := IF(@c=0, 'CREATE INDEX idx_nc_lote_estado ON no_conformidad (lote_id, estado)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c := (SELECT COUNT(*) FROM information_schema.statistics
           WHERE table_schema=DATABASE() AND table_name='no_conformidad' AND index_name='idx_nc_lote');
SET @sql := IF(@c=0, 'CREATE INDEX idx_nc_lote ON no_conformidad (lote_id)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c := (SELECT COUNT(*) FROM information_schema.statistics
           WHERE table_schema=DATABASE() AND table_name='no_conformidad' AND index_name='idx_nc_producto');
SET @sql := IF(@c=0, 'CREATE INDEX idx_nc_producto ON no_conformidad (producto_id)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @c := (SELECT COUNT(*) FROM information_schema.statistics
           WHERE table_schema=DATABASE() AND table_name='no_conformidad' AND index_name='idx_nc_evaluacion');
SET @sql := IF(@c=0, 'CREATE INDEX idx_nc_evaluacion ON no_conformidad (evaluacion_id)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

/* ===== 5) FKs: eliminar si existen y recrear ===== */

-- Drop FK no_conformidad.fk_nc_lote
SET @c := (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
           WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME='no_conformidad' AND CONSTRAINT_NAME='fk_nc_lote');
SET @sql := IF(@c>0, 'ALTER TABLE no_conformidad DROP FOREIGN KEY fk_nc_lote', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Drop FK no_conformidad.fk_nc_producto
SET @c := (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
           WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME='no_conformidad' AND CONSTRAINT_NAME='fk_nc_producto');
SET @sql := IF(@c>0, 'ALTER TABLE no_conformidad DROP FOREIGN KEY fk_nc_producto', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Drop FK no_conformidad.fk_nc_evaluacion
SET @c := (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
           WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME='no_conformidad' AND CONSTRAINT_NAME='fk_nc_evaluacion');
SET @sql := IF(@c>0, 'ALTER TABLE no_conformidad DROP FOREIGN KEY fk_nc_evaluacion', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Drop FK retencion_lote.fk_retencion_no_conformidad
SET @c := (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
           WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME='retencion_lote' AND CONSTRAINT_NAME='fk_retencion_no_conformidad');
SET @sql := IF(@c>0, 'ALTER TABLE retencion_lote DROP FOREIGN KEY fk_retencion_no_conformidad', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Crear FKs
ALTER TABLE retencion_lote
  ADD CONSTRAINT fk_retencion_no_conformidad
    FOREIGN KEY (no_conformidad_id) REFERENCES no_conformidad(id)
    ON DELETE SET NULL ON UPDATE RESTRICT;

ALTER TABLE no_conformidad
  ADD CONSTRAINT fk_nc_lote
    FOREIGN KEY (lote_id) REFERENCES lotes_productos(id);

ALTER TABLE no_conformidad
  ADD CONSTRAINT fk_nc_producto
    FOREIGN KEY (producto_id) REFERENCES productos(id);

ALTER TABLE no_conformidad
  ADD CONSTRAINT fk_nc_evaluacion
    FOREIGN KEY (evaluacion_id) REFERENCES evaluaciones_calidad(id);

