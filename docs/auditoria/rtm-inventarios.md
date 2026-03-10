# Auditoría ERP vs Requisitos

## Módulo: Inventarios

Nivel de auditoría: Nivel B (Funcional + Técnica)

Repositorios analizados:

* Backend (Spring Boot)
* Frontend (React + Vite)

Fuente de requisitos:

URS ERP – hoja `01_Inventarios`

---

## Matriz de trazabilidad consolidada

| Req ID | Descripción | Evidencia Backend | Evidencia Frontend | Estado Consolidado | Observaciones |
|---|---|---|---|---|---|
| INV-TR-001 | Backend: Registrar movimientos de inventario. Frontend: Gestión de productos. | `MovimientoInventarioController`, `MovimientoInventarioServiceImpl`, `/api/movimientos` | `Productos.jsx`, `FormularioProducto.jsx`, `productoService.js` | IMPLEMENTADO | Ambos estados son IMPLEMENTADO; se detecta desalineación de descripción entre RTM backend y frontend para el mismo ID. |
| INV-TR-002 | Backend: Validar stock antes de salidas/ajustes. Frontend: Registro y consulta de movimientos. | `MovimientoInventarioController` (pre-check), `StockQueryService`, `MovimientoInventarioServiceImpl` | `Movimientos.jsx`, `ListadoMovimientos.jsx`, `movimientoService.js` | IMPLEMENTADO | Ambos estados son IMPLEMENTADO; requiere homologación semántica por ID contra URS oficial. |
| INV-TR-003 | Backend: Control de idempotencia en movimientos. Frontend: Gestión de lotes. | `MovimientoInventarioController` (`Idempotency-Key`), `MovimientoInventarioServiceImpl` | `Lotes.jsx`, `loteService.js` | IMPLEMENTADO | Ambos estados son IMPLEMENTADO; existe diferencia de descripción entre matrices fuente. |
| INV-TR-004 | Backend: Gestión de productos (CRUD + búsquedas). Frontend: Ajustes de inventario. | `ProductoController`, `ProductoServiceImpl`, `ProductoRepository` | `Ajustes.jsx`, `ajusteInventarioService.js`, `useLotesByProductoAlmacen.js` | IMPLEMENTADO | Ambos estados son IMPLEMENTADO; pendiente alinear redacción/alcance de requisito. |
| INV-TR-005 | Backend: Gestión de lotes. Frontend: Kardex por producto/lote/fechas/almacén. | `LoteProductoController`, `LoteProductoServiceImpl`, `LoteProductoRepository` | `KardexPage.jsx`, `kardexService.js` | IMPLEMENTADO | Ambos estados son IMPLEMENTADO; IDs no homologados funcionalmente entre documentos base. |
| INV-TR-006 | Backend: Flujo de calidad de lote (liberar/rechazar). Frontend: Conteos físicos. | `LoteProductoController` (`/liberar`, `/rechazar`, `/liberar-retenido`), `LoteProductoServiceImpl` | `Conteos.jsx`, `ConteoDetalle.jsx`, `conteoService.js` | IMPLEMENTADO | Ambos estados son IMPLEMENTADO; validar correspondencia final contra URS. |
| INV-TR-007 | Backend: Aplicar FEFO/FIFO. Frontend: Gestión de ubicaciones. | `InventarioFefoController`, `MovimientoInventarioService.simulateFefo` | `Ubicaciones.jsx`, `ubicacionService.js` | IMPLEMENTADO | Ambos estados son IMPLEMENTADO; existe desalineación de alcance entre ambas RTM de origen. |
| INV-TR-008 | Backend: Kardex con saldo acumulado. Frontend: Solicitudes de movimiento. | `KardexController`, `KardexServiceImpl`, `/api/inventario/kardex` | `SolicitudesMovimiento.jsx`, `solicitudesService.js`, `solicitudMovimientoService.js` | PARCIAL | Backend IMPLEMENTADO + Frontend PARCIAL = PARCIAL. |
| INV-TR-009 | Backend: Conteo cíclico con workflow. Frontend: Alertas de inventario. | `ConteoCiclicoController`, `ConteoCiclicoService` | `Alertas.jsx`, `reportesService.js`, `loteService.js` | PARCIAL | Backend IMPLEMENTADO + Frontend PARCIAL = PARCIAL. |
| INV-TR-010 | Backend: Ajustes vinculados a movimiento real. Frontend: Reportes de inventario. | `AjusteInventarioController`, `AjusteInventarioServiceImpl`, `MovimientoInventarioService` | `Reportes.jsx`, `reportesService.js` | PARCIAL | Backend IMPLEMENTADO + Frontend PARCIAL = PARCIAL. |
| INV-TR-011 | Backend: Alertas (stock bajo/vencidos/retenidos). Frontend: Administración de almacenes. | `AlertaInventarioController`, `AlertaInventarioService` | `Almacenes.jsx`, `almacenService.js` | IMPLEMENTADO | Ambos estados son IMPLEMENTADO; homologar definición final del requisito con URS. |
| INV-TR-012 | Backend: Reportes exportables Excel. Frontend: Control de acceso por permisos. | `ReporteInventarioController`, `ReporteInventarioService`, `InventarioReportesController` | `routes.js`, `permissions.js` y páginas inventario | IMPLEMENTADO | Ambos estados son IMPLEMENTADO; se requiere normalización por requisito oficial. |
| INV-TR-013 | Trazabilidad de inventario/lotes con consulta operacional. | `ReporteInventarioController` (`/trazabilidad-lote`), `TrazabilidadLoteController` | Sin evidencia en RTM frontend | NO_IMPLEMENTADO | Backend PARCIAL + Frontend NO_IMPLEMENTADO (sin evidencia) = NO_IMPLEMENTADO. |
| INV-TR-014 | Cierre de período de inventario / bloqueo contable de movimientos. | Sin controller/service explícito de cierre de período | Sin evidencia en RTM frontend | NO_IMPLEMENTADO | Backend NO_IMPLEMENTADO + Frontend NO_IMPLEMENTADO = NO_IMPLEMENTADO. |

