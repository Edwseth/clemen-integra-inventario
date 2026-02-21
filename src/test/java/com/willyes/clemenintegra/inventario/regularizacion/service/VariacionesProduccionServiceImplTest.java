package com.willyes.clemenintegra.inventario.regularizacion.service;

import com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones.VariacionOPResponseDTO;
import com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadDetalleRepository;
import com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadRepository;
import com.willyes.clemenintegra.inventario.regularizacion.repository.projection.VariacionRegularizacionProjection;
import com.willyes.clemenintegra.inventario.regularizacion.service.impl.VariacionesProduccionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VariacionesProduccionServiceImplTest {

    @Mock
    private RegularizacionTrazabilidadRepository regularizacionRepository;

    @Mock
    private RegularizacionTrazabilidadDetalleRepository detalleRepository;

    @InjectMocks
    private VariacionesProduccionServiceImpl service;

    @Test
    void listarVariaciones_devuelvePositivasYNegativasConKpis() {
        VariacionRegularizacionProjection positiva = projection(1L, 10L, BigDecimal.valueOf(100), BigDecimal.valueOf(110), BigDecimal.valueOf(10));
        VariacionRegularizacionProjection negativa = projection(2L, 11L, BigDecimal.valueOf(100), BigDecimal.valueOf(90), BigDecimal.valueOf(-10));

        when(regularizacionRepository.findUltimasVariacionesPorOP(any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(positiva, negativa), PageRequest.of(0, 20), 2));

        List<VariacionOPResponseDTO> content = service
                .listarVariaciones(null, null, null, false, null, PageRequest.of(0, 20))
                .getContent();

        assertThat(content).hasSize(2);
        assertThat(content.get(0).diferencia()).isEqualByComparingTo("10");
        assertThat(content.get(0).diferenciaPct()).isEqualByComparingTo("10.00");
        assertThat(content.get(1).diferencia()).isEqualByComparingTo("-10");
        assertThat(content.get(1).rendimientoPct()).isEqualByComparingTo("90.00");
    }

    @Test
    void listarVariaciones_marcaDataInconsistenteCuandoProgramadaEsCero() {
        VariacionRegularizacionProjection inconsistente = projection(3L, 12L, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN);

        when(regularizacionRepository.findUltimasVariacionesPorOP(any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(inconsistente), PageRequest.of(0, 20), 1));

        VariacionOPResponseDTO row = service
                .listarVariaciones(null, null, null, false, null, PageRequest.of(0, 20))
                .getContent()
                .get(0);

        assertThat(row.dataInconsistente()).isTrue();
        assertThat(row.rendimientoPct()).isNull();
        assertThat(row.diferenciaPct()).isNull();
    }

    private VariacionRegularizacionProjection projection(Long regId,
                                                         Long opId,
                                                         BigDecimal programada,
                                                         BigDecimal real,
                                                         BigDecimal diferencia) {
        return new VariacionRegularizacionProjection() {
            @Override
            public Long getRegularizacionId() { return regId; }
            @Override
            public Long getOrdenProduccionId() { return opId; }
            @Override
            public BigDecimal getCantidadProgramada() { return programada; }
            @Override
            public BigDecimal getCantidadReal() { return real; }
            @Override
            public BigDecimal getDiferencia() { return diferencia; }
            @Override
            public Boolean getAjustarPt() { return false; }
            @Override
            public String getDocumentoReferencia() { return "DOC"; }
            @Override
            public String getObservaciones() { return "OBS"; }
            @Override
            public Long getUsuarioId() { return 9L; }
            @Override
            public LocalDateTime getFechaIngreso() { return LocalDateTime.now(); }
            @Override
            public Boolean getTieneDetalle() { return true; }
        };
    }
}
