-- V20251235 - Normalización de enums y columnas pendientes para ddl-auto=validate
-- Objetivo: alinear MySQL con las entidades (enums en MAYÚSCULA + columnas faltantes)

-- ============================================
-- 1) inventario.conteos_ciclicos.estado
-- ============================================
UPDATE conteos_ciclicos
SET estado = UPPER(estado);
UPDATE conteos_ciclicos
SET estado = 'BORRADOR'
WHERE estado IS NULL OR estado NOT IN ('BORRADOR','EN_CONTEO','CERRADO','APLICADO');
ALTER TABLE conteos_ciclicos
    MODIFY COLUMN estado ENUM('BORRADOR','EN_CONTEO','CERRADO','APLICADO') NOT NULL DEFAULT 'BORRADOR';

-- ============================================
-- 2) inventario.orden_compra_documentos.tipo_documento
-- ============================================
UPDATE orden_compra_documentos
SET tipo_documento = UPPER(tipo_documento);
UPDATE orden_compra_documentos
SET tipo_documento = 'FACTURA'
WHERE tipo_documento IS NULL OR tipo_documento NOT IN ('CERTIFICADO','FACTURA','GUIA');
ALTER TABLE orden_compra_documentos
    MODIFY COLUMN tipo_documento ENUM('CERTIFICADO','FACTURA','GUIA') NOT NULL;

-- ============================================
-- 3) inventario.productos - enums de calidad y modo de control
-- ============================================
UPDATE productos
SET tipo_analisis_calidad = UPPER(tipo_analisis_calidad);
UPDATE productos
SET tipo_analisis_calidad = 'FISICO'
WHERE tipo_analisis_calidad IN ('FISICO_QUIMICO','FISICO QUIMICO','FISICOQUIMICO');
UPDATE productos
SET tipo_analisis_calidad = 'QUIMICO_MICROBIOLOGICO'
WHERE tipo_analisis_calidad IN ('MICROBIOLOGICO','MICRO','QUIMICO');
UPDATE productos
SET tipo_analisis_calidad = 'NINGUNO'
WHERE tipo_analisis_calidad IS NULL
   OR tipo_analisis_calidad NOT IN ('NINGUNO','FISICO','QUIMICO_MICROBIOLOGICO','AMBOS');
ALTER TABLE productos
    MODIFY COLUMN tipo_analisis_calidad ENUM('NINGUNO','FISICO','QUIMICO_MICROBIOLOGICO','AMBOS') NOT NULL DEFAULT 'NINGUNO';

UPDATE productos
SET modo_control_inventario = UPPER(modo_control_inventario);
UPDATE productos
SET modo_control_inventario = 'CONTROL_STOCK'
WHERE modo_control_inventario IS NULL
   OR modo_control_inventario NOT IN ('CONTROL_STOCK','SIN_CONTROL_STOCK');
ALTER TABLE productos
    MODIFY COLUMN modo_control_inventario ENUM('CONTROL_STOCK','SIN_CONTROL_STOCK') NOT NULL DEFAULT 'CONTROL_STOCK';

-- ============================================
-- 4) inventario.almacenes.categoria_almacen
-- ============================================
UPDATE almacenes
SET categoria_almacen = UPPER(categoria_almacen);
UPDATE almacenes
SET categoria_almacen = 'MATERIA_PRIMA'
WHERE categoria_almacen IS NULL
   OR categoria_almacen NOT IN ('MATERIA_PRIMA','PRODUCTO_TERMINADO','MATERIAL_EMPAQUE','SUMINISTROS','REPUESTOS','OBSOLETOS','PRODUCTO_SEMI_ELABORADO');
ALTER TABLE almacenes
    MODIFY COLUMN categoria_almacen ENUM('MATERIA_PRIMA','PRODUCTO_TERMINADO','MATERIAL_EMPAQUE','SUMINISTROS','REPUESTOS','OBSOLETOS','PRODUCTO_SEMI_ELABORADO') NOT NULL;

-- ============================================
-- 5) inventario.solicitudes_movimiento.estado
-- ============================================
UPDATE solicitudes_movimiento
SET estado = UPPER(estado);
UPDATE solicitudes_movimiento
SET estado = 'PENDIENTE'
WHERE estado IS NULL
   OR estado NOT IN ('PENDIENTE','AUTORIZADA','PARCIAL','RECHAZADO','EJECUTADA','RESERVADA','ATENDIDA','CERRADA','CANCELADA');
ALTER TABLE solicitudes_movimiento
    MODIFY COLUMN estado ENUM('PENDIENTE','AUTORIZADA','PARCIAL','RECHAZADO','EJECUTADA','RESERVADA','ATENDIDA','CERRADA','CANCELADA') NOT NULL;

