package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.service.RetencionLoteService;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoteCalidadValidatorTest {

    @Mock
    private RetencionLoteService retencionLoteService;

    private LoteCalidadValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LoteCalidadValidator(retencionLoteService);
    }

    @Test
    void permiteLoteLiberadoODisponible() {
        LoteProducto liberado = new LoteProducto();
        liberado.setEstado(EstadoLote.LIBERADO);

        LoteProducto disponible = new LoteProducto();
        disponible.setEstado(EstadoLote.DISPONIBLE);

        assertThatCode(() -> validator.validarLoteUtilizable(liberado)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validarLoteUtilizable(disponible)).doesNotThrowAnyException();
    }

    @Test
    void bloqueaEstadosNoLiberadosConCodigoCentralizado() {
        LoteProducto lote = new LoteProducto();
        lote.setId(10L);
        lote.setEstado(EstadoLote.EN_CUARENTENA);

        assertThatThrownBy(() -> validator.validarLoteUtilizable(lote))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO);
    }

    @Test
    void respetaBloqueoPorNoConformidadCuandoEstaRetenido() {
        LoteProducto lote = new LoteProducto();
        lote.setId(20L);
        lote.setEstado(EstadoLote.RETENIDO);

        RetencionLote retencion = new RetencionLote();
        retencion.setId(30L);
        retencion.setMotivo(MotivoRetencion.NO_CONFORMIDAD);

        when(retencionLoteService.obtenerActivaPorLote(anyLong())).thenReturn(Optional.of(retencion));

        assertThatThrownBy(() -> validator.validarLoteUtilizable(lote))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.BLOQUEO_RETENCION_NC);
    }
}

