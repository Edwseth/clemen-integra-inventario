package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.inventario.service.StockQueryService;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final StockQueryService stockQueryService;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private final InventoryCatalogResolver catalogResolver;

    @Value("${inventory.lote.estadoLiberado}")
    private String estadoLiberadoConf;

    @Value("${inventory.mov.clasificacion.liberacionCalidad}")
    private String clasificacionLiberacionConf;

    @Value("${inventory.mov.clasificacion.rechazoCalidad}")
    private String clasificacionRechazoCalidad;

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

        return lotes.stream()
                .map(lote -> {
                    List<TipoEvaluacion> evaluaciones = evaluacionRepository.findByLoteProductoId(lote.getId())
                            .stream()
                            .map(EvaluacionCalidad::getTipoEvaluacion)
                            .collect(Collectors.toList());

                    if (tieneEvaluacionesRequeridas(lote.getProducto().getTipoAnalisis(), evaluaciones)) {
                        return null;
                    }

                    LoteProductoResponseDTO dto = loteProductoMapper.toDto(lote);
                    dto.setEvaluaciones(evaluaciones);
                    return dto;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean tieneEvaluacionesRequeridas(TipoAnalisisCalidad requerido, List<TipoEvaluacion> evaluaciones) {
        return switch (requerido) {
            case FISICO_QUIMICO -> evaluaciones.contains(TipoEvaluacion.FISICO_QUIMICO);
            case MICROBIOLOGICO -> evaluaciones.contains(TipoEvaluacion.MICROBIOLOGICO);
            case AMBOS -> evaluaciones.contains(TipoEvaluacion.FISICO_QUIMICO)
                    && evaluaciones.contains(TipoEvaluacion.MICROBIOLOGICO);
            default -> false;
        };
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
        List<Producto> todos = productoRepo.findAll();
        Map<Long, BigDecimal> stockMap = stockQueryService.obtenerStockDisponible(
                todos.stream().map(p -> p.getId().longValue()).toList());
        List<Producto> productosConAlerta = todos.stream()
                .filter(p -> stockMap.getOrDefault(p.getId().longValue(), BigDecimal.ZERO)
                        .compareTo(p.getStockMinimo()) < 0)
                .toList();

        for (Producto p : productosConAlerta) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue("Stock Bajo");
            row.createCell(1).setCellValue(p.getCodigoSku());
            row.createCell(2).setCellValue(p.getNombre());
            BigDecimal stock = stockMap.getOrDefault(p.getId().longValue(), BigDecimal.ZERO);
            row.createCell(3).setCellValue(stock.toPlainString());
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
        Usuario usuarioActual = usuarioService.obtenerUsuarioAutenticado();

        ClasificacionMovimientoInventario clasificacion;
        try {
            clasificacion = ClasificacionMovimientoInventario.valueOf(clasificacionRechazoCalidad);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CLASIFICACION_RECHAZO_INVALIDA");
        }

        Long motivoId = catalogResolver.getMotivoIdAjusteRechazo();
        MotivoMovimiento motivo = motivoMovimientoRepository.findById(motivoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MOTIVO_AJUSTE_RECHAZO_INEXISTENTE"));
        Long tipoDetalleTransferenciaId = catalogResolver.getTipoDetalleTransferenciaId();
        TipoMovimientoDetalle tipoDetalle = tipoMovimientoDetalleRepository.findById(tipoDetalleTransferenciaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TIPO_DETALLE_TRANSFERENCIA_INEXISTENTE"));

        LoteProducto lote = loteProductoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado"));
        validarEvaluacionesExistentes(id);

        Long almacenObsoletosId = catalogResolver.getAlmacenObsoletosId();
        Long almacenCuarentenaId = catalogResolver.getAlmacenCuarentenaId();
        if (lote.getAlmacen().getId().equals(almacenObsoletosId) && lote.getEstado() == EstadoLote.RECHAZADO) {
            boolean movExistente = movimientoInventarioRepository
                    .existsByTipoMovimientoAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoIdAndClasificacion(
                            TipoMovimiento.TRANSFERENCIA, lote.getId(), almacenCuarentenaId, almacenObsoletosId, clasificacion);
            if (movExistente) {
                return loteProductoMapper.toResponseDTO(lote);
            }
        }

        if (!lote.getAlmacen().getId().equals(almacenCuarentenaId)
                || (lote.getEstado() != EstadoLote.EN_CUARENTENA && lote.getEstado() != EstadoLote.RETENIDO)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_EN_ALMACEN_INVALIDO_PARA_RECHAZO");
        }
        if (lote.getStockReservado() != null && lote.getStockReservado().compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_CON_RESERVAS");
        }
        if (lote.getStockLote() == null || lote.getStockLote().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_SIN_STOCK");
        }

        Almacen origen = lote.getAlmacen();
        Almacen destino = almacenRepo.findById(almacenObsoletosId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_OBSOLETOS_INEXISTENTE"));
        BigDecimal cantidad = lote.getStockLote();
        Producto producto = lote.getProducto();

        lote.setEstado(EstadoLote.RECHAZADO);
        lote.setAlmacen(destino);
        loteRepo.save(lote);

        MovimientoInventario mov = MovimientoInventario.builder()
                .cantidad(cantidad)
                .tipoMovimiento(TipoMovimiento.TRANSFERENCIA)
                .clasificacion(clasificacion)
                .registradoPor(usuarioActual)
                .producto(producto)
                .lote(lote)
                .almacenOrigen(origen)
                .almacenDestino(destino)
                .motivoMovimiento(motivo)
                .tipoMovimientoDetalle(tipoDetalle)
                .ordenProduccion(lote.getOrdenProduccion())
                .build();
        movimientoInventarioRepository.save(mov);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoteProductoResponseDTO liberarLotePorCalidad(Long loteId, Usuario usuarioActual) {
        if (usuarioActual == null || usuarioActual.getRol() != RolUsuario.ROL_JEFE_CALIDAD) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el Jefe de Calidad puede liberar lotes.");
        }

        EstadoLote estadoLiberado;
        ClasificacionMovimientoInventario clasificacion;
        try {
            estadoLiberado = EstadoLote.valueOf(estadoLiberadoConf);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ESTADO_LIBERADO_INVALIDO");
        }
        try {
            clasificacion = ClasificacionMovimientoInventario.valueOf(clasificacionLiberacionConf);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CLASIFICACION_LIBERACION_INVALIDA");
        }

        Long motivoId = catalogResolver.getMotivoIdTransferenciaCalidad();
        MotivoMovimiento motivo = motivoMovimientoRepository.findById(motivoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MOTIVO_TRANSFERENCIA_INEXISTENTE"));
        Long tipoDetalleTransferenciaId = catalogResolver.getTipoDetalleTransferenciaId();
        TipoMovimientoDetalle tipoDetalle = tipoMovimientoDetalleRepository.findById(tipoDetalleTransferenciaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TIPO_DETALLE_TRANSFERENCIA_INEXISTENTE"));

        LoteProducto lote = loteProductoRepository.findByIdForUpdate(loteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado"));

        Long almacenPtId = catalogResolver.getAlmacenPtId();
        Long almacenCuarentenaId = catalogResolver.getAlmacenCuarentenaId();
        if (lote.getAlmacen().getId().equals(almacenPtId)
                && lote.getEstado() == estadoLiberado
                && lote.getFechaLiberacion() != null
                && lote.getUsuarioLiberador() != null) {
            boolean movExistente = movimientoInventarioRepository
                    .existsByTipoMovimientoAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoIdAndClasificacion(
                            TipoMovimiento.TRANSFERENCIA, lote.getId(), almacenCuarentenaId, almacenPtId, clasificacion);
            if (movExistente) {
                return loteProductoMapper.toResponseDTO(lote);
            }
        }

        if (!lote.getAlmacen().getId().equals(almacenCuarentenaId) || lote.getEstado() != EstadoLote.EN_CUARENTENA) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_EN_CUARENTENA");
        }
        if (lote.getStockReservado() != null && lote.getStockReservado().compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_CON_RESERVAS");
        }
        if (lote.getFechaVencimiento() != null && lote.getFechaVencimiento().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_VENCIDO");
        }
        if (lote.getStockLote() == null || lote.getStockLote().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_SIN_STOCK");
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

        Almacen origen = lote.getAlmacen();
        Almacen destino = almacenRepo.findById(almacenPtId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_PT_INEXISTENTE"));
        BigDecimal cantidad = lote.getStockLote();

        lote.setEstado(estadoLiberado);
        lote.setFechaLiberacion(LocalDateTime.now());
        lote.setUsuarioLiberador(usuarioActual);
        lote.setAlmacen(destino);
        loteRepo.save(lote);

        MovimientoInventario mov = MovimientoInventario.builder()
                .cantidad(cantidad)
                .tipoMovimiento(TipoMovimiento.TRANSFERENCIA)
                .clasificacion(clasificacion)
                .registradoPor(usuarioActual)
                .producto(producto)
                .lote(lote)
                .almacenOrigen(origen)
                .almacenDestino(destino)
                .motivoMovimiento(motivo)
                .tipoMovimientoDetalle(tipoDetalle)
                .ordenProduccion(lote.getOrdenProduccion())
                .build();
        movimientoInventarioRepository.save(mov);

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

