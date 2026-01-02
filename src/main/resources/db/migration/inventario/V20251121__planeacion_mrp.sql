DROP TABLE IF EXISTS sugerencias_abastecimiento;
DROP TABLE IF EXISTS detalles_corrida_mrp;
DROP TABLE IF EXISTS corridas_mrp;
DROP TABLE IF EXISTS plan_produccion_detalle;
DROP TABLE IF EXISTS plan_produccion_semanal;

CREATE TABLE plan_produccion_semanal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    semana_inicio DATE NOT NULL,
    semana_fin DATE NOT NULL,
    estado VARCHAR(30) NOT NULL,
    comentarios VARCHAR(500),
    creado_por_id BIGINT,
    fecha_creacion DATETIME NOT NULL,
    CONSTRAINT fk_plan_semana_usuario FOREIGN KEY (creado_por_id) REFERENCES usuarios (id)
) ENGINE=InnoDB;

CREATE TABLE plan_produccion_detalle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    producto_id INTEGER NOT NULL,
    cantidad_planificada DECIMAL(19,6) NOT NULL,
    unidad_medida_id BIGINT,
    prioridad INT,
    origen_demanda VARCHAR(100),
    observacion VARCHAR(500),
    CONSTRAINT fk_plan_det_plan FOREIGN KEY (plan_id) REFERENCES plan_produccion_semanal (id),
    CONSTRAINT fk_plan_det_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_plan_det_unidad FOREIGN KEY (unidad_medida_id) REFERENCES unidades_medida (id)
) ENGINE=InnoDB;

CREATE TABLE corridas_mrp (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    fecha_ejecucion DATETIME NOT NULL,
    usuario_ejecucion_id BIGINT,
    horizonte_inicio DATE,
    horizonte_fin DATE,
    estado VARCHAR(30),
    version_formula_usada VARCHAR(100),
    CONSTRAINT fk_corrida_plan FOREIGN KEY (plan_id) REFERENCES plan_produccion_semanal (id),
    CONSTRAINT fk_corrida_usuario FOREIGN KEY (usuario_ejecucion_id) REFERENCES usuarios (id)
) ENGINE=InnoDB;

CREATE TABLE detalles_corrida_mrp (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    corrida_id BIGINT NOT NULL,
    producto_id INTEGER NOT NULL,
    requerimiento_bruto DECIMAL(19,6) NOT NULL,
    inventario_disponible DECIMAL(19,6) NOT NULL,
    recepciones_programadas DECIMAL(19,6) NOT NULL,
    requerimiento_neto DECIMAL(19,6) NOT NULL,
    nivel_bom INT,
    mensaje_validacion VARCHAR(500),
    CONSTRAINT fk_detalle_corrida FOREIGN KEY (corrida_id) REFERENCES corridas_mrp (id),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos (id)
) ENGINE=InnoDB;

CREATE TABLE sugerencias_abastecimiento (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    detalle_corrida_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    cantidad_sugerida DECIMAL(19,6) NOT NULL,
    fecha_necesidad DATE NOT NULL,
    fecha_sugerida_lanzamiento DATE,
    lead_time_dias INT,
    estado VARCHAR(30) NOT NULL,
    orden_compra_id BIGINT,
    orden_produccion_id BIGINT,
    CONSTRAINT fk_sug_detalle FOREIGN KEY (detalle_corrida_id) REFERENCES detalles_corrida_mrp (id)
) ENGINE=InnoDB;
