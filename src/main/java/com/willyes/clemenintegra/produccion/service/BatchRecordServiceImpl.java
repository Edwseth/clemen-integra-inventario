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
import com.willyes.clemenintegra.inventario.model.ReservaLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import com.willyes.clemenintegra.produccion.dto.BatchRecordDTO;
import com.willyes.clemenintegra.produccion.model.CierreProduccion;
import com.willyes.clemenintegra.produccion.model.ControlEmpaqueLote;
import com.willyes.clemenintegra.produccion.model.ControlProcesoProduccion;
import com.willyes.clemenintegra.produccion.model.ObservacionProceso;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.ControlEmpaqueLoteRepository;
import com.willyes.clemenintegra.produccion.repository.ControlProcesoProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.ObservacionProcesoRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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

    @Override
    public BatchRecordDTO buildByOrdenProduccion(Long ordenProduccionId) {
        OrdenProduccion ordenProduccion = ordenProduccionRepository.findById(ordenProduccionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));

        BatchRecordDTO dto = new BatchRecordDTO();
        dto.op = mapOp(ordenProduccion);
        dto.formula = mapFormula(ordenProduccion);
        dto.consumos = mapConsumos(ordenProduccionId);
        dto.reservas = mapReservas(ordenProduccionId);
        dto.cierres = mapCierres(ordenProduccionId);
        dto.produccionFinal = mapProduccionFinal(ordenProduccion, dto.cierres);
        dto.loteProductoTerminado = mapLoteProductoTerminado(ordenProduccion);
        dto.calidad = mapCalidad(dto.loteProductoTerminado);
        dto.controlesProceso = mapControlesProceso(ordenProduccionId);
        dto.controlesEmpaque = mapControlesEmpaque(ordenProduccionId);
        dto.observacionesProceso = mapObservaciones(ordenProduccionId);
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
        if (ordenProduccion.getProducto() == null) {
            return null;
        }
        Optional<FormulaProducto> formulaOpt = formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(ordenProduccion.getProducto().getId().longValue(), EstadoFormula.APROBADA);
        FormulaProducto formula = formulaOpt.orElseGet(() -> formulaProductoRepository
                .findByProductoId(ordenProduccion.getProducto().getId().longValue()).orElse(null));
        if (formula == null) {
            return null;
        }
        BatchRecordDTO.FormulaDTO formulaDTO = new BatchRecordDTO.FormulaDTO();
        formulaDTO.version = formula.getVersion();
        List<BatchRecordDTO.DetalleFormulaDTO> detalles = new ArrayList<>();
        if (formula.getDetalles() != null) {
            for (DetalleFormula detalle : formula.getDetalles()) {
                BatchRecordDTO.DetalleFormulaDTO detalleDTO = new BatchRecordDTO.DetalleFormulaDTO();
                detalleDTO.insumoId = detalle.getInsumo() != null ? detalle.getInsumo().getId().longValue() : null;
                detalleDTO.codigoSku = detalle.getInsumo() != null ? detalle.getInsumo().getCodigoSku() : null;
                detalleDTO.nombre = detalle.getInsumo() != null ? detalle.getInsumo().getNombre() : null;
                detalleDTO.unidad = detalle.getUnidadMedida() != null ? detalle.getUnidadMedida().getNombre() : null;
                detalleDTO.cantidadNecesaria = detalle.getCantidadNecesaria();
                detalleDTO.obligatorio = Boolean.TRUE.equals(detalle.getObligatorio());
                detalles.add(detalleDTO);
            }
        }
        formulaDTO.detalles = detalles;
        return formulaDTO;
    }

    private List<BatchRecordDTO.ConsumoDTO> mapConsumos(Long ordenProduccionId) {
        List<MovimientoInventario> movimientos = movimientoInventarioRepository
                .findByOrdenProduccionId(ordenProduccionId, Pageable.unpaged())
                .getContent();
        List<BatchRecordDTO.ConsumoDTO> consumos = new ArrayList<>();
        for (MovimientoInventario movimiento : movimientos) {
            BatchRecordDTO.ConsumoDTO consumoDTO = new BatchRecordDTO.ConsumoDTO();
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
            consumos.add(consumoDTO);
        }
        return consumos;
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
        if (ordenProduccion.getProducto() == null) {
            return null;
        }
        Optional<LoteProducto> loteOpt = loteProductoRepository
                .findByOrdenProduccionIdAndProductoId(ordenProduccion.getId(), ordenProduccion.getProducto().getId().longValue());
        if (loteOpt.isEmpty()) {
            return null;
        }
        LoteProducto lote = loteOpt.get();
        BatchRecordDTO.LotePTDTO dto = new BatchRecordDTO.LotePTDTO();
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
        if (lotePTDTO == null || lotePTDTO.loteId == null) {
            return null;
        }
        BatchRecordDTO.CalidadDTO calidadDTO = new BatchRecordDTO.CalidadDTO();
        List<EvaluacionCalidad> evaluaciones = evaluacionCalidadRepository.findByLoteProductoId(lotePTDTO.loteId);
        List<BatchRecordDTO.EvaluacionDTO> evaluacionDTOS = new ArrayList<>();
        for (EvaluacionCalidad evaluacion : evaluaciones) {
            BatchRecordDTO.EvaluacionDTO dto = new BatchRecordDTO.EvaluacionDTO();
            dto.id = evaluacion.getId();
            dto.tipoEvaluacion = evaluacion.getTipoEvaluacion() != null ? evaluacion.getTipoEvaluacion().name() : null;
            dto.resultado = evaluacion.getResultado() != null ? evaluacion.getResultado().name() : null;
            dto.observaciones = evaluacion.getObservaciones();
            dto.fechaEvaluacion = evaluacion.getFechaEvaluacion();
            dto.evaluador = evaluacion.getUsuarioEvaluador() != null ? evaluacion.getUsuarioEvaluador().getNombreCompleto() : null;
            evaluacionDTOS.add(dto);
        }
        calidadDTO.evaluaciones = evaluacionDTOS;

        List<RetencionLote> retenciones = new ArrayList<>();
        retenciones.addAll(retencionLoteRepository.findByLote_IdAndEstado(lotePTDTO.loteId, EstadoRetencion.RETENIDO));
        retenciones.addAll(retencionLoteRepository.findByLote_IdAndEstado(lotePTDTO.loteId, EstadoRetencion.LIBERADO));
        List<BatchRecordDTO.RetencionDTO> retencionDTOS = new ArrayList<>();
        for (RetencionLote retencion : retenciones) {
            BatchRecordDTO.RetencionDTO dto = new BatchRecordDTO.RetencionDTO();
            dto.estado = retencion.getEstado() != null ? retencion.getEstado().name() : null;
            dto.motivo = retencion.getMotivo() != null ? retencion.getMotivo().name() : null;
            dto.fechaRetencion = retencion.getFechaRetencion();
            dto.fechaLiberacion = retencion.getFechaLiberacion();
            dto.aprobador = retencion.getAprobadoPor() != null ? retencion.getAprobadoPor().getNombreCompleto() : null;
            retencionDTOS.add(dto);
        }
        calidadDTO.retenciones = retencionDTOS;
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
}
