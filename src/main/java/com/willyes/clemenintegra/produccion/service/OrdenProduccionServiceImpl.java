package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.UbicacionFisicaRepository;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.produccion.dto.InsumoFaltanteDTO;
import com.willyes.clemenintegra.produccion.dto.CrearOrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.dto.EtapaProduccionResponse;
import com.willyes.clemenintegra.produccion.dto.InsumoOPDTO;
import com.willyes.clemenintegra.produccion.dto.ProduccionTrazabilidadResponseDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.produccion.mapper.ProduccionMapper;
import com.willyes.clemenintegra.produccion.service.UnidadConversionService;
import com.willyes.clemenintegra.inventario.service.UmValidator;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.OpHomeopaticoOverride;
import com.willyes.clemenintegra.produccion.model.CierreProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OpHomeopaticoOverrideRepository;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import com.willyes.clemenintegra.produccion.service.spec.OrdenProduccionSpecifications;
import com.willyes.clemenintegra.produccion.repository.ChecklistEtapaItemRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoDetalle;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
import com.willyes.clemenintegra.inventario.dto.LoteFefoDisponibleProjection;
import com.willyes.clemenintegra.inventario.service.ReservaLoteService;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import com.willyes.clemenintegra.produccion.service.ChecklistEtapaService;
import com.willyes.clemenintegra.produccion.service.LoteConsecutivoDiaService;
import com.willyes.clemenintegra.produccion.dto.LoteProductoResponse;
import com.willyes.clemenintegra.inventario.dto.AlmacenResponseDTO;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.calidad.service.VidaUtilProductoService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.model.enums.EstadoChecklistItem;
import com.willyes.clemenintegra.produccion.validators.ProduccionEtapasLockValidator;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadRepository;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.lang.Nullable;
import org.springframework.web.ErrorResponseException;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.UUID;
import java.util.Objects;
import java.util.Comparator;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import jakarta.persistence.OptimisticLockException;
import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.requiereFisico;
import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.requiereMicro;
import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.requiereQuimico;

@Service
@RequiredArgsConstructor
public class OrdenProduccionServiceImpl implements OrdenProduccionService {

    private static final Logger log = LoggerFactory.getLogger(OrdenProduccionServiceImpl.class);

    private final FormulaProductoRepository formulaProductoRepository;
    private final ProductoRepository productoRepository;
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
    private final VidaUtilProductoService vidaUtilProductoService;
    private final ReservaLoteService reservaLoteService;
    private final ReservaLoteRepository reservaLoteRepository;
    private final DisponibilidadInsumoService disponibilidadInsumoService;
    private final ChecklistEtapaService checklistEtapaService;
    private final ChecklistEtapaItemRepository checklistEtapaItemRepository;
    private final LoteConsecutivoDiaService loteConsecutivoDiaService;
    private final OpHomeopaticoOverrideRepository opHomeopaticoOverrideRepository;
    private final ProduccionEtapasLockValidator produccionEtapasLockValidator;
    private final RegularizacionTrazabilidadRepository regularizacionTrazabilidadRepository;
    private final CosteoProduccionService costeoProduccionService;
    private final UbicacionFisicaRepository ubicacionFisicaRepository;

    private static final int SEMANAS_HOMEOPATICO = 78;
    private static final int SEMANAS_HERENCIA_PS_PT = 78;
    private static final BigDecimal CANTIDAD_MAXIMA_HOMEOPATICO = new BigDecimal("30");
    private static final int MOTIVO_OVERRIDE_MIN_LENGTH = 20;
    private static final int MOTIVO_OVERRIDE_MAX_LENGTH = 500;

    @Value("${inventory.solicitud.estados.pendientes}")
    private String estadosSolicitudPendientesConf;

    @Value("${inventory.solicitud.estados.concluyentes}")
    private String estadosSolicitudConcluyentesConf;

    @Value("${inventory.mov.clasificacion.entradaPt}")
    private String clasificacionEntradaPtConf;

    private String generarCodigoOrden() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefijo = "OP-CLEMEN-" + fecha;
        List<String> codigosDia = repository.findCodigosByPrefijo(prefijo);

        int siguienteConsecutivo = codigosDia.stream()
                .map(codigo -> codigo.substring(prefijo.length() + 1))
                .map(String::trim)
                .filter(sufijo -> !sufijo.isEmpty())
                .map(sufijo -> {
                    try {
                        return Integer.parseInt(sufijo);
                    } catch (NumberFormatException ex) {
                        return 0;
                    }
                })
                .max(Integer::compareTo)
                .orElse(0) + 1;

