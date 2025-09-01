package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.produccion.dto.InsumoFaltanteDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.mapper.OrdenProduccionMapper;
import com.willyes.clemenintegra.produccion.mapper.ProduccionMapper;
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
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import jakarta.persistence.OptimisticLockException;

@Service
@RequiredArgsConstructor
public class OrdenProduccionServiceImpl implements OrdenProduccionService {

    private final FormulaProductoRepository formulaProductoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudMovimientoService solicitudMovimientoService;
    private final OrdenProduccionRepository repository;
    private final OrdenProduccionMapper ordenProduccionMapper;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private final CierreProduccionRepository cierreProduccionRepository;
    private final MovimientoInventarioService movimientoInventarioService;
    private final LoteProductoRepository loteProductoRepository;
    private final AlmacenRepository almacenRepository;

    private String generarCodigoOrden() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefijo = "OP-CLEMEN-" + fecha;
        Long contador = repository.countByCodigoOrdenStartingWith(prefijo);
        return prefijo + "-" + String.format("%02d", contador + 1);
    }

    @Transactional
    public ResultadoValidacionOrdenDTO guardarConValidacionStock(OrdenProduccion orden) {
        Long productoId = orden.getProducto().getId().longValue();

        FormulaProducto formula = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(productoId, EstadoFormula.APROBADA)
                .orElseThrow(() -> new IllegalArgumentException("No existe una fórmula activa y aprobada para el producto"));

        Map<Long, BigDecimal> cantidadesEscaladas = new HashMap<>();
        List<InsumoFaltanteDTO> faltantes = new ArrayList<>();
        boolean stockSuficiente = true;
        Integer maxProducible = null;

        BigDecimal cantidadProgramada = orden.getCantidadProgramada();

        for (DetalleFormula insumo : formula.getDetalles()) {
            Long insumoId = insumo.getInsumo().getId().longValue();
            Producto productoInsumo = productoRepository.findById(insumoId)
                    // LÍNEA CODEx: posible N+1, se consulta cada insumo individualmente
                    .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado: ID " + insumoId));

            BigDecimal cantidadRequerida = insumo.getCantidadNecesaria().multiply(cantidadProgramada);
            cantidadesEscaladas.put(insumoId, cantidadRequerida);

            BigDecimal stockActual = productoInsumo.getStockActual(); // LÍNEA CODEx: stock sin discriminar estado de lotes

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

    public ResultadoValidacionOrdenDTO crearOrden(OrdenProduccionRequestDTO dto) {
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        Usuario responsable = usuarioRepository.findById(dto.getResponsableId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        OrdenProduccion orden = ordenProduccionMapper.toEntity(dto, producto, responsable);
        return guardarConValidacionStock(orden);
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

            if (dto.getCantidad() == null || dto.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_INVALIDA");
            }

            BigDecimal programada = orden.getCantidadProgramada();
            BigDecimal acumulada = Optional.ofNullable(orden.getCantidadProducidaAcumulada()).orElse(BigDecimal.ZERO);
            BigDecimal nuevaAcumulada = acumulada.add(dto.getCantidad());

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

            CierreProduccion cierre = ProduccionMapper.toEntity(dto, orden);
            cierreProduccionRepository.save(cierre);

            // Movimiento de inventario
            try {
                Almacen preBodega = almacenRepository.findByNombre("Pre-Bodega")
                        .orElseThrow(() -> new IllegalStateException("ALMACEN_PRE_BODEGA_NO_CONFIGURADO"));

                LoteProducto lote = loteProductoRepository
                        .findByCodigoLoteAndProductoIdAndAlmacenId(orden.getLoteProduccion(), orden.getProducto().getId(), preBodega.getId())
                        .orElseGet(() -> loteProductoRepository.save(LoteProducto.builder()
                                .codigoLote(orden.getLoteProduccion())
                                .producto(orden.getProducto())
                                .almacen(preBodega)
                                .stockLote(BigDecimal.ZERO)
                                .estado(com.willyes.clemenintegra.inventario.model.enums.EstadoLote.DISPONIBLE)
                                .ordenProduccion(orden)
                                .build()));

                Long tipoDetalleId = tipoMovimientoDetalleRepository
                        .findByDescripcion("ENTRADA_PARCIAL_PRODUCCION")
                        .map(t -> t.getId())
                        .orElse(null);

                movimientoInventarioService.registrarMovimiento(new MovimientoInventarioDTO(
                        null,
                        dto.getCantidad(),
                        TipoMovimiento.ENTRADA,
                        ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO,
                        orden.getCodigoOrden(),
                        orden.getProducto().getId(),
                        lote.getId(),
                        null,
                        preBodega.getId(),
                        null,
                        null,
                        null,
                        null,
                        tipoDetalleId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        com.willyes.clemenintegra.inventario.model.enums.EstadoLote.DISPONIBLE
                ));
            } catch (Exception e) {
                // En caso de error en movimiento, se registra pero no evita el cierre
            }

            return repository.save(orden);
        } catch (OptimisticLockException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ORDEN_CONFLICTO");
        }
    }

    public Page<CierreProduccionResponseDTO> listarCierres(Long id, Pageable pageable) {
        return cierreProduccionRepository.findByOrdenProduccionId(id, pageable)
                .map(ProduccionMapper::toResponse);
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
