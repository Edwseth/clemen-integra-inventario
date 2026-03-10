# RTM Backend - Módulo Compras

## Alcance auditado

- URS fuente: `docs/auditoria/requisitos-erp.md` (requisitos COM-001 a COM-004).
- Ruta solicitada para módulo Compras: `src/main/java/com/willyes/clemenintegra/compras`.
- Hallazgo estructural: no existe paquete `.../compras`; la implementación backend de compras está centralizada en `src/main/java/com/willyes/clemenintegra/inventario` (órdenes de compra, proveedores y recepciones).

## Matriz de trazabilidad

| Req ID | Implementación encontrada | Archivos relevantes | Evidencia | Estado |
|---|---|---|---|---|
| COM-001 | **Creación de órdenes de compra** mediante `POST /api/ordenes-compra` (alias `/api/inventario/ordenes`). Se valida proveedor existente, detalles obligatorios, productos válidos por línea, cálculo de totales/IVA, tipo de orden, código de OC y persistencia transaccional. | `inventario/controller/OrdenCompraController.java`; `inventario/service/OrdenCompraService.java`; `inventario/repository/OrdenCompraRepository.java`; `inventario/model/OrdenCompra.java`; `inventario/model/OrdenCompraDetalle.java` | **Backend Controller:** método `crear(...)` en `OrdenCompraController`.<br>**Backend Service:** generación de código OC y cálculo de fecha compromiso en `OrdenCompraService`.<br>**Backend Repository:** `save`/consultas en `OrdenCompraRepository`.<br>**Backend Entity:** `OrdenCompra` + `OrdenCompraDetalle` modelan cabecera y líneas con cantidades, IVA y cantidades recibidas. | IMPLEMENTADO_BACKEND |
| COM-002 | **Registro de proveedor** mediante `POST /api/proveedores` con validación de duplicidad por identificación y correo; listado y autocomplete para reutilización en compras. | `inventario/controller/ProveedorController.java`; `inventario/service/ProveedorServiceImpl.java`; `inventario/repository/ProveedorRepository.java`; `inventario/model/Proveedor.java` | **Backend Controller:** `crear(...)` en `ProveedorController` valida `existsByIdentificacion/existsByEmail` y guarda proveedor.<br>**Backend Service:** `listar(...)` y `buscarAutocomplete(...)` en `ProveedorServiceImpl`.<br>**Backend Repository:** validaciones de existencia y búsquedas en `ProveedorRepository`.<br>**Backend Entity:** `Proveedor` con identificación, contacto y estado activo. | IMPLEMENTADO_BACKEND |
| COM-003 | **Recepción de mercancía** soportada en backend por registro de movimientos (`POST /api/movimientos` con `TipoMovimiento.RECEPCION`) y consulta de recepciones (`GET /api/recepciones`, `GET /api/recepciones/{codigo}`). Se crea/recupera cabecera `RecepcionOC`, se asocia a OC y se genera/actualiza lote. | `inventario/controller/MovimientoInventarioController.java`; `inventario/service/MovimientoInventarioServiceImpl.java`; `inventario/controller/RecepcionOCController.java`; `inventario/service/RecepcionOCServiceImpl.java`; `inventario/repository/RecepcionOCRepository.java`; `inventario/model/RecepcionOC.java` | **Backend Controller:** alta de movimiento en `MovimientoInventarioController.registrar(...)` y consulta de recepción por código en `RecepcionOCController`.<br>**Backend Service:** en `MovimientoInventarioServiceImpl` se exige `ordenCompraId`, valida OC activa y producto perteneciente; luego llama `recepcionOCService.findOrCreateCabecera(...)`.<br>**Backend Repository:** `RecepcionOCRepository` busca por OC/fecha y carga detalle por código.<br>**Backend Entity:** `RecepcionOC` persiste cabecera de recepción enlazada a orden, almacén y proveedor.<br>**Integración Inventarios/Calidad:** creación de lote con cuarentena automática si el producto requiere análisis (estado inicial `EN_CUARENTENA` y envío a almacén de cuarentena). | IMPLEMENTADO_BACKEND |
| COM-004 | **Control de cantidades recibidas** en recepción: se incrementa `cantidadRecibida` por línea de OC y se bloquea sobre-recepción (`422 ORDEN_CANTIDAD_EXCEDIDA`) cuando excede la cantidad solicitada. Además, el estado de la OC evoluciona según recepción parcial/completa. | `inventario/service/MovimientoInventarioServiceImpl.java`; `inventario/model/OrdenCompraDetalle.java`; `inventario/service/OrdenCompraService.java`; `inventario/repository/OrdenCompraRepository.java` | **Validación de negocio:** `actualizarOrdenCompraDetalle(...)` calcula `nuevaCantidad = recibida + cantidad`; si `nuevaCantidad > solicitada` lanza error `ORDEN_CANTIDAD_EXCEDIDA`.<br>**Backend Entity:** campo `cantidadRecibida` en `OrdenCompraDetalle`.<br>**Backend Service:** `evaluarYActualizarEstado(...)` en `OrdenCompraService` cambia a `PARCIALMENTE_RECIBIDA` o `RECIBIDA_COMPLETAMENTE` según avance de recepción.<br>**Backend Repository:** listados/queries usan `cantidad` vs `cantidadRecibida` para atrasos y trazabilidad. | IMPLEMENTADO_BACKEND |

## Integraciones identificadas (Compras ↔ otros módulos)

- **Inventarios:** integración directa y principal (OC, movimiento de recepción, lotes, stock y almacenes) en el paquete `inventario`.
- **Calidad:** integración backend en recepción por reglas de análisis (`requiereFisico/requiereQuimico/requiereMicro`) que fuerzan lote en cuarentena y bloquean flujo operativo hasta liberación.
- **Proveedores:** integración explícita por FK en `OrdenCompra` y `RecepcionOC`, y API dedicada `/api/proveedores`.

## Conclusión técnica

Para los requisitos URS auditados (`COM-001` a `COM-004`), la cobertura backend es **completa** bajo el criterio solicitado (`IMPLEMENTADO_BACKEND`). No obstante, existe una desviación de organización respecto al directorio esperado: la funcionalidad de Compras no está en `.../compras`, sino implementada dentro del módulo `inventario`.

## Resultado de Auditoría Backend

Requisitos evaluados: 5
IMPLEMENTADO_BACKEND: 5
PARCIAL: 0
NO_IMPLEMENTADO: 0

Cobertura backend del módulo Inventarios: **100%**