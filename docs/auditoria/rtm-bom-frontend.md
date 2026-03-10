# RTM Frontend — Módulo BOM

> Nota de auditoría: en este repositorio no se encontró `docs/auditoria/requisitos-erp.md` durante el análisis, por lo que la trazabilidad se construye con los Req ID solicitados (`BOM-001` a `BOM-004`) y evidencia de implementación en `src/modules/bom`.

| Req ID | Pantalla | Componentes | Endpoint usado | Estado |
|---|---|---|---|---|
| BOM-001 | **Ver Fórmulas** (`/bom/consultar`) y **Detalle de Fórmula** (`/bom/formulas/:id`) | `VerFormulas`, `DetalleFormula`, tabla de fórmulas (filtros por estado/producto), badges de estado, acciones de ver/clonar/cambio de estado | `GET /bom/formulas`, `GET /bom/formulas/{id}`, `POST /bom/formulas/{id}/clonar`, `POST /bom/formulas/{id}/cambiar-estado` | IMPLEMENTADO_FRONTEND |
| BOM-002 | **Registrar Fórmula** (`/bom/crear`) | `RegistrarFormula`, `InsumoAutocomplete`, formulario React Hook Form (datos generales + composición + archivo) | `POST /bom/formulas`, `GET /productos/buscar-fabricables` (vía `buscarProductosFabricablesAutocomplete`), `GET /productos/insumos/autocomplete` (vía `buscarInsumosAutocomplete`) | IMPLEMENTADO_FRONTEND |
| BOM-003 | **Consulta de Fórmula Activa** (`/bom/activa`) | `ConsultaFormulaActiva`, selector de producto terminado, tabla de detalles de la fórmula activa | `GET /api/bom/productos-terminados`, `GET /bom/formulas/producto/{productoId}/formula-activa` | IMPLEMENTADO_FRONTEND |
| BOM-004 | **Gestión de Detalles y Documentos** (`/bom/detalles*`, `/bom/documentos*`) y acciones embebidas en detalle de fórmula | `ListarDetalles`, `NuevoDetalle`, `EditarDetalle`, `ListarDocumentos`, `NuevoDocumento`, bloque de documentos en `DetalleFormula` | Detalles: `GET/POST/PUT/DELETE /bom/detalles[/{id}]`. Documentos: `GET /bom/formulas/{formulaId}/documentos`, `POST /bom/formulas/{formulaId}/documentos`, `DELETE /bom/formulas/documentos/{id}`, `GET /bom/formulas/documentos/{id}/descargar` | IMPLEMENTADO_FRONTEND |

## Inventario técnico identificado en `src/modules/bom`

- **Páginas:** `VerFormulas`, `RegistrarFormula`, `DetalleFormula`, `ListarDetalles`, `NuevoDetalle`, `EditarDetalle`, `ListarDocumentos`, `NuevoDocumento`.
- **Componentes clave:** `ConsultaFormulaActiva`, `InsumoAutocomplete`.
- **Formularios:** alta de fórmula, alta/edición de detalle, alta de documento, formularios de filtro en listados.
- **Tablas:** listado de fórmulas, detalle de insumos por fórmula, listado de detalles, listado de documentos, tabla de fórmula activa.
- **Hooks/servicios frontend:** `formulaProductoService`, `formulaService`, `detalleFormulaService`, `documentoFormulaService`.
- **Llamadas a API BOM observadas:**
    - Fórmulas: `/bom/formulas`, `/bom/formulas/{id}`, `/bom/formulas/{id}/clonar`, `/bom/formulas/{id}/cambiar-estado`, `/bom/formulas/{id}/estado`, `/bom/formulas/producto/{productoId}/formula-activa`.
    - Detalles: `/bom/detalles`, `/bom/detalles/{id}`.
    - Documentos: `/bom/formulas/{formulaId}/documentos`, `/bom/formulas/documentos/{id}`, `/bom/formulas/documentos/{id}/descargar`.
    - Soporte UI (autocompletados/productos): `/api/bom/productos-terminados` y endpoints consumidos por `productoService` para productos fabricables/insumos.