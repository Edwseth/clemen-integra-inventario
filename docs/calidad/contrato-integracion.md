# Contrato de integración de Calidad con Inventario

## Responsables por acción

| Acción | Responsable primario | Participantes secundarios | Notas |
| --- | --- | --- | --- |
| Registrar evaluación (cualquier tipo) | Analista de Calidad | Microbiólogo cuando aplica microbiología | Debe adjuntar evidencia y seleccionar resultado. |
| Revisar y aprobar evaluaciones condicionadas | Jefe de Calidad | Analista de Calidad | Define condicionantes y fecha objetivo. |
| Autorizar liberación final | Jefe de Calidad | Analista/Microbiólogo (aportan datos) | No se ejecuta si hay NC abiertas o condiciones activas. |
| Cargar plan de acción CAPA | Jefe de Calidad | Responsables de área | Sólo se habilita tras registrar la NC. |

## Eventos automáticos

- **Resultado NO CONFORME**: genera Retención y No Conformidad vinculadas al lote y a la evaluación que originó el resultado.
- **Resultado CONDICIONADO**: crea Condición de uso asociada al lote y deja la Retención activa hasta que el Jefe de Calidad la cierre.
- **Resultado CONFORME**: elimina Retenciones previas asociadas al mismo motivo (si están resueltas) y no crea NC.

## Reglas de liberación

1. Todas las evaluaciones requeridas según el `TipoAnalisisCalidad` del producto deben estar registradas y con resultado **CONFORME** o **CONDICIONADO** con Condición de uso aprobada.
2. Si existen No Conformidades asociadas al lote, deben estar en estado **CERRADA** para permitir la liberación.
3. Las Condiciones de uso en estado **ACTIVA** se mantienen visibles en la interfaz de usuario de inventario y despacho hasta su cierre o hasta que el lote se consuma completamente.
4. Lotes con resultado **NO_CONFORME** sólo pueden liberarse tras re-procesamiento documentado y actualización del resultado a CONFORME o CONDICIONADO.
