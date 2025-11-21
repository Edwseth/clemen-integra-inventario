CREATE TABLE proveedores (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    proveedor VARCHAR(100) NOT NULL UNIQUE,
    nit_cedula VARCHAR(20) NOT NULL UNIQUE,
    telefono_contacto VARCHAR(20),
    ciudad VARCHAR(120),
    email VARCHAR(100) UNIQUE,
    direccion VARCHAR(150),
    pagina_web VARCHAR(150),
    nombre_contacto VARCHAR(100) NOT NULL,
    activo BIT NOT NULL
);
