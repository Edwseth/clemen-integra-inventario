package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class RecepcionOCControllerIntegrationTest extends IntegrationTestMySqlContainer {

    private static final String CODIGO_RECEPCION = "RC-20240101-01";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AlmacenRepository almacenRepository;
    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private LoteProductoRepository loteProductoRepository;
    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private OrdenCompraRepository ordenCompraRepository;
    @Autowired
    private OrdenCompraDetalleRepository ordenCompraDetalleRepository;
    @Autowired
    private RecepcionOCRepository recepcionOCRepository;
    @Autowired
    private RecepcionOCDetalleRepository recepcionOCDetalleRepository;
    @Autowired
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Autowired
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @MockBean
    private com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;

    private RecepcionOC recepcionOC;
    private MovimientoInventario movimientoInventario;

    @BeforeEach
    void setUp() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("jefe")
                .clave("secreto")
                .nombreCompleto("Jefe Almacenes")
                .correo("jefe@example.com")
                .rol(RolUsuario.ROL_JEFE_ALMACENES)
                .activo(true)
                .bloqueado(false)
                .build());

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Principal")
                .ubicacion("Zona A")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen.PRINCIPAL)
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
                .codigoSku("SKU-123")
                .nombre("Producto Recepcion")
                .descripcionProducto("Descripción")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad.NINGUNO)
                .activo(true)
                .build());

        LoteProducto lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-01")
                .producto(producto)
                .almacen(almacen)
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("5.00"))
                .stockReservado(BigDecimal.ZERO.setScale(6))
                .agotado(false)
                .build());

        Proveedor proveedor = proveedorRepository.save(Proveedor.builder()
                .nombre("Proveedor Test")
                .identificacion("123456789")
                .telefono("1234567")
                .email("proveedor@example.com")
                .direccion("Calle 123")
                .nombreContacto("Contacto")
                .activo(true)
                .build());

        OrdenCompra ordenCompra = ordenCompraRepository.save(OrdenCompra.builder()
                .codigoOrden("OC-001")
                .fechaOrden(LocalDateTime.of(2023, 12, 31, 8, 0))
                .proveedor(proveedor)
                .estado(com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra.ENVIADA)
                .observaciones("Orden de prueba")
                .build());

        OrdenCompraDetalle ordenCompraDetalle = ordenCompraDetalleRepository.save(OrdenCompraDetalle.builder()
                .ordenCompra(ordenCompra)
                .producto(producto)
                .cantidad(new BigDecimal("10.000"))
                .valorUnitario(new BigDecimal("2.000"))
                .valorTotal(new BigDecimal("20.000"))
                .iva(BigDecimal.ZERO.setScale(2))
                .cantidadRecibida(BigDecimal.ZERO.setScale(3))
                .build());

        recepcionOC = recepcionOCRepository.save(RecepcionOC.builder()
                .codigo(CODIGO_RECEPCION)
                .fechaRecepcion(LocalDate.of(2024, 1, 1))
                .ordenCompra(ordenCompra)
                .almacenDestino(almacen)
                .proveedor(proveedor)
                .usuario(usuario)
                .observaciones("Recepción de prueba")
                .build());

        RecepcionOCDetalle detalle = recepcionOCDetalleRepository.save(RecepcionOCDetalle.builder()
                .recepcionOc(recepcionOC)
                .ordenCompraDetalle(ordenCompraDetalle)
                .producto(producto)
                .lote(lote)
                .cantidadRecibida(new BigDecimal("5.000"))
                .build());
        recepcionOC.getDetalles().add(detalle);

        MotivoMovimiento motivo = motivoMovimientoRepository.save(MotivoMovimiento.builder()
                .descripcion("Recepción de compra")
                .motivo(ClasificacionMovimientoInventario.RECEPCION_COMPRA)
                .build());

        TipoMovimientoDetalle tipoDetalle = tipoMovimientoDetalleRepository.save(TipoMovimientoDetalle.builder()
                .descripcion("Ingreso de recepción")
                .build());

        movimientoInventario = movimientoInventarioRepository.save(MovimientoInventario.builder()
                .cantidad(new BigDecimal("5.00"))
                .tipoMovimiento(TipoMovimiento.RECEPCION)
                .clasificacion(ClasificacionMovimientoInventario.RECEPCION_COMPRA)
                .registradoPor(usuario)
                .producto(producto)
                .lote(lote)
                .almacenDestino(almacen)
                .proveedor(proveedor)
                .ordenCompra(ordenCompra)
                .motivoMovimiento(motivo)
                .tipoMovimientoDetalle(tipoDetalle)
                .ordenCompraDetalle(ordenCompraDetalle)
                .recepcionOc(recepcionOC)
                .codigoRecepcion(CODIGO_RECEPCION)
                .docReferencia("OC-001")
                .build());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void obtenerPorCodigoEnRutaDevuelveCabeceraYMovimientos() throws Exception {
        mockMvc.perform(get("/api/recepciones/{codigo}", CODIGO_RECEPCION)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(CODIGO_RECEPCION))
                .andExpect(jsonPath("$.ordenCompraId").value(recepcionOC.getOrdenCompra().getId()))
                .andExpect(jsonPath("$.detalles[0].cantidadRecibida").value(5.0))
                .andExpect(jsonPath("$.movimientos[0].id").value(movimientoInventario.getId()))
                .andExpect(jsonPath("$.movimientos[0].codigoRecepcion").value(CODIGO_RECEPCION));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void obtenerPorCodigoEnQueryDevuelveCabeceraYMovimientos() throws Exception {
        mockMvc.perform(get("/api/recepciones")
                        .param("codigo", CODIGO_RECEPCION)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(CODIGO_RECEPCION))
                .andExpect(jsonPath("$.movimientos[0].codigoRecepcion").value(CODIGO_RECEPCION))
                .andExpect(jsonPath("$.movimientos[0].id").value(movimientoInventario.getId()));
    }
}
