package com.willyes.clemenintegra.calidad.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.CustomUserDetailsService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EvaluacionCalidadController.class)
@Import({SecurityConfig.class, ControlCalidadControllerTest.NotFoundHandler.class})
class ControlCalidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EvaluacionCalidadService service;

    // Dependencias de seguridad requeridas por SecurityConfig
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void cleanUp() {
        reset(service);
    }

    @RestControllerAdvice
    static class NotFoundHandler {
        @ExceptionHandler(NoSuchElementException.class)
        ResponseEntity<Void> handle() {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Test
    @WithMockUser(roles = "ROL_JEFE_CALIDAD")
    void evaluarLotePasado_retornarOk() throws Exception {
        EvaluacionCalidadRequestDTO request = EvaluacionCalidadRequestDTO.builder()
                .resultado(ResultadoEvaluacion.APROBADO)
                .observaciones("Lote aprobado")
                .loteProductoId(1L)
                .usuarioEvaluadorId(1L)
                .build();

        EvaluacionCalidadResponseDTO response = EvaluacionCalidadResponseDTO.builder()
                .id(1L)
                .resultado(ResultadoEvaluacion.APROBADO)
                .nombreLote("LOTE-1")
                .nombreEvaluador("Tester")
                .build();

        when(service.crear(any())).thenReturn(response);

        mockMvc.perform(post("/api/calidad/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("APROBADO"));

        verify(service).crear(any());
    }

    @Test
    @WithMockUser(roles = "ROL_JEFE_CALIDAD")
    void evaluarLoteRechazado_retornarOk() throws Exception {
        EvaluacionCalidadRequestDTO request = EvaluacionCalidadRequestDTO.builder()
                .resultado(ResultadoEvaluacion.RECHAZADO)
                .observaciones("Lote falló")
                .loteProductoId(1L)
                .usuarioEvaluadorId(1L)
                .build();

        EvaluacionCalidadResponseDTO response = EvaluacionCalidadResponseDTO.builder()
                .id(2L)
                .resultado(ResultadoEvaluacion.RECHAZADO)
                .nombreLote("LOTE-1")
                .nombreEvaluador("Tester")
                .build();

        when(service.crear(any())).thenReturn(response);

        mockMvc.perform(post("/api/calidad/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("RECHAZADO"));
    }

    @Test
    @WithMockUser(roles = "ROL_JEFE_CALIDAD")
    void evaluarLoteInexistente_retornarNotFound() throws Exception {
        EvaluacionCalidadRequestDTO request = EvaluacionCalidadRequestDTO.builder()
                .resultado(ResultadoEvaluacion.APROBADO)
                .observaciones("No existe")
                .loteProductoId(99L)
                .usuarioEvaluadorId(1L)
                .build();

        when(service.crear(any())).thenThrow(new NoSuchElementException("not found"));

        mockMvc.perform(post("/api/calidad/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ROL_JEFE_CALIDAD")
    void consultarEvaluacion_existente_retornarOk() throws Exception {
        EvaluacionCalidadResponseDTO response = EvaluacionCalidadResponseDTO.builder()
                .id(5L)
                .resultado(ResultadoEvaluacion.APROBADO)
                .nombreLote("LOTE-1")
                .nombreEvaluador("Tester")
                .build();
        when(service.obtenerPorId(5L)).thenReturn(response);

        mockMvc.perform(get("/api/calidad/evaluaciones/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L));
    }

    @Test
    @WithMockUser(roles = "ROL_JEFE_CALIDAD")
    void consultarEvaluacion_inexistente_retornarNotFound() throws Exception {
        when(service.obtenerPorId(99L)).thenThrow(new NoSuchElementException("not found"));

        mockMvc.perform(get("/api/calidad/evaluaciones/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ROL_ALMACENISTA")
    void evaluarLote_usuarioSinRolCalidad_retornarForbidden() throws Exception {
        EvaluacionCalidadRequestDTO request = EvaluacionCalidadRequestDTO.builder()
                .resultado(ResultadoEvaluacion.APROBADO)
                .observaciones("Lote aprobado")
                .loteProductoId(1L)
                .usuarioEvaluadorId(1L)
                .build();

        mockMvc.perform(post("/api/calidad/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

