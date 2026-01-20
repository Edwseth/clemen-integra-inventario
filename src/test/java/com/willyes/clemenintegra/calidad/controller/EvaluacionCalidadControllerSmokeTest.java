package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import com.willyes.clemenintegra.calidad.service.PlantillaAnalisisMicroService;
import com.willyes.clemenintegra.calidad.service.ResultadoAnalisisMicroService;
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

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluacionCalidadController.class)
@AutoConfigureMockMvc(addFilters = false)
class EvaluacionCalidadControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluacionCalidadService evaluacionCalidadService;

    @MockBean
    private ResultadoAnalisisMicroService resultadoAnalisisMicroService;

    @MockBean
    private PlantillaAnalisisMicroService plantillaAnalisisMicroService;

    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @MockBean
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Test
    void descargarPdfMicro_respondePdfConNombre() throws Exception {
        EvaluacionCalidadResponseDTO evaluacion = new EvaluacionCalidadResponseDTO();
        evaluacion.setNombreLote("L-500");
        given(resultadoAnalisisMicroService.obtenerPdfMicro(5L)).willReturn("pdf".getBytes(StandardCharsets.UTF_8));
        given(evaluacionCalidadService.obtenerPorId(5L)).willReturn(evaluacion);

        mockMvc.perform(get("/api/calidad/evaluaciones/5/micro/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"analisis-micro-L-500.pdf\""))
                .andExpect(content().bytes("pdf".getBytes(StandardCharsets.UTF_8)));
    }
}
