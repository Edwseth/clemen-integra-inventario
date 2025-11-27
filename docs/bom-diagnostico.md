# Diagnóstico BOM (insumos y detalle de fórmula)

Este archivo resume los hallazgos solicitados sobre el manejo de insumos y el mapeo/persistencia de `DetalleFormula`.

- **Modelo**: `src/main/java/com/willyes/clemenintegra/bom/model/DetalleFormula.java` define relaciones `@ManyToOne` hacia `FormulaProducto`, `Producto (insumo)` y `UnidadMedida` sin restricciones `nullable` explícitas.
- **Mapper**: `BomMapper.toEntity(DetalleFormulaRequest, FormulaProducto, Producto, UnidadMedida)` no especifica un mapeo de `unidad` a `unidadMedida`, lo que explica registros con `unidad_medida_id` en `NULL`. El parámetro `insumo` sí coincide con el nombre del campo.
- **Creación de detalles**: `DetalleFormulaController` construye instancias mínimas (solo IDs) y delega al mapper + `DetalleFormulaServiceImpl.guardar`, que reemplaza la fórmula por la cargada y luego guarda. No hay resolución explícita de `insumo`/`unidad`, por lo que el mapper es crítico.
- **Catálogo de insumos**: `ProductoController.getProductosInsumo` expone `GET /api/productos/insumos` y filtra solo `MATERIA_PRIMA` y `MATERIAL_EMPAQUE` (sin `SUMINISTROS` ni `PRODUCTO_SEMI_ELABORADO`).

Estos puntos sustentan los dos problemas reportados: pérdida de `unidad_medida_id` en nuevos detalles y exclusión de PS en el catálogo de insumos.
