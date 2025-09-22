package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.produccion.dto.InsumoFaltanteDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.dto.EtapaProduccionResponse;
import com.willyes.clemenintegra.produccion.dto.InsumoOPDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.produccion.mapper.ProduccionMapper;
import com.willyes.clemenintegra.produccion.service.UnidadConversionService;
import com.willyes.clemenintegra.inventario.service.UmValidator;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.CierreProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import com.willyes.clemenintegra.produccion.service.spec.OrdenProduccionSpecifications;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.dto.LoteFefoDisponibleProjection;
import com.willyes.clemenintegra.inventario.service.ReservaLoteService;
import com.willyes.clemenintegra.produccion.dto.LoteProductoResponse;
import com.willyes.clemenintegra.inventario.dto.AlmacenResponseDTO;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.VidaUtilProductoRepository;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.ErrorResponseException;
import org.springframework.http.ProblemDetail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Objects;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import jakarta.persistence.OptimisticLockException;

@Service
@RequiredArgsConstructor
public class OrdenProduccionServiceImpl implements OrdenProduccionService {

    private static final Logger log = LoggerFactory.getLogger(OrdenProduccionServiceImpl.class);

    private final FormulaProductoRepository formulaProductoRepository;
    private final ProductoRepository productoRepository;
    private final StockQueryService stockQueryService;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudMovimientoService solicitudMovimientoService;
    private final OrdenProduccionRepository repository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private final CierreProduccionRepository cierreProduccionRepository;
    private final MovimientoInventarioService movimientoInventarioService;
    private final LoteProductoRepository loteProductoRepository;
    private final AlmacenRepository almacenRepository;
    private final UnidadConversionService unidadConversionService;
    private final EtapaProduccionRepository etapaProduccionRepository;
    private final EtapaPlantillaRepository etapaPlantillaRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final MovimientoInventarioMapper movimientoInventarioMapper;
    private final UsuarioService usuarioService;
    private final SolicitudMovimientoRepository solicitudMovimientoRepository;
    private final InventoryCatalogResolver catalogResolver;
    private final UmValidator umValidator;
    private final VidaUtilProductoRepository vidaUtilProductoRepository;
    private final ReservaLoteService reservaLoteService;

    @Value("${inventory.solicitud.estados.pendientes}")
    private String estadosSolicitudPendientesConf;

    @Value("${inventory.solicitud.estados.concluyentes}")
    private String estadosSolicitudConcluyentesConf;

    @Value("${inventory.mov.clasificacion.entradaPt}")
    private String clasificacionEntradaPtConf;

