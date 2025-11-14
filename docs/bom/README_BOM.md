# Módulo BOM (Backend)

El módulo de fórmulas o **Bill of Materials (BOM)** gestiona las recetas de producción de los productos terminados. Mantiene la estructura técnica de insumos provenientes de Inventarios, articula el flujo de revisión y aprobación liderado por Calidad y expone a Producción la versión aprobada que consumen las órdenes de fabricación.

## Entidades
- **FormulaProducto**: asociada a un Producto. Administra versión, estado (`EstadoFormula`), indicador `activo`, bitácora de creación/actualización y colecciones de detalles y documentos vinculados.
- **DetalleFormula**: perteneciente a una `FormulaProducto`. Relaciona el producto insumo y su `UnidadMedida` con la `cantidadNecesaria`, así como el flag `obligatorio` para consumos indispensables.
- **DocumentoFormula**: documento técnico ligado a la fórmula. Registra `tipoDocumento`, `nombreVisible`, `nombreArchivo`, ubicación en disco, `fechaRegistro` y usuario responsable.
- **EstadoFormula**: enumeración con los valores `BORRADOR`, `EN_REVISION`, `APROBADA`, `RECHAZADA` que determinan el ciclo de vida.

## Ciclo de vida
- Transiciones permitidas:
  - `BORRADOR → EN_REVISION`
  - `EN_REVISION → APROBADA`
  - `EN_REVISION → RECHAZADA`
- Edición de detalles y documentos habilitada únicamente mientras la fórmula se encuentra en `BORRADOR` o `EN_REVISION`.
- Al aprobar una fórmula:
  - se marca con `estado = APROBADA` y `activo = true`;
  - todas las demás fórmulas del mismo producto se desactivan (`activo = false`).
- Producción consume exclusivamente fórmulas con estado `APROBADA` y `activo = true`.

## API BOM

### Fórmulas
- `GET /api/bom/formulas`
  - Lista el maestro de fórmulas con filtros opcionales por estado (`EstadoFormula`) y producto.
  - Respuesta: lista de `FormulaProductoResumenDTO`.
  - Roles: ROL_JEFE_PRODUCCION, ROL_JEFE_CALIDAD, ROL_SUPER_ADMIN.
- `POST /api/bom/formulas/{id}/clonar`
  - Genera una nueva versión copiando una fórmula existente respetando las reglas de edición.
  - Respuesta: `FormulaProductoResumenDTO` de la nueva versión.
  - Roles: ROL_JEFE_CALIDAD, ROL_SUPER_ADMIN.
- `POST /api/bom/formulas/{id}/cambiar-estado`
  - Aplica las transiciones del ciclo de vida y valida activaciones exclusivas.
  - Respuesta: `FormulaProductoResumenDTO` actualizado.
  - Roles: ROL_JEFE_CALIDAD, ROL_SUPER_ADMIN.

### Fórmula activa para Producción
- `GET /api/bom/formulas/producto/{productoId}/formula-activa`
  - Obtiene la fórmula aprobada y activa para el producto solicitado.
  - Respuesta: `FormulaActivaProduccionDTO` con los `DetalleFormulaProduccionDTO` requeridos.
  - Roles: ROL_JEFE_PRODUCCION, ROL_JEFE_CALIDAD, ROL_SUPER_ADMIN.

### Documentos
- `GET /api/bom/formulas/{formulaId}/documentos`
  - Lista los documentos asociados a la fórmula.
  - Respuesta: colección de `DocumentoFormulaResponseDTO`.
  - Roles: ROL_JEFE_PRODUCCION, ROL_JEFE_CALIDAD, ROL_SUPER_ADMIN.
- `POST /api/bom/formulas/{formulaId}/documentos`
  - Carga un nuevo documento (multipart) cuando la fórmula está editable.
  - Respuesta: `DocumentoFormulaResponseDTO` del archivo almacenado.
  - Roles: ROL_JEFE_CALIDAD, ROL_SUPER_ADMIN.
- `GET /api/bom/formulas/documentos/{documentoId}/descargar`
  - Permite la descarga segura del documento.
  - Respuesta: `DocumentoFormulaDescargaDTO`.
  - Roles: ROL_JEFE_PRODUCCION, ROL_JEFE_CALIDAD, ROL_SUPER_ADMIN.
- `DELETE /api/bom/formulas/documentos/{documentoId}`
  - Elimina un documento autorizado mientras la fórmula sigue editable.
  - Roles: ROL_JEFE_CALIDAD, ROL_SUPER_ADMIN.

## Seguridad y roles
- **Lectura:** `ROL_JEFE_PRODUCCION`, `ROL_JEFE_CALIDAD`, `ROL_SUPER_ADMIN`.
- **Edición:** `ROL_JEFE_CALIDAD`, `ROL_SUPER_ADMIN`.
Las restricciones se aplican vía anotaciones `@PreAuthorize` en los controladores BOM y se validan mediante las pruebas de seguridad (`FormulaProductoControllerSecurityTest`).

## Almacenamiento de documentos
- Estructura física: `uploads/bom/formulas/{formulaId}` dentro del servidor de archivos de la aplicación.
- Cada archivo conserva su `nombreArchivo` sanitizado y metadatos asociados antes de persistir en disco.
- Los documentos recopilan especificaciones técnicas, instructivos y registros de calidad vinculados a la fórmula.

## Pruebas
- `FormulaProductoServiceImplTest`: flujo de versiones, activación exclusiva y transiciones de estado.
- `DetalleFormulaServiceImplTest`: validaciones de insumos y restricciones de edición según estado.
- `DocumentoFormulaServiceImplTest`: carga, listado y eliminación de documentos con control de estados.
- `FormulaProductoControllerSecurityTest`: verificación de roles permitidos (200/403/401) para cada endpoint BOM.

## Integración con otros módulos
- **Inventarios:** provee los productos insumo y sus unidades para `DetalleFormula`.
- **Producción:** consume la fórmula `APROBADA` y `activa` en la preparación de órdenes.
- **Calidad:** lidera la revisión, aprobación/rechazo y la documentación regulatoria asociada a cada fórmula.
