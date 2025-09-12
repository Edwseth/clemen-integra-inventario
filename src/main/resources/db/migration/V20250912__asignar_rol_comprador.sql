-- Asignar rol comprador a usuarios existentes e insertar usuario por defecto
UPDATE usuarios SET rol = 'ROL_COMPRADOR' WHERE nombre_usuario IN ('comprador');

INSERT INTO usuarios (id, nombre_usuario, clave, nombre_completo, correo, rol, activo, bloqueado)
VALUES (100, 'comprador', '$2a$10$E6Qdl7/xZsmqilMaha3qHOrMIOLjmzT9gDC11FXGeUXLEVp3G5UpG', 'Usuario Comprador', 'comprador@demo.com', 'ROL_COMPRADOR', true, false)
ON DUPLICATE KEY UPDATE rol = VALUES(rol);
