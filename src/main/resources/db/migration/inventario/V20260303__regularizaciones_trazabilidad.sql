CREATE TABLE IF NOT EXISTS regularizaciones_trazabilidad (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    orden_produccion_id BIGINT NOT NULL,
    cantidad_programada DECIMAL(10,2) NOT NULL,
    cantidad_real DECIMAL(10,2) NOT NULL,
    diferencia DECIMAL(10,2) NOT NULL,
    ajustar_pt BIT(1) NOT NULL,
    documento_referencia VARCHAR(45),
    observaciones VARCHAR(500),
    idempotency_key VARCHAR(80) NOT NULL,
    usuario_id BIGINT NOT NULL,
    fecha_ingreso DATETIME(6) NOT NULL,
    CONSTRAINT uk_regularizacion_trazabilidad_idem UNIQUE (idempotency_key),
    CONSTRAINT fk_regularizacion_trazabilidad_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS regularizacion_detalle (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    regularizacion_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    lote_id BIGINT NOT NULL,
    cantidad DECIMAL(10,2) NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    almacen_origen_id INT NULL,
    almacen_destino_id INT NULL,
    movimiento_id BIGINT NOT NULL,
    CONSTRAINT fk_regularizacion_detalle_regularizacion FOREIGN KEY (regularizacion_id) REFERENCES regularizaciones_trazabilidad(id),
    CONSTRAINT fk_regularizacion_detalle_mov FOREIGN KEY (movimiento_id) REFERENCES movimientos_inventario(id)
);

INSERT INTO tipos_movimiento_detalle (descripcion)
SELECT 'REGULARIZACION_TRAZABILIDAD'
WHERE NOT EXISTS (SELECT 1 FROM tipos_movimiento_detalle WHERE descripcion = 'REGULARIZACION_TRAZABILIDAD');

INSERT INTO tipos_movimiento_detalle (descripcion)
SELECT 'REGULARIZACION_TRAZABILIDAD_PT'
WHERE NOT EXISTS (SELECT 1 FROM tipos_movimiento_detalle WHERE descripcion = 'REGULARIZACION_TRAZABILIDAD_PT');

SET @motivo_data_type := (
    SELECT DATA_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'motivos_movimiento'
      AND COLUMN_NAME = 'motivo'
);

SET @motivo_coltype := (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'motivos_movimiento'
      AND COLUMN_NAME = 'motivo'
);

SET @motivo_len := (
    SELECT CHARACTER_MAXIMUM_LENGTH
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'motivos_movimiento'
      AND COLUMN_NAME = 'motivo'
);

SET @required_motivo_len := CHAR_LENGTH('REGULARIZACION_TRAZABILIDAD_PT');

SET @enum_values := IF(@motivo_data_type = 'enum', TRIM(TRAILING ')' FROM SUBSTRING(@motivo_coltype, 6)), NULL);
SET @enum_values := IF(@motivo_data_type = 'enum' AND INSTR(@enum_values, '''REGULARIZACION_TRAZABILIDAD''') = 0, CONCAT(@enum_values, ',''REGULARIZACION_TRAZABILIDAD'''), @enum_values);
SET @enum_values := IF(@motivo_data_type = 'enum' AND INSTR(@enum_values, '''REGULARIZACION_TRAZABILIDAD_PT''') = 0, CONCAT(@enum_values, ',''REGULARIZACION_TRAZABILIDAD_PT'''), @enum_values);

SET @alter_motivo_sql := IF(
    @motivo_data_type = 'enum' AND @enum_values IS NOT NULL,
    CONCAT('ALTER TABLE motivos_movimiento MODIFY COLUMN motivo ENUM(', @enum_values, ') NOT NULL'),
    IF(
        @motivo_data_type IN ('varchar', 'char') AND @motivo_len < @required_motivo_len,
        CONCAT('ALTER TABLE motivos_movimiento MODIFY COLUMN motivo VARCHAR(', @required_motivo_len, ') NOT NULL'),
        'SELECT 1'
    )
);

PREPARE alter_motivo_stmt FROM @alter_motivo_sql;
EXECUTE alter_motivo_stmt;
DEALLOCATE PREPARE alter_motivo_stmt;

INSERT INTO motivos_movimiento (descripcion, motivo)
SELECT 'Regularizacion de trazabilidad', 'REGULARIZACION_TRAZABILIDAD'
WHERE NOT EXISTS (SELECT 1 FROM motivos_movimiento WHERE motivo = 'REGULARIZACION_TRAZABILIDAD');

INSERT INTO motivos_movimiento (descripcion, motivo)
SELECT 'Regularizacion de trazabilidad PT', 'REGULARIZACION_TRAZABILIDAD_PT'
WHERE NOT EXISTS (SELECT 1 FROM motivos_movimiento WHERE motivo = 'REGULARIZACION_TRAZABILIDAD_PT');
