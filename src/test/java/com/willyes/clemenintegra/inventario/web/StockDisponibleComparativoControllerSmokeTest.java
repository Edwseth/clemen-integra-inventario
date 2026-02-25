package com.willyes.clemenintegra.inventario.web;

import com.willyes.clemenintegra.inventario.controller.StockDisponibleComparativoController;
import com.willyes.clemenintegra.inventario.dto.StockDisponibleComparativoResponseDTO;
import com.willyes.clemenintegra.inventario.service.StockDisponibleComparativoService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import com.willyes.clemenintegra.support.TestAuth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockDisponibleComparativoController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
class StockDisponibleComparativoControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockDisponibleComparativoService stockDisponibleComparativoService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private com.willyes.clemenintegra.shared.performance.RequestTimingFilter requestTimingFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.logging.RequestIdFilter requestIdFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.security.SuperAdminSoloLecturaWriteBlockFilter superAdminSoloLecturaWriteBlockFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.repository.UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("GET /api/inventario/reportes/stock-disponible/comparativo responde 200 con datos")
    void obtenerComparativo_devuelveOk() throws Exception {
        when(stockDisponibleComparativoService.obtenerComparativo(any(LocalDate.class), any(), any(), any()))
                .thenReturn(List.of(new StockDisponibleComparativoResponseDTO(
                        "SKU-1", "Producto", "KG",
                        new BigDecimal("10.00"), new BigDecimal("12.00"), new BigDecimal("2.00"),
                        "LOTE-1", LocalDate.of(2025, 1, 1), "A1"
                )));

        mockMvc.perform(get("/api/inventario/reportes/stock-disponible/comparativo")
                        .with(TestAuth.auth("almacen", "INV_EXPORT"))
                        .param("fecha", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$[0].cantidadActual").value(12.00));
    }
}
