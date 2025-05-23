package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.config.TestSecurityConfig;
import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoteProductoController.class)
@Import(TestSecurityConfig.class)
class LoteProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoteProductoService loteProductoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void crearLote_ConDatosValidos_RetornaCreated() throws Exception {
        // Arrange
        LoteProductoRequestDTO request = new LoteProductoRequestDTO();
        request.setCodigoLote("L001-TEST");
        request.setFechaFabricacion(LocalDate.now().minusDays(2));
        request.setFechaVencimiento(LocalDate.now().plusMonths(6));
        request.setStockLote(new BigDecimal("100.50"));
        request.setEstado(EstadoLote.DISPONIBLE);
        request.setTemperaturaAlmacenamiento(22.0);
        request.setProductoId(1L);
        request.setAlmacenId(1L);

        // Simular respuesta del servicio
        when(loteProductoService.crearLote(any())).thenAnswer(invoc -> {
            var dto = new com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO();
            dto.setId(1L);
            dto.setCodigoLote("L001-TEST");
            dto.setEstado(EstadoLote.DISPONIBLE);
            dto.setNombreProducto("Producto Simulado");
            dto.setNombreAlmacen("Almacén Central");
            return dto;
        });

        // Act & Assert
        mockMvc.perform(post("/api/lotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoLote").value("L001-TEST"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.nombreProducto").value("Producto Simulado"));
    }
}

