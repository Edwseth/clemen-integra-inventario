package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import com.willyes.clemenintegra.inventario.service.ConteoCiclicoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConteoCiclicoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConteoCiclicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConteoCiclicoService conteoCiclicoService;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void crearConteoDevuelve201() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(5L)
                .almacenId(1)
                .estado(EstadoConteoCiclico.BORRADOR)
                .build();
        when(conteoCiclicoService.crearConteo(ArgumentMatchers.any(ConteoCiclicoRequestDTO.class)))
                .thenReturn(response);

        ConteoCiclicoRequestDTO request = new ConteoCiclicoRequestDTO();
        request.setAlmacenId(1);

        mockMvc.perform(post("/api/inventario/conteos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void aplicarConteoRespondeOk() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(7L)
                .almacenId(2)
                .estado(EstadoConteoCiclico.APLICADO)
                .build();
        when(conteoCiclicoService.aplicar(anyLong(), eq("k1"))).thenReturn(response);

        mockMvc.perform(post("/api/inventario/conteos/7/aplicar")
                        .header("Idempotency-Key", "k1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APLICADO"));
    }
}
