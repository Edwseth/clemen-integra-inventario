-- V20251236 - Normalizar documentos.tipo y documentos.estado a ENUM mayúscula
-- Objetivo: alinear ddl-auto=validate con enums de la entidad (TipoDocumento y EstadoDocumento)

-- 1) Normalizar datos existentes (UPPER)
UPDATE documentos
SET tipo = UPPER(tipo),
    estado = UPPER(estado);

-- 2) Backfill/fallback de valores nulos o inválidos
UPDATE documentos
SET tipo = 'OTRO'
WHERE tipo IS NULL OR tipo NOT IN ('PROCEDIMIENTO','FORMATO','REGISTRO','BITACORA_SANITIZACION','BITACORA_CALIBRACION','OTRO');

UPDATE documentos
SET estado = 'EN_ELABORACION'
WHERE estado IS NULL OR estado NOT IN ('EN_ELABORACION','VIGENTE','OBSOLETO');

-- 3) Convertir columnas a ENUM con valores exactos de los enums Java
ALTER TABLE documentos
    MODIFY COLUMN tipo ENUM('PROCEDIMIENTO','FORMATO','REGISTRO','BITACORA_SANITIZACION','BITACORA_CALIBRACION','OTRO') NOT NULL,
    MODIFY COLUMN estado ENUM('EN_ELABORACION','VIGENTE','OBSOLETO') NOT NULL;