        return prefijo + "-" + String.format("%02d", siguienteConsecutivo);
    }

    private String generarCodigoLote(Producto producto) {
        if (producto.getCategoriaProducto() == null ||
                (producto.getCategoriaProducto().getTipo() != TipoCategoria.PRODUCTO_TERMINADO
                        && producto.getCategoriaProducto().getTipo() != TipoCategoria.PRODUCTO_SEMI_ELABORADO)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PRODUCTO_NO_TERMINADO");
        }
        String prefijo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int consecutivo = loteConsecutivoDiaService.obtenerSiguienteConsecutivo(LocalDate.now());
        String iniciales = producto.getNombre() != null
                ? producto.getNombre().replaceAll("\\s+", "").toUpperCase()
                : "";
        iniciales = iniciales.substring(0, Math.min(3, iniciales.length()));
        return prefijo + String.format("%03d", consecutivo) + "-" + iniciales;
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

    private Integer obtenerSemanasVigenciaProductoTerminado(Producto producto) {
        // Ahora aplica para productos terminados (PT) y semielaborados (PS).
        if (producto == null || producto.getCategoriaProducto() == null) {
            throw new IllegalArgumentException("Producto inválido para calcular vida útil");
        }
        TipoCategoria tipoCategoria = producto.getCategoriaProducto().getTipo();
        if (tipoCategoria != TipoCategoria.PRODUCTO_TERMINADO
                && tipoCategoria != TipoCategoria.PRODUCTO_SEMI_ELABORADO) {
            throw new IllegalArgumentException(
                    "Solo productos terminados o semielaborados pueden tener vida útil registrada."
            );
        }
        return vidaUtilProductoService.buscarPorProductoId(producto.getId())
                .map(VidaUtilProducto::getSemanasVigencia)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.VIDA_UTIL_NO_CONFIGURADA,
                        "Debe configurar la vida útil del producto antes de producirlo"));
    }

    private TipoCategoria obtenerTipoCategoriaProducto(Producto producto) {
        return Optional.ofNullable(producto)
                .map(Producto::getCategoriaProducto)
                .map(CategoriaProducto::getTipo)
                .orElse(null);
    }

    private boolean esProductoFabricable(TipoCategoria tipoCategoria) {
        return tipoCategoria == TipoCategoria.PRODUCTO_TERMINADO
                || tipoCategoria == TipoCategoria.PRODUCTO_SEMI_ELABORADO;
    }

    private void validarRendimientoProductoFabricable(Producto producto) {
        TipoCategoria tipoCategoria = obtenerTipoCategoriaProducto(producto);
        if (tipoCategoria == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "El producto de la orden de producción no tiene categoría válida configurada.");
        }
        if (!esProductoFabricable(tipoCategoria)) {
            return;
        }
        BigDecimal rendimiento = Optional.ofNullable(producto)
                .map(Producto::getRendimientoUnidad)
                .orElse(null);
        if (rendimiento == null || rendimiento.compareTo(BigDecimal.ZERO) <= 0) {
            String identificador = Optional.ofNullable(producto.getCodigoSku())
                    .orElse(Optional.ofNullable(producto.getNombre()).orElse("producto"));
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    String.format("El producto %s requiere un rendimiento por unidad definido y mayor que cero para crear una orden de producción.", identificador));
        }
    }

    private List<DetalleFormula> obtenerDetallesFormulaSeguro(FormulaProducto formula) {
        return Optional.ofNullable(formula)
                .map(FormulaProducto::getDetalles)
                .orElse(List.of());
    }

    private List<DetalleFormula> obtenerInsumosPs(List<DetalleFormula> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return List.of();
        }
        return detalles.stream()
                .filter(Objects::nonNull)
                .filter(d -> d.getInsumo() != null
                        && d.getInsumo().getCategoriaProducto() != null
                        && d.getInsumo().getCategoriaProducto().getTipo() == TipoCategoria.PRODUCTO_SEMI_ELABORADO)
                .toList();
    }

    private BigDecimal calcularCantidadRequeridaInsumo(DetalleFormula detalle, OrdenProduccion orden) {
        if (detalle == null || orden == null) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        BigDecimal cantidadNecesaria = Optional.ofNullable(detalle.getCantidadNecesaria())
                .orElse(BigDecimal.ZERO);
        BigDecimal cantidadProgramada = Optional.ofNullable(orden.getCantidadProgramada())
                .orElse(BigDecimal.ZERO);
        BigDecimal requerida = cantidadNecesaria.multiply(cantidadProgramada);
        UnidadMedida unidad = Optional.ofNullable(detalle.getUnidadMedida())
                .orElseGet(() -> detalle.getInsumo() != null ? detalle.getInsumo().getUnidadMedida() : null);
        int escala = unidad != null ? catalogResolver.decimals(unidad) : 6;
        return requerida.setScale(escala, RoundingMode.HALF_UP);
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
        int escala = catalogResolver.decimals(producto.getUnidadMedida());

        BigDecimal cantidadLote = cantidad.setScale(escala, redondeo);
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

        List<EtapaPlantilla> plantilla = cargarPlantillaEtapas(orden.getProducto().getId());
        boolean esOrdenNueva = orden.getId() == null;
        if (esOrdenNueva && (plantilla == null || plantilla.isEmpty())) {
            String codigoProducto = Optional.ofNullable(orden.getProducto().getCodigoSku())
                    .orElseGet(() -> Optional.ofNullable(orden.getProducto().getNombre()).orElse(""));
            log.warn("OP sin etapas configuradas, productoId={}, codigoProducto={}, code={}",
                    orden.getProducto().getId(), codigoProducto, "ORDEN_PRODUCTO_SIN_ETAPAS");
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "ORDEN_PRODUCTO_SIN_ETAPAS: No se puede crear la orden porque el producto no tiene etapas de producción configuradas. Configure la plantilla de etapas y vuelva a intentarlo.");
        }

        FormulaProducto formula = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(productoId, EstadoFormula.APROBADA)
                .orElseThrow(() -> new IllegalArgumentException("No existe una fórmula activa y aprobada para el producto"));

        List<InsumoFaltanteDTO> faltantes = new ArrayList<>();
        boolean stockSuficiente = true;
        Integer maxProducible = null;

        BigDecimal cantidadProgramada = orden.getCantidadProgramada();

        List<DetalleFormula> detallesFormula = obtenerDetallesFormulaSeguro(formula);

        // Cargar todos los productos de los insumos en una sola consulta para evitar N+1
        List<Long> insumoIds = detallesFormula.stream()
                .map(DetalleFormula::getInsumo)
                .filter(Objects::nonNull)
                .map(p -> p.getId().longValue())
                .toList();
        Map<Long, Producto> productosInsumo = productoRepository.findAllById(insumoIds).stream()
                .collect(Collectors.toMap(p -> p.getId().longValue(), p -> p));

        for (DetalleFormula insumo : detallesFormula) {
            if (insumo == null || insumo.getInsumo() == null) {
                continue;
            }
            Long insumoId = insumo.getInsumo().getId().longValue();
            Producto productoInsumo = productosInsumo.get(insumoId);
            if (productoInsumo == null) {
                throw new IllegalArgumentException("Insumo no encontrado: ID " + insumoId);
            }

            ModoControlInventario modoControl = Optional.ofNullable(productoInsumo.getModoControlInventario())
                    .orElse(ModoControlInventario.CONTROL_STOCK);

            BigDecimal cantidadRequerida = insumo.getCantidadNecesaria().multiply(cantidadProgramada);

            List<Long> almacenesValidos = disponibilidadInsumoService.resolverAlmacenesPreferidos(insumo.getInsumo());
            if (almacenesValidos.isEmpty()) {
                TipoCategoria tipoCategoria = Optional.ofNullable(productoInsumo.getCategoriaProducto())
                        .map(CategoriaProducto::getTipo)
                        .orElse(null);
                log.warn("No se encontraron almacenes de origen configurados para insumo {} (categoría: {})",
                        insumoId, tipoCategoria);
            }

            // ---- FEFO: preview por insumo ----
            DistribucionFefoResult distribucionPreview = disponibilidadInsumoService.calcularDisponibilidad(
                    insumoId,
                    cantidadRequerida,
                    almacenesValidos,
                    true               // modoPreview
            );

            BigDecimal stockLibreFefo = Optional.ofNullable(distribucionPreview.getStockLibreTotal())
                    .orElse(BigDecimal.ZERO);
            BigDecimal faltanteFefo = Optional.ofNullable(distribucionPreview.getFaltante())
                    .orElse(BigDecimal.ZERO);

            int producibleConEste = 0;
            if (insumo.getCantidadNecesaria().compareTo(BigDecimal.ZERO) > 0) {
                producibleConEste = stockLibreFefo
                        .divide(insumo.getCantidadNecesaria(), 0, RoundingMode.DOWN)
                        .intValue();
            }
            if (maxProducible == null || producibleConEste < maxProducible) {
                maxProducible = producibleConEste;
            }

            log.debug("OP-VALIDACION insumoId={} requerido={} stockLibreFefo={} faltanteFefo={} maxProducible={}",
                    insumoId,
                    cantidadRequerida,
                    stockLibreFefo,
                    faltanteFefo,
                    maxProducible);

            if (modoControl == ModoControlInventario.CONTROL_STOCK && !distribucionPreview.isSuficiente()) {
                stockSuficiente = false;
                faltantes.add(InsumoFaltanteDTO.builder()
                        .productoId(insumoId)
                        .nombre(productoInsumo.getNombre())
                        .requerido(cantidadRequerida)
                        .disponible(stockLibreFefo)
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
        clonarEtapasParaOrden(guardada, plantilla);

        // Reserva FEFO real (incluye PS) – aquí ya se aplican las reservas
        reservarInsumosParaOP(guardada.getId(), orden.getLotePsId());

        OrdenProduccionResponseDTO ordenResp = ProduccionMapper.toResponse(guardada);

        return ResultadoValidacionOrdenDTO.builder()
                .esValida(true)
                .mensaje("Orden de producción creada correctamente")
                .orden(ordenResp)
                .build();
    }

    @Transactional
    public ResultadoValidacionOrdenDTO crearOrden(CrearOrdenProduccionRequestDTO dto) {
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
        validarRendimientoProductoFabricable(producto);
        BigDecimal cantidadConvertida = unidadConversionService.convertir(cantidadBase, unidadBase, unidadProducto);

        Integer semanasVigencia = vidaUtilProductoService.buscarPorProductoId(producto.getId())
                .map(VidaUtilProducto::getSemanasVigencia)
                .orElse(null);
        boolean requiereOverrideHomeopatico = requiereOverrideHomeopatico(semanasVigencia, cantidadConvertida);
        if (requiereOverrideHomeopatico) {
            validarHandshakeHomeopatico(dto, producto, semanasVigencia, cantidadConvertida);
        }

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
        if (requiereOverrideHomeopatico && resultado.isEsValida() && orden.getId() != null) {
            registrarOverrideHomeopatico(orden, producto, semanasVigencia,
                    cantidadConvertida, dto.getMotivoOverrideHomeopatico());
        }
        resultado.setUnidadesProducidas(unidadesProducidas);
        if (resultado.getOrden() != null) {
            resultado.getOrden().cantidadProgramadaBase = cantidadBase;
            resultado.getOrden().unidadMedidaBaseSimbolo = unidadBase;
            resultado.getOrden().unidadMedidaSimbolo = unidadProducto;
            resultado.getOrden().unidadesProducidas = unidadesProducidas;
        }
        return resultado;
    }

    private boolean requiereOverrideHomeopatico(Integer semanasVigencia, BigDecimal cantidadSolicitada) {
        return Objects.equals(semanasVigencia, SEMANAS_HOMEOPATICO)
                && cantidadSolicitada != null
                && cantidadSolicitada.compareTo(CANTIDAD_MAXIMA_HOMEOPATICO) > 0;
    }

    private void validarHandshakeHomeopatico(CrearOrdenProduccionRequestDTO dto,
                                             Producto producto,
                                             Integer semanasVigencia,
                                             BigDecimal cantidadSolicitada) {
        if (!Boolean.TRUE.equals(dto.getConfirmacionHomeopatico())) {
            throw new CustomBusinessException(
                    ApiErrorCode.OP_HOMEOPATICO_REQUIERE_CONFIRMACION,
                    "La cantidad solicitada supera el máximo recomendado para producto homeopático. Confirma para continuar.",
                    Map.of(
                            "productoId", producto.getId(),
                            "semanasVigencia", semanasVigencia,
                            "cantidadSolicitada", cantidadSolicitada,
                            "maxRecomendado", CANTIDAD_MAXIMA_HOMEOPATICO
                    )
            );
        }
        String motivo = dto.getMotivoOverrideHomeopatico();
        String motivoNormalizado = motivo != null ? motivo.trim() : null;
        if (motivoNormalizado == null || motivoNormalizado.length() < MOTIVO_OVERRIDE_MIN_LENGTH) {
            throw new CustomBusinessException(
                    ApiErrorCode.OP_HOMEOPATICO_MOTIVO_OBLIGATORIO,
                    "Debe ingresar una justificación de al menos 20 caracteres para continuar con la OP homeopática.",
                    Map.of(
                            "productoId", producto.getId(),
                            "semanasVigencia", semanasVigencia,
                            "cantidadSolicitada", cantidadSolicitada,
                            "maxRecomendado", CANTIDAD_MAXIMA_HOMEOPATICO,
                            "minCaracteresMotivo", MOTIVO_OVERRIDE_MIN_LENGTH
                    )
            );
        }
        if (motivoNormalizado.length() > MOTIVO_OVERRIDE_MAX_LENGTH) {
            throw new CustomBusinessException(
                    ApiErrorCode.SOLICITUD_INVALIDA,
                    "motivoOverrideHomeopatico no debe superar 500 caracteres",
                    Map.of("maxCaracteresMotivo", MOTIVO_OVERRIDE_MAX_LENGTH)
            );
        }
    }

    private void registrarOverrideHomeopatico(OrdenProduccion orden,
                                              Producto producto,
                                              Integer semanasVigencia,
                                              BigDecimal cantidadSolicitada,
                                              String motivo) {
        String motivoNormalizado = motivo != null ? motivo.trim() : null;

        orden.setConfirmacionHomeopatico(Boolean.TRUE);
        orden.setMotivoOverrideHomeopatico(motivoNormalizado);

        Usuario usuarioActual = null;
        try {
            usuarioActual = usuarioService.obtenerUsuarioAutenticado();
        } catch (RuntimeException ex) {
            log.warn("No fue posible resolver usuario autenticado para auditoría override homeopático", ex);
        }

        OpHomeopaticoOverride auditoria = OpHomeopaticoOverride.builder()
                .ordenProduccion(orden)
                .producto(producto)
                .semanasVigencia(semanasVigencia)
                .cantidadSolicitada(cantidadSolicitada)
                .motivo(motivoNormalizado)
                .usuario(usuarioActual)
                .build();

        opHomeopaticoOverrideRepository.save(auditoria);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<OrdenProduccionResponseDTO> listarPaginado(String codigo,
                                                           String producto,
                                                           EstadoProduccion estado,
                                                           String responsable,
                                                           LocalDateTime fechaInicio,
                                                           LocalDateTime fechaFin,
                                                           Pageable pageable) {
        Specification<OrdenProduccion> spec = OrdenProduccionSpecifications.and(
                OrdenProduccionSpecifications.byCodigo(codigo),
                OrdenProduccionSpecifications.byProducto(producto),
                OrdenProduccionSpecifications.byEstado(estado),
                OrdenProduccionSpecifications.byResponsable(responsable),
                OrdenProduccionSpecifications.byFechaBetween(fechaInicio, fechaFin)
        );
        return repository.findAll(spec, pageable).map(ProduccionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenProduccion> listar(String codigo,
                                        String producto,
                                        EstadoProduccion estado,
                                        String responsable,
                                        LocalDateTime fechaInicio,
                                        LocalDateTime fechaFin) {
        Specification<OrdenProduccion> spec = OrdenProduccionSpecifications.and(
                OrdenProduccionSpecifications.byCodigo(codigo),
                OrdenProduccionSpecifications.byProducto(producto),
                OrdenProduccionSpecifications.byEstado(estado),
                OrdenProduccionSpecifications.byResponsable(responsable),
                OrdenProduccionSpecifications.byFechaBetween(fechaInicio, fechaFin)
        );
        return repository.findAll(spec, Sort.by(Sort.Direction.DESC, "fechaInicio"));
    }

    public List<OrdenProduccion> listarTodas() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<OrdenProduccion> buscarPorId(Long id) {
        return repository.findByIdWithProductoCategoria(id).map(this::adjuntarFechaVencimientoLotePt);
    }

    private OrdenProduccion adjuntarFechaVencimientoLotePt(OrdenProduccion orden) {
        if (orden == null || orden.getProducto() == null || orden.getProducto().getId() == null) {
            orden.setFechaVencimientoLotePt(null);
            return orden;
        }

        Optional<LoteProducto> lotePt = loteProductoRepository
                .findByOrdenProduccionIdAndProductoId(orden.getId(), orden.getProducto().getId().longValue());

        orden.setFechaVencimientoLotePt(lotePt.map(LoteProducto::getFechaVencimiento).orElse(null));
        return orden;
    }

    private Long resolverEtapaPrincipal(Long ordenProduccionId) {
        return etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(ordenProduccionId)
                .stream()
                .findFirst()
                .map(EtapaProduccion::getId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "ETAPA_PRODUCCION_NO_CONFIGURADA"));
    }

    private EtapaProduccion seleccionarEtapaParaConsumo(List<EtapaProduccion> etapas, Long ordenProduccionId) {
        List<EtapaProduccion> existentes = Optional.ofNullable(etapas)
                .orElseGet(() -> etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(ordenProduccionId));
        Optional<EtapaProduccion> activa = existentes.stream()
                .filter(e -> e.getFechaInicio() != null && e.getFechaFin() == null)
                .findFirst();
        if (activa.isPresent()) {
            return activa.get();
        }
        return existentes.stream()
                .filter(e -> e.getFechaInicio() != null
                        && (e.getFechaFin() != null || e.getEstado() == EstadoEtapa.FINALIZADA))
                .max(Comparator
                        .comparing(EtapaProduccion::getFechaFin, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(EtapaProduccion::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "OP_SIN_ETAPA_ACTIVA"));
    }

    private EtapaProduccion obtenerUltimaEtapaFinalizada(List<EtapaProduccion> etapas, Long ordenProduccionId) {
        List<EtapaProduccion> existentes = Optional.ofNullable(etapas)
                .orElseGet(() -> etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(ordenProduccionId));
        return existentes.stream()
                .filter(e -> e.getFechaInicio() != null
                        && (e.getFechaFin() != null || e.getEstado() == EstadoEtapa.FINALIZADA))
                .max(Comparator
                        .comparing(EtapaProduccion::getFechaFin, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(EtapaProduccion::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private Long resolverTipoDetalleSalidaProduccionId() {
        Long id = catalogResolver.getTipoDetalleSalidaProduccionId();
        if (id == null) {
            id = catalogResolver.getTipoDetalleSalidaId();
        }
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TIPO_DETALLE_SALIDA_PRODUCCION_INEXISTENTE");
        }
        return id;
    }

    private BigDecimal safeCantidad(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private BigDecimal calcularCantidadRequeridaInsumo(DetalleFormula detalle, BigDecimal cantidadBase) {
        if (detalle == null) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        BigDecimal cantidadNecesaria = Optional.ofNullable(detalle.getCantidadNecesaria())
                .orElse(BigDecimal.ZERO);
        BigDecimal base = Optional.ofNullable(cantidadBase).orElse(BigDecimal.ZERO);
        BigDecimal requerida = cantidadNecesaria.multiply(base);
        UnidadMedida unidad = Optional.ofNullable(detalle.getUnidadMedida())
                .orElseGet(() -> detalle.getInsumo() != null ? detalle.getInsumo().getUnidadMedida() : null);
        int escala = unidad != null ? catalogResolver.decimals(unidad) : 6;
        return requerida.setScale(escala, RoundingMode.HALF_UP);
    }

    private void registrarConsumoDeInsumos(Long ordenProduccionId, Long etapaId, Long usuarioId) {
        boolean consumoRegistrado = !movimientoInventarioRepository
                .findByOrdenProduccionIdAndOrdenProduccionEtapaIdAndClasificacionOrderByFechaIngresoAsc(
                        ordenProduccionId,
                        etapaId,
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION
                ).isEmpty();
        if (consumoRegistrado) {
            log.debug("OP-consumo ya registrado, omitiendo duplicados op={}, etapa={}", ordenProduccionId, etapaId);
            return;
        }
        movimientoInventarioService.consumirInsumosPorOrden(ordenProduccionId, etapaId, usuarioId);
    }

    private void registrarConsumoRealPorCierreTotal(OrdenProduccion orden,
                                                    List<EtapaProduccion> etapas,
                                                    EtapaProduccion etapaConsumo,
                                                    Usuario usuario,
                                                    String traceId) {
        if (orden == null || orden.getId() == null || orden.getProducto() == null) {
            return;
        }
        Long productoFabricadoId = Optional.ofNullable(orden.getProducto())
                .map(Producto::getId)
                .map(Integer::longValue)
                .orElse(null);
        FormulaProducto formula = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(orden.getProducto().getId().longValue(), EstadoFormula.APROBADA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FORMULA_NO_ENCONTRADA"));

        Long detalleSalidaId = resolverTipoDetalleSalidaProduccionId();
        TipoMovimientoDetalle detalleSalida = tipoMovimientoDetalleRepository.findById(detalleSalidaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TIPO_DETALLE_SALIDA_PRODUCCION_INEXISTENTE"));

        Long motivoSalidaId = catalogResolver.getMotivoSalidaProduccionId();
        if (motivoSalidaId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MOTIVO_SALIDA_PRODUCCION_INEXISTENTE");
        }
        MotivoMovimiento motivoSalida = motivoMovimientoRepository.findById(motivoSalidaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MOTIVO_SALIDA_PRODUCCION_INEXISTENTE"));

        Long preBodegaId = catalogResolver.getAlmacenPreBodegaProduccionId();
        if (preBodegaId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_PRE_BODEGA_INEXISTENTE");
        }

        Map<Long, BigDecimal> requeridoPorProducto = new LinkedHashMap<>();
        for (DetalleFormula det : obtenerDetallesFormulaSeguro(formula)) {
            if (det == null || det.getInsumo() == null || det.getInsumo().getId() == null) {
                continue;
            }
            Long productoId = det.getInsumo().getId().longValue();
            if (productoFabricadoId != null && Objects.equals(productoId, productoFabricadoId)) {
                log.debug("OP-cierre total omitiendo producto fabricado como insumo op={}, productoId={}",
                        orden.getId(), productoFabricadoId);
                continue;
            }
            BigDecimal requerido = calcularCantidadRequeridaInsumo(det, orden);
            if (requerido.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            requeridoPorProducto.merge(productoId, requerido, BigDecimal::add);
        }
        if (requeridoPorProducto.isEmpty()) {
            log.debug("OP-cierre total sin insumos calculados op={}", orden.getId());
            return;
        }

        List<MovimientoInventario> alistados = movimientoInventarioRepository
                .findByOrdenProduccionIdAndClasificacion(
                        orden.getId(),
                        ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                        Pageable.unpaged()
                ).getContent();
        List<MovimientoInventario> consumosPrevios = movimientoInventarioRepository
                .findByOrdenProduccionIdAndClasificacion(
                        orden.getId(),
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                        Pageable.unpaged()
                ).getContent();

        Map<Long, BigDecimal> disponiblePorLote = new LinkedHashMap<>();
        Map<Long, Long> productoPorLote = new HashMap<>();
        Map<Long, LocalDateTime> fechaPorLote = new HashMap<>();

        for (MovimientoInventario mov : alistados) {
            if (mov == null || mov.getLote() == null || mov.getProducto() == null) {
                continue;
            }
            if (productoFabricadoId != null
                    && Objects.equals(mov.getProducto().getId().longValue(), productoFabricadoId)) {
                log.debug("OP-cierre total omitió movimiento del producto fabricado op={}, loteId={}",
                        orden.getId(), mov.getLote().getId());
                continue;
            }
            if (mov.getTipoMovimiento() != TipoMovimiento.TRANSFERENCIA) {
                continue;
            }
            if (mov.getAlmacenDestino() != null
                    && !Objects.equals(Long.valueOf(mov.getAlmacenDestino().getId().longValue()), preBodegaId)) {
                continue;
            }
            LoteProducto lotePreBodega = resolverLotePreBodegaParaConsumo(mov, preBodegaId);
            if (lotePreBodega == null || lotePreBodega.getId() == null) {
                log.warn("OP-cierre total sin lote en pre-bodega op={}, loteId={}, codigoLote={}, productoId={}",
                        orden.getId(),
                        mov.getLote().getId(),
                        mov.getLote().getCodigoLote(),
                        mov.getProducto().getId());
                continue;
            }
            Long loteId = lotePreBodega.getId();
            if (loteId == null) {
                continue;
            }
            BigDecimal cantidad = safeCantidad(mov.getCantidad());
            disponiblePorLote.merge(loteId, cantidad, BigDecimal::add);
            Long productoId = mov.getProducto().getId() != null
                    ? mov.getProducto().getId().longValue()
                    : (lotePreBodega.getProducto() != null ? lotePreBodega.getProducto().getId().longValue() : null);
            if (productoId != null) {
                productoPorLote.put(loteId, productoId);
            }
            if (mov.getFechaIngreso() != null) {
                fechaPorLote.merge(loteId, mov.getFechaIngreso(), (previa, nueva) -> {
                    if (previa == null) return nueva;
                    return previa.isBefore(nueva) ? previa : nueva;
                });
            }
        }

        for (MovimientoInventario mov : consumosPrevios) {
            if (mov == null || mov.getLote() == null) {
                continue;
            }
            if (productoFabricadoId != null
                    && mov.getProducto() != null
                    && Objects.equals(mov.getProducto().getId().longValue(), productoFabricadoId)) {
                continue;
            }
            if (mov.getTipoMovimiento() != TipoMovimiento.SALIDA) {
                continue;
            }
            if (mov.getTipoMovimientoDetalle() == null
                    || !Objects.equals(mov.getTipoMovimientoDetalle().getId(), detalleSalidaId)) {
                continue;
            }
            Long loteId = mov.getLote().getId();
            BigDecimal saldoActual = disponiblePorLote.get(loteId);
            if (saldoActual != null) {
                BigDecimal nuevoSaldo = saldoActual.subtract(safeCantidad(mov.getCantidad()));
                if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
                    nuevoSaldo = BigDecimal.ZERO;
                }
                disponiblePorLote.put(loteId, nuevoSaldo);
            }
        }

        Long etapaDestinoId = Optional.ofNullable(etapaConsumo)
                .map(EtapaProduccion::getId)
                .orElseGet(() -> Optional.ofNullable(obtenerUltimaEtapaFinalizada(etapas, orden.getId()))
                        .map(EtapaProduccion::getId)
                        .orElse(null));

        for (Map.Entry<Long, BigDecimal> entry : requeridoPorProducto.entrySet()) {
            Long productoId = entry.getKey();
            BigDecimal requerido = entry.getValue();
            BigDecimal consumido = Optional.ofNullable(
                    movimientoInventarioRepository.sumaCantidadPorOrdenProductoTipoDetalle(
                            orden.getId(),
                            productoId,
                            TipoMovimiento.SALIDA,
                            detalleSalidaId
                    )
            ).orElse(BigDecimal.ZERO);

            BigDecimal faltante = requerido.subtract(consumido);
            if (faltante.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            List<Long> lotesProducto = disponiblePorLote.entrySet().stream()
                    .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                    .filter(e -> Objects.equals(productoPorLote.get(e.getKey()), productoId))
                    .sorted(Comparator.comparing(
                                    (Map.Entry<Long, BigDecimal> e) -> fechaPorLote.getOrDefault(e.getKey(), LocalDateTime.MIN))
                            .thenComparing(Map.Entry::getKey))
                    .map(Map.Entry::getKey)
                    .toList();

            BigDecimal pendiente = faltante;
            for (Long loteId : lotesProducto) {
                BigDecimal disponible = disponiblePorLote.getOrDefault(loteId, BigDecimal.ZERO);
                if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal consumir = pendiente.min(disponible).setScale(6, RoundingMode.HALF_UP);
                if (consumir.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                MovimientoInventarioDTO salida = new MovimientoInventarioDTO(
                        null,
                        consumir,
                        TipoMovimiento.SALIDA,
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                        orden.getCodigoOrden(),
                        null,
                        null,
                        null,
                        null,
                        productoId.intValue(),
                        loteId,
                        preBodegaId.intValue(),
                        null,
                        null,
                        null,
                        motivoSalida.getId(),
                        detalleSalida.getId(),
                        null,
                        usuario.getId(),
                        orden.getId(),
                        etapaDestinoId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );
                log.info("OP-cierre total movDTO traceId={} opId={} etapaId={} productoId={} loteId={} tipo={} clasificacion={} almacenOrigenId={} almacenDestinoId={} tipoDetalleId={} motivoId={} cantidad={}",
                        traceId,
                        orden.getId(),
                        etapaDestinoId,
                        productoId,
                        loteId,
                        salida.tipoMovimiento(),
                        salida.clasificacionMovimientoInventario(),
                        salida.almacenOrigenId(),
                        salida.almacenDestinoId(),
                        salida.tipoMovimientoDetalleId(),
                        salida.motivoMovimientoId(),
                        salida.cantidad());
                movimientoInventarioService.registrarMovimiento(salida);

                pendiente = pendiente.subtract(consumir);
                disponiblePorLote.put(loteId, disponible.subtract(consumir));
                if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
            }

            if (pendiente.compareTo(BigDecimal.ZERO) > 0) {
                log.warn("OP-cierre total con alistado insuficiente para consumo real op={}, productoId={}, faltante={}",
                        orden.getId(), productoId, pendiente);
            }
        }
    }

    private void registrarDevolucionInsumosPorCierreParcial(OrdenProduccion orden,
                                                             BigDecimal cantidadRealAcumulada,
                                                             EtapaProduccion etapaConsumo,
                                                             Usuario usuario,
                                                             String traceId) {
        if (orden == null || orden.getId() == null || orden.getProducto() == null) {
            return;
        }

        FormulaProducto formula = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(orden.getProducto().getId().longValue(), EstadoFormula.APROBADA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FORMULA_NO_ENCONTRADA"));

        Long preBodegaId = catalogResolver.getAlmacenPreBodegaProduccionId();
        if (preBodegaId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_PRE_BODEGA_INEXISTENTE");
        }
        Long motivoDevId = catalogResolver.getMotivoIdDevolucionDesdeProduccion();
        if (motivoDevId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "MOTIVO_DEVOLUCION_DESDE_PRODUCCION_INEXISTENTE");
        }
        MotivoMovimiento motivoDevolucion = motivoMovimientoRepository.findById(motivoDevId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "MOTIVO_DEVOLUCION_DESDE_PRODUCCION_INEXISTENTE"));

        Map<Long, BigDecimal> consumoPlanPorProducto = new LinkedHashMap<>();
        Map<Long, BigDecimal> consumoRealPorProducto = new LinkedHashMap<>();

        for (DetalleFormula det : obtenerDetallesFormulaSeguro(formula)) {
            if (det == null || det.getInsumo() == null || det.getInsumo().getId() == null) {
                continue;
            }
            Long productoId = det.getInsumo().getId().longValue();
            consumoPlanPorProducto.merge(productoId,
                    calcularCantidadRequeridaInsumo(det, orden.getCantidadProgramada()),
                    BigDecimal::add);
            consumoRealPorProducto.merge(productoId,
                    calcularCantidadRequeridaInsumo(det, cantidadRealAcumulada),
                    BigDecimal::add);
        }

        if (consumoPlanPorProducto.isEmpty()) {
            return;
        }

        List<MovimientoInventario> consumos = movimientoInventarioRepository
                .findByOrdenProduccionIdAndClasificacion(
                        orden.getId(),
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                        Pageable.unpaged())
                .getContent();
        List<MovimientoInventario> devoluciones = movimientoInventarioRepository
                .findByOrdenProduccionIdAndClasificacion(
                        orden.getId(),
                        ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION,
                        Pageable.unpaged())
                .getContent();
        List<MovimientoInventario> traslados = movimientoInventarioRepository
                .findByOrdenProduccionIdAndClasificacion(
                        orden.getId(),
                        ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                        Pageable.unpaged())
                .getContent();

        Map<Long, BigDecimal> consumidoPorLote = new LinkedHashMap<>();
        Map<Long, BigDecimal> devueltoPorLote = new LinkedHashMap<>();
        Map<Long, Long> productoPorLote = new HashMap<>();
        Map<Long, LocalDateTime> fechaConsumoPorLote = new HashMap<>();
        Map<Long, Almacen> destinoAbastecedorPorLote = new HashMap<>();

        for (MovimientoInventario mov : traslados) {
            if (mov == null || mov.getLote() == null || mov.getLote().getId() == null
                    || mov.getProducto() == null || mov.getProducto().getId() == null) {
                continue;
            }
            LoteProducto lotePreBodega = resolverLotePreBodegaParaConsumo(mov, preBodegaId);
            if (lotePreBodega == null || lotePreBodega.getId() == null) {
                continue;
            }
            if (mov.getAlmacenOrigen() != null && mov.getAlmacenOrigen().getId() != null
                    && !Objects.equals(mov.getAlmacenOrigen().getId().longValue(), preBodegaId)) {
                destinoAbastecedorPorLote.putIfAbsent(lotePreBodega.getId(), mov.getAlmacenOrigen());
            }
            productoPorLote.putIfAbsent(lotePreBodega.getId(), mov.getProducto().getId().longValue());
        }

        for (MovimientoInventario mov : consumos) {
            if (mov == null || mov.getLote() == null || mov.getLote().getId() == null
                    || mov.getProducto() == null || mov.getProducto().getId() == null) {
                continue;
            }
            Long loteId = mov.getLote().getId();
            consumidoPorLote.merge(loteId, safeCantidad(mov.getCantidad()), BigDecimal::add);
            productoPorLote.putIfAbsent(loteId, mov.getProducto().getId().longValue());
            if (mov.getFechaIngreso() != null) {
                fechaConsumoPorLote.merge(loteId, mov.getFechaIngreso(),
                        (a, b) -> a.isBefore(b) ? a : b);
            }
        }
        for (MovimientoInventario mov : devoluciones) {
            if (mov == null || mov.getLote() == null || mov.getLote().getId() == null) {
                continue;
            }
            devueltoPorLote.merge(mov.getLote().getId(), safeCantidad(mov.getCantidad()), BigDecimal::add);
        }

        for (Map.Entry<Long, BigDecimal> entry : consumoPlanPorProducto.entrySet()) {
            Long productoId = entry.getKey();
            BigDecimal consumoPlan = entry.getValue();
            BigDecimal consumoReal = consumoRealPorProducto.getOrDefault(productoId, BigDecimal.ZERO);
            BigDecimal delta = consumoPlan.subtract(consumoReal).setScale(6, RoundingMode.HALF_UP);
            if (delta.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal pendiente = delta;
            List<Long> lotesProducto = consumidoPorLote.entrySet().stream()
                    .filter(e -> Objects.equals(productoPorLote.get(e.getKey()), productoId))
                    .sorted(Comparator.comparing(
                                    (Map.Entry<Long, BigDecimal> e) -> fechaConsumoPorLote.getOrDefault(e.getKey(), LocalDateTime.MIN))
                            .thenComparing(Map.Entry::getKey))
                    .map(Map.Entry::getKey)
                    .toList();

            for (Long loteId : lotesProducto) {
                BigDecimal consumido = consumidoPorLote.getOrDefault(loteId, BigDecimal.ZERO);
                BigDecimal devuelto = devueltoPorLote.getOrDefault(loteId, BigDecimal.ZERO);
                BigDecimal saldo = consumido.subtract(devuelto);
                if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal devolver = pendiente.min(saldo).setScale(6, RoundingMode.HALF_UP);
                if (devolver.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                Almacen destino = destinoAbastecedorPorLote.get(loteId);
                if (destino == null || destino.getId() == null) {
                    log.warn("OP-cierre parcial sin almacén abastecedor para devolución op={}, productoId={}, loteId={}",
                            orden.getId(), productoId, loteId);
                    continue;
                }

                String codigoLote = loteProductoRepository.findById(loteId)
                        .map(LoteProducto::getCodigoLote)
                        .orElse(null);
                boolean existeLoteDestino = codigoLote != null
                        && loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                                codigoLote,
                                productoId.intValue(),
                                destino.getId())
                        .isPresent();
                if (!existeLoteDestino) {
                    log.warn("OP-cierre parcial omite devolución para evitar crear lote op={}, productoId={}, loteId={}, destinoAlmacenId={}",
                            orden.getId(), productoId, loteId, destino.getId());
                    continue;
                }

                MovimientoInventarioDTO devolucion = new MovimientoInventarioDTO(
                        null,
                        devolver,
                        TipoMovimiento.DEVOLUCION,
                        ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION,
                        orden.getCodigoOrden(),
                        null,
                        null,
                        null,
                        null,
                        productoId.intValue(),
                        loteId,
                        preBodegaId.intValue(),
                        destino.getId(),
                        null,
                        null,
                        motivoDevolucion.getId(),
                        null,
                        null,
                        usuario.getId(),
                        orden.getId(),
                        Optional.ofNullable(etapaConsumo).map(EtapaProduccion::getId).orElse(null),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );
                log.info("OP-cierre parcial devolución DTO traceId={} opId={} etapaId={} productoId={} loteId={} tipo={} clasificacion={} almacenOrigenId={} almacenDestinoId={} motivoId={} cantidad={}",
                        traceId,
                        orden.getId(),
                        devolucion.ordenProduccionEtapaId(),
                        productoId,
                        loteId,
                        devolucion.tipoMovimiento(),
                        devolucion.clasificacionMovimientoInventario(),
                        devolucion.almacenOrigenId(),
                        devolucion.almacenDestinoId(),
                        devolucion.motivoMovimientoId(),
                        devolucion.cantidad());
                movimientoInventarioService.registrarMovimiento(devolucion);

                pendiente = pendiente.subtract(devolver);
                devueltoPorLote.put(loteId, devuelto.add(devolver));
                if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
            }

            if (pendiente.compareTo(BigDecimal.ZERO) > 0) {
                log.warn("OP-cierre parcial quedó delta sin devolver op={}, productoId={}, deltaPendiente={}",
                        orden.getId(), productoId, pendiente);
            }
        }
    }

    private LoteProducto resolverLotePreBodegaParaConsumo(MovimientoInventario movimiento, Long preBodegaId) {
        if (movimiento == null) {
            return null;
        }
        LoteProducto lote = movimiento.getLote();
        if (lote == null) {
            return null;
        }
        if (preBodegaId == null) {
            return lote;
        }
        Long almacenId = lote.getAlmacen() != null && lote.getAlmacen().getId() != null
                ? lote.getAlmacen().getId().longValue()
                : null;
        if (Objects.equals(almacenId, preBodegaId)) {
            return lote;
        }
        String codigoLote = lote.getCodigoLote();
        Integer productoId = movimiento.getProducto() != null
                ? movimiento.getProducto().getId()
                : (lote.getProducto() != null ? lote.getProducto().getId() : null);
        if (productoId == null || codigoLote == null || codigoLote.isBlank()) {
            return null;
        }
        return loteProductoRepository
                .findByCodigoLoteAndProductoIdAndAlmacenId(codigoLote, productoId, preBodegaId.intValue())
                .orElse(null);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    /*
     * Auditoría rápida (opcional, para ejecución manual) para seguir movimientos de la OP:
     * 1) Movimientos del producto fabricado (818) por orden:
     *    SELECT mi.id, mi.tipo_mov, mi.cantidad, mi.almacen_origen_id, mi.almacen_destino_id, mi.productos_id,
     *           lp.id lote_id, lp.codigo_lote, lp.almacenes_id lote_almacen_actual
     *    FROM movimientos_inventario mi
     *    JOIN lotes_productos lp ON lp.id = mi.lotes_productos_id
     *    WHERE mi.orden_produccion_id = :opId AND mi.productos_id = 818
     *    ORDER BY mi.id DESC;
     * 2) Todas las SALIDAS por orden:
     *    SELECT mi.id, mi.tipo_mov, mi.cantidad, mi.almacen_origen_id, mi.almacen_destino_id, mi.productos_id,
     *           mi.tipos_movimiento_detalle_id, mi.clasificacion,
     *           lp.id lote_id, lp.codigo_lote, lp.almacenes_id lote_almacen_actual
     *    FROM movimientos_inventario mi
     *    JOIN lotes_productos lp ON lp.id = mi.lotes_productos_id
     *    WHERE mi.orden_produccion_id = :opId AND mi.tipo_mov='SALIDA'
     *    ORDER BY mi.id DESC;
     */
    @Transactional
    public OrdenProduccion registrarCierre(Long id, CierreProduccionRequestDTO dto) {
        String traceId = UUID.randomUUID().toString();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("opTraceId", traceId)) {
            OrdenProduccion orden = repository.findByIdForUpdate(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));

            if (orden.getEstado() == EstadoProduccion.CANCELADA ||
                    orden.getEstado() == EstadoProduccion.CERRADA_INCOMPLETA) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ORDEN_NO_CERRABLE");
            }

            ClasificacionMovimientoInventario clasifEntrada;
            try {
                clasifEntrada = ClasificacionMovimientoInventario.valueOf(clasificacionEntradaPtConf);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CLASIFICACION_ENTRADA_PT_INVALIDA");
            }

            if (orden.getEstado() == EstadoProduccion.FINALIZADA) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "OP_YA_FINALIZADA");
            }

            if (dto.getCantidad() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_INVALIDA");
            }

            Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
            BigDecimal cantidad = validarCantidad(dto.getCantidad(), orden.getProducto());
            dto.setCantidad(cantidad);
            log.info("OP-cierre request traceId={} opId={} tipo={} cantidad={} usuarioId={}", traceId, orden.getId(),
                    dto.getTipo(), cantidad, usuario.getId());

            List<EtapaProduccion> etapas = etapaProduccionRepository
                    .findByOrdenProduccionIdOrderBySecuenciaAsc(orden.getId());
            boolean algunaEtapaIniciada = etapas.stream().anyMatch(e -> e.getFechaInicio() != null);
            boolean todasFinalizadas = !etapas.isEmpty()
                    && etapas.stream().allMatch(e -> e.getFechaFin() != null || e.getEstado() == EstadoEtapa.FINALIZADA);

            boolean permitirCierreSinActiva = dto.getTipo() == TipoCierre.TOTAL
                    && orden.getEstado() == EstadoProduccion.EN_PROCESO
                    && todasFinalizadas;

            if (!algunaEtapaIniciada && !permitirCierreSinActiva) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "OP_SIN_ETAPA_ACTIVA");
            }
            if (permitirCierreSinActiva) {
                log.debug("OP-cierre total sin etapa activa permitida op={}", orden.getId());
            }

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
                Integer semanasVigencia = obtenerSemanasVigenciaProductoTerminado(orden.getProducto());
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

            BigDecimal cantidadProgramada = Optional.ofNullable(orden.getCantidadProgramada()).orElse(BigDecimal.ZERO);
            BigDecimal acumuladoActual = Optional.ofNullable(orden.getCantidadProducidaAcumulada()).orElse(BigDecimal.ZERO);
            BigDecimal acumuladoPropuesto = acumuladoActual.add(cantidad);

            if (dto.getTipo() == TipoCierre.PARCIAL) {
                if (acumuladoPropuesto.compareTo(cantidadProgramada) >= 0) {
                    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
                    problem.setTitle("Regla de cierre de Orden de Producción");
                    problem.setDetail("El cierre parcial no puede completar o exceder la cantidad programada.");
                    problem.setProperty("code", "CIERRE_PARCIAL_SUPERA_PROGRAMADA");
                    problem.setProperty("cantidadProgramada", cantidadProgramada);
                    problem.setProperty("acumuladoActual", acumuladoActual);
                    problem.setProperty("cantidadSolicitada", cantidad);
                    problem.setProperty("acumuladoPropuesto", acumuladoPropuesto);
                    throw new ErrorResponseException(HttpStatus.UNPROCESSABLE_ENTITY, problem, null);
                }
            } else if (dto.getTipo() == TipoCierre.TOTAL
                    && acumuladoPropuesto.compareTo(cantidadProgramada) != 0) {
                boolean existeRegularizacion = regularizacionTrazabilidadRepository.existsByOrdenProduccionId(orden.getId());
                if (!existeRegularizacion) {
                    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
                    problem.setTitle("Regla de cierre de Orden de Producción");
                    problem.setDetail("El cierre total requiere regularización cuando no coincide con la cantidad programada.");
                    problem.setProperty("code", "CIERRE_TOTAL_NO_COINCIDE_PROGRAMADA");
                    problem.setProperty("cantidadProgramada", cantidadProgramada);
                    problem.setProperty("acumuladoActual", acumuladoActual);
                    problem.setProperty("cantidadSolicitada", cantidad);
                    problem.setProperty("acumuladoPropuesto", acumuladoPropuesto);
                    problem.setProperty("diferencia", acumuladoPropuesto.subtract(cantidadProgramada));
                    throw new ErrorResponseException(HttpStatus.UNPROCESSABLE_ENTITY, problem, null);
                }
            }
            boolean cierreDefinitivo = esCierreDefinitivo(dto);
            EstadoProduccion estadoObjetivo = calcularEstadoObjetivo(orden, acumuladoPropuesto, cierreDefinitivo);

            // Esta validación actúa por cierre individual; la regla global para evitar cerrar la OP sin cierres
            // se aplica en recalcularEstadoOrden cuando todas las etapas terminan.
            if ((estadoObjetivo == EstadoProduccion.FINALIZADA || estadoObjetivo == EstadoProduccion.CERRADA_INCOMPLETA)
                    && acumuladoPropuesto.compareTo(BigDecimal.ZERO) == 0) {
                ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
                problem.setTitle("Regla de cierre de Orden de Producción");
                problem.setDetail("La Orden de Producción no puede cerrarse porque no tiene ingresos registrados.");
                problem.setProperty("code", "OP_SIN_INGRESOS");
                problem.setProperty("idOrden", orden.getId());
                problem.setProperty("cantidadProgramada", cantidadProgramada);
                problem.setProperty("cantidadFabricadaTotal", acumuladoPropuesto);
                throw new ErrorResponseException(HttpStatus.CONFLICT, problem, null);
            }

            if (estadoObjetivo == EstadoProduccion.CERRADA_INCOMPLETA
                    && acumuladoPropuesto.compareTo(BigDecimal.ZERO) > 0
                    && acumuladoPropuesto.compareTo(cantidadProgramada) < 0
                    && !Boolean.TRUE.equals(dto.getConfirmarCierreParcial())) {
                BigDecimal porcentajeCumplimiento = calcularPorcentajeCumplimiento(acumuladoPropuesto, cantidadProgramada);
                BigDecimal cantidadFaltante = cantidadProgramada.subtract(acumuladoPropuesto);
                if (cantidadFaltante.compareTo(BigDecimal.ZERO) < 0) {
                    cantidadFaltante = BigDecimal.ZERO;
                }

                ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
                problem.setTitle("Regla de cierre de Orden de Producción");
                problem.setDetail("Se requiere confirmación para cierre parcial de la Orden de Producción.");
                problem.setProperty("code", "OP_CIERRE_PARCIAL_REQUIERE_CONFIRMACION");
                problem.setProperty("cantidadProgramada", cantidadProgramada);
                problem.setProperty("cantidadFabricadaTotal", acumuladoPropuesto);
                problem.setProperty("porcentajeCumplimiento", porcentajeCumplimiento);
                problem.setProperty("cantidadFaltante", cantidadFaltante);
                throw new ErrorResponseException(HttpStatus.CONFLICT, problem, null);
            }

            EtapaProduccion etapaConsumo = seleccionarEtapaParaConsumo(etapas, orden.getId());
            registrarConsumoDeInsumos(orden.getId(), etapaConsumo.getId(), usuario.getId());
            boolean reconciliarParcial = dto.getTipo() == TipoCierre.PARCIAL
                    || acumuladoPropuesto.compareTo(cantidadProgramada) < 0;
            if (reconciliarParcial) {
                registrarDevolucionInsumosPorCierreParcial(orden, acumuladoPropuesto, etapaConsumo, usuario, traceId);
            }
            if (dto.getTipo() == TipoCierre.TOTAL) {
                registrarConsumoRealPorCierreTotal(orden, etapas, etapaConsumo, usuario, traceId);
            }

            List<EstadoSolicitudMovimiento> estadosPendientes = parseEstados(estadosSolicitudPendientesConf);
            parseEstados(estadosSolicitudConcluyentesConf);

            Long motivoDevId = catalogResolver.getMotivoIdDevolucionDesdeProduccion();
            MotivoMovimiento motivoDevolucion = motivoMovimientoRepository.findById(motivoDevId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MOTIVO_DEVOLUCION_DESDE_PRODUCCION_INEXISTENTE"));

            List<SolicitudMovimiento> solicitudesPend = Optional.ofNullable(
                    solicitudMovimientoRepository.findWithDetalles(
                            orden.getId(),
                            estadosPendientes,
                            null,
                            null,
                            false,
                            List.of()
                    )
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
                        solicitudMovimientoRepository.findWithDetalles(
                                orden.getId(),
                                null,
                                null,
                                null,
                                false,
                                List.of()
                        )
                ).orElse(List.of());
                long lotesConReserva = todas.stream()
                        .flatMap(s -> s.getDetalles().stream())
                        .map(SolicitudMovimientoDetalle::getLote)
                        .filter(l -> l != null && l.getStockReservado() != null && l.getStockReservado().compareTo(BigDecimal.ZERO) > 0)
                        .count();
                if (lotesConReserva > 0) {
                    log.warn("OP-cierre stock_reservado sin solicitud pendiente op={}, lotes={}", orden.getId(), lotesConReserva);
                }
                if (dto.getTipo() == TipoCierre.TOTAL) {
                    reservaLoteService.liberarReservasPorOrden(orden.getId());
                }
            }

            Long motivoEntradaId = catalogResolver.getMotivoIdEntradaProductoTerminado();
            MotivoMovimiento motivoEntrada = motivoMovimientoRepository.findById(motivoEntradaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MOTIVO_ENTRADA_PT_INEXISTENTE"));

            Long tipoDetalleEntradaId = catalogResolver.getTipoDetalleEntradaId();
            TipoMovimientoDetalle tipoDetalleEntrada = tipoMovimientoDetalleRepository.findById(tipoDetalleEntradaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TIPO_DETALLE_ENTRADA_INEXISTENTE"));

            BigDecimal acumulada = acumuladoActual;
            BigDecimal nuevaAcumulada = acumuladoPropuesto;
            BigDecimal porcentajeCumplimiento = calcularPorcentajeCumplimiento(nuevaAcumulada, cantidadProgramada);

            orden.setCantidadProducidaAcumulada(nuevaAcumulada);
            orden.setCantidadProducida(nuevaAcumulada);
            orden.setFechaUltimoCierre(LocalDateTime.now());

            // Cierres parciales de turno/avance no deben cambiar el estado final de la OP.
            // Solo los cierres definitivos (TOTAL o cerrada incompleta confirmada) deben
            // habilitar la transición a FINALIZADA o CERRADA_INCOMPLETA.
            if (cierreDefinitivo) {
                orden.setTipoCierre(dto.getTipo());
            } else {
                orden.setTipoCierre(null);
            }

            CierreProduccion cierre = ProduccionMapper.toEntity(dto, orden);
            cierre.setUsuarioId(usuario.getId());
            cierre.setUsuarioNombre(usuario.getNombreCompleto());
            cierreProduccionRepository.save(cierre);

            recalcularEstadoOrden(orden);

            if (orden.getEstado() == EstadoProduccion.FINALIZADA
                    || orden.getEstado() == EstadoProduccion.CERRADA_INCOMPLETA) {
                orden.setPorcentajeCumplimiento(porcentajeCumplimiento);
                orden.setTipoCierre(orden.getEstado() == EstadoProduccion.CERRADA_INCOMPLETA
                        ? TipoCierre.PARCIAL
                        : TipoCierre.TOTAL);
                orden.setUsuarioCierreId(usuario.getId());
                orden.setFechaCierre(LocalDateTime.now());
            }

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
            TipoCategoria tipoProducto = obtenerTipoCategoriaProducto(orden.getProducto());
            boolean requiereAnalisis = requiereFisico(orden.getProducto())
                    || requiereQuimico(orden.getProducto())
                    || requiereMicro(orden.getProducto());
            if (tipoProducto == TipoCategoria.PRODUCTO_SEMI_ELABORADO) {
                destino = almacenCuarentena;
                estadoLote = EstadoLote.EN_CUARENTENA;
            } else {
                if (requiereAnalisis) {
                    destino = almacenCuarentena;
                    estadoLote = EstadoLote.EN_CUARENTENA;
                } else {
                    destino = almacenPt;
                    estadoLote = EstadoLote.DISPONIBLE;
                }
            }

            Integer destinoIdInt = destino.getId();
            if (destinoIdInt != null
                    && ubicacionFisicaRepository.existsByAlmacenIdAndActivoTrue(destinoIdInt)
                    && dto.getUbicacionDestinoId() == null) {
                throw new CustomBusinessException(ApiErrorCode.UBICACION_DESTINO_REQUERIDA,
                        "Debe indicar ubicación destino para el cierre de producción",
                        Map.of("almacenDestinoId", destinoIdInt));
            }

            String codigoLote = dto.getCodigoLote();
            if (lote == null) {
                if (codigoLote != null && !codigoLote.isBlank()) {
                    boolean existeCodigo = loteProductoRepository.existsByCodigoLote(codigoLote);
                    if (existeCodigo) {
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
                    boolean existeCodigo = loteProductoRepository.existsByCodigoLote(codigoLote);
                    if (existeCodigo) {
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
            if (dto.getTipo() == TipoCierre.TOTAL) {
                BigDecimal costoTotalMaterialRealOp = Optional.ofNullable(
                                movimientoInventarioRepository.sumarCostoMaterialRealOp(orden.getId()))
                        .orElse(BigDecimal.ZERO);
                BigDecimal cantidadRealProducida = regularizacionTrazabilidadRepository
                        .findTopByOrdenProduccionIdOrderByFechaIngresoDescIdDesc(orden.getId())
                        .map(r -> Optional.ofNullable(r.getCantidadReal()).orElse(BigDecimal.ZERO))
                        .orElse(acumuladoPropuesto);
                if (cantidadRealProducida != null && cantidadRealProducida.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal costoUnitarioPsPt = costeoProduccionService
                            .calcularCostoUnitarioMaterialOp(costoTotalMaterialRealOp, cantidadRealProducida);
                    lote.setCostoUnitarioMaterial(costoUnitarioPsPt);
                    lote.setCostoTotalMaterialIngresado(costoTotalMaterialRealOp.setScale(6, RoundingMode.HALF_UP));
                    loteProductoRepository.save(lote);
                }
            }
            log.info("OP-cierre lote op={}, producto={}, loteId={}, codigoLote={}, cantidad={}, fechaFabricacion={}, fechaVencimiento={}, almacenId={}, estado={}, usuario={}",
                    orden.getId(), orden.getProducto().getId(), lote.getId(), codigoLote, cantidad, fechaFabricacion, fechaVencimiento, destino.getId(), estadoLote, usuario.getId());

            MovimientoInventarioDTO movDto = new MovimientoInventarioDTO(
                    null,
                    cantidad,
                    TipoMovimiento.ENTRADA,
                    clasifEntrada,
                    orden.getCodigoOrden(),
                    null,
                    null,
                    null,
                    null,
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
                    null,
                    lote.getEstado(),
                    null,
                    null,
                    null,
                    dto.getUbicacionDestinoId());
            log.info("OP-cierre entrada DTO traceId={} opId={} etapaId={} productoId={} loteId={} tipo={} clasificacion={} almacenOrigenId={} almacenDestinoId={} tipoDetalleId={} motivoId={} cantidad={}",
                    traceId,
                    orden.getId(),
                    null,
                    orden.getProducto().getId(),
                    lote.getId(),
                    movDto.tipoMovimiento(),
                    movDto.clasificacionMovimientoInventario(),
                    movDto.almacenOrigenId(),
                    movDto.almacenDestinoId(),
                    movDto.tipoMovimientoDetalleId(),
                    movDto.motivoMovimientoId(),
                    movDto.cantidad());
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
        if (op == null || op.getId() == null) {
            return;
        }
        if (etapaProduccionRepository.existsByOrdenProduccionId(op.getId())) {
            return;
        }
        List<EtapaProduccion> etapas = plantilla.stream()
                .map(p -> EtapaProduccion.builder()
                        .nombre(p.getNombre())
                        .secuencia(p.getSecuencia())
                        .ordenProduccion(op)
                        .estado(EstadoEtapa.PENDIENTE)
                        .fechaInicio(null)
                        .fechaFin(null)
                        .usuarioId(null)
                        .usuarioNombre(null)
                        .build())
                .toList();
        try {
            etapaProduccionRepository.saveAll(etapas);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ETAPAS_DUPLICADAS_OP", ex);
        }
    }

    public void clonarEtapas(Long ordenId) {
        produccionEtapasLockValidator.assertEtapasEditables(ordenId);
        OrdenProduccion orden = repository.findById(ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));
        List<EtapaProduccion> existentes = etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(ordenId);
        if (existentes.isEmpty()) {
            List<EtapaPlantilla> plantilla = cargarPlantillaEtapas(orden.getProducto().getId());
            clonarEtapasParaOrden(orden, plantilla);
        }
    }

    private boolean esCierreDefinitivo(CierreProduccionRequestDTO dto) {
        if (dto == null) {
            return false;
        }
        return dto.getTipo() == TipoCierre.TOTAL || Boolean.TRUE.equals(dto.getCerradaIncompleta());
    }

    private EstadoProduccion calcularEstadoObjetivo(OrdenProduccion orden, BigDecimal producida, boolean cierreDefinitivo) {
        if (orden == null || orden.getId() == null) {
            return orden != null ? orden.getEstado() : null;
        }

        if (orden.getEstado() == EstadoProduccion.CANCELADA) {
            return EstadoProduccion.CANCELADA;
        }

        List<EtapaProduccion> etapas = etapaProduccionRepository
                .findByOrdenProduccionIdOrderBySecuenciaAsc(orden.getId());

        boolean todasFinalizadas = !etapas.isEmpty()
                && etapas.stream().allMatch(e -> e.getEstado() == EstadoEtapa.FINALIZADA);

        BigDecimal programada = Optional.ofNullable(orden.getCantidadProgramada()).orElse(BigDecimal.ZERO);
        BigDecimal producidaSafe = Optional.ofNullable(producida).orElse(BigDecimal.ZERO);

        if (todasFinalizadas && cierreDefinitivo) {
            if (producidaSafe.compareTo(programada) >= 0) {
                return EstadoProduccion.FINALIZADA;
            }
            return EstadoProduccion.CERRADA_INCOMPLETA;
        }

        return EstadoProduccion.EN_PROCESO;
    }

    private LoteProducto obtenerLotePsReservado(Long ordenId) {
        List<SolicitudMovimiento> solicitudes = Optional.ofNullable(
                solicitudMovimientoRepository.findWithDetalles(
                        ordenId,
                        null,
                        null,
                        null,
                        false,
                        List.of()
                )
        ).orElse(List.of());

        List<Long> lotesPs = solicitudes.stream()
                .flatMap(sol -> Optional.ofNullable(sol.getDetalles()).orElse(List.of()).stream())
                .map(SolicitudMovimientoDetalle::getLote)
                .filter(Objects::nonNull)
                .filter(l -> l.getProducto() != null
                        && l.getProducto().getCategoriaProducto() != null
                        && l.getProducto().getCategoriaProducto().getTipo() == TipoCategoria.PRODUCTO_SEMI_ELABORADO)
                .map(LoteProducto::getId)
                .distinct()
                .toList();

        if (lotesPs.isEmpty()) {
            return null;
        }
        // TODO: si negocio define prioridad entre múltiples PS, evaluar mínimo vencimiento u otra regla explícita.
        Long loteId = lotesPs.get(0);
        return loteProductoRepository.findById(loteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LOTE_NO_ENCONTRADO"));
    }

    private BigDecimal calcularPorcentajeCumplimiento(BigDecimal cantidadFabricada, BigDecimal cantidadPlaneada) {
        if (cantidadPlaneada == null || cantidadPlaneada.compareTo(BigDecimal.ZERO) == 0 || cantidadFabricada == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return cantidadFabricada
                .multiply(BigDecimal.valueOf(100))
                .divide(cantidadPlaneada, 4, RoundingMode.HALF_UP);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reservarInsumosParaOP(Long ordenId, @Nullable Long lotePsId) {
        OrdenProduccion orden = repository.findById(ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));

        FormulaProducto formula = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(orden.getProducto().getId().longValue(), EstadoFormula.APROBADA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FORMULA_NO_ENCONTRADA"));

        List<DetalleFormula> detallesFormula = obtenerDetallesFormulaSeguro(formula);
        // Idempotencia: si ya existen solicitudes SALIDA pendientes para esta OP, no recrear
        List<EstadoSolicitudMovimiento> estadosPendientes = parseEstados(estadosSolicitudPendientesConf);
        List<SolicitudMovimiento> yaPendientes = Optional.ofNullable(
                solicitudMovimientoRepository.findWithDetalles(
                        ordenId,
                        estadosPendientes,
                        null,
                        null,
                        false,
                        List.of()
                )
        ).orElse(List.of());

        boolean haySalidasPendientes = yaPendientes.stream()
                .anyMatch(s -> s.getTipoMovimiento() == TipoMovimiento.SALIDA);

        if (haySalidasPendientes) {
            log.info("OP-reserva: ya existen solicitudes SALIDA pendientes para ordenId={}, se omite recreación", ordenId);
            return;
        }

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        log.debug("OP-reserva iniciar ordenId={}, user={}", ordenId, usuario.getId());

        MotivoMovimiento motivo = motivoMovimientoRepository
                .findByMotivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION)
                .orElseThrow(() -> new IllegalStateException("Motivo SALIDA_PRODUCCION no configurado"));
        TipoMovimientoDetalle detalle = tipoMovimientoDetalleRepository
                .findById(catalogResolver.getTipoDetalleSalidaId())
                .orElseThrow(() -> new IllegalStateException("Tipo detalle SALIDA_PRODUCCION no configurado"));

        for (DetalleFormula insumo : detallesFormula) {
            if (insumo == null || insumo.getInsumo() == null) {
                continue;
            }
            Long insumoId = insumo.getInsumo().getId().longValue();
            BigDecimal requerida = calcularCantidadRequeridaInsumo(insumo, orden);
            if (requerida.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            ModoControlInventario modoControl = Optional.ofNullable(insumo.getInsumo())
                    .map(Producto::getModoControlInventario)
                    .orElse(ModoControlInventario.CONTROL_STOCK);

            if (modoControl == ModoControlInventario.SIN_CONTROL_STOCK) {
                log.debug("OP-reserva: insumo {} configurado sin control de stock, se omite reserva e inventario", insumoId);
                continue;
            }

            BigDecimal requeridaSolicitud = requerida.setScale(6, RoundingMode.HALF_UP);
            List<Long> almacenesValidos = disponibilidadInsumoService.resolverAlmacenesPreferidos(insumo.getInsumo());

            DistribucionFefoResult distribucion = disponibilidadInsumoService.calcularDisponibilidad(
                    insumoId,
                    requerida,
                    almacenesValidos,
                    false);

            BigDecimal faltanteDistribucion = Optional.ofNullable(distribucion.getFaltante())
                    .orElse(BigDecimal.ZERO)
                    .setScale(6, RoundingMode.HALF_UP);
            BigDecimal totalAsignado = calcularTotalDistribuido(distribucion.getDetalles());
            BigDecimal requeridaComparacion = requeridaSolicitud.setScale(8, RoundingMode.HALF_UP);
            boolean sinAsignacion = totalAsignado.compareTo(requeridaComparacion) < 0;

            if (faltanteDistribucion.compareTo(BigDecimal.ZERO) > 0 || sinAsignacion) {
                manejarStockInsuficiente(insumo.getInsumo(), distribucion);
            }

            List<DistribucionFefoDetalle> detallesDistribucion = distribucion.getDetalles();
            if (detallesDistribucion.isEmpty()) {
                manejarStockInsuficiente(insumo.getInsumo(), distribucion);
            }

            BigDecimal totalReservado = detallesDistribucion.stream()
                    .map(DistribucionFefoDetalle::getCantidadReserva)
                    .filter(Objects::nonNull)
                    .filter(cantidad -> cantidad.compareTo(BigDecimal.ZERO) > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(6, RoundingMode.HALF_UP);

            if (totalReservado.compareTo(requeridaSolicitud) != 0) {
                manejarStockInsuficiente(insumo.getInsumo(), distribucion);
            }

            for (DistribucionFefoDetalle detalleDistribucion : detallesDistribucion) {
                BigDecimal usarDetalle = Optional.ofNullable(detalleDistribucion.getCantidadReserva())
                        .orElse(BigDecimal.ZERO);
                if (usarDetalle.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                Long loteId = detalleDistribucion.getLoteProductoId();
                if (loteId == null) {
                    manejarStockInsuficiente(insumo.getInsumo(), distribucion);
                }

                SolicitudMovimientoRequestDTO solicitudReq = SolicitudMovimientoRequestDTO.builder()
                        .tipoMovimiento(TipoMovimiento.SALIDA)
                        .productoId(insumoId)
                        .loteId(loteId)
                        .cantidad(usarDetalle)
                        .ordenProduccionId(orden.getId())
                        .usuarioSolicitanteId(usuario.getId())
                        .motivoMovimientoId(motivo.getId())
                        .tipoMovimientoDetalleId(detalle.getId())
                        .almacenDestinoId(catalogResolver.getAlmacenPreBodegaProduccionId())
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
                    log.debug("OP-RESERVA antes limpiar: detallesPrevios={} ordenId={} insumoId={}",
                            detallesSolicitud.size(),
                            ordenId,
                            insumoId);
                    detallesSolicitud.clear();
                }

                Long almacenOrigenId = detalleDistribucion.getAlmacenId();
                Long almacenDestinoId = solicitud.getAlmacenDestino() != null
                        ? Long.valueOf(solicitud.getAlmacenDestino().getId())
                        : null;
                if (almacenOrigenId != null && Objects.equals(almacenOrigenId, almacenDestinoId)) {
                    throw new CustomBusinessException(
                            ApiErrorCode.SOLICITUD_ORIGEN_DESTINO_IGUALES,
                            "SOLICITUD_ORIGEN_DESTINO_IGUALES",
                            Map.of(
                                    "ordenProduccionId", ordenId,
                                    "productoInsumoId", insumoId,
                                    "almacenOrigenId", almacenOrigenId,
                                    "almacenDestinoId", almacenDestinoId
                            ));
                }

                SolicitudMovimientoDetalle detSolicitud = SolicitudMovimientoDetalle.builder()
                        .solicitudMovimiento(solicitud)
                        .lote(new LoteProducto(loteId))
                        .cantidad(usarDetalle)
                        .almacenOrigen(almacenOrigenId != null
                                ? new Almacen(Math.toIntExact(almacenOrigenId))
                                : null)
                        .almacenDestino(solicitud.getAlmacenDestino())
                        .build();
                detallesSolicitud.add(detSolicitud);

                solicitudMovimientoRepository.saveAndFlush(solicitud);
                reservaLoteService.sincronizarReservasSolicitud(solicitud);
            }
        }
    }

    private void manejarStockInsuficiente(Producto insumo, DistribucionFefoResult resultado) {
        BigDecimal faltante = Optional.ofNullable(resultado.getFaltante())
                .orElse(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);

        Producto producto = Optional.ofNullable(insumo)
                .orElseGet(() -> resultado.getProductoInsumoId() != null
                        ? productoRepository.findById(resultado.getProductoInsumoId()).orElse(null)
                        : null);

        String codigo = producto != null && producto.getCodigoSku() != null
                ? producto.getCodigoSku()
                : resultado.getProductoInsumoId() != null ? resultado.getProductoInsumoId().toString() : "";
        String nombre = producto != null && producto.getNombre() != null
                ? producto.getNombre()
                : "Insumo";
        String unidad = producto != null && producto.getUnidadMedida() != null
                && producto.getUnidadMedida().getNombre() != null
                ? producto.getUnidadMedida().getNombre()
                : "";

        log.warn(
                "STOCK_INSUFICIENTE en OP: insumo {} - {}, requerido={}, stockFisicoTotal={}, stockReservadoTotal={}, stockLibreTotal={}, faltante={}",
                codigo,
                nombre,
                Optional.ofNullable(resultado.getRequerido()).orElse(BigDecimal.ZERO),
                Optional.ofNullable(resultado.getStockFisicoTotal()).orElse(BigDecimal.ZERO),
                Optional.ofNullable(resultado.getStockReservadoTotal()).orElse(BigDecimal.ZERO),
                Optional.ofNullable(resultado.getStockLibreTotal()).orElse(BigDecimal.ZERO),
                faltante);

        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                String.format("STOCK_INSUFICIENTE: insumo %s - %s, faltan %s %s",
                        codigo,
                        nombre,
                        faltante.toPlainString(),
                        unidad));
    }

    private BigDecimal calcularTotalDistribuido(List<DistribucionFefoDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        }
        return detalles.stream()
                .map(DistribucionFefoDetalle::getCantidadCalculo)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(8, RoundingMode.HALF_UP);
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
        boolean etapaYaActiva = etapa.getEstado() == EstadoEtapa.EN_PROCESO
                && etapa.getFechaInicio() != null
                && etapa.getFechaFin() == null;
        if (etapaYaActiva) {
            return etapa;
        }
        if (etapa.getEstado() != EstadoEtapa.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ETAPA_NO_INICIABLE");
        }
        long activas = etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(ordenId);
        List<EtapaProduccion> activasList = activas > 0
                ? etapaProduccionRepository.findByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(ordenId)
                : List.of();
        List<Long> activasIds = activasList.stream().map(EtapaProduccion::getId).toList();
        if (activas > 1) {
            throw new CustomBusinessException(ApiErrorCode.PRODUCCION_MULTIPLES_ETAPAS_ACTIVAS,
                    "Existen múltiples etapas activas para la orden",
                    Map.of("ordenProduccionId", ordenId,
                            "etapaActivaIds", activasIds));
        }
        boolean hayOtraActiva = activas == 1 && activasIds.stream().noneMatch(id -> Objects.equals(id, etapaId));
        if (hayOtraActiva) {
            throw new CustomBusinessException(ApiErrorCode.PRODUCCION_OTRA_ETAPA_ACTIVA,
                    "Ya existe otra etapa en proceso para la orden",
                    Map.of("ordenProduccionId", ordenId,
                            "etapaActivaIds", activasIds));
        }

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        boolean actualizarOrden = false;
        if (orden.getEstado() == EstadoProduccion.CREADA) {
            validarSolicitudesMovimientosEjecutadas(orden);
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
            TipoCategoria tipoProducto = obtenerTipoCategoriaProducto(orden.getProducto());
            boolean requiereAnalisis = requiereFisico(orden.getProducto())
                    || requiereQuimico(orden.getProducto())
                    || requiereMicro(orden.getProducto());
            if (tipoProducto == TipoCategoria.PRODUCTO_SEMI_ELABORADO) {
                destino = almacenCuarentena;
                estadoLote = EstadoLote.EN_CUARENTENA;
            } else {
                if (requiereAnalisis) {
                    destino = almacenCuarentena;
                    estadoLote = EstadoLote.EN_CUARENTENA;
                } else {
                    destino = almacenPt;
                    estadoLote = EstadoLote.DISPONIBLE;
                }
            }

            LocalDateTime fechaFabricacion = LocalDateTime.now();
            Integer semanasVigencia = obtenerSemanasVigenciaProductoTerminado(orden.getProducto());
            LocalDateTime fechaVencimientoBase = semanasVigencia != null
                    ? fechaFabricacion.plusWeeks(semanasVigencia)
                    : null;
            LocalDateTime fechaVencimiento = fechaVencimientoBase;

            LoteProducto lotePsOrigen = null;
            if (orden.getProducto().getCategoriaProducto() != null
                    && orden.getProducto().getCategoriaProducto().getTipo() == TipoCategoria.PRODUCTO_TERMINADO
                    && Objects.equals(semanasVigencia, SEMANAS_HERENCIA_PS_PT)) {
                lotePsOrigen = obtenerLotePsReservado(orden.getId());
                if (lotePsOrigen != null && lotePsOrigen.getFechaVencimiento() != null) {
                    fechaVencimiento = lotePsOrigen.getFechaVencimiento();
                } else if (lotePsOrigen != null && semanasVigencia == null) {
                    throw new CustomBusinessException(ApiErrorCode.VIDA_UTIL_NO_CONFIGURADA,
                            "El lote semielaborado consumido no tiene fecha de vencimiento configurada");
                }
            }

            LoteProducto lote = LoteProducto.builder()
                    .codigoLote(codigoLote)
                    .producto(orden.getProducto())
                    .almacen(destino)
                    .estado(estadoLote)
                    .stockLote(BigDecimal.ZERO)
                    .fechaFabricacion(fechaFabricacion)
                    .fechaVencimiento(fechaVencimiento)
                    .ordenProduccion(orden)
                    .lotePsOrigen(lotePsOrigen)
                    .build();

            lote = loteProductoRepository.save(lote);
            orden.setLoteId(lote.getId());
            actualizarOrden = true;
        }
        if (actualizarOrden) {
            repository.save(orden);
        }

        etapa.setEstado(EstadoEtapa.EN_PROCESO);
        etapa.setFechaInicio(LocalDateTime.now());
        etapa.setUsuarioId(usuario.getId());
        etapa.setUsuarioNombre(usuario.getNombreCompleto());
        EtapaProduccion guardada = etapaProduccionRepository.save(etapa);
        checklistEtapaService.generarChecklistDesdeTemplateSiNoExiste(guardada.getId());

        // Consumo etapa 1: generar SALIDA_PRODUCCION desde Pre-Bodega (idempotente)
        if (etapa.getSecuencia() != null && etapa.getSecuencia() == 1) {
            movimientoInventarioService.consumirInsumosPorOrden(ordenId, etapaId, usuario.getId());
        }

        return guardada;
    }

    private void recalcularEstadoOrden(OrdenProduccion orden) {
        if (orden == null || orden.getId() == null || orden.getEstado() == EstadoProduccion.CANCELADA) {
            return;
        }

        List<EtapaProduccion> etapas = etapaProduccionRepository
                .findByOrdenProduccionIdOrderBySecuenciaAsc(orden.getId());

        boolean todasFinalizadas = !etapas.isEmpty()
                && etapas.stream().allMatch(e -> e.getEstado() == EstadoEtapa.FINALIZADA);

        BigDecimal programada = Optional.ofNullable(orden.getCantidadProgramada()).orElse(BigDecimal.ZERO);
        BigDecimal producida = Optional.ofNullable(orden.getCantidadProducidaAcumulada()).orElse(BigDecimal.ZERO);
        long cierresRegistrados = cierreProduccionRepository.countByOrdenProduccionId(orden.getId());
        boolean cierreDefinitivo = orden.getTipoCierre() == TipoCierre.TOTAL || orden.getTipoCierre() == TipoCierre.PARCIAL;

        if (todasFinalizadas) {
            if (!cierreDefinitivo) {
                orden.setEstado(EstadoProduccion.EN_PROCESO);
            } else {
                if (producida.compareTo(BigDecimal.ZERO) <= 0 || cierresRegistrados == 0) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "OP_SIN_CIERRES_PRODUCCION");
                }
                if (producida.compareTo(programada) >= 0) {
                    orden.setEstado(EstadoProduccion.FINALIZADA);
                } else {
                    orden.setEstado(EstadoProduccion.CERRADA_INCOMPLETA);
                }
                if (orden.getFechaFin() == null) {
                    orden.setFechaFin(LocalDateTime.now());
                }
            }
        } else if (orden.getEstado() != EstadoProduccion.CANCELADA) {
            orden.setEstado(EstadoProduccion.EN_PROCESO);
        }
    }

    private void validarSolicitudesMovimientosEjecutadas(OrdenProduccion orden) {
        List<SolicitudMovimiento> solicitudes = solicitudMovimientoRepository.findByOrdenProduccionId(orden.getId());

        if (solicitudes == null || solicitudes.isEmpty()) {
            log.warn("[PRODUCCION] OP {} sin solicitudes de movimiento de insumos. No se puede iniciar la producción.",
                    orden.getCodigoOrden());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ORDEN_SIN_SOLICITUDES_MOVIMIENTO");
        }

        List<EstadoSolicitudMovimiento> estadosPendientes = List.of(
                EstadoSolicitudMovimiento.PENDIENTE,
                EstadoSolicitudMovimiento.AUTORIZADA,
                EstadoSolicitudMovimiento.RESERVADA,
                EstadoSolicitudMovimiento.PARCIAL
        );

        boolean tieneSolicitudesPendientes = solicitudes.stream()
                .map(SolicitudMovimiento::getEstado)
                .filter(Objects::nonNull)
                .anyMatch(estadosPendientes::contains);

        if (tieneSolicitudesPendientes) {
            log.warn("[PRODUCCION] OP {} con solicitudes de movimiento pendientes. No se puede iniciar la producción.",
                    orden.getCodigoOrden());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ORDEN_MOVIMIENTOS_PENDIENTES");
        }
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
        if (etapa.getFechaInicio() == null) {
            throw new CustomBusinessException(ApiErrorCode.ETAPA_NO_INICIADA, "ETAPA_NO_INICIADA",
                    Map.of("ordenProduccionId", ordenId, "etapaId", etapaId));
        }
        checklistEtapaService.ensureChecklistOperativo(etapaId);
        checklistEtapaService.validarChecklistCompleto(etapaId);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO"));

        movimientoInventarioService.consumirInsumosPorOrden(ordenId, etapaId, usuario.getId());

        etapa.setEstado(EstadoEtapa.FINALIZADA);
        etapa.setFechaFin(LocalDateTime.now());
        etapa.setUsuarioId(usuario.getId());
        etapa.setUsuarioNombre(usuario.getNombreCompleto());
        EtapaProduccion guardada = etapaProduccionRepository.save(etapa);

        recalcularEstadoOrden(orden);
        repository.save(orden);

        return guardada;
    }

    public List<InsumoOPDTO> listarInsumos(Long id) {
        OrdenProduccion orden = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));
        FormulaProducto formula = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(orden.getProducto().getId().longValue(), EstadoFormula.APROBADA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FORMULA_NO_ENCONTRADA"));
        List<InsumoOPDTO> lista = new ArrayList<>();
        Long detalleSalidaId = resolverTipoDetalleSalidaProduccionId();
        for (DetalleFormula det : obtenerDetallesFormulaSeguro(formula)) {
            if (det == null || det.getInsumo() == null) {
                continue;
            }
            ModoControlInventario modoControl = Optional.ofNullable(det.getInsumo().getModoControlInventario())
                    .orElse(ModoControlInventario.CONTROL_STOCK);
            if (modoControl == ModoControlInventario.SIN_CONTROL_STOCK) {
                log.debug("OP-insumos: insumo {} sin control de stock, se omite en faltantes", det.getInsumo().getId());
                continue;
            }
            BigDecimal requerida = det.getCantidadNecesaria().multiply(orden.getCantidadProgramada());
            Long insumoId = det.getInsumo().getId().longValue();
            // Consumido real: salidas de producción registradas para la OP (independiente de etapa).
            BigDecimal consumida = Optional.ofNullable(
                    movimientoInventarioRepository.sumaCantidadPorOrdenProductoTipoDetalle(
                            id,
                            insumoId,
                            TipoMovimiento.SALIDA,
                            detalleSalidaId
                    )
            ).orElse(BigDecimal.ZERO);
            // Alistado/reservado: traslados internos a Pre-Bodega sin etapa (pre-arranque). El alistado no debe
            // contabilizar las salidas de producción.
            BigDecimal alistadoTransferencias = Optional.ofNullable(
                    movimientoInventarioRepository.sumaCantidadPorOrdenProductoClasificacionSinEtapa(
                            id,
                            insumoId,
                            ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                            TipoMovimiento.TRANSFERENCIA
                    )
            ).orElse(BigDecimal.ZERO);
            BigDecimal alistado = alistadoTransferencias;
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
                    faltante,
                    alistado
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

    public Page<MovimientoInventarioResponseDTO> listarMovimientos(Long id, Long etapaId, Pageable pageable) {
        Page<MovimientoInventario> page;
        if (etapaId != null) {
            page = movimientoInventarioRepository
                    .findByOrdenProduccionIdAndOrdenProduccionEtapaIdAndClasificacion(
                            id,
                            etapaId,
                            ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                            pageable);
        } else {
            page = movimientoInventarioRepository.findByOrdenProduccionId(id, pageable);
        }
        return page.map(movimientoInventarioMapper::safeToResponseDTO);
    }

    @Override
    public List<MovimientoInventarioResponseDTO> listarConsumosPorEtapa(Long ordenId,
                                                                        Long etapaId,
                                                                        @Nullable ClasificacionMovimientoInventario clasificacion) {
        ClasificacionMovimientoInventario clasificacionFiltro =
                clasificacion != null ? clasificacion : ClasificacionMovimientoInventario.SALIDA_PRODUCCION;
        List<MovimientoInventario> movimientos = movimientoInventarioRepository
                .findByOrdenProduccionIdAndOrdenProduccionEtapaIdAndClasificacionOrderByFechaIngresoAsc(
                        ordenId,
                        etapaId,
                        clasificacionFiltro
                );
        return movimientos.stream()
                .map(movimientoInventarioMapper::safeToResponseDTO)
                .toList();
    }

    @Override
    public List<MovimientoInventarioResponseDTO> listarMovimientosPorEtapa(Long ordenId,
                                                                           Long etapaId,
                                                                           @Nullable ClasificacionMovimientoInventario clasificacion) {
        OrdenProduccion orden = repository.findById(ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));
        EtapaProduccion etapa = etapaProduccionRepository.findById(etapaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ETAPA_NO_ENCONTRADA"));
        if (!Objects.equals(etapa.getOrdenProduccion().getId(), orden.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ETAPA_NO_PERTENECE_A_ORDEN");
        }

        ClasificacionMovimientoInventario clasificacionFiltro =
                clasificacion != null ? clasificacion : ClasificacionMovimientoInventario.SALIDA_PRODUCCION;

        List<MovimientoInventario> movimientos = movimientoInventarioRepository
                .findByOrdenProduccionIdAndOrdenProduccionEtapaIdAndClasificacionOrderByFechaIngresoDesc(
                        ordenId,
                        etapaId,
                        clasificacionFiltro
                );
        return movimientos.stream()
                .map(movimientoInventarioMapper::safeToResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public void cancelarOrden(Long ordenProduccionId, @Nullable String motivo) {
        OrdenProduccion orden = repository.findByIdForUpdate(ordenProduccionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));

        if (orden.getEstado() == EstadoProduccion.FINALIZADA
                || orden.getEstado() == EstadoProduccion.CANCELADA
                || orden.getEstado() == EstadoProduccion.CERRADA_INCOMPLETA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ORDEN_NO_CANCELABLE");
        }

        if (orden.getEstado() != EstadoProduccion.CREADA && orden.getEstado() != EstadoProduccion.EN_PROCESO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ORDEN_NO_CANCELABLE");
        }

        if (reservaLoteService.existenReservasConsumidasPorOrden(orden.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ORDEN_CONSUMOS_REGISTRADOS");
        }

        List<SolicitudMovimiento> solicitudes = Optional.ofNullable(
                solicitudMovimientoRepository.findWithDetalles(
                        orden.getId(),
                        null,
                        null,
                        null,
                        false,
                        List.of()
                )
        ).orElse(List.of());

        for (SolicitudMovimiento solicitud : solicitudes) {
            boolean tieneDetalles = solicitud.getDetalles() != null && !solicitud.getDetalles().isEmpty();
            boolean todosCancelados = true;
            boolean algunAtendido = false;

            if (tieneDetalles) {
                for (SolicitudMovimientoDetalle detalle : solicitud.getDetalles()) {
                    if (detalle.getEstado() == EstadoSolicitudMovimientoDetalle.ATENDIDO) {
                        algunAtendido = true;
                        todosCancelados = false;
                        continue;
                    }
                    detalle.setEstado(EstadoSolicitudMovimientoDetalle.CANCELADO);
                }
            }

            if (!tieneDetalles) {
                todosCancelados = true;
            }

            if (!algunAtendido && todosCancelados) {
                solicitud.setEstado(EstadoSolicitudMovimiento.CANCELADA);
                solicitud.setFechaResolucion(LocalDateTime.now());
            }
        }

        if (!solicitudes.isEmpty()) {
            solicitudMovimientoRepository.saveAll(solicitudes);
        }

        reservaLoteService.liberarReservasPorOrden(orden.getId());

        orden.setEstado(EstadoProduccion.CANCELADA);
        if (orden.getFechaFin() == null) {
            orden.setFechaFin(LocalDateTime.now());
        }

        repository.save(orden);
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

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        Long etapaConsumoId = resolverEtapaPrincipal(orden.getId());
        movimientoInventarioService.consumirInsumosPorOrden(orden.getId(), etapaConsumoId, usuario.getId());

        orden.setCantidadProducida(cantidadProducida);
        orden.setEstado(EstadoProduccion.FINALIZADA);
        orden.setFechaFin(LocalDateTime.now());

        return repository.save(orden);
    }

    @Override
    @Transactional(readOnly = true)
    public ProduccionTrazabilidadResponseDTO obtenerTrazabilidad(Long ordenProduccionId) {
        OrdenProduccion orden = repository.findById(ordenProduccionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));

        ProduccionTrazabilidadResponseDTO.OpDTO opDTO = ProduccionTrazabilidadResponseDTO.OpDTO.builder()
                .id(orden.getId())
                .codigoOrden(orden.getCodigoOrden())
                .productoId(orden.getProducto() != null ? orden.getProducto().getId().longValue() : null)
                .nombreProducto(orden.getProducto() != null ? orden.getProducto().getNombre() : null)
                .codigoSku(orden.getProducto() != null ? orden.getProducto().getCodigoSku() : null)
                .loteProduccion(orden.getLoteProduccion())
                .estado(orden.getEstado() != null ? orden.getEstado().name() : null)
                .fechaInicio(orden.getFechaInicio())
                .fechaFin(orden.getFechaFin())
                .build();

        List<ProduccionTrazabilidadResponseDTO.EtapaDTO> etapasDto = etapaProduccionRepository
                .findByOrdenProduccionIdOrderBySecuenciaAsc(ordenProduccionId).stream()
                .map(this::mapEtapaTrazabilidad)
                .toList();

        List<ProduccionTrazabilidadResponseDTO.ConsumoDTO> consumosDto = movimientoInventarioRepository
                .findByOrdenProduccionIdAndClasificacion(
                        ordenProduccionId,
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                        Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::mapConsumoTrazabilidad)
                .toList();

        List<ProduccionTrazabilidadResponseDTO.MovimientoDTO> movimientosDto = movimientoInventarioRepository
                .findByOrdenProduccionIdOrderByFechaIngresoAsc(ordenProduccionId).stream()
                .map(this::mapMovimientoTrazabilidad)
                .toList();

        List<ProduccionTrazabilidadResponseDTO.LoteResultanteDTO> lotesDto = loteProductoRepository
                .findByOrdenProduccionId(ordenProduccionId).stream()
                .map(lote -> ProduccionTrazabilidadResponseDTO.LoteResultanteDTO.builder()
                        .loteId(lote.getId())
                        .codigoLote(lote.getCodigoLote())
                        .estado(lote.getEstado() != null ? lote.getEstado().name() : null)
                        .almacenId(lote.getAlmacen() != null ? lote.getAlmacen().getId().longValue() : null)
                        .almacenNombre(lote.getAlmacen() != null ? lote.getAlmacen().getNombre() : null)
                        .fechaFabricacion(lote.getFechaFabricacion())
                        .fechaVencimiento(lote.getFechaVencimiento())
                        .lotePsOrigenId(lote.getLotePsOrigen() != null ? lote.getLotePsOrigen().getId() : null)
                        .build())
                .toList();

        List<ProduccionTrazabilidadResponseDTO.CierreDTO> cierresDto = cierreProduccionRepository
                .findByOrdenProduccionId(ordenProduccionId, Pageable.unpaged())
                .getContent()
                .stream()
                .map(cierre -> ProduccionTrazabilidadResponseDTO.CierreDTO.builder()
                        .id(cierre.getId())
                        .tipoCierre(cierre.getTipo() != null ? cierre.getTipo().name() : null)
                        .fecha(cierre.getFechaCierre())
                        .usuarioId(cierre.getUsuarioId())
                        .usuarioNombre(cierre.getUsuarioNombre())
                        .observacion(cierre.getObservacion())
                        .build())
                .toList();

        return ProduccionTrazabilidadResponseDTO.builder()
                .op(opDTO)
                .etapas(etapasDto)
                .consumos(consumosDto)
                .movimientos(movimientosDto)
                .lotesResultantes(lotesDto)
                .cierres(cierresDto)
                .build();
    }

    private ProduccionTrazabilidadResponseDTO.EtapaDTO mapEtapaTrazabilidad(EtapaProduccion etapa) {
        List<com.willyes.clemenintegra.produccion.model.ChecklistEtapaItem> items =
                checklistEtapaItemRepository.findByEtapaProduccionIdOrderByIdAsc(etapa.getId());
        int total = items.size();
        int obligPendientes = (int) items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getObligatorio()))
                .filter(i -> !(EstadoChecklistItem.COMPLETADO.equals(i.getEstado())
                        || (EstadoChecklistItem.NO_APLICA.equals(i.getEstado())
                        && Boolean.TRUE.equals(i.getPermitirNoAplica()))))
                .count();
        boolean completo = total > 0 && obligPendientes == 0;

        ProduccionTrazabilidadResponseDTO.ChecklistResumenDTO resumenChecklist =
                ProduccionTrazabilidadResponseDTO.ChecklistResumenDTO.builder()
                        .total(total)
                        .obligatoriosPendientes(obligPendientes)
                        .completo(completo)
                        .build();

        return ProduccionTrazabilidadResponseDTO.EtapaDTO.builder()
                .id(etapa.getId())
                .secuencia(etapa.getSecuencia())
                .nombreEtapa(etapa.getNombre())
                .estado(etapa.getEstado() != null ? etapa.getEstado().name() : null)
                .fechaInicio(etapa.getFechaInicio())
                .fechaFin(etapa.getFechaFin())
                .usuarioId(etapa.getUsuarioId())
                .usuarioNombre(etapa.getUsuarioNombre())
                .checklist(resumenChecklist)
                .build();
    }

    private ProduccionTrazabilidadResponseDTO.ConsumoDTO mapConsumoTrazabilidad(MovimientoInventario movimiento) {
        return ProduccionTrazabilidadResponseDTO.ConsumoDTO.builder()
                .movimientoId(movimiento.getId())
                .etapaId(movimiento.getOrdenProduccionEtapa() != null ? movimiento.getOrdenProduccionEtapa().getId() : null)
                .tipoMovimiento(movimiento.getTipoMovimiento() != null ? movimiento.getTipoMovimiento().name() : null)
                .clasificacion(movimiento.getClasificacion() != null ? movimiento.getClasificacion().name() : null)
                .productoId(movimiento.getProducto() != null ? movimiento.getProducto().getId().longValue() : null)
                .codigoSku(movimiento.getProducto() != null ? movimiento.getProducto().getCodigoSku() : null)
                .nombreProducto(movimiento.getProducto() != null ? movimiento.getProducto().getNombre() : null)
                .loteId(movimiento.getLote() != null ? movimiento.getLote().getId() : null)
                .codigoLote(movimiento.getLote() != null ? movimiento.getLote().getCodigoLote() : null)
                .almacenOrigenId(movimiento.getAlmacenOrigen() != null ? movimiento.getAlmacenOrigen().getId().longValue() : null)
                .almacenOrigenNombre(movimiento.getAlmacenOrigen() != null ? movimiento.getAlmacenOrigen().getNombre() : null)
                .cantidad(movimiento.getCantidad())
                .fechaMovimiento(movimiento.getFechaIngreso())
                .build();
    }

    private ProduccionTrazabilidadResponseDTO.MovimientoDTO mapMovimientoTrazabilidad(MovimientoInventario movimiento) {
        return ProduccionTrazabilidadResponseDTO.MovimientoDTO.builder()
                .movimientoId(movimiento.getId())
                .etapaId(movimiento.getOrdenProduccionEtapa() != null ? movimiento.getOrdenProduccionEtapa().getId() : null)
                .tipoMovimiento(movimiento.getTipoMovimiento() != null ? movimiento.getTipoMovimiento().name() : null)
                .clasificacion(movimiento.getClasificacion() != null ? movimiento.getClasificacion().name() : null)
                .loteId(movimiento.getLote() != null ? movimiento.getLote().getId() : null)
                .codigoLote(movimiento.getLote() != null ? movimiento.getLote().getCodigoLote() : null)
                .almacenOrigen(movimiento.getAlmacenOrigen() != null ? movimiento.getAlmacenOrigen().getNombre() : null)
                .almacenDestino(movimiento.getAlmacenDestino() != null ? movimiento.getAlmacenDestino().getNombre() : null)
                .cantidad(movimiento.getCantidad())
                .fechaMovimiento(movimiento.getFechaIngreso())
                .build();
    }
}
