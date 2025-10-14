package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryVencidosProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoteVencimientoScheduler {

    private final InventoryVencidosProperties properties;
    private final LoteVencimientoJobService jobService;

    @Scheduled(cron = "${inventory.vencidos.job.cron}", zone = "${inventory.vencidos.job.timezone}")
    public void actualizarLotesVencidos() {
        if (!properties.getJob().isEnabled()) {
            log.debug("[VENCIDOS] Job deshabilitado. estadoObjetivo={} cron={} tz={}",
                    properties.getEstadoObjetivo(), properties.getJob().getCron(), properties.getJob().getTimezone());
            return;
        }
        try {
            LoteVencimientoJobService.LoteVencimientoExecutionResult result = jobService.ejecutar();
            log.info("[VENCIDOS] afectados={} movimientos={} estadoObjetivo={} cron={} tz={}",
                    result.totalActualizados(), result.totalMovimientos(),
                    properties.getEstadoObjetivo(), properties.getJob().getCron(), properties.getJob().getTimezone());
        } catch (IllegalStateException ex) {
            log.error("[VENCIDOS] CONFIG_INCOMPLETA estadoObjetivo={} mensaje={}",
                    properties.getEstadoObjetivo(), ex.getMessage());
        } catch (Exception ex) {
            log.error("[VENCIDOS] ERROR estadoObjetivo={} mensaje={}",
                    properties.getEstadoObjetivo(), ex.getMessage(), ex);
        }
    }
}
