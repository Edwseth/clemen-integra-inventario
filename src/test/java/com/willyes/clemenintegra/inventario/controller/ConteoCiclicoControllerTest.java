package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import com.willyes.clemenintegra.inventario.service.ConteoCiclicoService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void listarConteosDevuelve200() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(5L)
                .almacenId(1)
                .estado(EstadoConteoCiclico.BORRADOR)
                .build();
        Page<ConteoCiclicoResponseDTO> page = new PageImpl<>(java.util.List.of(response));
        when(conteoCiclicoService.listar(ArgumentMatchers.isNull(), ArgumentMatchers.isNull(), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/inventario/conteos")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5));
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void obtenerPorIdDevuelve200() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(15L)
                .almacenId(2)
                .estado(EstadoConteoCiclico.EN_CONTEO)
                .build();
        when(conteoCiclicoService.obtenerPorId(15L)).thenReturn(response);

        mockMvc.perform(get("/api/inventario/conteos/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CONTEO"));
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void obtenerPorIdNoEncontradoDevuelve404() throws Exception {
        when(conteoCiclicoService.obtenerPorId(99L))
                .thenThrow(new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "Conteo no encontrado"));

        mockMvc.perform(get("/api/inventario/conteos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.RECURSO_NO_ENCONTRADO.name()));
    }

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

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void listarConEstadoInvalidoDevuelve400() throws Exception {
        when(conteoCiclicoService.listar(ArgumentMatchers.isNull(), anyString(), ArgumentMatchers.any(Pageable.class)))
                .thenThrow(new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Estado de conteo inválido"));

        mockMvc.perform(get("/api/inventario/conteos")
                        .param("estado", "INVALIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.SOLICITUD_INVALIDA.name()));
    }
}
