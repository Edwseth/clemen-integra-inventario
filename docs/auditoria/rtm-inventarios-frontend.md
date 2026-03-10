# Auditoría Frontend — Módulo Inventarios (Nivel B: Funcional + Técnica)

## 1) Alcance y método

- **Repositorio analizado:** `permisos-front` (React + Vite).
- **Objetivo:** identificar implementación en UI de requisitos del módulo Inventarios y clasificar cada requisito como **IMPLEMENTADO**, **PARCIAL** o **NO_IMPLEMENTADO**.
- **Criterios Nivel B aplicados por requisito:**
    1. existe pantalla,
    2. existe interacción,
    3. conexión con backend,
    4. validaciones frontend.

> Nota: en el repositorio no se encontró la hoja Excel URS con el detalle textual de `INV-TR-XXX`; por ello esta RTM usa una **normalización funcional** (`INV-TR-001...INV-TR-012`) basada en capacidades de Inventarios visibles en el código frontend. Se recomienda reemplazar la columna “Descripción” por la redacción oficial URS al momento de cierre de auditoría.

---

## 2) Componentes/rutas de inventario identificados

### Rutas y páginas principales detectadas

- `/inventario/productos` → `Productos`.
- `/inventario/lotes` → `Lotes`.
- `/inventario/movimientos` → `Movimientos` (tabs: listado, recepción OC, nuevo, salida PT, devolución PT, pendientes de ubicar).
- `/inventario/ajustes` → `Ajustes`.
- `/inventario/ubicaciones` → `Ubicaciones`.
- `/inventario/kardex` → `KardexPage`.
- `/inventario/conteos` y `/inventario/conteos/:id` → `Conteos`, `ConteoDetalle`.
- `/inventario/solicitudes` → `SolicitudesMovimiento`.
- `/inventario/reportes` y `/inventario/alertas`.

### Servicios/backend relacionados

- `productoService`, `movimientoService`, `ajusteInventarioService`, `loteService`, `almacenService`, `ubicacionService`.
- Servicios de inventario en módulo: `conteoService`, `kardexService`, `lotesService`, `picklistService`, `picklistPtService`, `solicitudesService`.
- Patrón de integración: `axiosInstance` + endpoints REST (`/productos`, `/movimientos`, `/inventario/ajustes`, `/lotes`, `/ubicaciones`, `/api/inventario/kardex`, etc.).

---

## 3) Matriz de auditoría (RTM)

