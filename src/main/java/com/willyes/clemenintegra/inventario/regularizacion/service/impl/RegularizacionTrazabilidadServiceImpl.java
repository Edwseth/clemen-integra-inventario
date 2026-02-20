package com.willyes.clemenintegra.inventario.regularizacion.service.impl;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
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
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
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
    private static final int ALMACEN_PT = 2;

    private final OrdenProduccionRepository ordenProduccionRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private final MovimientoInventarioService movimientoInventarioService;
    private final RegularizacionTrazabilidadRepository regularizacionRepository;
    private final RegularizacionTrazabilidadDetalleRepository detalleRepository;

    @Override
    @Transactional
    public RegularizacionTrazabilidadResponseDTO regularizarPorOP(RegularizacionTrazabilidadRequestDTO request,
                                                                  String idempotencyKey,
                                                                  Usuario usuarioAuth) {
        if (usuarioAuth == null) throw new CustomBusinessException(ApiErrorCode.SESION_INVALIDA, "Usuario autenticado requerido");
        if (!StringUtils.hasText(idempotencyKey)) throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Idempotency-Key es obligatorio");

        Optional<RegularizacionTrazabilidad> existente = regularizacionRepository.findByIdempotencyKey(idempotencyKey);
        if (existente.isPresent()) {
            return RegularizacionTrazabilidadResponseDTO.builder()
                    .regularizacionId(existente.get().getId())
                    .idempotencyKey(idempotencyKey)
                    .ordenProduccionId(existente.get().getOrdenProduccionId())
                    .cantidadProgramada(existente.get().getCantidadProgramada())
                    .cantidadReal(existente.get().getCantidadReal())
                    .diferencia(existente.get().getDiferencia())
                    .movimientos(List.of())
                    .registradoPorId(existente.get().getUsuario().getId())
                    .fecha(existente.get().getFechaIngreso())
                    .build();
        }

        OrdenProduccion op = ordenProduccionRepository.findById(request.ordenProduccionId())
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.ORDEN_PRODUCCION_NO_ENCONTRADA, "OP no encontrada"));

        BigDecimal programada = Optional.ofNullable(op.getCantidadProgramada()).orElse(BigDecimal.ZERO);
        BigDecimal diferencia = request.cantidadRealProducida().subtract(programada);

        RegularizacionTrazabilidad reg = regularizacionRepository.save(RegularizacionTrazabilidad.builder()
                .ordenProduccionId(op.getId())
                .cantidadProgramada(programada)
                .cantidadReal(request.cantidadRealProducida())
                .diferencia(diferencia)
                .ajustarPt(Boolean.TRUE.equals(request.ajustarProductoTerminado()))
                .documentoReferencia(request.documentoReferencia())
                .observaciones(request.observaciones())
                .idempotencyKey(idempotencyKey)
                .usuario(usuarioAuth)
                .fechaIngreso(LocalDateTime.now())
                .build());

        List<MovimientoInventario> salidasEmpaque = movimientoInventarioRepository
                .findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(op.getId(),
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA)
                .stream()
                .filter(m -> m.getProducto() != null && m.getProducto().getCategoriaProducto() != null
                        && Objects.equals(m.getProducto().getCategoriaProducto().getId(), CATEGORIA_EMPAQUE_ID))
                .toList();

        Map<Long, List<MovimientoInventario>> porProducto = salidasEmpaque.stream().collect(Collectors.groupingBy(m -> m.getProducto().getId().longValue()));

        List<MovimientoCreadoDTO> creados = new ArrayList<>();
        int sec = 0;
        for (Map.Entry<Long, List<MovimientoInventario>> e : porProducto.entrySet()) {
            Long productoId = e.getKey();
            BigDecimal pendiente = diferencia.abs();
            if (diferencia.signum() < 0) {
                List<MovimientoInventario> lifo = new ArrayList<>(e.getValue());
                lifo.sort(Comparator.comparing(MovimientoInventario::getFechaIngreso).reversed().thenComparing(MovimientoInventario::getId).reversed());
                for (MovimientoInventario consumo : lifo) {
                    if (pendiente.signum() <= 0) break;
                    BigDecimal qty = consumo.getCantidad().min(pendiente);
                    sec++;
                    creados.add(crearMovimiento(idempotencyKey, sec, productoId, consumo.getLote().getId(), qty,
                            TipoMovimiento.TRANSFERENCIA, ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD,
                            ALMACEN_PRE_BODEGA, resolveDestinoEmpaque(consumo), op.getId(), request));
                    pendiente = pendiente.subtract(qty);
                }
            } else if (diferencia.signum() > 0) {
                List<LoteProducto> fefo = loteProductoRepository.findFefoByProductoAndAlmacen(productoId, ALMACEN_PRE_BODEGA);
                for (LoteProducto lote : fefo) {
                    if (pendiente.signum() <= 0) break;
                    BigDecimal disponible = lote.getStockLote().subtract(Optional.ofNullable(lote.getStockReservado()).orElse(BigDecimal.ZERO));
                    if (disponible.signum() <= 0) continue;
                    BigDecimal qty = disponible.min(pendiente);
                    sec++;
                    creados.add(crearMovimiento(idempotencyKey, sec, productoId, lote.getId(), qty,
                            TipoMovimiento.SALIDA, ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD,
                            ALMACEN_PRE_BODEGA, null, op.getId(), request));
                    pendiente = pendiente.subtract(qty);
                }
            }
        }

        if (Boolean.TRUE.equals(request.ajustarProductoTerminado()) && diferencia.signum() != 0) {
            final int secFinal = sec;
            movimientoInventarioRepository.findFirstByOrdenProduccionIdAndTipoMovimientoAndClasificacionOrderByIdAsc(
                    op.getId(), TipoMovimiento.ENTRADA, ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO
            ).ifPresent(ptEntrada -> {
                Long pId = ptEntrada.getProducto().getId().longValue();
                Long loteId = ptEntrada.getLote().getId();
                int secPt = secFinal + 1;
                MovimientoCreadoDTO pt = crearMovimiento(idempotencyKey, secPt, pId, loteId, diferencia.abs(),
                        diferencia.signum() < 0 ? TipoMovimiento.SALIDA : TipoMovimiento.ENTRADA,
                        ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD_PT,
                        diferencia.signum() < 0 ? ALMACEN_PT : null,
                        diferencia.signum() > 0 ? ALMACEN_PT : null,
                        op.getId(), request);
                creados.add(pt);
            });
        }

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

    private int resolveDestinoEmpaque(MovimientoInventario consumo) {
        if (consumo.getAlmacenOrigen() != null && Objects.equals(consumo.getAlmacenOrigen().getId(), (long) ALMACEN_PRE_BODEGA)) {
            return ALMACEN_PRINCIPAL_EMPAQUE;
        }
        return ALMACEN_PRINCIPAL_EMPAQUE;
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
}
