-- Normaliza valores existentes para ajustarlos al dominio esperado en minúscula.
UPDATE plantillas_analisis
SET tipo_analisis = LOWER(TRIM(tipo_analisis))
WHERE tipo_analisis IS NOT NULL;

-- Mapeos defensivos de variantes históricas/alternativas.
UPDATE plantillas_analisis
SET tipo_analisis = 'micro'
WHERE tipo_analisis IN ('microbiologico', 'microbiológica', 'microbio');

UPDATE plantillas_analisis
SET tipo_analisis = 'fisico'
WHERE tipo_analisis IN ('fisico-quimico', 'fisico_quimico');

-- Falla de forma controlada si quedan valores fuera del dominio permitido.
SET @invalid_tipo_analisis_count := (
    SELECT COUNT(*)
    FROM plantillas_analisis
    WHERE tipo_analisis IS NULL
       OR tipo_analisis NOT IN ('fisico','quimico','micro')
);

SET @invalid_tipo_analisis_values := (
    SELECT COALESCE(
        GROUP_CONCAT(DISTINCT COALESCE(tipo_analisis, 'NULL') ORDER BY COALESCE(tipo_analisis, 'NULL') SEPARATOR ', '),
        'sin_valores_invalidos'
    )
    FROM plantillas_analisis
    WHERE tipo_analisis IS NULL
       OR tipo_analisis NOT IN ('fisico','quimico','micro')
);

SET @validation_error_message := CONCAT(
    'Migracion V20260316 abortada: valores invalidos en plantillas_analisis.tipo_analisis => ',
    @invalid_tipo_analisis_values
);

SET @validation_query := IF(
    @invalid_tipo_analisis_count > 0,
    CONCAT('SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''', REPLACE(@validation_error_message, '''', ''''''), ''''),
    'DO 0'
);

PREPARE stmt FROM @validation_query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verificación explícita solicitada para diagnóstico en logs de migración.
SELECT tipo_analisis, COUNT(*) AS total
FROM plantillas_analisis
WHERE tipo_analisis NOT IN ('fisico','quimico','micro')
GROUP BY tipo_analisis;

ALTER TABLE plantillas_analisis
    MODIFY COLUMN tipo_analisis ENUM('fisico','quimico','micro') NOT NULL;
