package com.willyes.clemenintegra.inventario.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.controller.ProductoController;
import com.willyes.clemenintegra.inventario.dto.InsumoAutocompleteDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoAutocompleteDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaAutocompleteDTO;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.support.TestAuth;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductoController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, UsuarioInactivoFilter.class, RequestTimingFilter.class, RequestIdFilter.class})
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
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Test
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

        when(productoService.crearProducto(any(ProductoRequestDTO.class), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(TestAuth.auth("tester", "INV_WRITE"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.nombre").value("Producto Demo"));
    }

    @Test
    @DisplayName("POST /api/productos con datos inválidos devuelve 400 y estructura de error global")
    void crearProducto_conBodyInvalido_deberiaRetornar400() throws Exception {
        Map<String, Object> payload = Map.of(
                "stockMinimo", 5,
                "unidadMedidaId", 1,
                "categoriaProductoId", 2
        );

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .with(TestAuth.auth("almacen", "INV_WRITE"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.message").value("Solicitud inválida"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
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
                        .with(TestAuth.auth("almacen", "INV_PRODUCT_READ"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-010"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/productos/buscar-fabricables devuelve 200")
    void buscarFabricables_deberiaRetornar200() throws Exception {
        ProductoAutocompleteDTO producto = new ProductoAutocompleteDTO(1, "SKU-FAB", "Producto Fabricable");
        Pageable pageable = PageRequest.of(0, 15);
        Page<ProductoAutocompleteDTO> page = new PageImpl<>(List.of(producto), pageable, 1);

        when(productoService.buscarProductosFabricablesAutocomplete(eq("re"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/productos/buscar-fabricables")
                        .with(TestAuth.auth("prod", "INV_PRODUCT_READ"))
                        .param("term", "re")
                        .param("page", "0")
                        .param("size", "15"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].codigoSku").value("SKU-FAB"));
    }

    @Test
    @DisplayName("GET /api/productos/buscar permite acceso a contador")
    void buscarProductosParaAjustes_contadorOk() throws Exception {
        UnidadMedidaAutocompleteDTO unidad = new UnidadMedidaAutocompleteDTO(3L, "Unidad", "U", 2);
        ProductoAutocompleteDTO response = new ProductoAutocompleteDTO(5, "SKU-RESV", "Resveratrol 500", unidad);

        when(productoService.buscarAutocompleteInventarioAjustes(eq("resveratrol"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/productos/buscar")
                        .with(TestAuth.auth("contador", "INV_PRODUCT_READ"))
                        .param("query", "resveratrol")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigoSku").value("SKU-RESV"))
                .andExpect(jsonPath("$.content[0].nombre").value("Resveratrol 500"))
                .andExpect(jsonPath("$.content[0].unidadMedida.id").value(3))
                .andExpect(jsonPath("$.content[0].unidadMedida.abreviatura").value("U"));

        verify(productoService).buscarAutocompleteInventarioAjustes(eq("resveratrol"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/productos/buscar rechaza roles sin permiso")
    void buscarProductosParaAjustes_rolNoPermitido() throws Exception {
        mockMvc.perform(get("/api/productos/buscar")
                        .with(TestAuth.auth("almacenista", "INV_READ"))
                        .param("query", "resveratrol"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/productos/buscar devuelve coincidencias por nombre")
    void buscarProductosParaAjustes_buscaPorNombre() throws Exception {
        ProductoAutocompleteDTO response = new ProductoAutocompleteDTO(9, "SKU-123", "Resveratrol Gold", null);
        when(productoService.buscarAutocompleteInventarioAjustes(eq("resveratrol"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/productos/buscar")
                        .with(TestAuth.auth("admin", "INV_PRODUCT_READ"))
                        .param("query", "resveratrol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Resveratrol Gold"));
    }

    @Test
    @DisplayName("GET /api/productos/buscar devuelve coincidencias por codigoSku")
    void buscarProductosParaAjustes_buscaPorSku() throws Exception {
        ProductoAutocompleteDTO response = new ProductoAutocompleteDTO(11, "SKU-ABC-01", "Producto SKU", null);
        when(productoService.buscarAutocompleteInventarioAjustes(eq("SKU-ABC"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/productos/buscar")
                        .with(TestAuth.auth("admin", "INV_PRODUCT_READ"))
                        .param("query", "SKU-ABC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigoSku").value("SKU-ABC-01"));
    }

    @Test
    @DisplayName("GET /api/productos/buscar acepta term por compatibilidad")
    void buscarProductosParaAjustes_aceptaTerm() throws Exception {
        ProductoAutocompleteDTO response = new ProductoAutocompleteDTO(12, "SKU-AL-01", "Almidón de maíz", null);
        when(productoService.buscarAutocompleteInventarioAjustes(eq("al"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/productos/buscar")
                        .with(TestAuth.auth("contador", "INV_PRODUCT_READ"))
                        .param("term", "al")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigoSku").value("SKU-AL-01"));

        verify(productoService).buscarAutocompleteInventarioAjustes(eq("al"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/productos/insumos/autocomplete devuelve 200 y unidad de medida en DTO")
    void buscarInsumosAutocomplete_deberiaRetornarUnidadMedida() throws Exception {
        InsumoAutocompleteDTO response = new InsumoAutocompleteDTO(15, "MP-015", "Insumo 15", "Unidad", 3L, "Unidad");
        Pageable pageable = PageRequest.of(0, 10);
        Page<InsumoAutocompleteDTO> page = new PageImpl<>(List.of(response), pageable, 1);

        when(productoService.buscarInsumosAutocomplete(anyString(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/productos/insumos/autocomplete")
                        .with(TestAuth.auth("admin", "INV_PRODUCT_READ"))
                        .param("term", "MP")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(15))
                .andExpect(jsonPath("$.content[0].unidad").value("Unidad"))
                .andExpect(jsonPath("$.content[0].unidadMedidaId").value(3))
                .andExpect(jsonPath("$.content[0].unidadMedidaNombre").value("Unidad"));

        verify(productoService).buscarInsumosAutocomplete(eq("MP"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/productos/insumos incluye MP, ME, SU y PS")
    void listarInsumosIncluyeCategoriasEsperadas() throws Exception {
        List<ProductoResponseDTO> productos = List.of(
                ProductoResponseDTO.builder().id(1L).nombre("MP").build(),
                ProductoResponseDTO.builder().id(2L).nombre("ME").build(),
                ProductoResponseDTO.builder().id(3L).nombre("SU").build(),
                ProductoResponseDTO.builder().id(4L).nombre("PS").build()
        );

        when(productoService.findByCategoriaTipoIn(any(List.class))).thenReturn(productos);

        mockMvc.perform(get("/api/productos/insumos")
                        .with(TestAuth.auth("prod", "INV_PRODUCT_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("MP"))
                .andExpect(jsonPath("$[1].nombre").value("ME"))
                .andExpect(jsonPath("$[2].nombre").value("SU"))
                .andExpect(jsonPath("$[3].nombre").value("PS"));

        verify(productoService).findByCategoriaTipoIn(argThat(tipos ->
                tipos.containsAll(List.of("MATERIA_PRIMA", "MATERIAL_EMPAQUE", "SUMINISTROS", "PRODUCTO_SEMI_ELABORADO"))));
    }
}
