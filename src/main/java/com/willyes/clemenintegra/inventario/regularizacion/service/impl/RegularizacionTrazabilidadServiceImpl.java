package com.willyes.clemenintegra.inventario.regularizacion.service.impl;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.regularizacion.dto.MovimientoCreadoDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadRequestDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadResponseDTO;
import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad;
import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidadDetalle;
import com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadDetalleRepository;
import com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadRepository;
import com.willyes.clemenintegra.inventario.regularizacion.service.RegularizacionTrazabilidadService;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegularizacionTrazabilidadServiceImpl implements RegularizacionTrazabilidadService {

    private static final long CATEGORIA_EMPAQUE_ID = 2L;
    private static final int ALMACEN_PRE_BODEGA = 6;
    private static final int ALMACEN_PRINCIPAL_EMPAQUE = 5;

    private final OrdenProduccionRepository ordenProduccionRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private final MovimientoInventarioService movimientoInventarioService;
    private final RegularizacionTrazabilidadRepository regularizacionRepository;
    private final RegularizacionTrazabilidadDetalleRepository detalleRepository;
    private final FormulaProductoRepository formulaProductoRepository;
    private final InventoryCatalogResolver inventoryCatalogResolver;

    @Override
    @Transactional
    public RegularizacionTrazabilidadResponseDTO regularizarPorOP(RegularizacionTrazabilidadRequestDTO request,
                                                                  String idempotencyKey,
                                                                  Usuario usuarioAuth) {
        if (usuarioAuth == null) throw new CustomBusinessException(ApiErrorCode.SESION_INVALIDA, "Usuario autenticado requerido");
        if (!StringUtils.hasText(idempotencyKey)) throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Idempotency-Key es obligatorio");

        Optional<RegularizacionTrazabilidad> existente = regularizacionRepository.findByIdempotencyKey(idempotencyKey);
        if (existente.isPresent()) {
            List<MovimientoCreadoDTO> movimientos = detalleRepository.findByRegularizacionIdOrderByIdAsc(existente.get().getId()).stream()
                    .map(d -> MovimientoCreadoDTO.builder()
                            .movimientoId(d.getMovimiento() != null ? d.getMovimiento().getId() : null)
                            .tipoMovimiento(d.getTipo())
                            .clasificacion(resolveClasificacion(d.getMovimiento() != null ? d.getMovimiento().getId() : null))
                            .loteProductoId(d.getLoteId())
                            .cantidad(d.getCantidad())
                            .build())
                    .toList();
            if (movimientos.isEmpty()) {
                throw new CustomBusinessException(ApiErrorCode.REGULARIZACION_IDEMPOTENTE_INCONSISTENTE,
                        "Idempotency-Key ya existe sin detalle de movimientos");
            }
            return RegularizacionTrazabilidadResponseDTO.builder()
                    .regularizacionId(existente.get().getId())
                    .idempotencyKey(idempotencyKey)
                    .ordenProduccionId(existente.get().getOrdenProduccionId())
                    .cantidadProgramada(existente.get().getCantidadProgramada())
                    .cantidadReal(existente.get().getCantidadReal())
                    .diferencia(existente.get().getDiferencia())
                    .movimientos(movimientos)
                    .registradoPorId(existente.get().getUsuario().getId())
                    .fecha(existente.get().getFechaIngreso())
                    .build();
        }

        boolean existeRegularizacionParaOp = regularizacionRepository.existsByOrdenProduccionId(request.ordenProduccionId());
        if (existeRegularizacionParaOp) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "La orden de producción ya tiene una regularización registrada");
        }

        OrdenProduccion op = ordenProduccionRepository.findById(request.ordenProduccionId())
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.ORDEN_PRODUCCION_NO_ENCONTRADA, "OP no encontrada"));

        BigDecimal programada = Optional.ofNullable(op.getCantidadProgramada()).orElse(BigDecimal.ZERO);
        BigDecimal diferencia = request.cantidadRealProducida().subtract(programada);

        List<MovimientoInventario> salidasOp = movimientoInventarioRepository
                .findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(op.getId(),
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA);
        Map<Long, List<MovimientoInventario>> consumosPorProducto = salidasOp.stream()
                .filter(m -> m.getProducto() != null && m.getProducto().getId() != null)
                .collect(Collectors.groupingBy(m -> m.getProducto().getId().longValue()));

        FormulaProducto formula = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(op.getProducto().getId().longValue(), EstadoFormula.APROBADA)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                        "No existe fórmula aprobada/activa para OP " + op.getId()));

        Map<Long, BigDecimal> deltaPorInsumo = new LinkedHashMap<>();
        for (DetalleFormula detalleFormula : Optional.ofNullable(formula.getDetalles()).orElse(List.of())) {
            if (detalleFormula.getInsumo() == null || detalleFormula.getInsumo().getId() == null
                    || detalleFormula.getCantidadNecesaria() == null
                    || detalleFormula.getInsumo().getCategoriaProducto() == null
                    || !Objects.equals(detalleFormula.getInsumo().getCategoriaProducto().getId(), CATEGORIA_EMPAQUE_ID)) {
                continue;
            }
            BigDecimal coef = detalleFormula.getCantidadNecesaria();
            BigDecimal consumoPlan = coef.multiply(programada);
            BigDecimal consumoReal = coef.multiply(request.cantidadRealProducida());
            BigDecimal delta = consumoPlan.subtract(consumoReal);
            if (delta.signum() != 0) {
                deltaPorInsumo.merge(detalleFormula.getInsumo().getId().longValue(), delta, BigDecimal::add);
            }
        }

        List<MovimientoPlan> planes = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> e : deltaPorInsumo.entrySet()) {
            Long productoId = e.getKey();
            BigDecimal deltaEmpaque = e.getValue();
            if (deltaEmpaque.signum() > 0) {
                planificarDevolucion(op.getId(), productoId, deltaEmpaque, consumosPorProducto, planes);
            } else if (deltaEmpaque.signum() < 0) {
                planificarConsumoAdicional(op.getId(), productoId, deltaEmpaque.abs(), consumosPorProducto, planes);
            }
        }

        boolean ajustoProductoResultado = false;
        if (diferencia.signum() != 0) {
            Producto productoResultado = op.getProducto();
            Long productoResultadoId = Optional.ofNullable(productoResultado)
                    .map(Producto::getId)
                    .map(Integer::longValue)
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                            "Producto resultado de OP no válido"));
            Long almacenProductoResultado = resolverAlmacenProductoResultado(productoResultado);
            MovimientoInventario entradaBase = movimientoInventarioRepository
                    .findFirstByOrdenProduccionIdAndProductoIdAndTipoMovimientoOrderByIdAsc(
                            op.getId(), productoResultadoId, TipoMovimiento.ENTRADA
                    )
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.REGULARIZACION_PT_SIN_ENTRADA_BASE,
                            "No existe movimiento de ENTRADA base para OP " + op.getId()));

            Long almacenLote = Optional.ofNullable(entradaBase.getLote())
                    .map(LoteProducto::getAlmacen)
                    .map(a -> a.getId().longValue())
                    .orElse(null);
            int almacenMovimiento = Math.toIntExact(almacenLote != null ? almacenLote : almacenProductoResultado);
            ClasificacionMovimientoInventario clasificacionProductoResultado =
                    resolverClasificacionRegularizacionProductoResultado(productoResultado);

            planes.add(new MovimientoPlan(
                    entradaBase.getProducto().getId().longValue(),
                    entradaBase.getLote().getId(),
                    diferencia.abs(),
                    diferencia.signum() < 0 ? TipoMovimiento.SALIDA : TipoMovimiento.ENTRADA,
                    clasificacionProductoResultado,
                    diferencia.signum() < 0 ? almacenMovimiento : null,
                    diferencia.signum() > 0 ? almacenMovimiento : null
            ));
            ajustoProductoResultado = true;
        }

        if (planes.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.REGULARIZACION_SIN_ACCIONES,
                    "No hay movimientos para regularizar OP " + op.getId());
        }

        RegularizacionTrazabilidad reg = regularizacionRepository.save(RegularizacionTrazabilidad.builder()
                .ordenProduccionId(op.getId())
                .cantidadProgramada(programada)
                .cantidadReal(request.cantidadRealProducida())
                .diferencia(diferencia)
                .ajustarPt(ajustoProductoResultado)
                .documentoReferencia(request.documentoReferencia())
                .observaciones(request.observaciones())
                .idempotencyKey(idempotencyKey)
                .usuario(usuarioAuth)
                .fechaIngreso(LocalDateTime.now())
                .build());

        List<MovimientoCreadoDTO> creados = new ArrayList<>();
        int sec = 0;
        for (MovimientoPlan plan : planes) {
            sec++;
            creados.add(crearMovimiento(idempotencyKey, sec, plan.productoId(), plan.loteId(), plan.cantidad(),
                    plan.tipo(), plan.clasificacion(), plan.almacenOrigenId(), plan.almacenDestinoId(), op.getId(), request));
        }

        recalcularCostoUnitarioLoteProducidoSiCierreTotal(op, request.cantidadRealProducida());

        return RegularizacionTrazabilidadResponseDTO.builder()
                .regularizacionId(reg.getId())
                .idempotencyKey(idempotencyKey)
                .ordenProduccionId(op.getId())
                .cantidadProgramada(programada)
                .cantidadReal(request.cantidadRealProducida())
                .diferencia(diferencia)
                .movimientos(creados)
                .registradoPorId(usuarioAuth.getId())
                .fecha(reg.getFechaIngreso())
                .build();
    }


    private void recalcularCostoUnitarioLoteProducidoSiCierreTotal(OrdenProduccion op, BigDecimal cantidadRealProducida) {
        if (op == null || op.getEstado() == null) {
            return;
        }
        if (op.getEstado() != EstadoProduccion.FINALIZADA && op.getEstado() != EstadoProduccion.CERRADA_INCOMPLETA) {
            return;
        }
        if (cantidadRealProducida == null || cantidadRealProducida.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        loteProductoRepository.findByOrdenProduccionIdAndProductoId(op.getId(), op.getProducto().getId().longValue())
                .ifPresent(lote -> {
                    BigDecimal costoTotalMaterialRealOp = Optional.ofNullable(
                                    movimientoInventarioRepository.sumarCostoMaterialRealOp(op.getId()))
                            .orElse(BigDecimal.ZERO)
                            .setScale(6, RoundingMode.HALF_UP);
                    BigDecimal costoUnitario = costoTotalMaterialRealOp
                            .divide(cantidadRealProducida, 6, RoundingMode.HALF_UP);
                    lote.setCostoTotalMaterialIngresado(costoTotalMaterialRealOp);
                    lote.setCostoUnitarioMaterial(costoUnitario);
                    loteProductoRepository.save(lote);
                });
    }

    private void planificarDevolucion(Long opId,
                                      Long productoId,
                                      BigDecimal cantidad,
                                      Map<Long, List<MovimientoInventario>> consumosPorProducto,
                                      List<MovimientoPlan> planes) {
        BigDecimal pendiente = cantidad;
        List<MovimientoInventario> consumos = new ArrayList<>(consumosPorProducto.getOrDefault(productoId, List.of()));
        consumos.sort(Comparator.comparing(MovimientoInventario::getFechaIngreso, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MovimientoInventario::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        if (consumos.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.REGULARIZACION_SIN_LOTE_CONSUMIDO,
                    "No hay consumo/lote para OP " + opId + " producto " + productoId);
        }

        for (MovimientoInventario consumo : consumos) {
            if (pendiente.signum() <= 0) break;
            if (consumo.getLote() == null || consumo.getLote().getId() == null || consumo.getCantidad() == null) continue;

            BigDecimal qty = consumo.getCantidad().min(pendiente);
            Integer origen = consumo.getAlmacenOrigen() != null ? Math.toIntExact(consumo.getAlmacenOrigen().getId()) : ALMACEN_PRE_BODEGA;
            Integer destino = resolveDestinoEmpaque(consumo, origen);

            planes.add(new MovimientoPlan(
                    productoId,
                    consumo.getLote().getId(),
                    qty,
                    TipoMovimiento.ENTRADA,
                    ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION,
                    origen,
                    destino
            ));
            pendiente = pendiente.subtract(qty);
        }

        if (pendiente.signum() > 0) {
            throw new CustomBusinessException(ApiErrorCode.REGULARIZACION_SIN_LOTE_CONSUMIDO,
                    "No hay consumo/lote suficiente para OP " + opId + " producto " + productoId);
        }
    }

    private void planificarConsumoAdicional(Long opId,
                                            Long productoId,
                                            BigDecimal cantidad,
                                            Map<Long, List<MovimientoInventario>> consumosPorProducto,
                                            List<MovimientoPlan> planes) {
        BigDecimal pendiente = cantidad;
        List<MovimientoInventario> consumos = new ArrayList<>(consumosPorProducto.getOrDefault(productoId, List.of()));
        consumos.sort(Comparator.comparing(MovimientoInventario::getFechaIngreso, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MovimientoInventario::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        for (MovimientoInventario consumo : consumos) {
            if (pendiente.signum() <= 0) break;
            if (consumo.getLote() == null || consumo.getLote().getId() == null || consumo.getCantidad() == null) continue;
            BigDecimal qty = consumo.getCantidad().min(pendiente);
            Integer origen = consumo.getAlmacenOrigen() != null ? Math.toIntExact(consumo.getAlmacenOrigen().getId()) : ALMACEN_PRE_BODEGA;
            planes.add(new MovimientoPlan(
                    productoId,
                    consumo.getLote().getId(),
                    qty,
                    TipoMovimiento.SALIDA,
                    ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD,
                    origen,
                    null
            ));
            pendiente = pendiente.subtract(qty);
        }

        if (pendiente.signum() > 0) {
            List<LoteProducto> fefoLotes = loteProductoRepository.findFefoByProductoAndAlmacen(productoId, ALMACEN_PRE_BODEGA);
            for (LoteProducto lote : fefoLotes) {
                if (pendiente.signum() <= 0) break;
                BigDecimal stock = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO)
                        .subtract(Optional.ofNullable(lote.getStockReservado()).orElse(BigDecimal.ZERO));
                if (stock.signum() <= 0) continue;
                BigDecimal qty = stock.min(pendiente);
                planes.add(new MovimientoPlan(
                        productoId,
                        lote.getId(),
                        qty,
                        TipoMovimiento.SALIDA,
                        ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD,
                        ALMACEN_PRE_BODEGA,
                        null
                ));
                pendiente = pendiente.subtract(qty);
            }
        }

        if (pendiente.signum() > 0) {
            throw new CustomBusinessException(ApiErrorCode.REGULARIZACION_SIN_LOTE_ELEGIBLE,
                    "No existe lote elegible para regularización OP " + opId + " producto " + productoId,
                    Map.of("ordenProduccionId", opId, "productoId", productoId));
        }
    }

    private int resolveDestinoEmpaque(MovimientoInventario consumo, Integer origen) {
        if (consumo.getAlmacenDestino() != null && consumo.getAlmacenDestino().getId() != null) {
            return Math.toIntExact(consumo.getAlmacenDestino().getId());
        }
        if (consumo.getAlmacenOrigen() != null && Objects.equals(consumo.getAlmacenOrigen().getId(), (long) ALMACEN_PRE_BODEGA)) {
            return ALMACEN_PRINCIPAL_EMPAQUE;
        }
        return origen != null ? origen : ALMACEN_PRE_BODEGA;
    }

    private String resolveClasificacion(Long movimientoId) {
        if (movimientoId == null) return null;
        return movimientoInventarioRepository.findById(movimientoId)
                .map(MovimientoInventario::getClasificacion)
                .map(Enum::name)
                .orElse(null);
    }

    private ClasificacionMovimientoInventario resolverClasificacionRegularizacionProductoResultado(Producto producto) {
        TipoCategoria tipo = Optional.ofNullable(producto)
                .map(Producto::getCategoriaProducto)
                .map(CategoriaProducto::getTipo)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                        "Producto resultado de OP sin categoría"));
        return switch (tipo) {
            case PRODUCTO_TERMINADO -> ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD_PT;
            case PRODUCTO_SEMI_ELABORADO -> ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD_PS;
            default -> throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "La regularización solo aplica para PT/PS. Tipo actual: " + tipo);
        };
    }

    private Long resolverAlmacenProductoResultado(Producto producto) {
        TipoCategoria tipo = Optional.ofNullable(producto)
                .map(Producto::getCategoriaProducto)
                .map(CategoriaProducto::getTipo)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                        "Producto resultado de OP sin categoría"));
        return switch (tipo) {
            case PRODUCTO_TERMINADO -> inventoryCatalogResolver.getAlmacenPtId();
            case PRODUCTO_SEMI_ELABORADO -> inventoryCatalogResolver.getAlmacenOrigenProductoSemiElaboradoId();
            default -> throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "No existe almacén configurado para tipo de producto resultado: " + tipo);
        };
    }

    private MovimientoCreadoDTO crearMovimiento(String idem, int sec, Long productoId, Long loteId, BigDecimal cantidad,
                                                TipoMovimiento tipo, ClasificacionMovimientoInventario clasificacion,
                                                Integer almacenOrigenId, Integer almacenDestinoId,
                                                Long opId, RegularizacionTrazabilidadRequestDTO request) {
        MotivoMovimiento motivo = motivoMovimientoRepository.findByMotivo(clasificacion)
                .orElseGet(() -> motivoMovimientoRepository.findByMotivo(
                                tipo == TipoMovimiento.SALIDA ? ClasificacionMovimientoInventario.SALIDA_PRODUCCION : ClasificacionMovimientoInventario.AJUSTE_POSITIVO)
                        .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.CATALOGO_FALTANTE, "Motivo no configurado")));
        TipoMovimientoDetalle detalle = tipoMovimientoDetalleRepository.findByDescripcion(clasificacion.name())
                .orElseGet(() -> tipoMovimientoDetalleRepository.findByDescripcion(tipo == TipoMovimiento.SALIDA ? "SALIDA_PRODUCCION" : "AJUSTE_POSITIVO")
                        .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.CATALOGO_FALTANTE, "Tipo detalle no configurado")));

        MovimientoInventarioResponseDTO creado = movimientoInventarioService.registrarMovimiento(new MovimientoInventarioDTO(
                null, cantidad, tipo, clasificacion, request.documentoReferencia(), request.observaciones(),
                null, null, null, Math.toIntExact(productoId), loteId,
                almacenOrigenId, almacenDestinoId, null, null,
                motivo.getId(), detalle.getId(), null, null, opId,
                null, null, null, null, null, null, null, null, null
        ), idem + ":" + sec);

        detalleRepository.save(RegularizacionTrazabilidadDetalle.builder()
                .regularizacion(regularizacionRepository.findByIdempotencyKey(idem).orElseThrow())
                .productoId(productoId)
                .loteId(loteId)
                .cantidad(cantidad)
                .tipo(tipo.name())
                .almacenOrigenId(almacenOrigenId)
                .almacenDestinoId(almacenDestinoId)
                .movimiento(MovimientoInventario.builder().id(creado.getId()).build())
                .build());

        log.info("REGULARIZACION_TRAZABILIDAD movId={} opId={} producto={} lote={} qty={} idem={}", creado.getId(), opId, productoId, loteId, cantidad, idem);
        return MovimientoCreadoDTO.builder().movimientoId(creado.getId()).tipoMovimiento(tipo.name())
                .clasificacion(clasificacion.name()).loteProductoId(loteId).cantidad(cantidad).build();
    }

    private record MovimientoPlan(Long productoId,
                                  Long loteId,
                                  BigDecimal cantidad,
                                  TipoMovimiento tipo,
                                  ClasificacionMovimientoInventario clasificacion,
                                  Integer almacenOrigenId,
                                  Integer almacenDestinoId) {
    }
}
