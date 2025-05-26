-- Limpiar datos previos
DELETE FROM motivos_movimiento;
DELETE FROM ordenes_compra;
DELETE FROM proveedores;
DELETE FROM tipos_movimiento_detalle;
DELETE FROM almacenes;
DELETE FROM productos;
DELETE FROM categorias_producto;
DELETE FROM unidades_medida;
DELETE FROM usuarios;

-- Insertar usuario
INSERT INTO usuarios (nombre_usuario, clave, nombre_completo, correo, rol, activo, bloqueado)
VALUES ('testuser', '123456', 'Usuario Test', 'usuario@test.com', 'ROL_ALMACENISTA', true, false);

-- Insertar unidad de medida
INSERT INTO unidades_medida (nombre, simbolo)
VALUES ('Kilogramo', 'kg');

-- Insertar categoría
INSERT INTO categorias_producto (nombre, tipo)
VALUES ('Materia Prima', 'MATERIA_PRIMA');

-- Insertar producto
INSERT INTO productos (
    codigo_sku, nombre, descripcion_producto, unidades_medida_id,
    categorias_producto_id, stock_actual, stock_minimo, stock_minimo_proveedor,
    activo, requiere_inspeccion, fecha_creacion, usuarios_id
) VALUES (
    'SKU001', 'Producto Test', 'Producto para pruebas', 1,
    1, 100, 10, 20, true, true, CURRENT_DATE, 1
);

-- Insertar almacén
INSERT INTO almacenes (nombre, ubicacion, categoria_almacen, tipo_almacen)
VALUES ('Almacen Central', 'Principal', 'MATERIA_PRIMA', 'PRINCIPAL');

-- Asegurar no duplicar tipo de movimiento detalle
DELETE FROM tipos_movimiento_detalle WHERE LOWER(descripcion) = LOWER('SALIDA_PRODUCCION');
INSERT INTO tipos_movimiento_detalle (descripcion)
VALUES ('SALIDA_PRODUCCION');

-- Insertar proveedor
INSERT INTO proveedores (
    proveedor, nit_cedula, direccion, telefono_contacto, email, pagina_web, nombre_contacto, activo
) VALUES (
    'Proveedor Prueba', '123456789', 'Calle Falsa 123', '3000000000', 'proveedor@test.com', NULL, 'Contacto Prueba', true
);

-- Insertar orden de compra
INSERT INTO ordenes_compra (
    estado, fecha_orden, observaciones, proveedor_id
) VALUES (
    'CREADA', CURRENT_DATE, 'Prueba orden', 1
);

-- Insertar motivo de movimiento
INSERT INTO motivos_movimiento (
    motivo, descripcion, tipo_movimiento
) VALUES (
    'RECEPCION_COMPRA', 'Recepción de productos por orden de compra', 'RECEPCION_COMPRA'
);
