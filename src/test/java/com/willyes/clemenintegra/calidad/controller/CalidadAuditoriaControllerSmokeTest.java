package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.service.AuditoriaLotePdfService;
import com.willyes.clemenintegra.calidad.service.AuditoriaLoteService;
import com.willyes.clemenintegra.calidad.service.CarpetaLotePdfService;
import com.willyes.clemenintegra.calidad.service.ReporteInvimaBpmPdfService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalidadAuditoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
class CalidadAuditoriaControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditoriaLoteService auditoriaLoteService;

    @MockBean
    private AuditoriaLotePdfService auditoriaLotePdfService;

    @MockBean
    private CarpetaLotePdfService carpetaLotePdfService;

    @MockBean
    private ReporteInvimaBpmPdfService reporteInvimaBpmPdfService;

    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @MockBean
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Test
    void descargaPdfAuditoria_respondePdfConNombre() throws Exception {
        AuditoriaLoteResponseDTO auditoria = AuditoriaLoteResponseDTO.builder()
                .codigoLote("L-100")
                .build();
        given(auditoriaLoteService.obtenerAuditoriaDeLote(10L)).willReturn(auditoria);
        given(auditoriaLotePdfService.generarPdf(auditoria)).willReturn("pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/calidad/auditoria-lote/10/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"auditoria-lote-L-100.pdf\""))
                .andExpect(content().bytes("pdf".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void descargarCarpetaLote_respondePdfConNombre() throws Exception {
        given(carpetaLotePdfService.generarCarpeta(5L)).willReturn("pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/calidad/reportes/lotes/5/carpeta"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"carpeta-lote-5.pdf\""))
                .andExpect(content().bytes("pdf".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void descargarReporteInvimaBpm_respondePdfConNombre() throws Exception {
        given(reporteInvimaBpmPdfService.generarPdf(eq(7L))).willReturn("pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/calidad/reportes/invima-bpm/lote/7/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invima-bpm-7.pdf\""))
                .andExpect(content().bytes("pdf".getBytes(StandardCharsets.UTF_8)));
    }
}
