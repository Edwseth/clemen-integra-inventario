# Reglas de transición de estado de lote

La siguiente matriz resume los efectos que provoca cada resultado de evaluación de calidad sobre los lotes, diferenciando entre materias primas/ material de empaque (MP/ME) y producto terminado (PT).

| Tipo de producto | Resultado de la evaluación | Estado del lote después de la evaluación | ¿Crea Retención? | ¿Crea No Conformidad? | ¿Crea Condición de uso? | Precondiciones para liberar el lote |
| --- | --- | --- | --- | --- | --- | --- |
| MP / ME | CONFORME | Se mueve a `DISPONIBLE`. | No. | No. | No. | Evaluación registrada como CONFORME. |
| MP / ME | CONDICIONADO | Permanece en `EN_CUARENTENA` hasta que se defina uso condicionado. | Sí, Retención automática para documentar la limitación. | Sí, NC vinculada a la retención. | Sí, Condición de uso obligatoria con alcance y vigencia definidos. | Requiere aprobación del Jefe de Calidad y resolución de la Condición de uso. |
| MP / ME | NO CONFORME | Cambia a `RECHAZADO`. | Sí, Retención automática. | Sí, NC automática para seguimiento. | No. | Solo liberable tras re-tratamiento y cierre de la NC; de lo contrario se descarta. |
| PT | CONFORME | Se mueve a `DISPONIBLE` y puede liberarse al almacén de producto terminado. | No. | No. | No. | Evaluación CONFORME registrada y sin NC abiertas. |
| PT | CONDICIONADO | Permanece en `RETENIDO` hasta decisión de liberación condicionada. | Sí, Retención automática para control de stock. | Sí, NC vinculada (clasificada como menor). | Sí, Condición de uso que debe mostrarse en despacho. | Liberación requiere aprobación del Jefe de Calidad, NC en estado CERRADA y condición marcada como vigente. |
| PT | NO CONFORME | Cambia a `RECHAZADO` y se bloquean movimientos de salida. | Sí, Retención automática. | Sí, NC de severidad mayor. | No. | Requiere re-procesamiento documentado y cierre de la NC; de lo contrario se dispone como desperdicio. |

## Consideraciones adicionales

- Toda retención creada por resultados CONDICIONADO o NO CONFORME debe referenciar el lote y la evaluación que la originó.
- Las Condiciones de uso activas deben ser visibles en la UI de despacho hasta que el lote se consuma o la condición se cierre explícitamente.
- La liberación final siempre la ejecuta el Jefe de Calidad, quien valida que las evaluaciones requeridas estén en estado CONFORME o que las condiciones asociadas hayan sido resueltas.