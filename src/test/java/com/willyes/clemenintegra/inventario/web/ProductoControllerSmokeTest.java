package com.willyes.clemenintegra.inventario.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.controller.ProductoController;
import com.willyes.clemenintegra.inventario.dto.InsumoAutocompleteDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoAutocompleteDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoOptionDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Test
    @WithMockUser(username = "tester", roles = {"SUPER_ADMIN"})
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

        Usuario usuario = Usuario.builder()
                .id(1L)
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .nombreUsuario("tester")
                .clave("secret")
                .activo(true)
                .bloqueado(false)
                .build();

        when(productoService.crearProducto(any(ProductoRequestDTO.class), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(usuario)))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
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
        Usuario usuario = Usuario.builder()
                .id(2L)
                .rol(RolUsuario.ROL_JEFE_ALMACENES)
                .nombreUsuario("almacen")
                .clave("secret")
                .activo(true)
                .bloqueado(false)
                .build();
        Map<String, Object> payload = Map.of(
                "stockMinimo", 5,
                "unidadMedidaId", 1,
                "categoriaProductoId", 2
        );

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .with(SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(usuario)))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
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

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/productos/buscar-fabricables devuelve 200")
    void buscarFabricables_deberiaRetornar200() throws Exception {
        ProductoAutocompleteDTO producto = new ProductoAutocompleteDTO(1, "SKU-FAB", "Producto Fabricable");
        Pageable pageable = PageRequest.of(0, 15);
        Page<ProductoAutocompleteDTO> page = new PageImpl<>(List.of(producto), pageable, 1);

        when(productoService.buscarProductosFabricablesAutocomplete(eq("re"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/productos/buscar-fabricables")
                        .param("term", "re")
                        .param("page", "0")
                        .param("size", "15"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].codigoSku").value("SKU-FAB"));
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    @DisplayName("GET /api/productos/buscar utiliza term/activo y devuelve opciones")
    void buscarProductos_conTerminoYActivo() throws Exception {
        ProductoOptionDTO option = ProductoOptionDTO.builder()
                .id(5L)
                .nombre("Producto RVC")
                .sku("RVC001")
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductoOptionDTO> page = new PageImpl<>(List.of(option), pageable, 1);
        when(productoService.buscarOpciones(anyString(), any(Boolean.class), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/productos/buscar")
                        .param("term", "RVC")
                        .param("activo", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].sku").value("RVC001"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productoService).buscarOpciones(eq("RVC"), eq(true), eq(null), any(Pageable.class));
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    @DisplayName("GET /api/productos/buscar envía almacenId al servicio cuando se proporciona")
    void buscarProductos_conAlmacenId() throws Exception {
        ProductoOptionDTO option = ProductoOptionDTO.builder()
                .id(7L)
                .nombre("Producto Bodega")
                .sku("BOD001")
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductoOptionDTO> page = new PageImpl<>(List.of(option), pageable, 1);
        when(productoService.buscarOpciones(anyString(), any(), anyLong(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/productos/buscar")
                        .param("q", "BOD")
                        .param("almacenId", "8")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].sku").value("BOD001"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productoService).buscarOpciones(eq("BOD"), eq(null), eq(8L), any(Pageable.class));
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    @DisplayName("GET /api/productos/insumos/autocomplete devuelve 200 y unidad de medida en DTO")
    void buscarInsumosAutocomplete_deberiaRetornarUnidadMedida() throws Exception {
        InsumoAutocompleteDTO response = new InsumoAutocompleteDTO(15, "MP-015", "Insumo 15", "Unidad", 3L, "Unidad");
        Pageable pageable = PageRequest.of(0, 10);
        Page<InsumoAutocompleteDTO> page = new PageImpl<>(List.of(response), pageable, 1);

        when(productoService.buscarInsumosAutocomplete(anyString(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/productos/insumos/autocomplete")
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
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/productos/insumos incluye MP, ME, SU y PS")
    void listarInsumosIncluyeCategoriasEsperadas() throws Exception {
        List<ProductoResponseDTO> productos = List.of(
                ProductoResponseDTO.builder().id(1L).nombre("MP").build(),
                ProductoResponseDTO.builder().id(2L).nombre("ME").build(),
                ProductoResponseDTO.builder().id(3L).nombre("SU").build(),
                ProductoResponseDTO.builder().id(4L).nombre("PS").build()
        );

        when(productoService.findByCategoriaTipoIn(any(List.class))).thenReturn(productos);

        mockMvc.perform(get("/api/productos/insumos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("MP"))
                .andExpect(jsonPath("$[1].nombre").value("ME"))
                .andExpect(jsonPath("$[2].nombre").value("SU"))
                .andExpect(jsonPath("$[3].nombre").value("PS"));

        verify(productoService).findByCategoriaTipoIn(argThat(tipos ->
                tipos.containsAll(List.of("MATERIA_PRIMA", "MATERIAL_EMPAQUE", "SUMINISTROS", "PRODUCTO_SEMI_ELABORADO"))));
    }
}
