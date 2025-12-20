package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.dto.CondicionUsoResponseDTO;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.service.CondicionUsoService;
import com.willyes.clemenintegra.calidad.service.NoConformidadService;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoteCalidadValidatorTest {

    @Mock
    private RetencionLoteService retencionLoteService;
    @Mock
    private NoConformidadService noConformidadService;
    @Mock
    private CondicionUsoService condicionUsoService;

    private LoteCalidadValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LoteCalidadValidator(retencionLoteService, noConformidadService, condicionUsoService);
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

        when(noConformidadService.obtenerActivaPorLote(anyLong())).thenReturn(Optional.empty());
        when(condicionUsoService.getActivasByLote(anyLong())).thenReturn(List.of());

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
        when(noConformidadService.obtenerActivaPorLote(anyLong())).thenReturn(Optional.empty());
        when(condicionUsoService.getActivasByLote(anyLong())).thenReturn(List.of());

        assertThatThrownBy(() -> validator.validarLoteUtilizable(lote))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.BLOQUEO_RETENCION_NC);
    }

    @Test
    void bloqueaLoteLiberadoConNcActiva() {
        LoteProducto lote = new LoteProducto();
        lote.setId(40L);
        lote.setEstado(EstadoLote.LIBERADO);

        NoConformidad nc = new NoConformidad();
        nc.setId(99L);
        when(noConformidadService.obtenerActivaPorLote(40L)).thenReturn(Optional.of(nc));

        assertThatThrownBy(() -> validator.validarLoteUtilizable(lote))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.BLOQUEO_NC_ACTIVA);
    }

    @Test
    void bloqueaLoteLiberadoConCondicionUsoActiva() {
        LoteProducto lote = new LoteProducto();
        lote.setId(50L);
        lote.setEstado(EstadoLote.LIBERADO);

        when(noConformidadService.obtenerActivaPorLote(50L)).thenReturn(Optional.empty());
        when(condicionUsoService.getActivasByLote(50L)).thenReturn(List.of(CondicionUsoResponseDTO.builder().id(1L).build()));

        assertThatThrownBy(() -> validator.validarLoteUtilizable(lote))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.BLOQUEO_CONDICION_USO);
    }
}
