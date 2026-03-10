# Matriz de Trazabilidad (RTM) — Inventarios

Fuente de verdad de requisitos: `docs/auditoria/requisitos-erp.md`.

Alcance de esta auditoría: análisis técnico del backend en `src/main/java/com/willyes/clemenintegra/inventario` (controllers, services, repositories, entities y validaciones de negocio).

Criterio aplicado en esta revisión (solo backend):
- **IMPLEMENTADO_BACKEND**: existe endpoint + lógica + persistencia.
- **PARCIAL**: existe implementación incompleta incluso en backend.
- **NO_IMPLEMENTADO**: no existe implementación en backend.

| Req ID | Implementación encontrada | Archivos relevantes | Evidencia | Estado |
|---|---|---|---|---|
| INV-TR-TR-GEN-001 | Se identificó flujo de transferencias entre almacenes mediante: (1) creación y aprobación de solicitudes con `almacenOrigen` y `almacenDestino`; (2) registro de movimientos con tipo/clasificación de transferencia y persistencia de origen/destino. Incluye validaciones de existencia de almacenes y normalización de movimientos de OP a transferencia interna. | `controller/SolicitudMovimientoController.java` (POST/PUT de solicitudes), `service/SolicitudMovimientoServiceImpl.java` (resuelve y valida almacén origen/destino), `controller/MovimientoInventarioController.java` (`POST /api/movimientos`), `service/MovimientoInventarioServiceImpl.java` (normaliza a `TipoMovimiento.TRANSFERENCIA` en ciertos flujos), `model/MovimientoInventario.java` (campos `almacenOrigen`/`almacenDestino`), `repository/SolicitudMovimientoRepository.java`, `repository/MovimientoInventarioRepository.java` | Backend Controller; Backend Service; Backend Repository; Backend Entity | IMPLEMENTADO_BACKEND |
| INV-TR-TR-GEN-002 | El sistema registra movimientos de inventario vía endpoint transaccional y persiste entidad `MovimientoInventario` con producto, lote, tipo, clasificación, cantidades, fecha y usuario. También expone consulta paginada/listado de movimientos. | `controller/MovimientoInventarioController.java` (`POST /api/movimientos`, `GET /api/movimientos`, `GET /api/movimientos/filtrar`, `GET /api/movimientos/buscar`), `service/MovimientoInventarioServiceImpl.java` (lógica de registro), `model/MovimientoInventario.java`, `repository/MovimientoInventarioRepository.java` | Backend Controller; Backend Service; Backend Repository; Backend Entity | IMPLEMENTADO_BACKEND |
| INV-TR-TR-GEN-003 | Se encontró validación explícita de disponibilidad antes de salidas/ajustes: consulta de stock disponible por producto, cálculo de stock por lote (`stockLote - stockReservado`), validación con reservas de solicitud y respuesta de conflicto cuando no hay disponibilidad. | `controller/MovimientoInventarioController.java` (pre-validación de stock en `registrar`), `service/StockQueryService.java` (cálculo de stock disponible por producto/almacén), `service/MovimientoInventarioServiceImpl.java` (validaciones adicionales de disponible por lote y reservas), `model/LoteProducto.java` (campos `stockLote` y `stockReservado`), `repository/ProductoRepository.java`, `repository/LoteProductoRepository.java` | Backend Controller; Backend Service; Backend Repository; Backend Entity | IMPLEMENTADO_BACKEND |
| INV-TR-TR-GEN-004 | El lote de producto queda modelado y registrado: existe entidad de lote con código, fechas y stock; endpoint para crear lotes; y movimientos que referencian lote con validaciones de calidad y uso operativo del lote. | `model/LoteProducto.java`, `controller/LoteProductoController.java` (`POST /api/lotes`), `service/LoteProductoServiceImpl.java` (creación/gestión de lotes), `model/MovimientoInventario.java` (relación a `LoteProducto`), `service/SolicitudMovimientoServiceImpl.java` y `service/MovimientoInventarioServiceImpl.java` (validaciones sobre lote y utilizable), `repository/LoteProductoRepository.java` | Backend Controller; Backend Service; Backend Repository; Backend Entity | IMPLEMENTADO_BACKEND |
| INV-TR-TR-GEN-005 | Se permite consultar historial de movimientos por filtros y por kardex (producto/lote/rango de fechas/almacén/OP), con cálculo de entradas, salidas y saldo acumulado; esto cubre trazabilidad operativa del historial. | `controller/MovimientoInventarioController.java` (`/filtrar`, `/buscar`, listado general), `controller/KardexController.java` (`GET /api/inventario/kardex`), `service/KardexServiceImpl.java` (consulta y cálculo de saldo), `repository/MovimientoInventarioRepository.java` (consultas filtradas y `buscarParaKardex`) | Backend Controller; Backend Service; Backend Repository | IMPLEMENTADO_BACKEND |

## Resultado de Auditoría Backend

Requisitos evaluados: 5
IMPLEMENTADO_BACKEND: 5
PARCIAL: 0
NO_IMPLEMENTADO: 0

Cobertura backend del módulo Inventarios: **100%**
