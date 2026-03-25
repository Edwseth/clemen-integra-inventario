package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.RetencionLoteRepository;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.ReservaLote;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import com.willyes.clemenintegra.produccion.dto.BatchRecordDecisionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.BatchRecordDTO;
import com.willyes.clemenintegra.produccion.model.CierreProduccion;
import com.willyes.clemenintegra.produccion.model.ControlEmpaqueLote;
import com.willyes.clemenintegra.produccion.model.ControlProcesoProduccion;
import com.willyes.clemenintegra.produccion.model.ObservacionProceso;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.ChecklistEtapaItem;
import com.willyes.clemenintegra.produccion.model.enums.EstadoChecklistItem;
import com.willyes.clemenintegra.produccion.model.enums.EstadoBatchRecord;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.ControlEmpaqueLoteRepository;
import com.willyes.clemenintegra.produccion.repository.ControlProcesoProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.ObservacionProcesoRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.ChecklistEtapaItemRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BatchRecordServiceImpl implements BatchRecordService {

    private final OrdenProduccionRepository ordenProduccionRepository;
    private final FormulaProductoRepository formulaProductoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ReservaLoteRepository reservaLoteRepository;
    private final CierreProduccionRepository cierreProduccionRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final EvaluacionCalidadRepository evaluacionCalidadRepository;
    private final RetencionLoteRepository retencionLoteRepository;
    private final ControlProcesoProduccionRepository controlProcesoProduccionRepository;
    private final ControlEmpaqueLoteRepository controlEmpaqueLoteRepository;
    private final ObservacionProcesoRepository observacionProcesoRepository;
    private final EtapaProduccionRepository etapaProduccionRepository;
    private final ChecklistEtapaItemRepository checklistEtapaItemRepository;

    @Override
    @Transactional
    public void decidirBatchRecord(Long ordenProduccionId,
                                   BatchRecordDecisionRequestDTO request,
                                   Authentication auth) {
        OrdenProduccion ordenProduccion = ordenProduccionRepository.findById(ordenProduccionId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "ORDEN_NO_ENCONTRADA"));

        if (ordenProduccion.getBatchRecordEstado() == EstadoBatchRecord.APROBADO) {
            // No se permite modificar un batch record ya aprobado
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA, "BATCH_RECORD_YA_APROBADO");
        }

        if (request == null || request.getDecision() == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "DECISION_BATCH_RECORD_REQUERIDA");
        }

        EstadoBatchRecord estadoActual = ordenProduccion.getBatchRecordEstado();
        if (estadoActual != null
                && estadoActual != EstadoBatchRecord.BORRADOR
                && estadoActual != EstadoBatchRecord.EN_REVISION_CALIDAD
                && estadoActual != EstadoBatchRecord.RECHAZADO) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA,
                    "El estado actual del batch record no permite registrar una decisión");
        }

        Usuario usuario = obtenerUsuarioDesdeAuth(auth);
        if (request.getDecision() == EstadoBatchRecord.RECHAZADO
                && (request.getObservacionesCalidad() == null || request.getObservacionesCalidad().isBlank())) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Las observaciones de calidad son obligatorias al rechazar el batch record");
        }

        ordenProduccion.setBatchRecordEstado(request.getDecision());
        ordenProduccion.setBatchRecordRevisadoPor(usuario);
        ordenProduccion.setBatchRecordFechaRevision(LocalDateTime.now());
        ordenProduccion.setBatchRecordObservacionesCalidad(request.getObservacionesCalidad());

        ordenProduccionRepository.save(ordenProduccion);
        log.info("Batch Record de OP {} marcado como {} por {}", ordenProduccionId, request.getDecision(),
                usuario != null ? usuario.getNombreCompleto() : "usuario-desconocido");
    }

    @Override
    public BatchRecordDTO buildByOrdenProduccion(Long ordenProduccionId) {
        log.info("Generando Batch Record para orden de producción {}", ordenProduccionId);
        OrdenProduccion ordenProduccion = ordenProduccionRepository.findById(ordenProduccionId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "ORDEN_NO_ENCONTRADA"));

        BatchRecordDTO dto = new BatchRecordDTO();
        dto.op = mapOp(ordenProduccion);
        dto.formula = mapFormula(ordenProduccion);
        dto.consumos = mapConsumos(ordenProduccion);
        dto.reservas = mapReservas(ordenProduccionId);
        dto.cierres = mapCierres(ordenProduccionId);
        dto.produccionFinal = mapProduccionFinal(ordenProduccion, dto.cierres);
        dto.loteProductoTerminado = mapLoteProductoTerminado(ordenProduccion);
        dto.calidad = mapCalidad(dto.loteProductoTerminado);
        dto.controlesProceso = mapControlesProceso(ordenProduccionId);
        dto.controlesEmpaque = mapControlesEmpaque(ordenProduccionId);
        dto.observacionesProceso = mapObservaciones(ordenProduccionId);
        dto.checklistEtapas = mapChecklistEtapas(ordenProduccionId);
        dto.estadoBatchRecord = ordenProduccion.getBatchRecordEstado();
        dto.revisadoPorNombre = ordenProduccion.getBatchRecordRevisadoPor() != null
                ? ordenProduccion.getBatchRecordRevisadoPor().getNombreCompleto()
                : null;
        dto.fechaRevision = ordenProduccion.getBatchRecordFechaRevision();
        dto.observacionesCalidad = ordenProduccion.getBatchRecordObservacionesCalidad();
        return dto;
    }

    private BatchRecordDTO.OpDTO mapOp(OrdenProduccion orden) {
        BatchRecordDTO.OpDTO opDTO = new BatchRecordDTO.OpDTO();
        opDTO.id = orden.getId();
        opDTO.codigoOrden = orden.getCodigoOrden();
        opDTO.loteProduccion = orden.getLoteProduccion();
        if (orden.getProducto() != null) {
            opDTO.productoId = orden.getProducto().getId() != null ? orden.getProducto().getId().longValue() : null;
            opDTO.productoNombre = orden.getProducto().getNombre();
            opDTO.codigoSku = orden.getProducto().getCodigoSku();
            opDTO.presentacion = orden.getUnidadMedida() != null ? orden.getUnidadMedida().getNombre() : null;
        }
        opDTO.cantidadProgramada = orden.getCantidadProgramada() != null ? orden.getCantidadProgramada().intValue() : null;
        opDTO.cantidadProducida = orden.getCantidadProducida() != null ? orden.getCantidadProducida().intValue() : null;
        opDTO.porcentajeCumplimiento = orden.getPorcentajeCumplimiento();
        opDTO.fechaInicio = orden.getFechaInicio();
        opDTO.fechaFin = orden.getFechaFin();
        opDTO.estado = orden.getEstado() != null ? orden.getEstado().name() : null;
        opDTO.responsableNombre = orden.getResponsable() != null ? orden.getResponsable().getNombreCompleto() : null;
        return opDTO;
    }

    private BatchRecordDTO.FormulaDTO mapFormula(OrdenProduccion ordenProduccion) {
        BatchRecordDTO.FormulaDTO formulaDTO = new BatchRecordDTO.FormulaDTO();
        List<BatchRecordDTO.DetalleFormulaDTO> detalles = new ArrayList<>();
        if (ordenProduccion.getProducto() == null) {
            formulaDTO.detalles = detalles;
            return formulaDTO;
        }
        FormulaProducto formula = obtenerFormulaProducto(ordenProduccion.getProducto());
        if (formula == null) {
            log.warn("No se encontró fórmula activa para el producto {}", ordenProduccion.getProducto().getId());
            formulaDTO.detalles = detalles;
            return formulaDTO;
        }
        formulaDTO.version = formula.getVersion();
        BigDecimal cantidadProgramada = ordenProduccion.getCantidadProgramada() != null
                ? ordenProduccion.getCantidadProgramada()
                : BigDecimal.ONE;
        if (formula.getDetalles() != null) {
            for (DetalleFormula detalle : formula.getDetalles()) {
                if (esProductoSemiElaborado(detalle)) {
                    boolean expandido = expandirProductoSemiElaborado(detalle, cantidadProgramada, detalles);
                    if (expandido) {
                        continue;
                    }
                }
                detalles.add(crearDetalleFormulaDTO(detalle, calcularCantidadTeoricaPt(detalle.getCantidadNecesaria(),
                        cantidadProgramada)));
            }
        }
        formulaDTO.detalles = detalles;
        return formulaDTO;
    }

    private boolean expandirProductoSemiElaborado(DetalleFormula detallePs,
                                                  BigDecimal cantidadProgramada,
                                                  List<BatchRecordDTO.DetalleFormulaDTO> detalles) {
        if (detallePs.getInsumo() == null || detallePs.getInsumo().getId() == null) {
            return false;
        }
        FormulaProducto formulaPs = obtenerFormulaProducto(detallePs.getInsumo());
        if (formulaPs == null || formulaPs.getDetalles() == null || formulaPs.getDetalles().isEmpty()) {
            return false;
        }
        for (DetalleFormula detalleMp : formulaPs.getDetalles()) {
            BigDecimal cantidadTeorica = calcularCantidadTeoricaPs(detalleMp.getCantidadNecesaria(),
                    detallePs.getCantidadNecesaria(), cantidadProgramada);
            detalles.add(crearDetalleFormulaDTO(detalleMp, cantidadTeorica));
        }
        return true;
    }

    private BigDecimal calcularCantidadTeoricaPs(BigDecimal cantidadMpPorPs,
                                                 BigDecimal cantidadPsPorPt,
                                                 BigDecimal cantidadProgramadaPt) {
        if (cantidadMpPorPs == null) {
            return null;
        }
        BigDecimal psPorPt = cantidadPsPorPt != null ? cantidadPsPorPt : BigDecimal.ZERO;
        BigDecimal cantidadPt = cantidadProgramadaPt != null ? cantidadProgramadaPt : BigDecimal.ONE;
        return cantidadMpPorPs.multiply(psPorPt).multiply(cantidadPt);
    }

    private BigDecimal calcularCantidadTeoricaPt(BigDecimal cantidadNecesariaPorUnidad, BigDecimal cantidadProgramadaPt) {
        if (cantidadNecesariaPorUnidad == null) {
            return null;
        }
        BigDecimal cantidadPt = cantidadProgramadaPt != null ? cantidadProgramadaPt : BigDecimal.ONE;
        return cantidadNecesariaPorUnidad.multiply(cantidadPt);
    }

    private BatchRecordDTO.DetalleFormulaDTO crearDetalleFormulaDTO(DetalleFormula detalle, BigDecimal cantidadNecesaria) {
        BatchRecordDTO.DetalleFormulaDTO detalleDTO = new BatchRecordDTO.DetalleFormulaDTO();
        // Mostrar todos los insumos declarados en el BOM, incluso los que no generan reservas (p.ej. SIN_CONTROL_STOCK)
        detalleDTO.insumoId = detalle.getInsumo() != null ? detalle.getInsumo().getId().longValue() : null;
        detalleDTO.codigoSku = detalle.getInsumo() != null ? detalle.getInsumo().getCodigoSku() : null;
        detalleDTO.nombre = detalle.getInsumo() != null ? detalle.getInsumo().getNombre() : null;
        detalleDTO.unidad = detalle.getUnidadMedida() != null ? detalle.getUnidadMedida().getNombre() : null;
        detalleDTO.cantidadNecesaria = cantidadNecesaria;
        detalleDTO.obligatorio = Boolean.TRUE.equals(detalle.getObligatorio());
        return detalleDTO;
    }

    private FormulaProducto obtenerFormulaProducto(Producto producto) {
        if (producto == null || producto.getId() == null) {
            return null;
        }
        Optional<FormulaProducto> formulaOpt = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(producto.getId().longValue(), EstadoFormula.APROBADA);
        return formulaOpt.orElseGet(() -> formulaProductoRepository
                .findByProductoId(producto.getId().longValue()).orElse(null));
    }

    private boolean esProductoSemiElaborado(DetalleFormula detalle) {
        return detalle != null
                && detalle.getInsumo() != null
                && detalle.getInsumo().getCategoriaProducto() != null
                && detalle.getInsumo().getCategoriaProducto().getTipo() == TipoCategoria.PRODUCTO_SEMI_ELABORADO;
    }

    private List<BatchRecordDTO.ConsumoDTO> mapConsumos(OrdenProduccion ordenProduccion) {
        Long ordenProduccionId = ordenProduccion != null ? ordenProduccion.getId() : null;
        List<MovimientoInventario> movimientos = movimientoInventarioRepository
                .findByOrdenProduccionIdAndClasificacion(
                        ordenProduccionId,
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                        Pageable.unpaged())
                .getContent();
        List<BatchRecordDTO.ConsumoDTO> consumos = new ArrayList<>();
        List<MovimientoInventario> consumosPs = new ArrayList<>();

        List<Long> productosSemiElaborados = obtenerProductosSemiElaboradosEnFormula(ordenProduccion);
        for (MovimientoInventario movimiento : movimientos) {
            // Solo se consideran consumos reales de producción (SALIDA_PRODUCCION desde Pre-Bodega)
            if (movimiento.getTipoMovimiento() != TipoMovimiento.SALIDA) {
                continue;
            }
            if (esMovimientoDePs(movimiento, productosSemiElaborados)) {
                consumosPs.add(movimiento);
                continue;
            }
            consumos.add(crearConsumoDtoDesdeMovimiento(movimiento));
        }

        for (MovimientoInventario movimientoPs : consumosPs) {
            List<BatchRecordDTO.ConsumoDTO> consumosExpandido = expandirConsumoPs(movimientoPs);
            if (consumosExpandido == null) {
                consumos.add(crearConsumoDtoDesdeMovimiento(movimientoPs));
                continue;
            }
            consumos.addAll(consumosExpandido);
        }
        return consumos;
    }

    private List<BatchRecordDTO.ConsumoDTO> expandirConsumoPs(MovimientoInventario movimientoPs) {
        if (movimientoPs == null || movimientoPs.getProducto() == null || movimientoPs.getProducto().getId() == null) {
            return null;
        }

        Long opOrigenPsId = obtenerOrdenProduccionOrigenDelPs(movimientoPs);
        if (opOrigenPsId == null) {
            return expandirConsumoPsPorFormula(movimientoPs);
        }

        List<MovimientoInventario> movimientosPs = movimientoInventarioRepository
                .findByOrdenProduccionIdAndClasificacion(
                        opOrigenPsId,
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                        Pageable.unpaged())
                .getContent();

        List<BatchRecordDTO.ConsumoDTO> consumos = new ArrayList<>();
        for (MovimientoInventario movimientoPsHijo : movimientosPs) {
            if (movimientoPsHijo.getTipoMovimiento() != TipoMovimiento.SALIDA) {
                continue;
            }
            BatchRecordDTO.ConsumoDTO consumoDTO = crearConsumoDtoDesdeMovimiento(movimientoPsHijo);
            consumoDTO.fromPs = true;
            consumoDTO.psDescripcion = movimientoPs.getProducto() != null ? movimientoPs.getProducto().getNombre() : null;
            consumoDTO.psCodigoSku = movimientoPs.getProducto() != null ? movimientoPs.getProducto().getCodigoSku() : null;
            consumos.add(consumoDTO);
        }
        return consumos.isEmpty() ? null : consumos;
    }

    private Long obtenerOrdenProduccionOrigenDelPs(MovimientoInventario movimientoPs) {
        if (movimientoPs == null || movimientoPs.getLote() == null) {
            return null;
        }
        return movimientoPs.getLote().getOrdenProduccion() != null
                ? movimientoPs.getLote().getOrdenProduccion().getId()
                : null;
    }

    private List<BatchRecordDTO.ConsumoDTO> expandirConsumoPsPorFormula(MovimientoInventario movimientoPs) {
        FormulaProducto formulaPs = obtenerFormulaProducto(movimientoPs.getProducto());
        if (formulaPs == null || formulaPs.getDetalles() == null || formulaPs.getDetalles().isEmpty()) {
            return null;
        }
        List<BatchRecordDTO.ConsumoDTO> consumos = new ArrayList<>();
        for (DetalleFormula detalle : formulaPs.getDetalles()) {
            BigDecimal cantidadTeorica = calcularCantidadDesdeConsumoPs(movimientoPs.getCantidad(), detalle.getCantidadNecesaria());
            BatchRecordDTO.ConsumoDTO consumoDTO = crearConsumoDesdeDetallePs(detalle, movimientoPs, cantidadTeorica);
            consumos.add(consumoDTO);
        }
        return consumos.isEmpty() ? null : consumos;
    }

    private List<Long> obtenerProductosSemiElaboradosEnFormula(OrdenProduccion ordenProduccion) {
        List<Long> productosPs = new ArrayList<>();
        if (ordenProduccion == null || ordenProduccion.getProducto() == null) {
            return productosPs;
        }
        FormulaProducto formulaPt = obtenerFormulaProducto(ordenProduccion.getProducto());
        if (formulaPt == null || formulaPt.getDetalles() == null) {
            return productosPs;
        }
        for (DetalleFormula detalle : formulaPt.getDetalles()) {
            if (detalle.getInsumo() != null
                    && detalle.getInsumo().getId() != null
                    && esProductoSemiElaborado(detalle)) {
                productosPs.add(detalle.getInsumo().getId().longValue());
            }
        }
        return productosPs;
    }

    private boolean esMovimientoDePs(MovimientoInventario movimiento, List<Long> productosPs) {
        if (movimiento == null || movimiento.getProducto() == null || movimiento.getProducto().getId() == null) {
            return false;
        }
        return productosPs.contains(movimiento.getProducto().getId().longValue());
    }

    private BatchRecordDTO.ConsumoDTO crearConsumoDtoDesdeMovimiento(MovimientoInventario movimiento) {
        BatchRecordDTO.ConsumoDTO consumoDTO = new BatchRecordDTO.ConsumoDTO();
        consumoDTO.movimientoId = movimiento.getId();
        consumoDTO.etapaId = movimiento.getOrdenProduccionEtapa() != null ? movimiento.getOrdenProduccionEtapa().getId() : null;
        consumoDTO.tipoMovimiento = movimiento.getTipoMovimiento() != null ? movimiento.getTipoMovimiento().name() : null;
        consumoDTO.clasificacionMovimiento = movimiento.getClasificacion() != null ? movimiento.getClasificacion().name() : null;
        consumoDTO.productoId = movimiento.getProducto() != null ? movimiento.getProducto().getId().longValue() : null;
        consumoDTO.codigoSku = movimiento.getProducto() != null ? movimiento.getProducto().getCodigoSku() : null;
        consumoDTO.nombreProducto = movimiento.getProducto() != null ? movimiento.getProducto().getNombre() : null;
        consumoDTO.loteId = movimiento.getLote() != null ? movimiento.getLote().getId() : null;
        consumoDTO.codigoLote = movimiento.getLote() != null ? movimiento.getLote().getCodigoLote() : null;
        consumoDTO.almacenOrigen = movimiento.getAlmacenOrigen() != null ? movimiento.getAlmacenOrigen().getNombre() : null;
        consumoDTO.cantidad = movimiento.getCantidad();
        consumoDTO.unidad = movimiento.getProducto() != null && movimiento.getProducto().getUnidadMedida() != null
                ? movimiento.getProducto().getUnidadMedida().getNombre()
                : null;
        consumoDTO.fechaMovimiento = movimiento.getFechaIngreso();
        return consumoDTO;
    }

    private BatchRecordDTO.ConsumoDTO crearConsumoDesdeDetallePs(DetalleFormula detalle,
                                                                 MovimientoInventario movimientoPs,
                                                                 BigDecimal cantidadTeorica) {
        BatchRecordDTO.ConsumoDTO consumoDTO = new BatchRecordDTO.ConsumoDTO();
        consumoDTO.movimientoId = movimientoPs.getId();
        consumoDTO.etapaId = movimientoPs.getOrdenProduccionEtapa() != null ? movimientoPs.getOrdenProduccionEtapa().getId() : null;
        consumoDTO.tipoMovimiento = movimientoPs.getTipoMovimiento() != null ? movimientoPs.getTipoMovimiento().name() : null;
        consumoDTO.clasificacionMovimiento = movimientoPs.getClasificacion() != null ? movimientoPs.getClasificacion().name() : null;
        consumoDTO.productoId = detalle.getInsumo() != null ? detalle.getInsumo().getId().longValue() : null;
        consumoDTO.codigoSku = detalle.getInsumo() != null ? detalle.getInsumo().getCodigoSku() : null;
        consumoDTO.nombreProducto = detalle.getInsumo() != null ? detalle.getInsumo().getNombre() : null;
        consumoDTO.loteId = movimientoPs.getLote() != null ? movimientoPs.getLote().getId() : null;
        consumoDTO.codigoLote = movimientoPs.getLote() != null ? movimientoPs.getLote().getCodigoLote() : null;
        consumoDTO.almacenOrigen = movimientoPs.getAlmacenOrigen() != null ? movimientoPs.getAlmacenOrigen().getNombre() : null;
        consumoDTO.cantidad = cantidadTeorica;
        consumoDTO.unidad = detalle.getUnidadMedida() != null
                ? detalle.getUnidadMedida().getNombre()
                : movimientoPs.getProducto() != null && movimientoPs.getProducto().getUnidadMedida() != null
                ? movimientoPs.getProducto().getUnidadMedida().getNombre()
                : null;
        consumoDTO.fechaMovimiento = movimientoPs.getFechaIngreso();
        consumoDTO.fromPs = true;
        consumoDTO.psDescripcion = movimientoPs.getProducto() != null ? movimientoPs.getProducto().getNombre() : null;
        consumoDTO.psCodigoSku = movimientoPs.getProducto() != null ? movimientoPs.getProducto().getCodigoSku() : null;
        return consumoDTO;
    }

    private BigDecimal calcularCantidadDesdeConsumoPs(BigDecimal cantidadPsConsumida, BigDecimal cantidadDetalle) {
        if (cantidadPsConsumida == null || cantidadDetalle == null) {
            return null;
        }
        return cantidadPsConsumida.multiply(cantidadDetalle);
    }

    private List<BatchRecordDTO.ReservaDTO> mapReservas(Long ordenProduccionId) {
        List<ReservaLote> reservas = reservaLoteRepository
                .findBySolicitudMovimientoDetalle_SolicitudMovimiento_OrdenProduccionId(ordenProduccionId);
        List<BatchRecordDTO.ReservaDTO> resultado = new ArrayList<>();
        for (ReservaLote reserva : reservas) {
            BatchRecordDTO.ReservaDTO reservaDTO = new BatchRecordDTO.ReservaDTO();
            if (reserva.getSolicitudMovimientoDetalle() != null
                    && reserva.getSolicitudMovimientoDetalle().getSolicitudMovimiento() != null
                    && reserva.getSolicitudMovimientoDetalle().getSolicitudMovimiento().getProducto() != null) {
                reservaDTO.insumoId = reserva.getSolicitudMovimientoDetalle().getSolicitudMovimiento().getProducto().getId().longValue();
            }
            reservaDTO.loteId = reserva.getLote() != null ? reserva.getLote().getId() : null;
            reservaDTO.codigoLote = reserva.getLote() != null ? reserva.getLote().getCodigoLote() : null;
            reservaDTO.cantidadReservada = reserva.getCantidadReservada();
            reservaDTO.cantidadConsumida = reserva.getCantidadConsumida();
            reservaDTO.estado = reserva.getEstado() != null ? reserva.getEstado().name() : null;
            resultado.add(reservaDTO);
        }
        return resultado;
    }

    private List<BatchRecordDTO.CierreDTO> mapCierres(Long ordenProduccionId) {
        List<CierreProduccion> cierres = cierreProduccionRepository
                .findByOrdenProduccionId(ordenProduccionId, Pageable.unpaged())
                .getContent();
        List<BatchRecordDTO.CierreDTO> respuesta = new ArrayList<>();
        for (CierreProduccion cierre : cierres) {
            BatchRecordDTO.CierreDTO cierreDTO = new BatchRecordDTO.CierreDTO();
            cierreDTO.id = cierre.getId();
            cierreDTO.tipo = cierre.getTipo() != null ? cierre.getTipo().name() : null;
            cierreDTO.cantidad = cierre.getCantidad() != null ? cierre.getCantidad().intValue() : null;
            cierreDTO.turno = cierre.getTurno();
            cierreDTO.observacion = cierre.getObservacion();
            cierreDTO.fechaCierre = cierre.getFechaCierre();
            cierreDTO.usuarioNombre = cierre.getUsuarioNombre();
            respuesta.add(cierreDTO);
        }
        return respuesta;
    }

    private BatchRecordDTO.ProduccionFinalDTO mapProduccionFinal(OrdenProduccion ordenProduccion, List<BatchRecordDTO.CierreDTO> cierres) {
        BatchRecordDTO.ProduccionFinalDTO dto = new BatchRecordDTO.ProduccionFinalDTO();
        dto.unidadesProducidas = ordenProduccion.getCantidadProducida() != null ? ordenProduccion.getCantidadProducida().intValue() : null;
        dto.rendimientoCalculado = ordenProduccion.getPorcentajeCumplimiento();
        dto.unidadesRechazadas = 0;
        dto.unidadesAprobadas = dto.unidadesProducidas;
        if (dto.unidadesAprobadas == null && cierres != null && !cierres.isEmpty()) {
            dto.unidadesAprobadas = cierres.stream()
                    .map(c -> c.cantidad)
                    .filter(q -> q != null)
                    .reduce(Integer::sum)
                    .orElse(null);
        }
        return dto;
    }

    private BatchRecordDTO.LotePTDTO mapLoteProductoTerminado(OrdenProduccion ordenProduccion) {
        BatchRecordDTO.LotePTDTO dto = new BatchRecordDTO.LotePTDTO();
        if (ordenProduccion.getProducto() == null) {
            log.warn("Orden de producción {} no tiene producto asociado para lote PT", ordenProduccion.getId());
            return dto;
        }
        Optional<LoteProducto> loteOpt = loteProductoRepository
                .findByOrdenProduccionIdAndProductoId(ordenProduccion.getId(), ordenProduccion.getProducto().getId().longValue());
        if (loteOpt.isEmpty()) {
            log.warn("No se encontró lote de producto terminado para la orden {}", ordenProduccion.getId());
            return dto;
        }
        LoteProducto lote = loteOpt.get();
        dto.loteId = lote.getId();
        dto.codigoLote = lote.getCodigoLote();
        dto.estado = lote.getEstado() != null ? lote.getEstado().name() : null;
        dto.fechaFabricacion = lote.getFechaFabricacion();
        dto.fechaVencimiento = lote.getFechaVencimiento();
        dto.fechaLiberacion = lote.getFechaLiberacion();
        dto.usuarioLiberador = lote.getUsuarioLiberador() != null ? lote.getUsuarioLiberador().getNombreCompleto() : null;
        return dto;
    }

    private BatchRecordDTO.CalidadDTO mapCalidad(BatchRecordDTO.LotePTDTO lotePTDTO) {
        BatchRecordDTO.CalidadDTO calidadDTO = new BatchRecordDTO.CalidadDTO();
        calidadDTO.evaluaciones = new ArrayList<>();
        calidadDTO.retenciones = new ArrayList<>();
        if (lotePTDTO == null || lotePTDTO.loteId == null) {
            return calidadDTO;
        }
        List<EvaluacionCalidad> evaluaciones = evaluacionCalidadRepository.findByLoteProductoId(lotePTDTO.loteId);
        for (EvaluacionCalidad evaluacion : evaluaciones) {
            BatchRecordDTO.EvaluacionDTO dto = new BatchRecordDTO.EvaluacionDTO();
            dto.id = evaluacion.getId();
            dto.tipoEvaluacion = evaluacion.getTipoEvaluacion() != null ? evaluacion.getTipoEvaluacion().name() : null;
            dto.resultado = evaluacion.getResultado() != null ? evaluacion.getResultado().name() : null;
            dto.observaciones = evaluacion.getObservaciones();
            dto.fechaEvaluacion = evaluacion.getFechaEvaluacion();
            dto.evaluador = evaluacion.getUsuarioEvaluador() != null ? evaluacion.getUsuarioEvaluador().getNombreCompleto() : null;
            calidadDTO.evaluaciones.add(dto);
        }

        List<RetencionLote> retenciones = new ArrayList<>();
        retenciones.addAll(retencionLoteRepository.findByLote_IdAndEstado(lotePTDTO.loteId, EstadoRetencion.RETENIDO));
        retenciones.addAll(retencionLoteRepository.findByLote_IdAndEstado(lotePTDTO.loteId, EstadoRetencion.LIBERADO));
        for (RetencionLote retencion : retenciones) {
            BatchRecordDTO.RetencionDTO dto = new BatchRecordDTO.RetencionDTO();
            dto.estado = retencion.getEstado() != null ? retencion.getEstado().name() : null;
            dto.motivo = retencion.getMotivo() != null ? retencion.getMotivo().name() : null;
            dto.fechaRetencion = retencion.getFechaRetencion();
            dto.fechaLiberacion = retencion.getFechaLiberacion();
            dto.aprobador = retencion.getAprobadoPor() != null ? retencion.getAprobadoPor().getNombreCompleto() : null;
            calidadDTO.retenciones.add(dto);
        }
        return calidadDTO;
    }

    private List<BatchRecordDTO.ControlProcesoDTO> mapControlesProceso(Long ordenProduccionId) {
        List<ControlProcesoProduccion> controles = controlProcesoProduccionRepository.findByOrdenProduccionId(ordenProduccionId);
        List<BatchRecordDTO.ControlProcesoDTO> resultado = new ArrayList<>();
        for (ControlProcesoProduccion control : controles) {
            BatchRecordDTO.ControlProcesoDTO dto = new BatchRecordDTO.ControlProcesoDTO();
            dto.id = control.getId();
            dto.etapa = control.getEtapa();
            dto.parametro = control.getParametro();
            dto.valorMedido = control.getValorMedido();
            dto.unidad = control.getUnidad();
            dto.cumple = control.getCumple();
            dto.observaciones = control.getObservaciones();
            dto.evaluadoPor = control.getEvaluadoPor() != null ? control.getEvaluadoPor().getNombreCompleto() : null;
            dto.fechaRegistro = control.getFechaRegistro();
            resultado.add(dto);
        }
        return resultado;
    }

    private List<BatchRecordDTO.ControlEmpaqueDTO> mapControlesEmpaque(Long ordenProduccionId) {
        List<ControlEmpaqueLote> controles = controlEmpaqueLoteRepository.findByOrdenProduccionId(ordenProduccionId);
        List<BatchRecordDTO.ControlEmpaqueDTO> resultado = new ArrayList<>();
        for (ControlEmpaqueLote control : controles) {
            BatchRecordDTO.ControlEmpaqueDTO dto = new BatchRecordDTO.ControlEmpaqueDTO();
            dto.id = control.getId();
            dto.parametro = control.getParametro();
            dto.valorMedido = control.getValorMedido();
            dto.unidad = control.getUnidad();
            dto.cumple = control.getCumple();
            dto.observaciones = control.getObservaciones();
            dto.evaluadoPor = control.getEvaluadoPor() != null ? control.getEvaluadoPor().getNombreCompleto() : null;
            dto.fechaRegistro = control.getFechaRegistro();
            resultado.add(dto);
        }
        return resultado;
    }

    private List<BatchRecordDTO.ObservacionProcesoDTO> mapObservaciones(Long ordenProduccionId) {
        List<ObservacionProceso> observaciones = observacionProcesoRepository.findByOrdenProduccionId(ordenProduccionId);
        List<BatchRecordDTO.ObservacionProcesoDTO> resultado = new ArrayList<>();
        for (ObservacionProceso observacion : observaciones) {
            BatchRecordDTO.ObservacionProcesoDTO dto = new BatchRecordDTO.ObservacionProcesoDTO();
            dto.id = observacion.getId();
            dto.tipo = observacion.getTipo();
            dto.descripcion = observacion.getDescripcion();
            dto.registradoPor = observacion.getRegistradoPor() != null ? observacion.getRegistradoPor().getNombreCompleto() : null;
            dto.fechaRegistro = observacion.getFechaRegistro();
            resultado.add(dto);
        }
        return resultado;
    }

    private List<BatchRecordDTO.ChecklistEtapaDTO> mapChecklistEtapas(Long ordenProduccionId) {
        List<EtapaProduccion> etapas = etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(ordenProduccionId);
        List<BatchRecordDTO.ChecklistEtapaDTO> resultado = new ArrayList<>();
        for (EtapaProduccion etapa : etapas) {
            List<ChecklistEtapaItem> items = checklistEtapaItemRepository.findByEtapaProduccionIdOrderByIdAsc(etapa.getId());
            if (items.isEmpty()) {
                continue;
            }
            BatchRecordDTO.ChecklistEtapaDTO dto = new BatchRecordDTO.ChecklistEtapaDTO();
            dto.etapaId = etapa.getId();
            dto.etapaNombre = etapa.getNombre();
            dto.items = items.stream().map(this::mapChecklistItem).toList();
            resultado.add(dto);
        }
        return resultado;
    }

    private BatchRecordDTO.ChecklistItemDTO mapChecklistItem(ChecklistEtapaItem item) {
        BatchRecordDTO.ChecklistItemDTO dto = new BatchRecordDTO.ChecklistItemDTO();
        dto.itemId = item.getId();
        dto.nombrePaso = item.getNombrePaso();
        dto.obligatorio = item.getObligatorio();
        dto.estado = item.getEstado() != null ? item.getEstado().name() : null;
        dto.noAplica = item.getNoAplica();
        dto.permitirNoAplica = item.getPermitirNoAplica();
        dto.observacion = item.getObservacion();
        dto.completedAt = item.getCompletedAt();
        dto.completedBy = item.getCompletedBy() != null ? item.getCompletedBy().getNombreCompleto() : null;
        return dto;
    }

    private Usuario obtenerUsuarioDesdeAuth(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "USUARIO_NO_AUTENTICADO");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUsuario();
        }
        if (principal instanceof Usuario usuario) {
            return usuario;
        }
        throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "USUARIO_NO_AUTENTICADO");
    }
}
