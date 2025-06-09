package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class LoteProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void crearLote_ConDatosValidos_RetornaCreated() throws Exception {
        LoteProductoRequestDTO request = new LoteProductoRequestDTO();
        request.setCodigoLote("LOTE-INT-001");
        request.setFechaFabricacion(LocalDate.now().minusDays(3));
        request.setFechaVencimiento(LocalDate.now().plusMonths(6));
        request.setStockLote(new BigDecimal("100.5"));
        request.setEstado(EstadoLote.DISPONIBLE);
        request.setTemperaturaAlmacenamiento(22.0);
        request.setProductoId(1L);
        request.setAlmacenId(1L);

        mockMvc.perform(post("/api/lotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print()) // 👈 Esto imprime la respuesta
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoLote").value("LOTE-INT-001"))
                .andDo(print());

    }

    @Test
    void crearLote_CodigoLoteDuplicado_RetornaConflict() throws Exception {
        LoteProductoRequestDTO dto = new LoteProductoRequestDTO();
        dto.setCodigoLote("LOTE-DUPLICADO");
        dto.setFechaFabricacion(LocalDate.of(2025, 5, 20));
        dto.setFechaVencimiento(LocalDate.of(2026, 5, 23));
        dto.setStockLote(new BigDecimal("100.00")); // obligatorio
        dto.setEstado(EstadoLote.DISPONIBLE);
        dto.setProductoId(1L);
        dto.setAlmacenId(1L);

        // 1. Crear el primer lote
        mockMvc.perform(post("/api/lotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // 2. Intentar crear otro lote con el mismo código
        mockMvc.perform(post("/api/lotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Ya existe un lote con el código")));
    }

    @Test
    @WithMockUser(username = "jefeCalidad", roles = {"ROL_JEFE_CALIDAD"})
    void exportarLotesPorVencer_UsuarioAutorizado_RetornaExcel() throws Exception {
        mockMvc.perform(get("/api/lotes/reporte-vencimiento"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment;")))
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @WithMockUser(username = "almacenista", roles = {"ROL_ALMACENISTA"})
    void exportarLotesPorVencer_UsuarioNoAutorizado_RetornaForbidden() throws Exception {
        mockMvc.perform(get("/api/lotes/reporte-vencimiento"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "jefeCalidad", roles = {"ROL_JEFE_CALIDAD"})
    void exportarAlertasActivas_UsuarioAutorizado_RetornaExcel() throws Exception {
        mockMvc.perform(get("/api/lotes/reporte-alertas"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment;")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @WithMockUser(username = "visitante", roles = {"ROL_ALMACENISTA"})
    void exportarAlertasActivas_UsuarioNoAutorizado_RetornaForbidden() throws Exception {
        mockMvc.perform(get("/api/lotes/reporte-alertas"))
                .andExpect(status().isForbidden());
    }


}

