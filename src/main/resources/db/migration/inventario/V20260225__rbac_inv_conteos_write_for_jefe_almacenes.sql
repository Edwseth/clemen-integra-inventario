INSERT INTO permisos (codigo, modulo, accion, descripcion, activo)
VALUES ('INV_CONTEOS_WRITE', 'INV', 'WRITE', 'Gestion de conteos ciclicos', b'1')
ON DUPLICATE KEY UPDATE
    modulo = VALUES(modulo),
    accion = VALUES(accion),
    descripcion = VALUES(descripcion),
    activo = VALUES(activo);

INSERT IGNORE INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'INV_CONTEOS_WRITE'
WHERE r.codigo = 'ROL_JEFE_ALMACENES';

DELETE rp
FROM roles_permisos rp
JOIN roles r ON r.id = rp.rol_id
JOIN permisos p ON p.id = rp.permiso_id
WHERE r.codigo = 'ROL_JEFE_ALMACENES'
  AND p.codigo IN ('INV_CONTEOS_APPLY', 'INV_CONTEOS_CLOSE');