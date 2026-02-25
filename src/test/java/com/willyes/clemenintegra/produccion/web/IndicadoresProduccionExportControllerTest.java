package com.willyes.clemenintegra.produccion.web;

import com.willyes.clemenintegra.produccion.controller.IndicadoresProduccionController;
import com.willyes.clemenintegra.produccion.dto.IndicadoresProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.service.ProduccionIndicadoresService;
import com.willyes.clemenintegra.produccion.service.ReporteIndicadoresProduccionService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IndicadoresProduccionController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
class IndicadoresProduccionExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProduccionIndicadoresService produccionIndicadoresService;
    @MockBean
    private ReporteIndicadoresProduccionService reporteIndicadoresProduccionService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void bypassFilters() throws ServletException, IOException {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(usuarioInactivoFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("exporta indicadores en excel con headers correctos")
    void exportarIndicadoresExcel_ok() throws Exception {
        IndicadoresProduccionResponseDTO indicadores = IndicadoresProduccionResponseDTO.builder()
                .totalOrdenesPeriodo(1)
                .ordenesEnTiempo(1)
                .ordenesRetrasadas(0)
                .porcentajeCumplimiento(100.0)
                .cantidadTotalPlanificada(new BigDecimal("10"))
                .cantidadTotalProducida(new BigDecimal("10"))
                .ordenesAbiertasConVencimientoVencido(0)
                .diasAlerta(3)
                .build();
        byte[] excelBytes = "excel".getBytes();

        when(produccionIndicadoresService.calcularIndicadores(any(LocalDate.class), any(LocalDate.class), any()))
                .thenReturn(indicadores);
        when(reporteIndicadoresProduccionService.generarExcelIndicadores(any(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(excelBytes);

        var result = mockMvc.perform(get("/api/produccion/indicadores/export/excel")
                        .param("fechaInicio", "2024-01-01")
                        .param("fechaFin", "2024-01-31")
                        .param("diasAlerta", "5"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("indicadores-produccion.xlsx")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }
}
