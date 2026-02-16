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
import com.willyes.clemenintegra.inventario.dto.ProductoPorLoteDTO;
import com.willyes.clemenintegra.calidad.dto.EstadoCalidadLoteResponseDTO;
import com.willyes.clemenintegra.calidad.dto.ReaperturaLoteRequestDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
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
import org.springframework.util.StringUtils;

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
import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.validarDisciplinasCompletas;
import com.willyes.clemenintegra.calidad.repository.ResultadoAnalisisMicrobiologicoRepository;
import jakarta.persistence.EntityManager;

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
    private final ResultadoAnalisisMicrobiologicoRepository resultadoAnalisisMicrobiologicoRepository;
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
    @jakarta.persistence.PersistenceContext
    private EntityManager entityManager;

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
            // Regla de negocio: los lotes con análisis requerido deben ingresar a cuarentena.
            Long cuarentenaId = catalogResolver.getAlmacenCuarentenaId();
            if (cuarentenaId == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_CUARENTENA_NO_CONFIGURADO");
            }
            almacen = almacenRepo.findById(cuarentenaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_CUARENTENA_INEXISTENTE"));
            lote.setAlmacen(almacen);
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

    public org.springframework.data.domain.Page<LoteProductoResponseDTO> obtenerLotesPorEvaluar(org.springframework.data.domain.Pageable pageable) {
        List<EstadoLote> estados = List.of(EstadoLote.EN_CUARENTENA, EstadoLote.RETENIDO);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        java.util.Set<String> authorities = auth != null
                ? auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet())
                : java.util.Collections.emptySet();
        boolean jefe = authorities.contains("ROL_JEFE_CALIDAD");
        boolean superAdmin = authorities.contains("ROL_SUPER_ADMIN");
        boolean analista = authorities.contains("ROL_ANALISTA_CALIDAD");
        boolean micro = authorities.contains("ROL_MICROBIOLOGO");

        org.springframework.data.domain.Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fechaFabricacion");
        org.springframework.data.domain.Pageable effectivePageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort);

        org.springframework.data.jpa.domain.Specification<LoteProducto> specification = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(root.get("estado").in(estados));
            if (!jefe && !superAdmin) {
                var productoJoin = root.join("producto");
                if (analista && micro) {
                    predicates.add(builder.or(
                            builder.isTrue(productoJoin.get("requiereAnalisisFisico")),
                            builder.isTrue(productoJoin.get("requiereAnalisisQuimico")),
                            builder.isTrue(productoJoin.get("requiereAnalisisMicrobiologico"))
                    ));
                } else if (analista) {
                    predicates.add(builder.isTrue(productoJoin.get("requiereAnalisisFisico")));
                } else if (micro) {
                    predicates.add(builder.or(
                            builder.isTrue(productoJoin.get("requiereAnalisisQuimico")),
                            builder.isTrue(productoJoin.get("requiereAnalisisMicrobiologico"))
                    ));
                }
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        List<LoteProducto> lotesOrdenados = loteRepo.findAll(specification, sort);
        List<Long> loteIds = lotesOrdenados.stream()
                .map(LoteProducto::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        java.util.Map<Long, List<EvaluacionCalidad>> evaluacionesPorLote = loteIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : evaluacionRepository.findByLoteProductoIdIn(loteIds).stream()
                .filter(e -> e.getLoteProducto() != null && e.getLoteProducto().getId() != null)
                .collect(java.util.stream.Collectors.groupingBy(e -> e.getLoteProducto().getId()));

        java.util.Set<Long> evaluacionesQuimicoMicro = evaluacionesPorLote.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.getTipoEvaluacion() == TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .map(EvaluacionCalidad::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<Long> evaluacionesConResultadosMicro = evaluacionesQuimicoMicro.isEmpty()
                ? java.util.Collections.emptySet()
                : resultadoAnalisisMicrobiologicoRepository.findByEvaluacionIdIn(new java.util.ArrayList<>(evaluacionesQuimicoMicro))
                .stream()
                .map(r -> r.getEvaluacion() != null ? r.getEvaluacion().getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        List<LoteProductoResponseDTO> filtrados = lotesOrdenados.stream()
                .map(lote -> {
                    List<EvaluacionCalidad> evaluaciones = evaluacionesPorLote.getOrDefault(lote.getId(), List.of());
                    EstadoEvaluacionPendiente estado = calcularEstadoEvaluacionPendiente(lote.getProducto(),
                            evaluaciones, evaluacionesConResultadosMicro);
                    // Criterio "por evaluar": incluir el lote si falta al menos una disciplina requerida.
                    if (!estado.estaPendiente()) {
                        return null;
                    }
                    LoteProductoResponseDTO dto = loteProductoMapper.toDto(lote);
                    dto.setEstadoCalidadResumen(lote.getEstadoCalidadResumen());
                    dto.setEvaluaciones(evaluaciones.stream()
                            .map(EvaluacionCalidad::getTipoEvaluacion)
                            .toList());
                    dto.setTieneEvaluacionFisica(estado.tieneEvaluacionFisica);
                    dto.setTieneEvaluacionQuimicoMicro(estado.tieneEvaluacionQuimicoMicro);
                    dto.setEvaluacionQuimicoMicroId(estado.evaluacionQuimicoMicroId);
                    dto.setTieneResultadosMicro(estado.tieneResultadosMicro);
                    dto.setPendienteFisico(estado.pendienteFisico);
                    dto.setPendienteQuimico(estado.pendienteQuimico);
                    dto.setPendienteMicro(estado.pendienteMicro);
                    if (lote.getProducto() != null) {
                        PlantillaAnalisisMicroDTO plantillaDto = plantillaAnalisisMicroService.obtenerPorProducto(lote.getProducto().getId().longValue());
                        if (plantillaDto != null) {
                            dto.setRequiereAnalisisMicro(plantillaDto.isRequiereAnalisisMicro());
                            if (plantillaDto.getId() != null) {
                                dto.setPlantillaMicroId(plantillaDto.getId());
                            }
                        }
                    }
                    return dto;
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        int start = (int) Math.min(effectivePageable.getOffset(), filtrados.size());
        int end = Math.min(start + effectivePageable.getPageSize(), filtrados.size());
        List<LoteProductoResponseDTO> pageContent = filtrados.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(pageContent, effectivePageable, filtrados.size());
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

        if (!EnumSet.of(EstadoLote.LIBERADO, EstadoLote.DISPONIBLE).contains(estadoActual)) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "El lote no puede reabrirse desde su estado actual.",
                    Map.of("loteId", loteId, "estadoActual", estadoActual.name()));
        }

        Almacen almacenPrevio = lote.getAlmacen();
        retencionLoteService.retenerLote(lote.getId(),
                MotivoRetencion.REEVALUACION,
                motivo.trim(),
                null,
                usuarioActual,
                true);

        lote = loteRepo.findById(loteId).orElse(lote);

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

    private EstadoEvaluacionPendiente calcularEstadoEvaluacionPendiente(Producto producto,
                                                                        List<EvaluacionCalidad> evaluaciones,
                                                                        java.util.Set<Long> evaluacionesConResultadosMicro) {
        EstadoEvaluacionPendiente estado = new EstadoEvaluacionPendiente();
        if (producto == null) {
            return estado;
        }
        List<EvaluacionCalidad> seguras = evaluaciones == null ? List.of() : evaluaciones;
        estado.requiereAnalisisFisico = requiereFisico(producto);
        estado.requiereAnalisisQuimico = requiereQuimico(producto);
        estado.requiereAnalisisMicro = requiereMicro(producto);

        estado.tieneEvaluacionFisica = seguras.stream()
                .anyMatch(e -> e.getTipoEvaluacion() == TipoEvaluacion.FISICO);
        List<EvaluacionCalidad> evaluacionesQM = seguras.stream()
                .filter(e -> e.getTipoEvaluacion() == TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .toList();
        estado.tieneEvaluacionQuimicoMicro = !evaluacionesQM.isEmpty();
        estado.evaluacionQuimicoMicroId = evaluacionesQM.stream()
                .max(java.util.Comparator.comparing(EvaluacionCalidad::getFechaEvaluacion,
                        java.util.Comparator.nullsLast(java.time.LocalDateTime::compareTo)))
                .map(EvaluacionCalidad::getId)
                .orElse(null);

        List<Long> evaluacionIds = evaluacionesQM.stream()
                .map(EvaluacionCalidad::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        estado.tieneResultadosMicro = !evaluacionIds.isEmpty()
                && evaluacionesConResultadosMicro != null
                && evaluacionIds.stream().anyMatch(evaluacionesConResultadosMicro::contains);

        estado.pendienteFisico = estado.requiereAnalisisFisico && !estado.tieneEvaluacionFisica;
        estado.pendienteQuimico = estado.requiereAnalisisQuimico && !estado.tieneEvaluacionQuimicoMicro;
        estado.pendienteMicro = estado.requiereAnalisisMicro
                && (!estado.tieneEvaluacionQuimicoMicro || !estado.tieneResultadosMicro);
        return estado;
    }

    private static class EstadoEvaluacionPendiente {
        private boolean requiereAnalisisFisico;
        private boolean requiereAnalisisQuimico;
        private boolean requiereAnalisisMicro;
        private boolean tieneEvaluacionFisica;
        private boolean tieneEvaluacionQuimicoMicro;
        private Long evaluacionQuimicoMicroId;
        private boolean tieneResultadosMicro;
        private boolean pendienteFisico;
        private boolean pendienteQuimico;
        private boolean pendienteMicro;

        private boolean estaPendiente() {
            return pendienteFisico || pendienteQuimico || pendienteMicro;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoteProductoResponseDTO> listarTodos(String producto, Long productoId, EstadoLote estado,
                                                     String almacen, Long almacenId, Boolean vencidos,
                                                     LocalDateTime fechaInicio, LocalDateTime fechaFin,
                                                     Pageable pageable) {
        Specification<LoteProducto> spec = Specification.where(equalsEstado(estado));

        if (productoId != null) {
            spec = spec.and(conProductoId(productoId));
        } else {
            spec = spec.and(productoNombreContains(producto));
        }

        if (almacenId != null) {
            spec = spec.and(conAlmacenId(almacenId));
        } else {
            spec = spec.and(almacenNombreContains(almacen));
        }

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
    public LoteProductoResponseDTO liberarLote(Long id, String observacion) {
        // Alias de liberación por calidad. Se recomienda usar /api/calidad/lotes/{id}/liberar.
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        return liberarLoteConReglasCalidad(id, usuario, true, observacion, "LIBERAR");
    }

    @Transactional
    @Override
    public LoteProductoResponseDTO rechazarLote(Long id, String observacion) {
        Usuario usuarioActual = usuarioService.obtenerUsuarioAutenticado();
        validarObservacionObligatoria(observacion);

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
        Almacen almacenActual = lote.getAlmacen();
        if (almacenActual == null || almacenObsoletosId == null) {
            // LOTE_EN_ALMACEN_INVALIDO_PARA_RECHAZO aplica solo a inconsistencias de almacén.
            throw new CustomBusinessException(ApiErrorCode.LOTE_EN_ALMACEN_INVALIDO_PARA_RECHAZO,
                    "LOTE_EN_ALMACEN_INVALIDO_PARA_RECHAZO");
        }

        Almacen almacenRechazo = almacenRepo.findById(almacenObsoletosId).orElse(null);
        if (almacenRechazo == null) {
            // LOTE_EN_ALMACEN_INVALIDO_PARA_RECHAZO aplica solo a inconsistencias de almacén.
            throw new CustomBusinessException(ApiErrorCode.LOTE_EN_ALMACEN_INVALIDO_PARA_RECHAZO,
                    "LOTE_EN_ALMACEN_INVALIDO_PARA_RECHAZO");
        }
        if (almacenActual.getId().equals(almacenRechazo.getId())) {
            // LOTE_EN_ALMACEN_INVALIDO_PARA_RECHAZO aplica solo a lotes ya en rechazo/obsoletos.
            throw new CustomBusinessException(ApiErrorCode.LOTE_EN_ALMACEN_INVALIDO_PARA_RECHAZO,
                    "LOTE_EN_ALMACEN_INVALIDO_PARA_RECHAZO");
        }

        if (lote.getEstado() != EstadoLote.EN_CUARENTENA && lote.getEstado() != EstadoLote.RETENIDO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_EN_CUARENTENA");
        }
        if (lote.getStockReservado() != null && lote.getStockReservado().compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_CON_RESERVAS");
        }
        if (lote.getStockLote() == null || lote.getStockLote().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_SIN_STOCK");
        }

        Almacen origen = almacenActual;
        Almacen destino = almacenRechazo;
        BigDecimal cantidad = lote.getStockLote();
        Producto producto = lote.getProducto();

        EstadoLote estadoAnterior = lote.getEstado();
        lote.setEstado(EstadoLote.RECHAZADO);
        lote.setAlmacen(destino);
        loteRepo.save(lote);

        registrarBitacoraEstadoCalidad(lote, estadoAnterior, lote.getEstado(), "RECHAZAR", observacion, usuarioActual);

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
    public LoteProductoResponseDTO liberarLoteRetenido(Long id, String observacion) {
        // Alias de liberación por calidad. Se recomienda usar /api/calidad/lotes/{id}/liberar.
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        return liberarLoteConReglasCalidad(id, usuario, true, observacion, "LIBERAR_RETENIDO");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoteProductoResponseDTO liberarLotePorCalidad(Long loteId, Usuario usuarioActual, String observacion) {
        if (usuarioActual == null
                || (usuarioActual.getRol() != RolUsuario.ROL_JEFE_CALIDAD
                && usuarioActual.getRol() != RolUsuario.ROL_SUPER_ADMIN)) {
            throw new CustomBusinessException(ApiErrorCode.ROL_INSUFICIENTE,
                    "Solo el Jefe de Calidad o Super Admin pueden liberar lotes.");
        }
        return liberarLoteConReglasCalidad(loteId, usuarioActual, true, observacion, "LIBERAR");
    }

    private LoteProductoResponseDTO liberarLoteConReglasCalidad(Long loteId,
                                                                Usuario usuarioActual,
                                                                boolean moverAlmacen,
                                                                String observacion,
                                                                String accionBitacora) {
        validarObservacionObligatoria(observacion);
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

        if (lote.getEstado() == EstadoLote.RECHAZADO || lote.getEstado() == EstadoLote.VENCIDO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_LIBERABLE");
        }
        if (lote.getEstado() != EstadoLote.EN_CUARENTENA && lote.getEstado() != EstadoLote.RETENIDO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_EN_CUARENTENA");
        }

        Long almacenCuarentenaId = catalogResolver.getAlmacenCuarentenaId();
        if (almacenCuarentenaId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_CUARENTENA_NO_CONFIGURADO");
        }
        if (lote.getAlmacen() == null || !almacenCuarentenaId.equals(lote.getAlmacen().getId().longValue())) {
            Long almacenId = lote.getAlmacen() != null ? Long.valueOf(lote.getAlmacen().getId()) : null;
            log.info("[INVENTARIO] intento de liberar lote fuera de cuarentena. loteId={} almacenId={} estado={}",
                    loteId, almacenId, lote.getEstado());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "LOTE_NO_EN_CUARENTENA (almacenId=" + almacenId + ", estado=" + lote.getEstado() + ")");
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

        validarEvaluacionesExistentes(loteId);
        validarDisciplinasParaLiberacion(lote);
        validarNoConformidadesParaLiberacion(loteId);
        validarRetencionesParaLiberacion(loteId);
        validarCondicionesUsoParaLiberacion(loteId);

        Producto producto = lote.getProducto();
        Long destinoPrincipalId = catalogResolver.resolveAlmacenPrincipal(producto);
        if (destinoPrincipalId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_DESTINO_NO_CONFIGURADO");
        }

        if (moverAlmacen && destinoPrincipalId.equals(lote.getAlmacen().getId().longValue())
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

        Almacen origen = lote.getAlmacen();
        Almacen destino = moverAlmacen
                ? almacenRepo.findById(destinoPrincipalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_DESTINO_INEXISTENTE"))
                : origen;
        BigDecimal cantidad = lote.getStockLote();

        EstadoLote estadoAnterior = lote.getEstado();
        lote.setEstado(estadoLiberado);
        lote.setFechaLiberacion(LocalDateTime.now());
        lote.setUsuarioLiberador(usuarioActual);
        lote.setAlmacen(destino);
        loteRepo.save(lote);

        registrarBitacoraEstadoCalidad(lote, estadoAnterior, estadoLiberado, accionBitacora, observacion, usuarioActual);

        log.info("[INVENTARIO] liberación de lote completada destino={} tipoProducto={} loteId={}",
                destinoPrincipalId,
                producto != null && producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getTipo() : null,
                loteId);

        if (moverAlmacen) {
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
        }

        return loteProductoMapper.toResponseDTO(lote);
    }

    private java.util.Set<Long> obtenerEvaluacionesConResultadosMicro(List<EvaluacionCalidad> evaluaciones) {
        if (evaluaciones == null || evaluaciones.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        List<Long> ids = evaluaciones.stream()
                .filter(e -> e.getTipoEvaluacion() == TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .map(EvaluacionCalidad::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return resultadoAnalisisMicrobiologicoRepository.findByEvaluacionIdIn(ids).stream()
                .map(r -> r.getEvaluacion() != null ? r.getEvaluacion().getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
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
        String observacion = construirDetalleReapertura(motivo, comentarios, almacenAnterior);
        registrarBitacoraEstadoCalidad(lote, estadoAnterior, EstadoLote.RETENIDO, "REABRIR", observacion, usuarioActual);
        bitacoraCambiosInventarioService.crear(BitacoraCambiosInventarioDTO.builder()
                .tablaAfectada("lotes_productos")
                .registroId(lote.getId())
                .campoModificado("motivo_reapertura")
                .valorAnt("N/A")
                .valorNuevo(observacion)
                .fechaCambio(ahora)
                .usuarioId(usuarioActual.getId())
                .usuarioNombre(resolveNombreUsuario(usuarioActual))
                .observacion(observacion)
                .accion("REABRIR")
                .build());
    }

    private void registrarBitacoraEstadoCalidad(LoteProducto lote,
                                                EstadoLote estadoAnterior,
                                                EstadoLote estadoNuevo,
                                                String accion,
                                                String observacion,
                                                Usuario usuarioActual) {
        if (lote == null || usuarioActual == null || usuarioActual.getId() == null) {
            return;
        }
        validarObservacionObligatoria(observacion);
        bitacoraCambiosInventarioService.crear(BitacoraCambiosInventarioDTO.builder()
                .tablaAfectada("lotes_productos")
                .registroId(lote.getId())
                .campoModificado("estado")
                .valorAnt(estadoAnterior != null ? estadoAnterior.name() : "N/A")
                .valorNuevo(estadoNuevo != null ? estadoNuevo.name() : "N/A")
                .fechaCambio(LocalDateTime.now())
                .usuarioId(usuarioActual.getId())
                .usuarioNombre(resolveNombreUsuario(usuarioActual))
                .observacion(observacion != null ? observacion.trim() : null)
                .accion(accion)
                .build());
    }

    private String resolveNombreUsuario(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        if (usuario.getNombreCompleto() != null && !usuario.getNombreCompleto().isBlank()) {
            return usuario.getNombreCompleto();
        }
        if (usuario.getNombreUsuario() != null && !usuario.getNombreUsuario().isBlank()) {
            return usuario.getNombreUsuario();
        }
        return "DESCONOCIDO";
    }

    private void validarObservacionObligatoria(String observacion) {
        if (observacion == null || observacion.isBlank()) {
            throw new CustomBusinessException(ApiErrorCode.OBSERVACION_REQUERIDA,
                    "La observación es obligatoria para registrar la transición de estado del lote.");
        }
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

    private void validarRetencionesParaLiberacion(Long loteId) {
        var retenciones = retencionLoteService.obtenerRetencionesActivas(loteId);
        if (retenciones != null && !retenciones.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO,
                    "No es posible liberar un lote con retenciones activas.",
                    Map.of("loteId", loteId));
        }
    }

    private void validarCondicionesUsoParaLiberacion(Long loteId) {
        var condiciones = condicionUsoService.getActivasByLote(loteId);
        if (condiciones != null && !condiciones.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.BLOQUEO_CONDICION_USO,
                    "No es posible liberar un lote con condiciones de uso activas.",
                    Map.of("loteId", loteId));
        }
    }

    private void validarDisciplinasParaLiberacion(LoteProducto lote) {
        List<EvaluacionCalidad> evaluaciones = evaluacionRepository.findByLoteProductoId(lote.getId());
        java.util.Set<Long> evaluacionesConResultadosMicro = obtenerEvaluacionesConResultadosMicro(evaluaciones);
        var validacion = validarDisciplinasCompletas(lote, evaluaciones,
                evaluacionId -> evaluacionesConResultadosMicro.contains(evaluacionId));
        if (!validacion.esValido()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, validacion.getPrimerMensaje());
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
                .tieneRetencionActiva(retencion != null)
                .tieneNoConformidadActiva(ncOpt.isPresent())
                .tieneCondicionUsoActiva(!condiciones.isEmpty())
                .retencionActiva(retencion != null)
                .motivoRetencion(retencion != null ? retencion.getMotivo() : null)
                .nc(ncResumen)
                .condicionUsoActiva(!condiciones.isEmpty())
                .condicionUso(condicionResumen)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoPorLoteDTO resolverProductoPorLote(String codigoLote, Long ordenProduccionId) {
        if (!StringUtils.hasText(codigoLote)) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "CODIGO_LOTE_OBLIGATORIO");
        }
        if (ordenProduccionId == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "ORDEN_PRODUCCION_OBLIGATORIA");
        }

        List<LoteProducto> lotes = loteProductoRepository.findByCodigoLoteAndOrdenProduccion(codigoLote, ordenProduccionId);
        if (lotes.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "LOTE_NO_ENCONTRADO");
        }
        if (lotes.size() > 1) {
            throw new CustomBusinessException(ApiErrorCode.NEGOCIO_GENERICO, "LOTE_AMBIGUO");
        }

        LoteProducto lote = lotes.get(0);
        Producto producto = lote.getProducto();
        if (producto == null || producto.getId() == null) {
            throw new CustomBusinessException(ApiErrorCode.NEGOCIO_GENERICO, "LOTE_SIN_PRODUCTO_ASOCIADO");
        }

        return new ProductoPorLoteDTO(
                producto.getId().longValue(),
                producto.getCodigoSku(),
                producto.getNombre()
        );
    }
}