    private String generarCodigoOrden() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefijo = "OP-CLEMEN-" + fecha;
        Long contador = repository.countByCodigoOrdenStartingWith(prefijo);
        return prefijo + "-" + String.format("%02d", contador + 1);
    }

    /**
     * Obtiene los almacenes habilitados para consumir insumos desde producción.
     * <p>
     * Actualmente solo se permite consumir desde el almacén de origen configurado
     * por categoría para producción. La pre-bodega de producción se excluye de
     * manera explícita porque su stock está reservado para las transferencias de
     * salida y no debe afectar la validación ni la reserva FEFO de insumos.
     * </p>
     */
    private List<Long> obtenerAlmacenesOrigen(Producto insumo) {
        Long preBodegaProduccionId = catalogResolver.getAlmacenPreBodegaProduccionId();
        Long origenId = obtenerAlmacenOrigenSegunCategoria(insumo);

        if (origenId == null || Objects.equals(origenId, preBodegaProduccionId)) {
            return List.of();
        }

        return List.of(origenId);
    }

    private Long obtenerAlmacenOrigenSegunCategoria(Producto insumo) {
        if (insumo == null || insumo.getCategoriaProducto() == null ||
                insumo.getCategoriaProducto().getTipo() == null) {
            return null;
        }

        return switch (insumo.getCategoriaProducto().getTipo()) {
            case MATERIA_PRIMA -> catalogResolver.getAlmacenMateriaPrimaId();
            case MATERIAL_EMPAQUE -> catalogResolver.getAlmacenMaterialEmpaqueId();
            case SUMINISTROS -> catalogResolver.getAlmacenSuministrosId();
            default -> null;
        };
    }

    private String generarCodigoLote(Producto producto) {
        if (producto.getCategoriaProducto() == null ||
                producto.getCategoriaProducto().getTipo() != TipoCategoria.PRODUCTO_TERMINADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PRODUCTO_NO_TERMINADO");
        }
        String prefijo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Optional<OrdenProduccion> ultimo = repository
                .findTopByLoteProduccionStartingWithOrderByLoteProduccionDesc(prefijo);
        int consecutivo = 0;
        if (ultimo.isPresent() && ultimo.get().getLoteProduccion() != null) {
            String codigo = ultimo.get().getLoteProduccion();
            if (codigo.length() >= 10) {
                String seq = codigo.substring(8, 10);
                consecutivo = Integer.parseInt(seq) + 1;
            }
        }
        if (consecutivo > 99) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CONSECUTIVO_DIARIO_EXCEDIDO");
        }
        String iniciales = producto.getNombre() != null
                ? producto.getNombre().replaceAll("\\s+", "").toUpperCase()
                : "";
        iniciales = iniciales.substring(0, Math.min(3, iniciales.length()));
        return prefijo + String.format("%02d", consecutivo) + "-" + iniciales;
    }

    private List<EstadoSolicitudMovimiento> parseEstados(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ESTADOS_SOLICITUD_NO_CONFIGURADOS");
        }
        try {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(EstadoSolicitudMovimiento::valueOf)
                    .toList();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ESTADOS_SOLICITUD_NO_CONFIGURADOS");
        }
    }

    private BigDecimal validarCantidad(BigDecimal cantidadOriginal, Producto producto) {
        if (cantidadOriginal == null || producto == null || producto.getUnidadMedida() == null
                || cantidadOriginal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_INVALIDA");
        }

        String umCodigo = Optional.ofNullable(producto.getUnidadMedida().getSimbolo())
                .orElse(producto.getUnidadMedida().getNombre());
        if (umCodigo == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_INVALIDA");
        }

        BigDecimal cantidad = umValidator.ajustar(cantidadOriginal);
        RoundingMode redondeo = umValidator.getRoundingMode();

        BigDecimal cantidadLote = cantidad.setScale(2, redondeo);
        int enterosLote = cantidadLote.precision() - cantidadLote.scale();
        if (enterosLote > 8) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PRECISION_LOTE_EXCEDIDA");
        }

        BigDecimal cantidadMov = cantidadLote;
        int enterosMov = cantidadMov.precision() - cantidadMov.scale();
        if (enterosMov > 7) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PRECISION_MOV_EXCEDIDA");
        }

        log.info("OP-cierre validarCantidad prod={}, um={}, recibida={}, final={}, modo={}"
                , producto.getId(), umCodigo, cantidadOriginal, cantidadLote, redondeo);

        return cantidadLote;
    }

    @Transactional
    public ResultadoValidacionOrdenDTO guardarConValidacionStock(OrdenProduccion orden) {
        if (orden.getCantidadProducida() == null) {
            orden.setCantidadProducida(BigDecimal.ZERO);
        }
        if (orden.getCantidadProducidaAcumulada() == null) {
            orden.setCantidadProducidaAcumulada(BigDecimal.ZERO);
        }
        Long productoId = orden.getProducto().getId().longValue();

        FormulaProducto formula = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(productoId, EstadoFormula.APROBADA)
                .orElseThrow(() -> new IllegalArgumentException("No existe una fórmula activa y aprobada para el producto"));

        Map<Long, BigDecimal> cantidadesEscaladas = new HashMap<>();
        List<InsumoFaltanteDTO> faltantes = new ArrayList<>();
        boolean stockSuficiente = true;
        Integer maxProducible = null;

        BigDecimal cantidadProgramada = orden.getCantidadProgramada();

        // Cargar todos los productos de los insumos en una sola consulta para evitar N+1
        List<Long> insumoIds = formula.getDetalles().stream()
                .map(d -> d.getInsumo().getId().longValue())
                .toList();
        Map<Long, Producto> productosInsumo = productoRepository.findAllById(insumoIds).stream()
                .collect(Collectors.toMap(p -> p.getId().longValue(), p -> p));

        for (DetalleFormula insumo : formula.getDetalles()) {
            Long insumoId = insumo.getInsumo().getId().longValue();
            Producto productoInsumo = productosInsumo.get(insumoId);
            if (productoInsumo == null) {
                throw new IllegalArgumentException("Insumo no encontrado: ID " + insumoId);
            }

            BigDecimal cantidadRequerida = insumo.getCantidadNecesaria().multiply(cantidadProgramada);
            cantidadesEscaladas.put(insumoId, cantidadRequerida);

            List<Long> almacenesValidos = obtenerAlmacenesOrigen(insumo.getInsumo());
            BigDecimal stockDisponible = BigDecimal.ZERO;
            if (!almacenesValidos.isEmpty()) {
                stockDisponible = stockQueryService
                        .obtenerStockDisponible(List.of(insumoId), almacenesValidos)
                        .getOrDefault(insumoId, BigDecimal.ZERO);
            }

            int producibleConEste = 0;
            if (insumo.getCantidadNecesaria().compareTo(BigDecimal.ZERO) > 0) {
                producibleConEste = stockDisponible.divide(insumo.getCantidadNecesaria(), 0, RoundingMode.DOWN).intValue();
            }
            if (maxProducible == null || producibleConEste < maxProducible) {
                maxProducible = producibleConEste;
            }

            if (stockDisponible.compareTo(cantidadRequerida) < 0) {
                stockSuficiente = false;
                faltantes.add(InsumoFaltanteDTO.builder()
                        .productoId(insumoId)
                        .nombre(productoInsumo.getNombre())
                        .requerido(cantidadRequerida)
                        .disponible(stockDisponible)
                        .unidadSimbolo(productoInsumo.getUnidadMedida() != null
                                ? productoInsumo.getUnidadMedida().getSimbolo()
                                : null)
                        .build());
            }
        }

        if (!stockSuficiente) {
            return ResultadoValidacionOrdenDTO.builder()
                    .esValida(false)
                    .mensaje("Stock insuficiente para algunos insumos")
                    .unidadesMaximasProducibles(maxProducible)
                    .insumosFaltantes(faltantes)
                    .build();
        }

        // Aseguramos que la fecha de inicio siempre sea asignada desde el backend
        orden.setFechaInicio(LocalDateTime.now());
        if (orden.getId() == null) {
            orden.setCodigoOrden(generarCodigoOrden());
        } else if (orden.getCodigoOrden() == null) {
            orden.setCodigoOrden(repository.findById(orden.getId())
                    .map(OrdenProduccion::getCodigoOrden)
                    .orElse(generarCodigoOrden()));
        }

        OrdenProduccion guardada = repository.save(orden);

        List<EtapaPlantilla> plantilla = cargarPlantillaEtapas(guardada.getProducto().getId());
        clonarEtapasParaOrden(guardada, plantilla);
        reservarInsumosParaOP(guardada.getId());

        Long motivoId = motivoMovimientoRepository
                .findByMotivo(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION)
                .map(m -> m.getId())
                .orElseThrow(() -> new IllegalStateException("Motivo TRANSFERENCIA_INTERNA_PRODUCCION no configurado"));
        Long tipoDetalleId = tipoMovimientoDetalleRepository
                .findById(catalogResolver.getTipoDetalleSalidaId())
                .map(TipoMovimientoDetalle::getId)
                .orElseThrow(() -> new IllegalStateException("Tipo detalle SALIDA_PRODUCCION no configurado"));

        for (DetalleFormula insumo : formula.getDetalles()) {
            Long insumoId = insumo.getInsumo().getId().longValue();
            BigDecimal cantidad = cantidadesEscaladas.get(insumoId);
            SolicitudMovimientoRequestDTO req = SolicitudMovimientoRequestDTO.builder()
                    .tipoMovimiento(TipoMovimiento.TRANSFERENCIA)
                    .productoId(insumoId)
                    .cantidad(cantidad)
                    .ordenProduccionId(guardada.getId())
                    .usuarioSolicitanteId(guardada.getResponsable().getId())
                    .motivoMovimientoId(motivoId)
                    .tipoMovimientoDetalleId(tipoDetalleId)
                    .almacenDestinoId(catalogResolver.getAlmacenPreBodegaProduccionId())
                    .build();
            solicitudMovimientoService.registrarSolicitud(req);
        }

        OrdenProduccionResponseDTO ordenResp = ProduccionMapper.toResponse(guardada);

        return ResultadoValidacionOrdenDTO.builder()
                .esValida(true)
                .mensaje("Orden de producción creada correctamente")
                .orden(ordenResp)
                .build();
    }

    @Transactional
    public ResultadoValidacionOrdenDTO crearOrden(OrdenProduccionRequestDTO dto) {
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        Usuario responsable = usuarioRepository.findById(dto.getResponsableId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        BigDecimal cantidadBase = dto.getCantidadProgramada();
        String unidadBase = dto.getUnidadMedidaSimbolo();
        String unidadProducto = producto.getUnidadMedida() != null ? producto.getUnidadMedida().getSimbolo() : unidadBase;
        if (unidadBase == null || unidadBase.isBlank()) {
            unidadBase = unidadProducto;
            dto.setUnidadMedidaSimbolo(unidadBase);
        }
        BigDecimal cantidadConvertida = unidadConversionService.convertir(cantidadBase, unidadBase, unidadProducto);

        BigDecimal unidadesProducidas = unidadConversionService.dividirNormalizado(
                cantidadConvertida,
                unidadProducto,
                producto.getRendimientoUnidad(),
                unidadProducto);

        OrdenProduccion orden = ProduccionMapper.toEntity(dto, producto, responsable);
        orden.setCantidadProgramada(cantidadConvertida);
        orden.setCantidadProducida(BigDecimal.ZERO);
        orden.setCantidadProducidaAcumulada(BigDecimal.ZERO);

        ResultadoValidacionOrdenDTO resultado = guardarConValidacionStock(orden);
        resultado.setUnidadesProducidas(unidadesProducidas);
        if (resultado.getOrden() != null) {
            resultado.getOrden().cantidadProgramadaBase = cantidadBase;
            resultado.getOrden().unidadMedidaBaseSimbolo = unidadBase;
            resultado.getOrden().unidadMedidaSimbolo = unidadProducto;
            resultado.getOrden().unidadesProducidas = unidadesProducidas;
        }
        return resultado;
    }


    @Override
    public Page<OrdenProduccionResponseDTO> listarPaginado(String codigo,
                                                           EstadoProduccion estado,
                                                           String responsable,
                                                           LocalDateTime fechaInicio,
                                                           LocalDateTime fechaFin,
                                                           Pageable pageable) {
        Specification<OrdenProduccion> spec = OrdenProduccionSpecifications.and(
                OrdenProduccionSpecifications.byCodigo(codigo),
                OrdenProduccionSpecifications.byEstado(estado),
                OrdenProduccionSpecifications.byResponsable(responsable),
                OrdenProduccionSpecifications.byFechaBetween(fechaInicio, fechaFin)
        );
        return repository.findAll(spec, pageable).map(ProduccionMapper::toResponse);
    }

    public List<OrdenProduccion> listarTodas() {
        return repository.findAll();
    }

    public Optional<OrdenProduccion> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public OrdenProduccion registrarCierre(Long id, CierreProduccionRequestDTO dto) {
        try {
            OrdenProduccion orden = repository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));

            if (orden.getEstado() == EstadoProduccion.FINALIZADA ||
                    orden.getEstado() == EstadoProduccion.CANCELADA ||
                    orden.getEstado() == EstadoProduccion.CERRADA_INCOMPLETA) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ORDEN_NO_CERRABLE");
            }

            if (dto.getCantidad() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_INVALIDA");
            }

            BigDecimal cantidad = validarCantidad(dto.getCantidad(), orden.getProducto());
            dto.setCantidad(cantidad);

            LoteProducto lote = loteProductoRepository
                    .findByOrdenProduccionIdAndProductoId(orden.getId(), orden.getProducto().getId().longValue())
                    .orElse(null);
            LocalDateTime fechaFabricacion = lote != null ? lote.getFechaFabricacion() : null;
            LocalDateTime fechaVencimiento = lote != null ? lote.getFechaVencimiento() : null;

            if (fechaFabricacion == null) {
                fechaFabricacion = etapaProduccionRepository
                        .findByOrdenProduccionIdOrderBySecuenciaAsc(orden.getId())
                        .stream()
                        .findFirst()
                        .map(EtapaProduccion::getFechaInicio)
                        .orElse(null);
            }
            if (fechaVencimiento == null) {
                Integer semanasVigencia = vidaUtilProductoRepository.findById(orden.getProducto().getId())
                        .map(VidaUtilProducto::getSemanasVigencia)
                        .orElse(null);
                fechaVencimiento = (fechaFabricacion != null && semanasVigencia != null)
                        ? fechaFabricacion.plusWeeks(semanasVigencia)
                        : null;
            }

            if (fechaFabricacion == null || fechaFabricacion.isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "FECHA_INVALIDA");
            }
            if (fechaVencimiento != null && fechaVencimiento.isBefore(fechaFabricacion)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "FECHA_INVALIDA");
            }

            if (orden.getId() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ORDEN_PRODUCCION_OBLIGATORIA");
            }

            List<EstadoSolicitudMovimiento> estadosPendientes = parseEstados(estadosSolicitudPendientesConf);
            parseEstados(estadosSolicitudConcluyentesConf);

            Long motivoDevId = catalogResolver.getMotivoIdDevolucionDesdeProduccion();
            MotivoMovimiento motivoDevolucion = motivoMovimientoRepository.findById(motivoDevId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MOTIVO_DEVOLUCION_DESDE_PRODUCCION_INEXISTENTE"));

            List<SolicitudMovimiento> solicitudesPend = Optional.ofNullable(
                    solicitudMovimientoRepository.findWithDetalles(orden.getId(), estadosPendientes, null, null)
            ).orElse(List.of());

            List<Map<String, Object>> pendientes = new ArrayList<>();

            for (SolicitudMovimiento sol : solicitudesPend) {
                for (SolicitudMovimientoDetalle det : sol.getDetalles()) {
                    if (det.getLote() == null || det.getLote().getProducto() == null) {
                        continue;
                    }
                    Long prodId = det.getLote().getProducto().getId().longValue();
                    Long loteId = det.getLote().getId();

                    Long tipoDetalleSalidaId = catalogResolver.getTipoDetalleSalidaId();
                    Long tipoDetalleTransferenciaId = catalogResolver.getTipoDetalleTransferenciaId();
                    BigDecimal salida = movimientoInventarioRepository.sumaPorSolicitudYTipo(
                            sol.getId(), prodId, loteId, TipoMovimiento.SALIDA, tipoDetalleSalidaId, null);
                    if (tipoDetalleTransferenciaId != null) {
                        salida = salida.add(movimientoInventarioRepository.sumaPorSolicitudYTipo(
                                sol.getId(), prodId, loteId, TipoMovimiento.TRANSFERENCIA, tipoDetalleTransferenciaId, null));
                    }
                    BigDecimal devolucion = movimientoInventarioRepository.sumaPorSolicitudYTipo(
                            sol.getId(), prodId, loteId, TipoMovimiento.DEVOLUCION, null, motivoDevolucion.getId());

                    BigDecimal movido = salida.add(devolucion);
                    BigDecimal faltante = det.getCantidad().subtract(movido);
                    if (faltante.compareTo(BigDecimal.ZERO) > 0) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("solicitudId", sol.getId());
                        item.put("detalleId", det.getId());
                        item.put("productoId", prodId);
                        item.put("loteId", loteId);
                        item.put("solicitado", det.getCantidad());
                        item.put("movido", movido);
                        item.put("faltante", faltante);
                        pendientes.add(item);
                    }
                }
            }

            if (!pendientes.isEmpty()) {
                log.warn("OP-cierre reservas pendientes op={}, detallesPendientes={}", orden.getId(), pendientes.size());
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
                        "RESERVAS_PENDIENTES_OP");
                problem.setProperty("detalles", pendientes.stream().limit(20).toList());
                throw new ErrorResponseException(HttpStatus.UNPROCESSABLE_ENTITY, problem, null);
            }

            if (solicitudesPend.isEmpty()) {
                List<SolicitudMovimiento> todas = Optional.ofNullable(
                        solicitudMovimientoRepository.findWithDetalles(orden.getId(), null, null, null)
                ).orElse(List.of());
                long lotesConReserva = todas.stream()
                        .flatMap(s -> s.getDetalles().stream())
                        .map(SolicitudMovimientoDetalle::getLote)
                        .filter(l -> l != null && l.getStockReservado() != null && l.getStockReservado().compareTo(BigDecimal.ZERO) > 0)
                        .count();
                if (lotesConReserva > 0) {
                    log.warn("OP-cierre stock_reservado sin solicitud pendiente op={}, lotes={}", orden.getId(), lotesConReserva);
                }
            }

            Long motivoEntradaId = catalogResolver.getMotivoIdEntradaProductoTerminado();
            MotivoMovimiento motivoEntrada = motivoMovimientoRepository.findById(motivoEntradaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MOTIVO_ENTRADA_PT_INEXISTENTE"));

            ClasificacionMovimientoInventario clasifEntrada;
            try {
                clasifEntrada = ClasificacionMovimientoInventario.valueOf(clasificacionEntradaPtConf);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CLASIFICACION_ENTRADA_PT_INVALIDA");
            }

            Long tipoDetalleEntradaId = catalogResolver.getTipoDetalleEntradaId();
            TipoMovimientoDetalle tipoDetalleEntrada = tipoMovimientoDetalleRepository.findById(tipoDetalleEntradaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TIPO_DETALLE_ENTRADA_INEXISTENTE"));

            BigDecimal acumulada = Optional.ofNullable(orden.getCantidadProducidaAcumulada()).orElse(BigDecimal.ZERO);
            BigDecimal nuevaAcumulada = acumulada.add(cantidad);

            if (dto.getTipo() == TipoCierre.TOTAL) {
                boolean etapasFinalizadas = etapaProduccionRepository
                        .findByOrdenProduccionIdOrderBySecuenciaAsc(orden.getId())
                        .stream()
                        .allMatch(e -> e.getEstado() == EstadoEtapa.FINALIZADA);
                if (etapasFinalizadas) {
                    orden.setEstado(EstadoProduccion.FINALIZADA);
                    orden.setFechaFin(LocalDateTime.now());
                }
            }

            orden.setCantidadProducidaAcumulada(nuevaAcumulada);
            orden.setCantidadProducida(nuevaAcumulada);
            orden.setFechaUltimoCierre(LocalDateTime.now());

            Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
            CierreProduccion cierre = ProduccionMapper.toEntity(dto, orden);
            cierre.setUsuarioId(usuario.getId());
            cierre.setUsuarioNombre(usuario.getNombreCompleto());
            cierreProduccionRepository.save(cierre);

            if (orden.getProducto() == null || orden.getProducto().getTipoAnalisis() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCTO_SIN_TIPO_ANALISIS");
            }

            Long almacenPtId = catalogResolver.getAlmacenPtId();
            Long almacenCuarentenaId = catalogResolver.getAlmacenCuarentenaId();

            Almacen almacenPt = almacenRepository.findById(almacenPtId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_INEXISTENTE"));
            Almacen almacenCuarentena = almacenRepository.findById(almacenCuarentenaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_INEXISTENTE"));

            Almacen destino;
            EstadoLote estadoLote;
            TipoAnalisisCalidad tipoAnalisis = orden.getProducto().getTipoAnalisis();
            switch (tipoAnalisis) {
                case NINGUNO -> {
                    destino = almacenPt;
                    estadoLote = EstadoLote.DISPONIBLE;
                }
                case FISICO_QUIMICO, MICROBIOLOGICO, AMBOS -> {
                    destino = almacenCuarentena;
                    estadoLote = EstadoLote.EN_CUARENTENA;
                }
                default -> throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCTO_SIN_TIPO_ANALISIS");
            }

            String codigoLote = dto.getCodigoLote();
            if (lote == null) {
                if (codigoLote != null && !codigoLote.isBlank()) {
                    Optional<LoteProducto> existente = loteProductoRepository.findByCodigoLote(codigoLote);
                    if (existente.isPresent()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "CODIGO_LOTE_DUPLICADO");
                    }
                    orden.setLoteProduccion(codigoLote);
                } else {
                    codigoLote = Optional.ofNullable(orden.getLoteProduccion())
                            .orElseGet(() -> {
                                String gen = generarCodigoLote(orden.getProducto());
                                orden.setLoteProduccion(gen);
                                return gen;
                            });
                }

                lote = LoteProducto.builder()
                        .codigoLote(codigoLote)
                        .producto(orden.getProducto())
                        .almacen(destino)
                        .estado(estadoLote)
                        .stockLote(BigDecimal.ZERO)
                        .fechaFabricacion(fechaFabricacion)
                        .fechaVencimiento(fechaVencimiento)
                        .ordenProduccion(orden)
                        .build();
            } else {
                if (!lote.getAlmacen().getId().equals(destino.getId()) || lote.getEstado() != estadoLote) {
                    log.warn("Lote PT incompatible op={}, producto={}, loteId={}, almacenId={}, estadoLote={}, destino={}, estadoDestino={}",
                            orden.getId(), orden.getProducto().getId(), lote.getId(), lote.getAlmacen().getId(), lote.getEstado(), destino.getId(), estadoLote);
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "LOTE_PT_INCOMPATIBLE");
                }
                if (codigoLote != null && lote.getCodigoLote() != null && !lote.getCodigoLote().equals(codigoLote)) {
                    Optional<LoteProducto> existente = loteProductoRepository.findByCodigoLote(codigoLote);
                    if (existente.isPresent() && !existente.get().getId().equals(lote.getId())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "CODIGO_LOTE_DUPLICADO");
                    }
                } else if (lote.getCodigoLote() == null && codigoLote != null) {
                    lote.setCodigoLote(codigoLote);
                }
                if (lote.getFechaFabricacion() == null) {
                    lote.setFechaFabricacion(fechaFabricacion);
                }
                if (fechaVencimiento != null) {
                    lote.setFechaVencimiento(fechaVencimiento);
                }
                codigoLote = lote.getCodigoLote();
            }
            loteProductoRepository.save(lote);
            log.info("OP-cierre lote op={}, producto={}, loteId={}, codigoLote={}, cantidad={}, fechaFabricacion={}, fechaVencimiento={}, almacenId={}, estado={}, usuario={}",
                    orden.getId(), orden.getProducto().getId(), lote.getId(), codigoLote, cantidad, fechaFabricacion, fechaVencimiento, destino.getId(), estadoLote, usuario.getId());

            MovimientoInventarioDTO movDto = new MovimientoInventarioDTO(
                    null,
                    cantidad,
                    TipoMovimiento.ENTRADA,
                    clasifEntrada,
                    orden.getCodigoOrden(),
                    orden.getProducto().getId(),
                    lote.getId(),
                    null,
                    destino.getId().intValue(),
                    null,
                    null,
                    motivoEntrada.getId(),
                    tipoDetalleEntrada.getId(),
                    null,
                    null,
                    orden.getId(),
                    null,
                    null,
                    null,
                    lote.getEstado(),
                    null,
                    null);
            movimientoInventarioService.registrarMovimiento(movDto);
            log.info("OP-cierre entrada PT op={}, producto={}, lote={}, cantidad={}, usuario={}, destino={}, motivoId={}, tipoDetalleId={}",
                    orden.getId(), orden.getProducto().getId(), lote.getId(), cantidad, usuario.getId(), destino.getId(), motivoEntrada.getId(), tipoDetalleEntrada.getId());

            return repository.save(orden);
        } catch (OptimisticLockException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ORDEN_CONFLICTO");
        }
    }

    public Page<CierreProduccionResponseDTO> listarCierres(Long id, Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                if (!"fechaCierre".equals(order.getProperty())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Propiedad de ordenamiento inválida: " + order.getProperty());
                }
            });
        }
        Pageable effective = pageable.getSort().isUnsorted()
                ? org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), org.springframework.data.domain.Sort.by("fechaCierre").descending())
                : pageable;
        return cierreProduccionRepository.findByOrdenProduccionId(id, effective)
                .map(ProduccionMapper::toResponse);
    }

    public List<EtapaProduccionResponse> listarEtapas(Long id) {
        return etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(id)
                .stream()
                .map(ProduccionMapper::toResponse)
                .toList();
    }

    public List<EtapaPlantilla> cargarPlantillaEtapas(Integer productoId) {
        return etapaPlantillaRepository.findByProductoIdAndActivoTrueOrderBySecuenciaAsc(productoId);
    }

    public void clonarEtapasParaOrden(OrdenProduccion op, List<EtapaPlantilla> plantilla) {
        if (plantilla == null || plantilla.isEmpty()) return;
        List<EtapaProduccion> etapas = plantilla.stream()
                .map(p -> EtapaProduccion.builder()
                        .nombre(p.getNombre())
                        .secuencia(p.getSecuencia())
                        .ordenProduccion(op)
                        .estado(EstadoEtapa.PENDIENTE)
                        .build())
                .toList();
        etapaProduccionRepository.saveAll(etapas);
    }

    public void clonarEtapas(Long ordenId) {
        OrdenProduccion orden = repository.findById(ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));
        List<EtapaProduccion> existentes = etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(ordenId);
        if (existentes.isEmpty()) {
            List<EtapaPlantilla> plantilla = cargarPlantillaEtapas(orden.getProducto().getId());
            clonarEtapasParaOrden(orden, plantilla);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reservarInsumosParaOP(Long ordenId) {
        OrdenProduccion orden = repository.findById(ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));

        FormulaProducto formula = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(orden.getProducto().getId().longValue(), EstadoFormula.APROBADA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FORMULA_NO_ENCONTRADA"));

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        log.debug("OP-reserva iniciar ordenId={}, user={}", ordenId, usuario.getId());

        MotivoMovimiento motivo = motivoMovimientoRepository
                .findByMotivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION)
                .orElseThrow(() -> new IllegalStateException("Motivo SALIDA_PRODUCCION no configurado"));
        TipoMovimientoDetalle detalle = tipoMovimientoDetalleRepository
                .findById(catalogResolver.getTipoDetalleSalidaId())
                .orElseThrow(() -> new IllegalStateException("Tipo detalle SALIDA_PRODUCCION no configurado"));

        for (DetalleFormula insumo : formula.getDetalles()) {
            Long insumoId = insumo.getInsumo().getId().longValue();
            BigDecimal requerida = insumo.getCantidadNecesaria().multiply(orden.getCantidadProgramada());
            BigDecimal restante = requerida;

            List<Long> almacenesValidos = obtenerAlmacenesOrigen(insumo.getInsumo());
            if (almacenesValidos.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "STOCK_INSUFICIENTE: faltan " + restante);
            }

            List<LoteFefoDisponibleProjection> lotes = loteProductoRepository.findFefoDisponibles(insumoId, Integer.MAX_VALUE)
                    .stream()
                    .filter(l -> l.getAlmacenId() != null && almacenesValidos.contains(l.getAlmacenId().longValue()))
                    .toList();

            if (lotes.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "STOCK_INSUFICIENTE: faltan " + restante);
            }

            Long primerLoteId = lotes.get(0).getLoteProductoId();
            if (primerLoteId == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "STOCK_INSUFICIENTE: faltan " + restante);
            }

            SolicitudMovimientoRequestDTO solicitudReq = SolicitudMovimientoRequestDTO.builder()
                    .tipoMovimiento(TipoMovimiento.SALIDA)
                    .productoId(insumoId)
                    .loteId(primerLoteId)
                    .cantidad(requerida)
                    .ordenProduccionId(orden.getId())
                    .usuarioSolicitanteId(usuario.getId())
                    .motivoMovimientoId(motivo.getId())
                    .tipoMovimientoDetalleId(detalle.getId())
                    .build();

            SolicitudMovimientoResponseDTO solicitudCreada = solicitudMovimientoService.registrarSolicitud(solicitudReq);
            Long solicitudId = solicitudCreada.getId();
            if (solicitudId == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SOLICITUD_NO_ENCONTRADA");
            }

            SolicitudMovimiento solicitud = solicitudMovimientoRepository.findById(solicitudId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SOLICITUD_NO_ENCONTRADA"));

            solicitud.setLote(null);
            solicitud.setAlmacenOrigen(null);

            List<SolicitudMovimientoDetalle> detallesSolicitud = solicitud.getDetalles();
            if (detallesSolicitud == null) {
                detallesSolicitud = new ArrayList<>();
                solicitud.setDetalles(detallesSolicitud);
            } else {
                detallesSolicitud.clear();
            }

            for (LoteFefoDisponibleProjection lote : lotes) {
                if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal disponible = lote.getStockLote();
                BigDecimal usar = disponible.min(restante);
                if (usar.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                SolicitudMovimientoDetalle detSolicitud = SolicitudMovimientoDetalle.builder()
                        .solicitudMovimiento(solicitud)
                        .lote(new LoteProducto(lote.getLoteProductoId()))
                        .cantidad(usar)
                        .almacenOrigen(lote.getAlmacenId() != null ? new Almacen(lote.getAlmacenId()) : null)
                        .almacenDestino(solicitud.getAlmacenDestino())
                        .build();
                detallesSolicitud.add(detSolicitud);

                restante = restante.subtract(usar);
            }

            if (restante.compareTo(BigDecimal.ZERO) > 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "STOCK_INSUFICIENTE: faltan " + restante);
            }

            solicitudMovimientoRepository.saveAndFlush(solicitud);
            reservaLoteService.sincronizarReservasSolicitud(solicitud);
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public EtapaProduccion iniciarEtapa(Long ordenId, Long etapaId) {
        OrdenProduccion orden = repository.findById(ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));
        if (orden.getEstado() == EstadoProduccion.FINALIZADA || orden.getEstado() == EstadoProduccion.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ORDEN_NO_MODIFICABLE");
        }
        EtapaProduccion etapa = etapaProduccionRepository.findById(etapaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ETAPA_NO_ENCONTRADA"));
        if (!etapa.getOrdenProduccion().getId().equals(ordenId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ETAPA_NO_PERTENECE_A_ORDEN");
        }
        if (etapa.getEstado() != EstadoEtapa.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ETAPA_NO_INICIABLE");
        }

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        boolean actualizarOrden = false;
        if (orden.getEstado() == EstadoProduccion.CREADA) {
            orden.setEstado(EstadoProduccion.EN_PROCESO);
            actualizarOrden = true;
        }
        if ((etapa.getSecuencia() != null && etapa.getSecuencia() == 1) && orden.getLoteProduccion() == null) {
            String codigoLote = generarCodigoLote(orden.getProducto());
            orden.setLoteProduccion(codigoLote);

            if (orden.getProducto() == null || orden.getProducto().getTipoAnalisis() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCTO_SIN_TIPO_ANALISIS");
            }

            Long almacenPtId = catalogResolver.getAlmacenPtId();
            Long almacenCuarentenaId = catalogResolver.getAlmacenCuarentenaId();

            Almacen almacenPt = almacenRepository.findById(almacenPtId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_INEXISTENTE"));
            Almacen almacenCuarentena = almacenRepository.findById(almacenCuarentenaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_INEXISTENTE"));

            Almacen destino;
            EstadoLote estadoLote;
            TipoAnalisisCalidad tipoAnalisis = orden.getProducto().getTipoAnalisis();
            switch (tipoAnalisis) {
                case NINGUNO -> {
                    destino = almacenPt;
                    estadoLote = EstadoLote.DISPONIBLE;
                }
                case FISICO_QUIMICO, MICROBIOLOGICO, AMBOS -> {
                    destino = almacenCuarentena;
                    estadoLote = EstadoLote.EN_CUARENTENA;
                }
                default -> throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCTO_SIN_TIPO_ANALISIS");
            }

            LocalDateTime fechaFabricacion = LocalDateTime.now();
            Integer semanasVigencia = vidaUtilProductoRepository.findById(orden.getProducto().getId())
                    .map(VidaUtilProducto::getSemanasVigencia)
                    .orElse(null);
            LocalDateTime fechaVencimiento = semanasVigencia != null
                    ? fechaFabricacion.plusWeeks(semanasVigencia)
                    : null;

            LoteProducto lote = LoteProducto.builder()
                    .codigoLote(codigoLote)
                    .producto(orden.getProducto())
                    .almacen(destino)
                    .estado(estadoLote)
                    .stockLote(BigDecimal.ZERO)
                    .fechaFabricacion(fechaFabricacion)
                    .fechaVencimiento(fechaVencimiento)
                    .ordenProduccion(orden)
                    .build();

            lote = loteProductoRepository.save(lote);
            orden.setLoteId(lote.getId());
            actualizarOrden = true;
        }
        if (actualizarOrden) {
            repository.save(orden);
        }

        // Consumo etapa 1 desde reservas
        if (etapa.getSecuencia() != null && etapa.getSecuencia() == 1) {
            boolean yaConsumido = movimientoInventarioRepository
                    .existsByOrdenProduccionIdAndClasificacion(ordenId, ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
            if (!yaConsumido) {
                List<SolicitudMovimiento> reservas = solicitudMovimientoRepository
                        .findWithDetalles(ordenId, List.of(EstadoSolicitudMovimiento.RESERVADA), null, null);
                for (SolicitudMovimiento res : reservas) {
                    for (SolicitudMovimientoDetalle detRes : res.getDetalles()) {
                        MovimientoInventarioDTO movDto = new MovimientoInventarioDTO(
                                null,
                                detRes.getCantidad(),
                                TipoMovimiento.SALIDA,
                                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                                null,
                                res.getProducto().getId(),
                                detRes.getLote().getId(),
                                detRes.getAlmacenOrigen() != null ? detRes.getAlmacenOrigen().getId().intValue() : detRes.getLote().getAlmacen().getId().intValue(),
                                detRes.getAlmacenDestino() != null ? detRes.getAlmacenDestino().getId().intValue() : null,
                                null,
                                null,
                                res.getMotivoMovimiento() != null ? res.getMotivoMovimiento().getId() : null,
                                res.getTipoMovimientoDetalle() != null ? res.getTipoMovimientoDetalle().getId() : null,
                                res.getId(),
                                null,
                                ordenId,
                                null,
                                null,
                                null,
                                detRes.getLote().getEstado(),
                                null,
                                null);
                        movimientoInventarioService.registrarMovimiento(movDto);
                    }
                }
            }
        }

        etapa.setEstado(EstadoEtapa.EN_PROCESO);
        etapa.setFechaInicio(LocalDateTime.now());
        etapa.setUsuarioId(usuario.getId());
        etapa.setUsuarioNombre(usuario.getNombreCompleto());
        return etapaProduccionRepository.save(etapa);
    }

    @Transactional
    public EtapaProduccion finalizarEtapa(Long ordenId, Long etapaId, Long usuarioId) {
        OrdenProduccion orden = repository.findById(ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));
        EtapaProduccion etapa = etapaProduccionRepository.findById(etapaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ETAPA_NO_ENCONTRADA"));
        if (!etapa.getOrdenProduccion().getId().equals(ordenId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ETAPA_NO_PERTENECE_A_ORDEN");
        }
        if (etapa.getEstado() != EstadoEtapa.EN_PROCESO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ETAPA_NO_FINALIZABLE");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO"));
        etapa.setEstado(EstadoEtapa.FINALIZADA);
        etapa.setFechaFin(LocalDateTime.now());
        etapa.setUsuarioId(usuario.getId());
        etapa.setUsuarioNombre(usuario.getNombreCompleto());
        return etapaProduccionRepository.save(etapa);
    }

    public List<InsumoOPDTO> listarInsumos(Long id) {
        OrdenProduccion orden = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));
        FormulaProducto formula = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(orden.getProducto().getId().longValue(), EstadoFormula.APROBADA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FORMULA_NO_ENCONTRADA"));
        TipoMovimientoDetalle detalleSalida = tipoMovimientoDetalleRepository.findById(catalogResolver.getTipoDetalleSalidaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DETALLE_MOVIMIENTO_NO_ENCONTRADO"));
        Long detalleId = detalleSalida.getId();
        List<InsumoOPDTO> lista = new ArrayList<>();
        for (DetalleFormula det : formula.getDetalles()) {
            BigDecimal requerida = det.getCantidadNecesaria().multiply(orden.getCantidadProgramada());
            Long insumoId = det.getInsumo().getId().longValue();
            BigDecimal consumida = Optional.ofNullable(
                    movimientoInventarioRepository.sumaCantidadPorOrdenYProducto(id, insumoId, detalleId)
            ).orElse(BigDecimal.ZERO);
            BigDecimal faltante = requerida.subtract(consumida);
            if (faltante.compareTo(BigDecimal.ZERO) < 0) {
                faltante = BigDecimal.ZERO;
            }
            lista.add(new InsumoOPDTO(
                    insumoId,
                    det.getInsumo().getNombre(),
                    det.getInsumo().getUnidadMedida() != null ? det.getInsumo().getUnidadMedida().getNombre() : null,
                    requerida,
                    consumida,
                    faltante
            ));
        }
        return lista;
    }

    @Override
    public LoteProductoResponse obtenerLote(Long ordenId) {
        return repository.findById(ordenId)
                .filter(op -> op.getProducto() != null && op.getProducto().getId() != null)
                .flatMap(op -> loteProductoRepository.findByOrdenProduccionIdAndProductoId(
                        ordenId, op.getProducto().getId().longValue()))
                .map(lote -> LoteProductoResponse.builder()
                        .id(lote.getId())
                        .codigoLote(lote.getCodigoLote())
                        .fechaFabricacion(lote.getFechaFabricacion())
                        .fechaVencimiento(lote.getFechaVencimiento())
                        .estado(lote.getEstado())
                        .almacen(new AlmacenResponseDTO(lote.getAlmacen()))
                        .build())
                .orElse(null);
    }

    public Page<MovimientoInventarioResponseDTO> listarMovimientos(Long id, Pageable pageable) {
        return movimientoInventarioRepository.findByOrdenProduccionId(id, pageable)
                .map(movimientoInventarioMapper::safeToResponseDTO);
    }

    @Transactional
    public OrdenProduccion finalizar(Long id, BigDecimal cantidadProducida) {
        OrdenProduccion orden = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));

        if (orden.getEstado() == EstadoProduccion.FINALIZADA || orden.getEstado() == EstadoProduccion.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ORDEN_NO_FINALIZABLE");
        }

        if (cantidadProducida == null || cantidadProducida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_INVALIDA");
        }

        if (cantidadProducida.compareTo(orden.getCantidadProgramada()) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_EXCEDE_PROGRAMADA");
        }

        if (orden.getProducto() == null || orden.getProducto().getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ORDEN_SIN_PRODUCTO");
        }

        orden.setCantidadProducida(cantidadProducida);
        orden.setEstado(EstadoProduccion.FINALIZADA);
        orden.setFechaFin(LocalDateTime.now());

        return repository.save(orden);
    }
}
