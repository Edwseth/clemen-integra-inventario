package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryVencidosProperties;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoteVencimientoJobService {

    private static final EnumSet<EstadoLote> ESTADOS_EXCLUIDOS = EnumSet.of(EstadoLote.VENCIDO, EstadoLote.RECHAZADO);

    private final InventoryVencidosProperties properties;
    private final LoteProductoRepository loteProductoRepository;
    private final LoteVencimientoChunkProcessor chunkProcessor;
    private final LoteVencimientoContextResolver contextResolver;

    public LoteVencimientoPreview dryRun() {
        properties.validateForExecution();
        ZoneId zoneId = properties.resolveZoneId();
        LocalDate today = LocalDate.now(zoneId);
        LocalDateTime cutoff = today.plusDays(1).atStartOfDay();

        List<Long> ids = new ArrayList<>();
        int page = 0;
        int chunkSize = properties.getChunkSize();
        Pageable pageableBase = PageRequest.of(0, chunkSize, Sort.by(Sort.Direction.ASC, "id"));

        while (true) {
            Pageable pageable = PageRequest.of(page, chunkSize, pageableBase.getSort());
            List<Long> pageIds = loteProductoRepository.findIdsParaExpirar(cutoff, ESTADOS_EXCLUIDOS, pageable);
            if (pageIds.isEmpty()) {
                break;
            }
            ids.addAll(pageIds);
            if (pageIds.size() < chunkSize) {
                break;
            }
            page++;
        }

        return new LoteVencimientoPreview(ids.size(), ids);
    }

    public LoteVencimientoExecutionResult ejecutar() {
        properties.validateForExecution();
        ZoneId zoneId = properties.resolveZoneId();
        LocalDate today = LocalDate.now(zoneId);
        LocalDateTime inicioDia = today.atStartOfDay();
        LocalDateTime cutoff = today.plusDays(1).atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1).minusNanos(1);

        Long destinoId = properties.getMovimiento().isEnabled()
                ? contextResolver.resolveAlmacenDestinoId(properties)
                : null;
        Usuario usuarioSistema = properties.getMovimiento().isEnabled()
                ? contextResolver.resolveUsuarioSistema()
                : null;

        long totalActualizados = 0L;
        long totalMovimientos = 0L;
        int chunkSize = properties.getChunkSize();
        Pageable pageable = PageRequest.of(0, chunkSize, Sort.by(Sort.Direction.ASC, "id"));

        while (true) {
            List<Long> ids = loteProductoRepository.findIdsParaExpirar(cutoff, ESTADOS_EXCLUIDOS, pageable);
            if (ids.isEmpty()) {
                break;
            }
            LocalDateTime fechaMovimiento = LocalDateTime.now(zoneId);
            LoteVencimientoChunkProcessor.ChunkResult result = chunkProcessor
                    .process(ids, cutoff, inicioDia, finDia, fechaMovimiento, destinoId, usuarioSistema);
            totalActualizados += result.actualizados();
            totalMovimientos += result.movimientos();
        }

        return new LoteVencimientoExecutionResult(totalActualizados, totalMovimientos);
    }

    public record LoteVencimientoPreview(long total, List<Long> loteIds) { }

    public record LoteVencimientoExecutionResult(long totalActualizados, long totalMovimientos) { }
}
