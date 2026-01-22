UPDATE checklist_etapa_template
SET activo = b'1'
WHERE activo IS NULL;

ALTER TABLE checklist_etapa_template
    MODIFY COLUMN activo BIT(1) NOT NULL DEFAULT b'1';
