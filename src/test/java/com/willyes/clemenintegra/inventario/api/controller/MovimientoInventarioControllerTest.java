package com.willyes.clemenintegra.inventario.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.application.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.domain.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.domain.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.domain.enums.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.application.service.MovimientoInventarioService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovimientoInventarioController.class)
class MovimientoInventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovimientoInventarioService movimientoInventarioService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registrarMovimiento_DeberiaRetornar201() throws Exception {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                new BigDecimal("10.5"),
                TipoMovimiento.RECEPCION_COMPRA,
                "DOC-456",
                1L,
                1L,
                1L,
                1L,
                1L,
                1L,
                TipoMovimientoDetalle.RECEPCION_COMPRA,
                1L
        );

        Mockito.when(movimientoInventarioService.registrarMovimiento(Mockito.any())).thenReturn(dto);

        mockMvc.perform(post("/movimientos-inventario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value("10.5"))
                .andExpect(jsonPath("$.tipoMovimiento").value("RECEPCION_COMPRA"))
                .andExpect(jsonPath("$.docReferencia").value("DOC-456"));
    }
}


