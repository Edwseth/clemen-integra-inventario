package com.willyes.clemenintegra.inventario.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.controller.ProductoController;
import com.willyes.clemenintegra.inventario.dto.ProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductoController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
class ProductoControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductoService productoService;

    @MockBean
    private ProductoRepository productoRepository;

    @MockBean
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @MockBean
    private UnidadMedidaRepository unidadMedidaRepository;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    @DisplayName("POST /api/productos devuelve 201 y datos mínimos al crear un producto")
    void crearProducto_deberiaRetornar201() throws Exception {
        ProductoRequestDTO request = ProductoRequestDTO.builder()
                .sku("SKU-001")
                .nombre("Producto Demo")
                .descripcionProducto("Descripcion opcional")
                .stockMinimo(BigDecimal.TEN)
                .unidadMedidaId(1L)
                .categoriaProductoId(2L)
                .build();

        ProductoResponseDTO response = ProductoResponseDTO.builder()
                .id(100L)
                .sku("SKU-001")
                .nombre("Producto Demo")
                .unidadMedida(new UnidadMedidaResponseDTO(1L, "Unidad", "u"))
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(productoService.crearProducto(any(ProductoRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.nombre").value("Producto Demo"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    @DisplayName("POST /api/productos con datos inválidos devuelve 400 y estructura de error global")
    void crearProducto_conBodyInvalido_deberiaRetornar400() throws Exception {
        Map<String, Object> payload = Map.of(
                "stockMinimo", 5,
                "unidadMedidaId", 1,
                "categoriaProductoId", 2
        );

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.message").value("Solicitud inválida"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    @DisplayName("GET /api/productos devuelve 200 con lista paginada mínima")
    void listarProductos_deberiaRetornar200() throws Exception {
        ProductoResponseDTO producto = ProductoResponseDTO.builder()
                .id(10L)
                .sku("SKU-010")
                .nombre("Producto 10")
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductoResponseDTO> page = new PageImpl<>(List.of(producto), pageable, 1);

        when(productoService.listarTodos(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/productos")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-010"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
}
