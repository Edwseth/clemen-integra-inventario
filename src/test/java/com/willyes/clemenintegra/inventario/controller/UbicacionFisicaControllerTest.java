package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.UbicacionFisicaRequestDTO;
import com.willyes.clemenintegra.inventario.dto.UbicacionFisicaResponseDTO;
import com.willyes.clemenintegra.inventario.service.UbicacionFisicaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UbicacionFisicaController.class)
@AutoConfigureMockMvc(addFilters = false)
class UbicacionFisicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UbicacionFisicaService ubicacionFisicaService;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void getUbicacionesFiltraPorAlmacenYQuery() throws Exception {
        List<UbicacionFisicaResponseDTO> respuesta = List.of(
                UbicacionFisicaResponseDTO.builder()
                        .id(1L)
                        .almacenId(6)
                        .codigo("A1")
                        .descripcion("Pasillo 1")
                        .activo(true)
                        .build()
        );
        when(ubicacionFisicaService.buscar(eq(6), eq("A1"))).thenReturn(respuesta);

        mockMvc.perform(get("/api/ubicaciones")
                        .param("almacenId", "6")
                        .param("q", "A1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo").value("A1"))
                .andExpect(jsonPath("$[0].almacenId").value(6));
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void postCreaUbicacion() throws Exception {
        UbicacionFisicaResponseDTO respuesta = UbicacionFisicaResponseDTO.builder()
                .id(10L)
                .almacenId(6)
                .codigo("P6-A1-E2")
                .descripcion("Pasillo 6 Estante 2")
                .activo(true)
                .build();
        when(ubicacionFisicaService.crear(any(UbicacionFisicaRequestDTO.class))).thenReturn(respuesta);

        UbicacionFisicaRequestDTO request = new UbicacionFisicaRequestDTO();
        request.setAlmacenId(6);
        request.setCodigo("P6-A1-E2");
        request.setDescripcion("Pasillo 6 Estante 2");

        mockMvc.perform(post("/api/ubicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.codigo").value("P6-A1-E2"))
                .andExpect(jsonPath("$.almacenId").value(6));
    }
}
