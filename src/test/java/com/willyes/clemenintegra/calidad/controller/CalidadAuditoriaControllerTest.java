package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.service.AuditoriaLotePdfService;
import com.willyes.clemenintegra.calidad.service.AuditoriaLoteService;
import com.willyes.clemenintegra.calidad.service.CarpetaLotePdfService;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalidadAuditoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
class CalidadAuditoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditoriaLoteService auditoriaLoteService;
    @MockBean
    private AuditoriaLotePdfService auditoriaLotePdfService;
    @MockBean
    private CarpetaLotePdfService carpetaLotePdfService;
    @MockBean
    private EvaluacionCalidadService evaluacionCalidadService;
    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void devuelveAuditoriaLoteComoJson() throws Exception {
        AuditoriaLoteResponseDTO dto = AuditoriaLoteResponseDTO.builder()
                .loteId(1L)
                .codigoLote("LOT-01")
                .nombreProducto("Producto")
                .build();

        when(auditoriaLoteService.obtenerAuditoriaDeLote(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/calidad/auditoria-lote/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoLote").value("LOT-01"))
                .andExpect(jsonPath("$.nombreProducto").value("Producto"));
    }

    @Test
    void descargaPdfAuditoria() throws Exception {
        AuditoriaLoteResponseDTO dto = AuditoriaLoteResponseDTO.builder()
                .loteId(2L)
                .codigoLote("LOT-02")
                .build();
        when(auditoriaLoteService.obtenerAuditoriaDeLote(2L)).thenReturn(dto);
        when(auditoriaLotePdfService.generarPdf(any())).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/calidad/auditoria-lote/2/pdf"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo("pdf".getBytes()))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void exportaExcelEvaluaciones() throws Exception {
        when(evaluacionCalidadService.generarReporteEvaluacionesExcel(any(), any(), any())).thenReturn("excel".getBytes());

        mockMvc.perform(get("/api/calidad/reportes/evaluaciones/excel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo("excel".getBytes()));
    }

    @Test
    void descargaCarpetaLote() throws Exception {
        when(carpetaLotePdfService.generarCarpeta(3L)).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/calidad/reportes/lotes/3/carpeta"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo("pdf".getBytes()));
    }

    @Test
    void retorna404CuandoNoExisteLoteEnCarpeta() throws Exception {
        when(carpetaLotePdfService.generarCarpeta(999L))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/calidad/reportes/lotes/999/carpeta"))
                .andExpect(status().isNotFound());
    }
}