-- ============================================
-- 6) produccion.orden_produccion.batch_record_estado
-- ============================================
UPDATE orden_produccion
SET batch_record_estado = UPPER(batch_record_estado);
UPDATE orden_produccion
SET batch_record_estado = 'BORRADOR'
WHERE batch_record_estado IS NULL
   OR batch_record_estado NOT IN ('BORRADOR','EN_REVISION_CALIDAD','APROBADO','RECHAZADO');
ALTER TABLE orden_produccion
    MODIFY COLUMN batch_record_estado ENUM('BORRADOR','EN_REVISION_CALIDAD','APROBADO','RECHAZADO') NOT NULL DEFAULT 'BORRADOR';

-- ============================================
-- 7) planeacion.* (MRP y planes de producción)
-- ============================================
-- plan_produccion_semanal.estado
UPDATE plan_produccion_semanal
SET estado = UPPER(estado);
UPDATE plan_produccion_semanal
SET estado = 'BORRADOR'
WHERE estado IS NULL OR estado NOT IN ('BORRADOR','CONFIRMADO','CERRADO');
ALTER TABLE plan_produccion_semanal
    MODIFY COLUMN estado ENUM('BORRADOR','CONFIRMADO','CERRADO') NOT NULL DEFAULT 'BORRADOR';

-- corridas_mrp.estado
UPDATE corridas_mrp
SET estado = UPPER(estado);
UPDATE corridas_mrp
SET estado = 'EN_PROCESO'
WHERE estado IS NULL OR estado NOT IN ('EN_PROCESO','COMPLETADA','ERROR');
ALTER TABLE corridas_mrp
    MODIFY COLUMN estado ENUM('EN_PROCESO','COMPLETADA','ERROR') NULL DEFAULT NULL;

-- sugerencias_abastecimiento.tipo y estado
UPDATE sugerencias_abastecimiento
SET tipo = UPPER(tipo);
UPDATE sugerencias_abastecimiento
SET tipo = 'COMPRA'
WHERE tipo IS NULL OR tipo NOT IN ('COMPRA','FABRICAR');
UPDATE sugerencias_abastecimiento
SET estado = UPPER(estado);
UPDATE sugerencias_abastecimiento
SET estado = 'PENDIENTE'
WHERE estado IS NULL OR estado NOT IN ('PENDIENTE','APROBADA','DESCARTADA','CONVERTIDA');
ALTER TABLE sugerencias_abastecimiento
    MODIFY COLUMN tipo ENUM('COMPRA','FABRICAR') NOT NULL,
    MODIFY COLUMN estado ENUM('PENDIENTE','APROBADA','DESCARTADA','CONVERTIDA') NOT NULL DEFAULT 'PENDIENTE';

-- ============================================
-- 8) documental.documentos (tipo/area/estado)
-- ============================================
UPDATE documentos
SET tipo = UPPER(tipo),
    area = UPPER(area),
    estado = UPPER(estado);
UPDATE documentos
SET tipo = 'OTRO'
WHERE tipo IS NULL OR tipo NOT IN ('PROCEDIMIENTO','FORMATO','REGISTRO','BITACORA_SANITIZACION','BITACORA_CALIBRACION','OTRO');
UPDATE documentos
SET area = 'GENERAL'
WHERE area IS NULL OR area NOT IN ('CALIDAD','PRODUCCION','INVENTARIOS','MANTENIMIENTO','COMPRAS','GENERAL');
UPDATE documentos
SET estado = 'EN_ELABORACION'
WHERE estado IS NULL OR estado NOT IN ('EN_ELABORACION','VIGENTE','OBSOLETO');
ALTER TABLE documentos
    MODIFY COLUMN tipo ENUM('PROCEDIMIENTO','FORMATO','REGISTRO','BITACORA_SANITIZACION','BITACORA_CALIBRACION','OTRO') NOT NULL,
    MODIFY COLUMN area ENUM('CALIDAD','PRODUCCION','INVENTARIOS','MANTENIMIENTO','COMPRAS','GENERAL') NOT NULL,
    MODIFY COLUMN estado ENUM('EN_ELABORACION','VIGENTE','OBSOLETO') NOT NULL;

-- ============================================
-- 9) inventario.ordenes_compra - columnas pendientes de la entidad
-- ============================================
ALTER TABLE ordenes_compra
    ADD COLUMN IF NOT EXISTS condiciones_pago ENUM('ANTICIPADO','CONTADO','DIAS_30','DIAS_60') NULL,
    ADD COLUMN IF NOT EXISTS comprador VARCHAR(100) NULL;

