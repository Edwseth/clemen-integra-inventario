-- Inserta placeholder de checklist para etapas de plantilla sin template activo
INSERT INTO checklist_etapa_template (etapa_plantilla_id, nombre_item, obligatorio, permitir_no_aplica, orden, activo, created_at)
SELECT ep.id,
       'Definir checklist operativo',
       1,
       0,
       1,
       1,
       NOW(6)
FROM etapa_plantilla ep
LEFT JOIN checklist_etapa_template cet
    ON cet.etapa_plantilla_id = ep.id
   AND cet.activo = 1
WHERE cet.id IS NULL;
