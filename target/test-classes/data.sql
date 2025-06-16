-- Limpiar datos previos
DELETE FROM motivos_movimiento;
DELETE FROM ordenes_compra;
DELETE FROM proveedores;
DELETE FROM tipos_movimiento_detalle;
DELETE FROM almacenes;
DELETE FROM orden_compra_detalle;
DELETE FROM ordenes_compra;
DELETE FROM productos;
DELETE FROM categorias_producto;
DELETE FROM unidades_medida;
DELETE FROM usuarios;

-- Insertar usuario
INSERT INTO usuarios (nombre_usuario, clave, nombre_completo, correo, rol, activo, bloqueado)
VALUES ('testuser', '123456', 'Usuario Test', 'usuario@test.com', 'ROL_ALMACENISTA', 1, 0);

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
    1, 100.00, 150.00, 20.00, 1, 1, CURRENT_DATE, 1
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
    'Proveedor Prueba', '123456789', 'Calle Falsa 123', '3000000000', 'proveedor@test.com', NULL, 'Contacto Prueba', 1
);

-- Insertar orden de compra
INSERT INTO ordenes_compra (
    estado, fecha_orden, observaciones, proveedor_id
) VALUES (
    'CREADA', CURRENT_DATE, 'Prueba orden', 1
);

-- Insertar detalle de orden de compra
INSERT INTO orden_compra_detalle (
    cantidad, valor_unitario, valor_total, iva, cantidad_recibida,
    ordenes_compra_id, productos_id
) VALUES (
    10.0, 1000.0, 11900.0, 19.0, 5.0, -- 5.0, 1000.0, 5000.0, 19.0, 0.0,
    1, -- ordenes_compra_id
    1  -- productos_id (Producto Test)
);


-- Insertar motivo de movimiento
INSERT INTO motivos_movimiento (
    motivo, descripcion, tipo_movimiento
) VALUES (
    'RECEPCION_COMPRA', 'Recepción de productos por orden de compra', 'RECEPCION_COMPRA'
);

-- Insertar un ajuste de inventario de ejemplo
INSERT INTO ajustes_inventario (
    fecha,
    cantidad,
    motivo,
    observaciones,
    productos_id,
    almacenes_id,
    usuarios_id
) VALUES (
    CURRENT_TIMESTAMP,
    5.00,
    'AJUSTE_INICIAL',
    'Ajuste de stock inicial para pruebas',
    1,
    1,
    1
);

-- Insertar lote vencido
INSERT INTO lotes_productos (
    codigo_lote,
    fecha_fabricacion,
    fecha_vencimiento,
    stock_lote,
    estado,
    temperatura_almacenamiento,
    fecha_liberacion,
    producto_id,
    almacen_id
) VALUES (
    'LOTE-VENCIDO-001',
    '2024-04-01',
    '2024-05-01',
    50.00,
    'DISPONIBLE',
    25.0,
    NULL,
    1,
    1
);

-- Insertar lote en CUARENTENA con más de 15 días de antigüedad
INSERT INTO lotes_productos (
    codigo_lote,
    fecha_fabricacion,
    fecha_vencimiento,
    stock_lote,
    estado,
    temperatura_almacenamiento,
    fecha_liberacion,
    producto_id,
    almacen_id
) VALUES (
    'LOTE-EN_CUARENTENA-001',
    '2024-04-01',
    '2024-08-01',
    40.00,
    'EN_CUARENTENA',
    18.0,
    NULL,
    1,
    1
);

-- Insertar producto final
INSERT INTO productos (
    codigo_sku, nombre, descripcion_producto, unidades_medida_id,
    categorias_producto_id, stock_actual, stock_minimo, stock_minimo_proveedor,
    activo, requiere_inspeccion, fecha_creacion, usuarios_id
) VALUES (
    'SKU-BEBIDA-001', 'Bebida enriquecida', 'Bebida con vitaminas y minerales', 1,
    1, 0.00, 0.00, 0.00, 1, 0, CURRENT_DATE, 1
);

