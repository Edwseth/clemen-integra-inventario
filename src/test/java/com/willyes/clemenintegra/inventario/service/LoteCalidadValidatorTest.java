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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void bloqueaLoteRetenidoConRetencionManual() {
        LoteProducto lote = new LoteProducto();
        lote.setId(60L);
        lote.setEstado(EstadoLote.RETENIDO);

        RetencionLote retencion = new RetencionLote();
        retencion.setId(70L);
        retencion.setMotivo(MotivoRetencion.OTRO);

        when(retencionLoteService.obtenerActivaPorLote(60L)).thenReturn(Optional.of(retencion));
        when(noConformidadService.obtenerActivaPorLote(60L)).thenReturn(Optional.empty());
        when(condicionUsoService.getActivasByLote(60L)).thenReturn(List.of());

        assertThatThrownBy(() -> validator.validarLoteUtilizable(lote))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO);
    }

    @Test
    void bloqueaLoteVencidoAunqueEsteLiberadoYConStock() {
        LoteProducto lote = new LoteProducto();
        lote.setId(80L);
        lote.setCodigoLote("LOT-EXP-01");
        lote.setEstado(EstadoLote.LIBERADO);
        lote.setStockLote(new BigDecimal("15.000000"));
        lote.setFechaVencimiento(LocalDateTime.now().minusDays(2));

        when(noConformidadService.obtenerActivaPorLote(80L)).thenReturn(Optional.empty());
        when(condicionUsoService.getActivasByLote(80L)).thenReturn(List.of());

        assertThatThrownBy(() -> validator.validarLoteUtilizable(lote))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.LOTE_VENCIDO)
                .hasMessageContaining("está vencido");
    }

    @Test
    void permiteLoteVigenteEnEstadoLiberado() {
        LoteProducto lote = new LoteProducto();
        lote.setId(81L);
        lote.setCodigoLote("LOT-VIG-01");
        lote.setEstado(EstadoLote.LIBERADO);
        lote.setStockLote(new BigDecimal("8.000000"));
        lote.setFechaVencimiento(LocalDateTime.now().plusDays(5));

        when(noConformidadService.obtenerActivaPorLote(81L)).thenReturn(Optional.empty());
        when(condicionUsoService.getActivasByLote(81L)).thenReturn(List.of());

        validator.validarLoteUtilizable(lote);

        assertThat(lote.getEstado()).isEqualTo(EstadoLote.LIBERADO);
    }

    @Test
    void bloqueaLoteSinFechaVencimiento() {
        LoteProducto lote = new LoteProducto();
        lote.setId(82L);
        lote.setCodigoLote("LOT-SIN-FECHA");
        lote.setEstado(EstadoLote.LIBERADO);

        when(noConformidadService.obtenerActivaPorLote(82L)).thenReturn(Optional.empty());
        when(condicionUsoService.getActivasByLote(82L)).thenReturn(List.of());

        assertThatThrownBy(() -> validator.validarLoteUtilizable(lote))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.LOTE_VENCIDO)
                .hasMessageContaining("no tiene fecha de vencimiento");
    }

    @Test
    void bloqueaLoteQueVenceHoy() {
        LoteProducto lote = new LoteProducto();
        lote.setId(83L);
        lote.setCodigoLote("LOT-HOY");
        lote.setEstado(EstadoLote.LIBERADO);
        lote.setFechaVencimiento(LocalDateTime.now().withHour(0).withMinute(0));

        when(noConformidadService.obtenerActivaPorLote(83L)).thenReturn(Optional.empty());
        when(condicionUsoService.getActivasByLote(83L)).thenReturn(List.of());

        assertThatThrownBy(() -> validator.validarLoteUtilizable(lote))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.LOTE_VENCIDO);
    }

}