---

## Resumen de cumplimiento

| Estado | Cantidad |
|---|---:|
| IMPLEMENTADO | 9 |
| PARCIAL | 3 |
| NO_IMPLEMENTADO | 2 |

---

## Hallazgos principales

- El consolidado muestra buena cobertura técnica (9/14 requisitos consolidados en IMPLEMENTADO).
- Existen 3 requisitos en estado PARCIAL (`INV-TR-008` a `INV-TR-010`) por cobertura incompleta en frontend según las RTM fuente.
- Hay 2 requisitos en NO_IMPLEMENTADO (`INV-TR-013`, `INV-TR-014`) por falta de cobertura integral o ausencia explícita de funcionalidad.
- Se detecta una **desalineación de normalización de Req ID** entre RTM backend y frontend (mismo ID, descripciones diferentes), lo que obliga a homologación formal con el URS.

---

## Riesgos operativos

- **Alto:** no contar con cierre de período (`INV-TR-014`) compromete control contable y trazabilidad temporal de movimientos.
- **Medio-alto:** trazabilidad no consolidada extremo a extremo (`INV-TR-013`) impacta investigación de desvíos por lote.
- **Medio:** requisitos parciales (`INV-TR-008` a `INV-TR-010`) pueden degradar control operativo (aprobaciones, alertamiento y explotación de reportes).
- **Medio:** desalineación de IDs entre matrices fuente puede generar decisiones de auditoría erróneas si no se homologa con URS oficial.

---

## Recomendaciones técnicas

1. Homologar de inmediato los `INV-TR-XXX` con la hoja URS `01_Inventarios` y corregir la correspondencia 1:1 de descripción por ID.
2. Definir plan de cierre para `INV-TR-013` y `INV-TR-014` con criterios de aceptación técnicos y funcionales.
3. Completar cobertura de los requisitos parciales con pruebas E2E (UI + API) y evidencia auditable (requests/responses y casos de prueba).
4. Incorporar una tabla de “mapa de equivalencias” Backend vs Frontend durante la transición, hasta cerrar la homologación URS.
5. Mantener esta RTM consolidada como fuente única de seguimiento para evitar divergencias entre documentos separados.

