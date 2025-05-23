-- Insertar usuario requerido por FK en productos
INSERT INTO usuarios (id, nombre, correo)
VALUES (1, 'Usuario Test', 'usuario@test.com');

-- Insertar unidad de medida
INSERT INTO unidades_medida (id, nombre, simbolo)
VALUES (1, 'Kilogramo', 'kg');

-- Insertar categoría de producto
INSERT INTO categorias_producto (id, nombre, tipo)
VALUES (1, 'Materia Prima', 'MATERIA_PRIMA');

-- Insertar producto con FK completas
INSERT INTO productos (
    id, codigo_sku, nombre, descripcion_producto, unidades_medida_id,
    categorias_producto_id, stock_actual, stock_minimo, stock_minimo_proveedor,
    activo, fecha_creacion, creado_por, requiere_inspeccion, usuarios_id
) VALUES (
    1, 'SKU001', 'Producto Test', 'Producto para pruebas', 1,
    1, 100, 10, 20, 1, CURRENT_DATE, 'admin', 1, 1
);

-- Insertar almacén
INSERT INTO almacenes (id, nombre, ubicacion, categoria_almacen, tipo_almacen)
VALUES (1, 'Almacen Central', 'Principal', 'MATERIA_PRIMA', 'PRINCIPAL');
