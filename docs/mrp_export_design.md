# Diseño de exportación de sugerencias MRP

## Patrones existentes para reutilizar
- **Reportes Excel de inventario**: `ReporteInventarioController` publica endpoints que generan un `Workbook` desde servicios como `ReporteInventarioService`, `ProductoService` y `MovimientoInventarioService` y lo devuelven con `Content-Disposition` como `attachment` y tipo `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`. Utiliza `ByteArrayOutputStream` y cierra el `Workbook` tras escribirlo.
- **PDF de órdenes de compra**: `OrdenCompraController` expone `GET /api/ordenes-compra/{id}/pdf` que carga la entidad, delega en `OrdenCompraPdfService` (basado en OpenHTMLtoPDF con plantillas FreeMarker/HTML) y responde con encabezados `Content-Type: application/pdf` y `Content-Disposition` de descarga.

## MrpExportService propuesto
Ubicación sugerida: `com.willyes.clemenintegra.planeacion.service` con implementación `MrpExportServiceImpl` en el mismo paquete (o `impl`).

Interfaz recomendada:
```java
public interface MrpExportService {
    /**
     * Obtiene las sugerencias de una corrida MRP con filtros opcionales ya preparadas para exportar.
     */
    List<SugerenciaAbastecimientoExportDTO> obtenerSugerenciasExportables(
            Long corridaMrpId,
            String criticidad,
            String categoriaProducto);

    /** Genera el Excel (XLSX) listo para descargar. */
    byte[] exportarSugerenciasExcel(Long corridaMrpId, String criticidad, String categoriaProducto);

    /** Genera el PDF renderizado con OpenHTMLtoPDF. */
    byte[] exportarSugerenciasPdf(Long corridaMrpId, String criticidad, String categoriaProducto);
}
```

Responsabilidades clave de la implementación:
- Reutilizar un repositorio/query de `SugerenciaAbastecimiento` para traer los datos asociados a `CorridaMrp` y aplicar filtros (criticidad, categoría de insumo) de la tabla del frontend.
- Mapear a `SugerenciaAbastecimientoExportDTO` con datos completos (producto, UDM, requerimientos, inventario, recepciones programadas, fechas, lead time, tipo sugerencia, estado, criticidad, observaciones).
- Para Excel: apoyarse en utilidades de reportes existentes (p. ej. helper que crea `Workbook` con Apache POI similar a `ReporteInventarioServiceImpl`).
- Para PDF: usar una plantilla HTML (Thymeleaf/Freemarker) y renderizar con OpenHTMLtoPDF como en `OrdenCompraPdfService`.

## DTO de exportación sugerido
`com.willyes.clemenintegra.planeacion.dto.SugerenciaAbastecimientoExportDTO`

Campos recomendados:
- `Long idSugerencia`
- `Long corridaId`
- `String productoCodigo`
- `String productoNombre`
- `String categoria`
- `String unidadMedida`
- `String tipoSugerencia` (COMPRAR/FABRICAR/TRANSFERIR)
- `BigDecimal requerimientoBruto`
- `BigDecimal inventarioDisponible`
- `BigDecimal recepcionesProgramadas`
- `BigDecimal requerimientoNeto`
- `BigDecimal stockSeguridad`
- `String criticidad` (p. ej. ALTA/MEDIA/BAJA)
- `LocalDate fechaNecesidad`
- `LocalDate fechaSugeridaLanzamiento`
- `Integer leadTimeDias`
- `String estado`
- `String observaciones`

## Endpoints REST recomendados
Controlador en `com.willyes.clemenintegra.planeacion.controller`:

- `GET /api/mrp/corridas/{id}/sugerencias/export/excel`
  - `@PreAuthorize("hasAnyAuthority('ROL_PLANEADOR','ROL_JEFE_PLANEACION','ROL_SUPER_ADMIN')")`
  - Query params opcionales: `criticidad`, `categoriaProducto` para replicar los filtros de la tabla.
  - Responde `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` con `Content-Disposition: attachment; filename="mrp_sugerencias_{id}.xlsx"`.

- `GET /api/mrp/corridas/{id}/sugerencias/export/pdf`
  - `@PreAuthorize("hasAnyAuthority('ROL_PLANEADOR','ROL_JEFE_PLANEACION','ROL_SUPER_ADMIN')")`
  - Mismos query params de filtro que Excel.
  - Responde `application/pdf` con `Content-Disposition: inline; filename="mrp_sugerencias_{id}.pdf"` para visualización/imprimir en navegador.

El controlador delega en `MrpExportService`, manejando solo encabezados y parámetros. Puede seguir el patrón de `ReporteInventarioController` para Excel (escritura en `ByteArrayOutputStream`) y el patrón de `OrdenCompraController`/`OrdenCompraPdfService` para PDF.

## Reutilización de utilidades existentes
- **Excel**: reutilizar métodos de construcción de `Workbook`/estilos usados en `ReporteInventarioServiceImpl` y `MovimientoInventarioServiceImpl` para crear hojas, encabezados y formateo numérico, evitando lógica duplicada. Considerar extraer helpers compartidos a un utilitario en `inventario` o `planeacion` si aún no existen.
- **PDF**: seguir el enfoque de `OrdenCompraPdfService` con plantillas HTML en `resources/templates/mrp/sugerencias-mrp.ftl` (u otra) y `PdfRendererBuilder` para renderizar. Compartir funciones de formateo numérico/fecha y carga de plantillas.

## Manejo de filtros del frontend
- Aceptar `criticidad` y `categoriaProducto` como query params opcionales en los endpoints y pasarlos a `MrpExportService` para filtrar la consulta/DTO antes de exportar.
- Los mismos filtros deben aplicarse tanto a la generación del Excel como del PDF para garantizar que se exporte exactamente lo que el usuario ve en la tabla.
