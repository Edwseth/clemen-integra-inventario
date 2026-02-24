package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.inventario.service.InventarioGeneralCorteReportService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.inventario.service.ReporteInventarioService;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        ReporteInventarioController.class,
        ReporteInventarioControllerSecurityTest.MethodSecurityConfig.class
})
class ReporteInventarioControllerSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private ReporteInventarioController reporteInventarioController;

    @MockBean
    private ReporteInventarioService reporteInventarioService;
    @MockBean
    private ProductoService productoService;
    @MockBean
    private LoteProductoService loteProductoService;
    @MockBean
    private MovimientoInventarioService movimientoInventarioService;
    @MockBean
    private ProductoRepository productoRepository;
    @MockBean
    private InventarioGeneralCorteReportService inventarioGeneralCorteReportService;
    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void altaRotacionPermiteSuperAdmin() throws Exception {
        when(reporteInventarioService.generarReporteAltaRotacion(any(), any()))
                .thenReturn(new XSSFWorkbook());

        var response = reporteInventarioController.altaRotacion(
                LocalDate.now(),
                LocalDate.now().plusDays(1)
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
