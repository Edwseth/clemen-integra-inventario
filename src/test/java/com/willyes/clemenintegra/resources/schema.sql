-- -----------------------------------------------------
-- Tabla: usuarios
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre_usuario VARCHAR(45) NOT NULL UNIQUE,
    clave VARCHAR(80) NOT NULL,
    nombre_completo VARCHAR(100) NOT NULL,
    correo VARCHAR(100) NOT NULL UNIQUE,
    rol VARCHAR(50) NOT NULL,
    activo BOOLEAN NOT NULL,
    bloqueado BOOLEAN NOT NULL
);

-- -----------------------------------------------------
-- Tabla: unidades_medida
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS unidades_medida (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    simbolo VARCHAR(10) NOT NULL UNIQUE
);

-- -----------------------------------------------------
-- Tabla: categorias_producto
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS categorias_producto (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    tipo VARCHAR(50) NOT NULL
);

-- -----------------------------------------------------
-- Tabla: productos
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS productos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo_sku VARCHAR(255) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion_producto VARCHAR(255),
    unidades_medida_id BIGINT NOT NULL,
    categorias_producto_id BIGINT NOT NULL,
    stock_actual DECIMAL(12,3) NOT NULL,
    stock_minimo DECIMAL(12,3) NOT NULL,
    stock_minimo_proveedor DECIMAL(12,3),
    activo BIT NOT NULL,
    fecha_creacion DATE NOT NULL,
    requiere_inspeccion BIT NOT NULL,
    usuarios_id BIGINT NOT NULL,
    FOREIGN KEY (unidades_medida_id) REFERENCES unidades_medida(id),
    FOREIGN KEY (categorias_producto_id) REFERENCES categorias_producto(id),
    FOREIGN KEY (usuarios_id) REFERENCES usuarios(id)
);

-- -----------------------------------------------------
-- Tabla: proveedores
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS proveedores (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    proveedor VARCHAR(255) NOT NULL UNIQUE,
    nit_cedula VARCHAR(255) NOT NULL UNIQUE,
    direccion VARCHAR(255) NOT NULL,
    telefono_contacto VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    pagina_web VARCHAR(255),
    nombre_contacto VARCHAR(255),
    activo BOOLEAN NOT NULL
);

-- -----------------------------------------------------
-- Tabla: ordenes_compra
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS ordenes_compra (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    estado VARCHAR(50) NOT NULL,
    fecha_orden DATE NOT NULL,
    observaciones VARCHAR(255),
    proveedor_id BIGINT NOT NULL,
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);

-- -----------------------------------------------------
-- Tabla: orden_compra_detalle
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS orden_compra_detalle (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cantidad DECIMAL(10,3),
    valor_unitario DECIMAL(10,3),
    valor_total DECIMAL(12,3),
    iva DECIMAL(5,3),
    cantidad_recibida DECIMAL(10,3),
    ordenes_compra_id INT NOT NULL,
    productos_id INT NOT NULL,
    FOREIGN KEY (ordenes_compra_id) REFERENCES ordenes_compra(id),
    FOREIGN KEY (productos_id) REFERENCES productos(id)
);

-- -----------------------------------------------------
-- Tabla: almacenes
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS almacenes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    ubicacion VARCHAR(255),
    categoria_almacen VARCHAR(50) NOT NULL,
    tipo_almacen VARCHAR(50) NOT NULL
);

-- -----------------------------------------------------
-- Tabla: lotes_productos
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS lotes_productos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo_lote VARCHAR(100) NOT NULL UNIQUE,
    fecha_fabricacion DATE,
    fecha_vencimiento DATE,
    stock_lote DECIMAL(10, 3) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    temperatura_almacenamiento DOUBLE,
    fecha_liberacion DATE,
    producto_id BIGINT NOT NULL,
    almacen_id BIGINT NOT NULL,
    usuario_liberador_id BIGINT,
    orden_produccion_id BIGINT,
    produccion_id BIGINT,
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (almacen_id) REFERENCES almacenes(id)
);

