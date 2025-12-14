package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.AlertaOrdenProduccionDTO;
import com.willyes.clemenintegra.produccion.dto.IndicadoresProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProduccionIndicadoresServiceImpl implements ProduccionIndicadoresService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final OrdenProduccionRepository ordenProduccionRepository;

    @Override
    public IndicadoresProduccionResponseDTO calcularIndicadores(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime desde = fechaInicio.atStartOfDay();
        LocalDateTime hasta = fechaFin.atTime(LocalTime.MAX);
        List<OrdenProduccion> ordenes = ordenProduccionRepository.findByFechaFinBetween(desde, hasta);

        long ordenesEnTiempo = 0;
        long ordenesRetrasadas = 0;
        long ordenesAbiertasVencidas = 0;
        BigDecimal cantidadPlanificada = ZERO;
        BigDecimal cantidadProducida = ZERO;
        LocalDateTime hoy = LocalDateTime.now();

        for (OrdenProduccion orden : ordenes) {
            cantidadPlanificada = cantidadPlanificada.add(valorNoNulo(orden.getCantidadProgramada()));
            cantidadProducida = cantidadProducida.add(obtenerCantidadProducida(orden));

            boolean tieneFechaFin = orden.getFechaFin() != null;
            boolean esCerrada = esOrdenCerrada(orden);
            boolean esRetrasadaAbierta = !esCerrada && tieneFechaFin && orden.getFechaFin().isBefore(hoy);
            boolean esRetrasadaCerrada = esCerrada && tieneFechaFin && orden.getFechaCierre() != null
                    && orden.getFechaCierre().isAfter(orden.getFechaFin());

            if (esCerrada && tieneFechaFin && orden.getFechaCierre() != null
                    && !orden.getFechaCierre().isAfter(orden.getFechaFin())) {
                ordenesEnTiempo++;
            } else if (esRetrasadaCerrada || esRetrasadaAbierta) {
                ordenesRetrasadas++;
            }

            if (esRetrasadaAbierta) {
                ordenesAbiertasVencidas++;
            }
        }

        long totalOrdenes = ordenes.size();
        long denominador = ordenesEnTiempo + ordenesRetrasadas;
        double porcentajeCumplimiento = denominador == 0
                ? 0
                : (ordenesEnTiempo * 100.0) / denominador;

        return IndicadoresProduccionResponseDTO.builder()
                .totalOrdenesPeriodo(totalOrdenes)
                .ordenesEnTiempo(ordenesEnTiempo)
                .ordenesRetrasadas(ordenesRetrasadas)
                .porcentajeCumplimiento(porcentajeCumplimiento)
                .cantidadTotalPlanificada(cantidadPlanificada)
                .cantidadTotalProducida(cantidadProducida)
                .ordenesAbiertasConVencimientoVencido(ordenesAbiertasVencidas)
                .build();
    }

    @Override
    public List<AlertaOrdenProduccionDTO> obtenerOrdenesConAlertas(LocalDate fechaReferencia, int diasVentana) {
        LocalDate fechaBase = fechaReferencia != null ? fechaReferencia : LocalDate.now();
        int ventana = diasVentana > 0 ? diasVentana : 3;
        LocalDateTime inicioVentana = fechaBase.atStartOfDay();
        LocalDateTime finVentana = fechaBase.plusDays(ventana).atTime(LocalTime.MAX);

        List<EstadoProduccion> estadosCerrados = List.of(
                EstadoProduccion.FINALIZADA,
                EstadoProduccion.CERRADA_INCOMPLETA,
                EstadoProduccion.CANCELADA);

        List<AlertaOrdenProduccionDTO> alertas = new ArrayList<>();
        List<OrdenProduccion> proximas = ordenProduccionRepository
                .findByEstadoNotInAndFechaFinBetween(estadosCerrados, inicioVentana, finVentana);
        proximas.stream()
                .map(op -> crearAlerta(op, "PROXIMA_VENCER"))
                .filter(Objects::nonNull)
                .forEach(alertas::add);

        List<OrdenProduccion> retrasadas = ordenProduccionRepository
                .findByEstadoNotInAndFechaFinBefore(estadosCerrados, inicioVentana);
        retrasadas.stream()
                .map(op -> crearAlerta(op, "RETRASADA"))
                .filter(Objects::nonNull)
                .forEach(alertas::add);

        return alertas;
    }

    private boolean esOrdenCerrada(OrdenProduccion orden) {
        EstadoProduccion estado = orden.getEstado();
        return estado == EstadoProduccion.FINALIZADA
                || estado == EstadoProduccion.CERRADA_INCOMPLETA
                || estado == EstadoProduccion.CANCELADA;
    }

    private BigDecimal valorNoNulo(BigDecimal valor) {
        return valor != null ? valor : ZERO;
    }

    private BigDecimal obtenerCantidadProducida(OrdenProduccion orden) {
        if (orden.getCantidadProducidaAcumulada() != null) {
            return orden.getCantidadProducidaAcumulada();
        }
        if (orden.getCantidadProducida() != null) {
            return orden.getCantidadProducida();
        }
        return ZERO;
    }

    private AlertaOrdenProduccionDTO crearAlerta(OrdenProduccion orden, String tipoAlerta) {
        if (orden.getFechaFin() == null) {
            return null;
        }
        return AlertaOrdenProduccionDTO.builder()
                .ordenId(orden.getId())
                .codigoOrden(orden.getCodigoOrden())
                .productoPrincipal(orden.getProducto() != null ? orden.getProducto().getNombre() : null)
                .fechaCompromiso(orden.getFechaFin().toLocalDate())
                .estado(orden.getEstado() != null ? orden.getEstado().name() : null)
                .tipoAlerta(tipoAlerta)
                .build();
    }
}
