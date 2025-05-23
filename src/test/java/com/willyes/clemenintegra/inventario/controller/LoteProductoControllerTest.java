package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

}

