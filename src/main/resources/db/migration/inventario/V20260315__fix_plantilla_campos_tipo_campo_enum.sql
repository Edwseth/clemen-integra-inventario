-- Normaliza el contenido existente para ajustarlo a los valores válidos del enum en minúscula.
UPDATE plantilla_campos
SET tipo_campo = LOWER(TRIM(tipo_campo))
WHERE tipo_campo IS NOT NULL;

-- Falla de forma controlada si quedan valores fuera del dominio permitido.
SET @invalid_tipo_campo_count := (
    SELECT COUNT(*)
    FROM plantilla_campos
    WHERE tipo_campo IS NULL
       OR tipo_campo NOT IN ('triestado','numerico','texto','fecha','select')
);

SET @invalid_tipo_campo_values := (
    SELECT COALESCE(
        GROUP_CONCAT(DISTINCT COALESCE(tipo_campo, 'NULL') ORDER BY COALESCE(tipo_campo, 'NULL') SEPARATOR ', '),
        'sin_valores_invalidos'
    )
    FROM plantilla_campos
    WHERE tipo_campo IS NULL
       OR tipo_campo NOT IN ('triestado','numerico','texto','fecha','select')
);

SET @validation_error_message := CONCAT(
    'Migracion V20260315 abortada: valores invalidos en plantilla_campos.tipo_campo => ',
    @invalid_tipo_campo_values
);

SET @validation_query := IF(
    @invalid_tipo_campo_count > 0,
    CONCAT('SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''', REPLACE(@validation_error_message, '''', ''''''), ''''),
    'DO 0'
);

PREPARE stmt FROM @validation_query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE plantilla_campos
    MODIFY COLUMN tipo_campo ENUM('triestado','numerico','texto','fecha','select') NOT NULL;
