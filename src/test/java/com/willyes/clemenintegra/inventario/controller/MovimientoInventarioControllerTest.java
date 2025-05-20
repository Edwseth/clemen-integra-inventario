package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
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
                BigDecimal.valueOf(10.5),                            // 1) cantidad
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,  // 2) tipoMovimiento (enum correcto)
                "DOC-456",                                           // 3) docReferencia
                1L, // 4) productoId
                1L, // 5) loteProductoId
                1L, // 6) almacenId
                1L, // 7) proveedorId
                1L, // 8) ordenCompraId
                1L, // 9) motivoMovimientoId
                1L, // 10) tipoMovimientoDetalleId
                1L  // 11) usuarioRegistroId
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


