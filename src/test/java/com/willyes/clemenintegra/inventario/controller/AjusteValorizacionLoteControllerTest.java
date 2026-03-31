package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.valorizacion.AjusteValorizacionLoteRequestDTO;
import com.willyes.clemenintegra.inventario.service.AjusteValorizacionLoteService;
import com.willyes.clemenintegra.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AjusteValorizacionLoteControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AjusteValorizacionLoteService service = mock(AjusteValorizacionLoteService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AjusteValorizacionLoteController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void postSinIdempotencyKeyFallaPorContrato() throws Exception {
        AjusteValorizacionLoteRequestDTO request = new AjusteValorizacionLoteRequestDTO(
                new BigDecimal("10.5"), "REGULARIZACION", "obs", "doc", false
        );

        mockMvc.perform(post("/api/inventario/lotes/1/ajuste-valorizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
