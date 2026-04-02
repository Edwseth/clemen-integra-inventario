# Seguimiento Gerencial Transversal del Plan Semanal (Backend V1)

## 1) Endpoint documentado

- **Ruta:** `GET /api/gerencial/planes-semanales/{planSemanalId}/seguimiento`
- **Propósito funcional:** entregar una lectura gerencial consolidada del avance de un plan semanal de producción, cruzando señales de planeación, BOM, MRP, compras, producción, inventario y calidad.
- **Contrato de respuesta:** objeto raíz con:
  - `summary`: métricas agregadas del plan.
  - `items[]`: detalle por ítem de plan.

### Estructura general de respuesta

```json
{
  "summary": {
    "planSemanalId": 123,
    "semanaInicio": "2026-03-30",
    "semanaFin": "2026-04-05",
    "estadoPlan": "CONFIRMADO",
    "totalItems": 10,
    "itemsNoIniciados": 1,
    "itemsEnProceso": 4,
    "itemsEnRiesgo": 2,
    "itemsBloqueados": 1,
    "itemsCompletados": 1,
    "itemsCerradosConNovedad": 1,
    "porcentajeCumplimientoGeneral": 62.35,
    "totalOpGeneradas": 14,
    "totalOpCerradas": 8,
    "totalAlertas": 9,
    "totalItemsConIntervencionRequerida": 4
  },
  "items": [
    {
      "planDetalleId": 456,
      "estadoGerencial": "EN_PROCESO",
      "etapaActual": "PRODUCCION",
      "bloqueoPrincipal": { "codigo": "NONE", "mensaje": null },
      "responsableActual": { "tipo": "USUARIO", "id": 22, "nombre": "Operario" },
      "cantidadEjecutada": 120.0,
      "porcentajeCumplimiento": 80.0,
      "requiereIntervencion": false
    }
  ]
}
```

### Significado de `summary`

`summary` es una consolidación del plan semanal completo (no de un solo producto):
- resume distribución de estados gerenciales,
- totaliza OP generadas/cerradas y alertas,
- calcula el `% cumplimiento` global como **sumatoria ejecutada / sumatoria planificada** de todos los ítems.

### Significado de `items[]`

Cada elemento de `items[]` representa **exactamente un** `plan_produccion_detalle` del plan semanal consultado, con:
- estado gerencial,
- etapa operativa actual,
- bloqueo principal,
- responsable actual,
- ejecución y cumplimiento,
- alertas y metadatos de trazabilidad.

---

## 2) Construcción en el servicio agregador

### Regla de construcción base

1. Se carga `plan_produccion_semanal` con sus detalles.
2. Se construye `items[]` **1:1 desde `plan_produccion_detalle`**.
3. Para cada detalle se cruzan señales de varias capas del ERP.
4. Se construye `summary` únicamente a partir de los `items[]` ya calculados.

### Señales consolidadas por capa

- **Planeación:** estado del plan y ventana temporal.
- **BOM:** existencia de fórmula aprobada activa por producto.
- **MRP:** sugerencias pendientes de abastecimiento (señal derivada por producto).
- **Compras/Recepciones:** pendientes de recepción en OC (señal derivada por producto).
- **Producción:** OP por `plan_detalle_id`, estados y cantidades producidas.
- **Inventario/Calidad:** estado de lotes y bloqueos de calidad.

---

## 3) Reglas semánticas documentadas

## `etapaActual`

Se determina por precedencia:
1. `PLANEACION` si el plan está en borrador.
2. `BOM_FORMULA` si no existe fórmula aprobada.
3. Sin OP generadas:
   - `ABASTECIMIENTO_COMPRAS` si hay señal MRP pendiente confiable por producto.
   - `RECEPCION_INVENTARIO` si hay señal de recepción pendiente confiable por producto.
   - si no aplica lo anterior: `PRODUCCION`.
4. Con OP activas o sin cierre total: `PRODUCCION`.
5. Con lotes bloqueados por calidad: `CALIDAD`.
6. Con OP cerradas (con/sin lote final utilizable): `LIBERACION_FINAL`.

## `estadoGerencial`

Precedencia:
1. `BLOQUEADO` si existe `bloqueoPrincipal != NONE`.
2. `CERRADO_CON_NOVEDAD` si hay batch rechazado o cierre incompleto.
3. `COMPLETADO` si ejecutada >= planificada y hay lote final utilizable.
4. `EN_RIESGO` si no hay OP y está en ventana crítica.
5. `EN_PROCESO` si existen OP.
6. `NO_INICIADO` si no existen OP y no cae en riesgo.

## `bloqueoPrincipal`

Se toma el primer bloqueo aplicable en orden:
1. `SIN_FORMULA_APROBADA`
2. `ABASTECIMIENTO_PENDIENTE_CRITICO`
3. `RECEPCION_INSUFICIENTE`
4. `LOTE_BLOQUEADO_CALIDAD`
5. `OP_RETRASADA`
6. `BATCH_RECHAZADO`
7. `NONE` si no hay bloqueo.

## `responsableActual`

Regla:
- si hay bloqueo funcional, se asigna el área dueña del bloqueo (`BOM/Desarrollo`, `Compras`, `Inventarios`, `Calidad`);
- si hay OP con responsable asignado, se reporta ese usuario;
- fallback operativo: `Producción` o `Planeación`.

## `cantidadEjecutada`

Suma por ítem de las OP relacionadas al `plan_detalle_id`:
- usa `cantidadProducidaAcumulada` cuando exista,
- en su defecto usa `cantidadProducida`.

## `% cumplimiento`

Fórmula (ítem y resumen):
- `% = (ejecutada / planificada) * 100`
- escala de 2 decimales, redondeo `HALF_UP`.
- si planificada es nula o `<= 0`, retorna `0.00`.

---

## 4) Limitaciones explícitas de V1 (sin maquillaje)

- **MRP:** señal derivada por producto (no trazabilidad exacta por `plan_detalle_id`).
- **Compras/Recepciones:** señales derivadas por producto (no trazabilidad exacta por `plan_detalle_id`).
- Cuando un producto aparece en múltiples detalles del mismo plan, esas señales pueden indicar condición de producto sin atribución exacta a un único ítem.
- El endpoint es **confiable para lectura gerencial V1** (visibilidad y priorización), pero **no debe asumirse como trazabilidad perfecta end-to-end por ítem** en capas MRP/Compras/Recepciones.

---

## 5) Qué no debe asumirse incorrectamente

1. Que todas las señales son 100% trazables por `plan_detalle_id`.
2. Que un `BLOQUEADO` siempre implica problema en OP (puede venir de BOM/MRP/Compras/Calidad).
3. Que `summary` proviene de cálculos independientes: en V1 se deriva de `items[]`.
4. Que ausencia de bloqueo implica completitud logística; puede haber riesgo operativo por tiempo (`EN_RIESGO`) sin bloqueo explícito.