-- -----------------------------------------------------
-- Tabla: evaluaciones_calidad
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS evaluaciones_calidad (
    id INT PRIMARY KEY AUTO_INCREMENT,
    resultado ENUM('APROBADO','RECHAZADO','CONDICIONADO') NOT NULL,
    fecha_evaluacion DATETIME NOT NULL,
    observaciones TEXT NOT NULL,
    archivo_adjunto VARCHAR(255),
    lotes_productos_id INT NOT NULL,
    usuarios_id INT NOT NULL,
    FOREIGN KEY (lotes_productos_id) REFERENCES lotes_productos(id),
    FOREIGN KEY (usuarios_id) REFERENCES usuarios(id)
);


-- -----------------------------------------------------
-- Tabla: tipos_movimiento_detalle
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS tipos_movimiento_detalle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    descripcion VARCHAR(100) NOT NULL UNIQUE
);

-- -----------------------------------------------------
-- Tabla: motivos_movimiento
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS motivos_movimiento (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    motivo VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    tipo_movimiento VARCHAR(50) NOT NULL
);

-- -----------------------------------------------------
-- Tabla: ajustes_inventario
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS ajustes_inventario (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fecha DATETIME NOT NULL,
    cantidad DECIMAL(10,3) NOT NULL,
    motivo VARCHAR(100) NOT NULL,
    observaciones VARCHAR(255),
    productos_id BIGINT NOT NULL,
    almacenes_id BIGINT NOT NULL,
    usuarios_id BIGINT NOT NULL
);

-- FKs para ajustes_inventario
ALTER TABLE ajustes_inventario
  ADD CONSTRAINT fk_ajuste_producto
    FOREIGN KEY (productos_id) REFERENCES productos(id);

ALTER TABLE ajustes_inventario
  ADD CONSTRAINT fk_ajuste_almacen
    FOREIGN KEY (almacenes_id) REFERENCES almacenes(id);

ALTER TABLE ajustes_inventario
  ADD CONSTRAINT fk_ajuste_usuario
    FOREIGN KEY (usuarios_id) REFERENCES usuarios(id);

-- -----------------------------------------------------
-- Tabla: movimientos_inventario
-- -----------------------------------------------------
CREATE TABLE movimientos_inventario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cantidad DECIMAL(10,3) NOT NULL,
    tipo_mov ENUM('RECEPCION_COMPRA','RECEPCION_DEVOLUCION_CLIENTE','AJUSTE_POSITIVO','AJUSTE_NEGATIVO','SALIDA_PRODUCCION','ENTRADA_PRODUCCION','TRANSFERENCIA_ENTRADA','TRANSFERENCIA_SALIDA','DEVOLUCION_PROVEEDOR','SALIDA_MUESTRA','SALIDA_OBSOLETO','SALIDA_DONACION') NOT NULL,
    tipos_movimiento_detalle_id INT NOT NULL,
    fecha_ingreso DATETIME NOT NULL,
    doc_referencia VARCHAR(45),
    registrado_por_id INT NOT NULL,
    productos_id INT NOT NULL,
    lotes_productos_id INT NOT NULL,
    almacenes_id INT NOT NULL,
    proveedores_id INT NOT NULL,
    ordenes_compra_id INT NOT NULL,
    motivos_movimiento_id INT NOT NULL,
    orden_compra_detalle_id INT,

    CONSTRAINT fk_movimientos_inventario_tipo_mov_detalle FOREIGN KEY (tipos_movimiento_detalle_id) REFERENCES tipos_movimiento_detalle(id),
    CONSTRAINT fk_movimientos_inventario_productos FOREIGN KEY (productos_id) REFERENCES productos(id),
    CONSTRAINT fk_movimientos_inventario_lotes FOREIGN KEY (lotes_productos_id) REFERENCES lotes_productos(id),
    CONSTRAINT fk_movimientos_inventario_almacenes FOREIGN KEY (almacenes_id) REFERENCES almacenes(id),
    CONSTRAINT fk_movimientos_inventario_proveedores FOREIGN KEY (proveedores_id) REFERENCES proveedores(id),
    CONSTRAINT fk_movimientos_inventario_ordenes FOREIGN KEY (ordenes_compra_id) REFERENCES ordenes_compra(id),
    CONSTRAINT fk_movimientos_inventario_motivos FOREIGN KEY (motivos_movimiento_id) REFERENCES motivos_movimiento(id),
    CONSTRAINT fk_movimientos_inventario_orden_compra_detalle FOREIGN KEY (orden_compra_detalle_id) REFERENCES orden_compra_detalle(id),
    CONSTRAINT fk_movimientos_inventario_usuario FOREIGN KEY (registrado_por_id) REFERENCES usuarios(id)
);