-- Insertar Insumos
-- Agua Purificada
INSERT INTO productos (codigo_sku, nombre, stock_actual, stock_minimo, stock_minimo_proveedor, unidades_medida_id, categorias_producto_id, activo, requiere_inspeccion, fecha_creacion, usuarios_id)
VALUES ('PV000', 'Agua Purificada', 300.00, 50.00, 100.00, 1, 1, 1, 0, CURRENT_DATE, 1);

-- Linaza Polvo
INSERT INTO productos (codigo_sku, nombre, stock_actual, stock_minimo, stock_minimo_proveedor, unidades_medida_id, categorias_producto_id, activo, requiere_inspeccion, fecha_creacion, usuarios_id)
VALUES ('PV066', 'Linaza Polvo', 50.00, 10.00, 20.00, 1, 1, 1, 0, CURRENT_DATE, 1);

-- Malva Hoja Entero
INSERT INTO productos (codigo_sku, nombre, stock_actual, stock_minimo, stock_minimo_proveedor, unidades_medida_id, categorias_producto_id, activo, requiere_inspeccion, fecha_creacion, usuarios_id)
VALUES ('PV136', 'Malva Hoja Entero', 50.00, 10.00, 20.00, 1, 1, 1, 0, CURRENT_DATE, 1);

-- Insertar fórmula
INSERT INTO formula_producto (producto_id, version, estado, fecha_creacion, creado_por_id)
VALUES (2, 'v1.0', 'APROBADA', CURRENT_DATE, 1);

-- Insertar detalles de fórmula
-- Agua Purificada (producto_id 3)
INSERT INTO detalle_formula (formula_id, insumo_id, cantidad_necesaria, unidad_medida_id, obligatorio)
VALUES (1, 3, 256.133, 1, 1);

-- Linaza Polvo (producto_id 4)
INSERT INTO detalle_formula (formula_id, insumo_id, cantidad_necesaria, unidad_medida_id, obligatorio)
VALUES (1, 4, 1.215, 1, 1);

-- Malva Hoja Entero (producto_id 5)
INSERT INTO detalle_formula (formula_id, insumo_id, cantidad_necesaria, unidad_medida_id, obligatorio)
VALUES (1, 5, 1.215, 1, 1);

-- Insertar orden de producción con lote_produccion
INSERT INTO orden_produccion (
    lote_produccion,
    producto_id,
    responsable_id,
    fecha_inicio,
    fecha_fin,
    cantidad_programada,
    cantidad_producida,
    estado
) VALUES (
    'LOTE-PRUEBA-001',
    2,  -- producto_id = Bebida enriquecida
    1,  -- usuario_id = testuser
    CURRENT_TIMESTAMP,
    NULL,
    100,
    0,
    'EN_PROCESO'
);

INSERT INTO lotes_productos (
    codigo_lote,
    fecha_fabricacion,
    fecha_vencimiento,
    stock_lote,
    estado,
    temperatura_almacenamiento,
    fecha_liberacion,
    producto_id,
    almacen_id,
    orden_produccion_id
) VALUES (
    'LOTE-PRUEBA-001',
    DATEADD('DAY', -3, CURRENT_DATE),
    DATEADD('DAY', 60, CURRENT_DATE),
    100.00,
    'EN_CUARENTENA',
    18.0,
    NULL,
    2,
    1,
    1
);

-- Agrega lote retenido
INSERT INTO lotes_productos (
    codigo_lote, fecha_fabricacion, fecha_vencimiento, stock_lote,
    estado, temperatura_almacenamiento, producto_id, almacen_id
) VALUES (
    'LOTE-RETENIDO-001', '2024-04-01', '2024-08-01', 50.00,
    'RETENIDO', 20.0, 1, 1
);

INSERT INTO usuarios (id, nombre_usuario, clave, nombre_completo, correo, rol, activo, bloqueado)
VALUES (99, 'usuarioInactivo', 'clave123', 'Usuario Inactivo', 'inactivo@demo.com', 'ROL_ALMACENISTA', false, false);

