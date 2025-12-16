CREATE TABLE IF NOT EXISTS ubicaciones_fisicas (
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
) ENGINE=InnoDB;

ALTER TABLE lotes_productos
    ADD COLUMN IF NOT EXISTS ubicaciones_fisicas_id BIGINT NULL;

SET @fk_name := 'fk_lotes_productos_ubicacion_fisica';
SET @schema_name := (SELECT DATABASE());
SET @fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @schema_name
      AND CONSTRAINT_NAME = @fk_name
);
SET @add_fk_sql := IF(
    @fk_exists = 0,
    'ALTER TABLE lotes_productos ADD CONSTRAINT fk_lotes_productos_ubicacion_fisica FOREIGN KEY (ubicaciones_fisicas_id) REFERENCES ubicaciones_fisicas(id)',
    'DO 0'
);
PREPARE stmt FROM @add_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
