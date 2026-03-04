package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoOrdenCompra;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class OrdenCompraListadoFiltrosIntegrationTest extends IntegrationTestMySqlContainer {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private OrdenCompraRepository ordenCompraRepository;
    @Autowired
    private OrdenCompraDetalleRepository ordenCompraDetalleRepository;

    @MockBean
    private com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("comprador")
                .clave("secret")
                .nombreCompleto("Usuario Comprador")
                .correo("comprador@example.com")
                .rol(RolUsuario.ROL_COMPRADOR)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .simbolo("u")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("MP")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-OC-001")
                .nombre("Insumo OC")
                .descripcionProducto("Insumo")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .activo(true)
                .build());

        Producto servicio = productoRepository.save(Producto.builder()
                .codigoSku("SKU-OC-SRV")
                .nombre("Servicio OC")
                .descripcionProducto("Servicio")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .modoControlInventario(ModoControlInventario.SIN_CONTROL_STOCK)
                .activo(true)
                .build());

        Proveedor aldaplast = proveedorRepository.save(Proveedor.builder()
                .nombre("ALDAPLAST")
                .identificacion("100")
                .telefono("111")
                .email("aldaplast@example.com")
                .direccion("Dir 1")
                .nombreContacto("C1")
                .activo(true)
                .build());

        Proveedor otro = proveedorRepository.save(Proveedor.builder()
                .nombre("Proveedor Norte")
                .identificacion("200")
                .telefono("222")
                .email("norte@example.com")
                .direccion("Dir 2")
                .nombreContacto("C2")
                .activo(true)
                .build());

        OrdenCompra ocAtrasada = ordenCompraRepository.save(OrdenCompra.builder()
                .codigoOrden("OC-ATRA")
                .fechaOrden(LocalDateTime.now().minusDays(5))
                .fechaCompromisoEntrega(LocalDate.now().minusDays(1))
                .proveedor(aldaplast)
                .estado(EstadoOrdenCompra.ENVIADA)
                .tipo(TipoOrdenCompra.BIENES)
                .build());

        OrdenCompra ocMismoProveedor = ordenCompraRepository.save(OrdenCompra.builder()
                .codigoOrden("OC-ALDA")
                .fechaOrden(LocalDateTime.now().minusDays(2))
                .fechaCompromisoEntrega(LocalDate.now().plusDays(4))
                .proveedor(aldaplast)
                .estado(EstadoOrdenCompra.CREADA)
                .tipo(TipoOrdenCompra.BIENES)
                .build());

        OrdenCompra ocOtroProveedor = ordenCompraRepository.save(OrdenCompra.builder()
                .codigoOrden("OC-NORTE")
                .fechaOrden(LocalDateTime.now().minusDays(1))
                .fechaCompromisoEntrega(LocalDate.now().plusDays(5))
                .proveedor(otro)
                .estado(EstadoOrdenCompra.ENVIADA)
                .tipo(TipoOrdenCompra.SERVICIOS)
                .build());

        ordenCompraDetalleRepository.save(OrdenCompraDetalle.builder()
                .ordenCompra(ocAtrasada)
                .producto(producto)
                .cantidad(new BigDecimal("10.000"))
                .cantidadRecibida(new BigDecimal("5.000"))
                .valorUnitario(BigDecimal.ONE)
                .valorTotal(new BigDecimal("10.000"))
                .iva(BigDecimal.ZERO)
                .build());

        ordenCompraDetalleRepository.save(OrdenCompraDetalle.builder()
                .ordenCompra(ocMismoProveedor)
                .producto(producto)
                .cantidad(new BigDecimal("8.000"))
                .cantidadRecibida(BigDecimal.ZERO)
                .valorUnitario(BigDecimal.ONE)
                .valorTotal(new BigDecimal("8.000"))
                .iva(BigDecimal.ZERO)
                .build());

        ordenCompraDetalleRepository.save(OrdenCompraDetalle.builder()
                .ordenCompra(ocOtroProveedor)
                .producto(servicio)
                .cantidad(new BigDecimal("6.000"))
                .cantidadRecibida(BigDecimal.ZERO)
                .valorUnitario(BigDecimal.ONE)
                .valorTotal(new BigDecimal("6.000"))
                .iva(BigDecimal.ZERO)
                .build());
    }

    @Test
    @WithMockUser(authorities = "INV_READ")
    void listarSinFiltrosRetornaPageConContenido() throws Exception {
        mockMvc.perform(get("/api/ordenes-compra")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    @WithMockUser(authorities = "INV_READ")
    void filtraPorEstado() throws Exception {
        mockMvc.perform(get("/api/ordenes-compra")
                        .param("page", "0")
                        .param("size", "10")
                        .param("estado", "ENVIADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].estado", everyItem(org.hamcrest.Matchers.is("ENVIADA"))))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @WithMockUser(authorities = "INV_READ")
    void filtraPorProveedorCaseInsensitiveContains() throws Exception {
        mockMvc.perform(get("/api/ordenes-compra")
                        .param("page", "0")
                        .param("size", "10")
                        .param("proveedor", "aldap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].proveedorNombre", everyItem(containsStringIgnoringCase("aldap"))));
    }

    @Test
    @WithMockUser(authorities = "INV_READ")
    void filtraPorEstadoYProveedorConAnd() throws Exception {
        mockMvc.perform(get("/api/ordenes-compra")
                        .param("page", "0")
                        .param("size", "10")
                        .param("estado", "ENVIADA")
                        .param("proveedor", "aldap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].estado").value("ENVIADA"))
                .andExpect(jsonPath("$.content[0].proveedorNombre", containsStringIgnoringCase("aldap")));
    }

    @Test
    @WithMockUser(authorities = "INV_READ")
    void filtraAtrasadasMasEstadoYProveedor() throws Exception {
        mockMvc.perform(get("/api/ordenes-compra")
                        .param("page", "0")
                        .param("size", "10")
                        .param("atrasadas", "true")
                        .param("estado", "ENVIADA")
                        .param("proveedor", "aldap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].codigoOrden").value("OC-ATRA"))
                .andExpect(jsonPath("$.content[0].estado").value("ENVIADA"))
                .andExpect(jsonPath("$.content[0].proveedorNombre", containsStringIgnoringCase("aldap")));
    }

    @Test
    @WithMockUser(authorities = "INV_READ")
    void listarSinTipoIncluyeBienesYServicios() throws Exception {
        mockMvc.perform(get("/api/ordenes-compra")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].tipo", containsInAnyOrder("BIENES", "BIENES", "SERVICIOS")));
    }

    @Test
    @WithMockUser(authorities = "INV_READ")
    void listarConTipoServiciosDevuelveSoloServicios() throws Exception {
        mockMvc.perform(get("/api/ordenes-compra")
                        .param("page", "0")
                        .param("size", "10")
                        .param("tipo", "SERVICIOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[*].tipo", everyItem(org.hamcrest.Matchers.is("SERVICIOS"))));
    }

    @Test
    @WithMockUser(authorities = "INV_READ")
    void listarEstadoSinTipoMantieneDefaultBienes() throws Exception {
        mockMvc.perform(get("/api/ordenes-compra/estado")
                        .param("page", "0")
                        .param("size", "10")
                        .param("estado", "ENVIADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].codigoOrden").value("OC-ATRA"))
                .andExpect(jsonPath("$.content[*].tipo", everyItem(org.hamcrest.Matchers.is("BIENES"))));
    }

    @Test
    @WithMockUser(authorities = "INV_READ")
    void listarEstadoConTipoServiciosDevuelveSoloServicios() throws Exception {
        mockMvc.perform(get("/api/ordenes-compra/estado")
                        .param("page", "0")
                        .param("size", "10")
                        .param("estado", "ENVIADA")
                        .param("tipo", "SERVICIOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].codigoOrden").value("OC-NORTE"))
                .andExpect(jsonPath("$.content[*].tipo", everyItem(org.hamcrest.Matchers.is("SERVICIOS"))));
    }
}
