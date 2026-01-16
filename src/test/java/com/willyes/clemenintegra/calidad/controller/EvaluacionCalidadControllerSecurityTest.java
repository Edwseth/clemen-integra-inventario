package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisMicroDTO;
import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import com.willyes.clemenintegra.calidad.service.PlantillaAnalisisMicroService;
import com.willyes.clemenintegra.calidad.service.ResultadoAnalisisMicroService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluacionCalidadController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(com.willyes.clemenintegra.shared.security.SecurityConfig.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class EvaluacionCalidadControllerSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

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
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.repository.UsuarioRepository usuarioRepository;

    @BeforeEach
    void configureFilters() throws ServletException, java.io.IOException {
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
    void rechazaAnalistaEnPlantillaMicroDesdeEvaluaciones() throws Exception {
        mockMvc.perform(get("/api/calidad/evaluaciones/plantillas/micro/producto/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("analista")
                                .authorities(() -> "ROL_ANALISTA_CALIDAD")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteMicrobiologoEnPlantillaMicroDesdeEvaluaciones() throws Exception {
        PlantillaAnalisisMicroDTO dto = new PlantillaAnalisisMicroDTO();
        dto.setRequiereAnalisisMicro(true);
        when(plantillaAnalisisMicroService.obtenerPorProducto(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/calidad/evaluaciones/plantillas/micro/producto/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("micro")
                                .authorities(() -> "ROL_MICROBIOLOGO")))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaAnalistaEnResultadosMicro() throws Exception {
        mockMvc.perform(get("/api/calidad/evaluaciones/5/resultados-micro")
                        .with(SecurityMockMvcRequestPostProcessors.user("analista")
                                .authorities(() -> "ROL_ANALISTA_CALIDAD")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/calidad/evaluaciones/5/resultados-micro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]")
                        .with(SecurityMockMvcRequestPostProcessors.user("analista")
                                .authorities(() -> "ROL_ANALISTA_CALIDAD")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteMicrobiologoEnResultadosMicro() throws Exception {
        when(resultadoAnalisisMicroService.obtenerPorEvaluacion(5L)).thenReturn(Collections.emptyList());
        when(resultadoAnalisisMicroService.guardarResultados(anyLong(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Collections.singletonList(ResultadoAnalisisMicroResponseDTO.builder().build()));

        mockMvc.perform(get("/api/calidad/evaluaciones/5/resultados-micro")
                        .with(SecurityMockMvcRequestPostProcessors.user("micro")
                                .authorities(() -> "ROL_MICROBIOLOGO")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/calidad/evaluaciones/5/resultados-micro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]")
                        .with(SecurityMockMvcRequestPostProcessors.user("micro")
                                .authorities(() -> "ROL_MICROBIOLOGO")))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaAnalistaEnPdfMicro() throws Exception {
        mockMvc.perform(get("/api/calidad/evaluaciones/9/micro/pdf")
                        .with(SecurityMockMvcRequestPostProcessors.user("analista")
                                .authorities(() -> "ROL_ANALISTA_CALIDAD")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteMicrobiologoEnPdfMicro() throws Exception {
        when(resultadoAnalisisMicroService.obtenerPdfMicro(9L)).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/calidad/evaluaciones/9/micro/pdf")
                        .accept(MediaType.APPLICATION_PDF)
                        .with(SecurityMockMvcRequestPostProcessors.user("micro")
                                .authorities(() -> "ROL_MICROBIOLOGO")))
                .andExpect(status().isOk());
    }

    @Test
    void permiteAnalistaRegistrarEvaluacion() throws Exception {
        EvaluacionCalidadResponseDTO response = EvaluacionCalidadResponseDTO.builder()
                .id(1L)
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .nombreLote("LOTE-01")
                .nombreProducto("Producto X")
                .nombreEvaluador("Analista Calidad")
                .build();
        when(evaluacionCalidadService.crear(any(), anyList())).thenReturn(response);

        mockMvc.perform(multipart("/api/calidad/evaluaciones")
                        .param("tipoEvaluacion", "FISICO")
                        .param("observaciones", "OK")
                        .param("loteProductoId", "10")
                        .with(SecurityMockMvcRequestPostProcessors.user("analista")
                                .authorities(() -> "ROL_ANALISTA_CALIDAD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombreLote").value("LOTE-01"))
                .andExpect(jsonPath("$.nombreProducto").value("Producto X"))
                .andExpect(jsonPath("$.nombreEvaluador").value("Analista Calidad"));
    }

    @Test
    void permiteMicrobiologoRegistrarEvaluacion() throws Exception {
        EvaluacionCalidadResponseDTO response = EvaluacionCalidadResponseDTO.builder()
                .id(2L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .nombreLote("LOTE-02")
                .nombreProducto("Producto Y")
                .nombreEvaluador("Microbiologo")
                .build();
        when(evaluacionCalidadService.crear(any(), anyList())).thenReturn(response);

        mockMvc.perform(multipart("/api/calidad/evaluaciones")
                        .param("tipoEvaluacion", "QUIMICO_MICROBIOLOGICO")
                        .param("observaciones", "OK")
                        .param("loteProductoId", "20")
                        .with(SecurityMockMvcRequestPostProcessors.user("micro")
                                .authorities(() -> "ROL_MICROBIOLOGO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.nombreLote").value("LOTE-02"))
                .andExpect(jsonPath("$.nombreProducto").value("Producto Y"))
                .andExpect(jsonPath("$.nombreEvaluador").value("Microbiologo"));
    }
}
