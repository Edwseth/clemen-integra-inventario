package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LoteProductoPorEvaluarIntegrationTest extends IntegrationTestMySqlContainer {

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

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void listarLotesPorEvaluar_sinEstado_devuelveMezclaRetenidoYCuarentena() throws Exception {
        TestData data = crearDataBase();
        crearLote(data, EstadoLote.EN_CUARENTENA, "LP-EVAL-C");
        crearLote(data, EstadoLote.RETENIDO, "LP-EVAL-R");

        mockMvc.perform(get("/api/lotes/por-evaluar")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].estado", hasItems("EN_CUARENTENA", "RETENIDO")));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void listarLotesPorEvaluar_estadoRetenido_devuelveSoloRetenidos() throws Exception {
        TestData data = crearDataBase();
        crearLote(data, EstadoLote.EN_CUARENTENA, "LP-EVAL-C2");
        crearLote(data, EstadoLote.RETENIDO, "LP-EVAL-R2");

        mockMvc.perform(get("/api/lotes/por-evaluar")
                        .param("page", "0")
                        .param("size", "20")
                        .param("estado", "RETENIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].estado", everyItem(is("RETENIDO"))));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void listarLotesPorEvaluar_estadoEnCuarentena_devuelveSoloEnCuarentena() throws Exception {
        TestData data = crearDataBase();
        crearLote(data, EstadoLote.EN_CUARENTENA, "LP-EVAL-C3");
        crearLote(data, EstadoLote.RETENIDO, "LP-EVAL-R3");

        mockMvc.perform(get("/api/lotes/por-evaluar")
                        .param("page", "0")
                        .param("size", "20")
                        .param("estado", "EN_CUARENTENA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].estado", everyItem(is("EN_CUARENTENA"))));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void listarLotesPorEvaluar_estadoInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/lotes/por-evaluar")
                        .param("page", "0")
                        .param("size", "10")
                        .param("estado", "XXX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRO_INVALIDO"))
                .andExpect(jsonPath("$.message", startsWith("estado inválido:")));
    }

    private TestData crearDataBase() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("calidad-user-" + suffix)
                .clave("secret")
                .nombreCompleto("Usuario Calidad")
                .correo("calidad-user-" + suffix + "@example.com")
                .rol(RolUsuario.ROL_JEFE_CALIDAD)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad QA " + suffix)
                .simbolo("U" + suffix.substring(0, 3).toUpperCase())
                .codigo("C" + suffix.substring(0, 3).toUpperCase())
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria Lote " + suffix)
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-LOTE-EVAL-" + suffix)
                .nombre("Producto Lote Eval " + suffix)
                .descripcionProducto("Producto para evaluación")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.FISICO)
                .requiereAnalisisFisico(true)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .activo(true)
                .build());

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Almacen Calidad " + suffix)
                .ubicacion("Zona QA")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        return new TestData(usuario, producto, almacen);
    }

    private void crearLote(TestData data, EstadoLote estado, String codigoPrefix) {
        loteProductoRepository.save(LoteProducto.builder()
                .codigoLote(codigoPrefix + "-" + UUID.randomUUID().toString().substring(0, 4))
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .stockLote(BigDecimal.TEN)
                .estado(estado)
                .producto(data.producto)
                .almacen(data.almacen)
                .usuarioLiberador(data.usuario)
                .build());
    }

    private record TestData(Usuario usuario, Producto producto, Almacen almacen) {
    }
}
