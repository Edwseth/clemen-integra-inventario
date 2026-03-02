package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.UbicacionFisica;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UbicacionFisicaRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LotePendientesUbicarPtIntegrationTest extends IntegrationTestMySqlContainer {

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
    private LoteProductoRepository loteProductoRepository;
    @Autowired
    private UbicacionFisicaRepository ubicacionFisicaRepository;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    @WithMockUser(authorities = "INV_LOTES_READ")
    void listarPendientesUbicarPt_filtraCorrectamente() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Usuario user = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("inv-user-" + suffix)
                .clave("secret")
                .nombreCompleto("Inv User")
                .correo("inv-user-" + suffix + "@example.com")
                .rol(RolUsuario.ROL_JEFE_ALMACENES)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida um = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad " + suffix)
                .simbolo("U" + suffix.substring(0, 2))
                .codigo("UN" + suffix.substring(0, 2))
                .build());

        CategoriaProducto catPt = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("PT " + suffix)
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());
        CategoriaProducto catMp = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("MP " + suffix)
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Producto prodPt = productoRepository.save(Producto.builder()
                .codigoSku("PT-" + suffix)
                .nombre("Producto PT " + suffix)
                .stockMinimo(BigDecimal.ZERO)
                .activo(true)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(um)
                .categoriaProducto(catPt)
                .creadoPor(user)
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .build());

        Producto prodMp = productoRepository.save(Producto.builder()
                .codigoSku("MP-" + suffix)
                .nombre("Producto MP " + suffix)
                .stockMinimo(BigDecimal.ZERO)
                .activo(true)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(um)
                .categoriaProducto(catMp)
                .creadoPor(user)
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .build());

        Almacen cuarentena = almacenRepository.save(Almacen.builder()
                .nombre("Cuarentena " + suffix)
                .ubicacion("Z7")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());
        Almacen principalPt = almacenRepository.save(Almacen.builder()
                .nombre("Principal PT " + suffix)
                .ubicacion("Z2")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        ubicacionFisicaRepository.save(UbicacionFisica.builder()
                .almacen(principalPt)
                .codigo("A1")
                .descripcion("Rack A1")
                .activo(true)
                .usuario(user)
                .build());

        loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LP-OK-" + suffix)
                .fechaFabricacion(LocalDateTime.now().minusDays(2))
                .fechaVencimiento(LocalDateTime.now().plusDays(10))
                .stockLote(new BigDecimal("10.00"))
                .stockReservado(new BigDecimal("2.00"))
                .estado(EstadoLote.LIBERADO)
                .producto(prodPt)
                .almacen(cuarentena)
                .build());

        loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LP-PT-SIN-STOCK-" + suffix)
                .fechaFabricacion(LocalDateTime.now().minusDays(2))
                .stockLote(new BigDecimal("3.00"))
                .stockReservado(new BigDecimal("3.00"))
                .estado(EstadoLote.LIBERADO)
                .producto(prodPt)
                .almacen(cuarentena)
                .build());

        loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LP-MP-" + suffix)
                .fechaFabricacion(LocalDateTime.now().minusDays(2))
                .stockLote(new BigDecimal("5.00"))
                .stockReservado(BigDecimal.ZERO)
                .estado(EstadoLote.LIBERADO)
                .producto(prodMp)
                .almacen(cuarentena)
                .build());

        loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LP-PT-PRINCIPAL-" + suffix)
                .fechaFabricacion(LocalDateTime.now().minusDays(2))
                .stockLote(new BigDecimal("5.00"))
                .stockReservado(BigDecimal.ZERO)
                .estado(EstadoLote.LIBERADO)
                .producto(prodPt)
                .almacen(principalPt)
                .build());

        when(inventoryCatalogResolver.getAlmacenCuarentenaId()).thenReturn(cuarentena.getId().longValue());
        when(inventoryCatalogResolver.getAlmacenPtId()).thenReturn(principalPt.getId().longValue());

        mockMvc.perform(get("/api/lotes/pendientes-ubicar-pt")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].estado").value("LIBERADO"))
                .andExpect(jsonPath("$.content[0].nombreProducto").value("Producto PT " + suffix))
                .andExpect(jsonPath("$.content[0].almacenIdActual").value(cuarentena.getId()))
                .andExpect(jsonPath("$.content[0].almacenDestinoSugeridoId").value(principalPt.getId()))
                .andExpect(jsonPath("$.content[0].requiereUbicacionDestino").value(true));
    }
}
