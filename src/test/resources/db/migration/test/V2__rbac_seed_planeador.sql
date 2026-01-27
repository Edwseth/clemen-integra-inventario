MERGE INTO roles (codigo, nombre, activo)
KEY (codigo)
VALUES ('ROL_PLANEADOR', 'Planeador', TRUE);

MERGE INTO permisos (codigo, modulo, accion, descripcion, activo)
KEY (codigo)
VALUES ('PROD_OP_CREATE', 'PRODUCCION', 'CREATE', 'Crear orden de producción', TRUE);

INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'PROD_OP_CREATE'
WHERE r.codigo = 'ROL_PLANEADOR'
  AND NOT EXISTS (
      SELECT 1
      FROM roles_permisos rp
      WHERE rp.rol_id = r.id
        AND rp.permiso_id = p.id
  );
