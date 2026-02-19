package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraDetalleRequestDTO;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraRequestDTO;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.model.enums.CondicionesPago;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.mapper.OrdenCompraMapper;
import com.willyes.clemenintegra.inventario.mapper.RecepcionOCMapper;
import com.willyes.clemenintegra.inventario.service.HistorialEstadoOrdenService;
import com.willyes.clemenintegra.inventario.service.OrdenCompraPdfService;
import com.willyes.clemenintegra.inventario.service.OrdenCompraService;
import com.willyes.clemenintegra.inventario.service.RecepcionOCService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        OrdenCompraController.class,
        OrdenCompraControllerSecurityTest.MethodSecurityConfig.class
})
class OrdenCompraControllerSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private OrdenCompraController ordenCompraController;

    @MockBean
    private OrdenCompraRepository ordenCompraRepository;
    @MockBean
    private OrdenCompraDetalleRepository detalleRepository;
    @MockBean
    private ProveedorRepository proveedorRepository;
    @MockBean
    private ProductoRepository productoRepository;
    @MockBean
    private OrdenCompraService ordenCompraService;
    @MockBean
    private HistorialEstadoOrdenService historialEstadoOrdenService;
    @MockBean
    private RecepcionOCService recepcionOCService;
    @MockBean
    private RecepcionOCRepository recepcionOCRepository;
    @MockBean
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @MockBean
    private MovimientoInventarioMapper movimientoInventarioMapper;
    @MockBean
    private RecepcionOCMapper recepcionOCMapper;
    @MockBean
    private OrdenCompraPdfService ordenCompraPdfService;
    @MockBean
    private OrdenCompraMapper ordenCompraMapper;
    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    @WithMockUser(authorities = "PO_WRITE")
    void crearOrdenCompraPermiteSuperAdmin() throws Exception {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(1);

        Producto producto = new Producto();
        producto.setId(2);

        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));
        when(ordenCompraService.calcularFechaCompromisoEntrega(any())).thenReturn(LocalDate.now());
        when(ordenCompraService.generarCodigoOrdenCompra()).thenReturn("OC-001");

        OrdenCompra guardada = OrdenCompra.builder()
                .id(10)
                .proveedor(proveedor)
                .fechaOrden(LocalDateTime.now())
                .detalles(List.of())
                .build();
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenReturn(guardada);

        OrdenCompraDetalleRequestDTO detalle = new OrdenCompraDetalleRequestDTO(
                2L,
                BigDecimal.ONE,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                LocalDate.now().plusDays(7)
        );

        OrdenCompraRequestDTO request = new OrdenCompraRequestDTO(
                1L,
                CondicionesPago.CONTADO,
                "comprador",
                "obs",
                BigDecimal.ZERO,
                LocalDate.now().plusDays(10),
                List.of(detalle)
        );

        var response = ordenCompraController.crear(request);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }
}
