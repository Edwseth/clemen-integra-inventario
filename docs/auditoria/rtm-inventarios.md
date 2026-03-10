# Auditoría ERP vs Requisitos
## Módulo: Inventarios

Fuente de requisitos: Excel URS ERP

Nivel de auditoría: Nivel B (Funcional + Técnica)

Repositorio analizado:
- Backend
- Frontend

---

## Matriz de Trazabilidad de Requisitos (RTM)

| Req ID | Descripción | Evidencia Backend | Evidencia Frontend | Estado | Observaciones |
|------|-------------|------------------|------------------|------|---------------|
| INV-TR-GEN-001 | Registrar movimientos inventario | MovimientoInventarioController | MovimientosPage.jsx | IMPLEMENTADO | Flujo completo |
| INV-TR-GEN-002 | Ajustes inventario | AjusteInventarioController | AjustesPage.jsx | PARCIAL | Validaciones incompletas |
| INV-TR-GEN-003 | Transferencia entre almacenes | MovimientoInventarioService | TransferenciaModal.jsx | IMPLEMENTADO | OK |

---

## Resumen de cumplimiento

| Estado | Cantidad |
|------|------|
| Implementado | X |
| Parcial | X |
| No implementado | X |

---

## Gaps detectados

Lista de requisitos que no están cubiertos por el ERP.

---

## Riesgos operativos

Identificar requisitos críticos que no estén implementados.

---

## Recomendaciones técnicas

Acciones sugeridas para cerrar gaps.