package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.calidad.dto.CondicionUsoResponseDTO;
import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisMicroDTO;
import com.willyes.clemenintegra.calidad.mapper.CondicionUsoMapper;
import com.willyes.clemenintegra.calidad.model.CondicionUso;
import com.willyes.clemenintegra.calidad.model.enums.*;
import com.willyes.clemenintegra.calidad.repository.CondicionUsoRepository;
import com.willyes.clemenintegra.inventario.dto.BitacoraCambiosInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EstadoCalidadLoteResponseDTO;
import com.willyes.clemenintegra.calidad.dto.ReaperturaLoteRequestDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.service.RetencionLoteService;
import com.willyes.clemenintegra.calidad.service.NoConformidadService;
import com.willyes.clemenintegra.calidad.service.CondicionUsoService;
import com.willyes.clemenintegra.calidad.service.PlantillaAnalisisMicroService;

import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
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
import java.util.Collections;
import java.util.EnumSet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.requiereFisico;
import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.requiereMicro;
import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.requiereQuimico;

@Service
@RequiredArgsConstructor
public class LoteProductoServiceImpl implements LoteProductoService {

    private static final Logger log = LoggerFactory.getLogger(LoteProductoServiceImpl.class);

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
    private final RetencionLoteService retencionLoteService;
    private final NoConformidadService noConformidadService;
    private final CondicionUsoService condicionUsoService;
    private final PlantillaAnalisisMicroService plantillaAnalisisMicroService;
    private final CondicionUsoRepository condicionUsoRepository;
    private final CondicionUsoMapper mapper;
    private final BitacoraCambiosInventarioService bitacoraCambiosInventarioService;

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
        boolean requiereAnalisis = requiereFisico(producto) || requiereQuimico(producto) || requiereMicro(producto);
        if (!requiereAnalisis) {
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
            java.util.Set<String> authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
            boolean jefe = authorities.contains("ROL_JEFE_CALIDAD");
            boolean superAdmin = authorities.contains("ROL_SUPER_ADMIN");
            boolean analista = authorities.contains("ROL_ANALISTA_CALIDAD");
            boolean micro = authorities.contains("ROL_MICROBIOLOGO");

            if (jefe || superAdmin) {
                lotes = loteRepo.findByEstadoIn(estados);
            } else if (analista) {
                lotes = loteRepo.findByEstadoInAndProducto_TipoAnalisisIn(
                        estados,
                        List.of(TipoAnalisisCalidad.FISICO, TipoAnalisisCalidad.AMBOS)
                );
            } else if (micro) {
                lotes = loteRepo.findByEstadoInAndProducto_TipoAnalisisIn(
                        estados,
                        List.of(TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO, TipoAnalisisCalidad.AMBOS)
                );
            } else {
                lotes = loteRepo.findByEstadoIn(estados);
            }
        } else {
            lotes = loteRepo.findByEstadoIn(estados);
        }

