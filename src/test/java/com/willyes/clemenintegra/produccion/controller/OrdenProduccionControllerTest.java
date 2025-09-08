package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.service.OrdenProduccionService;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrdenProduccionController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrdenProduccionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    OrdenProduccionService service;

    @MockBean
    UsuarioService usuarioService;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void cerrarParcial_ok() throws Exception {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.EN_PROCESO)
                .build();
        when(service.registrarCierre(eq(34L), any())).thenReturn(orden);

        String json = """
            { "cantidad": 300, "tipo": "PARCIAL", "observacion": "cierre turno" }
            """;

        mockMvc.perform(post("/api/produccion/ordenes/{id}/cierres", 34)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void cerrarTotal_ok() throws Exception {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.EN_PROCESO)
                .build();
        when(service.registrarCierre(eq(34L), any())).thenReturn(orden);

        String json = """
            { "cantidad": 300, "tipo": "TOTAL", "observacion": "cierre turno" }
            """;

        mockMvc.perform(post("/api/produccion/ordenes/{id}/cierres", 34)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void cerrarSinTipo_400() throws Exception {
        String json = """
            { "cantidad": 300, "observacion": "sin tipo" }
            """;

        mockMvc.perform(post("/api/produccion/ordenes/{id}/cierres", 34)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("tipo"));
    }
}

