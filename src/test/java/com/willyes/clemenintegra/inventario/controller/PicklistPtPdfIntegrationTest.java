package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.PicklistPt;
import com.willyes.clemenintegra.inventario.model.PicklistPtAsignacion;
import com.willyes.clemenintegra.inventario.model.PicklistPtLinea;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UbicacionFisica;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.PicklistPtEstado;
import com.willyes.clemenintegra.inventario.model.enums.PicklistPtModoAsignacion;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.PicklistPtRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UbicacionFisicaRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
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
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PicklistPtPdfIntegrationTest extends IntegrationTestMySqlContainer {

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
    private AlmacenRepository almacenRepository;
    @Autowired
    private UbicacionFisicaRepository ubicacionFisicaRepository;
    @Autowired
    private LoteProductoRepository loteProductoRepository;
    @Autowired
    private PicklistPtRepository picklistPtRepository;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;

    private PicklistPt picklist;

    @BeforeEach
    void setUp() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("pdf-user")
                .clave("secret")
                .nombreCompleto("Usuario PDF")
                .correo("pdf@example.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .simbolo("UND")
                .codigo("UND")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria PT")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-PT-001")
                .nombre("Producto PT")
                .descripcionProducto("Producto para PDF")
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

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Almacen PT")
                .ubicacion("Zona PT")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        UbicacionFisica ubicacion = ubicacionFisicaRepository.save(UbicacionFisica.builder()
                .almacen(almacen)
                .codigo("UB-PT-01")
                .descripcion("Ubicacion PT")
                .activo(true)
                .build());

        LoteProducto loteProducto = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LP-PT-001")
                .stockLote(new BigDecimal("100.000"))
                .estado(EstadoLote.LIBERADO)
                .producto(producto)
                .almacen(almacen)
                .ubicacionFisica(ubicacion)
                .build());

        picklist = PicklistPt.builder()
                .codigo("PL-PT-TEST-001")
                .clienteNombre("Cliente PDF")
                .minVidaUtilDias(30)
                .estado(PicklistPtEstado.GENERADO)
                .almacenPtId(almacen.getId())
                .tipoMovimientoDetalleId(1)
                .docReferencia("DOC-PDF")
                .observaciones("Obs PDF")
                .creadoPor(usuario)
                .build();

        PicklistPtLinea linea = PicklistPtLinea.builder()
                .picklist(picklist)
                .producto(producto)
                .cantidad(new BigDecimal("5.000"))
                .modoAsignacion(PicklistPtModoAsignacion.MANUAL_LOTE)
                .loteProducto(loteProducto)
                .build();

        PicklistPtAsignacion asignacion = PicklistPtAsignacion.builder()
                .picklist(picklist)
                .producto(producto)
                .loteProducto(loteProducto)
                .cantidadAsignada(new BigDecimal("5.000"))
                .fechaVencimiento(LocalDateTime.now().plusDays(60))
                .almacenId(almacen.getId())
                .orden((short) 1)
                .build();

        picklist.setLineas(List.of(linea));
        picklist.setAsignaciones(List.of(asignacion));
        picklist = picklistPtRepository.save(picklist);
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void pdfEntregaRespuestaOkSinMultipleBagFetch() throws Exception {
        mockMvc.perform(get("/api/inventario/picklists-pt/{id}/pdf", picklist.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
