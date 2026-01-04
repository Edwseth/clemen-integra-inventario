-- Crear tabla de plantilla de checklist por etapa de producción y columnas adicionales en instancia

-- Tabla checklist_etapa_template
SET @sql_create_template := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'checklist_etapa_template'
        ),
        'SELECT "OK: tabla checklist_etapa_template ya existe" AS info;',
        'CREATE TABLE checklist_etapa_template (
            id BIGINT NOT NULL AUTO_INCREMENT,
            etapa_plantilla_id BIGINT NOT NULL,
            nombre_item VARCHAR(255) NOT NULL,
            obligatorio BIT NOT NULL DEFAULT b''0'',
            permitir_no_aplica BIT NOT NULL DEFAULT b''0'',
            orden INT NOT NULL DEFAULT 0,
            activo BIT NOT NULL DEFAULT b''1'',
            created_at DATETIME(6) NOT NULL,
            updated_at DATETIME(6),
            PRIMARY KEY (id),
            KEY idx_cet_etapa_orden (etapa_plantilla_id, orden),
            CONSTRAINT fk_cet_etapa_plantilla FOREIGN KEY (etapa_plantilla_id) REFERENCES etapa_plantilla (id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;'
    )
);
PREPARE stmt_ct FROM @sql_create_template; EXECUTE stmt_ct; DEALLOCATE PREPARE stmt_ct;

-- Alteraciones a etapa_checklist_item
SET @sql_estado := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'etapa_checklist_item'
              AND column_name = 'estado'
        ),
        'SELECT "OK: columna estado ya existe en etapa_checklist_item" AS info;',
        'ALTER TABLE etapa_checklist_item
            ADD COLUMN estado ENUM(''PENDIENTE'',''COMPLETADO'',''NO_APLICA'') NOT NULL DEFAULT ''PENDIENTE''
                AFTER completado;'
    )
);
PREPARE stmt_estado FROM @sql_estado; EXECUTE stmt_estado; DEALLOCATE PREPARE stmt_estado;

SET @sql_no_aplica := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'etapa_checklist_item'
              AND column_name = 'no_aplica'
        ),
        'SELECT "OK: columna no_aplica ya existe en etapa_checklist_item" AS info;',
        'ALTER TABLE etapa_checklist_item
            ADD COLUMN no_aplica BIT NOT NULL DEFAULT b''0'' AFTER completado;'
    )
);
PREPARE stmt_no_aplica FROM @sql_no_aplica; EXECUTE stmt_no_aplica; DEALLOCATE PREPARE stmt_no_aplica;

SET @sql_permitir_no_aplica := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'etapa_checklist_item'
              AND column_name = 'permitir_no_aplica'
        ),
        'SELECT "OK: columna permitir_no_aplica ya existe en etapa_checklist_item" AS info;',
        'ALTER TABLE etapa_checklist_item
            ADD COLUMN permitir_no_aplica BIT NOT NULL DEFAULT b''0'' AFTER no_aplica;'
    )
);
PREPARE stmt_permitir_no_aplica FROM @sql_permitir_no_aplica; EXECUTE stmt_permitir_no_aplica; DEALLOCATE PREPARE stmt_permitir_no_aplica;

SET @sql_index_estado := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'etapa_checklist_item'
              AND index_name = 'idx_checklist_etapa_estado'
        ),
        'SELECT "OK: índice idx_checklist_etapa_estado ya existe" AS info;',
        'ALTER TABLE etapa_checklist_item
            ADD INDEX idx_checklist_etapa_estado (etapa_produccion_id, estado);'
    )
);
PREPARE stmt_index_estado FROM @sql_index_estado; EXECUTE stmt_index_estado; DEALLOCATE PREPARE stmt_index_estado;
