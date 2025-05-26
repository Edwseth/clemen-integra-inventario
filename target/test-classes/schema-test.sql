-- Tabla: usuarios
CREATE TABLE usuarios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre_usuario VARCHAR(45) NOT NULL UNIQUE,
    clave VARCHAR(80) NOT NULL,
    nombre_completo VARCHAR(100) NOT NULL,
    correo VARCHAR(100) NOT NULL UNIQUE,
    rol VARCHAR(50) NOT NULL,
    activo BOOLEAN NOT NULL,
    bloqueado BOOLEAN NOT NULL
);

-- Tabla: unidades_medida
CREATE TABLE unidades_medida (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    simbolo VARCHAR(10) NOT NULL UNIQUE
);

-- Tabla: categorias_producto
CREATE TABLE categorias_producto (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    tipo VARCHAR(50) NOT NULL
);

-- Tabla: productos
CREATE TABLE productos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
    requiere_inspeccion BIT NOT NULL,
    usuarios_id BIGINT NOT NULL,
    FOREIGN KEY (unidades_medida_id) REFERENCES unidades_medida(id),
    FOREIGN KEY (categorias_producto_id) REFERENCES categorias_producto(id),
    FOREIGN KEY (usuarios_id) REFERENCES usuarios(id)
);

-- Tabla: almacenes
CREATE TABLE almacenes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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

-- Tabla: tipos_movimiento_detalle
CREATE TABLE tipos_movimiento_detalle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    descripcion VARCHAR(100) NOT NULL UNIQUE
);

-- Tabla: proveedores
CREATE TABLE proveedores (
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

-- Tabla: ordenes_compra
CREATE TABLE ordenes_compra (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    estado VARCHAR(50) NOT NULL,
    fecha_orden DATE NOT NULL,
    observaciones VARCHAR(255),
    proveedor_id BIGINT NOT NULL,
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);

-- Tabla: motivos_movimiento
CREATE TABLE motivos_movimiento (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    motivo VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    tipo_movimiento VARCHAR(50) NOT NULL
);