        return lotes.stream()
                .map(lote -> {
                    List<EvaluacionCalidad> evaluaciones = evaluacionRepository.findByLoteProductoId(lote.getId());

                    if (tieneEvaluacionesRequeridas(lote.getProducto(), evaluaciones)) {
                        return null;
                    }

                    LoteProductoResponseDTO dto = loteProductoMapper.toDto(lote);
                    dto.setEvaluaciones(evaluaciones.stream()
                            .map(EvaluacionCalidad::getTipoEvaluacion)
                            .toList());
                    PlantillaAnalisisMicroDTO plantillaDto = plantillaAnalisisMicroService.obtenerPorProducto(lote.getProducto().getId().longValue());
                    if (plantillaDto != null) {
                        dto.setRequiereAnalisisMicro(plantillaDto.isRequiereAnalisisMicro());
                        if (plantillaDto.getId() != null) {
                            dto.setPlantillaMicroId(plantillaDto.getId());
                        }
                    }
                    return dto;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CondicionUsoResponseDTO> listarCondicionesUso(Long loteId, EstadoCondicionUso estado) {
        List<CondicionUso> entidades = (estado == null)
                ? condicionUsoRepository.findByLote_Id(loteId)
                : condicionUsoRepository.findByLote_IdAndEstado(loteId, estado);

        if (entidades == null || entidades.isEmpty()) {
            return Collections.emptyList();
        }
        return entidades.stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public LoteProductoResponseDTO reabrirParaReevaluacion(Long loteId,
                                                           ReaperturaLoteRequestDTO dto,
                                                           Usuario usuarioActual) {
        if (usuarioActual == null || usuarioActual.getId() == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Se requiere un usuario autenticado para reabrir el lote.");
        }

        String motivo = dto != null ? dto.getMotivo() : null;
        if (motivo == null || motivo.isBlank()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Debe indicar el motivo de la reapertura.");
        }

        LoteProducto lote = loteProductoRepository.findByIdForUpdate(loteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LOTE_NO_ENCONTRADO"));

        EstadoLote estadoActual = lote.getEstado();
        if (estadoActual == null) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "El lote carece de estado asignado.",
                    Map.of("loteId", loteId));
        }

        if (EnumSet.of(EstadoLote.EN_CUARENTENA, EstadoLote.RETENIDO).contains(estadoActual)) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "El lote ya se encuentra en evaluación de calidad.",
                    Map.of("loteId", loteId, "estadoActual", estadoActual.name()));
        }

        if (!EnumSet.of(EstadoLote.LIBERADO, EstadoLote.RECHAZADO, EstadoLote.DISPONIBLE).contains(estadoActual)) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "El lote no puede reabrirse desde su estado actual.",
                    Map.of("loteId", loteId, "estadoActual", estadoActual.name()));
        }

        Long cuarentenaId = catalogResolver.getAlmacenCuarentenaId();
        if (cuarentenaId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_CUARENTENA_NO_CONFIGURADO");
        }

        Almacen almacenPrevio = lote.getAlmacen();
        Almacen almacenCuarentena = almacenRepo.findById(cuarentenaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_CUARENTENA_INEXISTENTE"));

        lote.setEstado(EstadoLote.RETENIDO);
        lote.setAlmacen(almacenCuarentena);
        lote = loteRepo.save(lote);

        registrarBitacoraReapertura(lote,
                estadoActual,
                almacenPrevio,
                motivo.trim(),
                dto != null ? dto.getComentarios() : null,
                usuarioActual);

        log.info("[CALIDAD] lote reabierto para reevaluación. loteId={} estadoAnterior={} almacenPrevio={} usuario={}",
                lote.getId(), estadoActual, almacenPrevio != null ? almacenPrevio.getId() : null,
                usuarioActual.getId());

        return loteProductoMapper.toResponseDTO(lote);
    }

    private boolean tieneEvaluacionesRequeridas(Producto producto, List<EvaluacionCalidad> evaluaciones) {
        if (producto == null) {
            return false;
        }
        List<EvaluacionCalidad> seguras = evaluaciones == null ? List.of() : evaluaciones;

        boolean fisicoOk = seguras.stream().anyMatch(e ->
                e.getTipoEvaluacion() == TipoEvaluacion.FISICO &&
                        (e.getResultado() == ResultadoEvaluacion.CONFORME
                                || e.getResultado() == ResultadoEvaluacion.CONDICIONADO));

        boolean microOk = seguras.stream().anyMatch(e ->
                e.getTipoEvaluacion() == TipoEvaluacion.QUIMICO_MICROBIOLOGICO &&
                        (e.getResultado() == ResultadoEvaluacion.CONFORME
                                || e.getResultado() == ResultadoEvaluacion.CONDICIONADO));

        boolean requiereFisico = requiereFisico(producto);
        boolean requiereMicro = requiereMicro(producto) || requiereQuimico(producto);

        return (!requiereFisico || fisicoOk) && (!requiereMicro || microOk);
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

    @Transactional(readOnly = true)
    public Workbook generarReporteLotesPorVencerExcel(LocalDateTime inicio, LocalDateTime fin) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioEf = inicio != null ? inicio : hoy.atStartOfDay();
        LocalDateTime finEf = fin != null ? fin : hoy.plusDays(30).atTime(java.time.LocalTime.MAX);

        List<LoteProducto> lotes = loteRepo.findByFechaVencimientoBetweenFetchProducto(inicioEf, finEf);

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
        validarNoConformidadesParaLiberacion(lote.getId());

        if (lote.getEstado() != EstadoLote.EN_CUARENTENA
                && lote.getEstado() != EstadoLote.RETENIDO) {
            throw new IllegalStateException("El lote no puede ser liberado desde su estado actual");
        }

        lote.setEstado(EstadoLote.LIBERADO);
        lote.setFechaLiberacion(LocalDateTime.now());
        lote.setUsuarioLiberador(usuarioService.obtenerUsuarioAutenticado());

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
        validarNoConformidadesParaLiberacion(lote.getId());
        if (lote.getEstado() != EstadoLote.RETENIDO) {
            throw new CustomBusinessException(ApiErrorCode.BLOQUEO_ESTADO_CUARENTENA,
                    "El lote debe estar en estado RETENIDO para liberarlo.",
                    Map.of("loteId", id, "estadoActual", lote.getEstado() != null ? lote.getEstado().name() : null));
        }
        lote.setEstado(EstadoLote.LIBERADO);
        lote.setFechaLiberacion(LocalDateTime.now());
        lote.setUsuarioLiberador(usuarioService.obtenerUsuarioAutenticado());
        loteRepo.save(lote);
        return loteProductoMapper.toResponseDTO(lote);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoteProductoResponseDTO liberarLotePorCalidad(Long loteId, Usuario usuarioActual) {
        if (usuarioActual == null
                || (usuarioActual.getRol() != RolUsuario.ROL_JEFE_CALIDAD
                && usuarioActual.getRol() != RolUsuario.ROL_SUPER_ADMIN)) {
            throw new CustomBusinessException(ApiErrorCode.ROL_INSUFICIENTE,
                    "Solo el Jefe de Calidad o Super Admin pueden liberar lotes.");
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

        Producto producto = lote.getProducto();
        Long destinoPrincipalId = catalogResolver.resolveAlmacenPrincipal(producto);
        Long almacenCuarentenaId = catalogResolver.getAlmacenCuarentenaId();
        if (destinoPrincipalId.equals(lote.getAlmacen().getId().longValue())
                && lote.getEstado() == estadoLiberado
                && lote.getFechaLiberacion() != null
                && lote.getUsuarioLiberador() != null) {
            boolean movExistente = movimientoInventarioRepository
                    .existsByTipoMovimientoAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoIdAndClasificacion(
                            TipoMovimiento.TRANSFERENCIA, lote.getId(), almacenCuarentenaId, destinoPrincipalId, clasificacion);
            if (movExistente) {
                return loteProductoMapper.toResponseDTO(lote);
            }
        }

        if (!almacenCuarentenaId.equals(lote.getAlmacen().getId().longValue()) || lote.getEstado() != EstadoLote.EN_CUARENTENA) {
            Long almacenId = Long.valueOf(lote.getAlmacen().getId());
            EstadoLote estadoLote = lote.getEstado();
            log.info("[INVENTARIO] intento de liberar lote fuera de cuarentena. loteId={} almacenId={} estado={}",
                    loteId, almacenId, estadoLote);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "LOTE_NO_EN_CUARENTENA (almacenId=" + almacenId + ", estado=" + estadoLote + ")");
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

        boolean requiereFisico = requiereFisico(producto);
        boolean requiereQuimico = requiereQuimico(producto);
        boolean requiereMicro = requiereMicro(producto);

        if (requiereFisico) {
            validarEvaluacion(evaluaciones, TipoEvaluacion.FISICO);
        }
        if (requiereQuimico || requiereMicro) {
            validarEvaluacion(evaluaciones, TipoEvaluacion.QUIMICO_MICROBIOLOGICO);
        }

        validarNoConformidadesParaLiberacion(loteId);

        Almacen origen = lote.getAlmacen();
        Almacen destino = almacenRepo.findById(destinoPrincipalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_DESTINO_INEXISTENTE"));
        BigDecimal cantidad = lote.getStockLote();

        lote.setEstado(estadoLiberado);
        lote.setFechaLiberacion(LocalDateTime.now());
        lote.setUsuarioLiberador(usuarioActual);
        lote.setAlmacen(destino);
        loteRepo.save(lote);

        log.info("[INVENTARIO] liberación de lote completada destino={} tipoProducto={} loteId={}",
                destinoPrincipalId,
                producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getTipo() : null,
                loteId);

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

    private void registrarBitacoraReapertura(LoteProducto lote,
                                             EstadoLote estadoAnterior,
                                             Almacen almacenAnterior,
                                             String motivo,
                                             String comentarios,
                                             Usuario usuarioActual) {
        if (lote == null || usuarioActual == null || usuarioActual.getId() == null) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        bitacoraCambiosInventarioService.crear(BitacoraCambiosInventarioDTO.builder()
                .tablaAfectada("lotes_productos")
                .registroId(lote.getId())
                .campoModificado("estado")
                .valorAnt(estadoAnterior != null ? estadoAnterior.name() : "N/A")
                .valorNuevo(EstadoLote.RETENIDO.name())
                .fechaCambio(ahora)
                .usuarioId(usuarioActual.getId())
                .build());

        bitacoraCambiosInventarioService.crear(BitacoraCambiosInventarioDTO.builder()
                .tablaAfectada("lotes_productos")
                .registroId(lote.getId())
                .campoModificado("motivo_reapertura")
                .valorAnt("N/A")
                .valorNuevo(construirDetalleReapertura(motivo, comentarios, almacenAnterior))
                .fechaCambio(ahora)
                .usuarioId(usuarioActual.getId())
                .build());
    }

    private String construirDetalleReapertura(String motivo,
                                              String comentarios,
                                              Almacen almacenAnterior) {
        StringBuilder detalle = new StringBuilder("motivo=").append(motivo);
        if (comentarios != null && !comentarios.isBlank()) {
            detalle.append(" | comentarios=").append(comentarios.trim());
        }
        if (almacenAnterior != null && almacenAnterior.getNombre() != null) {
            detalle.append(" | almacenAnterior=").append(almacenAnterior.getNombre());
        }
        if (detalle.length() > 255) {
            return detalle.substring(0, 255);
        }
        return detalle.toString();
    }

    private void validarEvaluacion(List<EvaluacionCalidad> evaluaciones, TipoEvaluacion tipo) {
        List<EvaluacionCalidad> filtradas = evaluaciones.stream()
                .filter(e -> e.getTipoEvaluacion() == tipo)
                .toList();
        if (filtradas.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.EVALUACIONES_FALTANTES,
                    "Falta registrar la evaluación requerida antes de liberar el lote.",
                    Map.of("tipoEvaluacion", tipo.name()));
        }
        boolean existeNoConforme = filtradas.stream()
                .anyMatch(e -> e.getResultado() == ResultadoEvaluacion.NO_CONFORME);
        if (existeNoConforme) {
            throw new CustomBusinessException(ApiErrorCode.EVALUACIONES_FALTANTES,
                    "Existe al menos una evaluación NO_CONFORME.",
                    Map.of("tipoEvaluacion", tipo.name()));
        }
        boolean existeOk = filtradas.stream()
                .anyMatch(e -> e.getResultado() == ResultadoEvaluacion.CONFORME
                        || e.getResultado() == ResultadoEvaluacion.CONDICIONADO);
        if (!existeOk) {
            throw new CustomBusinessException(ApiErrorCode.EVALUACIONES_FALTANTES,
                    "Se requiere evaluación CONFORME o CONDICIONADO para liberar el lote.",
                    Map.of("tipoEvaluacion", tipo.name()));
        }

        boolean conSoportes = filtradas.stream()
                .anyMatch(e -> e.getArchivosAdjuntos() != null && !e.getArchivosAdjuntos().isEmpty());
        if (!conSoportes) {
            throw new CustomBusinessException(ApiErrorCode.EVALUACIONES_FALTANTES,
                    "Debe adjuntar al menos un soporte antes de liberar el lote.",
                    Map.of("tipoEvaluacion", tipo.name()));
        }
    }

    private void validarEvaluacionesExistentes(Long loteId) {
        if (evaluacionRepository.findByLoteProductoId(loteId).isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.EVALUACIONES_FALTANTES,
                    "El lote no cuenta con evaluaciones registradas.",
                    Map.of("loteId", loteId));
        }
    }

    private void validarNoConformidadesParaLiberacion(Long loteId) {
        if (loteId == null) {
            return;
        }
        var retenciones = retencionLoteService.obtenerRetencionesActivas(loteId);
        boolean retencionNcActiva = false;
        for (var retencion : retenciones) {
            if (retencion.getMotivo() != MotivoRetencion.NO_CONFORMIDAD) {
                continue;
            }
            retencionNcActiva = true;
            var noConformidad = retencion.getNoConformidad();
            if (noConformidad == null) {
                log.info("[INVENTARIO] liberación bloqueada por retención sin NC asociada. loteId={} retencionId={}",
                        loteId, retencion.getId());
                throw new CustomBusinessException(ApiErrorCode.BLOQUEO_RETENCION_NC,
                        "Existe una retención activa por no conformidad sin detalle asociado.",
                        Map.of("loteId", loteId, "retencionId", retencion.getId()));
            }
            if (noConformidad.getEstado() != EstadoNoConformidad.CERRADA) {
                log.info("[INVENTARIO] liberación bloqueada por NC abierta. loteId={} ncId={}", loteId, noConformidad.getId());
                throw new CustomBusinessException(ApiErrorCode.NC_ABIERTA,
                        "Debe cerrar la no conformidad " + noConformidad.getCodigo() + " antes de liberar el lote.",
                        Map.of("loteId", loteId, "ncId", noConformidad.getId()));
            }
        }
        noConformidadService.obtenerActivaPorLote(loteId).ifPresent(nc -> {
            log.info("[INVENTARIO] liberación bloqueada por NC abierta. loteId={} ncId={}", loteId, nc.getId());
            throw new CustomBusinessException(ApiErrorCode.NC_ABIERTA,
                    "Debe cerrar la no conformidad " + nc.getCodigo() + " antes de liberar el lote.",
                    Map.of("loteId", loteId, "ncId", nc.getId()));
        });
        if (retencionNcActiva) {
            log.debug("[INVENTARIO] retención por NC verificada con NC cerrada para lote {}", loteId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EstadoCalidadLoteResponseDTO obtenerEstadoCalidad(Long loteId) {
        LoteProducto lote = loteRepo.findById(loteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LOTE_NO_ENCONTRADO"));

        var retenciones = retencionLoteService.obtenerRetencionesActivas(loteId);
        var retencion = retenciones.stream()
                .filter(r -> r.getMotivo() == MotivoRetencion.NO_CONFORMIDAD)
                .findFirst()
                .orElse(null);
        var ncOpt = noConformidadService.obtenerActivaPorLote(loteId);
        var condiciones = condicionUsoService.getActivasByLote(loteId);

        EstadoCalidadLoteResponseDTO.NcResumen ncResumen = ncOpt.map(nc -> EstadoCalidadLoteResponseDTO.NcResumen.builder()
                .id(nc.getId())
                .severidad(nc.getSeveridad())
                .estado(nc.getEstado())
                .reportadoPorNombre(nc.getUsuarioReporta() != null ? nc.getUsuarioReporta().getNombreCompleto() : null)
                .build()).orElse(null);

        EstadoCalidadLoteResponseDTO.CondicionUsoResumen condicionResumen = condiciones.isEmpty() ? null
                : EstadoCalidadLoteResponseDTO.CondicionUsoResumen.builder()
                .id(condiciones.get(0).getId())
                .tipo(condiciones.get(0).getTipo())
                .parametroFecha(condiciones.get(0).getParametroFecha())
                .descripcion(condiciones.get(0).getDescripcion())
                .build();

        return EstadoCalidadLoteResponseDTO.builder()
                .loteId(loteId)
                .codigoLote(lote.getCodigoLote())
                .estadoLote(lote.getEstado() != null ? lote.getEstado().name() : null)
                .retencionActiva(retencion != null)
                .motivoRetencion(retencion != null ? retencion.getMotivo() : null)
                .nc(ncResumen)
                .condicionUsoActiva(!condiciones.isEmpty())
                .condicionUso(condicionResumen)
                .build();
    }
}

