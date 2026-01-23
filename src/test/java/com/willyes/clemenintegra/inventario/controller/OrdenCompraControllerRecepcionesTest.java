package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.RecepcionOCResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.OrdenCompraMapper;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.HistorialEstadoOrdenService;
import com.willyes.clemenintegra.inventario.service.OrdenCompraPdfService;
import com.willyes.clemenintegra.inventario.service.OrdenCompraService;
import com.willyes.clemenintegra.inventario.service.RecepcionOCService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrdenCompraController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        })
class OrdenCompraControllerRecepcionesTest {

    @Autowired
    private MockMvc mockMvc;

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
    private OrdenCompraPdfService ordenCompraPdfService;
    @MockBean
    private OrdenCompraMapper ordenCompraMapper;
    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void recepcionesEndpointReturnsList() throws Exception {
        when(recepcionOCService.listarRecepcionesPorOrden(1L))
                .thenReturn(List.of(RecepcionOCResponseDTO.builder().id(20L).codigo("RC-001").build()));

        var result = mockMvc.perform(get("/api/ordenes-compra/1/recepciones")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isNotNull();
    }
}
