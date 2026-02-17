MERGE INTO roles (codigo, nombre, activo)
KEY (codigo)
VALUES ('ROL_PLANEADOR', 'Planeador', TRUE);

MERGE INTO permisos (codigo, modulo, accion, descripcion, activo)
KEY (codigo)
VALUES ('PROD_OP_CREATE', 'PRODUCCION', 'CREATE', 'Crear orden de producción', TRUE),
       ('PROD_OP_READ', 'PRODUCCION', 'READ', 'Leer orden de producción', TRUE),
       ('PROD_ETAPA_CHECKLIST_READ', 'PRODUCCION', 'READ', 'Leer checklist de etapa', TRUE),
       ('PROD_BATCH_RECORD_READ', 'PRODUCCION', 'READ', 'Leer batch record', TRUE),
       ('PROD_OP_EXPORT', 'PRODUCCION', 'EXPORT', 'Exportar orden de producción', TRUE),
       ('PROD_BATCH_RECORD_EXPORT', 'PRODUCCION', 'EXPORT', 'Exportar batch record', TRUE),
       ('PROD_REPORTS_READ', 'PRODUCCION', 'READ', 'Leer reportes de producción', TRUE),
       ('PROD_INDICADORES_READ', 'PRODUCCION', 'READ', 'Leer indicadores de producción', TRUE),
       ('PROD_INDICADORES_EXPORT', 'PRODUCCION', 'EXPORT', 'Exportar indicadores de producción', TRUE),
       ('PROD_ALERTAS_READ', 'PRODUCCION', 'READ', 'Leer alertas de producción', TRUE),
       ('INV_READ', 'INV', 'READ', 'Lectura general de inventario', TRUE),
       ('MENU_PROD', 'MENU', 'NAV', 'Visibilidad y navegación del módulo de producción', TRUE);

INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo in (
    'PROD_OP_CREATE',
    'PROD_OP_READ',
    'PROD_ETAPA_CHECKLIST_READ',
    'PROD_BATCH_RECORD_READ',
    'PROD_OP_EXPORT',
    'PROD_BATCH_RECORD_EXPORT',
    'PROD_REPORTS_READ',
    'PROD_INDICADORES_READ',
    'PROD_INDICADORES_EXPORT',
    'PROD_ALERTAS_READ',
    'INV_READ',
    'MENU_PROD'
)
WHERE r.codigo = 'ROL_PLANEADOR'
  AND NOT EXISTS (
      SELECT 1
      FROM roles_permisos rp
      WHERE rp.rol_id = r.id
        AND rp.permiso_id = p.id
  );
