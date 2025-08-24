package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovimientoInventarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class MovimientoInventarioControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private MovimientoInventarioService service;

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void filtrarAceptaSoloFecha() throws Exception {
        Page<MovimientoInventarioResponseDTO> empty = new PageImpl<>(List.of(), PageRequest.of(0,10), 0);
        when(service.filtrar(any(), any(), any(), any(), any(), any(), any())).thenReturn(empty);

        mvc.perform(get("/api/movimientos/filtrar")
                .param("fechaInicio", "2025-08-01")
                .param("fechaFin", "2025-08-31")
                .param("page", "0")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> fin = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(service).filtrar(inicio.capture(), fin.capture(), any(), any(), any(), any(), any());
        assertEquals(LocalDateTime.of(2025,8,1,0,0,0), inicio.getValue());
        assertEquals(LocalDateTime.of(2025,8,31,23,59,59), fin.getValue());
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void filtrarAceptaFechaHora() throws Exception {
        Page<MovimientoInventarioResponseDTO> empty = new PageImpl<>(List.of(), PageRequest.of(0,10), 0);
        when(service.filtrar(any(), any(), any(), any(), any(), any(), any())).thenReturn(empty);

        mvc.perform(get("/api/movimientos/filtrar")
                .param("fechaInicio", "2025-08-01T00:00:00")
                .param("fechaFin", "2025-08-31T23:59:59")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> fin = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(service).filtrar(inicio.capture(), fin.capture(), any(), any(), any(), any(), any());
        assertEquals(LocalDateTime.of(2025,8,1,0,0,0), inicio.getValue());
        assertEquals(LocalDateTime.of(2025,8,31,23,59,59), fin.getValue());
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void filtrarFaltaParametroRetorna400() throws Exception {
        mvc.perform(get("/api/movimientos/filtrar")
                .param("fechaInicio", "2025-08-01")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void filtrarRangoInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/movimientos/filtrar")
                .param("fechaInicio", "2025-08-31")
                .param("fechaFin", "2025-08-01")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void filtrarZonaHorariaRetorna400() throws Exception {
        mvc.perform(get("/api/movimientos/filtrar")
                .param("fechaInicio", "2025-08-01T00:00:00Z")
                .param("fechaFin", "2025-08-31T23:59:59")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
    }
}
