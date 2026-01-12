package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrdenCompraControllerIntegrationTest extends IntegrationTestMySqlContainer {

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

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;

    private OrdenCompra ordenCompra;

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
                .simbolo("UND")
                .codigo("UND")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria OC")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-OC-1")
                .nombre("Producto OC")
                .descripcionProducto("Producto para OC")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .activo(true)
                .build());

        Proveedor proveedor = proveedorRepository.save(Proveedor.builder()
                .nombre("Proveedor Demo")
                .identificacion("900123456")
                .telefono("555-0000")
                .ciudad("Bogota")
                .email("proveedor@example.com")
                .direccion("Calle 123")
                .paginaWeb("https://proveedor.example.com")
                .nombreContacto("Contacto Demo")
                .activo(true)
                .build());

        OrdenCompraDetalle detalle = OrdenCompraDetalle.builder()
                .producto(producto)
                .cantidad(new BigDecimal("10.000"))
                .cantidadRecibida(new BigDecimal("2.000"))
                .valorUnitario(new BigDecimal("5.000"))
                .valorTotal(new BigDecimal("50.000"))
                .iva(new BigDecimal("19.00"))
                .fechaNecesidad(LocalDate.now().plusDays(5))
                .build();

        ordenCompra = OrdenCompra.builder()
                .codigoOrden("OC-0001")
                .fechaOrden(LocalDateTime.now())
                .estado(EstadoOrdenCompra.ENVIADA)
                .proveedor(proveedor)
                .fechaCompromisoEntrega(LocalDate.now().plusDays(7))
                .descuento(BigDecimal.ZERO)
                .detalles(List.of(detalle))
                .build();
        detalle.setOrdenCompra(ordenCompra);

        ordenCompra = ordenCompraRepository.save(ordenCompra);
    }

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    void listarOrdenesCompraIncluyeProveedor() throws Exception {
        mockMvc.perform(get("/api/ordenes-compra")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ordenCompra.getId()))
                .andExpect(jsonPath("$.content[0].proveedorNombre").value("Proveedor Demo"))
                .andExpect(jsonPath("$.content[0].totalPedido").isNumber())
                .andExpect(jsonPath("$.content[0].totalRecibido").isNumber());
    }
}
