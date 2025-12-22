package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.service.AuditoriaLoteService;
import com.willyes.clemenintegra.calidad.service.CarpetaLotePdfService;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import com.willyes.clemenintegra.calidad.service.ReporteInvimaBpmPdfService;
import com.willyes.clemenintegra.calidad.service.ResultadoAnalisisMicroService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportesCalidadController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportesCalidadControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluacionCalidadService evaluacionCalidadService;

    @MockBean
    private CarpetaLotePdfService carpetaLotePdfService;

    @MockBean
    private AuditoriaLoteService auditoriaLoteService;

    @MockBean
    private ResultadoAnalisisMicroService resultadoAnalisisMicroService;

    @MockBean
    private ReporteInvimaBpmPdfService reporteInvimaBpmPdfService;

    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    void exportarEvaluacionesExcel_respondeExcelConNombre() throws Exception {
        given(evaluacionCalidadService.generarReporteEvaluacionesExcel(any(), any(), any()))
                .willReturn("excel".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/calidad/reportes/evaluaciones/excel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString("reporte-evaluaciones-")))
                .andExpect(content().bytes("excel".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void descargarCarpetaLote_respondePdfConNombre() throws Exception {
        given(carpetaLotePdfService.generarCarpeta(9L)).willReturn("pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/calidad/reportes/lote/9/carpeta-pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"carpeta-lote-9.pdf\""))
                .andExpect(content().bytes("pdf".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void descargarAnalisisMicro_respondePdfConNombre() throws Exception {
        AuditoriaLoteResponseDTO auditoria = AuditoriaLoteResponseDTO.builder()
                .codigoLote("L-999")
                .evaluaciones(List.of(AuditoriaLoteResponseDTO.EvaluacionResumenDTO.builder()
                        .id(12L)
                        .pdfMicroDisponible(true)
                        .build()))
                .build();
        given(auditoriaLoteService.obtenerAuditoriaDeLote(9L)).willReturn(auditoria);
        given(resultadoAnalisisMicroService.obtenerPdfMicro(12L)).willReturn("pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/calidad/reportes/lote/9/analisis-micro-pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"analisis-micro-L-999.pdf\""))
                .andExpect(content().bytes("pdf".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void descargarInvimaBpm_respondePdfConNombre() throws Exception {
        given(reporteInvimaBpmPdfService.generarPdf(eq(3L))).willReturn("pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/calidad/reportes/invima-bpm/v1/pdf").param("loteId", "3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invima-bpm-3.pdf\""))
                .andExpect(content().bytes("pdf".getBytes(StandardCharsets.UTF_8)));
    }
}
