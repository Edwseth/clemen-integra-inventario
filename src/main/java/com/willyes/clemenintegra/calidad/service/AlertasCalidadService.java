package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.AlertaLoteCalidadDTO;
import com.willyes.clemenintegra.calidad.dto.ResumenAlertasCalidadDTO;
import com.willyes.clemenintegra.calidad.model.enums.TipoAlertaLoteCalidad;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertasCalidadService {

    private static final int DIAS_UMBRAL_DEFECTO = 30;
    private static final EnumSet<EstadoLote> ESTADOS_ALERTA_VENCIMIENTO = EnumSet.of(
            EstadoLote.DISPONIBLE,
            EstadoLote.LIBERADO,
            EstadoLote.EN_CUARENTENA,
            EstadoLote.RETENIDO
    );
    private static final EnumSet<EstadoLote> ESTADOS_PENDIENTES_LIBERAR = EnumSet.of(
            EstadoLote.EN_CUARENTENA,
            EstadoLote.RETENIDO
    );

    private final LoteProductoRepository loteProductoRepository;
    private Clock clock = Clock.systemDefaultZone();

    public ResumenAlertasCalidadDTO obtenerAlertas(int diasUmbral) {
        int umbral = diasUmbral > 0 ? diasUmbral : DIAS_UMBRAL_DEFECTO;
        LocalDate hoy = LocalDate.now(clock);
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.plusDays(umbral).atTime(LocalTime.MAX);

        // Radiografía: FEFO/vencimientos ya se consultan en Inventarios con
        // - LoteProductoRepository#findByFechaVencimientoBetweenFetchProducto / #findVencidosFetch
        // - LoteProductoRepository#listarLotesConVencimiento y FEFO en #findFefoSalidaPt
        // Radiografía: pendientes de liberar se listan en LoteProductoServiceImpl#obtenerLotesPorEvaluar
        // y en AlertaInventarioServiceImpl#obtenerLotesRetenidosOCuarentenaProlongados.
        List<LoteProducto> proximos = loteProductoRepository.findAlertasProximasVencer(inicio, fin, ESTADOS_ALERTA_VENCIMIENTO);
        List<LoteProducto> vencidos = loteProductoRepository.findAlertasVencidos(inicio, ESTADOS_ALERTA_VENCIMIENTO);
        List<LoteProducto> pendientes = loteProductoRepository.findAlertasPendientesLiberar(ESTADOS_PENDIENTES_LIBERAR);

        List<AlertaLoteCalidadDTO> lotesProximos = proximos.stream()
                .map(lote -> mapAlerta(lote, TipoAlertaLoteCalidad.PROXIMO_VENCER, hoy))
                .toList();
        List<AlertaLoteCalidadDTO> lotesVencidos = vencidos.stream()
                .map(lote -> mapAlerta(lote, TipoAlertaLoteCalidad.VENCIDO, hoy))
                .toList();
        List<AlertaLoteCalidadDTO> lotesPendientesLiberar = pendientes.stream()
                .map(lote -> mapAlerta(lote, TipoAlertaLoteCalidad.PENDIENTE_LIBERAR, hoy))
                .toList();

        // TODO: enganchar aquí una futura notificación automática (correo/job) con las alertas consolidadas.
        return ResumenAlertasCalidadDTO.builder()
                .lotesProximosVencer(lotesProximos)
                .lotesVencidos(lotesVencidos)
                .lotesPendientesLiberar(lotesPendientesLiberar)
                .totalProximosVencer(lotesProximos.size())
                .totalVencidos(lotesVencidos.size())
                .totalPendientesLiberar(lotesPendientesLiberar.size())
                .build();
    }

    private AlertaLoteCalidadDTO mapAlerta(LoteProducto lote, TipoAlertaLoteCalidad tipo, LocalDate hoy) {
        LocalDateTime fechaVencimiento = lote.getFechaVencimiento();
        Integer diasParaVencer = null;
        if (fechaVencimiento != null) {
            diasParaVencer = (int) ChronoUnit.DAYS.between(hoy, fechaVencimiento.toLocalDate());
        }
        return AlertaLoteCalidadDTO.builder()
                .loteId(lote.getId())
                .codigoLote(lote.getCodigoLote())
                .productoId(lote.getProducto() != null ? lote.getProducto().getId().longValue() : null)
                .codigoSku(lote.getProducto() != null ? lote.getProducto().getCodigoSku() : null)
                .nombreProducto(lote.getProducto() != null ? lote.getProducto().getNombre() : null)
                .almacenId(lote.getAlmacen() != null ? lote.getAlmacen().getId().longValue() : null)
                .nombreAlmacen(lote.getAlmacen() != null ? lote.getAlmacen().getNombre() : null)
                .estadoLote(lote.getEstado())
                .fechaVencimiento(fechaVencimiento)
                .diasParaVencer(diasParaVencer)
                .tipoAlerta(tipo)
                .build();
    }

    void setClock(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }
}