-- -----------------------------------------------------
-- Tabla: formula_producto
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS formula_producto (
    id INT PRIMARY KEY AUTO_INCREMENT,
    producto_id INT NOT NULL,
    version VARCHAR(50) NOT NULL,
    estado ENUM('BORRADOR','EN_REVISION','APROBADA','RECHAZADA') DEFAULT 'BORRADOR',
    fecha_creacion DATETIME NOT NULL,
    creado_por_id INT NOT NULL,
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (creado_por_id) REFERENCES usuarios(id)
);

-- -----------------------------------------------------
-- Tabla: detalle_fórmula
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS detalle_formula (
    id INT PRIMARY KEY AUTO_INCREMENT,
    formula_id INT NOT NULL,
    insumo_id INT NOT NULL,
    cantidad_necesaria DECIMAL(10,3) NOT NULL,
    unidad_medida_id INT NOT NULL,
    obligatorio TINYINT DEFAULT 1,
    FOREIGN KEY (formula_id) REFERENCES formula_producto(id),
    FOREIGN KEY (insumo_id) REFERENCES productos(id),
    FOREIGN KEY (unidad_medida_id) REFERENCES unidades_medida(id)
);

-- -----------------------------------------------------
-- Tabla: orden_produccion
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS orden_produccion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    lote_produccion VARCHAR(50) NOT NULL UNIQUE,
    producto_id INT NOT NULL,
    responsable_id INT NOT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin DATETIME,
    cantidad_programada INT NOT NULL,
    cantidad_producida INT DEFAULT 0,
    estado ENUM('CREADA','EN_PROCESO','FINALIZADA','CANCELADA') DEFAULT 'EN_PROCESO',
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (responsable_id) REFERENCES usuarios(id)
);

-- -----------------------------------------------------
-- Tabla: etapa_produccion
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS etapa_produccion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre_etapa VARCHAR(100) NOT NULL UNIQUE,
    secuencia INT NOT NULL,
    orden_produccion_id INT NOT NULL,
    FOREIGN KEY (orden_produccion_id) REFERENCES orden_produccion(id)
);

-- -----------------------------------------------------
-- Tabla: detalle_etapa
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS detalle_etapa (
    id INT PRIMARY KEY AUTO_INCREMENT,
    orden_produccion_id INT NOT NULL,
    etapa_id INT NOT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin DATETIME,
    observaciones TEXT,
    usuario_id INT,
    FOREIGN KEY (orden_produccion_id) REFERENCES orden_produccion(id),
    FOREIGN KEY (etapa_id) REFERENCES etapa_produccion(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- -----------------------------------------------------
-- Tabla: control_calidad_proceso
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS control_calidad_proceso (
    id INT PRIMARY KEY AUTO_INCREMENT,
    detalle_etapa_id INT NOT NULL,
    parametro VARCHAR(100) NOT NULL,
    valor_medido VARCHAR(100) NOT NULL,
    cumple TINYINT DEFAULT 0,
    evaluado_por_id INT NOT NULL,
    FOREIGN KEY (detalle_etapa_id) REFERENCES detalle_etapa(id),
    FOREIGN KEY (evaluado_por_id) REFERENCES usuarios(id)
);

-- -----------------------------------------------------
-- Tabla: documento_fórmula
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS documento_formula (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    formula_id BIGINT NOT NULL,
    tipo_documento VARCHAR(50) NOT NULL CHECK (tipo_documento IN ('MSDS', 'INSTRUCTIVO', 'PROCEDIMIENTO')),
    ruta_archivo VARCHAR(255) NOT NULL,
    CONSTRAINT fk_documento_formula_formula FOREIGN KEY (formula_id) REFERENCES formula_producto(id) ON DELETE CASCADE
);