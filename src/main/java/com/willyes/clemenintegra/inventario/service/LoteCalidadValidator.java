package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.calidad.service.CondicionUsoService;
import com.willyes.clemenintegra.calidad.service.NoConformidadService;
import com.willyes.clemenintegra.calidad.service.RetencionLoteService;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LoteCalidadValidator {

    private static final EnumSet<EstadoLote> ESTADOS_NO_PERMITIDOS = EnumSet.of(
            EstadoLote.EN_CUARENTENA,
            EstadoLote.RETENIDO,
            EstadoLote.RECHAZADO,
            EstadoLote.VENCIDO
    );

    private final RetencionLoteService retencionLoteService;
    private final NoConformidadService noConformidadService;
    private final CondicionUsoService condicionUsoService;

    public void validarLoteUtilizable(LoteProducto lote) {
        EstadoLote estado = lote != null ? lote.getEstado() : null;

        if (lote != null && lote.getId() != null) {
            noConformidadService.obtenerActivaPorLote(lote.getId()).ifPresent(nc -> {
                throw new CustomBusinessException(ApiErrorCode.BLOQUEO_NC_ACTIVA,
                        "El lote tiene una no conformidad abierta y no puede utilizarse.",
                        Map.of("loteId", lote.getId(), "ncId", nc.getId()));
            });

            if (!condicionUsoService.getActivasByLote(lote.getId()).isEmpty()) {
                throw new CustomBusinessException(ApiErrorCode.BLOQUEO_CONDICION_USO,
                        "El lote tiene una condición de uso activa y no puede utilizarse.",
                        Map.of("loteId", lote.getId()));
            }
        }

        if (!ESTADOS_NO_PERMITIDOS.contains(estado)) {
            return;
        }

        if (estado == EstadoLote.RETENIDO && lote != null && lote.getId() != null) {
            retencionLoteService.obtenerActivaPorLote(lote.getId()).ifPresent(ret -> {
                if (ret.getMotivo() == MotivoRetencion.NO_CONFORMIDAD) {
                    throw new CustomBusinessException(ApiErrorCode.BLOQUEO_RETENCION_NC,
                            "El lote está retenido por una no conformidad abierta.",
                            Map.of("loteId", lote.getId(), "retencionId", ret.getId()));
                }
            });
        }

        throw new CustomBusinessException(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO,
                "El lote aún no está liberado por Calidad y no puede utilizarse en esta operación.",
                Map.of("loteId", lote != null ? lote.getId() : null,
                        "estado", estado != null ? estado.name() : null));
    }
}
