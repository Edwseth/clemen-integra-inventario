package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestH2;
import java.math.BigDecimal;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(authorities = "ROL_JEFE_CALIDAD")
class VidaUtilProductoControllerIntegrationTest extends IntegrationTestH2 {

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

    @MockBean
    private com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    private CategoriaProducto categoriaPt;
    private CategoriaProducto categoriaPs;

    @BeforeEach
    void setUp() {
        productoRepository.deleteAll();
        categoriaProductoRepository.deleteAll();
        unidadMedidaRepository.deleteAll();
        usuarioRepository.deleteAll();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("tester-vida-util-" + suffix)
                .clave("secret")
                .nombreCompleto("Tester Vida Util")
                .correo("tester-vida-util-" + suffix + "@example.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad Vida Util " + suffix)
                .simbolo("UV")
                .codigo("UV1")
                .build());

        categoriaPt = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("PT " + suffix)
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        categoriaPs = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("PS " + suffix)
                .tipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO)
                .build());

        CategoriaProducto categoriaMp = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("MP " + suffix)
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        productoRepository.save(crearProducto("PT0001", "Producto Citrato", usuario, unidad, categoriaPt));
        productoRepository.save(crearProducto("PS0002", "Citrato Semielaborado", usuario, unidad, categoriaPs));
        productoRepository.save(crearProducto("MP0001", "Citrato Materia Prima", usuario, unidad, categoriaMp));
        productoRepository.save(crearProducto("PT0003", "Otro Producto", usuario, unidad, categoriaPt));
    }

    @Test
    void buscarPorSkuAplicaFiltroCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/calidad/vida-util/productos-terminados")
                        .param("search", "PT0001")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].codigoSku").value("PT0001"));
    }

    @Test
    void buscarPorNombreAplicaFiltroContiene() throws Exception {
        mockMvc.perform(get("/api/calidad/vida-util/productos-terminados")
                        .param("search", "citrat")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].codigoSku",
                        Matchers.containsInAnyOrder("PT0001", "PS0002")));
    }

    @Test
    void buscarSinCoincidenciasDevuelveListaVacia() throws Exception {
        mockMvc.perform(get("/api/calidad/vida-util/productos-terminados")
                        .param("search", "zzzz")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    private Producto crearProducto(
            String codigoSku,
            String nombre,
            Usuario usuario,
            UnidadMedida unidad,
            CategoriaProducto categoria) {
        return Producto.builder()
                .codigoSku(codigoSku)
                .nombre(nombre)
                .stockMinimo(BigDecimal.ONE)
                .activo(true)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .build();
    }
}
