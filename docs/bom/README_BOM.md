# Módulo BOM (Fórmulas de Producto)

## Resumen funcional
El módulo BOM gestiona las fórmulas o listas de materiales asociadas a cada producto terminado. Cada fórmula define los insumos y cantidades requeridas desde Inventarios, es el insumo técnico que las órdenes de Producción consumen cuando existe una versión **APROBADA** y **activa**, y concentra la documentación y el flujo de aprobación liderado por Calidad.

## Entidades BOM
- **FormulaProducto**: referencia al `Producto`, maneja `version`, `estado`, indicador `activo` y registra usuario/fechas de creación y actualización.
- **DetalleFormula**: pertenece a una `FormulaProducto`, vincula el producto insumo con su `unidadMedida`, `cantidadNecesaria` y si es `obligatorio`.
- **DocumentoFormula**: archivo asociado a la fórmula con `ruta`, `tipoDocumento`, `nombreVisible`, `fechaRegistro` y usuario que lo cargó.
- **EstadoFormula**: enum con los estados `BORRADOR`, `EN_REVISION`, `APROBADA`, `RECHAZADA`.

## Ciclo de vida de la fórmula
```
BORRADOR → EN_REVISION → APROBADA
                      ↘
                       RECHAZADA
```
- Transiciones válidas: `BORRADOR → EN_REVISION`, `EN_REVISION → APROBADA`, `EN_REVISION → RECHAZADA`.
- Al aprobar una fórmula se marca como activa y se desactivan las demás versiones del mismo producto.
- Solo en `BORRADOR` o `EN_REVISION` se pueden editar detalles y documentos.
- Producción siempre consume una única fórmula `APROBADA` y `activa` por producto.

## Contratos de API
### Fórmulas
| Método y ruta | Descripción | DTO principal | Roles |
| --- | --- | --- | --- |
| `GET /api/bom/formulas` | Listado maestro de fórmulas. | `FormulaProductoResumenDTO` | Jefe Producción, Jefe Calidad, Super Admin |
| `GET /api/bom/formulas/{id}` | Detalle completo de una fórmula. | `FormulaProductoResponse` | Jefe Producción, Jefe Calidad, Super Admin |
| `POST /api/bom/formulas` | Crear nueva fórmula (multipart con datos y documento opcional). | `FormulaProductoResponse` | Jefe Calidad, Super Admin |
| `PUT /api/bom/formulas/{id}` | Actualizar fórmula existente. | `FormulaProductoResponse` | Jefe Calidad, Super Admin |
| `POST /api/bom/formulas/{id}/clonar` | Clonar o generar nueva versión. | `FormulaProductoResumenDTO` | Jefe Calidad, Super Admin |
| `POST /api/bom/formulas/{id}/cambiar-estado` | Cambiar estado según flujo. | `FormulaProductoResumenDTO` | Jefe Calidad, Super Admin |
| `DELETE /api/bom/formulas/{id}` | Eliminar fórmula. | — | Jefe Calidad, Super Admin |

### Producción
| Método y ruta | Descripción | DTO principal | Roles |
| --- | --- | --- | --- |
| `GET /api/bom/formulas/activa?productoId=&cantidad=` | Devuelve la fórmula activa validando insumos para una cantidad solicitada. | `FormulaProductoResponse` | Jefe Producción, Jefe Calidad, Super Admin |
| `GET /api/bom/formulas/producto/{productoId}/formula-activa` | Fórmula aprobada y activa expuesta a Producción. | `FormulaActivaProduccionDTO` | Jefe Producción, Jefe Calidad, Super Admin |

### Documentos
| Método y ruta | Descripción | DTO principal | Roles |
| --- | --- | --- | --- |
| `GET /api/bom/formulas/{formulaId}/documentos` | Listar documentos asociados a la fórmula. | `DocumentoFormulaResponseDTO` | Jefe Producción, Jefe Calidad, Super Admin |
| `POST /api/bom/formulas/{formulaId}/documentos` | Subir documento (multipart). | `DocumentoFormulaResponseDTO` | Jefe Calidad, Super Admin |
| `GET /api/bom/formulas/documentos/{documentoId}/descargar` | Descargar documento autorizado. | `DocumentoFormulaDescargaDTO` | Jefe Producción, Jefe Calidad, Super Admin |
| `DELETE /api/bom/formulas/documentos/{documentoId}` | Eliminar documento de una fórmula. | — | Jefe Calidad, Super Admin |

## Seguridad y roles
- **Lectura:** `ROL_JEFE_PRODUCCION`, `ROL_JEFE_CALIDAD`, `ROL_SUPER_ADMIN`.
- **Edición:** `ROL_JEFE_CALIDAD`, `ROL_SUPER_ADMIN`.
Las restricciones se aplican con `@PreAuthorize` en los controladores y cuentan con cobertura en `FormulaProductoControllerSecurityTest`.

## Almacenamiento de archivos
- Directorio raíz: `uploads/bom/formulas` dentro del directorio de la aplicación.
- Cada fórmula almacena sus documentos en una subcarpeta con su `formulaId`.
- La descarga pasa por el endpoint protegido; no se exponen rutas directas al sistema de archivos.

## Pruebas relevantes
- `FormulaProductoServiceImplTest`: reglas de negocio del servicio de fórmulas.
- `DetalleFormulaServiceImplTest`: validaciones de detalles e insumos requeridos.
- `DocumentoFormulaServiceImplTest`: carga, descarga y borrado de documentos.
- `FormulaProductoControllerSecurityTest`: matriz de roles y anotaciones de seguridad.
