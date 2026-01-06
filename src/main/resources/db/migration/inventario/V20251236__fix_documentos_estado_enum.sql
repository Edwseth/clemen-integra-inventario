-- V20251236 - Normalizar documentos.estado y convertir a ENUM
-- Objetivo: corregir valores legacy en PROD y alinear ddl-auto=validate

-- 1) Normalizar a mayúsculas
UPDATE documentos
SET estado = UPPER(estado);

-- 2) Backfill/fallback para valores nulos o fuera del catálogo
UPDATE documentos
SET estado = 'VIGENTE'
WHERE estado IS NULL OR estado NOT IN ('EN_ELABORACION','VIGENTE','OBSOLETO');

-- 3) Convertir a ENUM definitivo
ALTER TABLE documentos
    MODIFY COLUMN estado ENUM('EN_ELABORACION','VIGENTE','OBSOLETO') NOT NULL;
