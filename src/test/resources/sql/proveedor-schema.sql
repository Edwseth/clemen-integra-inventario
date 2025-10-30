CREATE TABLE IF NOT EXISTS proveedores (
    id INT AUTO_INCREMENT PRIMARY KEY,
    proveedor VARCHAR(100) NOT NULL,
    nit_cedula VARCHAR(20) NOT NULL,
    telefono_contacto VARCHAR(20),
    ciudad VARCHAR(120),
    email VARCHAR(100),
    direccion VARCHAR(150),
    pagina_web VARCHAR(150),
    nombre_contacto VARCHAR(100) NOT NULL,
    activo BIT NOT NULL,
    CONSTRAINT uk_proveedores_nombre UNIQUE (proveedor),
    CONSTRAINT uk_proveedores_nit UNIQUE (nit_cedula),
    CONSTRAINT uk_proveedores_email UNIQUE (email)
);
