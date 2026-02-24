package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UbicacionFisica;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventarioGeneralCorteReportService {

    private static final DateTimeFormatter FECHA_VENCE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final MovimientoSignosResolver movimientoSignosResolver;

    @Transactional(readOnly = true)
    public Workbook generarExcelInventarioGeneralCorte(LocalDateTime hasta) {
        List<MovimientoInventario> movimientos = movimientoInventarioRepository
                .findAllByFechaIngresoLessThanEqual(hasta);

        Map<ClaveInventario, BigDecimal> acumulado = new LinkedHashMap<>();
        Map<ClaveInventario, MetadataFila> metadata = new LinkedHashMap<>();

        for (MovimientoInventario mov : movimientos) {
            Producto producto = mov.getProducto();
            LoteProducto lote = mov.getLote();
            if (producto == null || lote == null || producto.getId() == null || lote.getId() == null) {
                continue;
            }
            for (MovimientoSignosResolver.AporteInventario aporte : movimientoSignosResolver.resolverAportesPorAlmacen(mov)) {
                if (aporte.almacenId() == null) {
                    continue;
                }
                ClaveInventario clave = new ClaveInventario(
                        producto.getId().longValue(),
                        lote.getId(),
                        aporte.almacenId());
                acumulado.merge(clave, aporte.cantidadFirmada(), BigDecimal::add);
                metadata.putIfAbsent(clave, MetadataFila.from(producto, lote));
            }
        }

        List<FilaInventario> filas = new ArrayList<>();
        for (Map.Entry<ClaveInventario, BigDecimal> e : acumulado.entrySet()) {
            if (e.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            MetadataFila data = metadata.get(e.getKey());
            if (data == null) {
                continue;
            }
            filas.add(new FilaInventario(
                    data.sku(),
                    data.nombre(),
                    data.udm(),
                    e.getValue(),
                    data.lote(),
                    data.vence(),
                    data.ubicacion()
            ));
        }

        filas.sort(Comparator
                .comparing(FilaInventario::sku, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(FilaInventario::lote, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(FilaInventario::ubicacion, Comparator.nullsLast(String::compareToIgnoreCase)));

        return construirWorkbook(filas);
    }

    private Workbook construirWorkbook(List<FilaInventario> filas) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventario General");

        CreationHelper creationHelper = workbook.getCreationHelper();
        DataFormat dataFormat = creationHelper.createDataFormat();
        CellStyle numericStyle = workbook.createCellStyle();
        numericStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

        String[] columnas = {"sku", "nombre", "udm", "cant", "lote", "vence", "ubicación"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            header.createCell(i).setCellValue(columnas[i]);
        }

        int rowNum = 1;
        for (FilaInventario fila : filas) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(valorTexto(fila.sku()));
            row.createCell(1).setCellValue(valorTexto(fila.nombre()));
            row.createCell(2).setCellValue(valorTexto(fila.udm()));
            var cantCell = row.createCell(3);
            cantCell.setCellValue(fila.cant().doubleValue());
            cantCell.setCellStyle(numericStyle);
            row.createCell(4).setCellValue(valorTexto(fila.lote()));
            row.createCell(5).setCellValue(valorTexto(fila.vence()));
            row.createCell(6).setCellValue(valorTexto(fila.ubicacion()));
        }

        for (int i = 0; i < columnas.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbook;
    }

    private static String valorTexto(String value) {
        return value == null ? "" : value;
    }

    private record ClaveInventario(Long productoId, Long loteId, Long almacenId) {}

    private record FilaInventario(String sku, String nombre, String udm, BigDecimal cant, String lote, String vence,
                                  String ubicacion) {}

    private record MetadataFila(String sku, String nombre, String udm, String lote, String vence, String ubicacion) {
        static MetadataFila from(Producto producto, LoteProducto lote) {
            UnidadMedida um = producto.getUnidadMedida();
            Almacen almacen = lote.getAlmacen();
            UbicacionFisica ubicacionFisica = lote.getUbicacionFisica();

            String almacenNombre = almacen != null ? almacen.getNombre() : null;
            String ubicacion = almacenNombre;
            if (ubicacionFisica != null) {
                String codigo = ubicacionFisica.getCodigo();
                String descripcion = ubicacionFisica.getDescripcion();
                String detalle = codigo != null && !codigo.isBlank()
                        ? codigo
                        : (descripcion != null && !descripcion.isBlank() ? descripcion : null);
                if (detalle != null) {
                    ubicacion = (almacenNombre == null || almacenNombre.isBlank())
                            ? detalle
                            : almacenNombre + " / " + detalle;
                }
            }

            return new MetadataFila(
                    producto.getCodigoSku(),
                    producto.getNombre(),
                    um != null ? um.getNombre() : null,
                    lote.getCodigoLote(),
                    lote.getFechaVencimiento() != null ? lote.getFechaVencimiento().format(FECHA_VENCE) : null,
                    ubicacion
            );
        }
    }
}
