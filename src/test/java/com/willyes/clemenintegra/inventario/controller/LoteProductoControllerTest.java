package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoteProductoController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoteProductoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private LoteProductoService service;

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void listarAceptaFormatos() throws Exception {
        Page<LoteProductoResponseDTO> empty = new PageImpl<>(List.of(), PageRequest.of(0,10), 0);
        when(service.listarTodos(any(), any(), any(), any(), any(), any(), any())).thenReturn(empty);

        mvc.perform(get("/api/lotes")
                .param("fechaInicio", "2025-08-01")
                .param("fechaFin", "2025-08-31T23:59:59")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> fin = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(service).listarTodos(any(), any(), any(), any(), inicio.capture(), fin.capture(), any());
        assertEquals(LocalDateTime.of(2025,8,1,0,0,0), inicio.getValue());
        assertEquals(LocalDateTime.of(2025,8,31,23,59,59), fin.getValue());
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void listarFaltaParametroRetorna400() throws Exception {
        mvc.perform(get("/api/lotes")
                .param("fechaInicio", "2025-08-01")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void listarRangoInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/lotes")
                .param("fechaInicio", "2025-08-31")
                .param("fechaFin", "2025-08-01")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void listarZonaHorariaRetorna400() throws Exception {
        mvc.perform(get("/api/lotes")
                .param("fechaInicio", "2025-08-01T00:00:00Z")
                .param("fechaFin", "2025-08-31T23:59:59")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
    }
}
