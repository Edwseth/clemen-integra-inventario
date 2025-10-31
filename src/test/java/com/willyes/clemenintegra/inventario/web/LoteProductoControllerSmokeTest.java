package com.willyes.clemenintegra.inventario.web;

import com.willyes.clemenintegra.inventario.controller.LoteProductoController;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoteProductoController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
class LoteProductoControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoteProductoService loteProductoService;
    @MockBean
    private LoteProductoRepository loteProductoRepository;
    @MockBean
    private LoteProductoMapper loteProductoMapper;
    @MockBean
    private EvaluacionCalidadService evaluacionCalidadService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    @DisplayName("GET /api/lotes devuelve 200 con contenido paginado")
    void listarLotes_devuelvePagina() throws Exception {
        LoteProductoResponseDTO dto = LoteProductoResponseDTO.builder()
                .id(1L)
                .codigoLote("L-001")
                .estado(EstadoLote.DISPONIBLE)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<LoteProductoResponseDTO> page = new PageImpl<>(List.of(dto), pageable, 1);
        when(loteProductoService.listarTodos(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/lotes")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].codigoLote").value("L-001"))
                .andExpect(jsonPath("$.content[0].estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    @DisplayName("GET /api/lotes/estado/{estado} devuelve lista simple")
    void listarPorEstado_devuelveLista() throws Exception {
        LoteProductoResponseDTO dto = LoteProductoResponseDTO.builder()
                .id(2L)
                .codigoLote("L-002")
                .estado(EstadoLote.LIBERADO)
                .build();
        when(loteProductoService.obtenerLotesPorEstado("LIBERADO")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/lotes/estado/LIBERADO"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].estado").value("LIBERADO"));
    }
}
