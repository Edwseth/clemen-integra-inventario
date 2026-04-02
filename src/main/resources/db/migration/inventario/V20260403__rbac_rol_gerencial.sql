-- Fase 2 ROL_GERENCIAL: alta formal del rol y permiso canónico de seguimiento gerencial.
-- Se mantiene modelo híbrido actual: usuarios.rol (enum) + RBAC.

ALTER TABLE usuarios
    MODIFY COLUMN rol ENUM(
        'ROL_GERENCIAL',
        'ROL_MICROBIOLOGO',
        'ROL_JEFE_CALIDAD',
        'ROL_ANALISTA_CALIDAD',
        'ROL_JEFE_ALMACENES',
        'ROL_ALMACENISTA',
        'ROL_JEFE_PRODUCCION',
        'ROL_LIDER_HOMEOPATICOS',
        'ROL_LIDER_ALIMENTOS',
        'ROL_CONTADOR',
        'ROL_COMPRADOR',
        'ROL_PLANEADOR',
        'ROL_SUPER_ADMIN'
    ) NOT NULL;

INSERT INTO roles (codigo, nombre, activo)
SELECT 'ROL_GERENCIAL', 'Gerencial', b'1'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE codigo = 'ROL_GERENCIAL'
);

INSERT INTO permisos (codigo, modulo, accion, descripcion, activo)
SELECT 'GER_SEGUIMIENTO_READ', 'GER', 'READ', 'Lectura del seguimiento gerencial consolidado', b'1'
WHERE NOT EXISTS (
    SELECT 1 FROM permisos WHERE codigo = 'GER_SEGUIMIENTO_READ'
);

-- Sincronización idempotente para consistencia temporal usuarios.rol <-> usuarios_roles.
INSERT INTO usuarios_roles (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuarios u
JOIN roles r ON r.codigo = 'ROL_GERENCIAL'
WHERE u.rol = 'ROL_GERENCIAL'
ON DUPLICATE KEY UPDATE rol_id = rol_id;

-- Set mínimo de permisos para ROL_GERENCIAL (solo lectura transversal).
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo IN (
    'GER_SEGUIMIENTO_READ',
    'PO_PLAN_SEMANAL_READ',
    'PO_MRP_READ',
    'PO_READ',
    'BOM_FORMULA_READ',
    'PROD_OP_READ',
    'PROD_BATCH_RECORD_READ',
    'INV_LOTES_READ',
    'QC_READ'
)
WHERE r.codigo = 'ROL_GERENCIAL'
  AND NOT EXISTS (
    SELECT 1 FROM roles_permisos rp
    WHERE rp.rol_id = r.id
      AND rp.permiso_id = p.id
  );
