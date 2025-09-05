package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LoteVencimientoScheduler {

    private final LoteProductoRepository loteProductoRepository;

    @Scheduled(cron = "0 0 0 * * *")
    public void actualizarLotesVencidos() {
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        List<LoteProducto> lotes = loteProductoRepository
                .findByFechaVencimientoBeforeAndEstadoNotIn(hoy,
                        List.of(EstadoLote.VENCIDO, EstadoLote.RECHAZADO));
        for (LoteProducto lote : lotes) {
            lote.setEstado(EstadoLote.VENCIDO);
            loteProductoRepository.save(lote);
        }
    }
}
