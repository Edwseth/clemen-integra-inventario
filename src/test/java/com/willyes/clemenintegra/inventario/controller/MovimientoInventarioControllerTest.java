package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
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

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value("10.5"))
                .andExpect(jsonPath("$.tipoMovimiento").value("RECEPCION_COMPRA"))
                .andExpect(jsonPath("$.docReferencia").value("DOC-456"));
    }

    @Test
    void registrarMovimiento_ProductoInexistente_DeberiaRetornar404() throws Exception {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.valueOf(5),
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-404",
                999L, // producto inexistente
                1L, 1L, 1L, 1L, 1L, 1L, 1L
        );

        Mockito.when(movimientoInventarioService.registrarMovimiento(Mockito.any()))
                .thenThrow(new EntityNotFoundException("Producto no encontrado"));

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Producto no encontrado"))
                .andExpect(jsonPath("$.status").value(404));

    }

    @Test
    void registrarMovimiento_CantidadInvalida_DeberiaRetornar400() throws Exception {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.valueOf(0), // ❌ Cantidad inválida
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-VAL-001",
                1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L
        );

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.errors.cantidad").value("La cantidad debe ser mayor a cero"));
    }

    @Test
    void registrarMovimiento_LoteInexistente_DeberiaRetornar404() throws Exception {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.valueOf(5),
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-LOTE-404",
                1L, 999L, 1L, 1L, 1L, 1L, 1L, 1L
        );

        Mockito.when(movimientoInventarioService.registrarMovimiento(Mockito.any()))
                .thenThrow(new IllegalArgumentException("Lote no encontrado"));

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lote no encontrado"));
    }

    @Test
    void registrarMovimiento_CodigoLoteDuplicado_DeberiaRetornar409() throws Exception {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.valueOf(10),
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-DUP",
                1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L
        );

        Mockito.when(movimientoInventarioService.registrarMovimiento(Mockito.any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate entry 'LOTE-001' for key 'codigo_lote'"));

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un lote con el código ingresado."));
    }


}


