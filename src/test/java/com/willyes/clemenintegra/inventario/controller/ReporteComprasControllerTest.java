package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.reportes.ReporteComprasRowDTO;
import com.willyes.clemenintegra.inventario.service.ReporteComprasService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReporteComprasController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ReporteComprasControllerTest.MethodSecurityTestConfig.class)
class ReporteComprasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReporteComprasService reporteComprasService;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    void obtenerReporteRetorna200ConCamposEsperados() throws Exception {
        ReporteComprasRowDTO fila = ReporteComprasRowDTO.builder()
                .ocCodigo("OC-001")
                .estado("ABIERTA")
                .productoCodigo("SKU-001")
                .productoNombre("Materia Prima")
                .udm("KG")
                .cantidad(new BigDecimal("10.50"))
                .fechaOc(LocalDateTime.of(2026, 2, 2, 10, 30))
                .fechaPactada(LocalDate.of(2026, 2, 10))
                .fechaRecepcion(LocalDate.of(2026, 2, 12))
                .proveedorNombre("Proveedor Demo")
                .condicionesPago("CONTADO")
                .precioUnitario(new BigDecimal("25000"))
                .iva(new BigDecimal("19"))
                .build();

        when(reporteComprasService.generar(any(), any())).thenReturn(List.of(fila));

        mockMvc.perform(get("/api/reportes/compras")
                        .param("desde", "2026-02-01")
                        .param("hasta", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ocCodigo").value("OC-001"))
                .andExpect(jsonPath("$[0].estado").value("ABIERTA"))
                .andExpect(jsonPath("$[0].productoCodigo").value("SKU-001"))
                .andExpect(jsonPath("$[0].productoNombre").value("Materia Prima"))
                .andExpect(jsonPath("$[0].udm").value("KG"))
                .andExpect(jsonPath("$[0].cantidad").isNotEmpty())
                .andExpect(jsonPath("$[0].fechaOc").isNotEmpty())
                .andExpect(jsonPath("$[0].fechaPactada").isNotEmpty())
                .andExpect(jsonPath("$[0].fechaRecepcion").isNotEmpty())
                .andExpect(jsonPath("$[0].proveedorNombre").value("Proveedor Demo"))
                .andExpect(jsonPath("$[0].condicionesPago").value("CONTADO"))
                .andExpect(jsonPath("$[0].precioUnitario").isNotEmpty())
                .andExpect(jsonPath("$[0].iva").isNotEmpty());
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void exportarExcelRetorna200YBytesNoVacios() throws Exception {
        when(reporteComprasService.exportarExcel(any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/reportes/compras/excel")
                        .param("desde", "2026-02-01")
                        .param("hasta", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"reporte_compras_2026-02-01_2026-02-28.xlsx\""));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
