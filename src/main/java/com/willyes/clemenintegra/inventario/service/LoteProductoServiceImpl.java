package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static com.willyes.clemenintegra.inventario.service.spec.LoteProductoSpecifications.*;

@Service
@RequiredArgsConstructor
public class LoteProductoServiceImpl implements LoteProductoService {

    private final LoteProductoRepository loteRepo;
    private final ProductoRepository productoRepo;
    private final AlmacenRepository almacenRepo;
    private final LoteProductoMapper loteProductoMapper;
    private final UsuarioService usuarioService;
    private final LoteProductoRepository loteProductoRepository;
    private final EvaluacionCalidadRepository evaluacionRepository;

    @Transactional
    public LoteProductoResponseDTO crearLote(LoteProductoRequestDTO dto) {
        Producto producto = productoRepo.findById(dto.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        Almacen almacen = almacenRepo.findById(dto.getAlmacenId())
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado"));

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        LoteProducto lote = loteProductoMapper.toEntity(dto, producto, almacen, usuario);
        if (producto.getTipoAnalisisCalidad() == TipoAnalisisCalidad.NINGUNO) {
            lote.setEstado(EstadoLote.DISPONIBLE);
        } else {
            lote.setEstado(EstadoLote.EN_CUARENTENA);
        }

        lote = loteRepo.saveAndFlush(lote); // sin try-catch, lo maneja el ControllerAdvice

        return loteProductoMapper.toResponseDTO(lote);
    }

    public List<LoteProductoResponseDTO> obtenerLotesPorEstado(String estado) {
        EstadoLote estadoEnum = EstadoLote.valueOf(estado.toUpperCase());
        return loteRepo.findByEstado(estadoEnum).stream()
                .map(loteProductoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<LoteProductoResponseDTO> obtenerLotesPorEvaluar() {
        List<EstadoLote> estados = List.of(EstadoLote.EN_CUARENTENA, EstadoLote.RETENIDO);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        List<LoteProducto> lotes;
        if (auth != null) {
            boolean analista = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROL_ANALISTA_CALIDAD"::equals);
            boolean micro = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROL_MICROBIOLOGO"::equals);

            if (analista) {
                lotes = loteRepo.findByEstadoInAndProducto_TipoAnalisisIn(
                        estados,
                        List.of(TipoAnalisisCalidad.FISICO_QUIMICO, TipoAnalisisCalidad.AMBOS)
                );
            } else if (micro) {
                lotes = loteRepo.findByEstadoInAndProducto_TipoAnalisisIn(
                        estados,
                        List.of(TipoAnalisisCalidad.MICROBIOLOGICO, TipoAnalisisCalidad.AMBOS)
                );
            } else {
                lotes = loteRepo.findByEstadoIn(estados);
            }
        } else {
            lotes = loteRepo.findByEstadoIn(estados);
        }

        return lotes.stream().map(loteProductoMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public Page<LoteProductoResponseDTO> listarTodos(String producto, EstadoLote estado, String almacen,
                                                     Boolean vencidos, LocalDateTime fechaInicio,
                                                     LocalDateTime fechaFin, Pageable pageable) {
        Specification<LoteProducto> spec = Specification.where(productoNombreContains(producto))
                .and(equalsEstado(estado))
                .and(almacenNombreContains(almacen));

        if (Boolean.TRUE.equals(vencidos)) {
            spec = spec.and(fechaVencimientoAntesDe(LocalDateTime.now()));
        } else {
            LocalDateTime inicio = fechaInicio;
            LocalDateTime fin = fechaFin;
            if (inicio != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaVencimiento"), inicio));
            }
            if (fin != null) {
                spec = spec.and(fechaVencimientoAntesDe(fin));
            }
        }

        Page<LoteProducto> lotes = loteProductoRepository.findAll(spec, pageable);
        return lotes.map(loteProductoMapper::toResponseDTO);
    }

    public Workbook generarReporteLotesPorVencerExcel() {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(30); // Puedes parametrizar esto si lo deseas

        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = limite.atTime(23, 59, 59);

        List<LoteProducto> lotes = loteRepo.findByFechaVencimientoBetween(inicio, fin);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Lotes por Vencer");

        // Encabezados
        Row header = sheet.createRow(0);
        String[] columnas = {
                "ID Lote", "Código Lote", "Producto", "Fecha Vencimiento", "Stock Lote", "Estado", "Almacén", "Ubicación"
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
            String nombreProd = lote.getProducto() != null ? lote.getProducto().getNombre() : "";
            row.createCell(2).setCellValue(nombreProd);
            row.createCell(3).setCellValue(lote.getFechaVencimiento() != null ? lote.getFechaVencimiento().toString() : "");
            Cell cell = row.createCell(4);
            BigDecimal stock = (lote.getStockLote() != null) ? lote.getStockLote() : BigDecimal.ZERO;
            cell.setCellValue(stock.doubleValue());
            row.createCell(5).setCellValue(lote.getEstado().name());
            row.createCell(6).setCellValue(lote.getAlmacen().getNombre());
            String ubicacion = lote.getAlmacen() != null ? lote.getAlmacen().getUbicacion() : null;
            row.createCell(7).setCellValue(ubicacion != null ? ubicacion : "-");
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
                        || (l.getFechaVencimiento() != null && l.getFechaVencimiento().isBefore(LocalDateTime.now())))
                .toList();

        for (LoteProducto l : lotesConAlerta) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue("Lote - " + l.getEstado().name());
            row.createCell(1).setCellValue(l.getCodigoLote());
            String nombreProducto = l.getProducto() != null ? l.getProducto().getNombre() : "";
            row.createCell(2).setCellValue(nombreProducto);
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

    @Transactional
    @Override
    public LoteProductoResponseDTO liberarLote(Long id) {
        LoteProducto lote = loteRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Lote no encontrado"));
        validarEvaluacionesExistentes(id);

        if (lote.getEstado() == EstadoLote.EN_CUARENTENA) {
            lote.setEstado(EstadoLote.DISPONIBLE);
            lote.setFechaLiberacion(LocalDateTime.now());
            lote.setUsuarioLiberador(usuarioService.obtenerUsuarioAutenticado());
        } else if (lote.getEstado() == EstadoLote.RETENIDO) {
            lote.setEstado(EstadoLote.LIBERADO);
            lote.setUsuarioLiberador(usuarioService.obtenerUsuarioAutenticado());
        } else {
            throw new IllegalStateException("El lote no puede ser liberado desde su estado actual");
        }

        loteRepo.save(lote);
        return loteProductoMapper.toResponseDTO(lote);
    }

    @Transactional
    @Override
    public LoteProductoResponseDTO rechazarLote(Long id) {
        LoteProducto lote = loteRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Lote no encontrado"));
        validarEvaluacionesExistentes(id);
        lote.setEstado(EstadoLote.RECHAZADO);
        loteRepo.save(lote);
        return loteProductoMapper.toResponseDTO(lote);
    }

    @Transactional
    @Override
    public LoteProductoResponseDTO liberarLoteRetenido(Long id) {
        LoteProducto lote = loteRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Lote no encontrado"));
        validarEvaluacionesExistentes(id);
        if (lote.getEstado() != EstadoLote.RETENIDO) {
            throw new IllegalStateException("El lote no está en estado RETENIDO");
        }
        lote.setEstado(EstadoLote.LIBERADO);
        lote.setUsuarioLiberador(usuarioService.obtenerUsuarioAutenticado());
        loteRepo.save(lote);
        return loteProductoMapper.toResponseDTO(lote);
    }

    @Transactional
    @Override
    public LoteProductoResponseDTO liberarLotePorCalidad(Long loteId, Usuario usuarioActual) {
        if (usuarioActual == null || usuarioActual.getRol() != RolUsuario.ROL_JEFE_CALIDAD) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el Jefe de Calidad puede liberar lotes.");
        }

        LoteProducto lote = loteRepo.findById(loteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado"));

        if (lote.getEstado() != EstadoLote.EN_CUARENTENA && lote.getEstado() != EstadoLote.RETENIDO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este lote no puede ser liberado.");
        }

        List<EvaluacionCalidad> evaluaciones = evaluacionRepository.findByLoteProductoId(loteId);

        Producto producto = lote.getProducto();
        TipoAnalisisCalidad tipo = producto.getTipoAnalisisCalidad();

        if (tipo == TipoAnalisisCalidad.FISICO_QUIMICO || tipo == TipoAnalisisCalidad.AMBOS) {
            validarEvaluacion(evaluaciones, TipoEvaluacion.FISICO_QUIMICO);
        }
        if (tipo == TipoAnalisisCalidad.MICROBIOLOGICO || tipo == TipoAnalisisCalidad.AMBOS) {
            validarEvaluacion(evaluaciones, TipoEvaluacion.MICROBIOLOGICO);
        }

        lote.setEstado(EstadoLote.DISPONIBLE);
        lote.setFechaLiberacion(LocalDateTime.now());
        lote.setUsuarioLiberador(usuarioActual);
        loteRepo.save(lote);

        return loteProductoMapper.toResponseDTO(lote);
    }

    private void validarEvaluacion(List<EvaluacionCalidad> evaluaciones, TipoEvaluacion tipo) {
        List<EvaluacionCalidad> filtradas = evaluaciones.stream()
                .filter(e -> e.getTipoEvaluacion() == tipo)
                .toList();
        if (filtradas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta evaluación requerida");
        }
        boolean aprobadas = filtradas.stream()
                .allMatch(e -> e.getResultado() == ResultadoEvaluacion.CONFORME);
        if (!aprobadas) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La evaluación requerida no está aprobada");
        }
    }

    private void validarEvaluacionesExistentes(Long loteId) {
        if (evaluacionRepository.findByLoteProductoId(loteId).isEmpty()) {
            throw new IllegalStateException("El lote no cuenta con evaluaciones registradas");
        }
    }
}

