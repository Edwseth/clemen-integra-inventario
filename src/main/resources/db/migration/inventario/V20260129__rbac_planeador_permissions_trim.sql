delete rp
from roles_permisos rp
join roles r on r.id = rp.rol_id
join permisos p on p.id = rp.permiso_id
where r.codigo = 'ROL_PLANEADOR'
  and p.codigo in (
    'PROD_BATCH_RECORD_WRITE',
    'PROD_ETAPA_CHECKLIST_WRITE',
    'PROD_ETAPA_START',
    'PROD_ETAPA_FINISH',
    'PROD_ETAPA_CLONE',
    'PROD_OP_EDIT',
    'PROD_OP_WORKFLOW_CANCEL',
    'PROD_OP_WORKFLOW_FINALIZE'
  );