| Req ID | Descripción | Evidencia Frontend | Estado | Observaciones |
|---|---|---|---|---|
| INV-TR-001 | Gestión de productos (listar/filtrar/crear/editar/eliminar) | `src/modules/inventario/pages/Productos.jsx`, `src/modules/inventario/components/FormularioProducto.jsx`, `src/services/productoService.js` | IMPLEMENTADO | Pantalla completa + filtros + ABM. Conexión API a `/productos`, `/categorias`, `/unidades`. Validación con `react-hook-form` + `yup` en formulario. |
| INV-TR-002 | Registro y consulta de movimientos de inventario | `src/modules/inventario/pages/Movimientos.jsx`, `src/modules/inventario/components/ListadoMovimientos.jsx`, `src/services/movimientoService.js` | IMPLEMENTADO | Existe pantalla con tabs e interacciones de filtrado/paginación. Backend en `/movimientos/filtrar` y registro vía servicio. Validaciones UI: rango de fechas requerido y selección válida de producto en autocomplete. |
| INV-TR-003 | Gestión de lotes (consulta/filtros/detalle) | `src/modules/inventario/pages/Lotes.jsx`, `src/services/loteService.js` | IMPLEMENTADO | Pantalla de lotes con filtros por producto/estado/almacén, detalle expandible y trazabilidad OP. Backend en `/lotes`. Validación funcional de filtros y manejo de errores API. |
| INV-TR-004 | Ajustes de inventario (alta y consulta histórica) | `src/modules/inventario/pages/Ajustes.jsx`, `src/services/ajusteInventarioService.js`, `src/modules/inventario/hooks/useLotesByProductoAlmacen.js` | IMPLEMENTADO | Modal de registro + tabla histórica. Backend en `/inventario/ajustes`. Validación robusta con `yup` (cantidad > 0, lote/almacén/producto requeridos, motivo/observación obligatorios en ajuste negativo). |
| INV-TR-005 | Kardex por producto/lote/fechas/almacén | `src/modules/inventario/pages/KardexPage.jsx`, `src/modules/inventario/services/kardexService.js` | IMPLEMENTADO | Pantalla con filtros y consulta de movimientos valorizados. Backend en `/api/inventario/kardex` y resolución de producto por lote. Validaciones en frontend para evitar consulta sin producto/SKU y manejo de errores. |
| INV-TR-006 | Conteos físicos de inventario | `src/modules/inventario/pages/Conteos.jsx`, `src/modules/inventario/pages/ConteoDetalle.jsx`, `src/modules/inventario/services/conteoService.js` | IMPLEMENTADO | Pantalla de listado + creación y detalle. Backend en `/inventario/conteos` (listar, crear, iniciar, cerrar, aplicar). Validaciones UI (almacén obligatorio para crear, reglas de estado en detalle). |
| INV-TR-007 | Gestión de ubicaciones de almacén | `src/modules/inventario/pages/Ubicaciones.jsx`, `src/services/ubicacionService.js` | IMPLEMENTADO | Pantalla con listado/edición/creación y filtro por almacén. Backend en `/ubicaciones`. Validaciones explícitas de campos obligatorios (almacén/código). |
| INV-TR-008 | Solicitudes de movimiento de inventario | `src/modules/inventario/pages/SolicitudesMovimiento.jsx`, `src/services/solicitudMovimientoService.js`, `src/modules/inventario/services/solicitudesService.js` | PARCIAL | Se observa pantalla y servicios de consulta/decisión, pero parte del flujo depende de permisos y navegación contextual con Movimientos. Requiere contraste con URS para confirmar cobertura total (estados y reglas de aprobación). |
| INV-TR-009 | Alertas de inventario | `src/modules/inventario/pages/Alertas.jsx`, `src/services/reportesService.js`, `src/services/loteService.js` | PARCIAL | Existe página de alertas y consumo de endpoints de inventario/alertas, pero la granularidad de reglas (umbrales, vencimientos, criticidad) debe compararse contra URS oficial. |
| INV-TR-010 | Reportes de inventario | `src/modules/inventario/pages/Reportes.jsx`, `src/services/reportesService.js` | PARCIAL | Pantalla y servicios de reportes implementados; estado parcial por ausencia del catálogo URS para validar todos los reportes esperados. |
| INV-TR-011 | Administración de almacenes (consulta en módulo inventario) | `src/modules/inventario/pages/Almacenes.jsx`, `src/services/almacenService.js` | IMPLEMENTADO | Existe pantalla de consulta con búsqueda y paginación conectada a `/almacenes`. Sin operaciones de creación/edición en esta vista. |
| INV-TR-012 | Control de acceso por permisos en Inventarios | `src/modules/inventario/config/routes.js`, `src/modules/inventario/utils/permissions.js`, páginas de inventario | IMPLEMENTADO | Rutas y acciones condicionadas por permisos (`READ/WRITE` por submódulo). Se observa bloqueo de tabs y botones según rol/permiso. |

---

## 4) Hallazgos técnicos relevantes

1. **Cobertura de UI del módulo inventarios alta:** hay páginas para productos, lotes, movimientos, ajustes, kardex, conteos, ubicaciones, reportes y alertas.
2. **Integración backend consistente:** servicios centralizados con `axiosInstance` y manejo de parámetros/paginación.
3. **Validaciones frontend presentes en formularios críticos:** especialmente en Productos y Ajustes (esquemas `yup` + `react-hook-form`).
4. **Gobierno de permisos:** aplicado en rutas y acciones de UI (lectura/escritura/aprobación).
5. **Limitación para cierre 100% URS:** falta el detalle oficial de cada `INV-TR-XXX`; por tanto, los casos en estado **PARCIAL** requieren trazabilidad 1:1 contra Excel URS.

---

## 5) Recomendaciones para cierre de auditoría

- Sustituir esta matriz base por el listado oficial de requisitos `INV-TR-XXX` del Excel URS.
- Para cada `INV-TR-XXX`, conservar el estado y expandir evidencia con líneas de código y evidencia de ejecución (capturas/requests) cuando aplique.
- Confirmar con QA funcional los requisitos marcados como **PARCIAL** (Solicitudes, Alertas, Reportes) por posible dependencia de reglas de negocio no inferibles sólo desde frontend.
