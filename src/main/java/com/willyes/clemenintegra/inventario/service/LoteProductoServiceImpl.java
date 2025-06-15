package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.*;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoteProductoServiceImpl implements LoteProductoService {

    private final LoteProductoRepository loteRepo;
    private final ProductoRepository productoRepo;
    private final AlmacenRepository almacenRepo;
    private final LoteProductoMapper loteProductoMapper;
    private final UsuarioService usuarioService;

    @Transactional
    public LoteProductoResponseDTO crearLote(LoteProductoRequestDTO dto) {
        Producto producto = productoRepo.findById(dto.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        Almacen almacen = almacenRepo.findById(dto.getAlmacenId())
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado"));

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        LoteProducto lote = loteProductoMapper.toEntity(dto, producto, almacen, usuario);
        lote = loteRepo.saveAndFlush(lote); // sin try-catch, lo maneja el ControllerAdvice

        return loteProductoMapper.toDto(lote);
    }

    public List<LoteProductoResponseDTO> obtenerLotesPorEstado(String estado) {
        EstadoLote estadoEnum = EstadoLote.valueOf(estado.toUpperCase());
        return loteRepo.findByEstado(estadoEnum).stream()
                .map(loteProductoMapper::toDto)
                .collect(Collectors.toList());
    }

    public Workbook generarReporteLotesPorVencerExcel() {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(30); // Puedes parametrizar esto si lo deseas

        List<LoteProducto> lotes = loteRepo.findByFechaVencimientoBetween(hoy, limite);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Lotes por Vencer");

        // Encabezados
        Row header = sheet.createRow(0);
        String[] columnas = {
                "ID Lote", "Código Lote", "Producto", "Fecha Vencimiento", "Stock Lote", "Estado", "Almacén"
        };
        for (int i = 0; i < columnas.length; i++) {
            header.createCell(i).setCellValue(columnas[i]);
        }

        // Contenido
        int rowNum = 1;
        for (LoteProducto lote : lotes) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(lote.getId());
            row.createCell(1).setCellValue(lote.getCodigoLote());
            row.createCell(2).setCellValue(lote.getProducto().getNombre());
            row.createCell(3).setCellValue(lote.getFechaVencimiento() != null ? lote.getFechaVencimiento().toString() : "");
            row.createCell(4).setCellValue(lote.getStockLote() != null ? lote.getStockLote().doubleValue() : 0);
            row.createCell(5).setCellValue(lote.getEstado().name());
            row.createCell(6).setCellValue(lote.getAlmacen().getNombre());
        }

        for (int i = 0; i < columnas.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbook;
    }

    public ByteArrayOutputStream generarReporteAlertasActivasExcel() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Alertas Activas");
        int rowIdx = 0;

        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("Tipo Alerta");
        header.createCell(1).setCellValue("Código SKU / Lote");
        header.createCell(2).setCellValue("Nombre Producto");
        header.createCell(3).setCellValue("Estado / Stock");
        header.createCell(4).setCellValue("Fecha");

        // Productos con stock bajo
        List<Producto> productosConAlerta = productoRepo.findAll().stream()
                .filter(p -> p.getStockActual().compareTo(p.getStockMinimo()) < 0)
                .toList();

        for (Producto p : productosConAlerta) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue("Stock Bajo");
            row.createCell(1).setCellValue(p.getCodigoSku());
            row.createCell(2).setCellValue(p.getNombre());
            row.createCell(3).setCellValue(p.getStockActual().toPlainString());
            row.createCell(4).setCellValue(""); // Sin fecha
        }

        // Lotes con alerta (vencido, retenido, cuarentena)
        List<LoteProducto> lotesConAlerta = loteRepo.findAll().stream()
                .filter(l -> l.getEstado() == EstadoLote.RETENIDO
                        || l.getEstado() == EstadoLote.EN_CUARENTENA
                        || (l.getFechaVencimiento() != null && l.getFechaVencimiento().isBefore(LocalDate.now())))
                .toList();

        for (LoteProducto l : lotesConAlerta) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue("Lote - " + l.getEstado().name());
            row.createCell(1).setCellValue(l.getCodigoLote());
            row.createCell(2).setCellValue(l.getProducto().getNombre());
            row.createCell(3).setCellValue(l.getEstado().name());
            row.createCell(4).setCellValue(l.getFechaVencimiento() != null ? l.getFechaVencimiento().toString() : "");
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            workbook.close();
            return bos;
        } catch (IOException e) {
            throw new RuntimeException("Error generando reporte de alertas activas", e);
        }
    }
}

