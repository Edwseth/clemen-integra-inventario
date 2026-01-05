ALTER TABLE bitacora_cambios_inventario
    ADD COLUMN accion VARCHAR(60) NULL,
    ADD COLUMN observacion VARCHAR(500) NULL,
    ADD COLUMN usuario_nombre VARCHAR(255) NULL;

UPDATE bitacora_cambios_inventario b
LEFT JOIN usuarios u ON u.id = b.usuarios_id
SET b.accion = COALESCE(b.accion, 'ACTUALIZACION'),
    b.observacion = COALESCE(b.observacion, ''),
    b.usuario_nombre = COALESCE(b.usuario_nombre, COALESCE(u.nombre_completo, ''));

ALTER TABLE bitacora_cambios_inventario
    MODIFY COLUMN accion VARCHAR(60) NOT NULL,
    MODIFY COLUMN observacion VARCHAR(500) NOT NULL,
    MODIFY COLUMN usuario_nombre VARCHAR(255) NOT NULL;
