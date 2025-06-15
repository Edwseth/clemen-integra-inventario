package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovimientoInventarioController.class)
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class MovimientoInventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MovimientoInventarioService service;
    @MockBean
    private ProductoRepository productoRepository;
    @MockBean
    private LoteProductoRepository loteProductoRepository;

    private Producto producto;
    private LoteProducto lote;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setId(1L);
        producto.setStockActual(BigDecimal.TEN);

        lote = new LoteProducto();
        lote.setId(1L);
        lote.setStockLote(BigDecimal.TEN);
    }

    @Test
    void registrarMovimientoEntradaValido_debeRetornarCreated() throws Exception {
        MovimientoInventarioDTO request = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(5),
                ClasificacionMovimientoInventario.ENTRADA_PRODUCCION,
                "DOC-1",
                1L,1L,1L,1L,1L,1L,
                1L,1L,1L
        );

        MovimientoInventarioResponseDTO response = new MovimientoInventarioResponseDTO(
                10L,
                BigDecimal.valueOf(5),
                1L,
                "ENTRADA_PRODUCCION",
                "Producto Test",
                "LOTE-001",
                "Almacen 1"
        );

        when(service.registrarMovimiento(ArgumentMatchers.any())).thenReturn(response);

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void registrarMovimientoSalidaValido_debeRetornarCreated() throws Exception {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(loteProductoRepository.findById(1L)).thenReturn(Optional.of(lote));

        MovimientoInventarioDTO request = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(3),
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-2",
                1L,1L,1L,1L,1L,1L,
                1L,1L,1L
        );

        MovimientoInventarioResponseDTO response = new MovimientoInventarioResponseDTO(
                11L,
                BigDecimal.valueOf(3),
                1L,
                "SALIDA_PRODUCCION",
                "Producto Test",
                "LOTE-001",
                "Almacen 1"
        );
        when(service.registrarMovimiento(ArgumentMatchers.any())).thenReturn(response);

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11L));
    }

    @Test
    void registrarSalidaConStockInsuficiente_debeRetornarConflict() throws Exception {
        producto.setStockActual(BigDecimal.ONE);
        lote.setStockLote(BigDecimal.ONE);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(loteProductoRepository.findById(1L)).thenReturn(Optional.of(lote));

        MovimientoInventarioDTO request = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(5),
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-3",
                1L,1L,1L,1L,1L,1L,
                1L,1L,1L
        );

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No hay suficiente stock disponible"));

        verify(service, never()).registrarMovimiento(any());
    }

    @Test
    void registrarMovimientoProductoInexistente_debeRetornarNotFound() throws Exception {
        when(productoRepository.findById(1L)).thenReturn(Optional.empty());

        MovimientoInventarioDTO request = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(1),
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-4",
                1L,1L,1L,1L,1L,1L,
                1L,1L,1L
        );

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarMovimientoLoteInexistente_debeRetornarNotFound() throws Exception {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(loteProductoRepository.findById(1L)).thenReturn(Optional.empty());

        MovimientoInventarioDTO request = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(1),
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-5",
                1L,1L,1L,1L,1L,1L,
                1L,1L,1L
        );

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarMovimientos_debeRetornarOk() throws Exception {
        mockMvc.perform(get("/api/movimientos/filtrar"))
                .andExpect(status().isOk());
    }
}

