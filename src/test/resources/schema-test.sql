-- Tabla: usuarios
CREATE TABLE usuarios (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) NOT NULL UNIQUE
);

-- Tabla: unidades_medida
CREATE TABLE unidades_medida (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    simbolo VARCHAR(10) NOT NULL UNIQUE
);

-- Tabla: categorias_producto
CREATE TABLE categorias_producto (
    id INT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    tipo VARCHAR(50) NOT NULL
);

-- Tabla: productos
CREATE TABLE productos (
    id BIGINT PRIMARY KEY,
    codigo_sku VARCHAR(255) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion_producto VARCHAR(255),
    unidades_medida_id BIGINT NOT NULL,
    categorias_producto_id BIGINT NOT NULL,
    stock_actual INT NOT NULL,
    stock_minimo INT NOT NULL,
    stock_minimo_proveedor INT,
    activo BIT NOT NULL,
    fecha_creacion DATE NOT NULL,
    creado_por VARCHAR(255) NOT NULL,
    requiere_inspeccion BIT NOT NULL,
    usuarios_id BIGINT NOT NULL,
    FOREIGN KEY (unidades_medida_id) REFERENCES unidades_medida(id),
    FOREIGN KEY (categorias_producto_id) REFERENCES categorias_producto(id),
    FOREIGN KEY (usuarios_id) REFERENCES usuarios(id)
);

-- Tabla: almacenes
CREATE TABLE almacenes (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    ubicacion VARCHAR(255),
    categoria_almacen VARCHAR(50) NOT NULL,
    tipo_almacen VARCHAR(50) NOT NULL
);

-- Tabla: lotes_productos
CREATE TABLE lotes_productos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo_lote VARCHAR(100) NOT NULL UNIQUE,
    fecha_fabricacion DATE,
    fecha_vencimiento DATE,
    stock_lote DECIMAL(10, 2) NOT NULL,
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
