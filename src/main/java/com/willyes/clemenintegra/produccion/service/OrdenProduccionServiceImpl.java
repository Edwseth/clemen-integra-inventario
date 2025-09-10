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
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.CierreProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import com.willyes.clemenintegra.produccion.service.spec.OrdenProduccionSpecifications;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;

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

    @Value("${inventory.almacen.pt.id}")
    private Long almacenPtId;

    @Value("${inventory.almacen.cuarentena.id}")
    private Long almacenCuarentenaId;

    @Value("${inventory.motivo.entradaPt}")
    private String motivoEntradaPt;

    @Value("${inventory.tipoDetalle.entradaId}")
    private Long tipoDetalleEntradaId;

    private String generarCodigoOrden() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefijo = "OP-CLEMEN-" + fecha;
        Long contador = repository.countByCodigoOrdenStartingWith(prefijo);
        return prefijo + "-" + String.format("%02d", contador + 1);
    }

    private List<Long> obtenerAlmacenesOrigen(Producto insumo) {
        if (insumo == null || insumo.getCategoriaProducto() == null
                || insumo.getCategoriaProducto().getTipo() == null) {
            return List.of();
        }
        return switch (insumo.getCategoriaProducto().getTipo()) {
            case MATERIA_PRIMA -> List.of(1L);
            case MATERIAL_EMPAQUE -> List.of(5L);
            default -> List.of();
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

            BigDecimal stockActual = stockQueryService.obtenerStockDisponible(insumoId); // LÍNEA CODEx corregida: stock desde lotes

            int producibleConEste = 0;
            if (insumo.getCantidadNecesaria().compareTo(BigDecimal.ZERO) > 0) {
                producibleConEste = stockActual.divide(insumo.getCantidadNecesaria(), 0, RoundingMode.DOWN).intValue();
            }
            if (maxProducible == null || producibleConEste < maxProducible) {
                maxProducible = producibleConEste;
            }

            if (stockActual.compareTo(cantidadRequerida) < 0) {
                stockSuficiente = false;
                faltantes.add(InsumoFaltanteDTO.builder()
                        .productoId(insumoId)
                        .nombre(productoInsumo.getNombre())
                        .requerido(cantidadRequerida)
                        .disponible(stockActual)
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
                .findByDescripcion("SALIDA_PRODUCCION")
                .map(t -> t.getId())
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

            BigDecimal cantidad = dto.getCantidad().setScale(2, RoundingMode.DOWN);
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_INVALIDA");
            }
            dto.setCantidad(cantidad);

            if (orden.getId() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ORDEN_PRODUCCION_OBLIGATORIA");
            }

            ClasificacionMovimientoInventario clasifEntrada;
            try {
                clasifEntrada = ClasificacionMovimientoInventario.valueOf(motivoEntradaPt);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MOTIVO_ENTRADA_PT_INEXISTENTE");
            }

            MotivoMovimiento motivoEntrada = motivoMovimientoRepository.findByMotivo(clasifEntrada)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MOTIVO_ENTRADA_PT_INEXISTENTE"));

            TipoMovimientoDetalle tipoDetalleEntrada = tipoMovimientoDetalleRepository.findById(tipoDetalleEntradaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TIPO_DETALLE_ENTRADA_INEXISTENTE"));

            LoteProducto lote = loteProductoRepository
                    .findByOrdenProduccionIdAndProductoId(orden.getId(), orden.getProducto().getId().longValue())
                    .orElse(null);

            if (lote != null) {
                Optional<MovimientoInventario> existente = movimientoInventarioRepository
                        .findByTipoMovimientoAndMotivoMovimientoIdAndOrdenProduccionIdAndProductoIdAndLoteId(
                                TipoMovimiento.ENTRADA,
                                motivoEntrada.getId(),
                                orden.getId(),
                                orden.getProducto().getId().longValue(),
                                lote.getId()
                        );
                if (existente.isPresent()) {
                    MovimientoInventario mov = existente.get();
                    if (mov.getCantidad().compareTo(cantidad) == 0) {
                        log.info("OP-cierre entrada PT idempotente op={}, producto={}, lote={}, movId={}, cantidad={}",
                                orden.getId(), orden.getProducto().getId(), lote.getId(), mov.getId(), cantidad);
                        return orden;
                    } else {
                        log.warn("OP-cierre entrada PT duplicada op={}, producto={}, lote={}, movId={}, cantExistente={}, cantNueva={}",
                                orden.getId(), orden.getProducto().getId(), lote.getId(), mov.getId(), mov.getCantidad(), cantidad);
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "ENTRADA_PT_YA_REGISTRADA");
                    }
                }
            }

            BigDecimal programada = orden.getCantidadProgramada();
            BigDecimal acumulada = Optional.ofNullable(orden.getCantidadProducidaAcumulada()).orElse(BigDecimal.ZERO);
            BigDecimal restante = programada.subtract(acumulada);
            if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "SIN_CANTIDAD_RESTANTE");
            }
            if (cantidad.compareTo(restante) > 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_EXCEDE_RESTANTE");
            }

            BigDecimal nuevaAcumulada = acumulada.add(cantidad);

            if (dto.getTipo() == TipoCierre.PARCIAL) {
                if (nuevaAcumulada.compareTo(programada) > 0) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_EXCEDE_PROGRAMADA");
                }
            } else if (dto.getTipo() == TipoCierre.TOTAL) {
                boolean incompleta = Boolean.TRUE.equals(dto.getCerradaIncompleta());
                if (incompleta) {
                    if (nuevaAcumulada.compareTo(programada) >= 0) {
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_INVALIDA");
                    }
                    orden.setEstado(EstadoProduccion.CERRADA_INCOMPLETA);
                    orden.setFechaFin(LocalDateTime.now());
                } else {
                    if (nuevaAcumulada.compareTo(programada) != 0) {
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_INVALIDA");
                    }
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

            if (almacenPtId == null || almacenCuarentenaId == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_NO_CONFIGURADO");
            }

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

            LocalDateTime fechaFabricacion = dto.getFechaFabricacion();
            if (fechaFabricacion == null || fechaFabricacion.isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "FECHA_INVALIDA");
            }
            LocalDateTime fechaVencimiento = dto.getFechaVencimiento();
            if (fechaVencimiento != null && fechaVencimiento.isBefore(fechaFabricacion)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "FECHA_INVALIDA");
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
                        .stockLote(cantidad)
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
                lote.setStockLote(lote.getStockLote().add(cantidad).setScale(2, RoundingMode.DOWN));
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
                    usuario.getId(),
                    orden.getId(),
                    null,
                    null,
                    null,
                    lote.getEstado()
            );
            try {
                movimientoInventarioService.registrarMovimiento(movDto);
            } catch (DataIntegrityViolationException ex) {
                log.warn("OP-cierre entrada PT colisión UNIQUE op={}, producto={}, lote={}, motivoId={}, resultado=CONFLICTO", orden.getId(), orden.getProducto().getId(), lote.getId(), motivoEntrada.getId());
                throw new ResponseStatusException(HttpStatus.CONFLICT, "ENTRADA_PT_YA_REGISTRADA");
            }
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
                .findByDescripcion("SALIDA_PRODUCCION")
                .orElseThrow(() -> new IllegalStateException("Tipo detalle SALIDA_PRODUCCION no configurado"));

        for (DetalleFormula insumo : formula.getDetalles()) {
            Long insumoId = insumo.getInsumo().getId().longValue();
            BigDecimal requerida = insumo.getCantidadNecesaria().multiply(orden.getCantidadProgramada());
            BigDecimal restante = requerida;

            List<Long> almacenesValidos = obtenerAlmacenesOrigen(insumo.getInsumo());
            List<LoteProducto> lotes = loteProductoRepository.findDisponiblesFefo(
                    insumoId,
                    List.of(EstadoLote.DISPONIBLE, EstadoLote.LIBERADO),
                    almacenesValidos.isEmpty() ? null : almacenesValidos);

            SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                    .tipoMovimiento(TipoMovimiento.SALIDA)
                    .producto(insumo.getInsumo())
                    .cantidad(requerida)
                    .ordenProduccion(orden)
                    .usuarioSolicitante(usuario)
                    .motivoMovimiento(motivo)
                    .tipoMovimientoDetalle(detalle)
                    .estado(EstadoSolicitudMovimiento.RESERVADA)
                    .build();

            for (LoteProducto lote : lotes) {
                if (restante.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal disponible = lote.getStockLote().subtract(lote.getStockReservado());
                BigDecimal usar = disponible.min(restante);
                if (usar.compareTo(BigDecimal.ZERO) <= 0) continue;

                int updated = loteProductoRepository.reservarStock(lote.getId(), usar);
                if (updated == 0) {
                    continue;
                }

                SolicitudMovimientoDetalle detSolicitud = SolicitudMovimientoDetalle.builder()
                        .solicitudMovimiento(solicitud)
                        .lote(lote)
                        .cantidad(usar)
                        .almacenOrigen(lote.getAlmacen())
                        .build();
                solicitud.getDetalles().add(detSolicitud);

                restante = restante.subtract(usar);
            }

            if (restante.compareTo(BigDecimal.ZERO) > 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "STOCK_INSUFICIENTE: faltan " + restante);
            }

            solicitudMovimientoRepository.save(solicitud);
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
            orden.setLoteProduccion(generarCodigoLote(orden.getProducto()));
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
                                usuario.getId(),
                                ordenId,
                                null,
                                null,
                                null,
                                detRes.getLote().getEstado()
                        );
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
        EtapaProduccion guardada = etapaProduccionRepository.save(etapa);

        boolean todasFinalizadas = etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(ordenId)
                .stream()
                .allMatch(e -> e.getEstado() == EstadoEtapa.FINALIZADA);
        if (todasFinalizadas) {
            orden.setEstado(EstadoProduccion.FINALIZADA);
            orden.setFechaFin(LocalDateTime.now());
            repository.save(orden);
        }
        return guardada;
    }

    public List<InsumoOPDTO> listarInsumos(Long id) {
        OrdenProduccion orden = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));
        FormulaProducto formula = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(orden.getProducto().getId().longValue(), EstadoFormula.APROBADA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FORMULA_NO_ENCONTRADA"));
        TipoMovimientoDetalle detalleSalida = tipoMovimientoDetalleRepository.findByDescripcion("SALIDA_PRODUCCION")
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
