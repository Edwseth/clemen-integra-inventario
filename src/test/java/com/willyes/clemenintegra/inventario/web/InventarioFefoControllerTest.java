package com.willyes.clemenintegra.inventario.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.controller.InventarioFefoController;
import com.willyes.clemenintegra.inventario.dto.LoteConsumoDTO;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventarioFefoController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
class InventarioFefoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MovimientoInventarioService movimientoInventarioService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;

    @Test
    @DisplayName("GET /api/inventario/fefo/preview devuelve lotes ordenados y total")
    void previewFefo_ok() throws Exception {
        LoteConsumoDTO primero = LoteConsumoDTO.builder()
                .loteId(10L)
                .codigoLote("L1")
                .fechaVencimiento(LocalDateTime.now().plusDays(5))
                .almacenId(2L)
                .disponibleAntes(new BigDecimal("500.000000"))
                .tomar(new BigDecimal("500.000000"))
                .disponibleDespues(BigDecimal.ZERO.setScale(6))
                .build();
        LoteConsumoDTO segundo = LoteConsumoDTO.builder()
                .loteId(11L)
                .codigoLote("L2")
                .fechaVencimiento(LocalDateTime.now().plusDays(15))
                .almacenId(2L)
                .disponibleAntes(new BigDecimal("600.000000"))
                .tomar(new BigDecimal("400.000000"))
                .disponibleDespues(new BigDecimal("200.000000"))
                .build();

        when(movimientoInventarioService.simulateFefo(eq(1L), any(BigDecimal.class), eq(2L)))
                .thenReturn(List.of(primero, segundo));

        mockMvc.perform(get("/api/inventario/fefo/preview")
                        .param("productoId", "1")
                        .param("cantidad", "900")
                        .param("almacenId", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumos[0].loteId").value(10))
                .andExpect(jsonPath("$.consumos[1].tomar").value(400.000000))
                .andExpect(jsonPath("$.totalTomar").value(900.000000));
    }

    @Test
    @DisplayName("GET /api/inventario/fefo/preview retorna 400 cuando hay stock insuficiente")
    void previewFefo_stockInsuficiente() throws Exception {
        CustomBusinessException exception = new CustomBusinessException(
                ApiErrorCode.STOCK_INSUFICIENTE,
                "Stock insuficiente",
                Map.of("faltante", new BigDecimal("100"))
        );
        when(movimientoInventarioService.simulateFefo(eq(1L), any(BigDecimal.class), org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(exception);

        mockMvc.perform(get("/api/inventario/fefo/preview")
                        .param("productoId", "1")
                        .param("cantidad", "1200")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.message").value("Stock insuficiente"));
    }
}
