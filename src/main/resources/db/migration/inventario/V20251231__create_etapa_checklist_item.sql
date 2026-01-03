-- V20251231 - Crear tabla etapa_checklist_item para checklist por etapa de producción
SET @sql := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name = 'etapa_checklist_item'
    ),
    'SELECT "OK: tabla etapa_checklist_item ya existe" AS info;',
    'CREATE TABLE etapa_checklist_item (
        id BIGINT NOT NULL AUTO_INCREMENT,
        etapa_produccion_id BIGINT NOT NULL,
        nombre_paso VARCHAR(255) NOT NULL,
        obligatorio BIT NOT NULL DEFAULT b''0'',
        completado BIT NOT NULL DEFAULT b''0'',
        observacion TEXT,
        completed_at DATETIME(6),
        completed_by_id BIGINT,
        created_at DATETIME(6) NOT NULL,
        created_by_id BIGINT,
        updated_at DATETIME(6),
        updated_by_id BIGINT,
        PRIMARY KEY (id),
        KEY idx_checklist_etapa_id (etapa_produccion_id),
        CONSTRAINT fk_eci_etapa FOREIGN KEY (etapa_produccion_id) REFERENCES etapa_produccion (id),
        CONSTRAINT fk_eci_completed_by FOREIGN KEY (completed_by_id) REFERENCES usuarios (id),
        CONSTRAINT fk_eci_created_by FOREIGN KEY (created_by_id) REFERENCES usuarios (id),
        CONSTRAINT fk_eci_updated_by FOREIGN KEY (updated_by_id) REFERENCES usuarios (id)
     ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
