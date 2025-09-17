-- Garantiza una sola fila por combinación de producto, código de lote y almacén.
-- Antes de ejecutar este script en producción, consolidar duplicados exactos
-- (mismo productos_id, codigo_lote y almacenes_id) para evitar errores.

ALTER TABLE lotes_productos
    ADD CONSTRAINT unq_lote_por_almacen UNIQUE (productos_id, codigo_lote, almacenes_id);
