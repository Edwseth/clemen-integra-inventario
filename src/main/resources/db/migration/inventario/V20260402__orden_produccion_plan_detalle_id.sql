SET @schema := DATABASE();

SET @tabla_orden_existe := (
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = @schema
          AND table_name = 'orden_produccion'
    )
);

SET @tabla_plan_detalle_existe := (
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = @schema
          AND table_name = 'plan_produccion_detalle'
    )
);

SET @sql := IF(@tabla_orden_existe = 1,
    'SELECT "OK: tabla orden_produccion existe" AS info;',
    'SELECT "SKIP: tabla orden_produccion no existe" AS info;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@tabla_plan_detalle_existe = 1,
    'SELECT "OK: tabla plan_produccion_detalle existe" AS info;',
    'SELECT "SKIP: tabla plan_produccion_detalle no existe" AS info;'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        @tabla_orden_existe = 0,
        'SELECT "SKIP: tabla orden_produccion no existe" AS info;',
        IF(
            EXISTS (
                SELECT 1 FROM information_schema.columns
                WHERE table_schema = @schema
                  AND table_name = 'orden_produccion'
                  AND column_name = 'plan_detalle_id'
            ),
            'SELECT "OK: columna plan_detalle_id ya existe" AS info;',
            'ALTER TABLE orden_produccion ADD COLUMN plan_detalle_id BIGINT NULL AFTER responsable_id;'
        )
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_existe := (
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema
          AND table_name = 'orden_produccion'
          AND index_name = 'idx_orden_produccion_plan_detalle_id'
    )
);

SET @sql := (
    SELECT IF(
        @tabla_orden_existe = 0,
        'SELECT "SKIP: tabla orden_produccion no existe" AS info;',
        IF(
            @idx_existe = 1,
            'SELECT "OK: índice idx_orden_produccion_plan_detalle_id ya existe" AS info;',
            'CREATE INDEX idx_orden_produccion_plan_detalle_id ON orden_produccion (plan_detalle_id);'
        )
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_existe := (
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = @schema
          AND table_name = 'orden_produccion'
          AND constraint_name = 'fk_orden_produccion_plan_detalle'
          AND constraint_type = 'FOREIGN KEY'
    )
);

SET @sql := (
    SELECT IF(
        @tabla_orden_existe = 0 OR @tabla_plan_detalle_existe = 0,
        'SELECT "SKIP: falta tabla orden_produccion o plan_produccion_detalle" AS info;',
        IF(
            @fk_existe = 1,
            'SELECT "OK: FK fk_orden_produccion_plan_detalle ya existe" AS info;',
            'ALTER TABLE orden_produccion ADD CONSTRAINT fk_orden_produccion_plan_detalle FOREIGN KEY (plan_detalle_id) REFERENCES plan_produccion_detalle (id);'
        )
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
