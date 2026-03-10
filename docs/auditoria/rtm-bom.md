# Matriz de Trazabilidad (RTM) — Módulo BOM (Backend)

## Alcance

- **Fuente de verdad URS:** `docs/auditoria/requisitos-erp.md`.
- **Requisitos auditados:** `BOM-001`, `BOM-002`, `BOM-003`, `BOM-004`.
- **Código analizado (backend):** `src/main/java/com/willyes/clemenintegra/bom`.
- **Nota técnica:** en el repositorio el módulo BOM está en `com/willyes/clemenintegra/bom` (no en `modules/bom`).

## Resultado de auditoría

| Req ID | Implementación encontrada | Archivos relevantes | Evidencia | Estado |
|---|---|---|---|---|
| BOM-001 | Se implementa definición de fórmulas de producto con CRUD de fórmula y CRUD de detalles/insumos. El alta permite registrar producto, versión, estado, observación, insumos (cantidad/unidad/tipo) y adjuntar documento. | `FormulaProductoController`, `DetalleFormulaController`, `FormulaProductoServiceImpl`, `DetalleFormulaServiceImpl`, `FormulaProductoRepository`, `DetalleFormulaRepository`, `FormulaProducto`, `DetalleFormula` | **Backend Controller:** `POST /api/bom/formulas`, `PUT /api/bom/formulas/{id}`, `GET /api/bom/formulas/{id}`, `POST/PUT/DELETE /api/bom/detalles`.<br>**Backend Service:** `guardar(...)` de fórmula y detalle, con verificación de fórmula editable.<br>**Backend Repository:** persistencia y consultas por filtros de detalle.<br>**Backend Entity:** relación `FormulaProducto` ↔ `DetalleFormula` con cantidades y unidad de medida.<br>**Validaciones de negocio:** detalle exige insumo y unidad; sólo se permite modificar detalles cuando la fórmula está en `BORRADOR` o `EN_REVISION`. | IMPLEMENTADO_BACKEND |
| BOM-002 | Se implementa versionado explícito de fórmulas. Existe clonación que calcula y asigna la siguiente versión (`Vx.y`) y normalización de versión al guardar. También hay campos estructurados `versionMajor/versionMinor`. | `FormulaProductoController`, `FormulaProductoServiceImpl`, `FormulaProductoRepository`, `FormulaProducto` | **Backend Controller:** `POST /api/bom/formulas/{id}/clonar`.<br>**Backend Service:** `clonarFormula(...)`, `calcularSiguienteVersion(...)`, `formatearVersion(...)`, `parsearVersion(...)`, `normalizarVersion(...)`.<br>**Backend Repository:** `findTopByProductoIdOrderByVersionMajorDescVersionMinorDesc(...)` para tomar la última versión.<br>**Backend Entity:** campos `version`, `versionMajor`, `versionMinor` con `getVersion()` normalizado.<br>**Validaciones de negocio:** formato de versión inválido lanza error; la clonación exige fórmula y usuario válidos. | IMPLEMENTADO_BACKEND |
| BOM-003 | Se implementa consulta de insumos requeridos en detalle de fórmula y en fórmula activa para producción, incluyendo cantidades requeridas por lote de producción y disponibilidad por insumo/lotes. | `FormulaProductoController`, `DetalleFormulaController`, `FormulaProductoServiceImpl`, `FormulaProductoRepository`, `DetalleFormulaRepository`, `DetalleFormula` | **Backend Controller:** `GET /api/bom/formulas/{id}`, `GET /api/bom/detalles?formulaId=...`, `GET /api/bom/formulas/activa?productoId=...&cantidad=...`.<br>**Backend Service:** `buscarDetallePorId(...)` y `obtenerFormulaActivaPorProducto(...)` (calcula `cantidadTotalNecesaria`, stock libre FEFO, faltante, lotes y estado de suficiencia).<br>**Backend Repository:** carga de fórmula con detalles (`findByIdConDetalles`) y filtros por detalle (`findByFiltros`).<br>**Backend Entity:** `DetalleFormula` guarda insumo, unidad y cantidad necesaria.<br>**Validaciones de negocio:** si no existe fórmula activa aprobada para el producto, retorna error; cantidad de producción <= 0 se normaliza a 1 para cálculo. | IMPLEMENTADO_BACKEND |
| BOM-004 | Se implementa validación de fórmula activa mediante estado/aprobación y regla de unicidad operativa de fórmula activa por producto. Además, el flujo de estados controla aprobación formal. | `FormulaProductoController`, `FormulaProductoServiceImpl`, `FormulaProductoRepository`, `EstadoFormula`, `FormulaProducto` | **Backend Controller:** `POST /api/bom/formulas/{id}/cambiar-estado`, `GET /api/bom/formulas/activa`, `GET /api/bom/formulas/producto/{productoId}/formula-activa`.<br>**Backend Service:** `cambiarEstado(...)` valida transiciones (`BORRADOR→EN_REVISION`, `EN_REVISION→APROBADA/RECHAZADA`), activa sólo aprobadas y desactiva otras fórmulas del mismo producto al aprobar.<br>**Backend Repository:** `findByProductoIdAndEstadoAndActivoTrue(...)` para recuperar fórmula activa aprobada; `desactivarOtrasFormulasDelProducto(...)` para mantener una activa.<br>**Backend Entity/Enum:** `EstadoFormula` y campo `activo` en `FormulaProducto`.<br>**Validaciones de negocio:** transición inválida y ausencia de fórmula activa aprobada generan excepción de negocio. | IMPLEMENTADO_BACKEND |

## Conclusión

El backend BOM cubre los cuatro requisitos URS auditados para este módulo (`BOM-001` a `BOM-004`) con endpoints, servicios, repositorios, entidades y validaciones de negocio trazables. No se detectaron requisitos BOM del URS sin implementación backend dentro del alcance definido.

## Resultado de Auditoría Backend

Requisitos evaluados: 4
IMPLEMENTADO_BACKEND: 4
PARCIAL: 0
NO_IMPLEMENTADO: 0

Cobertura backend del módulo BOM: **100%**
