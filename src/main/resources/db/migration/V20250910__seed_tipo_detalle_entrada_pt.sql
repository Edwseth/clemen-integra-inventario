-- Ensure ENTRADA_PRODUCTO_TERMINADO has ID 6
INSERT INTO tipos_movimiento_detalle (id, descripcion)
VALUES (6, 'ENTRADA_PRODUCTO_TERMINADO')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);
