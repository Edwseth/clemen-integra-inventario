package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadDetalleDTO;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import com.willyes.clemenintegra.calidad.service.ResultadoAnalisisMicroService;
import com.willyes.clemenintegra.calidad.service.PlantillaAnalisisMicroService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluacionCalidadController.class)
@AutoConfigureMockMvc(addFilters = false)
class EvaluacionCalidadControllerTest {

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
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void obtieneDetalleEvaluacion() throws Exception {
        EvaluacionCalidadDetalleDTO detalle = EvaluacionCalidadDetalleDTO.builder()
                .idEvaluacion(1L)
                .codigoLote("LOT-01")
                .nombreProducto("Producto X")
                .tipoEvaluacion("QUIMICO_MICROBIOLOGICO")
                .fechaEvaluacion(LocalDateTime.now())
                .resultado("CONFORME")
                .archivosAdjuntos(Collections.emptyList())
                .resultadosMicro(Collections.emptyList())
                .build();

        when(evaluacionCalidadService.obtenerDetalle(1L)).thenReturn(detalle);

        mockMvc.perform(get("/api/calidad/evaluaciones/1/detalle")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoLote").value("LOT-01"))
                .andExpect(jsonPath("$.nombreProducto").value("Producto X"));
    }

    @Test
    void retornaNotFoundCuandoNoExisteEvaluacion() throws Exception {
        when(evaluacionCalidadService.obtenerDetalle(anyLong()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluación no encontrada"));

        mockMvc.perform(get("/api/calidad/evaluaciones/999/detalle"))
                .andExpect(status().isNotFound());
    }

    @Test
    void descargaPdfMicroExistente() throws Exception {
        when(resultadoAnalisisMicroService.obtenerPdfMicro(5L)).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/calidad/evaluaciones/5/microbiologico/pdf")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty());

        verify(resultadoAnalisisMicroService).obtenerPdfMicro(5L);
    }

    @Test
    void generaPdfMicroCuandoNoExisteAdjunto() throws Exception {
        when(resultadoAnalisisMicroService.obtenerPdfMicro(7L)).thenReturn("nuevo".getBytes());

        mockMvc.perform(get("/api/calidad/evaluaciones/7/micro-pdf")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo("nuevo".getBytes()));

        verify(resultadoAnalisisMicroService).obtenerPdfMicro(7L);
    }
}
