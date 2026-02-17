insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'MENU_INV', 'MENU', 'NAV', 'Visibilidad y navegación del módulo de inventarios', b'1'
where not exists (select 1 from permisos where codigo = 'MENU_INV');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'MENU_PROD', 'MENU', 'NAV', 'Visibilidad y navegación del módulo de producción', b'1'
where not exists (select 1 from permisos where codigo = 'MENU_PROD');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'MENU_QC', 'MENU', 'NAV', 'Visibilidad y navegación del módulo de calidad', b'1'
where not exists (select 1 from permisos where codigo = 'MENU_QC');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'MENU_PO', 'MENU', 'NAV', 'Visibilidad y navegación del módulo de planeación', b'1'
where not exists (select 1 from permisos where codigo = 'MENU_PO');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'MENU_BOM', 'MENU', 'NAV', 'Visibilidad y navegación del módulo de fórmulas BOM', b'1'
where not exists (select 1 from permisos where codigo = 'MENU_BOM');

insert into permisos (codigo, modulo, accion, descripcion, activo)
select 'MENU_DOC', 'MENU', 'NAV', 'Visibilidad y navegación del módulo de control documental', b'1'
where not exists (select 1 from permisos where codigo = 'MENU_DOC');
